<template>
  <el-popover
    v-model:visible="visible"
    placement="bottom-end"
    :width="380"
    trigger="click"
    popper-class="notification-popover"
  >
    <template #reference>
      <el-badge
        :value="unreadCount > 0 ? unreadCount : undefined"
        :max="99"
        :hidden="unreadCount === 0"
        class="notification-badge"
      >
        <span class="toolbar-icon-button hover-effect" title="通知消息" @click="handleOpen">
          <el-icon><Bell /></el-icon>
        </span>
      </el-badge>
    </template>

    <div class="notification-panel">
      <div class="notification-panel__header">
        <span class="notification-panel__title">通知消息</span>
        <div class="notification-panel__actions">
          <el-button v-if="unreadCount > 0" link type="primary" size="small" @click="handleReadAll">
            全部已读
          </el-button>
          <el-button link type="info" size="small" @click="loadList">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
      </div>

      <div v-if="loading" class="notification-panel__loading">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="list.length === 0" class="notification-panel__empty">
        <el-empty description="暂无通知" :image-size="60" />
      </div>
      <div v-else class="notification-panel__list">
        <div
          v-for="item in list"
          :key="item.id"
          class="notification-item"
          :class="{ 'is-unread': item.isRead === '0' }"
          @click="handleClick(item)"
        >
          <div class="notification-item__dot" />
          <div class="notification-item__content">
            <div class="notification-item__title">{{ item.title }}</div>
            <div class="notification-item__body">{{ item.content }}</div>
            <div class="notification-item__time">{{ formatTime(item.createTime) }}</div>
          </div>
          <div class="notification-item__actions">
            <el-button
              v-if="item.isRead === '0'"
              link
              type="primary"
              size="small"
              @click.stop="handleRead(item.id)"
            >
              已读
            </el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(item.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <div class="notification-panel__footer">
        <el-button link type="primary" size="small" @click="handleViewAll">
          查看全部
        </el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Refresh, Delete } from '@element-plus/icons-vue'
import { parseTime } from '@/utils/junsong'
import { normalizeNotificationLink } from '@/utils/notificationRoute'
import {
  listNotification,
  getUnreadCount,
  markRead,
  markAllRead,
  delNotification,
  type NotificationItem,
} from '@/api/system/notification'
import { useNotificationWs } from '@/composables/useNotificationWs'

const router = useRouter()
const visible = ref(false)
const loading = ref(false)
const list = ref<NotificationItem[]>([])
const unreadCount = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

// WebSocket 实时通知（连接失败时静默降级到轮询）
const { lastNotification, connect } = useNotificationWs()

watch(lastNotification, () => {
  // 收到 WebSocket 推送时立即刷新未读数
  loadUnreadCount()
  // 如果面板打开，同时刷新列表
  if (visible.value) {
    loadList()
  }
})

function formatTime(time: string) {
  return parseTime(time, '{m}-{d} {h}:{i}') || time
}

async function loadUnreadCount() {
  try {
    const res: any = await getUnreadCount()
    unreadCount.value = res.data || 0
  } catch (e) {
    // ignore
  }
}

async function loadList() {
  loading.value = true
  try {
    const res: any = await listNotification({ pageSize: 20 })
    list.value = res.rows || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
  await loadUnreadCount()
}

function handleOpen() {
  if (!visible.value) {
    loadList()
  }
}

async function handleRead(id: number) {
  await markRead(id)
  ElMessage.success('已标记已读')
  await loadList()
}

async function handleReadAll() {
  await markAllRead()
  ElMessage.success('全部已读')
  await loadList()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该通知吗？', '提示', { type: 'warning' })
  await delNotification([id])
  ElMessage.success('已删除')
  await loadList()
}

function handleClick(item: NotificationItem) {
  if (item.isRead === '0') {
    markRead(item.id)
  }
  visible.value = false

  if (item.type === 'wf_todo' && item.bizId) {
    // 待办任务：跳转到任务列表并带上 taskId，自动打开详情
    router.push(`/workflow/task?taskId=${item.bizId}`)
  } else if (item.type === 'wf_finished' && item.bizId) {
    // 已办结：跳转到流程实例列表
    router.push(`/workflow/instance?processInstanceId=${item.bizId}`)
  } else if (item.type === 'wf_rejected' && item.bizId) {
    // 已驳回：跳转到流程实例列表
    router.push(`/workflow/instance?processInstanceId=${item.bizId}`)
  } else if (item.type === 'register_audit') {
    router.push(normalizeNotificationLink(item.linkUrl) || '/system/user')
  } else if (item.linkUrl) {
    const target = normalizeNotificationLink(item.linkUrl)
    router.push(target)
  } else {
    handleRead(item.id)
  }
}

function handleViewAll() {
  visible.value = false
  router.push('/user/notification')
}

onMounted(() => {
  loadUnreadCount()
  timer = setInterval(loadUnreadCount, 30000)
  // 建立 WebSocket 连接（失败时静默降级到轮询）
  connect()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.notification-badge :deep(.el-badge__content) {
  top: 4px;
  right: 4px;
}

.notification-panel {
  max-height: 480px;
  display: flex;
  flex-direction: column;
}

.notification-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.notification-panel__title {
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.notification-panel__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.notification-panel__list {
  max-height: 360px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s ease;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.notification-item:hover {
  background: var(--el-fill-color-light);
}

.notification-item.is-unread {
  background: var(--el-color-primary-light-9);
}

.notification-item.is-unread:hover {
  background: var(--el-color-primary-light-8);
}

.notification-item__dot {
  width: 8px;
  height: 8px;
  min-width: 8px;
  border-radius: 50%;
  background: var(--el-color-primary);
  margin-top: 6px;
}

.notification-item.is-unread .notification-item__dot {
  background: var(--el-color-danger);
}

.notification-item__content {
  flex: 1;
  min-width: 0;
}

.notification-item__title {
  font-size: 13px;
  font-weight: 650;
  color: var(--el-text-color-primary);
  line-height: 1.4;
  margin-bottom: 4px;
}

.notification-item__body {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-item__time {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.notification-item__actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.notification-item:hover .notification-item__actions {
  opacity: 1;
}

.notification-panel__footer {
  padding: 10px 16px;
  text-align: center;
  border-top: 1px solid var(--el-border-color-lighter);
}

.notification-panel__empty {
  padding: 20px 0;
}

.notification-panel__loading {
  padding: 16px;
}
</style>

<style>
.notification-popover {
  padding: 0 !important;
}
</style>
