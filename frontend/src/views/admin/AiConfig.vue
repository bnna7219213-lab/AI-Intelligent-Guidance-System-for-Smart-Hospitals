<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">AI 模型配置</h2>
      <el-tag type="success">配置实时生效，无需重启</el-tag>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>Chat Model（对话模型）</span>
          </template>
          <el-form ref="chatFormRef" :model="chatForm" label-width="100px">
            <el-form-item label="API URL">
              <el-input v-model="chatForm.apiUrl" placeholder="https://api.openai.com/v1" />
            </el-form-item>
            <el-form-item label="API Key">
              <el-input v-model="chatForm.apiKey" type="password" show-password placeholder="sk-..." />
            </el-form-item>
            <el-form-item label="Model Name">
              <el-input v-model="chatForm.modelName" placeholder="gpt-4o-mini" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="chatTesting" @click="testChatConnection">测试连接</el-button>
              <el-button type="success" @click="saveChat">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>Vector Model（向量模型）</span>
          </template>
          <el-form ref="vectorFormRef" :model="vectorForm" label-width="100px">
            <el-form-item label="API URL">
              <el-input v-model="vectorForm.apiUrl" placeholder="https://api.openai.com/v1" />
            </el-form-item>
            <el-form-item label="API Key">
              <el-input v-model="vectorForm.apiKey" type="password" show-password placeholder="sk-..." />
            </el-form-item>
            <el-form-item label="Model Name">
              <el-input v-model="vectorForm.modelName" placeholder="text-embedding-3-small" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="vectorTesting" @click="testVectorConnection">测试连接</el-button>
              <el-button type="success" @click="saveVector">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiConfigs, saveAiConfig, testAiConnection } from '@/api/admin'

const chatForm = reactive({ configKey: 'CHAT_MODEL', apiUrl: '', apiKey: '', modelName: '' })
const vectorForm = reactive({ configKey: 'VECTOR_MODEL', apiUrl: '', apiKey: '', modelName: '' })
const chatTesting = ref(false)
const vectorTesting = ref(false)

async function fetchConfig() {
  try {
    const res = await getAiConfigs()
    const configs = res.data || []
    const chat = configs.find(c => c.configKey === 'CHAT_MODEL')
    const vector = configs.find(c => c.configKey === 'VECTOR_MODEL')
    if (chat) Object.assign(chatForm, chat)
    if (vector) Object.assign(vectorForm, vector)
  } catch (e) {
    // silent
  }
}

async function saveChat() {
  await saveAiConfig(chatForm)
  ElMessage.success('Chat Model 配置已保存（实时生效）')
}

async function saveVector() {
  await saveAiConfig(vectorForm)
  ElMessage.success('Vector Model 配置已保存（实时生效）')
}

async function testChatConnection() {
  chatTesting.value = true
  try {
    const res = await testAiConnection({
      apiUrl: chatForm.apiUrl,
      apiKey: chatForm.apiKey,
      modelName: chatForm.modelName
    })
    if (res.data) {
      ElMessage.success('Chat Model 连接测试成功！')
    } else {
      ElMessage.error('Chat Model 连接测试失败')
    }
  } catch {
    ElMessage.error('Chat Model 连接测试失败')
  } finally {
    chatTesting.value = false
  }
}

async function testVectorConnection() {
  vectorTesting.value = true
  try {
    const res = await testAiConnection({
      apiUrl: vectorForm.apiUrl,
      apiKey: vectorForm.apiKey,
      modelName: vectorForm.modelName
    })
    if (res.data) {
      ElMessage.success('Vector Model 连接测试成功！')
    } else {
      ElMessage.error('Vector Model 连接测试失败')
    }
  } catch {
    ElMessage.error('Vector Model 连接测试失败')
  } finally {
    vectorTesting.value = false
  }
}

onMounted(fetchConfig)
</script>
