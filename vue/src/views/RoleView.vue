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

    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold;">角色管理</span>
          <el-button type="primary" @click="add">新增角色</el-button>
        </div>
      </template>

      <el-table :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="code" label="角色编码" width="150" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
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
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
        v-model="dialogVisible"
        :title="form.id ? '编辑角色' : '新增角色'"
        width="500px"
        @close="resetForm"
    >
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入角色编码（如：ADMIN）" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import request from '@/utils/request'

const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref(null)

const form = ref({
  name: '',
  code: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const load = () => {
  request.get('/api/role', {
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

const add = () => {
  form.value = {}
  dialogVisible.value = true
}

const save = () => {
  if (!formRef.value) return

  formRef.value.validate(valid => {
    if (valid) {
      const url = form.value.id ? '/api/role' : '/api/role'
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
  ElMessageBox.confirm('确定要删除该角色吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.delete(`/api/role/${id}`).then(res => {
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

onMounted(() => {
  load()
})
</script>
