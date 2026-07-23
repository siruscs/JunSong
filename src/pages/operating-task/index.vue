<template>
  <view class="page">
    <view class="hero">
      <view class="hero-main">
        <view class="eyebrow">经营任务</view>
        <view class="hero-title">{{ currentDeptName || '全部门店' }}</view>
      </view>
      <view class="hero-badge" v-if="pendingCount > 0">
        <text class="hero-badge-num">{{ pendingCount }}</text>
        <text class="hero-badge-label">待办</text>
      </view>
    </view>

    <!-- 筛选标签 -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ active: activeTab === 'pending' }"
        @tap="switchTab('pending')"
      >
        <text>待处理</text>
        <text class="tab-count" v-if="counts.pending > 0">{{ counts.pending }}</text>
      </view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'mine' }"
        @tap="switchTab('mine')"
      >
        <text>我负责</text>
        <text class="tab-count" v-if="counts.mine > 0">{{ counts.mine }}</text>
      </view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'done' }"
        @tap="switchTab('done')"
      >
        <text>已完成</text>
      </view>
    </view>

    <scroll-view
      scroll-y
      class="scroll"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <view
        class="task-card"
        hover-class="task-card--active"
        v-for="item in rows"
        :key="item.taskId"
        @tap="openDetail(item)"
      >
        <view class="task-card-head">
          <text class="task-title" :class="{ overdue: isOverdue(item) }">{{ item.title }}</text>
          <text class="task-priority" :class="priorityClass(item.priority)">{{ priorityLabel(item.priority) }}</text>
        </view>
        <view class="task-card-meta">
          <text class="task-dept">{{ item.deptName || '—' }}</text>
          <text class="task-source">{{ sourceLabel(item.sourceModule) }}</text>
        </view>
        <view class="task-card-foot">
          <text class="task-status" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</text>
          <text class="task-due" :class="{ overdue: isOverdue(item) }">{{ formatTime(item.dueTime) || '—' }}</text>
          <text class="task-assignee" v-if="item.assigneeName">{{ item.assigneeName }}</text>
        </view>
      </view>

      <view class="load-error" v-if="!loading && loadError">
        <view class="empty-title">任务加载失败</view>
        <view class="empty-subtitle">{{ loadError }}</view>
        <button class="retry-button" @tap="refresh">重新加载</button>
      </view>
      <view class="empty" v-if="!loading && !loadError && rows.length === 0">
        <view class="empty-mark">务</view>
        <view class="empty-title">暂无经营任务</view>
        <view class="empty-subtitle">{{ emptyHint }}</view>
      </view>
      <view class="loading" v-if="loading">加载中</view>
      <view class="loading" v-if="finished && rows.length > 0">没有更多了</view>
    </scroll-view>

    <!-- 任务详情弹窗 -->
    <view class="detail-mask" v-if="detailVisible" @tap="closeDetail"></view>
    <view class="detail-panel" v-if="detailVisible">
      <view class="detail-header">
        <text class="detail-title">任务详情</text>
        <text class="detail-close" @tap="closeDetail">×</text>
      </view>
      <scroll-view scroll-y class="detail-scroll" v-if="currentTask">
        <view class="detail-section">
          <text class="detail-label">标题</text>
          <text class="detail-value">{{ currentTask.title }}</text>
        </view>
        <view class="detail-section">
          <text class="detail-label">来源</text>
          <text class="detail-value">{{ sourceLabel(currentTask.sourceModule) }} / {{ currentTask.sourceType }}</text>
        </view>
        <view class="detail-section">
          <text class="detail-label">门店</text>
          <text class="detail-value">{{ currentTask.deptName || currentTask.deptId || '—' }}</text>
        </view>
        <view class="detail-section">
          <text class="detail-label">优先级</text>
          <text class="detail-value" :class="priorityClass(currentTask.priority)">{{ priorityLabel(currentTask.priority) }}</text>
        </view>
        <view class="detail-section">
          <text class="detail-label">状态</text>
          <text class="detail-value" :class="statusClass(currentTask.status)">{{ statusLabel(currentTask.status) }}</text>
        </view>
        <view class="detail-section">
          <text class="detail-label">负责人</text>
          <text class="detail-value">{{ currentTask.assigneeName || '—' }}</text>
        </view>
        <view class="detail-section">
          <text class="detail-label">截止时间</text>
          <text class="detail-value" :class="{ overdue: isOverdue(currentTask) }">{{ formatTime(currentTask.dueTime) || '—' }}</text>
        </view>
        <view class="detail-section" v-if="currentTask.impactAmount != null">
          <text class="detail-label">影响金额</text>
          <text class="detail-value money">¥{{ formatMoney(currentTask.impactAmount) }}</text>
        </view>
        <view class="detail-section" v-if="currentTask.handlerNote">
          <text class="detail-label">处理备注</text>
          <text class="detail-value">{{ currentTask.handlerNote }}</text>
        </view>
        <view class="detail-section" v-if="currentTask.rejectReason">
          <text class="detail-label">驳回原因</text>
          <text class="detail-value">{{ currentTask.rejectReason }}</text>
        </view>
      </scroll-view>

      <!-- 操作按钮 -->
      <view class="detail-actions" v-if="currentTask">
        <button
          class="action-btn claim"
          v-if="canClaim(currentTask) && hasClaimPerm"
          :disabled="submitting"
          @tap.stop="handleClaim"
        >{{ submitting ? '处理中' : '认领' }}</button>
        <button
          class="action-btn complete"
          v-if="canComplete(currentTask) && hasCompletePerm"
          :disabled="submitting"
          @tap.stop="handleComplete"
        >{{ submitting ? '处理中' : '完成' }}</button>
        <button
          class="action-btn reject"
          v-if="canReject(currentTask) && hasRejectPerm"
          :disabled="submitting"
          @tap.stop="handleReject"
        >{{ submitting ? '处理中' : '驳回' }}</button>
        <button
          class="action-btn reopen"
          v-if="canReopen(currentTask) && hasReopenPerm"
          :disabled="submitting"
          @tap.stop="handleReopen"
        >{{ submitting ? '处理中' : '重开' }}</button>
        <button
          class="action-btn source"
          v-if="currentTask.sourceRoute"
          @tap.stop="handleViewSource"
        >查看来源</button>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { workContext } from '@/utils/workContext.js'
import { hasExactPermission } from '@/utils/permission.js'

const STATUS_LABELS = {
  PENDING: '待处理',
  IN_PROGRESS: '处理中',
  DONE: '已完成',
  REJECTED: '已驳回',
  REOPENED: '已重开'
}

const PRIORITY_LABELS = {
  URGENT: '紧急',
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低'
}

const SOURCE_LABELS = {
  FINANCE: '财务',
  STOCK: '库存',
  MEMBER: '会员',
  WORKFLOW: '工作流',
  SYSTEM: '系统'
}

export default {
  data() {
    return {
      activeTab: 'pending',
      rows: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      loading: false,
      refreshing: false,
      finished: false,
      loadError: '',
      pendingCount: 0,
      counts: { pending: 0, mine: 0 },
      submitting: false,
      detailVisible: false,
      currentTask: null,
      contextVersion: null
    }
  },
  computed: {
    currentDeptName() {
      const snap = workContext.snapshot()
      return snap.currentDept?.name || ''
    },
    emptyHint() {
      if (this.activeTab === 'pending') return '暂无待处理任务'
      if (this.activeTab === 'mine') return '暂无你负责的任务'
      return '暂无已完成任务'
    },
    hasClaimPerm() {
      return hasExactPermission('system:operatingTask:claim')
    },
    hasCompletePerm() {
      return hasExactPermission('system:operatingTask:complete')
    },
    hasRejectPerm() {
      return hasExactPermission('system:operatingTask:reject')
    },
    hasReopenPerm() {
      return hasExactPermission('system:operatingTask:reopen')
    }
  },
  onLoad() {
    this.contextVersion = workContext.captureVersion()
    this.loadList()
    this.loadPendingCount()
  },
  onShow() {
    // 部门切换后重新加载
    const currentVersion = workContext.captureVersion()
    if (this.contextVersion !== null && this.contextVersion !== currentVersion) {
      this.contextVersion = currentVersion
      this.refresh()
    }
  },
  methods: {
    buildQuery() {
      const snap = workContext.snapshot()
      const params = {
        pageNum: this.pageNum,
        pageSize: this.pageSize,
        deptId: snap.currentDeptId || undefined
      }
      if (this.activeTab === 'pending') {
        params.status = 'PENDING'
      } else if (this.activeTab === 'mine') {
        params.status = 'IN_PROGRESS'
        params.assigneeId = snap.user?.userId || undefined
      } else if (this.activeTab === 'done') {
        params.status = 'DONE'
      }
      return params
    },
    async loadList() {
      if (this.loading) return
      this.loading = true
      this.loadError = ''
      try {
        const res = await request({
          url: '/operatingTask/list',
          method: 'GET',
          data: this.buildQuery(),
          silent: true
        })
        const data = res?.data || res || {}
        const rows = data.rows || []
        if (this.pageNum === 1) {
          this.rows = rows
        } else {
          this.rows = [...this.rows, ...rows]
        }
        this.total = data.total || 0
        this.finished = this.rows.length >= this.total
        this.updateCounts()
      } catch (e) {
        this.loadError = e?.msg || e?.message || '请求失败'
        if (this.pageNum === 1) this.rows = []
      } finally {
        this.loading = false
        this.refreshing = false
      }
    },
    async loadPendingCount() {
      try {
        const res = await request({
          url: '/operatingTask/pendingCount',
          method: 'GET',
          silent: true
        })
        this.pendingCount = Number(res?.data ?? res ?? 0)
      } catch {
        this.pendingCount = 0
      }
    },
    updateCounts() {
      this.counts.pending = this.rows.filter((t) => t.status === 'PENDING').length
      this.counts.mine = this.rows.filter((t) => t.status === 'IN_PROGRESS').length
    },
    switchTab(tab) {
      if (this.activeTab === tab) return
      this.activeTab = tab
      this.pageNum = 1
      this.finished = false
      this.loadList()
    },
    onRefresh() {
      this.refreshing = true
      this.refresh()
    },
    refresh() {
      this.pageNum = 1
      this.finished = false
      this.loadList()
      this.loadPendingCount()
    },
    loadMore() {
      if (this.loading || this.finished) return
      this.pageNum += 1
      this.loadList()
    },
    openDetail(item) {
      this.currentTask = item
      this.detailVisible = true
      this.loadDetail(item.taskId)
    },
    async loadDetail(taskId) {
      try {
        const res = await request({
          url: '/operatingTask/' + taskId,
          method: 'GET',
          silent: true
        })
        if (res?.data) {
          this.currentTask = res.data
        }
      } catch {
        // 详情加载失败保留列表数据
      }
    },
    closeDetail() {
      this.detailVisible = false
      this.currentTask = null
    },
    canClaim(task) {
      return task?.status === 'PENDING'
    },
    canComplete(task) {
      return ['IN_PROGRESS', 'REOPENED'].includes(task?.status)
    },
    canReject(task) {
      return ['IN_PROGRESS', 'REOPENED'].includes(task?.status)
    },
    canReopen(task) {
      return ['DONE', 'REJECTED'].includes(task?.status)
    },
    async handleClaim() {
      if (this.submitting) return
      this.submitting = true
      try {
        await request({
          url: '/operatingTask/claim/' + this.currentTask.taskId,
          method: 'PUT'
        })
        uni.showToast({ title: '认领成功', icon: 'success' })
        await this.afterActionSuccess()
      } catch (e) {
        this.handleActionError(e)
      } finally {
        this.submitting = false
      }
    },
    async handleComplete() {
      if (this.submitting) return
      const handlerNote = await this.promptInput('请输入处理备注', '处理说明（必填）')
      if (!handlerNote) return
      this.submitting = true
      try {
        await request({
          url: '/operatingTask/complete/' + this.currentTask.taskId,
          method: 'PUT',
          data: { handlerNote }
        })
        uni.showToast({ title: '任务已完成', icon: 'success' })
        await this.afterActionSuccess()
      } catch (e) {
        this.handleActionError(e)
      } finally {
        this.submitting = false
      }
    },
    async handleReject() {
      if (this.submitting) return
      const rejectReason = await this.promptInput('请输入驳回原因', '驳回原因（必填）')
      if (!rejectReason) return
      this.submitting = true
      try {
        await request({
          url: '/operatingTask/reject/' + this.currentTask.taskId,
          method: 'PUT',
          data: { rejectReason }
        })
        uni.showToast({ title: '任务已驳回', icon: 'success' })
        await this.afterActionSuccess()
      } catch (e) {
        this.handleActionError(e)
      } finally {
        this.submitting = false
      }
    },
    async handleReopen() {
      if (this.submitting) return
      const reason = await this.promptInput('请输入重开原因', '重开原因（必填）')
      if (!reason) return
      this.submitting = true
      try {
        await request({
          url: '/operatingTask/reopen/' + this.currentTask.taskId,
          method: 'PUT',
          data: { reason }
        })
        uni.showToast({ title: '任务已重开', icon: 'success' })
        await this.afterActionSuccess()
      } catch (e) {
        this.handleActionError(e)
      } finally {
        this.submitting = false
      }
    },
    async afterActionSuccess() {
      this.closeDetail()
      this.refresh()
    },
    handleActionError(e) {
      const code = Number(e?.code)
      const msg = e?.msg || e?.message || '操作失败'
      // 401 由 authSession 自动处理；403/状态冲突/重复提交在此明确提示
      if (code === 401) return
      if (code === 403) {
        uni.showToast({ title: '暂无操作权限', icon: 'none' })
        return
      }
      // 状态冲突：后端返回「任务状态已变更，请刷新」
      if (msg.includes('状态') || msg.includes('已变更') || code === 409) {
        uni.showModal({
          title: '任务状态冲突',
          content: msg + '\n是否刷新列表？',
          confirmText: '刷新',
          success: (res) => {
            if (res.confirm) this.refresh()
          }
        })
        return
      }
      uni.showToast({ title: msg, icon: 'none' })
    },
    promptInput(title, placeholder) {
      return new Promise((resolve) => {
        uni.showModal({
          title,
          editable: true,
          placeholderText: placeholder,
          confirmText: '确定',
          cancelText: '取消',
          success: (res) => {
            if (res.confirm && res.content && res.content.trim()) {
              resolve(res.content.trim())
            } else {
              resolve('')
            }
          },
          fail: () => resolve('')
        })
      })
    },
    handleViewSource() {
      if (!this.currentTask?.sourceRoute) {
        uni.showToast({ title: '该任务未配置来源路由', icon: 'none' })
        return
      }
      // 来源路由由后端权威返回，小程序端跳转需映射为小程序页面
      const route = this.currentTask.sourceRoute
      if (route === '/finance/expense') {
        uni.navigateTo({ url: '/pages/list/index?module=expense' })
      } else if (route === '/finance/reviewTask') {
        uni.navigateTo({ url: '/pages/operating-task/index' })
      } else if (route === '/finance/stock/health') {
        uni.navigateTo({ url: '/pages/list/index?module=product' })
      } else if (route.startsWith('/member/')) {
        uni.navigateTo({ url: '/pages/member/dashboard' })
      } else {
        uni.showToast({ title: '来源页面暂不支持小程序查看', icon: 'none' })
      }
    },
    isOverdue(task) {
      if (!task?.dueTime || !task?.status) return false
      const due = new Date(String(task.dueTime).replace(' ', 'T'))
      if (isNaN(due.getTime())) return false
      return due.getTime() < Date.now() && ['PENDING', 'IN_PROGRESS', 'REOPENED'].includes(task.status)
    },
    formatTime(time) {
      if (!time) return ''
      const d = new Date(String(time).replace(' ', 'T'))
      if (isNaN(d.getTime())) return ''
      const pad = (n) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    formatMoney(val) {
      return Number(val || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    priorityLabel(p) {
      return PRIORITY_LABELS[p] || p || '—'
    },
    priorityClass(p) {
      const map = { URGENT: 'priority-urgent', HIGH: 'priority-high', MEDIUM: 'priority-medium', LOW: 'priority-low' }
      return map[p] || 'priority-low'
    },
    statusLabel(s) {
      return STATUS_LABELS[s] || s || '—'
    },
    statusClass(s) {
      const map = {
        PENDING: 'status-pending', IN_PROGRESS: 'status-progress', DONE: 'status-done',
        REJECTED: 'status-rejected', REOPENED: 'status-reopened'
      }
      return map[s] || 'status-pending'
    },
    sourceLabel(s) {
      return SOURCE_LABELS[s] || s || '—'
    }
  }
}
</script>

<style>
.page { min-height: 100vh; background: #E8EEF5; }
.hero { padding: 24rpx 32rpx 16rpx; background: linear-gradient(135deg, #087CF0 0%, #0EA5E9 100%); color: #fff; display: flex; justify-content: space-between; align-items: center; }
.hero-main { flex: 1; }
.eyebrow { font-size: 24rpx; opacity: 0.85; }
.hero-title { font-size: 40rpx; font-weight: 700; }
.hero-badge { text-align: center; }
.hero-badge-num { font-size: 40rpx; font-weight: 700; }
.hero-badge-label { font-size: 22rpx; opacity: 0.85; }

.tab-bar { display: flex; background: #fff; border-bottom: 1rpx solid #e5e7eb; }
.tab-item { flex: 1; text-align: center; padding: 24rpx 0; font-size: 28rpx; color: #6b7280; position: relative; }
.tab-item.active { color: #087CF0; font-weight: 600; }
.tab-item.active::after { content: ''; position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: 60rpx; height: 4rpx; background: #087CF0; border-radius: 2rpx; }
.tab-count { margin-left: 8rpx; background: #ef4444; color: #fff; font-size: 20rpx; padding: 2rpx 12rpx; border-radius: 20rpx; }

.scroll { height: calc(100vh - 280rpx); }
.task-card { margin: 16rpx 24rpx; padding: 24rpx; background: #fff; border-radius: 16rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04); }
.task-card--active { background: #f0f9ff; }
.task-card-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12rpx; }
.task-title { font-size: 30rpx; font-weight: 600; color: #1f2937; flex: 1; }
.task-title.overdue { color: #ef4444; }
.task-priority { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; color: #fff; }
.priority-urgent { background: #ef4444; }
.priority-high { background: #f97316; }
.priority-medium { background: #eab308; color: #1f2937; }
.priority-low { background: #9ca3af; }
.task-card-meta { display: flex; gap: 16rpx; margin-bottom: 12rpx; }
.task-dept { font-size: 24rpx; color: #6b7280; }
.task-source { font-size: 24rpx; color: #087CF0; }
.task-card-foot { display: flex; justify-content: space-between; align-items: center; }
.task-status { font-size: 24rpx; padding: 2rpx 12rpx; border-radius: 6rpx; }
.status-pending { background: #fef3c7; color: #92400e; }
.status-progress { background: #dbeafe; color: #1e40af; }
.status-done { background: #d1fae5; color: #065f46; }
.status-rejected { background: #f3f4f6; color: #4b5563; }
.status-reopened { background: #fef3c7; color: #92400e; }
.task-due { font-size: 24rpx; color: #6b7280; }
.task-due.overdue { color: #ef4444; font-weight: 600; }
.task-assignee { font-size: 24rpx; color: #6b7280; }

.empty { text-align: center; padding: 120rpx 0; }
.empty-mark { width: 96rpx; height: 96rpx; line-height: 96rpx; margin: 0 auto 24rpx; border-radius: 50%; background: #e5e7eb; color: #9ca3af; font-size: 40rpx; }
.empty-title { font-size: 32rpx; color: #374151; margin-bottom: 8rpx; }
.empty-subtitle { font-size: 26rpx; color: #9ca3af; }
.load-error { text-align: center; padding: 80rpx 0; }
.retry-button { margin-top: 24rpx; background: #087CF0; color: #fff; border: none; border-radius: 8rpx; padding: 16rpx 48rpx; font-size: 28rpx; }
.loading { text-align: center; padding: 24rpx; color: #9ca3af; font-size: 26rpx; }

.detail-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 100; }
.detail-panel { position: fixed; left: 0; right: 0; bottom: 0; max-height: 80vh; background: #fff; border-radius: 24rpx 24rpx 0 0; z-index: 101; display: flex; flex-direction: column; }
.detail-header { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 32rpx; border-bottom: 1rpx solid #e5e7eb; }
.detail-title { font-size: 32rpx; font-weight: 700; }
.detail-close { font-size: 48rpx; color: #9ca3af; padding: 0 8rpx; }
.detail-scroll { flex: 1; padding: 24rpx 32rpx; max-height: 50vh; }
.detail-section { margin-bottom: 20rpx; }
.detail-label { font-size: 24rpx; color: #9ca3af; display: block; margin-bottom: 4rpx; }
.detail-value { font-size: 28rpx; color: #1f2937; }
.detail-value.money { color: #ef4444; font-weight: 600; }
.detail-value.overdue { color: #ef4444; }
.detail-actions { padding: 16rpx 32rpx 32rpx; display: flex; flex-wrap: wrap; gap: 16rpx; border-top: 1rpx solid #e5e7eb; }
.action-btn { flex: 1; min-width: 160rpx; padding: 20rpx 0; border-radius: 12rpx; font-size: 28rpx; text-align: center; border: none; }
.action-btn.claim { background: #10b981; color: #fff; }
.action-btn.complete { background: #087CF0; color: #fff; }
.action-btn.reject { background: #f59e0b; color: #fff; }
.action-btn.reopen { background: #6366f1; color: #fff; }
.action-btn.source { background: #f3f4f6; color: #374151; }
.action-btn[disabled] { opacity: 0.5; }
</style>
