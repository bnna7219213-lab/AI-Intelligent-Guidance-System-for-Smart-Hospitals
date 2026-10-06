<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">预约挂号</h2>
    </div>

    <!-- Filters -->
    <el-card style="margin-bottom: 16px;">
      <el-form :inline="true" @submit.prevent="fetchSchedules">
        <el-form-item label="科室">
          <el-select
            v-model="filterDepartmentId"
            placeholder="选择科室"
            clearable
            style="width: 180px"
            @change="fetchSchedules"
          >
            <el-option
              v-for="dept in departments"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="filterDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 160px"
            @change="fetchSchedules"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchSchedules">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Schedule Table -->
    <el-card v-loading="loading">
      <el-table :data="schedules" stripe>
        <el-table-column prop="doctorName" label="医生" width="120" />
        <el-table-column prop="title" label="职称" width="120" />
        <el-table-column prop="departmentName" label="科室" width="140" />
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="period" label="时段" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.period }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remainCount" label="剩余号源" width="100">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.remainCount <= 3 }">
              {{ row.remainCount }}/{{ row.totalCount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="fee" label="挂号费" width="100">
          <template #default="{ row }">
            <span style="color: #e6a23c; font-weight: 600;">¥{{ row.fee }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              type="primary"
              :disabled="row.remainCount <= 0"
              link
              @click="handleRegister(row)"
            >
              {{ row.remainCount > 0 ? '挂号' : '已满' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!schedules.length && !loading" description="暂无可用排班" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAvailableSchedules } from '@/api/patient'
import { getDepartmentList } from '@/api/admin'
import { createRegistration } from '@/api/patient'

const loading = ref(false)
const schedules = ref([])
const departments = ref([])
const filterDepartmentId = ref(null)
const filterDate = ref(null)

async function fetchDepartments() {
  const res = await getDepartmentList()
  departments.value = res.data || []
}

async function fetchSchedules() {
  loading.value = true
  try {
    const params = {}
    if (filterDepartmentId.value) params.departmentId = filterDepartmentId.value
    if (filterDate.value) params.date = filterDate.value
    const res = await getAvailableSchedules(params)
    schedules.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleRegister(row) {
  const message = '确认挂 ' + row.doctorName + ' ' + row.date + ' ' + row.period + ' 的号？挂号费 ¥' + row.fee
  await ElMessageBox.confirm(message, '确认挂号', { type: 'info' })
  try {
    await createRegistration({ schedulingId: row.id })
    ElMessage.success('挂号成功！')
    fetchSchedules()
  } catch (error) {
    // Optimistic lock error handled by interceptor
  }
}

onMounted(() => {
  fetchDepartments()
  fetchSchedules()
})
</script>

<style scoped>
.text-danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
