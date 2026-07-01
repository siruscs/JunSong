<template>
  <div class="app-container">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--total" @click="filterByStatus('')">
          <div class="stat-card__inner">
            <el-icon :size="28"><Bell /></el-icon>
            <div class="stat-card__body">
              <div class="stat-card__value">{{ stats.total }}</div>
              <div class="stat-card__label">全部通知</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--unread" @click="filterByStatus('0')">
          <div class="stat-card__inner">
            <el-icon :size="28"><Message /></el-icon>
            <div class="stat-card__body">
              <div class="stat-card__value">{{ stats.unread }}</div>
              <div class="stat-card__label">未读通知</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--todo" @click="filterByType('wf_todo')">
          <div class="stat-card__inner">
            <el-icon :size="28"><Tickets /></el-icon>
            <div class="stat-card__body">
              <div class="stat-card__value">{{ stats.todo }}</div>
              <div class="stat-card__label">待办通知</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--read" @click="filterByStatus('1')">
          <div class="stat-card__inner">
            <el-icon :size="28"><CircleCheck /></el-icon>
            <div class="stat-card__body">
              <div class="stat-card__value">{{ stats.read }}</div>
              <div class="stat-card__label">已读通知</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 类型筛选 Tab -->
    <el-tabs v-model="activeType" @tab-change="handleTabChange" class="notification-tabs">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待办任务" name="wf_todo" />
      <el-tab-pane label="流程办结" name="wf_finished" />
      <el-tab-pane label="流程驳回" name="wf_rejected" />
      <el-tab-pane label="催办" name="wf_urge" />
      <el-tab-pane label="抄送" name="wf_cc" />
      <el-tab-pane label="加签" name="wf_addsign" />
      <el-tab-pane label="撤回" name="wf_withdraw" />
      <el-tab-pane label="超时催办" name="wf_timeout_urge" />
      <el-tab-pane label="超时转办" name="wf_timeout_transfer" />
      <el-tab-pane label="注册审核" name="register_audit" />
      <el-tab-pane label="系统" name="system" />
    </el-tabs>

    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入通知标题关键词"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否已读" prop="isRead">
        <el-select v-model="queryParams.isRead" placeholder="全部" clearable style="width: 160px">
          <el-option label="未读" value="0" />
          <el-option label="已读" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作工具栏 -->
    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="success" :icon="Check" @click="handleReadAll" :disabled="unreadCount === 0">
        全部已读
      </el-button>
      <el-button type="danger" :icon="Delete" @click="handleBatchDelete" :disabled="multiple">
        批量删除
      </el-button>
      <el-button :icon="Refresh" @click="getList">刷新</el-button>
    </RightToolbar>

    <!-- 通知列表 -->
    <el-table
      v-loading="loading"
      :data="notificationList"
      @selection-change="handleSelectionChange"
      @row-click="handleRowClick"
      row-key="id"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isRead === '0'" type="danger" size="small" effect="dark">未读</el-tag>
          <el-tag v-else type="info" size="small">已读</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="typeTagType(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题" min-width="200" :show-overflow-tooltip="true">
        <template #default="{ row }">
          <span :class="{ 'title-unread': row.isRead === '0' }">{{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column label="内容" min-width="280" :show-overflow-tooltip="true" prop="content" />
      <el-table-column label="接收时间" width="160" align="center" prop="createTime">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="阅读时间" width="160" align="center" prop="readTime">
        <template #default="{ row }">
          {{ row.readTime ? formatTime(row.readTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button
            v-if="row.isRead === '0'"
            link
            type="primary"
            @click.stop="handleRead(row)"
          >
            标记已读
          </el-button>
          <el-button
            v-if="row.linkUrl || isClickable(row)"
            link
            type="success"
            @click.stop="handleView(row)"
          >
            查看
          </el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell, Message, Tickets, CircleCheck, Search, Refresh, Check, Delete,
} from '@element-plus/icons-vue'
import { parseTime } from '@/utils/junsong'
import {
  listNotification,
  getUnreadCount,
  markRead,
  markAllRead,
  delNotification,
  type NotificationItem,
} from '@/api/system/notification'

const router = useRouter()
const queryFormRef = ref()
const loading = ref(false)
const showSearch = ref(true)
const multiple = ref(true)
const total = ref(0)
const notificationList = ref<NotificationItem[]>([])
const selectedIds = ref<number[]>([])
const unreadCount = ref(0)
const activeType = ref('')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  type: '',
  isRead: '',
})

const stats = reactive({
  total: 0,
  unread: 0,
  read: 0,
  todo: 0,
})

const TYPE_LABEL_MAP: Record<string, string> = {
  wf_todo: '待办任务',
  wf_finished: '流程办结',
  wf_rejected: '流程驳回',
  wf_urge: '催办',
  wf_cc: '抄送',
  wf_addsign: '加签',
  wf_withdraw: '撤回',
  wf_timeout_urge: '超时催办',
  wf_timeout_transfer: '超时转办',
  register_audit: '注册审核',
  system: '系统',
}

const TYPE_TAG_MAP: Record<string, string> = {
  wf_todo: 'danger',
  wf_finished: 'success',
  wf_rejected: 'danger',
  wf_urge: 'warning',
  wf_cc: 'info',
  wf_addsign: 'warning',
  wf_withdraw: 'info',
  wf_timeout_urge: 'danger',
  wf_timeout_transfer: 'danger',
  register_audit: 'primary',
  system: 'info',
}

function typeLabel(type: string) {
  return TYPE_LABEL_MAP[type] || type
}

function typeTagType(type: string) {
  return TYPE_TAG_MAP[type] || 'info'
}

function formatTime(time: string) {
  return parseTime(time, '{y}-{m}-{d} {h}:{i}') || time
}

function isClickable(row: NotificationItem) {
  const clickableTypes = ['wf_todo', 'wf_finished', 'wf_rejected', 'register_audit']
  return clickableTypes.includes(row.type) || !!row.linkUrl
}

async function getList() {
  loading.value = true
  try {
    const res: any = await listNotification(queryParams)
    notificationList.value = res.rows || []
    total.value = res.total || 0
  } catch (e) {
    notificationList.value = []
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res: any = await getUnreadCount()
    unreadCount.value = res.data || 0
    stats.unread = res.data || 0
    const allRes: any = await listNotification({ pageNum: 1, pageSize: 1 })
    stats.total = allRes.total || 0
    const readRes: any = await listNotification({ pageNum: 1, pageSize: 1, isRead: '1' })
    stats.read = readRes.total || 0
    const todoRes: any = await listNotification({ pageNum: 1, pageSize: 1, type: 'wf_todo' })
    stats.todo = todoRes.total || 0
  } catch (e) {
    // ignore
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryFormRef.value?.resetFields()
  queryParams.type = activeType.value
  queryParams.isRead = ''
  handleQuery()
}

function handleTabChange(name: string) {
  queryParams.type = name
  queryParams.pageNum = 1
  getList()
}

function filterByStatus(isRead: string) {
  queryParams.isRead = isRead
  queryParams.type = ''
  activeType.value = ''
  handleQuery()
}

function filterByType(type: string) {
  queryParams.type = type
  queryParams.isRead = ''
  activeType.value = type
  handleQuery()
}

function handleSelectionChange(selection: NotificationItem[]) {
  selectedIds.value = selection.map((item) => item.id)
  multiple.value = !selection.length
}

async function handleRead(row: NotificationItem) {
  await markRead(row.id)
  row.isRead = '1'
  ElMessage.success('已标记已读')
  await loadStats()
}

async function handleReadAll() {
  await ElMessageBox.confirm('确认将所有未读通知标记为已读吗？', '提示', { type: 'warning' })
  await markAllRead()
  ElMessage.success('全部已读')
  await getList()
  await loadStats()
}

async function handleDelete(row: NotificationItem) {
  await ElMessageBox.confirm('确认删除该通知吗？', '提示', { type: 'warning' })
  await delNotification([row.id])
  ElMessage.success('已删除')
  await getList()
  await loadStats()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条通知吗？`, '提示', {
    type: 'warning',
  })
  await delNotification(selectedIds.value)
  ElMessage.success('已删除')
  await getList()
  await loadStats()
}

function handleRowClick(row: NotificationItem) {
  if (row.isRead === '0') {
    markRead(row.id).then(() => {
      row.isRead = '1'
      loadStats()
    })
  }
  handleView(row)
}

function handleView(row: NotificationItem) {
  if (row.isRead === '0') {
    markRead(row.id)
    row.isRead = '1'
    loadStats()
  }

  if (row.type === 'wf_todo' && row.bizId) {
    router.push(`/workflow/task?taskId=${row.bizId}`)
  } else if ((row.type === 'wf_finished' || row.type === 'wf_rejected') && row.bizId) {
    router.push(`/workflow/instance?processInstanceId=${row.bizId}`)
  } else if (row.type === 'register_audit') {
    router.push(row.linkUrl || '/system/user')
  } else if (row.linkUrl) {
    router.push(row.linkUrl)
  }
}

onMounted(() => {
  getList()
  loadStats()
})
</script>

<style scoped>
.stat-cards {
  margin-bottom: 16px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card__inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card__body {
  flex: 1;
}

.stat-card__value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.stat-card--total .stat-card__value {
  color: var(--el-color-primary);
}

.stat-card--unread .stat-card__value {
  color: var(--el-color-danger);
}

.stat-card--todo .stat-card__value {
  color: var(--el-color-warning);
}

.stat-card--read .stat-card__value {
  color: var(--el-color-success);
}

.notification-tabs {
  margin-bottom: 12px;
}

.title-unread {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
