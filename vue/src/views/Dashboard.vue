<template>
  <div style="padding: 20px;">
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="display: flex; align-items: center;">
            <el-icon style="font-size: 40px; color: #409EFF; margin-right: 15px;">
              <user />
            </el-icon>
            <div>
              <div style="color: #999; font-size: 14px;">用户总数</div>
              <div style="font-size: 24px; font-weight: bold; color: #333;">
                {{ totalUsers }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="display: flex; align-items: center;">
            <el-icon style="font-size: 40px; color: #67C23A; margin-right: 15px;">
              <user-filled />
            </el-icon>
            <div>
              <div style="color: #999; font-size: 14px;">男用户</div>
              <div style="font-size: 24px; font-weight: bold; color: #333;">
                {{ maleUsers }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="display: flex; align-items: center;">
            <el-icon style="font-size: 40px; color: #F56C6C; margin-right: 15px;">
              <user-filled />
            </el-icon>
            <div>
              <div style="color: #999; font-size: 14px;">女用户</div>
              <div style="font-size: 24px; font-weight: bold; color: #333;">
                {{ femaleUsers }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="display: flex; align-items: center;">
            <el-icon style="font-size: 40px; color: #E6A23C; margin-right: 15px;">
              <trend-charts />
            </el-icon>
            <div>
              <div style="color: #999; font-size: 14px;">平均年龄</div>
              <div style="font-size: 24px; font-weight: bold; color: #333;">
                {{ avgAge }} 岁
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="font-weight: bold;">男女比例</div>
          </template>
          <div ref="pieChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="font-weight: bold;">年龄分布</div>
          </template>
          <div ref="barChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近新增用户 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <div style="font-weight: bold;">最近新增用户 (Top 5)</div>
      </template>
      <el-table :data="recentUsers" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="sex" label="性别" width="80" />
        <el-table-column prop="address" label="地址" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { User, UserFilled, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const totalUsers = ref(0)
const maleUsers = ref(0)
const femaleUsers = ref(0)
const avgAge = ref(0)
const recentUsers = ref([])
const pieChart = ref(null)
const barChart = ref(null)

const loadDashboard = () => {
  request.get('/api/user').then(res => {
    if (res.code === '0') {
      const users = res.data.records
      totalUsers.value = res.data.total

      // 统计男女数量
      maleUsers.value = users.filter(u => u.sex === '男').length
      femaleUsers.value = users.filter(u => u.sex === '女').length

      // 计算平均年龄
      const totalAge = users.reduce((sum, u) => sum + (u.age || 0), 0)
      avgAge.value = users.length > 0 ? Math.round(totalAge / users.length) : 0

      // 最近新增（取前 5 个）
      recentUsers.value = users.slice(0, 5)

      // 初始化图表
      nextTick(() => {
        initPieChart()
        initBarChart()
      })
    }
  })
}

const initPieChart = () => {
  const chart = echarts.init(pieChart.value)
  chart.setOption({
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '性别分布',
        type: 'pie',
        radius: '50%',
        data: [
          { value: maleUsers.value, name: '男' },
          { value: femaleUsers.value, name: '女' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  })
}

const initBarChart = () => {
  const chart = echarts.init(barChart.value)

  // 年龄段统计
  const ageGroups = {
    '18-25': 0,
    '26-35': 0,
    '36-45': 0,
    '46+': 0
  }

  recentUsers.value.forEach(u => {
    const age = u.age || 0
    if (age >= 18 && age <= 25) ageGroups['18-25']++
    else if (age >= 26 && age <= 35) ageGroups['26-35']++
    else if (age >= 36 && age <= 45) ageGroups['36-45']++
    else if (age > 45) ageGroups['46+']++
  })

  chart.setOption({
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: ['18-25', '26-35', '36-45', '46+']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        data: [
          ageGroups['18-25'],
          ageGroups['26-35'],
          ageGroups['36-45'],
          ageGroups['46+']
        ],
        type: 'bar',
        itemStyle: {
          color: '#409EFF'
        }
      }
    ]
  })
}

onMounted(() => {
  loadDashboard()

  // 窗口大小改变时重新渲染图表
  window.addEventListener('resize', () => {
    if (pieChart.value) {
      echarts.getInstanceByDom(pieChart.value).resize()
    }
    if (barChart.value) {
      echarts.getInstanceByDom(barChart.value).resize()
    }
  })
})
</script>
