<template>
  <div class="page-container">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchKey"
          placeholder="搜索用户名/姓名"
          clearable
          style="width: 200px"
          @keyup.enter="fetchData"
        />
        <el-select v-model="roleFilter" placeholder="角色筛选" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="管理员" value="admin" />
          <el-option label="医生" value="doctor" />
          <el-option label="患者" value="patient" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="success" @click="openCreateDoctor">创建医生账号</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="userList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="val => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="warning" link @click="handleResetPwd(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pagination"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- Create Doctor Dialog -->
    <el-dialog v-model="dialogVisible" title="创建医生账号" width="500px">
      <el-form ref="doctorFormRef" :model="doctorForm" :rules="doctorRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="doctorForm.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="doctorForm.password" type="password" placeholder="登录密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="doctorForm.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="科室" prop="departmentId">
          <el-select v-model="doctorForm.departmentId" placeholder="请选择科室" style="width: 100%">
            <el-option
              v-for="dept in departments"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="职称" prop="title">
          <el-input v-model="doctorForm.title" placeholder="如：主任医师" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDoctor">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUserList,
  toggleUserStatus,
  createDoctor,
  resetPassword,
  getDepartmentList
} from '@/api/admin'

const loading = ref(false)
const userList = ref([])
const searchKey = ref('')
const roleFilter = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const dialogVisible = ref(false)
const doctorFormRef = ref(null)
const departments = ref([])
const doctorForm = reactive({
  username: '',
  password: '',
  realName: '',
  departmentId: null,
  title: ''
})

const doctorRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择科室', trigger: 'change' }],
  title: [{ required: true, message: '请输入职称', trigger: 'blur' }]
}

function roleLabel(role) {
  const map = { admin: '管理员', doctor: '医生', patient: '患者' }
  return map[role] || role
}

function roleTagType(role) {
  const map = { admin: 'danger', doctor: 'warning', patient: 'success' }
  return map[role] || ''
}

async function fetchData() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchKey.value) params.keyword = searchKey.value
    const res = await getUserList(params)
    userList.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row, val) {
  try {
    await toggleUserStatus(row.id, val)
    ElMessage.success('状态已更新')
  } catch {
    row.status = val === 1 ? 0 : 1
  }
}

async function handleResetPwd(row) {
  await ElMessageBox.confirm(`确定要重置用户 ${row.username} 的密码吗？`, '提示', { type: 'warning' })
  await resetPassword(row.id)
  ElMessage.success('密码已重置')
}

async function openCreateDoctor() {
  doctorForm.username = ''
  doctorForm.password = ''
  doctorForm.realName = ''
  doctorForm.departmentId = null
  doctorForm.title = ''
  dialogVisible.value = true
  if (!departments.value.length) {
    const res = await getDepartmentList()
    departments.value = res.data || []
  }
}

async function submitDoctor() {
  const valid = await doctorFormRef.value.validate().catch(() => false)
  if (!valid) return
  await createDoctor(doctorForm)
  ElMessage.success('医生账号创建成功')
  dialogVisible.value = false
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
