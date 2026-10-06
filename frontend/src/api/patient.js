import request from '@/utils/request'

// ===== Profile =====
export function getProfile() {
  return request({ url: '/patient/profile', method: 'get' })
}

export function updateProfile(data) {
  return request({ url: '/patient/profile', method: 'put', data })
}

// ===== Consultation =====
export function getConsultationSessions(params) {
  return request({ url: '/patient/consultations', method: 'get', params })
}

// ===== Triage =====
export function submitTriage(data) {
  return request({ url: '/patient/triage', method: 'post', data })
}

// ===== Schedules =====
export function getAvailableSchedules(params) {
  return request({ url: '/patient/schedules', method: 'get', params })
}

// ===== Registrations =====
export function createRegistration(data) {
  return request({ url: '/patient/registrations', method: 'post', data })
}

export function getMyRegistrations() {
  return request({ url: '/patient/registrations', method: 'get' })
}

export function cancelRegistration(id) {
  return request({ url: `/patient/registrations/${id}/cancel`, method: 'put' })
}

export function getRegistrationRecord(id) {
  return request({ url: `/patient/registrations/${id}/record`, method: 'get' })
}
