<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <text class="loading-text">加载中...</text>
    </view>

    <template v-else-if="record">
      <view class="hero-card">
        <view class="hero-bg"></view>
        <view class="hero-content">
          <view class="hero-eyebrow">{{ processName(record) }}</view>
          <view class="hero-title">{{ taskName(record) }}</view>
          <view class="hero-meta" v-if="currentNode(record)">当前节点：{{ currentNode(record) }}</view>
        </view>
      </view>

      <view class="section-card">
        <view class="section-title">任务信息</view>
        <view class="field-row">
          <text class="field-label">任务名称</text>
          <text class="field-value">{{ taskName(record) }}</text>
        </view>
        <view class="field-row">
          <text class="field-label">流程名称</text>
          <text class="field-value">{{ processName(record) }}</text>
        </view>
        <view class="field-row">
          <text class="field-label">发起人</text>
          <text class="field-value">{{ starter(record) }}</text>
        </view>
        <view class="field-row">
          <text class="field-label">创建时间</text>
          <text class="field-value">{{ record.createTime || record.startTime || '-' }}</text>
        </view>
        <view class="field-row" v-if="record.businessTitle || record.businessKey">
          <text class="field-label">业务单号</text>
          <text class="field-value">{{ record.businessTitle || record.businessKey }}</text>
        </view>
        <view class="field-row" v-if="record.taskDescription || record.description">
          <text class="field-label">任务说明</text>
          <text class="field-value">{{ record.taskDescription || record.description }}</text>
        </view>
      </view>

      <view class="section-card" v-if="variables.length">
        <view class="section-title">审批内容</view>
        <view class="field-row" v-for="(item, idx) in variables" :key="idx">
          <text class="field-label">{{ item.label }}</text>
          <text class="field-value">{{ item.value }}</text>
        </view>
      </view>

      <view class="footer-placeholder"></view>
    </template>

    <view v-else class="empty-wrap">
      <text class="empty-text">暂无任务数据</text>
    </view>

    <view v-if="record && !loading" class="footer-bar">
      <button class="action-btn reject-btn" @tap="openRejectPanel">驳回</button>
      <button class="action-btn approve-btn" @tap="openApprovePanel">审批通过</button>
    </view>

    <view class="mask" v-if="approveOpen" @tap="closeApprove">
      <view class="panel" @tap.stop>
        <view class="panel-title">审批通过</view>
        <view class="panel-sub">请填写审批意见</view>
        <textarea
          class="panel-textarea"
          v-model="approveForm.comment"
          placeholder="请输入审批意见，可不填"
          maxlength="200"
        />
        <view class="panel-actions">
          <button class="panel-cancel" @tap="closeApprove">取消</button>
          <button class="panel-confirm approve" :disabled="submitting" @tap="submitApprove">
            {{ submitting ? '提交中' : '确认通过' }}
          </button>
        </view>
      </view>
    </view>

    <view class="mask" v-if="rejectOpen" @tap="closeReject">
      <view class="panel" @tap.stop>
        <view class="panel-title">驳回任务</view>
        <view class="panel-sub">请选择驳回节点并填写原因</view>
        <view class="panel-field" v-if="backNodes.length">
          <text class="panel-label">驳回至</text>
          <picker class="panel-picker" :range="backNodeLabels" :value="backNodeIndex" @change="onBackNodeChange">
            <view class="panel-picker-box">
              <text class="panel-picker-text">{{ selectedBackNodeLabel || '请选择节点' }}</text>
              <text class="panel-picker-arrow">›</text>
            </view>
          </picker>
        </view>
        <textarea
          class="panel-textarea"
          v-model="rejectForm.comment"
          placeholder="请输入驳回原因"
          maxlength="200"
        />
        <view class="panel-actions">
          <button class="panel-cancel" @tap="closeReject">取消</button>
          <button class="panel-confirm reject" :disabled="submitting" @tap="submitReject">
            {{ submitting ? '提交中' : '确认驳回' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getTaskDetail, approveTask, rejectTask } from '@/api/workflow.js'

export default {
  data() {
    return {
      taskId: '',
      record: null,
      loading: true,
      submitting: false,
      approveOpen: false,
      rejectOpen: false,
      approveForm: { comment: '' },
      rejectForm: { comment: '', targetNode: '' },
      backNodes: [],
      backNodeIndex: 0
    }
  },
  computed: {
    backNodeLabels() {
      return this.backNodes.map((node) => node.nodeName || node.name || node.label || '节点')
    },
    selectedBackNodeLabel() {
      const node = this.backNodes[this.backNodeIndex]
      if (!node) return ''
      return node.nodeName || node.name || node.label || '节点'
    },
    variables() {
      const vars = this.record?.variables || this.record?.processVariables || this.record?.formData
      if (!vars || typeof vars !== 'object') return []
      return Object.keys(vars)
        .filter((key) => !['__$'].includes(key))
        .map((key) => {
          const raw = vars[key]
          const value = raw === null || raw === undefined ? '' : String(raw)
          if (value === '') return null
          return { label: this.formatLabel(key), value }
        })
        .filter(Boolean)
    }
  },
  onLoad(options) {
    this.taskId = options.taskId || ''
    if (!this.taskId) {
      uni.showToast({ title: '缺少任务参数', icon: 'none' })
      this.loading = false
      return
    }
    this.loadDetail()
  },
  methods: {
    taskName(item) {
      return item.taskName || item.name || item.businessTitle || '未命名任务'
    },
    processName(item) {
      return item.processName || item.processDefinitionName || item.flowName || '未知流程'
    },
    currentNode(item) {
      return item.nodeName || item.taskName || ''
    },
    starter(item) {
      return item.startUserName || item.createBy || item.applyUser || item.initiator || '-'
    },
    formatLabel(key) {
      const map = {
        reason: '事由', amount: '金额', days: '天数', startDate: '开始日期', endDate: '结束日期',
        remark: '备注', content: '内容', title: '标题', applicant: '申请人', dept: '部门'
      }
      return map[key] || key
    },
    async loadDetail() {
      this.loading = true
      try {
        const res = await getTaskDetail(this.taskId)
        this.record = res.data || res
        this.backNodes = this.record.backNodes || this.record.nodeList || this.record.rejectNodes || []
      } catch (e) {
        console.error('加载任务详情失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    openApprovePanel() {
      this.approveForm.comment = ''
      this.approveOpen = true
    },
    closeApprove() {
      this.approveOpen = false
    },
    async submitApprove() {
      if (this.submitting) return
      this.submitting = true
      try {
        await approveTask(this.taskId, { comment: this.approveForm.comment || '同意' })
        uni.showToast({ title: '审批通过', icon: 'success' })
        this.closeApprove()
        setTimeout(() => uni.navigateBack(), 1200)
      } catch (e) {
        console.error('审批通过失败', e)
      } finally {
        this.submitting = false
      }
    },
    openRejectPanel() {
      this.rejectForm.comment = ''
      this.rejectForm.targetNode = this.backNodes[0]?.nodeId || this.backNodes[0]?.id || ''
      this.backNodeIndex = 0
      this.rejectOpen = true
    },
    closeReject() {
      this.rejectOpen = false
    },
    onBackNodeChange(e) {
      this.backNodeIndex = Number(e.detail.value)
      const node = this.backNodes[this.backNodeIndex]
      this.rejectForm.targetNode = node?.nodeId || node?.id || node?.key || ''
    },
    async submitReject() {
      if (this.submitting) return
      const comment = (this.rejectForm.comment || '').trim()
      if (!comment) {
        uni.showToast({ title: '请输入驳回原因', icon: 'none' })
        return
      }
      if (this.backNodes.length && !this.rejectForm.targetNode) {
        uni.showToast({ title: '请选择驳回节点', icon: 'none' })
        return
      }
      this.submitting = true
      try {
        await rejectTask(this.taskId, {
          comment,
          targetNode: this.rejectForm.targetNode
        })
        uni.showToast({ title: '已驳回', icon: 'success' })
        this.closeReject()
        setTimeout(() => uni.navigateBack(), 1200)
      } catch (e) {
        console.error('驳回失败', e)
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F0F4F8;
  padding-bottom: env(safe-area-inset-bottom);
}

.loading-wrap,
.empty-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

.loading-text,
.empty-text {
  font-size: 28rpx;
  color: #94A3B8;
}

.hero-card {
  position: relative;
  margin: 24rpx 28rpx;
  border-radius: 22rpx;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #173B57, #2A6F97, #3A8DB8);
}

.hero-content {
  position: relative;
  z-index: 1;
  padding: 40rpx 36rpx;
}

.hero-eyebrow {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 2rpx;
  margin-bottom: 12rpx;
}

.hero-title {
  font-size: 38rpx;
  font-weight: 800;
  color: #FFFFFF;
  line-height: 1.4;
}

.hero-meta {
  margin-top: 14rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.82);
}

.section-card {
  margin: 0 28rpx 20rpx;
  background: #FFFFFF;
  border-radius: 22rpx;
  padding: 28rpx 32rpx;
  box-shadow: 0 2rpx 16rpx rgba(42, 111, 151, 0.06);
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
  margin-bottom: 18rpx;
  padding-left: 16rpx;
  border-left: 4rpx solid #2A6F97;
}

.field-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #F0F4F8;
}

.field-row:last-child {
  border-bottom: none;
}

.field-label {
  font-size: 26rpx;
  color: #5A6B7F;
  flex-shrink: 0;
}

.field-value {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  color: #1A2332;
  font-weight: 600;
  text-align: right;
  word-break: break-all;
}

.footer-placeholder {
  height: 150rpx;
}

.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 16rpx 28rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #FFFFFF;
  box-shadow: 0 -2rpx 16rpx rgba(42, 111, 151, 0.08);
  z-index: 100;
}

.action-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
  font-weight: 700;
  border-radius: 999rpx;
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
  box-shadow: 0 6rpx 18rpx rgba(42, 111, 151, 0.25);
}

.reject-btn {
  background: #FEE2E2;
  color: #B91C1C;
}

.mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 200;
  display: flex;
  align-items: flex-end;
  background: rgba(15, 23, 42, 0.45);
}

.panel {
  width: 100%;
  padding: 30rpx 28rpx calc(30rpx + env(safe-area-inset-bottom));
  border-radius: 28rpx 28rpx 0 0;
  background: #FFFFFF;
  box-sizing: border-box;
}

.panel-title {
  font-size: 34rpx;
  font-weight: 800;
  color: #1A2332;
}

.panel-sub {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #708196;
}

.panel-field {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 22rpx;
}

.panel-label {
  flex-shrink: 0;
  width: 120rpx;
  font-size: 26rpx;
  font-weight: 700;
  color: #1A2332;
}

.panel-picker {
  flex: 1;
  min-width: 0;
}

.panel-picker-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  height: 78rpx;
  padding: 0 22rpx;
  background: #F8FAFC;
  border: 1rpx solid #E2E8F0;
  border-radius: 16rpx;
  box-sizing: border-box;
}

.panel-picker-text {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  color: #1A2332;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-picker-arrow {
  font-size: 32rpx;
  color: #94A3B8;
  line-height: 1;
}

.panel-textarea {
  width: 100%;
  margin-top: 22rpx;
  height: 180rpx;
  padding: 22rpx;
  background: #F8FAFC;
  border: 1rpx solid #E2E8F0;
  border-radius: 16rpx;
  box-sizing: border-box;
  font-size: 26rpx;
  color: #1A2332;
}

.panel-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 26rpx;
}

.panel-cancel,
.panel-confirm {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 700;
  border: none;
  margin: 0;
  padding: 0;
}

.panel-cancel::after,
.panel-confirm::after {
  border: none;
}

.panel-cancel {
  background: #F1F5F9;
  color: #475569;
}

.panel-confirm.approve {
  background: linear-gradient(135deg, #2A6F97, #3A8DB8);
  color: #FFFFFF;
}

.panel-confirm.reject {
  background: #EF4444;
  color: #FFFFFF;
}

.panel-confirm[disabled] {
  opacity: 0.6;
}
</style>
