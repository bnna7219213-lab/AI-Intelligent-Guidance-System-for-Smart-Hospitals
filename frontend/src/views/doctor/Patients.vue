<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">我的患者</h2>
      <el-button type="primary" @click="fetchData">刷新</el-button>
    </div>

    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="registrationNo" label="挂号单号" width="170" />
        <el-table-column prop="patientName" label="患者姓名" width="120">
          <template #default="{ row }">
            {{ row.patientName || `患者${row.patientId}` }}
          </template>
        </el-table-column>
        <el-table-column prop="departmentName" label="科室" width="140">
          <template #default="{ row }">
            {{ row.departmentName || '-' }}
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'REGISTERED'"
              type="success"
              link
              @click="handleStart(row)"
            >
              开始接诊
            </el-button>
            <el-button
              v-if="row.status === 'IN_PROGRESS'"
              type="warning"
              link
              @click="handleComplete(row)"
            >
              完成就诊
            </el-button>
            <el-button
              v-if="row.status === 'IN_PROGRESS'"
              type="primary"
              link
              @click="navigateToRecord(row)"
            >
              填写病历
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPatientList, startConsultation, completeConsultation } from '@/api/doctor'

const router = useRouter()
const loading = ref(false)
const list = ref([])

function statusLabel(status) {
  const map = {
    REGISTERED: '待就诊',
    IN_PROGRESS: '接诊中',
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
    const res = await getPatientList({ pageNum: 1, pageSize: 50 })
    list.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

async function handleStart(row) {
  await startConsultation(row.id)
  ElMessage.success('已开始接诊')
  fetchData()
}

async function handleComplete(row) {
  await completeConsultation(row.id)
  ElMessage.success('就诊已完成')
  fetchData()
}

function navigateToRecord(row) {
  router.push({ name: 'DoctorMedicalRecords', query: { registrationId: row.id } })
}

onMounted(fetchData)
</script>
