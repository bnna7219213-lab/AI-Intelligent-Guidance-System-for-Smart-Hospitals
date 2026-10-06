<template>
  <div class="login-bg">
    <div class="glass-card">
      <h1 class="login-title">AI 智慧医院智能导诊系统</h1>
      <p class="login-subtitle">智能预问诊 · 智能分诊 · 医学知识库</p>
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="full-width"
            size="large"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <router-link to="/register">还没有账号？立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form)
    userStore.login({
      token: res.data.token,
      userId: res.data.userId,
      role: res.data.role,
      username: res.data.username
    })
    ElMessage.success('登录成功')
    router.push(userStore.getUserHome())
  } catch (error) {
    // error handled by axios interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-title {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  text-align: center;
  margin-bottom: 8px;
}
.login-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  text-align: center;
  margin-bottom: 30px;
}
.login-form {
  margin-top: 10px;
}
.login-footer {
  text-align: center;
  margin-top: 16px;
}
.login-footer a {
  color: #fff;
  font-size: 13px;
  opacity: 0.85;
}
.login-footer a:hover {
  opacity: 1;
  text-decoration: underline;
}
:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.2);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.3) inset;
}
:deep(.el-input__inner),
:deep(.el-input__prefix) {
  color: #fff;
}
:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.6);
}
:deep(.el-button--primary) {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.4);
}
:deep(.el-button--primary:hover) {
  background: rgba(255, 255, 255, 0.4);
  border-color: rgba(255, 255, 255, 0.5);
}
</style>
