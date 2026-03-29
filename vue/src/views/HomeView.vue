<template>
  <div style="height: 100%">
    <el-container style="height: 100%;">
      <el-aside width="220px" class="modern-aside">
        <div class="logo-section">
          <el-icon :size="32" color="#fff"><Menu /></el-icon>
          <span class="logo-text">后台管理系统</span>
        </div>
        <el-menu
            default-active="2"
            style="height: calc(100vh - 70px)"
            background-color="transparent"
            router
            class="modern-menu"
        >
          <el-menu-item index="/">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/roles">
            <el-icon><UserFilled /></el-icon>
            <span>角色管理</span>
          </el-menu-item>
          <el-menu-item index="/logs">
            <el-icon><Document /></el-icon>
            <span>操作日志</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="modern-header">
          <div class="header-left">
            <el-icon :size="24" class="breadcrumb-icon"><Location /></el-icon>
            <span class="breadcrumb-text">{{ currentRouteName }}</span>
          </div>
          <div class="header-right">
            <el-badge :value="5" class="notification-badge" :hidden="false">
              <el-icon :size="22" class="notification-icon"><Bell /></el-icon>
            </el-badge>
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
                <el-button type="success" @click="add" class="action-btn">
                  <el-icon><Plus /></el-icon> 新增用户
                </el-button>
                <el-button type="warning" @click="exportData" class="action-btn">
                  <el-icon><Download /></el-icon> 导出 Excel
                </el-button>
                <el-button type="info" @click="importDialogVisible = true" class="action-btn">
                  <el-icon><Upload /></el-icon> 导入 Excel
                </el-button>
                <el-button
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
              stripe
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
                  >
                    <el-icon><Edit /></el-icon> 编辑
                  </el-button>
                  <el-button
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
const tableData = ref([])
const search = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const importDialogVisible = ref(false)
const formRef = ref(null)
const uploadRef = ref(null)
const currentUser = ref(null)
const importing = ref(false)
const selectedFile = ref(null)
const viewUser = ref({})

const currentRouteName = computed(() => {
  const routeMap = {
    '/': '用户管理',
    '/dashboard': '数据仪表盘',
    '/roles': '角色管理',
    '/logs': '操作日志'
  }
  return routeMap[route.path] || '用户管理'
})

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
    ElMessage.info('个人中心功能开发中...')
  } else if (command === 'settings') {
    ElMessage.info('系统设置功能开发中...')
  }
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
  window.open('http://localhost:9090/api/user/export')
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
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    currentUser.value = JSON.parse(userInfo)
  }
  load()
})
</script>

<style scoped>
.modern-aside {
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.1);
}

.logo-section {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
}

.modern-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.9);
  font-size: 15px;
  border-radius: 10px !important;
  margin: 6px 16px !important;
  width: calc(100% - 32px) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modern-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.2) !important;
  transform: translateX(8px);
}

.modern-menu .el-menu-item.is-active {
  background: rgba(255, 255, 255, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.modern-header {
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.breadcrumb-icon {
  color: #667eea;
}

.breadcrumb-text {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-badge {
  cursor: pointer;
  margin-right: 10px;
}

.notification-icon {
  color: #666;
  transition: all 0.3s ease;
}

.notification-icon:hover {
  color: #667eea;
  transform: scale(1.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 24px;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.user-info:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.arrow-icon {
  color: #999;
}

.main-card {
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.toolbar-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  border-radius: 12px;
}

.search-box {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.button-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.action-btn {
  border-radius: 8px;
  font-weight: 500;
  padding: 10px 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.batch-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modern-table {
  border-radius: 12px;
  overflow: hidden;
}

.table-btn {
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 13px;
  margin: 0 2px;
  transition: all 0.3s ease;
}

.table-btn:hover {
  transform: scale(1.05);
}

.pagination-section {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.modern-pagination :deep(.el-pagination) {
  padding: 16px 0;
}

.modern-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.modern-descriptions :deep(.el-descriptions__label) {
  font-weight: 600;
  width: 100px;
}

.modern-upload :deep(.el-upload-dragger) {
  padding: 40px 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.upload-icon {
  font-size: 60px;
  color: #667eea;
  margin-bottom: 16px;
}

.upload-text {
  color: #666;
  font-size: 15px;
}

.upload-text em {
  color: #667eea;
  font-style: normal;
  font-weight: 600;
}

.upload-tip {
  color: #999;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  margin-top: 12px;
}
</style>