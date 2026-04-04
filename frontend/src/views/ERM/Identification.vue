<template>
  <div class="erm-page">
    <h2>风险识别（GB/T 20984）</h2>
    <p class="sub">资产、威胁、脆弱性、已有安全措施；数据按租户隔离。</p>
    <el-tabs v-model="tab" @tab-change="onTab">
      <el-tab-pane label="资产" name="assets">
        <div class="toolbar">
          <el-button type="primary" @click="openAsset()">新增</el-button>
          <el-button @click="loadAssets">刷新</el-button>
        </div>
        <el-table :data="assets" v-loading="loading" border size="small" class="mt8">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="category" label="类别" width="120" />
          <el-table-column prop="criticality" label="重要度" width="90" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openAsset(row)">编辑</el-button>
              <el-button link type="danger" @click="delAsset(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="威胁" name="threats">
        <div class="toolbar">
          <el-button type="primary" @click="openThreat()">新增</el-button>
          <el-button @click="loadThreats">刷新</el-button>
        </div>
        <el-table :data="threats" v-loading="loading" border size="small" class="mt8">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="category" label="类别" width="140" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openThreat(row)">编辑</el-button>
              <el-button link type="danger" @click="delThreat(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="脆弱性" name="vulns">
        <div class="toolbar">
          <el-button type="primary" @click="openVuln()">新增</el-button>
          <el-button @click="loadVulns">刷新</el-button>
        </div>
        <el-table :data="vulns" v-loading="loading" border size="small" class="mt8">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="severity" label="严重度" width="100" />
          <el-table-column prop="relatedAssetId" label="关联资产" width="100" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openVuln(row)">编辑</el-button>
              <el-button link type="danger" @click="delVuln(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="安全措施" name="measures">
        <div class="toolbar">
          <el-button type="primary" @click="openMeasure()">新增</el-button>
          <el-button @click="loadMeasures">刷新</el-button>
        </div>
        <el-table :data="measures" v-loading="loading" border size="small" class="mt8">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="measureType" label="类型" width="140" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openMeasure(row)">编辑</el-button>
              <el-button link type="danger" @click="delMeasure(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dlg.asset" :title="assetForm.id ? '编辑资产' : '新增资产'" width="520px">
      <el-form label-width="100px">
        <el-form-item label="名称"><el-input v-model="assetForm.name" /></el-form-item>
        <el-form-item label="类别"><el-input v-model="assetForm.category" /></el-form-item>
        <el-form-item label="重要度1-5"><el-input-number v-model="assetForm.criticality" :min="1" :max="5" /></el-form-item>
        <el-form-item label="描述"><el-input type="textarea" v-model="assetForm.description" :rows="3" /></el-form-item>
        <el-form-item label="责任人"><el-input v-model="assetForm.ownerLabel" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="assetForm.locationLabel" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.asset = false">取消</el-button>
        <el-button type="primary" @click="saveAsset">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dlg.threat" :title="threatForm._id ? '编辑威胁' : '新增威胁'" width="520px">
      <el-form label-width="100px">
        <el-form-item label="名称"><el-input v-model="threatForm.name" /></el-form-item>
        <el-form-item label="类别"><el-input v-model="threatForm.category" placeholder="STRIDE / 人为 / 自然 …" /></el-form-item>
        <el-form-item label="描述"><el-input type="textarea" v-model="threatForm.description" :rows="3" /></el-form-item>
        <el-form-item label="来源"><el-input v-model="threatForm.sourceLabel" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.threat = false">取消</el-button>
        <el-button type="primary" @click="saveThreat">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dlg.vuln" :title="vulnForm._id ? '编辑脆弱性' : '新增脆弱性'" width="520px">
      <el-form label-width="100px">
        <el-form-item label="名称"><el-input v-model="vulnForm.name" /></el-form-item>
        <el-form-item label="严重度"><el-input v-model="vulnForm.severity" /></el-form-item>
        <el-form-item label="描述"><el-input type="textarea" v-model="vulnForm.description" :rows="3" /></el-form-item>
        <el-form-item label="关联资产ID"><el-input-number v-model="vulnForm.relatedAssetId" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.vuln = false">取消</el-button>
        <el-button type="primary" @click="saveVuln">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dlg.measure" :title="measureForm._id ? '编辑措施' : '新增措施'" width="520px">
      <el-form label-width="100px">
        <el-form-item label="名称"><el-input v-model="measureForm.name" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="measureForm.measureType" /></el-form-item>
        <el-form-item label="描述"><el-input type="textarea" v-model="measureForm.description" :rows="3" /></el-form-item>
        <el-form-item label="有效性"><el-input type="textarea" v-model="measureForm.effectivenessNote" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.measure = false">取消</el-button>
        <el-button type="primary" @click="saveMeasure">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api/http'

const tab = ref('assets')
const loading = ref(false)
const assets = ref([])
const threats = ref([])
const vulns = ref([])
const measures = ref([])

const dlg = reactive({ asset: false, threat: false, vuln: false, measure: false })
const assetForm = reactive({ id: null, name: '', category: '', criticality: 3, description: '', ownerLabel: '', locationLabel: '' })
const threatForm = reactive({ _id: null, name: '', category: '', description: '', sourceLabel: '' })
const vulnForm = reactive({ _id: null, name: '', severity: '', description: '', relatedAssetId: null })
const measureForm = reactive({ _id: null, name: '', measureType: '', description: '', effectivenessNote: '' })

function onTab() {
  /* 懒加载可选 */
}

async function loadAssets() {
  loading.value = true
  try {
    const { data } = await http.get('/erm/assets')
    if (data.code === 1) assets.value = data.data || []
  } finally {
    loading.value = false
  }
}
async function loadThreats() {
  loading.value = true
  try {
    const { data } = await http.get('/erm/threats')
    if (data.code === 1) threats.value = data.data || []
  } finally {
    loading.value = false
  }
}
async function loadVulns() {
  loading.value = true
  try {
    const { data } = await http.get('/erm/vulnerabilities')
    if (data.code === 1) vulns.value = data.data || []
  } finally {
    loading.value = false
  }
}
async function loadMeasures() {
  loading.value = true
  try {
    const { data } = await http.get('/erm/measures')
    if (data.code === 1) measures.value = data.data || []
  } finally {
    loading.value = false
  }
}

function openAsset(row) {
  if (row) {
    Object.assign(assetForm, {
      id: row.id,
      name: row.name,
      category: row.category || '',
      criticality: row.criticality,
      description: row.description || '',
      ownerLabel: row.ownerLabel || '',
      locationLabel: row.locationLabel || ''
    })
  } else Object.assign(assetForm, { id: null, name: '', category: '', criticality: 3, description: '', ownerLabel: '', locationLabel: '' })
  dlg.asset = true
}
async function saveAsset() {
  const body = {
    name: assetForm.name,
    category: assetForm.category,
    criticality: assetForm.criticality,
    description: assetForm.description,
    ownerLabel: assetForm.ownerLabel,
    locationLabel: assetForm.locationLabel
  }
  if (assetForm.id) {
    const { data } = await http.put(`/erm/assets/${assetForm.id}`, body)
    if (data.code !== 1) return ElMessage.error(data.msg)
  } else {
    const { data } = await http.post('/erm/assets', body)
    if (data.code !== 1) return ElMessage.error(data.msg)
  }
  dlg.asset = false
  ElMessage.success('已保存')
  loadAssets()
}
async function delAsset(row) {
  await ElMessageBox.confirm('确定删除？', '确认')
  const { data } = await http.delete(`/erm/assets/${row.id}`)
  if (data.code !== 1) return ElMessage.error(data.msg)
  loadAssets()
}

function openThreat(row) {
  if (row) Object.assign(threatForm, { _id: row.id, name: row.name, category: row.category || '', description: row.description || '', sourceLabel: row.sourceLabel || '' })
  else Object.assign(threatForm, { _id: null, name: '', category: '', description: '', sourceLabel: '' })
  dlg.threat = true
}
async function saveThreat() {
  const body = { name: threatForm.name, category: threatForm.category, description: threatForm.description, sourceLabel: threatForm.sourceLabel }
  if (threatForm._id) {
    const { data } = await http.put(`/erm/threats/${threatForm._id}`, body)
    if (data.code !== 1) return ElMessage.error(data.msg)
  } else {
    const { data } = await http.post('/erm/threats', body)
    if (data.code !== 1) return ElMessage.error(data.msg)
  }
  dlg.threat = false
  ElMessage.success('已保存')
  loadThreats()
}
async function delThreat(row) {
  await ElMessageBox.confirm('确定删除？', '确认')
  const { data } = await http.delete(`/erm/threats/${row.id}`)
  if (data.code !== 1) return ElMessage.error(data.msg)
  loadThreats()
}

function openVuln(row) {
  if (row) {
    Object.assign(vulnForm, {
      _id: row.id,
      name: row.name,
      severity: row.severity || '',
      description: row.description || '',
      relatedAssetId: row.relatedAssetId
    })
  } else Object.assign(vulnForm, { _id: null, name: '', severity: '', description: '', relatedAssetId: null })
  dlg.vuln = true
}
async function saveVuln() {
  const body = {
    name: vulnForm.name,
    severity: vulnForm.severity,
    description: vulnForm.description,
    relatedAssetId: vulnForm.relatedAssetId && vulnForm.relatedAssetId > 0 ? vulnForm.relatedAssetId : null
  }
  if (vulnForm._id) {
    const { data } = await http.put(`/erm/vulnerabilities/${vulnForm._id}`, body)
    if (data.code !== 1) return ElMessage.error(data.msg)
  } else {
    const { data } = await http.post('/erm/vulnerabilities', body)
    if (data.code !== 1) return ElMessage.error(data.msg)
  }
  dlg.vuln = false
  ElMessage.success('已保存')
  loadVulns()
}
async function delVuln(row) {
  await ElMessageBox.confirm('确定删除？', '确认')
  const { data } = await http.delete(`/erm/vulnerabilities/${row.id}`)
  if (data.code !== 1) return ElMessage.error(data.msg)
  loadVulns()
}

function openMeasure(row) {
  if (row) {
    Object.assign(measureForm, {
      _id: row.id,
      name: row.name,
      measureType: row.measureType || '',
      description: row.description || '',
      effectivenessNote: row.effectivenessNote || ''
    })
  } else Object.assign(measureForm, { _id: null, name: '', measureType: '', description: '', effectivenessNote: '' })
  dlg.measure = true
}
async function saveMeasure() {
  const body = {
    name: measureForm.name,
    measureType: measureForm.measureType,
    description: measureForm.description,
    effectivenessNote: measureForm.effectivenessNote
  }
  if (measureForm._id) {
    const { data } = await http.put(`/erm/measures/${measureForm._id}`, body)
    if (data.code !== 1) return ElMessage.error(data.msg)
  } else {
    const { data } = await http.post('/erm/measures', body)
    if (data.code !== 1) return ElMessage.error(data.msg)
  }
  dlg.measure = false
  ElMessage.success('已保存')
  loadMeasures()
}
async function delMeasure(row) {
  await ElMessageBox.confirm('确定删除？', '确认')
  const { data } = await http.delete(`/erm/measures/${row.id}`)
  if (data.code !== 1) return ElMessage.error(data.msg)
  loadMeasures()
}

onMounted(() => {
  loadAssets()
  loadThreats()
  loadVulns()
  loadMeasures()
})
</script>

<style scoped>
.erm-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
}
.sub {
  color: #666;
}
.toolbar {
  display: flex;
  gap: 8px;
}
.mt8 {
  margin-top: 8px;
}
</style>
