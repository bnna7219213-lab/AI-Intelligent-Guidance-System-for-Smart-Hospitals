<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">电子病历</h2>
      <div v-if="record">
        <el-tag :type="record.status === 'SUBMITTED' ? 'success' : 'info'">
          {{ record.status === 'SUBMITTED' ? '已提交' : '草稿' }}
        </el-tag>
      </div>
    </div>

    <el-card v-loading="loading">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :disabled="isSubmitted"
        label-position="top"
      >
        <el-form-item label="1. 主诉（Chief Complaint）" prop="chiefComplaint">
          <el-input
            v-model="form.chiefComplaint"
            type="textarea"
            :rows="3"
            placeholder="患者主要症状描述..."
          />
        </el-form-item>

        <el-form-item label="2. 现病史（Present Illness）" prop="presentIllness">
          <el-input
            v-model="form.presentIllness"
            type="textarea"
            :rows="3"
            placeholder="当前疾病的发展过程..."
          />
        </el-form-item>

        <el-form-item label="3. 既往史（Past History）" prop="pastHistory">
          <el-input
            v-model="form.pastHistory"
            type="textarea"
            :rows="3"
            placeholder="既往疾病史、手术史、输血史..."
          />
        </el-form-item>

        <el-form-item label="4. 过敏史（Allergy History）" prop="allergyHistory">
          <el-input
            v-model="form.allergyHistory"
            type="textarea"
            :rows="2"
            placeholder="药物过敏、食物过敏等..."
          />
        </el-form-item>

        <el-form-item label="5. 体格检查（Physical Exam）" prop="physicalExam">
          <el-input
            v-model="form.physicalExam"
            type="textarea"
            :rows="3"
            placeholder="检查结果..."
          />
        </el-form-item>

        <el-form-item label="6. 诊断（Diagnosis）" prop="diagnosis">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            :rows="2"
            placeholder="初步诊断..."
          />
        </el-form-item>

        <el-form-item label="7. 治疗方案（Treatment Plan）" prop="treatmentPlan">
          <el-input
            v-model="form.treatmentPlan"
            type="textarea"
            :rows="3"
            placeholder="治疗计划、用药建议..."
          />
        </el-form-item>

        <el-form-item v-if="!isSubmitted">
          <el-button type="info" @click="saveDraft" :loading="saving">保存草稿</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="saving">提交病历</el-button>
        </el-form-item>
        <el-alert
          v-else
          title="病历已提交，内容已锁定，不可修改"
          type="success"
          :closable="false"
          show-icon
        />
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMedicalRecord, saveMedicalRecord } from '@/api/doctor'

const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const formRef = ref(null)
const record = ref(null)

const form = reactive({
  id: null,
  registrationId: null,
  chiefComplaint: '',
  presentIllness: '',
  pastHistory: '',
  allergyHistory: '',
  physicalExam: '',
  diagnosis: '',
  treatmentPlan: '',
  status: 'DRAFT'
})

const isSubmitted = computed(() => record.value?.status === 'SUBMITTED')

const rules = {
  chiefComplaint: [{ required: true, message: '请填写主诉', trigger: 'blur' }],
  diagnosis: [{ required: true, message: '请填写诊断', trigger: 'blur' }]
}

async function fetchRecord() {
  const regId = route.query.registrationId
  if (!regId) return
  loading.value = true
  try {
    const res = await getMedicalRecord(regId)
    if (res.data) {
      Object.assign(form, res.data)
      record.value = res.data
    } else {
      form.registrationId = Number(regId)
    }
  } finally {
    loading.value = false
  }
}

async function saveDraft() {
  form.status = 'DRAFT'
  await doSave()
  ElMessage.success('草稿保存成功')
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  form.status = 'SUBMITTED'
  await doSave()
  ElMessage.success('病历已提交')
  record.value = { ...form }
}

async function doSave() {
  saving.value = true
  try {
    await saveMedicalRecord(form)
  } finally {
    saving.value = false
  }
}

onMounted(fetchRecord)
</script>
