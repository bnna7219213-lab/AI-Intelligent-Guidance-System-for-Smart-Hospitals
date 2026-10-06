<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">医生管理</h2>
    </div>

    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="departmentName" label="科室" width="150">
          <template #default="{ row }">
            <span>{{ row.departmentName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="职称" width="130" />
        <el-table-column prop="username" label="账号" width="140" />
        <el-table-column label="账号状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.userStatus === 1 ? 'success' : 'info'">
              {{ row.userStatus === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminDoctorList } from '@/api/admin'

const loading = ref(false)
const list = ref([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminDoctorList()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
