<template>
  <div class="erm-page">
    <div class="head">
      <h2>风险评估记录</h2>
      <el-button type="primary" @click="dlg = true">新建评估</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="framework" label="框架" width="120" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="高中低" width="120">
        <template #default="{ row }">
          {{ row.highRiskCount }} / {{ row.mediumRiskCount }} / {{ row.lowRiskCount }}
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新" width="200" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push('/erm/assessments/' + row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" title="新建评估" width="480px">
      <el-form label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="框架"><el-input v-model="form.framework" placeholder="默认 GB/T 20984" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../../api/http'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const dlg = ref(false)
const form = reactive({ title: '', framework: '' })

async function load() {
  loading.value = true
  try {
    const { data } = await http.get('/erm/assessments', { params: { page: 0, size: 50 } })
    if (data.code === 1) rows.value = data.data || []
  } finally {
    loading.value = false
  }
}

async function create() {
  const { data } = await http.post('/erm/assessments', { title: form.title, framework: form.framework || null })
  if (data.code !== 1) return ElMessage.error(data.msg)
  dlg.value = false
  form.title = ''
  form.framework = ''
  ElMessage.success('已创建')
  const id = data.data.id
  router.push('/erm/assessments/' + id)
}

onMounted(load)
</script>

<style scoped>
.erm-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
