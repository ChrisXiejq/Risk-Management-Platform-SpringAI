<template>
  <div class="erm-page">
    <h2>Agent 智能风险分析</h2>
    <p class="sub">调用现有后端多 Agent 能力；会话 ID 与「评估记录」中的 chatId 对齐，便于与场景/证据工具协同。</p>
    <el-form inline class="mb">
      <el-form-item label="chatId">
        <el-input v-model="chatId" style="width: 280px" placeholder="erm-assess-{评估ID}" />
      </el-form-item>
      <el-form-item>
        <el-button @click="clearLocal">清空本地消息</el-button>
      </el-form-item>
    </el-form>
    <div class="chat-box">
      <div v-for="(m, i) in messages" :key="i" :class="['bubble', m.role]">
        <div class="role">{{ m.role === 'user' ? '我' : 'Agent' }}</div>
        <div class="text">{{ m.text }}</div>
      </div>
    </div>
    <div class="input-row">
      <el-input
        v-model="input"
        type="textarea"
        :rows="3"
        placeholder="例如：根据当前租户已登记的资产与威胁，按 GB/T 20984 给出风险分析要点与整改优先级建议。"
        @keydown.enter.ctrl="send"
      />
      <el-button type="primary" :loading="sending" @click="send">发送</el-button>
    </div>
    <p class="hint">Ctrl+Enter 发送。首次使用请先在「评估记录」打开详情以自动绑定 chatId。</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../../api/http'

const route = useRoute()
const chatId = ref('default')
const input = ref('')
const sending = ref(false)
const messages = ref([])

onMounted(() => {
  if (route.query.chatId) {
    chatId.value = String(route.query.chatId)
  }
})

function clearLocal() {
  messages.value = []
}

async function send() {
  const text = input.value.trim()
  if (!text) return
  sending.value = true
  messages.value.push({ role: 'user', text })
  input.value = ''
  try {
    const { data } = await http.post('/risk/assess', {
      message: text,
      chatId: chatId.value || 'default'
    })
    if (data.success) {
      messages.value.push({ role: 'assistant', text: data.content || '' })
    } else {
      messages.value.push({ role: 'assistant', text: data.error || '调用失败' })
    }
  } catch (e) {
    const msg = e.response?.data?.msg || e.response?.data?.message || e.message
    ElMessage.error(msg)
    messages.value.push({ role: 'assistant', text: '请求失败: ' + msg })
  } finally {
    sending.value = false
  }
}
</script>

<style scoped>
.erm-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 16px;
}
.sub {
  color: #666;
  margin-bottom: 12px;
}
.mb {
  margin-bottom: 12px;
}
.chat-box {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  min-height: 240px;
  max-height: 420px;
  overflow-y: auto;
  background: #fafafa;
  margin-bottom: 12px;
}
.bubble {
  margin-bottom: 12px;
}
.bubble.user .text {
  background: #ecf5ff;
}
.bubble.assistant .text {
  background: #fff;
  border: 1px solid #e4e7ed;
}
.role {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.text {
  padding: 8px 12px;
  border-radius: 8px;
  white-space: pre-wrap;
  line-height: 1.5;
}
.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.hint {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
