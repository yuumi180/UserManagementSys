<template>
  <div class="login-container">
    <div class="login-box">
      <el-card class="login-card">
        <div class="login-header">
          <el-icon :size="50" color="#409EFF"><UserFilled /></el-icon>
          <h2 class="login-title">用户管理系统</h2>
          <p class="login-subtitle">Welcome Back!</p>
        </div>
        <el-form :model="loginForm" :rules="rules" ref="formRef" label-width="80px">
          <el-form-item label="用户名" prop="username">
            <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                prefix-icon="User"
                size="large"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                show-password
                size="large"
                @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="rememberMe" style="margin-right: 10px;">记住我</el-checkbox>
            <el-button
                type="primary"
                style="width: 100%"
                :loading="loading"
                @click="handleLogin"
                size="large"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = ref({
  username: localStorage.getItem('rememberedUser') || '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = () => {
  if (!formRef.value) return

  formRef.value.validate(valid => {
    if (valid) {
      loading.value = true
      request.post('/api/user/login', loginForm.value).then(res => {
        if (res.code === '0') {
          const userInfo = res.data
          localStorage.setItem('token', userInfo.token)
          localStorage.setItem('userInfo', JSON.stringify(userInfo))
          if (rememberMe.value) {
            localStorage.setItem('rememberedUser', loginForm.value.username)
          } else {
            localStorage.removeItem('rememberedUser')
          }
          ElMessage.success('登录成功')
          router.push('/')
        } else {
          ElMessage.error(res.msg || '登录失败')
        }
      }).finally(() => {
        loading.value = false
      })
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: move 20s linear infinite;
}

@keyframes move {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

.login-box {
  position: relative;
  z-index: 1;
  animation: fadeInUp 0.5s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-card {
  width: 450px;
  padding: 10px;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px 0;
}

.login-title {
  margin: 15px 0 5px;
  color: #333;
  font-size: 28px;
  font-weight: 600;
}

.login-subtitle {
  color: #999;
  font-size: 14px;
}
</style>
