<template>
  <div class="consultation-page">
    <!-- Session History Sidebar -->
    <div class="session-sidebar">
      <div class="sidebar-header">
        <span>历史会话</span>
        <el-button type="primary" link @click="newSession">新建会话</el-button>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.sessionId"
          :class="['session-item', { active: currentSessionId === session.sessionId }]"
          @click="switchSession(session.sessionId)"
        >
          <div class="session-title">{{ session.title || '新会话' }}</div>
          <div class="session-time">{{ session.createTime }}</div>
        </div>
        <el-empty v-if="!sessions.length" description="暂无会话" :image-size="60" />
      </div>
    </div>

    <!-- Chat Area -->
    <div class="chat-area">
      <div class="chat-messages" ref="messagesRef">
        <div v-if="!messages.length" class="chat-empty">
          <div style="font-size: 48px;">🤖</div>
          <p>您好！我是AI智能导诊助手</p>
          <p class="hint">请描述您的症状，我将为您提供健康咨询</p>
        </div>
        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          :class="['message-bubble', msg.role === 'USER' ? 'message-user' : 'message-ai']"
        >
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-time">{{ msg.time }}</div>
        </div>
      </div>

      <div class="chat-input-area">
        <el-button
          v-if="streaming"
          type="danger"
          @click="stopStream"
        >
          停止生成
        </el-button>
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          placeholder="请输入您的症状或问题...（Enter发送，Shift+Enter换行）"
          @keydown.enter.exact.prevent="handleSend"
        />
        <div class="input-actions">
          <el-button
            type="primary"
            :loading="streaming"
            :disabled="!inputText.trim()"
            @click="handleSend"
          >
            {{ streaming ? '生成中...' : '发送' }}
          </el-button>
          <el-button
            v-if="messages.length && !streaming"
            type="success"
            @click="goToTriage"
          >
            前往分诊
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { streamChat } from '@/utils/sse'
import { getConsultationSessions } from '@/api/patient'

const router = useRouter()
const messages = ref([])
const sessions = ref([])
const currentSessionId = ref(null)
const inputText = ref('')
const streaming = ref(false)
const messagesRef = ref(null)
let abortController = null

function getCurrentTime() {
  return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function fetchSessions() {
  try {
    const res = await getConsultationSessions()
    sessions.value = res.data || []
  } catch {
    // silent
  }
}

function newSession() {
  currentSessionId.value = null
  messages.value = []
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  streaming.value = false
}

function switchSession(sessionId) {
  currentSessionId.value = sessionId
  // In a full implementation, load messages for this session
  messages.value = []
}

async function handleSend() {
  if (!inputText.value.trim() || streaming.value) return

  const userMsg = inputText.value.trim()
  messages.value.push({ role: 'USER', content: userMsg, time: getCurrentTime() })
  messages.value.push({ role: 'ASSISTANT', content: '', time: getCurrentTime() })
  inputText.value = ''
  streaming.value = true

  await scrollToBottom()

  try {
    abortController = streamChat(
      userMsg,
      currentSessionId.value,
      (chunk) => {
        // Real streaming - append content word by word
        const lastMsg = messages.value[messages.value.length - 1]
        if (lastMsg && lastMsg.role === 'ASSISTANT') {
          lastMsg.content += chunk
          scrollToBottom()
        }
      },
      (error) => {
        ElMessage.error(`流式连接错误: ${error.message}`)
        streaming.value = false
        abortController = null
      },
      () => {
        streaming.value = false
        abortController = null
        fetchSessions()
      }
    )
  } catch (error) {
    streaming.value = false
  }
}

function stopStream() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  streaming.value = false
}

function goToTriage() {
  router.push('/patient/triage')
}

async function scrollToBottom() {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

onMounted(fetchSessions)
</script>

<style scoped>
.consultation-page {
  display: flex;
  height: calc(100vh - 100px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
.session-sidebar {
  width: 240px;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.session-list {
  flex: 1;
  overflow-y: auto;
}
.session-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f7fa;
}
.session-item:hover {
  background: #f5f7fa;
}
.session-item.active {
  background: #ecf5ff;
  border-left: 3px solid #409eff;
}
.session-title {
  font-size: 13px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-time {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
.chat-empty {
  text-align: center;
  padding-top: 80px;
  color: #909399;
}
.chat-empty .hint {
  font-size: 12px;
  margin-top: 8px;
}
.message-bubble {
  margin-bottom: 16px;
  max-width: 70%;
}
.message-user {
  margin-left: auto;
  text-align: right;
}
.message-user .message-content {
  background: #409eff;
  color: #fff;
  border-radius: 12px 12px 4px 12px;
}
.message-ai .message-content {
  background: #f5f7fa;
  color: #303133;
  border-radius: 12px 12px 12px 4px;
}
.message-content {
  padding: 12px 16px;
  display: inline-block;
  text-align: left;
  word-break: break-word;
  line-height: 1.5;
}
.message-time {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}
.chat-input-area {
  border-top: 1px solid #ebeef5;
  padding: 16px;
}
.input-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
