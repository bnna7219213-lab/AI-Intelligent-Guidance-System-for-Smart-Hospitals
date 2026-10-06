<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">医学知识库</h2>
    </div>

    <el-tabs v-model="activeTab">
      <!-- Groups Tab -->
      <el-tab-pane label="知识分组" name="groups">
        <div class="toolbar" style="margin-bottom: 12px;">
          <el-button type="primary" @click="openGroupDialog()">新增分组</el-button>
        </div>
        <el-table :data="groups" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="分组名称" min-width="150" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="170" />
        </el-table>
      </el-tab-pane>

      <!-- Documents Tab -->
      <el-tab-pane label="文档管理" name="documents">
        <div class="toolbar" style="margin-bottom: 12px;">
          <el-select v-model="selectedGroupId" placeholder="选择分组" style="width: 200px; margin-right: 12px;">
            <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
          <el-upload
            ref="uploadRef"
            :action="uploadAction"
            :auto-upload="false"
            :show-file-list="false"
            :http-request="handleUpload"
            accept=".pdf,.docx,.txt,.md"
          >
            <el-button type="primary" :disabled="!selectedGroupId">上传文档</el-button>
          </el-upload>
        </div>
        <el-table :data="documents" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="fileName" label="文件名" min-width="200" />
          <el-table-column prop="fileType" label="类型" width="80" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'EMBEDDED' ? 'success' : 'info'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button type="warning" link @click="handleEmbed(row)" :loading="row.embedLoading">
                向量化
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- Search Tab -->
      <el-tab-pane label="语义搜索" name="search">
        <el-card>
          <el-form label-width="100px">
            <el-form-item label="查询文本">
              <el-input
                v-model="searchQuery"
                type="textarea"
                :rows="4"
                placeholder="输入要搜索的医学问题或关键词..."
              />
            </el-form-item>
            <el-form-item label="分组">
              <el-select v-model="searchGroupId" placeholder="选择分组（可选）" clearable style="width: 200px">
                <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="Top K">
              <el-input-number v-model="topK" :min="1" :max="20" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
            </el-form-item>
          </el-form>

          <el-divider />

          <div v-if="searchResults.length">
            <div v-for="(item, idx) in searchResults" :key="idx" class="search-result-item">
              <div class="search-result-content">{{ item.content }}</div>
              <div class="search-result-meta">
                <el-tag size="small">相似度: {{ (item.similarity || item.score || 0).toFixed(4) }}</el-tag>
                <span class="search-result-source">{{ item.source || item.fileName }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无搜索结果" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- Group Dialog -->
    <el-dialog v-model="groupDialogVisible" :title="groupForm.id ? '编辑分组' : '新增分组'" width="500px">
      <el-form ref="groupFormRef" :model="groupForm" :rules="groupRules" label-width="100px">
        <el-form-item label="分组名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="如：内科知识" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="groupForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGroup">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getKbGroups,
  createKbGroup,
  uploadKbDocument,
  embedKbDocument,
  searchKb
} from '@/api/admin'

const activeTab = ref('groups')
const groups = ref([])
const documents = ref([])
const selectedGroupId = ref(null)
const searchQuery = ref('')
const searchGroupId = ref(null)
const topK = ref(5)
const searchResults = ref([])

const groupDialogVisible = ref(false)
const groupFormRef = ref(null)
const groupForm = reactive({ id: null, name: '', description: '' })
const groupRules = {
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }]
}

const uploadAction = ''

async function fetchGroups() {
  const res = await getKbGroups()
  groups.value = res.data || []
}

async function fetchDocuments() {
  // Documents are loaded per group selection in this simplified version
  documents.value = []
}

function openGroupDialog(row) {
  if (row) {
    Object.assign(groupForm, row)
  } else {
    Object.assign(groupForm, { id: null, name: '', description: '' })
  }
  groupDialogVisible.value = true
}

async function submitGroup() {
  const valid = await groupFormRef.value.validate().catch(() => false)
  if (!valid) return
  await createKbGroup(groupForm)
  ElMessage.success('分组创建成功')
  groupDialogVisible.value = false
  fetchGroups()
}

async function handleUpload(options) {
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('groupId', selectedGroupId.value)
  await uploadKbDocument(formData)
  ElMessage.success('文档上传成功')
  fetchDocuments()
}

async function handleEmbed(row) {
  row.embedLoading = true
  try {
    await embedKbDocument(row.id)
    ElMessage.success('文档向量化完成')
    row.status = 'EMBEDDED'
  } finally {
    row.embedLoading = false
  }
}

async function handleSearch() {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入查询文本')
    return
  }
  const params = { query: searchQuery.value, topK: topK.value }
  if (searchGroupId.value) params.groupId = searchGroupId.value
  const res = await searchKb(params)
  searchResults.value = res.data || []
}

onMounted(() => {
  fetchGroups()
  fetchDocuments()
})
</script>

<style scoped>
.search-result-item {
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
}
.search-result-item:last-child {
  border-bottom: none;
}
.search-result-content {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  margin-bottom: 8px;
}
.search-result-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}
.search-result-source {
  font-size: 12px;
  color: #909399;
}
</style>
