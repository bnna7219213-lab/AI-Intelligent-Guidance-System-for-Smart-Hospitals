<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">提示词管理</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="12" v-for="item in list" :key="item.id" style="margin-bottom: 16px;">
        <el-card>
          <template #header>
            <div class="flex-between">
              <span>{{ item.promptName || item.promptKey }}</span>
              <el-button type="primary" link @click="openEdit(item)">编辑</el-button>
            </div>
          </template>
          <div class="prompt-content">{{ item.content?.substring(0, 200) }}...</div>
          <div class="prompt-meta">
            <el-tag size="small">Key: {{ item.promptKey }}</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Edit Dialog -->
    <el-dialog v-model="dialogVisible" title="编辑提示词" width="700px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="提示词名称">
          <el-input v-model="form.promptName" />
        </el-form-item>
        <el-form-item label="提示词Key">
          <el-input v-model="form.promptKey" disabled />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="15"
            placeholder="请输入提示词内容..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPromptList, updatePrompt } from '@/api/admin'

const list = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, promptKey: '', promptName: '', content: '' })

async function fetchData() {
  const res = await getPromptList()
  list.value = res.data || []
}

function openEdit(item) {
  Object.assign(form, item)
  dialogVisible.value = true
}

async function submitEdit() {
  if (!form.content) {
    ElMessage.warning('请输入提示词内容')
    return
  }
  await updatePrompt(form)
  ElMessage.success('提示词更新成功')
  dialogVisible.value = false
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.prompt-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  max-height: 100px;
  overflow: hidden;
  margin-bottom: 10px;
}
.prompt-meta {
  display: flex;
  gap: 8px;
}
</style>
