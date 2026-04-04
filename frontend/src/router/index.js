import { createRouter, createWebHashHistory } from 'vue-router'
import { useErmAuthStore } from '../stores/ermAuth'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/Home/home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/erm/login',
    name: 'erm-login',
    component: () => import('../views/ERM/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/erm/dashboard',
    name: 'erm-dashboard',
    component: () => import('../views/ERM/Dashboard.vue'),
    meta: { requiresErmAuth: true, title: '风险总览' }
  },
  {
    path: '/erm/identification',
    name: 'erm-identification',
    component: () => import('../views/ERM/Identification.vue'),
    meta: { requiresErmAuth: true, title: '风险识别' }
  },
  {
    path: '/erm/assessments',
    name: 'erm-assessments',
    component: () => import('../views/ERM/Assessments.vue'),
    meta: { requiresErmAuth: true, title: '评估记录' }
  },
  {
    path: '/erm/assessments/:id',
    name: 'erm-assessment-detail',
    component: () => import('../views/ERM/AssessmentDetail.vue'),
    meta: { requiresErmAuth: true, title: '评估详情' }
  },
  {
    path: '/erm/agent',
    name: 'erm-agent',
    component: () => import('../views/ERM/Agent.vue'),
    meta: { requiresErmAuth: true, title: '智能分析' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

const DEFAULT_TITLE = '企业安全风险评估系统'

router.beforeEach((to, from, next) => {
  if (to.meta.requiresErmAuth) {
    const auth = useErmAuthStore()
    if (!auth.token) {
      next({ path: '/erm/login', query: { redirect: to.fullPath } })
      return
    }
  }
  next()
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · ${DEFAULT_TITLE}` : DEFAULT_TITLE
})

export default router
