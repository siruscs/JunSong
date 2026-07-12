<template>
  <view class="page">
    <view class="hero">
      <view class="hero-main">
        <view class="eyebrow">审批中心</view>
        <view class="hero-title">{{ isDone ? '已办任务' : '待办任务' }}</view>
      </view>
      <view class="hero-badge">
        <text class="hero-badge-num">{{ rows.length }}</text>
        <text class="hero-badge-label">{{ isDone ? '已处理' : '待处理' }}</text>
      </view>
    </view>

    <scroll-view
      scroll-y
      class="scroll"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="refresh"
    >
      <view class="task-card" v-for="item in rows" :key="item.taskId">
        <view class="task-main" hover-class="task-main--active" @tap="openDetail(item)">
          <view class="task-bar"></view>
          <view class="task-body">
            <view class="task-head">
              <text class="task-name">{{ taskName(item) }}</text>
              <text class="task-tag" v-if="item.taskName">{{ flowNodeText(item) }}</text>
            </view>
            <view class="task-meta">
              <text class="task-flow">{{ processName(item) }}</text>
              <text class="task-divider" v-if="starter(item)">·</text>
              <text class="task-starter" v-if="starter(item)">发起人：{{ starter(item) }}</text>
            </view>
            <view class="task-foot">
              <text class="task-time">{{ item.createTime || item.startTime || '-' }}</text>
              <text class="task-arrow">›</text>
            </view>
          </view>
        </view>
        <view class="task-actions" v-if="!isDone">
          <button class="action-btn reject-btn" :disabled="acting === item.taskId" @tap.stop="quickReject(item)">驳回</button>
          <button class="action-btn approve-btn" :disabled="acting === item.taskId" @tap.stop="quickApprove(item)">
            {{ acting === item.taskId ? '处理中' : '通过' }}
          </button>
        </view>
      </view>

      <view class="empty" v-if="!loading && rows.length === 0">
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

export default {
  data() {
    return {
      rows: [],
      loading: false,
      refreshing: false,
      acting: '',
      isDone: false
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
    async refresh() {
      this.refreshing = true
      await this.fetchList()
      this.refreshing = false
    },
    async fetchList() {
      this.loading = true
      try {
        const res = this.isDone ? await getDoneTasks() : await getTodoTasks()
        this.rows = res.rows || res.data || []
      } catch (e) {
        console.error('加载任务失败', e)
      } finally {
        this.loading = false
      }
    },
    openDetail(item) {
      const taskId = item.taskId || item.id
      if (!taskId) return
      uni.navigateTo({ url: '/pages/workflow/detail?taskId=' + taskId })
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
      this.rows = this.rows.filter((item) => (item.taskId || item.id) !== taskId)
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F0F4F8;
  width: 100vw;
  max-width: 100vw;
  overflow-x: hidden;
  box-sizing: border-box;
}

.hero {
  margin: 24rpx 28rpx 0;
  padding: 36rpx 30rpx;
  background: linear-gradient(135deg, #173B57, #2A6F97);
  border-radius: 24rpx;
  box-shadow: 0 20rpx 54rpx rgba(42, 111, 151, 0.18);
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
  box-shadow: 0 8rpx 26rpx rgba(42, 111, 151, 0.07);
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
  background: linear-gradient(180deg, #2A6F97, #8EC8D2);
  flex-shrink: 0;
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

.task-meta {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 12rpx;
  min-width: 0;
}

.task-flow {
  font-size: 25rpx;
  color: #2A6F97;
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
  background: linear-gradient(135deg, #2A6F97, #3A8DB8);
  color: #FFFFFF;
  box-shadow: 0 4rpx 14rpx rgba(42, 111, 151, 0.25);
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

.empty {
  padding: 100rpx 40rpx;
  text-align: center;
}

.empty-mark {
  width: 96rpx;
  height: 96rpx;
  line-height: 96rpx;
  margin: 0 auto 22rpx;
  border-radius: 28rpx;
  background: #ECF4F7;
  color: #2A6F97;
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
