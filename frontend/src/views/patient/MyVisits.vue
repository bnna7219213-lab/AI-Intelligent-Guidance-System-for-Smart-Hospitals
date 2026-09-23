<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">我的就诊记录</h2>
      <el-button type="primary" @click="fetchData">刷新</el-button>
    </div>

    <el-card v-loading="loading">
      <el-table :data="registrations" stripe>
        <el-table-column prop="registrationNo" label="挂号单号" width="170" />
        <el-table-column prop="doctorName" label="医生" width="120" />
        <el-table-column prop="departmentName" label="科室" width="140" />
        <el-table-column prop="visitDate" label="就诊日期" width="120" />
        <el-table-column prop="period" label="时段" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.period }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'COMPLETED'"
              type="primary"
              link
              @click="viewRecord(row)"
            >
              查看病历
            </el-button>
            <el-button
              v-if="row.status === 'REGISTERED'"
              type="danger"
              link
              @click="handleCancel(row)"
            >
              取消挂号
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Medical Record Dialog -->
    <el-dialog v-model="recordDialogVisible" title="电子病历" width="700px">
      <el-descriptions :column="2" border size="small" v-if="currentRecord">
        <el-descriptions-item label="挂号单号">{{ currentRecord.registrationNo }}</el-descriptions-item>
        <el-descriptions-item label="就诊日期">{{ currentRecord.visitDate }}</el-descriptions-item>
      </el-descriptions>
      <el-divider />
      <div v-if="recordDetail" class="record-detail">
        <div class="record-section">
          <h4>1. 主诉</h4>
          <p>{{ recordDetail.chiefComplaint || '-' }}</p>
        </div>
        <div class="record-section">
          <h4>2. 现病史</h4>
          <p>{{ recordDetail.presentIllness || '-' }}</p>
        </div>
        <div class="record-section">
          <h4>3. 既往史</h4>
          <p>{{ recordDetail.pastHistory || '-' }}</p>
        </div>
        <div class="record-section">
          <h4>4. 过敏史</h4>
          <p>{{ recordDetail.allergyHistory || '-' }}</p>
        </div>
        <div class="record-section">
          <h4>5. 体格检查</h4>
          <p>{{ recordDetail.physicalExam || '-' }}</p>
        </div>
        <div class="record-section">
          <h4>6. 诊断</h4>
          <p>{{ recordDetail.diagnosis || '-' }}</p>
        </div>
        <div class="record-section">
          <h4>7. 治疗方案</h4>
          <p>{{ recordDetail.treatmentPlan || '-' }}</p>
        </div>
      </div>
      <el-empty v-else description="暂无病历记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyRegistrations, cancelRegistration, getRegistrationRecord } from '@/api/patient'

const loading = ref(false)
const registrations = ref([])
const recordDialogVisible = ref(false)
const currentRecord = ref(null)
const recordDetail = ref(null)

function statusLabel(status) {
  const map = {
    REGISTERED: '待就诊',
    IN_PROGRESS: '就诊中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

function statusType(status) {
  const map = {
    REGISTERED: 'info',
    IN_PROGRESS: 'warning',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }
  return map[status] || ''
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getMyRegistrations()
    registrations.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function viewRecord(row) {
  currentRecord.value = row
  recordDetail.value = null
  recordDialogVisible.value = true
  try {
    const res = await getRegistrationRecord(row.id)
    recordDetail.value = res.data
  } catch {
    // silent
  }
}

async function handleCancel(row) {
  await ElMessageBox.confirm('确定取消该挂号吗？', '提示', { type: 'warning' })
  await cancelRegistration(row.id)
  ElMessage.success('挂号已取消')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.record-section {
  margin-bottom: 16px;
}
.record-section h4 {
  margin: 0 0 6px 0;
  color: #303133;
  font-size: 14px;
}
.record-section p {
  margin: 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
}
</style>
