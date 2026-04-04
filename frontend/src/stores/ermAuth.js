import { defineStore } from 'pinia'

export const useErmAuthStore = defineStore('ermAuth', {
  state: () => ({
    token: '',
    tenantCode: '',
    username: '',
    displayName: '',
    role: ''
  }),
  actions: {
    setSession(payload) {
      this.token = payload.token || ''
      this.tenantCode = payload.tenantCode || ''
      this.username = payload.username || ''
      this.displayName = payload.displayName || ''
      this.role = payload.role || ''
    },
    logout() {
      this.token = ''
      this.tenantCode = ''
      this.username = ''
      this.displayName = ''
      this.role = ''
    }
  },
  persist: true
})
