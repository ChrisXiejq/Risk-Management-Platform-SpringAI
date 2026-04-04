<template>
  <div id="whole-box">
    <div class="nav-container">
      <div class="nav-brand" @click="router.push('/')">
        <span class="brand-title">{{ $t('topnav.AppTitle') }}</span>
        <span class="brand-sub">{{ $t('topnav.AppSubtitle') }}</span>
      </div>
      <div class="nav-box">
        <div class="nav-menu-horizontal">
          <el-menu
            mode="horizontal"
            :ellipsis="false"
            :default-active="activePath"
            @select="onMenuSelect"
          >
            <el-menu-item index="/">{{ $t('topnav.Home') }}</el-menu-item>
            <el-menu-item index="/erm/dashboard">{{ $t('topnav.ErmOverview') }}</el-menu-item>
            <el-menu-item index="/erm/identification">{{ $t('topnav.ErmIdentification') }}</el-menu-item>
            <el-menu-item index="/erm/assessments">{{ $t('topnav.ErmAssessments') }}</el-menu-item>
            <el-menu-item index="/erm/agent">{{ $t('topnav.ErmAgent') }}</el-menu-item>
            <el-menu-item v-if="!auth.token" index="/erm/login">{{ $t('topnav.ErmLogin') }}</el-menu-item>
            <el-menu-item v-else index="__logout__">{{ $t('topnav.ErmLogout') }}</el-menu-item>
          </el-menu>
        </div>
        <div class="nav-tools">
          <el-dropdown trigger="hover" @command="handleLanguageChange">
            <span class="el-dropdown-link">
              {{ $t('topnav.language') }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="en">{{ $t('topnav.English') }}</el-dropdown-item>
                <el-dropdown-item command="zn">{{ $t('topnav.Chinese') }}</el-dropdown-item>
                <el-dropdown-item command="de">{{ $t('topnav.German') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="nav-menu-dropdown">
          <el-dropdown trigger="click">
            <span class="el-dropdown-link">{{ $t('topnav.menu') }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/')">{{ $t('topnav.Home') }}</el-dropdown-item>
                <el-dropdown-item @click="router.push('/erm/dashboard')">{{ $t('topnav.ErmOverview') }}</el-dropdown-item>
                <el-dropdown-item @click="router.push('/erm/identification')">{{ $t('topnav.ErmIdentification') }}</el-dropdown-item>
                <el-dropdown-item @click="router.push('/erm/assessments')">{{ $t('topnav.ErmAssessments') }}</el-dropdown-item>
                <el-dropdown-item @click="router.push('/erm/agent')">{{ $t('topnav.ErmAgent') }}</el-dropdown-item>
                <el-dropdown-item v-if="!auth.token" @click="router.push('/erm/login')">{{ $t('topnav.ErmLogin') }}</el-dropdown-item>
                <el-dropdown-item v-else @click="onLogout">{{ $t('topnav.ErmLogout') }}</el-dropdown-item>
                <el-dropdown-item divided disabled style="cursor: default; color: #909399; font-size: 12px;">
                  {{ $t('topnav.language') }}
                </el-dropdown-item>
                <el-dropdown-item @click="handleLanguageChange('en')">{{ $t('topnav.English') }}</el-dropdown-item>
                <el-dropdown-item @click="handleLanguageChange('zn')">{{ $t('topnav.Chinese') }}</el-dropdown-item>
                <el-dropdown-item @click="handleLanguageChange('de')">{{ $t('topnav.German') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useErmAuthStore } from '../stores/ermAuth'

const route = useRoute()
const router = useRouter()
const auth = useErmAuthStore()
const { locale } = useI18n()

const activePath = computed(() => {
  const p = route.path
  if (/^\/erm\/assessments\/[^/]+$/.test(p)) {
    return '/erm/assessments'
  }
  return p
})

const handleLanguageChange = (language) => {
  locale.value = language
}

function onMenuSelect(key) {
  if (key === '__logout__') {
    onLogout()
    return
  }
  router.push(key)
}

function onLogout() {
  auth.logout()
  router.push('/erm/login')
}
</script>

<style scoped>
#whole-box {
  display: flex;
  width: 100%;
  justify-content: center;
}

.nav-container {
  width: 100%;
  max-width: 1400px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0 8px;
  gap: 16px;
  background-color: #fff;
  flex-wrap: wrap;
}

.nav-brand {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 200px;
}

.brand-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.3;
}

.brand-sub {
  font-size: 12px;
  color: #606266;
}

.nav-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.nav-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.nav-menu-horizontal :deep(.el-menu--horizontal) {
  border-bottom: none;
}

.nav-menu-horizontal :deep(.el-menu-item) {
  font-size: 15px;
}

.el-dropdown-link {
  cursor: pointer;
  padding: 6px 12px;
  color: #409eff;
  border-radius: 4px;
  background-color: #f9f9f9;
}

.el-dropdown-link:hover {
  background-color: #ecf5ff;
}

@media (min-width: 721px) {
  .nav-menu-horizontal {
    display: flex;
    width: 100%;
    justify-content: flex-end;
  }
  .nav-menu-dropdown {
    display: none;
  }
}

@media (max-width: 720px) {
  .nav-menu-horizontal {
    display: none;
  }
  .nav-menu-dropdown {
    display: block;
  }
  .brand-title {
    font-size: 1rem;
  }
}
</style>
