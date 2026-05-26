<template>
  <div style="height: 100%" :class="{ 'compact-mode': appSettings.compactMode }">
    <el-container style="height: 100%;">
      <el-aside width="220px" class="modern-aside">
        <div class="logo-section">
          <el-icon :size="32" color="#fff"><Menu /></el-icon>
          <span class="logo-text">后台管理系统</span>
        </div>
        <el-menu
            :default-active="route.path"
            style="height: calc(100vh - 70px)"
            background-color="transparent"
            router
            class="modern-menu"
        >
          <el-menu-item v-for="menu in visibleMenus" :key="menu.path" :index="menu.path">
            <el-icon><component :is="menuIconMap[menu.icon] || User" /></el-icon>
            <span>{{ menu.name }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="modern-header">
          <div class="header-left">
            <el-icon :size="22" class="breadcrumb-icon"><Location /></el-icon>
            <div>
              <span class="breadcrumb-text">{{ currentRouteName }}</span>
              <span class="breadcrumb-subtitle">人员、角色与操作记录</span>
            </div>
          </div>
          <div class="header-right">
            <el-popover
              placement="bottom-end"
              width="340"
              trigger="click"
              popper-class="notification-popover"
            >
              <template #reference>
                <button class="notification-button" type="button">
                  <el-badge :value="unreadCount" :hidden="!appSettings.notifications || unreadCount === 0" class="notification-badge">
                    <el-icon :size="20" class="notification-icon"><Bell /></el-icon>
                  </el-badge>
                </button>
              </template>
              <div class="notification-panel">
                <div class="notification-head">
                  <div>
                    <div class="notification-title">消息中心</div>
                    <div class="notification-meta">{{ unreadCount }} 条未读提醒</div>
                  </div>
                  <el-button link type="primary" :disabled="unreadCount === 0" @click="markAllRead">
                    全部已读
                  </el-button>
                </div>
                <div class="notification-list">
                  <button
                    v-for="item in notifications"
                    :key="item.id"
                    type="button"
                    class="notification-item"
                    :class="{ unread: !item.read }"
                    @click="markNotificationRead(item)"
                  >
                    <span class="notification-dot"></span>
                    <span class="notification-content">
                      <span class="notification-item-title">{{ item.title }}</span>
                      <span class="notification-item-desc">{{ item.desc }}</span>
                    </span>
                    <span class="notification-time">{{ item.time }}</span>
                  </button>
                </div>
              </div>
            </el-popover>
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="36" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
                <span class="username">{{ currentUser?.nickname || currentUser?.username || '管理员' }}</span>
                <el-icon :size="16" class="arrow-icon"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon> 个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="settings">
                    <el-icon><Setting /></el-icon> 系统设置
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon> 退出系统
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main>
          <el-card class="main-card">
            <div class="toolbar-section">
              <div class="search-box">
                <el-input
                    v-model="search"
                    placeholder="搜索用户名或昵称..."
                    style="width: 320px;"
                    clearable
                    @clear="load"
                    class="search-input"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-button type="primary" @click="load" class="action-btn">
                  <el-icon><Search /></el-icon> 搜索
                </el-button>
              </div>
              <div class="button-group">
                <el-button v-if="hasPermission('btn:user:add')" type="success" @click="add" class="action-btn">
                  <el-icon><Plus /></el-icon> 新增用户
                </el-button>
                <el-button v-if="hasPermission('btn:user:export')" type="warning" @click="exportData" class="action-btn">
                  <el-icon><Download /></el-icon> 导出 Excel
                </el-button>
                <el-button v-if="hasPermission('btn:user:import')" type="info" @click="importDialogVisible = true" class="action-btn">
                  <el-icon><Upload /></el-icon> 导入 Excel
                </el-button>
                <el-button
                  v-if="hasPermission('btn:user:delete')"
                  type="danger"
                  @click="batchDelete"
                  :disabled="selectedIds.length === 0"
                  class="action-btn batch-btn"
                >
                  <el-icon><Delete /></el-icon> 批量删除 ({{ selectedIds.length }})
                </el-button>
              </div>
            </div>

            <el-table
              :data="tableData"
              border
              :stripe="appSettings.tableStripe"
              style="width: 100%"
              @selection-change="handleSelectionChange"
              class="modern-table"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column prop="id" label="ID" width="80" sortable />
              <el-table-column prop="username" label="用户名" width="140" />
              <el-table-column prop="nickname" label="昵称" width="140" />
              <el-table-column prop="age" label="年龄" width="80" sortable />
              <el-table-column prop="sex" label="性别" width="80" />
              <el-table-column prop="address" label="地址" show-overflow-tooltip />
              <el-table-column label="操作" width="280" fixed="right" header-align="center">
                <template #default="scope">
                  <el-button
                    type="info"
                    size="small"
                    @click="handleView(scope.row)"
                    class="table-btn"
                  >
                    <el-icon><View /></el-icon> 查看
                  </el-button>
                  <el-button
                    type="primary"
                    size="small"
                    @click="handleEdit(scope.row)"
                    class="table-btn"
                    v-if="hasPermission('btn:user:edit')"
                  >
                    <el-icon><Edit /></el-icon> 编辑
                  </el-button>
                  <el-button
                    v-if="hasPermission('btn:user:delete')"
                    type="danger"
                    size="small"
                    @click="handleDelete(scope.row.id)"
                    class="table-btn"
                  >
                    <el-icon><Delete /></el-icon> 删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-section">
              <el-pagination
                  v-model:current-page="currentPage"
                  v-model:page-size="pageSize"
                  :page-sizes="[10, 20, 50, 100]"
                  layout="total, sizes, prev, pager, next, jumper"
                  :total="total"
                  @size-change="handleSizeChange"
                  @current-change="handleCurrentChange"
                  class="modern-pagination"
              />
            </div>
          </el-card>
        </el-main>
      </el-container>
    </el-container>

    <el-dialog
        v-model="dialogVisible"
        :title="form.id ? '编辑用户' : '新增用户'"
        width="550px"
        @close="resetForm"
        class="modern-dialog"
    >
      <el-form :model="form" label-width="90px" :rules="rules" ref="formRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="1" :max="150" style="width: 100%" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="form.sex">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="3" placeholder="请输入地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
        v-model="viewDialogVisible"
        title="用户详情"
        width="550px"
        class="modern-dialog"
    >
      <el-descriptions :column="1" border class="modern-descriptions">
        <el-descriptions-item label="ID">
          <el-tag type="primary">{{ viewUser.id }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="用户名">{{ viewUser.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ viewUser.nickname }}</el-descriptions-item>
        <el-descriptions-item label="年龄">
          <el-tag type="success">{{ viewUser.age }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          <el-tag :type="viewUser.sex === '男' ? 'primary' : 'danger'">{{ viewUser.sex }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="地址">{{ viewUser.address }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
        v-model="importDialogVisible"
        title="导入 Excel"
        width="550px"
        class="modern-dialog"
    >
      <el-upload
          ref="uploadRef"
          drag
          action="#"
          :auto-upload="false"
          :limit="1"
          accept=".xlsx, .xls"
          :on-change="handleFileChange"
          class="modern-upload"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="upload-tip">
            <el-icon><InfoFilled /></el-icon> 只能上传 xlsx/xls 文件
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing">
          {{ importing ? '导入中...' : '开始导入' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
        v-model="profileDialogVisible"
        title="个人中心"
        width="620px"
        class="modern-dialog"
    >
      <div class="profile-layout">
        <div class="profile-card">
          <el-avatar :size="72" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
          <div class="profile-name">{{ profileForm.nickname || profileForm.username || '管理员' }}</div>
          <div class="profile-role">系统管理员</div>
          <div class="profile-meta">
            <span>ID {{ profileForm.id || '-' }}</span>
            <span>{{ profileForm.sex || '未设置' }}</span>
          </div>
        </div>
        <el-form :model="profileForm" label-width="86px" class="profile-form">
          <el-form-item label="用户名">
            <el-input v-model="profileForm.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="年龄">
            <el-input-number v-model="profileForm.age" :min="1" :max="150" style="width: 100%" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="profileForm.sex">
              <el-radio label="男">男</el-radio>
              <el-radio label="女">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="地址">
            <el-input v-model="profileForm.address" type="textarea" :rows="3" placeholder="请输入地址" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存资料</el-button>
      </template>
    </el-dialog>

    <el-dialog
        v-model="settingsDialogVisible"
        title="系统设置"
        width="620px"
        class="modern-dialog"
    >
      <div class="settings-list">
        <div class="setting-row">
          <div>
            <div class="setting-title">紧凑模式</div>
            <div class="setting-desc">减小工具栏、表格和页脚间距，适合高频录入。</div>
          </div>
          <el-switch v-model="appSettings.compactMode" @change="saveSettings" />
        </div>
        <div class="setting-row">
          <div>
            <div class="setting-title">表格斑马纹</div>
            <div class="setting-desc">在数据较多时增强横向阅读定位。</div>
          </div>
          <el-switch v-model="appSettings.tableStripe" @change="saveSettings" />
        </div>
        <div class="setting-row">
          <div>
            <div class="setting-title">消息提醒</div>
            <div class="setting-desc">关闭后隐藏顶部消息红点，但保留消息中心内容。</div>
          </div>
          <el-switch v-model="appSettings.notifications" @change="saveSettings" />
        </div>
      </div>
      <template #footer>
        <el-button @click="resetSettings">恢复默认</el-button>
        <el-button type="primary" @click="settingsDialogVisible = false">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, DataAnalysis, Document, UserFilled, Menu, Location, Bell,
  ArrowDown, Setting, SwitchButton, Search, Plus, Download, Upload,
  Delete, View, Edit, UploadFilled, InfoFilled
} from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const defaultMenus = [
  { name: '用户管理', code: 'menu:user', path: '/', icon: 'User', sort: 10 },
  { name: '仪表盘', code: 'menu:dashboard', path: '/dashboard', icon: 'DataAnalysis', sort: 20 },
  { name: '角色管理', code: 'menu:role', path: '/roles', icon: 'UserFilled', sort: 30 },
  { name: '操作日志', code: 'menu:log', path: '/logs', icon: 'Document', sort: 40 }
]
const menuIconMap = {
  User,
  DataAnalysis,
  UserFilled,
  Document
}
const tableData = ref([])
const search = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const importDialogVisible = ref(false)
const profileDialogVisible = ref(false)
const settingsDialogVisible = ref(false)
const formRef = ref(null)
const uploadRef = ref(null)
const currentUser = ref(null)
const importing = ref(false)
const selectedFile = ref(null)
const viewUser = ref({})
const profileForm = ref({})
const defaultSettings = {
  compactMode: false,
  tableStripe: true,
  notifications: true
}
const appSettings = ref({ ...defaultSettings })
const notifications = ref([
  { id: 1, title: '用户资料已更新', desc: 'admin 的基础资料刚刚完成同步', time: '刚刚', read: false },
  { id: 2, title: 'Excel 导入可用', desc: '当前支持 xlsx/xls 用户批量导入', time: '今天', read: false },
  { id: 3, title: '数据库已连接', desc: 'user_management 数据库连接正常', time: '今天', read: false },
  { id: 4, title: '操作日志记录中', desc: '带 @Log 的接口会自动写入日志', time: '今天', read: false },
  { id: 5, title: 'Redis 未启动时已降级', desc: '登录限流不会阻断本地开发检查', time: '今天', read: false }
])

const currentRouteName = computed(() => {
  const routeMap = {
    '/': '用户管理',
    '/dashboard': '数据仪表盘',
    '/roles': '角色管理',
    '/logs': '操作日志'
  }
  return routeMap[route.path] || '用户管理'
})

const unreadCount = computed(() => notifications.value.filter(item => !item.read).length)
const visibleMenus = computed(() => {
  const menus = currentUser.value?.menus
  return Array.isArray(menus) && menus.length > 0 ? menus : defaultMenus
})

const hasPermission = (code) => {
  const permissions = currentUser.value?.permissions
  return !Array.isArray(permissions) || permissions.length === 0 || permissions.includes(code)
}

const form = ref({
  username: '',
  nickname: '',
  age: null,
  sex: '',
  address: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const load = () => {
  request.get('/api/user', {
    params: {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      search: search.value
    }
  }).then(res => {
    if (res.code === '0') {
      tableData.value = res.data.records
      total.value = res.data.total
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  })
}

const add = () => {
  form.value = {}
  dialogVisible.value = true
}

const save = () => {
  if (!formRef.value) return

  formRef.value.validate(valid => {
    if (valid) {
      const url = form.value.id ? '/api/user' : '/api/user'
      const method = form.value.id ? 'put' : 'post'

      request({
        url,
        method,
        data: form.value
      }).then(res => {
        if (res.code === '0') {
          ElMessage.success(form.value.id ? '更新成功' : '保存成功')
          dialogVisible.value = false
          load()
        } else {
          ElMessage.error(res.msg || '操作失败')
        }
      })
    }
  })
}

const handleEdit = (row) => {
  form.value = JSON.parse(JSON.stringify(row))
  dialogVisible.value = true
}

const handleView = (row) => {
  viewUser.value = JSON.parse(JSON.stringify(row))
  viewDialogVisible.value = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.delete(`/api/user/${id}`).then(res => {
      if (res.code === '0') {
        ElMessage.success('删除成功')
        load()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    })
  })
}

const handleCurrentChange = (pageNum) => {
  currentPage.value = pageNum
  load()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  load()
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出系统吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      request.post('/api/user/logout').then(res => {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        ElMessage.success('已退出登录')
        window.location.href = '/login'
      })
    })
  } else if (command === 'profile') {
    openProfile()
  } else if (command === 'settings') {
    settingsDialogVisible.value = true
  }
}

const openProfile = () => {
  profileForm.value = {
    id: currentUser.value?.id,
    username: currentUser.value?.username || '',
    nickname: currentUser.value?.nickname || '',
    age: currentUser.value?.age || null,
    sex: currentUser.value?.sex || '',
    address: currentUser.value?.address || ''
  }
  profileDialogVisible.value = true
}

const saveProfile = () => {
  const updatedUser = {
    ...(currentUser.value || {}),
    ...profileForm.value
  }
  delete updatedUser.password
  request.put('/api/user', updatedUser).then(res => {
    if (res.code === '0') {
      currentUser.value = updatedUser
      localStorage.setItem('userInfo', JSON.stringify(updatedUser))
      profileDialogVisible.value = false
      ElMessage.success('个人资料已保存')
      load()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  })
}

const loadSettings = () => {
  const saved = localStorage.getItem('appSettings')
  if (!saved) return
  try {
    appSettings.value = {
      ...defaultSettings,
      ...JSON.parse(saved)
    }
  } catch (e) {
    appSettings.value = { ...defaultSettings }
  }
}

const saveSettings = () => {
  localStorage.setItem('appSettings', JSON.stringify(appSettings.value))
}

const resetSettings = () => {
  appSettings.value = { ...defaultSettings }
  saveSettings()
  ElMessage.success('系统设置已恢复默认')
}

const markNotificationRead = (item) => {
  item.read = true
}

const markAllRead = () => {
  notifications.value.forEach(item => {
    item.read = true
  })
}

const selectedIds = ref([])

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

const batchDelete = () => {
  ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 个用户吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.delete('/api/user/batch', {
      data: selectedIds.value
    }).then(res => {
      if (res.code === '0') {
        ElMessage.success('批量删除成功')
        load()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    })
  })
}

const exportData = () => {
  request.get('/api/user/export', {
    responseType: 'blob'
  }).then(blob => {
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '用户列表.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  })
}

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleImport = () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  importing.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)

  request.post('/api/user/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }).then(res => {
    if (res.code === '0') {
      ElMessage.success('导入成功')
      importDialogVisible.value = false
      selectedFile.value = null
      uploadRef.value?.clearFiles()
      load()
    } else {
      ElMessage.error(res.msg || '导入失败')
    }
  }).catch(() => {
    ElMessage.error('导入失败')
  }).finally(() => {
    importing.value = false
  })
}

onMounted(() => {
  loadSettings()
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    currentUser.value = JSON.parse(userInfo)
  }
  load()
})
</script>

<style scoped>
.modern-aside {
  background: #18372b;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 8px 0 26px rgba(24, 55, 43, 0.16);
}

.logo-section {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  padding: 0 22px;
  background: #153126;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  font-size: 17px;
  font-weight: 700;
  color: #fbf6ea;
  letter-spacing: 0;
}

.modern-menu .el-menu-item {
  color: rgba(251, 246, 234, 0.78);
  font-size: 14px;
  border-radius: 6px !important;
  margin: 5px 14px !important;
  width: calc(100% - 28px) !important;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.modern-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #fff7e8 !important;
}

.modern-menu .el-menu-item.is-active {
  background: #f2dfc6 !important;
  color: #18372b !important;
  box-shadow: none;
  font-weight: 700;
}

.modern-header {
  background: rgba(255, 253, 247, 0.94);
  border-bottom: 1px solid #ded6c8;
  box-shadow: none;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 70px;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.breadcrumb-icon {
  color: #1f4d3a;
}

.breadcrumb-text {
  display: block;
  font-size: 17px;
  font-weight: 700;
  color: #252923;
}

.breadcrumb-subtitle {
  display: block;
  margin-top: 3px;
  font-size: 12px;
  color: #777267;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.notification-button {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #ded6c8;
  border-radius: 8px;
  background: #fffaf0;
  cursor: pointer;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.notification-button:hover {
  border-color: #b66a2c;
  background: #f8efe3;
}

.notification-badge {
  line-height: 1;
}

.notification-icon {
  color: #3d433a;
}

.notification-panel {
  background: #fffdf7;
  border-radius: 8px;
  overflow: hidden;
}

.notification-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #e6ded0;
}

.notification-title {
  font-size: 15px;
  font-weight: 700;
  color: #252923;
}

.notification-meta {
  margin-top: 3px;
  font-size: 12px;
  color: #777267;
}

.notification-list {
  max-height: 320px;
  overflow-y: auto;
}

.notification-item {
  width: 100%;
  display: grid;
  grid-template-columns: 10px 1fr auto;
  gap: 10px;
  padding: 13px 16px;
  border: 0;
  border-bottom: 1px solid #f0e8dc;
  background: #fffdf7;
  text-align: left;
  cursor: pointer;
}

.notification-item:hover {
  background: #f8f2e8;
}

.notification-item.unread {
  background: #f6efe0;
}

.notification-dot {
  width: 7px;
  height: 7px;
  margin-top: 7px;
  border-radius: 999px;
  background: transparent;
}

.notification-item.unread .notification-dot {
  background: #b66a2c;
}

.notification-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.notification-item-title {
  color: #252923;
  font-size: 13px;
  font-weight: 700;
}

.notification-item-desc {
  color: #6f756d;
  font-size: 12px;
  line-height: 1.45;
}

.notification-time {
  color: #9a9387;
  font-size: 12px;
  white-space: nowrap;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 10px 6px 6px;
  border: 1px solid #ded6c8;
  border-radius: 8px;
  background: #fffaf0;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.user-info:hover {
  border-color: #b66a2c;
  background: #f8efe3;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #252923;
}

.arrow-icon {
  color: #777267;
}

.main-card {
  border-radius: 8px;
  box-shadow: 0 12px 28px rgba(41, 35, 24, 0.08);
}

.toolbar-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  padding: 16px;
  background: #f6efe4;
  border: 1px solid #e3dacb;
  border-radius: 8px;
}

.search-box {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 6px;
}

.button-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.action-btn {
  border-radius: 6px;
  font-weight: 600;
  padding: 9px 14px;
}

.batch-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modern-table {
  border-radius: 8px;
  overflow: hidden;
}

.table-btn {
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 13px;
  margin: 0 2px 0 0;
}

.pagination-section {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.modern-pagination :deep(.el-pagination) {
  padding: 16px 0;
}

.modern-dialog :deep(.el-dialog) {
  border-radius: 8px;
  overflow: hidden;
}

.modern-descriptions :deep(.el-descriptions__label) {
  font-weight: 600;
  width: 100px;
}

.modern-upload :deep(.el-upload-dragger) {
  padding: 40px 20px;
  border-radius: 8px;
  background: #fbf8f1;
}

.upload-icon {
  font-size: 60px;
  color: #1f4d3a;
  margin-bottom: 16px;
}

.upload-text {
  color: #6f756d;
  font-size: 15px;
}

.upload-text em {
  color: #1f4d3a;
  font-style: normal;
  font-weight: 600;
}

.upload-tip {
  color: #777267;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  margin-top: 12px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 190px 1fr;
  gap: 22px;
}

.profile-card {
  display: flex;
  align-items: center;
  flex-direction: column;
  padding: 22px 16px;
  border: 1px solid #e3dacb;
  border-radius: 8px;
  background: #f8f2e8;
}

.profile-name {
  margin-top: 14px;
  font-size: 17px;
  font-weight: 700;
  color: #252923;
}

.profile-role {
  margin-top: 6px;
  padding: 4px 8px;
  border-radius: 999px;
  background: #e4eee7;
  color: #1f4d3a;
  font-size: 12px;
  font-weight: 700;
}

.profile-meta {
  display: flex;
  gap: 8px;
  margin-top: 18px;
  color: #777267;
  font-size: 12px;
}

.profile-form {
  min-width: 0;
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px;
  border: 1px solid #e3dacb;
  border-radius: 8px;
  background: #fffaf0;
}

.setting-title {
  font-size: 14px;
  font-weight: 700;
  color: #252923;
}

.setting-desc {
  margin-top: 5px;
  color: #6f756d;
  font-size: 12px;
  line-height: 1.5;
}

.compact-mode :deep(.el-main) {
  padding: 14px;
}

.compact-mode .toolbar-section {
  margin-bottom: 12px;
  padding: 12px;
}

.compact-mode .action-btn {
  padding: 7px 12px;
}

.compact-mode .pagination-section {
  margin-top: 12px;
}

@media (max-width: 1100px) {
  .toolbar-section {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box,
  .button-group {
    width: 100%;
  }

  .search-input {
    flex: 1;
  }
}

@media (max-width: 760px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}
</style>
