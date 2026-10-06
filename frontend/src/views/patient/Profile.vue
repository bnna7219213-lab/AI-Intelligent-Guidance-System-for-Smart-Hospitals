<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">个人档案</h2>
    </div>

    <el-card>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        v-loading="loading"
        style="max-width: 600px;"
      >
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio value="MALE">男</el-radio>
            <el-radio value="FEMALE">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="0" :max="150" />
        </el-form-item>
        <el-form-item label="既往病史" prop="chronicHistory">
          <el-input
            v-model="form.chronicHistory"
            type="textarea"
            :rows="3"
            placeholder="高血压、糖尿病等慢性病..."
          />
        </el-form-item>
        <el-form-item label="过敏史" prop="allergyHistory">
          <el-input
            v-model="form.allergyHistory"
            type="textarea"
            :rows="3"
            placeholder="药物过敏、食物过敏等..."
          />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存档案</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '@/api/patient'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const formRef = ref(null)

const form = reactive({
  name: '',
  gender: '',
  age: null,
  chronicHistory: '',
  allergyHistory: '',
  phone: ''
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }]
}

async function fetchProfile() {
  loading.value = true
  try {
    const res = await getProfile()
    if (res.data) {
      Object.assign(form, res.data)
    } else {
      form.name = userStore.username
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await updateProfile(form)
    ElMessage.success('档案保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(fetchProfile)
</script>
