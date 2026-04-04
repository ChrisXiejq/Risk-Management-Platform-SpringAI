<template>
  <div class="home-wrap">
    <el-card class="hero-card" shadow="never">
      <h1>{{ $t('home.title') }}</h1>
      <p class="lead">{{ $t('home.lead') }}</p>
      <ul class="features">
        <li v-for="(line, i) in featureLines" :key="i">{{ line }}</li>
      </ul>
      <div class="actions">
        <el-button type="primary" size="large" @click="goLogin">{{ $t('home.ctaLogin') }}</el-button>
        <el-button size="large" @click="goDashboard">{{ $t('home.ctaWorkspace') }}</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const { t } = useI18n()

const featureLines = computed(() => {
  const raw = t('home.features')
  return typeof raw === 'string' ? raw.split('|') : []
})

function goLogin() {
  router.push('/erm/login')
}

function goDashboard() {
  router.push('/erm/dashboard')
}
</script>

<style scoped>
.home-wrap {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 24px 16px 48px;
}

.hero-card {
  max-width: 800px;
  width: 100%;
  border-radius: 12px;
}

h1 {
  font-size: 1.75rem;
  margin: 0 0 12px;
  color: #303133;
}

.lead {
  font-size: 1.05rem;
  line-height: 1.7;
  color: #606266;
  margin-bottom: 20px;
}

.features {
  margin: 0 0 28px;
  padding-left: 1.25rem;
  color: #606266;
  line-height: 1.85;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
