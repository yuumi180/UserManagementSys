<template>
  <div style="padding: 20px; height: 100%; overflow-y: auto;">
    <!-- 返回按钮和导出按钮 -->
    <div style="margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center;">
      <el-button @click="$router.push('/')" style="margin-bottom: 10px;">
        <el-icon style="vertical-align: middle; margin-right: 5px;">
          <Back />
        </el-icon>
        返回用户管理
      </el-button>
      <el-button type="primary" @click="exportDashboard" icon="Download">
        导出仪表盘
      </el-button>
    </div>

    <!-- 所有内容 -->
    <div id="dashboard-content">
      <!-- 统计卡片 -->
      <el-row :gutter="20" style="margin-bottom: 20px;">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
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
          <el-card shadow="hover" class="stat-card">
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
          <el-card shadow="hover" class="stat-card">
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
          <el-card shadow="hover" class="stat-card">
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

      <!-- 地址分布 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <el-card>
            <template #header>
              <div style="font-weight: bold;">用户地址分布（环形图）</div>
            </template>
            <div ref="mapChart" style="height: 400px;"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>
              <div style="font-weight: bold;">城市用户数 TOP10</div>
            </template>
            <div ref="cityBarChart" style="height: 400px;"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 用户数量最多的地址 Top3 -->
      <el-card style="margin-top: 20px;">
        <template #header>
          <div style="font-weight: bold; font-size: 16px;">🏆 用户数量最多的地址 Top3</div>
        </template>
        <el-row :gutter="20">
          <!-- 第二名 -->
          <el-col :span="8">
            <el-card shadow="hover" class="top-card" style="background: linear-gradient(135deg, #e0e0e0 0%, #9e9e9e 100%);">
              <div style="text-align: center; color: #fff;">
                <div style="font-size: 50px; margin-bottom: 10px;">🥈</div>
                <div style="font-size: 14px; opacity: 0.9; margin-bottom: 5px;">第二名</div>
                <div style="font-size: 28px; font-weight: bold; margin-bottom: 10px;">
                  {{ top3Cities[1]?.name || '-' }}
                </div>
                <div style="font-size: 36px; font-weight: bold;">
                  {{ top3Cities[1]?.value || 0 }}
                  <span style="font-size: 16px;">人</span>
                </div>
              </div>
            </el-card>
          </el-col>

          <!-- 第一名 -->
          <el-col :span="8">
            <el-card shadow="hover" class="top-card" style="background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%); transform: scale(1.05);">
              <div style="text-align: center; color: #fff;">
                <div style="font-size: 60px; margin-bottom: 10px;">🏆</div>
                <div style="font-size: 16px; opacity: 0.9; margin-bottom: 5px;">第一名</div>
                <div style="font-size: 32px; font-weight: bold; margin-bottom: 10px;">
                  {{ top3Cities[0]?.name || '-' }}
                </div>
                <div style="font-size: 42px; font-weight: bold;">
                  {{ top3Cities[0]?.value || 0 }}
                  <span style="font-size: 18px;">人</span>
                </div>
              </div>
            </el-card>
          </el-col>

          <!-- 第三名 -->
          <el-col :span="8">
            <el-card shadow="hover" class="top-card" style="background: linear-gradient(135deg, #cd7f32 0%, #8b4513 100%);">
              <div style="text-align: center; color: #fff;">
                <div style="font-size: 50px; margin-bottom: 10px;">🥉</div>
                <div style="font-size: 14px; opacity: 0.9; margin-bottom: 5px;">第三名</div>
                <div style="font-size: 28px; font-weight: bold; margin-bottom: 10px;">
                  {{ top3Cities[2]?.name || '-' }}
                </div>
                <div style="font-size: 36px; font-weight: bold;">
                  {{ top3Cities[2]?.value || 0 }}
                  <span style="font-size: 16px;">人</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { User, UserFilled, TrendCharts, Back, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import html2canvas from 'html2canvas'
import request from '@/utils/request'

const totalUsers = ref(0)
const maleUsers = ref(0)
const femaleUsers = ref(0)
const avgAge = ref(0)
const recentUsers = ref([])
const top3Cities = ref([])
const pieChart = ref(null)
const barChart = ref(null)
const mapChart = ref(null)
const cityBarChart = ref(null)

const loadDashboard = () => {
  request.get('/api/user', {
    params: {
      pageNum: 1,
      pageSize: 100
    }
  }).then(res => {
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

      // 统计各城市用户数量
      const cityCount = {}
      users.forEach(u => {
        const address = u.address || ''
        const match = address.match(/([\u4e00-\u9fa5]+市)/)
        if (match) {
          const city = match[1]
          cityCount[city] = (cityCount[city] || 0) + 1
        }
      })

      // 转换为数组并排序，取前 3 个
      const sortedCities = Object.entries(cityCount)
        .map(([name, value]) => ({ name, value }))
        .sort((a, b) => b.value - a.value)
        .slice(0, 3)

      top3Cities.value = sortedCities

      // 初始化图表
      nextTick(() => {
        initPieChart()
        initBarChart()
        initMapChart(users)
        initCityBarChart(users)
      })
    }
  }).catch(err => {
    console.error('加载数据失败:', err)
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

const initMapChart = (users) => {
  console.log('地图图表 - 用户数据:', users);

  const chart = echarts.init(mapChart.value)

  // 统计各城市用户数量
  const cityCount = {}
  users.forEach(u => {
    const address = u.address || ''
    // 修复正则：匹配中文字符 + 市
    const match = address.match(/([\u4e00-\u9fa5]+市)/)
    console.log('地址:', address, '匹配结果:', match);
    if (match) {
      const city = match[1]
      cityCount[city] = (cityCount[city] || 0) + 1
    }
  })

  console.log('城市统计:', cityCount);

  // 转换为 ECharts 需要的格式
  const data = Object.keys(cityCount).map(city => ({
    name: city,
    value: cityCount[city]
  }))

  console.log('图表数据:', data);

  // 按数量排序
  data.sort((a, b) => b.value - a.value)

  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>用户数：{c}'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: data.map(item => item.name)
    },
    series: [
      {
        name: '用户地址分布',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        data: data,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        label: {
          formatter: '{b}: {c}'
        }
      }
    ]
  })
}

const initCityBarChart = (users) => {
  console.log('柱状图表 - 用户数据:', users);

  const chart = echarts.init(cityBarChart.value)

  // 统计各城市用户数量
  const cityCount = {}
  users.forEach(u => {
    const address = u.address || ''
    const match = address.match(/([\u4e00-\u9fa5]+市)/)
    console.log('地址:', address, '匹配结果:', match);
    if (match) {
      const city = match[1]
      cityCount[city] = (cityCount[city] || 0) + 1
    }
  })

  console.log('城市统计:', cityCount);

  // 转换为数组并排序
  const sortedCities = Object.entries(cityCount)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10) // 取前 10 个城市

  console.log('排序后城市:', sortedCities);

  const cities = sortedCities.map(item => item[0])
  const counts = sortedCities.map(item => item[1])

  console.log('图表数据 - 城市:', cities, '数量:', counts);

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'category',
      data: cities,
      axisLabel: {
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '用户数'
    },
    series: [
      {
        data: counts,
        type: 'bar',
        barWidth: '50%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 0.5, color: '#188df0' },
            { offset: 1, color: '#188df0' }
          ])
        },
        label: {
          show: true,
          position: 'top'
        }
      }
    ]
  })
}

// 导出 Dashboard 为图片
const exportDashboard = () => {
  const element = document.getElementById('dashboard-content')

  // 显示加载提示
  const loading = ElMessage({
    message: '正在生成图片...',
    type: 'info',
    duration: 0
  })

  html2canvas(element, {
    useCORS: true, // 允许跨域图片
    scale: 2, // 提高清晰度
    backgroundColor: '#ffffff', // 白色背景
    logging: false,
    width: element.scrollWidth,
    height: element.scrollHeight
  }).then(canvas => {
    loading.close()

    // 创建下载链接
    const link = document.createElement('a')
    link.download = `仪表盘-${new Date().toLocaleDateString()}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()

    ElMessage.success('导出成功！')
  }).catch(err => {
    loading.close()
    console.error('导出失败:', err)
    ElMessage.error('导出失败，请重试')
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
    if (mapChart.value) {
      echarts.getInstanceByDom(mapChart.value).resize()
    }
    if (cityBarChart.value) {
      echarts.getInstanceByDom(cityBarChart.value).resize()
    }
  })
})
</script>

<style scoped>
.stat-card {
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.top-card {
  transition: all 0.3s ease;
  border: none;
  border-radius: 12px;
}

.top-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}
</style>
