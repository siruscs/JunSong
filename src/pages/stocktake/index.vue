<template>
  <view class="page">
    <view class="hero">
      <view class="hero-main">
        <view class="eyebrow">库存盘点</view>
        <view class="hero-title">{{ currentDeptName || '全部门店' }}</view>
      </view>
      <view class="hero-badge" v-if="activeCount > 0">
        <text class="hero-badge-num">{{ activeCount }}</text>
        <text class="hero-badge-label">进行中</text>
      </view>
    </view>

    <!-- 状态筛选标签 -->
    <scroll-view scroll-x class="tab-bar" :show-scrollbar="false">
      <view
        class="tab-item"
        :class="{ active: activeStatus === '' }"
        @tap="switchStatus('')"
      >
        <text>全部</text>
      </view>
      <view
        v-for="opt in statusOptions"
        :key="opt.value"
        class="tab-item"
        :class="{ active: activeStatus === opt.value }"
        @tap="switchStatus(opt.value)"
      >
        <text>{{ opt.label }}</text>
      </view>
    </scroll-view>

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
        :key="item.stocktakeId"
        @tap="openDetail(item)"
      >
        <view class="task-card-head">
          <text class="task-title">{{ item.takeNo }}</text>
          <text class="task-status" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</text>
        </view>
        <view class="task-card-meta">
          <text class="task-dept">{{ item.deptName || item.deptId || '—' }}</text>
          <text class="task-source" v-if="item.counterUserName">盘点人：{{ item.counterUserName }}</text>
        </view>
        <view class="task-card-foot">
          <text class="task-time">{{ formatTime(item.createTime) || '—' }}</text>
          <text class="task-time" v-if="item.postedTime">过账：{{ formatTime(item.postedTime) }}</text>
        </view>
      </view>

      <view class="load-error" v-if="!loading && loadError">
        <view class="empty-title">盘点任务加载失败</view>
        <view class="empty-subtitle">{{ loadError }}</view>
        <button class="retry-button" @tap="refresh">重新加载</button>
      </view>
      <view class="empty" v-if="!loading && !loadError && rows.length === 0">
        <view class="empty-mark">盘</view>
        <view class="empty-title">暂无盘点任务</view>
        <view class="empty-subtitle">{{ emptyHint }}</view>
      </view>
      <view class="loading" v-if="loading">加载中</view>
      <view class="loading" v-if="finished && rows.length > 0">没有更多了</view>
    </scroll-view>
  </view>
</template>

<script>
import { listStocktakes } from '@/api/stocktake.js'
import { hasExactPermission } from '@/utils/permission.js'

export default {
  data() {
    return {
      rows: [],
      activeStatus: '',
      pageNum: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      loadError: '',
      refreshing: false,
      finished: false,
      currentDeptName: '',
      statusOptions: [
        { label: '草稿', value: 'DRAFT' },
        { label: '盘点中', value: 'COUNTING' },
        { label: '已提交', value: 'SUBMITTED' },
        { label: '复盘中', value: 'RECOUNTING' },
        { label: '已审批', value: 'APPROVED' },
        { label: '已过账', value: 'POSTED' },
        { label: '已冲销', value: 'REVERSED' },
        { label: '已取消', value: 'CANCELLED' }
      ],
      statusLabels: {
        DRAFT: '草稿',
        COUNTING: '盘点中',
        SUBMITTED: '已提交',
        RECOUNTING: '复盘中',
        APPROVED: '已审批',
        POSTED: '已过账',
        REVERSED: '已冲销',
        CANCELLED: '已取消'
      }
    }
  },
  computed: {
    activeCount() {
      return this.rows.filter((r) => ['DRAFT', 'COUNTING', 'SUBMITTED', 'RECOUNTING', 'APPROVED'].includes(r.status)).length
    },
    emptyHint() {
      if (this.activeStatus) return `当前状态 ${this.statusLabels[this.activeStatus] || this.activeStatus} 暂无任务`
      return '当前门店暂无盘点任务'
    }
  },
  onShow() {
    const userInfo = uni.getStorageSync('userInfo') || {}
    this.currentDeptName = userInfo.currentDeptName || ''
    this.refresh()
  },
  methods: {
    statusLabel(status) {
      return this.statusLabels[status] || status || '-'
    },
    statusClass(status) {
      if (['POSTED', 'APPROVED'].includes(status)) return 'status-success'
      if (['REVERSED', 'CANCELLED'].includes(status)) return 'status-danger'
      if (['COUNTING', 'RECOUNTING'].includes(status)) return 'status-warning'
      return 'status-info'
    },
    formatTime(value) {
      if (!value) return ''
      return String(value).replace('T', ' ').slice(0, 16)
    },
    switchStatus(status) {
      if (this.activeStatus === status) return
      this.activeStatus = status
      this.refresh()
    },
    async refresh() {
      this.pageNum = 1
      this.finished = false
      this.rows = []
      await this.loadPage()
    },
    async loadMore() {
      if (this.loading || this.finished) return
      this.pageNum += 1
      await this.loadPage()
    },
    async onRefresh() {
      this.refreshing = true
      await this.refresh()
      this.refreshing = false
    },
    async loadPage() {
      // 权限门禁：finance:stocktake:list（与 PC 共享后端权限码）
      if (!hasExactPermission('finance:stocktake:list')) {
        this.loadError = '暂无盘点列表查看权限'
        return
      }
      this.loading = true
      this.loadError = ''
      try {
        const params = {
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }
        if (this.activeStatus) params.status = this.activeStatus
        const res = await listStocktakes(params)
        const rows = res.rows || res.data?.rows || []
        const total = res.total ?? res.data?.total ?? 0
        if (this.pageNum === 1) {
          this.rows = rows
        } else {
          this.rows = this.rows.concat(rows)
        }
        this.total = total
        this.finished = this.rows.length >= total
      } catch (err) {
        this.loadError = err?.msg || err?.errMsg || '请求失败'
      } finally {
        this.loading = false
      }
    },
    openDetail(item) {
      if (!hasExactPermission('finance:stocktake:query')) {
        uni.showToast({ title: '暂无盘点详情查看权限', icon: 'none' })
        return
      }
      uni.navigateTo({
        url: `/pages/stocktake/detail?id=${item.stocktakeId}`
      })
    }
  }
}
</script>

<style>
.page { display: flex; flex-direction: column; height: 100vh; background: #f5f7fb; }
.hero { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #fff; border-bottom: 1px solid #e5e9f2; }
.hero-main { display: flex; flex-direction: column; gap: 4px; }
.eyebrow { font-size: 12px; color: #087CF0; letter-spacing: 0.5px; }
.hero-title { font-size: 18px; font-weight: 600; color: #18202f; }
.hero-badge { display: flex; flex-direction: column; align-items: center; padding: 6px 12px; border-radius: 12px; background: #fff7e6; }
.hero-badge-num { font-size: 18px; font-weight: 700; color: #fa8c16; }
.hero-badge-label { font-size: 11px; color: #8c8c8c; }
.tab-bar { display: flex; flex-direction: row; white-space: nowrap; padding: 8px 12px; background: #fff; border-bottom: 1px solid #e5e9f2; }
.tab-item { display: inline-flex; padding: 6px 14px; margin-right: 8px; border-radius: 16px; background: #f0f2f5; font-size: 13px; color: #595959; }
.tab-item.active { background: #087CF0; color: #fff; }
.scroll { flex: 1; padding: 12px; }
.task-card { background: #fff; border-radius: 10px; padding: 14px; margin-bottom: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.task-card--active { background: #f0f7ff; }
.task-card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.task-title { font-size: 15px; font-weight: 600; color: #18202f; }
.task-status { font-size: 12px; padding: 2px 8px; border-radius: 10px; }
.status-success { background: #f6ffed; color: #52c41a; }
.status-warning { background: #fff7e6; color: #fa8c16; }
.status-danger { background: #fff1f0; color: #f5222d; }
.status-info { background: #f0f2f5; color: #595959; }
.task-card-meta { display: flex; gap: 12px; margin-bottom: 6px; font-size: 13px; color: #595959; }
.task-dept { flex: 1; }
.task-card-foot { display: flex; gap: 12px; font-size: 12px; color: #8c8c8c; }
.empty { display: flex; flex-direction: column; align-items: center; padding: 60px 20px; gap: 8px; }
.empty-mark { width: 56px; height: 56px; border-radius: 50%; background: #f0f2f5; color: #bfbfbf; font-size: 24px; display: flex; align-items: center; justify-content: center; margin-bottom: 8px; }
.empty-title { font-size: 15px; color: #18202f; font-weight: 600; }
.empty-subtitle { font-size: 13px; color: #8c8c8c; }
.load-error { display: flex; flex-direction: column; align-items: center; padding: 40px 20px; gap: 8px; }
.retry-button { margin-top: 8px; padding: 6px 16px; background: #087CF0; color: #fff; border-radius: 6px; font-size: 13px; }
.loading { text-align: center; padding: 16px; color: #8c8c8c; font-size: 13px; }
</style>
