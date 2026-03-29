<template>
  <div class="login-container">
    <div class="login-box">
      <div class="glass-card">
        <div class="login-header">
          <div class="logo-wrapper">
            <el-icon :size="60" color="#fff"><UserFilled /></el-icon>
          </div>
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
                class="custom-input"
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
                class="custom-input"
                @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="rememberMe" style="margin-right: 10px;" class="remember-me">记住我</el-checkbox>
            <el-button
                type="primary"
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
                size="large"
            >
              <el-icon style="margin-right: 8px;"><Loading /></el-icon>
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled, Loading } from '@element-plus/icons-vue'
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
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.glass-card {
  width: 480px;
  padding: 50px 40px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 20px 0;
}

.logo-wrapper {
  display: inline-block;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
  margin-bottom: 20px;
}

.login-title {
  margin: 15px 0 10px;
  color: #333;
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-subtitle {
  color: #999;
  font-size: 15px;
  font-weight: 500;
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.custom-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.remember-me :deep(.el-checkbox__label) {
  color: #666;
  font-weight: 500;
}

.login-btn {
  width: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-weight: 600;
  font-size: 16px;
  letter-spacing: 1px;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.5);
}

.login-btn:active {
  transform: translateY(0);
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #555;
}
</style>
