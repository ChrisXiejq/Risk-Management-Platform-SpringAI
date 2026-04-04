<template>
  <div class="erm-login-wrap">
    <el-card class="erm-login-card" shadow="hover">
      <template #header>
        <span>企业安全风险评估 — 登录 / 注册租户</span>
      </template>
      <el-tabs v-model="tab">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-width="100px" @submit.prevent>
            <el-form-item label="租户代码">
              <el-input v-model="loginForm.tenantCode" placeholder="如 demo-corp" />
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="doLogin">登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册租户" name="reg">
          <el-form :model="regForm" label-width="100px">
            <el-form-item label="租户代码">
              <el-input v-model="regForm.tenantCode" placeholder="小写、数字、下划线，2–64 位" />
            </el-form-item>
            <el-form-item label="租户名称">
              <el-input v-model="regForm.tenantName" />
            </el-form-item>
            <el-form-item label="管理员账号">
              <el-input v-model="regForm.adminUsername" />
            </el-form-item>
            <el-form-item label="管理员密码">
              <el-input v-model="regForm.adminPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="显示名">
              <el-input v-model="regForm.displayName" placeholder="可选" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="doRegister">创建并登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { useErmAuthStore } from '../../stores/ermAuth'

const router = useRouter()
const route = useRoute()
const auth = useErmAuthStore()
const tab = ref('login')
const loading = ref(false)

const loginForm = reactive({
  tenantCode: '',
  username: '',
  password: ''
})

const regForm = reactive({
  tenantCode: '',
  tenantName: '',
  adminUsername: '',
  adminPassword: '',
  displayName: ''
})

async function doLogin() {
  loading.value = true
  try {
    const { data } = await http.post('/auth/login', { ...loginForm })
    if (data.code !== 1) {
      ElMessage.error(data.msg || '登录失败')
      return
    }
    auth.setSession(data.data)
    ElMessage.success('登录成功')
    {
      const r = route.query.redirect
      const path = Array.isArray(r) ? r[0] : r
      router.push(path || '/erm/dashboard')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  loading.value = true
  try {
    const { data } = await http.post('/auth/register', { ...regForm })
    if (data.code !== 1) {
      ElMessage.error(data.msg || '注册失败')
      return
    }
    auth.setSession(data.data)
    ElMessage.success('注册成功')
    {
      const r = route.query.redirect
      const path = Array.isArray(r) ? r[0] : r
      router.push(path || '/erm/dashboard')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.erm-login-wrap {
  display: flex;
  justify-content: center;
  padding: 40px 16px;
}
.erm-login-card {
  width: 100%;
  max-width: 520px;
}
</style>
