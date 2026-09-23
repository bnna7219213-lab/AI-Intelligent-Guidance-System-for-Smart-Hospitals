<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">MCP 工具中心</h2>
      <el-button type="primary" @click="openDialog()">注册工具</el-button>
    </div>

    <el-card>
      <el-table :data="tools" v-loading="loading" stripe>
        <el-table-column prop="toolCode" label="工具编码" width="150" />
        <el-table-column prop="toolName" label="工具名称" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="ENABLED"
              inactive-value="DISABLED"
              @change="val => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
      </el-table>
    </el-card>

    <!-- Tool Call Logs -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>调用日志</span>
      </template>
      <el-table :data="callLogs" stripe>
        <el-table-column prop="toolCode" label="工具编码" width="140" />
        <el-table-column prop="inputParams" label="输入参数" min-width="200" show-overflow-tooltip />
        <el-table-column prop="outputResult" label="输出结果" min-width="200" show-overflow-tooltip />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="调用时间" width="170" />
      </el-table>
    </el-card>

    <!-- Register Tool Dialog -->
    <el-dialog v-model="dialogVisible" title="注册新工具" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工具编码" prop="toolCode">
          <el-input v-model="form.toolCode" placeholder="如：search_drugs" />
        </el-form-item>
        <el-form-item label="工具名称" prop="toolName">
          <el-input v-model="form.toolName" placeholder="如：药品搜索" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" placeholder="工具用途描述" />
        </el-form-item>
        <el-form-item label="输入Schema" prop="inputSchema">
          <el-input
            v-model="form.inputSchema"
            type="textarea"
            :rows="6"
            placeholder='{"type":"object","properties":{"query":{"type":"string"}}}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMcpToolList,
  createMcpTool,
  toggleMcpTool,
  getMcpToolCallLogs
} from '@/api/admin'

const loading = ref(false)
const tools = ref([])
const callLogs = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  toolCode: '',
  toolName: '',
  description: '',
  inputSchema: ''
})

const rules = {
  toolCode: [{ required: true, message: '请输入工具编码', trigger: 'blur' }],
  toolName: [{ required: true, message: '请输入工具名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入工具描述', trigger: 'blur' }]
}

async function fetchTools() {
  loading.value = true
  try {
    const res = await getMcpToolList()
    tools.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function fetchLogs() {
  try {
    const res = await getMcpToolCallLogs({ pageNum: 1, pageSize: 50 })
    callLogs.value = res.data?.records || []
  } catch {
    // silent
  }
}

async function toggleStatus(row, val) {
  try {
    await toggleMcpTool(row.id, val)
    ElMessage.success(val === 'ENABLED' ? '工具已启用' : '工具已停用')
  } catch {
    row.status = val === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  }
}

function openDialog() {
  Object.assign(form, { toolCode: '', toolName: '', description: '', inputSchema: '' })
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  await createMcpTool(form)
  ElMessage.success('工具注册成功')
  dialogVisible.value = false
  fetchTools()
}

onMounted(() => {
  fetchTools()
  fetchLogs()
})
</script>
