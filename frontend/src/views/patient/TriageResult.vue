<template>
  <div class="page-container">
    <!-- Emergency Alert -->
    <el-alert
      v-if="result?.isEmergency"
      title="检测到危急征象，请立即前往急诊科"
      type="error"
      :closable="false"
      show-icon
      style="margin-bottom: 16px;"
    >
      <template #default>
        <p style="margin: 8px 0 0 0; font-size: 13px;">
          您的症状可能涉及危重情况，建议立即前往医院急诊科就诊，不要延误。
        </p>
      </template>
    </el-alert>

    <el-row :gutter="20">
      <!-- Recommended Department -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>推荐科室</span>
            <el-tag v-if="result?.emergencyLevel" :type="emergencyTagType" style="margin-left: 8px;">
              {{ result.emergencyLevel }}
            </el-tag>
          </template>
          <div class="dept-info">
            <div class="dept-name">{{ result?.recommendedDepartmentName || '推荐科室' }}</div>
            <div class="dept-desc">{{ result?.departmentDescription || '根据您的症状描述，该科室最为适合' }}</div>
          </div>
        </el-card>
      </el-col>

      <!-- Confidence & Reason -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>分析结果</span>
          </template>
          <div class="confidence-section">
            <div class="confidence-label">置信度</div>
            <el-progress
              :percentage="Math.round((result?.confidence || 0) * 100)"
              :color="confidenceColor"
              :stroke-width="14"
            />
          </div>
          <div class="reason-section">
            <div class="reason-label">推荐理由</div>
            <div class="reason-text">{{ result?.reason || '基于症状分析的综合推荐' }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Recommended Doctor -->
    <el-card style="margin-top: 16px;">
      <template #header>
        <span>推荐医生</span>
      </template>
      <div v-if="result?.recommendedDoctorName" class="doctor-info">
        <el-avatar :size="60" style="background: #409eff;">
          {{ result.recommendedDoctorName?.charAt(0) }}
        </el-avatar>
        <div class="doctor-detail">
          <div class="doctor-name">{{ result.recommendedDoctorName }}</div>
          <div class="doctor-meta">{{ result.doctorTitle || '' }} - {{ result.recommendedDepartmentName || '' }}</div>
          <div class="doctor-schedule">
            <el-tag size="small" type="success">可挂号</el-tag>
            <span v-if="result.fee" style="margin-left: 8px; color: #e6a23c;">挂号费: ¥{{ result.fee }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无推荐医生" />
    </el-card>

    <!-- Action -->
    <div class="action-area">
      <el-button type="primary" size="large" @click="goToRegistration">
        立即挂号
      </el-button>
      <el-button size="large" @click="goBackTriage">
        重新分诊
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const result = ref(null)

const emergencyTagType = computed(() => {
  if (!result.value?.emergencyLevel) return ''
  const level = result.value.emergencyLevel
  if (level.includes('高')) return 'danger'
  if (level.includes('中')) return 'warning'
  return 'info'
})

const confidenceColor = computed(() => {
  const c = (result.value?.confidence || 0) * 100
  if (c >= 80) return '#67c23a'
  if (c >= 50) return '#e6a23c'
  return '#f56c6c'
})

onMounted(() => {
  const stored = sessionStorage.getItem('triageResult')
  if (stored) {
    try {
      result.value = JSON.parse(stored)
    } catch {
      ElMessage.warning('分诊结果已失效，请重新分诊')
      router.push('/patient/triage')
    }
  } else {
    ElMessage.warning('请先完成分诊')
    router.push('/patient/triage')
  }
})

function goToRegistration() {
  router.push('/patient/registration')
}

function goBackTriage() {
  router.push('/patient/triage')
}
</script>

<style scoped>
.dept-info {
  text-align: center;
  padding: 16px 0;
}
.dept-name {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}
.dept-desc {
  color: #606266;
  font-size: 14px;
}
.confidence-section {
  margin-bottom: 20px;
}
.confidence-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.reason-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.reason-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
}
.doctor-info {
  display: flex;
  align-items: center;
  gap: 16px;
}
.doctor-detail {
  flex: 1;
}
.doctor-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.doctor-meta {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
.doctor-schedule {
  margin-top: 8px;
}
.action-area {
  text-align: center;
  margin-top: 24px;
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
