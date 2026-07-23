<template>
  <view class="page">
    <view class="hero">
      <view class="hero-main">
        <view class="eyebrow">{{ isDone ? '审批记录' : '统一办理' }}</view>
        <view class="hero-title">{{ isDone ? '已办任务' : '任务中心' }}</view>
      </view>
      <view class="hero-badge">
        <text class="hero-badge-num">{{ filteredRows.length }}</text>
        <text class="hero-badge-label">{{ isDone ? '已处理' : '待处理' }}</text>
      </view>
    </view>

    <view class="filter-tabs" v-if="!isDone">
      <view class="filter-tab" v-for="tab in filterTabs" :key="tab.value" :class="{ active: activeFilter === tab.value }" @tap="activeFilter = tab.value">
        <text>{{ tab.label }}</text>
        <text class="filter-count">{{ tab.count }}</text>
      </view>
    </view>

    <view class="partial-notice" v-if="partialNotice">{{ partialNotice }}</view>

    <scroll-view
      scroll-y
      class="scroll"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="refresh"
    >
      <view class="task-card" v-for="item in filteredRows" :key="item.key">
        <view class="task-main" hover-class="task-main--active" @tap="openTask(item)">
          <view class="task-bar" :class="'urgency-' + item.urgency"></view>
          <view class="task-body">
            <view class="task-head">
              <text class="task-name">{{ item.title }}</text>
              <text class="urgency-tag" :class="'urgency-' + item.urgency">{{ urgencyText(item.urgency) }}</text>
            </view>
            <view class="task-meta">
              <text class="task-flow">{{ item.category }}</text>
              <text class="task-divider" v-if="item.detail">·</text>
              <text class="task-starter" v-if="item.detail">{{ item.detail }}</text>
            </view>
            <view class="task-foot">
              <text class="task-time">{{ item.timeText || '-' }}</text>
              <text class="task-arrow">›</text>
            </view>
          </view>
        </view>
        <view class="task-actions" v-if="!isDone && item.type === 'approval'">
          <button class="action-btn reject-btn" :disabled="acting === item.taskId" @tap.stop="quickReject(item)">驳回</button>
          <button class="action-btn approve-btn" :disabled="acting === item.taskId" @tap.stop="quickApprove(item)">
            {{ acting === item.taskId ? '处理中' : '通过' }}
          </button>
        </view>
      </view>

      <view class="load-error" v-if="!loading && loadError">
        <view class="empty-title">任务加载失败</view>
        <view class="empty-subtitle">{{ loadError }}</view>
        <button class="retry-button" @tap="refresh">重新加载</button>
      </view>
      <view class="empty" v-if="!loading && !loadError && filteredRows.length === 0">
        <view class="empty-mark">办</view>
        <view class="empty-title">{{ isDone ? '暂无已办任务' : '暂无待办任务' }}</view>
        <view class="empty-subtitle">{{ isDone ? '还没有处理过任何审批' : '所有流程都已处理完毕' }}</view>
      </view>
      <view class="loading" v-if="loading">加载中</view>
    </scroll-view>
  </view>
</template>

<script>
import { getTodoTasks, getDoneTasks, approveTask, rejectTask } from '@/api/workflow.js'
import { listData } from '@/api/index.js'
import { hasActionPermission, hasExactPermission } from '@/utils/permission.js'
import { buildTaskCenterItems } from '@/utils/taskCenter.js'

export default {
  data() {
    return {
      rows: [],
      loading: false,
      refreshing: false,
      acting: '',
      isDone: false,
      activeFilter: 'all',
      loadError: '',
      partialNotice: '',
      requestVersion: 0
    }
  },
  computed: {
    filteredRows() {
      if (this.isDone || this.activeFilter === 'all') return this.rows
      return this.rows.filter((item) => item.type === this.activeFilter)
    },
    filterTabs() {
      return [
        { label: '全部任务', value: 'all', count: this.rows.length },
        { label: '审批任务', value: 'approval', count: this.rows.filter((item) => item.type === 'approval').length },
        { label: '待核销', value: 'verification', count: this.rows.filter((item) => item.type === 'verification').length }
      ]
    },
    canLoadExpenseTasks() {
      return !this.isDone && hasExactPermission('finance:expense:list') && hasActionPermission('expense', 'verify')
    }
  },
  onLoad(options) {
    this.isDone = options.tab === 'done'
    if (this.isDone) {
      uni.setNavigationBarTitle({ title: '已办任务' })
    }
  },
  onShow() {
    this.refresh()
  },
  methods: {
    taskName(item) {
      return item.taskName || item.name || item.businessTitle || '未命名任务'
    },
    processName(item) {
      return item.processName || item.processDefinitionName || item.flowName || '未知流程'
    },
    flowNodeText(item) {
      return item.nodeName || item.taskName || '当前节点'
    },
    starter(item) {
      return item.startUserName || item.createBy || item.applyUser || ''
    },
    urgencyText(urgency) {
      const labels = { overdue: '已逾期', soon: '即将到期', attention: '需关注', normal: '待处理' }
      return labels[urgency] || '待处理'
    },
    async refresh() {
      this.refreshing = true
      const requestVersion = this.requestVersion + 1
      await this.fetchList()
      if (requestVersion === this.requestVersion) this.refreshing = false
    },
    async loadAllExpenseTasks() {
      const pageSize = 100
      let pageNum = 1
      let totalPages = 1
      const rows = []
      while (pageNum <= totalPages) {
        const response = await listData('/finance/expense', { pageNum, pageSize, status: '0' })
        const pageRows = response.rows || response.data || []
        if (!Array.isArray(pageRows)) throw new Error('待核销费用数据格式异常')
        rows.push(...pageRows)
        const total = Number(response.total)
        totalPages = Number.isFinite(total)
          ? Math.max(1, Math.ceil(total / pageSize))
          : (pageRows.length === pageSize ? pageNum + 1 : pageNum)
        pageNum += 1
      }
      return rows
    },
    async fetchList() {
      const requestVersion = ++this.requestVersion
      this.loading = true
      this.loadError = ''
      this.partialNotice = ''
      try {
        const sources = [{ type: 'approval', promise: this.isDone ? getDoneTasks() : getTodoTasks() }]
        if (this.canLoadExpenseTasks) {
          sources.push({ type: 'verification', promise: this.loadAllExpenseTasks() })
        }
        const results = await Promise.allSettled(sources.map((source) => source.promise))
        if (requestVersion !== this.requestVersion) return
        let approvals = []
        let expenses = []
        const failedSources = []
        results.forEach((result, index) => {
          if (result.status === 'rejected') {
            failedSources.push(sources[index].type)
            return
          }
          const payload = result.value || {}
          const list = Array.isArray(payload) ? payload : (payload.rows || payload.data || [])
          if (sources[index].type === 'verification') expenses = Array.isArray(list) ? list : []
          else approvals = Array.isArray(list) ? list : []
        })
        if (failedSources.length === sources.length) {
          this.rows = []
          this.loadError = '请检查网络或权限后重试'
          return
        }
        if (failedSources.length > 0) {
          const labels = failedSources.map((type) => type === 'approval' ? '审批任务' : '待核销费用')
          this.partialNotice = `${labels.join('、')}暂时未加载，可下拉刷新重试。`
        }
        this.rows = buildTaskCenterItems({ approvals, expenses, preserveOrder: this.isDone })
      } catch (e) {
        if (requestVersion !== this.requestVersion) return
        this.rows = []
        this.loadError = '请检查网络或权限后重试'
      } finally {
        if (requestVersion === this.requestVersion) this.loading = false
      }
    },
    openTask(item) {
      if (item.type === 'verification') {
        const expenseId = Number(item.expenseId)
        if (!Number.isSafeInteger(expenseId) || expenseId <= 0) return
        uni.navigateTo({ url: '/pages/expense-verify/index?expenseIds=' + expenseId })
        return
      }
      const taskId = item.taskId || item.id
      if (!taskId) return
      uni.navigateTo({ url: '/pages/workflow/detail?taskId=' + encodeURIComponent(taskId) })
    },
    quickApprove(item) {
      const taskId = item.taskId || item.id
      if (!taskId) return
      uni.showModal({
        title: '审批通过',
        editable: true,
        placeholderText: '请输入审批意见（可不填）',
        success: async (res) => {
          if (!res.confirm) return
          this.acting = taskId
          try {
            await approveTask(taskId, { comment: res.content || '同意' })
            uni.showToast({ title: '审批通过', icon: 'success' })
            this.removeTask(taskId)
          } catch (e) {
            console.error('审批通过失败', e)
          } finally {
            this.acting = ''
          }
        }
      })
    },
    quickReject(item) {
      const taskId = item.taskId || item.id
      if (!taskId) return
      uni.showModal({
        title: '驳回任务',
        editable: true,
        placeholderText: '请输入驳回原因',
        success: async (res) => {
          if (!res.confirm) return
          const comment = (res.content || '').trim()
          if (!comment) {
            uni.showToast({ title: '请填写驳回原因', icon: 'none' })
            return
          }
          this.acting = taskId
          try {
            await rejectTask(taskId, { comment })
            uni.showToast({ title: '已驳回', icon: 'success' })
            this.removeTask(taskId)
          } catch (e) {
            console.error('驳回失败', e)
          } finally {
            this.acting = ''
          }
        }
      })
    },
    removeTask(taskId) {
      this.requestVersion += 1
      this.loading = false
      this.refreshing = false
      this.rows = this.rows.filter((item) => (item.taskId || item.id) !== taskId)
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #E8EEF5;
  width: 100vw;
  max-width: 100vw;
  overflow-x: hidden;
  box-sizing: border-box;
}

.hero {
  margin: 24rpx 28rpx 0;
  padding: 36rpx 30rpx;
  background: linear-gradient(135deg, #123F73, #087CF0);
  border-radius: 24rpx;
  box-shadow: 0 20rpx 54rpx rgba(8, 124, 240, 0.18);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-main {
  flex: 1;
  min-width: 0;
}

.eyebrow {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.72);
  font-weight: 600;
  letter-spacing: 2rpx;
}

.hero-title {
  margin-top: 8rpx;
  font-size: 42rpx;
  font-weight: 800;
  color: #FFFFFF;
}

.hero-badge {
  width: 110rpx;
  height: 110rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.14);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-badge-num {
  font-size: 40rpx;
  font-weight: 800;
  color: #FFFFFF;
}

.hero-badge-label {
  margin-top: 2rpx;
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.66);
}

.filter-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8rpx;
  margin: 20rpx 28rpx 0;
  padding: 8rpx;
  border: 1rpx solid #D5E0EC;
  border-radius: 8rpx;
  background: #FFFFFF;
}

.filter-tab {
  height: 68rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  border-radius: 6rpx;
  color: #60758C;
  font-size: 24rpx;
}

.filter-tab.active {
  background: #E8F3FF;
  color: #087CF0;
  font-weight: 700;
}

.filter-count {
  min-width: 30rpx;
  text-align: center;
  font-size: 20rpx;
}

.partial-notice {
  margin: 16rpx 28rpx 0;
  padding: 18rpx 22rpx;
  border-left: 6rpx solid #D97706;
  border-radius: 6rpx;
  background: #FFF7E6;
  color: #8A4B08;
  font-size: 23rpx;
  line-height: 1.5;
}

.scroll {
  width: 100%;
  height: calc(100vh - 220rpx);
  padding: 20rpx 28rpx 60rpx;
  box-sizing: border-box;
  overflow-x: hidden;
}

.task-card {
  margin-bottom: 20rpx;
  background: #FFFFFF;
  border-radius: 22rpx;
  box-shadow: 0 8rpx 26rpx rgba(8, 124, 240, 0.07);
  border: 1rpx solid rgba(226, 232, 240, 0.9);
  overflow: hidden;
}

.task-main {
  display: flex;
}

.task-main--active {
  opacity: 0.9;
}

.task-bar {
  width: 6rpx;
  background: linear-gradient(180deg, #087CF0, #A8C7E5);
  flex-shrink: 0;
}

.task-bar.urgency-overdue {
  background: #D92D20;
}

.task-bar.urgency-soon,
.task-bar.urgency-attention {
  background: #D97706;
}

.task-body {
  flex: 1;
  min-width: 0;
  padding: 24rpx 26rpx;
  box-sizing: border-box;
}

.task-head {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.task-name {
  flex: 1;
  min-width: 0;
  font-size: 32rpx;
  line-height: 42rpx;
  font-weight: 800;
  color: #102A3A;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-tag {
  flex-shrink: 0;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  background: #E0F2FE;
  color: #075985;
  font-size: 20rpx;
  font-weight: 700;
}

.urgency-tag {
  flex-shrink: 0;
  padding: 5rpx 12rpx;
  border-radius: 6rpx;
  background: #EEF2F6;
  color: #60758C;
  font-size: 20rpx;
  font-weight: 700;
}

.urgency-tag.urgency-overdue {
  background: #FEE4E2;
  color: #B42318;
}

.urgency-tag.urgency-soon,
.urgency-tag.urgency-attention {
  background: #FEF0C7;
  color: #B54708;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 12rpx;
  min-width: 0;
}

.task-flow {
  font-size: 25rpx;
  color: #087CF0;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-divider {
  color: #CBD5E1;
  font-size: 24rpx;
}

.task-starter {
  font-size: 24rpx;
  color: #708196;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14rpx;
  padding-top: 14rpx;
  border-top: 1rpx solid #F1F5F9;
}

.task-time {
  font-size: 22rpx;
  color: #94A3B8;
}

.task-arrow {
  font-size: 36rpx;
  color: #CBD5E1;
  line-height: 1;
}

.task-actions {
  display: flex;
  gap: 16rpx;
  padding: 18rpx 26rpx 22rpx;
  background: #FAFCFE;
  border-top: 1rpx solid #F1F5F9;
}

.action-btn {
  flex: 1;
  height: 74rpx;
  line-height: 74rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 700;
  text-align: center;
  border: none;
  margin: 0;
  padding: 0;
}

.action-btn::after {
  border: none;
}

.approve-btn {
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
  box-shadow: 0 4rpx 14rpx rgba(8, 124, 240, 0.25);
}

.reject-btn {
  background: #FEE2E2;
  color: #B91C1C;
}

.action-btn[disabled] {
  background: #E2E8F0;
  color: #94A3B8;
  box-shadow: none;
}

.empty,
.load-error {
  padding: 100rpx 40rpx;
  text-align: center;
}

.retry-button {
  width: 220rpx;
  height: 72rpx;
  margin: 28rpx auto 0;
  padding: 0;
  border: 1rpx solid #B8D4F0;
  border-radius: 8rpx;
  background: #E8F3FF;
  color: #087CF0;
  font-size: 26rpx;
  line-height: 72rpx;
}

.empty-mark {
  width: 96rpx;
  height: 96rpx;
  line-height: 96rpx;
  margin: 0 auto 22rpx;
  border-radius: 28rpx;
  background: #ECF4F7;
  color: #087CF0;
  font-size: 36rpx;
  font-weight: 800;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #102A3A;
}

.empty-subtitle {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #94A3B8;
}

.loading {
  padding: 50rpx;
  text-align: center;
  color: #94A3B8;
  font-size: 24rpx;
}
</style>
