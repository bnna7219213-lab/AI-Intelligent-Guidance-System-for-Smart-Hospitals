import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { role: 'admin' },
    children: [
      { path: '', redirect: '/admin/users' },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/Users.vue'), meta: { title: '用户管理' } },
      { path: 'departments', name: 'AdminDepartments', component: () => import('@/views/admin/Departments.vue'), meta: { title: '科室管理' } },
      { path: 'doctors', name: 'AdminDoctors', component: () => import('@/views/admin/Doctors.vue'), meta: { title: '医生管理' } },
      { path: 'schedules', name: 'AdminSchedules', component: () => import('@/views/admin/Schedules.vue'), meta: { title: '排班管理' } },
      { path: 'ai-config', name: 'AdminAiConfig', component: () => import('@/views/admin/AiConfig.vue'), meta: { title: 'AI配置' } },
      { path: 'prompts', name: 'AdminPrompts', component: () => import('@/views/admin/Prompts.vue'), meta: { title: '提示词管理' } },
      { path: 'knowledge-base', name: 'AdminKnowledgeBase', component: () => import('@/views/admin/KnowledgeBase.vue'), meta: { title: '知识库管理' } },
      { path: 'mcp-tools', name: 'AdminMcpTools', component: () => import('@/views/admin/McpTools.vue'), meta: { title: 'MCP工具' } },
      { path: 'symptom-tags', name: 'AdminSymptomTags', component: () => import('@/views/admin/SymptomTags.vue'), meta: { title: '症状标签' } },
      { path: 'operations', name: 'AdminOperations', component: () => import('@/views/admin/Operations.vue'), meta: { title: '运营分析' } },
      { path: 'observability', name: 'AdminObservability', component: () => import('@/views/admin/Observability.vue'), meta: { title: '系统监控' } }
    ]
  },
  {
    path: '/doctor',
    component: () => import('@/layouts/DoctorLayout.vue'),
    meta: { role: 'doctor' },
    children: [
      { path: '', redirect: '/doctor/patients' },
      { path: 'patients', name: 'DoctorPatients', component: () => import('@/views/doctor/Patients.vue'), meta: { title: '患者列表' } },
      { path: 'medical-records', name: 'DoctorMedicalRecords', component: () => import('@/views/doctor/MedicalRecords.vue'), meta: { title: '病历管理' } }
    ]
  },
  {
    path: '/patient',
    component: () => import('@/layouts/PatientLayout.vue'),
    meta: { role: 'patient' },
    children: [
      { path: '', redirect: '/patient/profile' },
      { path: 'profile', name: 'PatientProfile', component: () => import('@/views/patient/Profile.vue'), meta: { title: '个人中心' } },
      { path: 'consultation', name: 'PatientConsultation', component: () => import('@/views/patient/Consultation.vue'), meta: { title: 'AI问诊' } },
      { path: 'triage', name: 'PatientTriage', component: () => import('@/views/patient/Triage.vue'), meta: { title: '智能分诊' } },
      { path: 'triage-result', name: 'PatientTriageResult', component: () => import('@/views/patient/TriageResult.vue'), meta: { title: '分诊结果' } },
      { path: 'registration', name: 'PatientRegistration', component: () => import('@/views/patient/Registration.vue'), meta: { title: '预约挂号' } },
      { path: 'my-visits', name: 'PatientMyVisits', component: () => import('@/views/patient/MyVisits.vue'), meta: { title: '我的就诊' } }
    ]
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.public) {
    next()
  } else if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
