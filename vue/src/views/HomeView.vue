<template>
  <div style="height: 100%">
    <el-container style="height: 100%;">
      <el-aside width="200px" style="background-color: rgb(238, 241, 246);">
        <div style="height: 50px;line-height: 50px;padding-left: 30px;font-weight: bold;color: blue;">
          后台管理系统
        </div>
        <el-menu
          default-active="1"
          style="height: calc(100vh - 50px)"
          background-color="rgb(238, 241, 246)"
          router>
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header style="text-align: right; font-size: 12px; border-bottom: 1px solid #ccc; display: flex; align-items: center; justify-content: flex-end;">
          <el-dropdown @command="handleCommand">
            <span style="margin-right: 15px; cursor: pointer;">
              <el-avatar :size="30" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" style="vertical-align: middle; margin-right: 5px;" />
              {{ currentUser?.nickname || currentUser?.username || '管理员' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出系统</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-header>

        <el-main>
          <el-card>
            <div style="margin-bottom: 20px;">
              <el-input
                v-model="search"
                placeholder="请输入用户名或昵称搜索"
                style="width: 300px; margin-right: 10px;"
                clearable
                @clear="load"
              />
              <el-button type="primary" @click="load">搜索</el-button>
              <el-button type="success" @click="add">新增用户</el-button>
              <el-button type="warning" @click="exportData">导出 Excel</el-button>
              <el-button type="info" @click="importDialogVisible = true">导入 Excel</el-button>
              <el-button type="danger" @click="batchDelete" :disabled="selectedIds.length === 0">批量删除</el-button>
            </div>

            <el-table :data="tableData" border stripe style="width: 100%" @selection-change="handleSelectionChange">
              <el-table-column type="selection" width="55" />
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="username" label="用户名" width="120" />
              <el-table-column prop="nickname" label="昵称" width="120" />
              <el-table-column prop="age" label="年龄" width="80" />
              <el-table-column prop="sex" label="性别" width="80" />
              <el-table-column prop="address" label="地址" show-overflow-tooltip />
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="scope">
                  <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
                  <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div style="margin-top: 20px; text-align: right;">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="total"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </el-card>
        </el-main>
      </el-container>
    </el-container>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑用户' : '新增用户'"
      width="500px"
      @close="resetForm"
    >
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
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
      v-model="importDialogVisible"
      title="导入 Excel"
      width="500px"
    >
      <el-upload
        ref="uploadRef"
        drag
        action="#"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx, .xls"
        :on-change="handleFileChange"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            只能上传 xlsx/xls 文件
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, DataAnalysis } from '@element-plus/icons-vue'
import request from '@/utils/request'

const tableData = ref([])
const search = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const formRef = ref(null)
const uploadRef = ref(null)
const currentUser = ref(null)
const importing = ref(false)
const selectedFile = ref(null)

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
.el-header {
  background-color: #fff;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
