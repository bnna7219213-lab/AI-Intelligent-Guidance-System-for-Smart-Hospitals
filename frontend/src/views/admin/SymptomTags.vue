<template>
  <div class="page-container">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">症状标签</h2>
      <el-button type="primary" @click="openDialog()">新增标签</el-button>
    </div>

    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="标签名称" min-width="130" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="relatedDepartments" label="关联科室" min-width="150" show-overflow-tooltip />
        <el-table-column prop="weight" label="权重" width="80" />
        <el-table-column label="危险标志" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isRedFlag" type="danger" size="small">危重征象</el-tag>
            <el-tag v-else type="info" size="small">一般</el-tag>
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑标签' : '新增标签'" width="550px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="如：胸痛" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="症状描述" />
        </el-form-item>
        <el-form-item label="关联科室">
          <el-input v-model="form.relatedDepartments" placeholder="如：心内科,急诊科（逗号分隔）" />
        </el-form-item>
        <el-form-item label="权重">
          <el-input-number v-model="form.weight" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="危重征象">
          <el-switch v-model="form.isRedFlag" :active-value="1" :inactive-value="0" />
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
  getSymptomTagList,
  createSymptomTag,
  updateSymptomTag,
  deleteSymptomTag
} from '@/api/admin'

const loading = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  name: '',
  description: '',
  relatedDepartments: '',
  weight: 50,
  isRedFlag: 0
})

const rules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getSymptomTagList()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: null, name: '', description: '', relatedDepartments: '', weight: 50, isRedFlag: 0 })
  }
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.id) {
    await updateSymptomTag(form.id, form)
    ElMessage.success('更新成功')
  } else {
    await createSymptomTag(form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除标签「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteSymptomTag(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>
