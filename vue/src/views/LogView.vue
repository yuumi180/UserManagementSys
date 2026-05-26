<template>
  <div style="padding: 20px;">
    <!-- 返回按钮 -->
    <div style="margin-bottom: 20px;">
      <el-button @click="$router.push('/')" style="margin-bottom: 10px;">
        <el-icon style="vertical-align: middle; margin-right: 5px;">
          <Back />
        </el-icon>
        返回用户管理
      </el-button>
    </div>

    <el-card class="ai-risk-card" v-if="riskAnalysis">
      <div class="risk-head">
        <div>
          <div class="risk-title">AI 日志风险分析</div>
          <div class="risk-summary">{{ riskAnalysis.summary }}</div>
        </div>
        <el-tag :type="riskTagType" size="large">风险等级：{{ riskAnalysis.riskLevel }}</el-tag>
      </div>
      <el-row :gutter="12" class="risk-metrics">
        <el-col :span="6">
          <div class="risk-metric"><span>风险分</span><strong>{{ riskAnalysis.score }}</strong></div>
        </el-col>
        <el-col :span="6">
          <div class="risk-metric"><span>日志数</span><strong>{{ riskAnalysis.totalLogs }}</strong></div>
        </el-col>
        <el-col :span="6">
          <div class="risk-metric"><span>删除操作</span><strong>{{ riskAnalysis.deleteCount }}</strong></div>
        </el-col>
        <el-col :span="6">
          <div class="risk-metric"><span>失败请求</span><strong>{{ riskAnalysis.failedCount }}</strong></div>
        </el-col>
      </el-row>
      <el-row :gutter="18">
        <el-col :span="12">
          <div class="risk-section-title">风险点</div>
          <ul class="risk-list">
            <li v-for="item in riskAnalysis.risks" :key="item">{{ item }}</li>
          </ul>
        </el-col>
        <el-col :span="12">
          <div class="risk-section-title">处置建议</div>
          <ul class="risk-list">
            <li v-for="item in riskAnalysis.suggestions" :key="item">{{ item }}</li>
          </ul>
        </el-col>
      </el-row>
      <div v-if="riskAnalysis.frequentUrls?.length" class="risk-section-title">高频接口</div>
      <div v-if="riskAnalysis.frequentUrls?.length" class="url-list">
        <div v-for="item in riskAnalysis.frequentUrls" :key="item.url" class="url-item">
          <span>{{ item.url }}</span>
          <strong>{{ item.count }} 次</strong>
        </div>
      </div>
    </el-card>

    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div style="font-weight: bold;">操作日志</div>
          <el-button v-if="hasPermission('btn:ai:log')" type="success" @click="loadRiskAnalysis" :loading="riskLoading">AI 风险分析</el-button>
        </div>
      </template>

      <el-table :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="operation" label="操作" width="150" />
        <el-table-column prop="method" label="请求方法" width="100" />
        <el-table-column prop="url" label="请求 URL" show-overflow-tooltip />
        <el-table-column prop="createTime" label="操作时间" width="180" />
      </el-table>

      <div style="margin-top: 20px; text-align: right;">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { Back } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const riskLoading = ref(false)
const riskAnalysis = ref(null)

const riskTagType = computed(() => {
  if (!riskAnalysis.value) return 'success'
  if (riskAnalysis.value.riskLevel === '高') return 'danger'
  if (riskAnalysis.value.riskLevel === '中') return 'warning'
  return 'success'
})

const hasPermission = (code) => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  const permissions = userInfo.permissions
  return !Array.isArray(permissions) || permissions.length === 0 || permissions.includes(code)
}

const load = () => {
  request.get('/api/log', {
    params: {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
  }).then(res => {
    if (res.code === '0') {
      tableData.value = res.data.records
      total.value = res.data.total
    }
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

const loadRiskAnalysis = () => {
  riskLoading.value = true
  request.get('/api/ai/log-risk-analysis').then(res => {
    if (res.code === '0') {
      riskAnalysis.value = res.data
    } else {
      ElMessage.error(res.msg || '风险分析失败')
    }
  }).finally(() => {
    riskLoading.value = false
  })
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.ai-risk-card {
  margin-bottom: 18px;
}

.risk-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.risk-title {
  color: #252923;
  font-size: 17px;
  font-weight: 700;
}

.risk-summary {
  margin-top: 6px;
  color: #6f756d;
  line-height: 1.6;
}

.risk-metrics {
  margin-bottom: 16px;
}

.risk-metric {
  padding: 14px;
  border: 1px solid #e3dacb;
  border-radius: 8px;
  background: #fffaf0;
}

.risk-metric span {
  display: block;
  color: #777267;
  font-size: 12px;
}

.risk-metric strong {
  display: block;
  margin-top: 6px;
  color: #1f4d3a;
  font-size: 22px;
}

.risk-section-title {
  margin: 10px 0;
  color: #252923;
  font-weight: 700;
}

.risk-list {
  padding-left: 18px;
  color: #4d554a;
  line-height: 1.8;
}

.url-list {
  display: flex;
  flex-direction: column;
  border: 1px solid #e3dacb;
  border-radius: 8px;
  overflow: hidden;
}

.url-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
  border-bottom: 1px solid #eee5d8;
  background: #fffdf7;
}

.url-item:last-child {
  border-bottom: 0;
}

.url-item span {
  color: #4d554a;
  word-break: break-all;
}

.url-item strong {
  white-space: nowrap;
  color: #b66a2c;
}
</style>
