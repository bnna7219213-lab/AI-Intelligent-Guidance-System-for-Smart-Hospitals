import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const role = ref(localStorage.getItem('role') || '')
  const username = ref(localStorage.getItem('username') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'admin')
  const isDoctor = computed(() => role.value === 'doctor')
  const isPatient = computed(() => role.value === 'patient')

  function login(userData) {
    token.value = userData.token
    userId.value = userData.userId
    role.value = userData.role
    username.value = userData.username
    localStorage.setItem('token', userData.token)
    localStorage.setItem('userId', userData.userId)
    localStorage.setItem('role', userData.role)
    localStorage.setItem('username', userData.username)
  }

  function logout() {
    token.value = ''
    userId.value = ''
    role.value = ''
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('role')
    localStorage.removeItem('username')
  }

  function getUserHome() {
    switch (role.value) {
      case 'admin': return '/admin'
      case 'doctor': return '/doctor'
      case 'patient': return '/patient'
      default: return '/login'
    }
  }

  return {
    token,
    userId,
    role,
    username,
    isLoggedIn,
    isAdmin,
    isDoctor,
    isPatient,
    login,
    logout,
    getUserHome
  }
})
