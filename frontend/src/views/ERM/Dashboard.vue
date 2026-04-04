<template>
  <div class="erm-page">
    <div class="head-row">
      <h2>风险信息总览</h2>
      <div class="who" v-if="auth.displayName || auth.username">
        <span>{{ auth.displayName || auth.username }} @ {{ auth.tenantCode }}</span>
        <el-button size="small" @click="logout">退出</el-button>
      </div>
    </div>
    <p class="sub">基于 GB/T 20984 识别与评估流程的租户级指标；Agent 能力见「智能分析」模块。</p>
    <el-row :gutter="16" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" v-for="c in cards" :key="c.k">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-val">{{ c.v }}</div>
          <div class="stat-label">{{ c.label }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="mt" header="最近评估记录">
      <el-table :data="recent" size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="高中低" width="140">
          <template #default="{ row }">
            {{ row.highCount }} / {{ row.mediumCount }} / {{ row.lowCount }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新" width="200" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/erm/assessments/' + row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-card class="mt" header="说明">
      <ul class="hints">
        <li v-for="(h, i) in hints" :key="i">{{ h }}</li>
      </ul>
    </el-card>
    <div class="actions mt">
      <el-button type="primary" @click="$router.push('/erm/identification')">风险识别</el-button>
      <el-button @click="$router.push('/erm/assessments')">评估记录</el-button>
      <el-button @click="$router.push('/erm/agent')">智能分析（Agent）</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { useErmAuthStore } from '../../stores/ermAuth'

const router = useRouter()
const auth = useErmAuthStore()

const loading = ref(true)
const counts = ref(null)
const recent = ref([])
const hints = ref([])

const cards = computed(() => {
  const o = counts.value
  if (!o) return []
  return [
    { k: 'assets', v: o.assets, label: '已登记资产' },
    { k: 'threats', v: o.threats, label: '威胁场景' },
    { k: 'vul', v: o.vulnerabilities, label: '脆弱性' },
    { k: 'ms', v: o.measures, label: '安全措施' },
    { k: 'as', v: o.assessments, label: '评估记录总数' },
    { k: 'draft', v: o.assessmentsDraft, label: '草稿' },
    { k: 'prog', v: o.assessmentsInProgress, label: '进行中' },
    { k: 'done', v: o.assessmentsCompleted, label: '已完成' },
    { k: 'hi', v: o.highRiskCells, label: '高风险单元（汇总）' },
    { k: 'med', v: o.mediumRiskCells, label: '中风险单元（汇总）' },
    { k: 'lo', v: o.lowRiskCells, label: '低风险单元（汇总）' }
  ]
})

async function load() {
  loading.value = true
  try {
    const { data } = await http.get('/erm/dashboard/overview')
    if (data.code !== 1) {
      ElMessage.error(data.msg || '加载失败')
      return
    }
    counts.value = data.data.counts
    recent.value = data.data.recentAssessments || []
    hints.value = data.data.hints || []
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function logout() {
  auth.logout()
  router.push('/erm/login')
}
</script>

<style scoped>
.erm-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
}
.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.who {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}
.sub {
  color: #666;
  margin-bottom: 16px;
}
.stat-card {
  margin-bottom: 16px;
}
.stat-val {
  font-size: 28px;
  font-weight: 600;
}
.stat-label {
  color: #888;
  margin-top: 4px;
}
.mt {
  margin-top: 16px;
}
.hints {
  margin: 0;
  padding-left: 20px;
  color: #555;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
