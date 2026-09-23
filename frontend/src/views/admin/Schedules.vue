<template>
  <div class="page-container">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="fetchData"
        />
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="warning" @click="handleShiftDates">排班日期顺延</el-button>
        <el-button type="success" @click="openDialog()">新建排班</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="doctorName" label="医生" width="120" />
        <el-table-column prop="departmentName" label="科室" width="140" />
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="period" label="时段" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.period }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总数" width="80" />
        <el-table-column prop="remainCount" label="剩余" width="80" />
        <el-table-column prop="fee" label="挂号费" width="100">
          <template #default="{ row }">¥{{ row.fee }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Create Schedule Dialog -->
    <el-dialog v-model="dialogVisible" title="新建排班" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="医生" prop="doctorId">
          <el-select v-model="form.doctorId" placeholder="请选择医生" style="width: 100%" @change="onDoctorChange">
            <el-option
              v-for="doc in doctors"
              :key="doc.id"
              :label="`${doc.realName} (${doc.title || ''})`"
              :value="doc.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="form.date" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时段" prop="period">
          <el-select v-model="form.period" placeholder="请选择时段" style="width: 100%">
            <el-option label="上午" value="上午" />
            <el-option label="下午" value="下午" />
          </el-select>
        </el-form-item>
        <el-form-item label="号源数" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="挂号费" prop="fee">
          <el-input-number v-model="form.fee" :min="0" :precision="2" />
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
  getAdminScheduleList,
  createSchedule,
  deleteSchedule,
  shiftScheduleDates,
  getAdminDoctorList,
  getDepartmentList
} from '@/api/admin'

const loading = ref(false)
const list = ref([])
const dateRange = ref(null)
const dialogVisible = ref(false)
const formRef = ref(null)
const doctors = ref([])
const departments = ref([])

const form = reactive({
  doctorId: null,
  departmentId: null,
  date: '',
  period: '',
  totalCount: 20,
  fee: 10.0
})

const rules = {
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  period: [{ required: true, message: '请选择时段', trigger: 'change' }],
  totalCount: [{ required: true, message: '请输入号源数', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getAdminScheduleList(params)
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function onDoctorChange(doctorId) {
  const doc = doctors.value.find(d => d.id === doctorId)
  if (doc) form.departmentId = doc.departmentId
}

async function openDialog() {
  form.doctorId = null
  form.departmentId = null
  form.date = ''
  form.period = ''
  form.totalCount = 20
  form.fee = 10.0
  dialogVisible.value = true
  if (!doctors.value.length) {
    const res = await getAdminDoctorList()
    doctors.value = res.data || []
  }
  if (!departments.value.length) {
    const res = await getDepartmentList()
    departments.value = res.data || []
  }
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  await createSchedule(form)
  ElMessage.success('排班创建成功')
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该排班记录吗？', '提示', { type: 'warning' })
  await deleteSchedule(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

async function handleShiftDates() {
  await ElMessageBox.confirm(
    '确定执行排班日期顺延吗？这将把所有未来排班的日期向后推移一天。',
    '排班日期顺延',
    { type: 'warning' }
  )
  await shiftScheduleDates()
  ElMessage.success('排班日期已顺延')
  fetchData()
}

onMounted(fetchData)
</script>
