<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">系统监控 & AI可观测</h2>
    </div>

    <!-- AI Usage Statistics -->
    <el-card>
      <template #header>
        <span>AI 使用统计</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="metric-card">
            <div class="metric-label">总调用次数</div>
            <div class="metric-value">{{ aiStats.totalRequests || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="metric-card">
            <div class="metric-label">总Token消耗</div>
            <div class="metric-value">{{ formatTokens(aiStats.totalTokens) }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="metric-card">
            <div class="metric-label">平均耗时</div>
            <div class="metric-value">{{ Math.round(aiStats.avgDurationMs || 0) }}ms</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="metric-card">
            <div class="metric-label">成功率</div>
            <div class="metric-value metric-success">{{ Math.round((aiStats.successRate || 0) * 100) }}%</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- Agent Timeline Section -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>Agent 执行时间线</span>
      </template>

      <!-- Agent Runs List -->
      <el-table :data="agentRuns" stripe @row-click="selectRun" highlight-current-row>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="sessionId" label="Session ID" min-width="200" show-overflow-tooltip />
        <el-table-column prop="usageType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.usageType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalSteps" label="步骤数" width="80" />
        <el-table-column prop="totalTokens" label="Token数" width="90" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>

      <!-- Selected Run Detail -->
      <div v-if="selectedRun" class="run-detail">
        <el-divider />
        <h4>Run 详情 - Session: {{ selectedRun.run?.sessionId }}</h4>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="状态">{{ selectedRun.run?.status }}</el-descriptions-item>
          <el-descriptions-item label="步骤数">{{ selectedRun.run?.totalSteps }}</el-descriptions-item>
          <el-descriptions-item label="Token">{{ selectedRun.run?.totalTokens }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ selectedRun.run?.durationMs }}ms</el-descriptions-item>
          <el-descriptions-item label="类型">{{ selectedRun.run?.usageType }}</el-descriptions-item>
          <el-descriptions-item label="错误" v-if="selectedRun.run?.errorMsg">
            <span style="color: #f56c6c">{{ selectedRun.run?.errorMsg }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- Steps Timeline -->
        <h4 style="margin-top: 16px;">步骤时间线</h4>
        <el-timeline>
          <el-timeline-item
            v-for="(step, idx) in selectedRun.steps"
            :key="idx"
            :type="step.status === 'SUCCESS' ? 'success' : 'danger'"
            :timestamp="`${step.durationMs}ms`"
            placement="top"
          >
            <el-card>
              <template #header>
                <span>Step {{ idx + 1 }} - {{ step.toolName || step.stepType }}</span>
              </template>
              <div><strong>输入:</strong></div>
              <pre class="step-io">{{ step.inputData }}</pre>
              <div><strong>输出:</strong></div>
              <pre class="step-io">{{ step.outputData }}</pre>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAiUsageStats, getAgentRuns, getAgentRunDetail } from '@/api/admin'

const aiStats = ref({})
const agentRuns = ref([])
const selectedRun = ref(null)

function formatTokens(val) {
  if (!val) return '0'
  if (val >= 1000000) return (val / 1000000).toFixed(1) + 'M'
  if (val >= 1000) return (val / 1000).toFixed(1) + 'K'
  return val.toString()
}

async function fetchAiStats() {
  const res = await getAiUsageStats()
  aiStats.value = res.data || {}
}

async function fetchAgentRuns() {
  const res = await getAgentRuns({ pageNum: 1, pageSize: 20 })
  agentRuns.value = res.data?.records || []
}

async function selectRun(row) {
  selectedRun.value = null
  const res = await getAgentRunDetail(row.id)
  selectedRun.value = res.data
}

onMounted(() => {
  fetchAiStats()
  fetchAgentRuns()
})
</script>

<style scoped>
.metric-card {
  text-align: center;
  padding: 16px 0;
}
.metric-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}
.metric-success {
  color: #67c23a;
}
.run-detail {
  margin-top: 16px;
}
.step-io {
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 100px;
  overflow: auto;
  margin: 4px 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
