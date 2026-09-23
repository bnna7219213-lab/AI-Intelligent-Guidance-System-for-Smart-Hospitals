import request from '@/utils/request'

// ===== Patients =====
export function getPatientList(params) {
  return request({ url: '/doctor/patients', method: 'get', params })
}

// ===== Registrations =====
export function startConsultation(id) {
  return request({ url: `/doctor/registrations/${id}/start`, method: 'put' })
}

export function completeConsultation(id) {
  return request({ url: `/doctor/registrations/${id}/complete`, method: 'put' })
}

// ===== Medical Records =====
export function getMedicalRecord(registrationId) {
  return request({ url: `/doctor/medical-records/${registrationId}`, method: 'get' })
}

export function saveMedicalRecord(data) {
  return request({ url: '/doctor/medical-records', method: 'post', data })
}
