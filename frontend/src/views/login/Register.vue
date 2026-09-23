<template>
  <div class="register-bg">
    <div class="glass-card">
      <h1 class="register-title">用户注册</h1>
      <p class="register-subtitle">创建您的就诊账号</p>
      <el-form ref="formRef" :model="form" :rules="rules" class="register-form">
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
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请确认密码"
            :prefix-icon="Lock"
            show-password
            size="large"
          />
        </el-form-item>
        <el-form-item prop="realName">
          <el-input
            v-model="form.realName"
            placeholder="请输入真实姓名"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="role">
          <el-radio-group v-model="form.role">
            <el-radio value="patient">患者</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="full-width"
            size="large"
            :loading="loading"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        <router-link to="/login">已有账号？立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  role: 'patient'
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirmPassword, trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      realName: form.realName,
      role: form.role
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    // error handled by axios interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-title {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  text-align: center;
  margin-bottom: 8px;
}
.register-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  text-align: center;
  margin-bottom: 30px;
}
.register-footer {
  text-align: center;
  margin-top: 16px;
}
.register-footer a {
  color: #fff;
  font-size: 13px;
  opacity: 0.85;
}
.register-footer a:hover {
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
:deep(.el-radio__label) {
  color: #fff;
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
