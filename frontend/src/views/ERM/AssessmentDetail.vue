<template>
  <div class="erm-page" v-loading="loading">
    <template v-if="detail">
      <div class="head">
        <h2>{{ detail.assessment.title }}</h2>
        <el-button @click="$router.push('/erm/assessments')">返回列表</el-button>
      </div>
      <el-descriptions :column="2" border size="small" class="mb">
        <el-descriptions-item label="ID">{{ detail.assessment.id }}</el-descriptions-item>
        <el-descriptions-item label="框架">{{ detail.assessment.framework }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-select v-model="status" style="width: 160px" @change="patchMeta">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="会话 chatId">{{ detail.assessment.chatId || '（未绑定）' }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">
          <el-input type="textarea" v-model="summary" :rows="2" @blur="patchMeta" />
        </el-descriptions-item>
      </el-descriptions>

      <el-card header="关联识别要素（本评估范围）" class="mb">
        <el-form label-width="100px">
          <el-form-item label="资产">
            <el-select v-model="linkSel.assets" multiple filterable style="width: 100%" placeholder="选择资产">
              <el-option v-for="a in allAssets" :key="a.id" :label="a.name + ' (#' + a.id + ')'" :value="a.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="威胁">
            <el-select v-model="linkSel.threats" multiple filterable style="width: 100%">
              <el-option v-for="t in allThreats" :key="t.id" :label="t.name + ' (#' + t.id + ')'" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="脆弱性">
            <el-select v-model="linkSel.vulns" multiple filterable style="width: 100%">
              <el-option v-for="v in allVulns" :key="v.id" :label="v.name + ' (#' + v.id + ')'" :value="v.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="安全措施">
            <el-select v-model="linkSel.measures" multiple filterable style="width: 100%">
              <el-option v-for="m in allMeasures" :key="m.id" :label="m.name + ' (#' + m.id + ')'" :value="m.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveLinks">保存关联</el-button>
            <el-button @click="goAgent">打开智能分析</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card header="风险分析与等级（Likelihood × Impact）" class="mb">
        <el-table :data="detail.assessedRisks" size="small" border>
          <el-table-column prop="title" label="描述" />
          <el-table-column prop="likelihood" label="L" width="60" />
          <el-table-column prop="impact" label="I" width="60" />
          <el-table-column prop="riskLevel" label="等级" width="90" />
          <el-table-column prop="treatment" label="处置" width="100" />
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button link type="danger" @click="delRisk(row)">删</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="add-risk">
          <el-input v-model="riskForm.title" placeholder="风险描述" style="flex: 1" />
          <el-input-number v-model="riskForm.likelihood" :min="1" :max="5" />
          <el-input-number v-model="riskForm.impact" :min="1" :max="5" />
          <el-input v-model="riskForm.treatment" placeholder="处置建议" style="width: 160px" />
          <el-button type="primary" @click="addRisk">添加</el-button>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api/http'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const detail = ref(null)
const status = ref('DRAFT')
const summary = ref('')

const allAssets = ref([])
const allThreats = ref([])
const allVulns = ref([])
const allMeasures = ref([])

const linkSel = reactive({
  assets: [],
  threats: [],
  vulns: [],
  measures: []
})

const riskForm = reactive({
  title: '',
  likelihood: 3,
  impact: 3,
  treatment: '',
  notes: ''
})

watch(
  () => route.params.id,
  () => loadAll()
)

async function loadAll() {
  loading.value = true
  try {
    const id = route.params.id
    const [d, a, t, v, m] = await Promise.all([
      http.get(`/erm/assessments/${id}`),
      http.get('/erm/assets'),
      http.get('/erm/threats'),
      http.get('/erm/vulnerabilities'),
      http.get('/erm/measures')
    ])
    if (d.data.code !== 1) {
      ElMessage.error(d.data.msg)
      return
    }
    detail.value = d.data.data
    status.value = detail.value.assessment.status
    summary.value = detail.value.assessment.summary || ''
    linkSel.assets = [...(detail.value.assetIds || [])]
    linkSel.threats = [...(detail.value.threatIds || [])]
    linkSel.vulns = [...(detail.value.vulnerabilityIds || [])]
    linkSel.measures = [...(detail.value.measureIds || [])]

    if (a.data.code === 1) allAssets.value = a.data.data || []
    if (t.data.code === 1) allThreats.value = t.data.data || []
    if (v.data.code === 1) allVulns.value = v.data.data || []
    if (m.data.code === 1) allMeasures.value = m.data.data || []

    const aid = detail.value.assessment.id
    if (!detail.value.assessment.chatId) {
      const chatId = `erm-assess-${aid}`
      await http.patch(`/erm/assessments/${aid}`, { chatId })
      detail.value.assessment.chatId = chatId
    }
  } finally {
    loading.value = false
  }
}

async function patchMeta() {
  const id = route.params.id
  const { data } = await http.patch(`/erm/assessments/${id}`, {
    status: status.value,
    summary: summary.value
  })
  if (data.code !== 1) return ElMessage.error(data.msg)
  ElMessage.success('已更新')
}

async function saveLinks() {
  const id = route.params.id
  const { data } = await http.put(`/erm/assessments/${id}/links`, {
    assetIds: linkSel.assets,
    threatIds: linkSel.threats,
    vulnerabilityIds: linkSel.vulns,
    measureIds: linkSel.measures
  })
  if (data.code !== 1) return ElMessage.error(data.msg)
  ElMessage.success('关联已保存')
}

function goAgent() {
  const c = detail.value?.assessment?.chatId
  router.push({ path: '/erm/agent', query: { chatId: c } })
}

async function addRisk() {
  const id = route.params.id
  const { data } = await http.post(`/erm/assessments/${id}/risks`, {
    title: riskForm.title,
    likelihood: riskForm.likelihood,
    impact: riskForm.impact,
    notes: riskForm.notes,
    treatment: riskForm.treatment
  })
  if (data.code !== 1) return ElMessage.error(data.msg)
  riskForm.title = ''
  ElMessage.success('已添加')
  loadAll()
}

async function delRisk(row) {
  await ElMessageBox.confirm('删除该风险项？', '确认')
  const id = route.params.id
  const { data } = await http.delete(`/erm/assessments/${id}/risks/${row.id}`)
  if (data.code !== 1) return ElMessage.error(data.msg)
  loadAll()
}

onMounted(loadAll)
</script>

<style scoped>
.erm-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 16px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mb {
  margin-bottom: 16px;
}
.add-risk {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  align-items: center;
}
</style>
