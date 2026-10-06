<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">运营分析</h2>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        @change="fetchStats"
      />
    </div>

    <!-- Statistics Cards -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">总挂号数</div>
          <div class="stat-value">{{ stats.totalRegistrations || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">完诊率</div>
          <div class="stat-value stat-green">{{ completionRate }}%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">总收入</div>
          <div class="stat-value stat-orange">¥{{ stats.totalRevenue || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">取消挂号</div>
          <div class="stat-value stat-red">{{ stats.cancelledRegistrations || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Department-wise Registrations -->
    <el-card style="margin-top: 16px;">
      <template #header>
        <span>科室挂号统计</span>
      </template>
      <el-table :data="schedulingUsage" stripe>
        <el-table-column prop="departmentName" label="科室" min-width="150" />
        <el-table-column prop="totalSchedules" label="总排班数" width="120" />
        <el-table-column prop="usedSchedules" label="已挂号数" width="120" />
        <el-table-column label="使用率" width="120">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.usageRate || 0)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Triage Hit Rate -->
    <el-card style="margin-top: 16px;">
      <template #header>
        <div>
          <span>分诊命中率</span>
          <el-tag type="primary" style="margin-left: 8px;">核心创新指标</el-tag>
        </div>
      </template>
      <el-table :data="triageHitRate" stripe>
        <el-table-column prop="departmentName" label="推荐科室" min-width="150" />
        <el-table-column prop="totalTriage" label="推荐次数" width="120" />
        <el-table-column prop="hitCount" label="转化挂号" width="120" />
        <el-table-column label="命中率" width="150">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round(row.hitRate || 0)"
              :color="row.hitRate >= 60 ? '#67c23a' : row.hitRate >= 30 ? '#e6a23c' : '#f56c6c'"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  getRegistrationStats,
  getSchedulingUsage,
  getTriageHitRate
} from '@/api/admin'

const dateRange = ref(null)
const stats = ref({})
const schedulingUsage = ref([])
const triageHitRate = ref([])

const completionRate = computed(() => {
  const total = stats.value.totalRegistrations || 0
  const completed = stats.value.completedRegistrations || 0
  if (!total) return 0
  return Math.round((completed / total) * 100)
})

async function fetchStats() {
  const params = {}
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  const [regRes, schedRes, triageRes] = await Promise.all([
    getRegistrationStats(params),
    getSchedulingUsage(),
    getTriageHitRate()
  ])
  stats.value = regRes.data || {}
  schedulingUsage.value = schedRes.data || []
  triageHitRate.value = triageRes.data || []
}

onMounted(fetchStats)
</script>

<style scoped>
.stats-row {
  margin-bottom: 0;
}
.stat-card {
  text-align: center;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.stat-green {
  color: #67c23a;
}
.stat-orange {
  color: #e6a23c;
}
.stat-red {
  color: #f56c6c;
}
</style>
