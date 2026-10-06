<template>
  <div class="page-container">
    <div class="triage-container">
      <el-card class="triage-card">
        <div class="triage-header">
          <div style="font-size: 48px; margin-bottom: 16px;">🩺</div>
          <h2>智能分诊</h2>
          <p class="triage-desc">请详细描述您的症状，AI将为您推荐最合适的科室和医生</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent>
          <el-form-item prop="symptom">
            <el-input
              v-model="form.symptom"
              type="textarea"
              :rows="8"
              placeholder="请详细描述您的症状，例如：&#10;&#10;我这两天一直头痛，主要是太阳穴附近，伴有轻微的恶心感。头痛在下午比较严重，休息后会略微缓解。最近工作压力大，睡眠质量也不太好。"
              :disabled="loading"
            />
          </el-form-item>
        </el-form>

        <div class="triage-actions">
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleSubmit"
          >
            开始智能分诊
          </el-button>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="loading-area">
          <el-progress :percentage="progress" :stroke-width="12" style="max-width: 400px; margin: 0 auto;" />
          <p class="loading-text">
            <el-icon class="is-loading"><Loading /></el-icon>
            AI正在分析中...请使用ReAct多步推理，请耐心等待
          </p>
          <p class="loading-hint">这可能耗时30-60秒，正在调用MCP工具和知识库</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { submitTriage } from '@/api/patient'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const progress = ref(0)

const form = reactive({
  symptom: ''
})

const rules = {
  symptom: [
    { required: true, message: '请描述您的症状', trigger: 'blur' },
    { min: 10, message: '请至少输入10个字描述症状', trigger: 'blur' }
  ]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  progress.value = 0

  // Simulate progress animation
  const progressTimer = setInterval(() => {
    if (progress.value < 90) {
      progress.value += Math.random() * 5
    }
  }, 1000)

  try {
    const res = await submitTriage({ symptom: form.symptom })
    progress.value = 100
    clearInterval(progressTimer)
    ElMessage.success('分诊分析完成')
    // Store result and navigate
    sessionStorage.setItem('triageResult', JSON.stringify(res.data))
    router.push('/patient/triage-result')
  } catch (error) {
    clearInterval(progressTimer)
    loading.value = false
    progress.value = 0
  }
}
</script>

<style scoped>
.triage-container {
  max-width: 700px;
  margin: 0 auto;
}
.triage-card {
  padding: 30px;
}
.triage-header {
  text-align: center;
  margin-bottom: 30px;
}
.triage-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
}
.triage-desc {
  color: #909399;
  font-size: 14px;
}
.triage-actions {
  text-align: center;
  margin-top: 20px;
}
.loading-area {
  margin-top: 30px;
  text-align: center;
}
.loading-text {
  margin-top: 16px;
  font-size: 15px;
  color: #409eff;
}
.loading-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
