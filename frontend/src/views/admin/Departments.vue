<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">科室管理</h2>
      <el-button type="primary" @click="openDialog()">新增科室</el-button>
    </div>

    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="科室名称" min-width="150" />
        <el-table-column prop="code" label="科室编码" width="130" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
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
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- CRUD Dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑科室' : '新增科室'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="科室名称" prop="name">
          <el-input v-model="form.name" placeholder="如：内科" />
        </el-form-item>
        <el-form-item label="科室编码" prop="code">
          <el-input v-model="form.code" placeholder="如：INTERNAL" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="科室简介" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDepartmentList,
  createDepartment,
  updateDepartment,
  deleteDepartment,
  toggleDepartmentStatus
} from '@/api/admin'

const loading = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, name: '', code: '', description: '' })

const rules = {
  name: [{ required: true, message: '请输入科室名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入科室编码', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getDepartmentList()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row, val) {
  try {
    await toggleDepartmentStatus(row.id, val)
    ElMessage.success('状态已更新')
  } catch {
    row.status = val === 1 ? 0 : 1
  }
}

function openDialog(row) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: null, name: '', code: '', description: '' })
  }
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.id) {
    await updateDepartment(form.id, form)
    ElMessage.success('更新成功')
  } else {
    await createDepartment(form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除科室「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteDepartment(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>
