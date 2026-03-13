<template>
  <div style="padding: 20px;">
    <el-card>
      <template #header>
        <div style="font-weight: bold;">操作日志</div>
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
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

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

onMounted(() => {
  load()
})
</script>
