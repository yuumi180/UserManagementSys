<template>
  <div class="ai-chat-page">
    <div class="page-toolbar">
      <el-button @click="$router.push('/')">
        <el-icon><Back /></el-icon>
        返回用户管理
      </el-button>
      <div class="toolbar-title">
        <div class="page-title">AI 数据问答</div>
        <div class="page-subtitle">基于用户、角色和操作日志的只读分析助手</div>
      </div>
    </div>

    <div class="chat-layout">
      <section class="chat-panel">
        <div class="chat-header">
          <div>
            <div class="section-title">对话</div>
            <div class="section-desc">可以询问用户结构、城市分布、角色配置和日志风险</div>
          </div>
          <el-button text type="primary" @click="resetChat">清空</el-button>
        </div>

        <div ref="messageListRef" class="message-list">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-row"
            :class="message.role"
          >
            <div class="message-avatar">
              <el-icon v-if="message.role === 'assistant'"><Cpu /></el-icon>
              <el-icon v-else><User /></el-icon>
            </div>
            <div class="message-bubble">
              <div class="message-meta">{{ message.role === 'assistant' ? 'AI 助手' : '我' }}</div>
              <div
                v-if="message.content"
                class="message-content rendered-markdown"
                v-html="renderMarkdown(message.content)"
              />
              <div v-else class="message-content typing-text">正在生成回答...</div>
              <div v-if="message.sources?.length" class="source-list">
                <el-tag v-for="source in message.sources" :key="source" size="small">
                  {{ source }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>

        <div class="quick-actions">
          <button
            v-for="item in quickQuestions"
            :key="item"
            type="button"
            class="quick-button"
            @click="askQuickQuestion(item)"
          >
            {{ item }}
          </button>
        </div>

        <div class="composer">
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            resize="none"
            maxlength="300"
            show-word-limit
            placeholder="例如：帮我分析一下当前用户年龄分布，并指出数据质量问题"
            @keydown.ctrl.enter="sendMessage"
          />
          <el-button type="primary" :loading="loading" @click="sendMessage">
            <el-icon><Promotion /></el-icon>
            发送
          </el-button>
        </div>
      </section>

      <aside class="insight-panel">
        <div class="history-panel">
          <div class="history-head">
            <div class="section-title">对话记录</div>
            <el-button size="small" type="primary" text @click="createNewSession">
              <el-icon><Plus /></el-icon>
              新建
            </el-button>
          </div>
          <el-input
            v-model="searchKeyword"
            size="small"
            clearable
            placeholder="搜索标题或消息内容"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <div class="session-list">
            <button
              v-for="session in filteredSessions"
              :key="session.id"
              type="button"
              class="session-item"
              :class="{ active: session.id === currentSessionId }"
              @click="switchSession(session.id)"
            >
              <span class="session-main">
                <span class="session-title">{{ session.title }}</span>
                <span class="session-meta">
                  {{ formatSessionTime(session.updatedAt) }} · {{ countSessionMessages(session) }} 条
                </span>
              </span>
              <el-button
                class="delete-session"
                type="danger"
                text
                size="small"
                title="删除记录"
                @click.stop="deleteSession(session.id)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </button>
            <div v-if="filteredSessions.length === 0" class="session-empty">
              没有找到匹配记录
            </div>
          </div>
        </div>

        <div class="section-title with-gap">模型状态</div>
        <div class="model-box">
          <div class="model-row">
            <span>类型</span>
            <strong>{{ modelInfo.provider || '-' }}</strong>
          </div>
          <div class="model-row">
            <span>模型</span>
            <strong>{{ modelInfo.model || '-' }}</strong>
          </div>
          <el-tag :type="modelStatusTagType" class="model-tag">
            {{ modelStatusText }}
          </el-tag>
          <p v-if="modelInfo.fallback && modelInfo.reason" class="model-reason">
            {{ modelInfo.reason }}
          </p>
        </div>

        <div class="section-title with-gap">数据上下文</div>
        <div class="metric-grid">
          <div class="metric-card">
            <span>用户总数</span>
            <strong>{{ metrics.totalUsers ?? '-' }}</strong>
          </div>
          <div class="metric-card">
            <span>平均年龄</span>
            <strong>{{ metrics.avgAge ?? '-' }}</strong>
          </div>
          <div class="metric-card">
            <span>角色数量</span>
            <strong>{{ metrics.roleCount ?? '-' }}</strong>
          </div>
          <div class="metric-card">
            <span>近期日志</span>
            <strong>{{ metrics.recentLogCount ?? '-' }}</strong>
          </div>
        </div>

        <div class="suggestion-panel">
          <div class="section-title">推荐追问</div>
          <button
            v-for="item in suggestions"
            :key="item"
            type="button"
            class="suggestion-item"
            @click="askQuickQuestion(item)"
          >
            <span>{{ item }}</span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>

        <div class="guard-panel">
          <div class="section-title">安全边界</div>
          <p>助手只读取用户管理系统数据，不执行新增、修改、删除操作，也不会返回密码等敏感字段。</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowRight, Back, Cpu, Delete, Plus, Promotion, Search, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const API_BASE_URL = process.env.VUE_APP_BASE_API || 'http://localhost:9090'
const STORAGE_KEY = 'aiChatSessions'
const CURRENT_SESSION_KEY = 'aiChatCurrentSessionId'
const TYPE_INTERVAL = 18
const TYPE_STEP = 3
const DEFAULT_SUGGESTIONS = ['分析用户结构', '查看城市 Top5', '总结最近日志风险']
const WELCOME_MESSAGE = '你好，我可以基于当前用户管理系统的数据回答问题。请先在系统设置中确认大模型 API 配置，然后输入你的问题。'

const input = ref('')
const loading = ref(false)
const hasAsked = ref(false)
const isStreaming = ref(false)
const messageListRef = ref(null)
const metrics = ref({})
const modelInfo = ref({})
const suggestions = ref([...DEFAULT_SUGGESTIONS])
const messages = ref([])
const sessions = ref([])
const currentSessionId = ref('')
const searchKeyword = ref('')
const quickQuestions = [
  '当前用户总数是多少？',
  '帮我分析年龄分布',
  '哪些城市用户最多？',
  '最近日志有没有风险？'
]

let typingTimer = null
let persistTimer = null

const filteredSessions = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const ordered = [...sessions.value].sort((a, b) => Number(b.updatedAt || 0) - Number(a.updatedAt || 0))
  if (!keyword) {
    return ordered
  }
  return ordered.filter(session => {
    const title = String(session.title || '').toLowerCase()
    const content = (session.messages || [])
      .map(message => message.content || '')
      .join('\n')
      .toLowerCase()
    return title.includes(keyword) || content.includes(keyword)
  })
})

const hasModelStatus = computed(() => Boolean(modelInfo.value.provider || modelInfo.value.model))
const modelStatusText = computed(() => {
  if (isStreaming.value) return '流式输出中'
  if (!hasAsked.value) return '等待提问'
  if (!hasModelStatus.value) return '未返回模型状态'
  return modelInfo.value.fallback ? '规则兜底' : '大模型回答'
})
const modelStatusTagType = computed(() => {
  if (isStreaming.value) return 'primary'
  if (!hasAsked.value || !hasModelStatus.value) return 'info'
  return modelInfo.value.fallback ? 'warning' : 'success'
})

onMounted(() => {
  hydrateSessions()
})

onBeforeUnmount(() => {
  flushPersistCurrentSession()
})

const sendMessage = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return

  const history = buildHistory()
  appendMessage('user', text)
  updateSessionTitleFromQuestion(text)
  const assistantMessage = appendMessage('assistant', '', [])
  input.value = ''
  loading.value = true
  isStreaming.value = true
  hasAsked.value = true
  modelInfo.value = {}
  persistCurrentSession()

  try {
    const response = await fetch(`${API_BASE_URL}/api/ai/chat-stream`, {
      method: 'POST',
      headers: buildHeaders(),
      body: JSON.stringify({
        message: text,
        modelConfig: getModelConfig(),
        history
      })
    })

    if (response.status === 401) {
      removeMessage(assistantMessage.id)
      persistCurrentSession()
      handleUnauthorized()
      return
    }
    if (!response.ok || !response.body) {
      throw new Error(`AI 问答请求失败，HTTP ${response.status}`)
    }

    await readStream(response, assistantMessage)
    await waitForTypingDone(assistantMessage)

    if (!assistantMessage.content) {
      assistantMessage.content = '当前没有生成有效回答。'
    }
  } catch (error) {
    stopTyping(assistantMessage)
    const message = error?.message || 'AI 问答请求失败，请检查后端服务、登录状态和大模型接口配置'
    assistantMessage.content = assistantMessage.content || message
    ElMessage.error(message)
  } finally {
    loading.value = false
    isStreaming.value = false
    persistCurrentSession()
    scrollToBottom()
  }
}

const readStream = async (response, assistantMessage) => {
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      handleStreamLine(line, assistantMessage)
    }
  }

  buffer += decoder.decode()
  if (buffer.trim()) {
    handleStreamLine(buffer, assistantMessage)
  }
}

const handleStreamLine = (line, assistantMessage) => {
  const text = line.trim()
  if (!text) return

  let event
  try {
    event = JSON.parse(text)
  } catch (e) {
    return
  }

  if (event.type === 'meta') {
    const data = event.data || {}
    metrics.value = data.metrics || metrics.value
    suggestions.value = data.suggestions?.length ? data.suggestions : suggestions.value
    assistantMessage.sources = data.sources || []
    persistCurrentSession()
    scrollToBottom()
    return
  }

  if (event.type === 'chunk') {
    enqueueAssistantContent(assistantMessage, event.content || '')
    return
  }

  if (event.type === 'done') {
    modelInfo.value = event.model || {}
    persistCurrentSession()
    if (event.model?.fallback && event.model?.reason) {
      ElMessage.warning(`大模型调用失败，已使用规则兜底：${event.model.reason}`)
    }
    return
  }

  if (event.type === 'error') {
    const message = event.message || 'AI 流式输出异常'
    assistantMessage.content = assistantMessage.content || message
    persistCurrentSession()
    ElMessage.error(message)
  }
}

const enqueueAssistantContent = (message, content) => {
  if (!content) return
  message.pendingContent = `${message.pendingContent || ''}${content}`
  if (!message.typing) {
    drainTypingQueue(message)
  }
}

const drainTypingQueue = (message) => {
  message.typing = true

  const tick = () => {
    const pending = message.pendingContent || ''
    if (!pending) {
      message.typing = false
      typingTimer = null
      schedulePersistCurrentSession()
      return
    }

    const step = Math.min(TYPE_STEP, pending.length)
    message.content += pending.slice(0, step)
    message.pendingContent = pending.slice(step)
    schedulePersistCurrentSession()
    scrollToBottom()
    typingTimer = window.setTimeout(tick, TYPE_INTERVAL)
  }

  tick()
}

const waitForTypingDone = (message) => new Promise(resolve => {
  const check = () => {
    if (!message.typing && !message.pendingContent) {
      resolve()
      return
    }
    window.setTimeout(check, 30)
  }
  check()
})

const stopTyping = (message) => {
  if (typingTimer) {
    window.clearTimeout(typingTimer)
    typingTimer = null
  }
  if (message) {
    message.pendingContent = ''
    message.typing = false
  }
}

const askQuickQuestion = (question) => {
  input.value = question
  sendMessage()
}

const createNewSession = () => {
  if (loading.value) {
    ElMessage.warning('回答生成中，稍后再新建对话')
    return
  }
  flushPersistCurrentSession()
  const session = createSession()
  sessions.value.unshift(session)
  loadSession(session.id)
  writeSessions()
}

const switchSession = (sessionId) => {
  if (sessionId === currentSessionId.value) return
  if (loading.value) {
    ElMessage.warning('回答生成中，稍后再切换对话')
    return
  }
  flushPersistCurrentSession()
  loadSession(sessionId)
}

const deleteSession = (sessionId) => {
  if (loading.value) {
    ElMessage.warning('回答生成中，稍后再删除记录')
    return
  }
  const nextSessions = sessions.value.filter(session => session.id !== sessionId)
  if (nextSessions.length === 0) {
    const session = createSession()
    sessions.value = [session]
    loadSession(session.id)
    writeSessions()
    return
  }

  sessions.value = nextSessions
  if (sessionId === currentSessionId.value) {
    loadSession(nextSessions[0].id)
  }
  writeSessions()
}

const resetChat = () => {
  if (loading.value) {
    ElMessage.warning('回答生成中，稍后再清空对话')
    return
  }
  stopTyping()
  messages.value = [
    createMessage('assistant', '对话已清空。你可以继续询问用户结构、角色配置或日志风险。')
  ]
  suggestions.value = [...DEFAULT_SUGGESTIONS]
  modelInfo.value = {}
  metrics.value = {}
  hasAsked.value = false
  isStreaming.value = false
  updateCurrentSession({
    title: '新对话',
    messages: sanitizeMessages(messages.value),
    suggestions: [...suggestions.value],
    modelInfo: {},
    metrics: {},
    updatedAt: Date.now()
  })
  writeSessions()
}

const hydrateSessions = () => {
  const storedSessions = readSessions()
  if (storedSessions.length === 0) {
    const session = createSession()
    sessions.value = [session]
    loadSession(session.id)
    writeSessions()
    return
  }

  sessions.value = storedSessions
  const storedCurrentId = localStorage.getItem(CURRENT_SESSION_KEY)
  const target = sessions.value.find(session => session.id === storedCurrentId) || sessions.value[0]
  loadSession(target.id)
}

const loadSession = (sessionId) => {
  const session = sessions.value.find(item => item.id === sessionId)
  if (!session) return

  currentSessionId.value = session.id
  messages.value = sanitizeMessages(session.messages?.length ? session.messages : [createMessage('assistant', WELCOME_MESSAGE)])
  metrics.value = { ...(session.metrics || {}) }
  modelInfo.value = { ...(session.modelInfo || {}) }
  suggestions.value = session.suggestions?.length ? [...session.suggestions] : [...DEFAULT_SUGGESTIONS]
  hasAsked.value = messages.value.some(message => message.role === 'user')
  isStreaming.value = false
  localStorage.setItem(CURRENT_SESSION_KEY, session.id)
  scrollToBottom()
}

const createSession = () => {
  const now = Date.now()
  return {
    id: `ai-${now}-${Math.random().toString(16).slice(2)}`,
    title: '新对话',
    createdAt: now,
    updatedAt: now,
    messages: [createMessage('assistant', WELCOME_MESSAGE)],
    metrics: {},
    modelInfo: {},
    suggestions: [...DEFAULT_SUGGESTIONS]
  }
}

const createMessage = (role, content, sources = []) => ({
  id: Date.now() + Math.random(),
  role,
  content,
  pendingContent: '',
  typing: false,
  sources
})

const updateSessionTitleFromQuestion = (question) => {
  const session = sessions.value.find(item => item.id === currentSessionId.value)
  if (!session || session.title !== '新对话') return

  updateCurrentSession({
    title: buildSessionTitle(question)
  })
}

const buildSessionTitle = (text) => {
  const normalized = text.replace(/\s+/g, ' ').trim()
  return normalized.length > 18 ? `${normalized.slice(0, 18)}...` : normalized || '新对话'
}

const persistCurrentSession = () => {
  if (!currentSessionId.value) return

  updateCurrentSession({
    messages: sanitizeMessages(messages.value),
    metrics: { ...metrics.value },
    modelInfo: { ...modelInfo.value },
    suggestions: [...suggestions.value],
    updatedAt: Date.now()
  })
  writeSessions()
}

const schedulePersistCurrentSession = () => {
  if (persistTimer) {
    window.clearTimeout(persistTimer)
  }
  persistTimer = window.setTimeout(() => {
    persistTimer = null
    persistCurrentSession()
  }, 180)
}

const flushPersistCurrentSession = () => {
  if (persistTimer) {
    window.clearTimeout(persistTimer)
    persistTimer = null
  }
  persistCurrentSession()
}

const updateCurrentSession = (patch) => {
  const index = sessions.value.findIndex(session => session.id === currentSessionId.value)
  if (index === -1) return

  sessions.value.splice(index, 1, {
    ...sessions.value[index],
    ...patch
  })
}

const readSessions = () => {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    return Array.isArray(parsed)
      ? parsed.map(normalizeSession).filter(Boolean)
      : []
  } catch (e) {
    return []
  }
}

const writeSessions = () => {
  const payload = sessions.value.map(normalizeSession).filter(Boolean)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
  if (currentSessionId.value) {
    localStorage.setItem(CURRENT_SESSION_KEY, currentSessionId.value)
  }
}

const normalizeSession = (session) => {
  if (!session || !session.id) return null
  return {
    id: session.id,
    title: session.title || '新对话',
    createdAt: Number(session.createdAt || Date.now()),
    updatedAt: Number(session.updatedAt || session.createdAt || Date.now()),
    messages: sanitizeMessages(session.messages || []),
    metrics: session.metrics || {},
    modelInfo: session.modelInfo || {},
    suggestions: Array.isArray(session.suggestions) && session.suggestions.length
      ? session.suggestions
      : [...DEFAULT_SUGGESTIONS]
  }
}

const sanitizeMessages = (items) => (items || []).map(item => ({
  id: item.id || Date.now() + Math.random(),
  role: item.role === 'user' ? 'user' : 'assistant',
  content: item.content || '',
  pendingContent: '',
  typing: false,
  sources: Array.isArray(item.sources) ? item.sources : []
}))

const removeMessage = (id) => {
  messages.value = messages.value.filter(item => item.id !== id)
}

const countSessionMessages = (session) => (session.messages || [])
  .filter(message => message.role === 'user' || (message.role === 'assistant' && message.content !== WELCOME_MESSAGE))
  .length

const formatSessionTime = (value) => {
  const date = new Date(Number(value || Date.now()))
  const now = new Date()
  const pad = number => String(number).padStart(2, '0')
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`
  if (date.toDateString() === now.toDateString()) {
    return `今天 ${time}`
  }
  if (date.getFullYear() === now.getFullYear()) {
    return `${date.getMonth() + 1}月${date.getDate()}日 ${time}`
  }
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()} ${time}`
}

const buildHeaders = () => {
  const headers = {
    'Content-Type': 'application/json'
  }
  const token = localStorage.getItem('token')
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  return headers
}

const buildHistory = () => messages.value
  .filter(item => item.content)
  .slice(-8)
  .map(item => ({
    role: item.role,
    content: item.content
  }))

const getModelConfig = () => {
  let settings = {}
  try {
    settings = JSON.parse(localStorage.getItem('appSettings') || '{}')
  } catch (e) {
    settings = {}
  }

  return {
    provider: settings.aiProvider || 'openai-compatible',
    baseUrl: settings.aiBaseUrl || '',
    apiKey: settings.aiApiKey || '',
    model: settings.aiModel || ''
  }
}

const appendMessage = (role, content, sources = []) => {
  const message = createMessage(role, content, sources)
  messages.value.push(message)
  schedulePersistCurrentSession()
  scrollToBottom()
  return message
}

const renderMarkdown = (content) => {
  const normalized = normalizeMarkdown(content)
  const lines = normalized.split('\n')
  let html = ''
  let inList = false

  lines.forEach(line => {
    const trimmed = line.trim()
    if (!trimmed) {
      if (inList) {
        html += '</ul>'
        inList = false
      }
      return
    }

    const bullet = trimmed.match(/^[-*]\s+(.+)$/)
    if (bullet) {
      if (!inList) {
        html += '<ul>'
        inList = true
      }
      html += `<li>${formatInlineMarkdown(escapeHtml(bullet[1]))}</li>`
      return
    }

    if (inList) {
      html += '</ul>'
      inList = false
    }
    html += `<p>${formatInlineMarkdown(escapeHtml(trimmed))}</p>`
  })

  if (inList) {
    html += '</ul>'
  }
  return html
}

const normalizeMarkdown = (content) => content
  .replace(/\r\n/g, '\n')
  .replace(/([：:])\s*-\s*/g, '$1\n- ')
  .replace(/([。！？])\s*-\s*/g, '$1\n- ')
  .replace(/([\u4e00-\u9fa5A-Za-z0-9）)])-\s*([\u4e00-\u9fa5])/g, '$1\n- $2')
  .replace(/\n{3,}/g, '\n\n')

const formatInlineMarkdown = (escapedText) => escapedText
  .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')

const escapeHtml = (content) => String(content)
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#39;')

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const handleUnauthorized = () => {
  ElMessage.error('登录已过期，请重新登录')
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  setTimeout(() => {
    window.location.href = '/login'
  }, 1000)
}
</script>

<style scoped>
.ai-chat-page {
  height: 100%;
  min-height: 100vh;
  padding: 22px;
  background: #f5f7fb;
  color: #1f2937;
}

.page-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.toolbar-title {
  min-width: 0;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
}

.page-subtitle {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

.chat-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  height: calc(100vh - 104px);
}

.chat-panel,
.insight-panel {
  min-height: 0;
  border: 1px solid #d9e2ef;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
}

.chat-panel {
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 20px;
  border-bottom: 1px solid #e6ebf2;
}

.section-title {
  color: #111827;
  font-size: 15px;
  font-weight: 700;
}

.section-desc {
  margin-top: 4px;
  color: #667085;
  font-size: 12px;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
  background: #f8fafc;
}

.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 34px;
  border-radius: 8px;
  background: #e8eef8;
  color: #315c9f;
}

.message-row.user .message-avatar {
  background: #e7f6ee;
  color: #16794c;
}

.message-bubble {
  max-width: min(720px, 78%);
  padding: 12px 14px;
  border: 1px solid #dce5f0;
  border-radius: 8px;
  background: #ffffff;
}

.message-row.user .message-bubble {
  border-color: #ccebd9;
  background: #f1fbf5;
}

.message-meta {
  margin-bottom: 6px;
  color: #667085;
  font-size: 12px;
  font-weight: 600;
}

.message-content {
  color: #1f2937;
  line-height: 1.72;
  word-break: break-word;
}

.rendered-markdown :deep(p) {
  margin: 0 0 8px;
}

.rendered-markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.rendered-markdown :deep(strong) {
  color: #111827;
  font-weight: 700;
}

.rendered-markdown :deep(ul) {
  margin: 6px 0 10px;
  padding-left: 20px;
}

.rendered-markdown :deep(li) {
  margin: 4px 0;
}

.typing-text {
  color: #667085;
}

.source-list {
  display: flex;
  gap: 6px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.quick-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 12px 20px;
  border-top: 1px solid #e6ebf2;
}

.quick-button,
.suggestion-item {
  border: 1px solid #d9e2ef;
  border-radius: 6px;
  background: #ffffff;
  color: #344054;
  cursor: pointer;
  font: inherit;
}

.quick-button {
  padding: 7px 10px;
  font-size: 13px;
}

.quick-button:hover,
.suggestion-item:hover {
  border-color: #3f73c3;
  color: #2457a3;
  background: #f2f7ff;
}

.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 92px;
  gap: 12px;
  padding: 16px 20px 20px;
  border-top: 1px solid #e6ebf2;
}

.composer .el-button {
  height: 76px;
}

.insight-panel {
  padding: 18px;
  overflow-y: auto;
}

.history-panel {
  padding-bottom: 18px;
  border-bottom: 1px solid #e6ebf2;
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 240px;
  margin-top: 12px;
  overflow-y: auto;
}

.session-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 58px;
  padding: 9px 8px 9px 10px;
  border: 1px solid #d9e2ef;
  border-radius: 8px;
  background: #ffffff;
  color: #344054;
  cursor: pointer;
  text-align: left;
}

.session-item:hover,
.session-item.active {
  border-color: #3f73c3;
  background: #f2f7ff;
}

.session-main {
  min-width: 0;
  display: block;
}

.session-title {
  display: block;
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  display: block;
  margin-top: 5px;
  color: #667085;
  font-size: 12px;
}

.delete-session {
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
}

.session-empty {
  padding: 16px 10px;
  color: #667085;
  font-size: 13px;
  text-align: center;
}

.model-box {
  margin-top: 12px;
  padding: 14px;
  border: 1px solid #d9e2ef;
  border-radius: 8px;
  background: #f8fafc;
}

.model-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: #667085;
  font-size: 12px;
}

.model-row strong {
  max-width: 170px;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-tag {
  margin-top: 4px;
}

.model-reason {
  margin-top: 10px;
  color: #9a3412;
  font-size: 12px;
  line-height: 1.6;
  word-break: break-word;
}

.with-gap {
  margin-top: 18px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0 22px;
}

.metric-card {
  padding: 14px;
  border: 1px solid #d9e2ef;
  border-radius: 8px;
  background: #f8fafc;
}

.metric-card span {
  display: block;
  color: #667085;
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  color: #111827;
  font-size: 24px;
}

.suggestion-panel,
.guard-panel {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #e6ebf2;
}

.suggestion-item {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  padding: 10px 12px;
  text-align: left;
}

.guard-panel p {
  margin-top: 10px;
  color: #667085;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .chat-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .chat-panel {
    min-height: 680px;
  }
}

@media (max-width: 640px) {
  .ai-chat-page {
    padding: 14px;
  }

  .page-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .message-bubble {
    max-width: 84%;
  }

  .composer {
    grid-template-columns: 1fr;
  }

  .composer .el-button {
    height: 42px;
  }
}
</style>
