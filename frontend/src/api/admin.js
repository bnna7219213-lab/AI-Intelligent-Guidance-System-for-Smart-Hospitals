import request from '@/utils/request'

// ===== Users =====
export function getUserList(params) {
  return request({ url: '/admin/users', method: 'get', params })
}

export function toggleUserStatus(id, status) {
  return request({ url: `/admin/users/${id}/status`, method: 'put', params: { status } })
}

export function createDoctor(data) {
  return request({ url: '/admin/users/doctor', method: 'post', data })
}

export function resetPassword(id) {
  return request({ url: `/admin/users/${id}/reset-pwd`, method: 'put' })
}

// ===== Departments =====
export function getDepartmentList() {
  return request({ url: '/admin/departments', method: 'get' })
}

export function createDepartment(data) {
  return request({ url: '/admin/departments', method: 'post', data })
}

export function updateDepartment(id, data) {
  return request({ url: `/admin/departments/${id}`, method: 'put', data })
}

export function deleteDepartment(id) {
  return request({ url: `/admin/departments/${id}`, method: 'delete' })
}

export function toggleDepartmentStatus(id, status) {
  return request({ url: `/admin/departments/${id}/status`, method: 'put', params: { status } })
}

// ===== Doctors =====
export function getAdminDoctorList(params) {
  return request({ url: '/admin/doctors', method: 'get', params })
}

// ===== Schedules =====
export function getAdminScheduleList(params) {
  return request({ url: '/admin/schedules', method: 'get', params })
}

export function createSchedule(data) {
  return request({ url: '/admin/schedules', method: 'post', data })
}

export function deleteSchedule(id) {
  return request({ url: `/admin/schedules/${id}`, method: 'delete' })
}

export function shiftScheduleDates() {
  return request({ url: '/admin/schedules/shift-dates', method: 'post' })
}

// ===== AI Config =====
export function getAiConfigs() {
  return request({ url: '/admin/ai-configs', method: 'get' })
}

export function saveAiConfig(data) {
  return request({ url: '/admin/ai-configs', method: 'put', data })
}

export function testAiConnection(data) {
  return request({ url: '/admin/ai-configs/test', method: 'post', data })
}

// ===== Prompts =====
export function getPromptList() {
  return request({ url: '/admin/prompts', method: 'get' })
}

export function updatePrompt(data) {
  return request({ url: '/admin/prompts', method: 'put', data })
}

// ===== Knowledge Base =====
export function getKbGroups() {
  return request({ url: '/admin/kb/groups', method: 'get' })
}

export function createKbGroup(data) {
  return request({ url: '/admin/kb/groups', method: 'post', data })
}

export function uploadKbDocument(data) {
  return request({ url: '/admin/kb/documents', method: 'post', data })
}

export function embedKbDocument(id) {
  return request({ url: `/admin/kb/documents/${id}/embed`, method: 'post' })
}

export function searchKb(data) {
  return request({ url: '/admin/kb/search', method: 'post', data })
}

// ===== MCP Tools =====
export function getMcpToolList() {
  return request({ url: '/admin/mcp-tools', method: 'get' })
}

export function createMcpTool(data) {
  return request({ url: '/admin/mcp-tools', method: 'post', data })
}

export function toggleMcpTool(id, status) {
  return request({ url: `/admin/mcp-tools/${id}/toggle`, method: 'put', params: { status } })
}

export function getMcpToolCallLogs(params) {
  return request({ url: '/admin/mcp-tools/logs', method: 'get', params })
}

// ===== Symptom Tags =====
export function getSymptomTagList() {
  return request({ url: '/admin/symptom-tags', method: 'get' })
}

export function createSymptomTag(data) {
  return request({ url: '/admin/symptom-tags', method: 'post', data })
}

export function updateSymptomTag(id, data) {
  return request({ url: `/admin/symptom-tags/${id}`, method: 'put', data })
}

export function deleteSymptomTag(id) {
  return request({ url: `/admin/symptom-tags/${id}`, method: 'delete' })
}

// ===== Operations =====
export function getRegistrationStats(params) {
  return request({ url: '/admin/operations/registrations', method: 'get', params })
}

export function getSchedulingUsage() {
  return request({ url: '/admin/operations/scheduling-usage', method: 'get' })
}

export function getTriageHitRate() {
  return request({ url: '/admin/operations/triage-hit-rate', method: 'get' })
}

// ===== Observability =====
export function getAiUsageStats() {
  return request({ url: '/admin/observability/ai-usage', method: 'get' })
}

export function getAgentRuns(params) {
  return request({ url: '/admin/observability/agent-runs', method: 'get', params })
}

export function getAgentRunDetail(id) {
  return request({ url: `/admin/observability/agent-runs/${id}`, method: 'get' })
}
