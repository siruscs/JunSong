<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">动作详情</text>
      <text class="hero-sub">候选会员 · 执行反馈</text>
    </view>

    <scroll-view scroll-y class="scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">

      <!-- 动作信息 -->
      <view class="section-card" v-if="actionInfo">
        <view class="section-header">
          <view class="section-dot" style="background:#0EA5E9"></view>
          <text class="section-title">{{ actionInfo.actionTitle || '增长动作' }}</text>
        </view>
        <view class="info-row" v-if="actionInfo.actionType">
          <text class="info-label">动作类型</text>
          <text class="info-value">{{ actionInfo.actionType }}</text>
        </view>
        <view class="info-row" v-if="actionInfo.pressureLevel">
          <text class="info-label">压力等级</text>
          <text class="info-value">{{ actionInfo.pressureLevel }}</text>
        </view>
        <view class="info-row" v-if="actionInfo.candidateCount !== undefined">
          <text class="info-label">候选人数</text>
          <text class="info-value">{{ actionInfo.candidateCount }}</text>
        </view>
        <view class="info-row" v-if="actionInfo.executedCount !== undefined">
          <text class="info-label">已执行</text>
          <text class="info-value">{{ actionInfo.executedCount }}</text>
        </view>
        <view class="info-row" v-if="actionInfo.actionReason">
          <text class="info-label">动作原因</text>
          <text class="info-value">{{ actionInfo.actionReason }}</text>
        </view>
        <view class="info-block" v-if="actionInfo.suggestedScript">
          <text class="info-label">建议话术</text>
          <text class="info-text">{{ actionInfo.suggestedScript }}</text>
        </view>
      </view>

      <!-- 候选会员列表 -->
      <view class="section-card">
        <view class="section-header">
          <view class="section-dot" style="background:#2A6F97"></view>
          <text class="section-title">候选会员</text>
          <text class="section-count">{{ members.length }} 人</text>
        </view>
        <view class="list" v-if="members.length">
          <view class="member-card" v-for="item in members" :key="item.id || item.memberId">
            <view class="member-main">
              <text class="member-name">{{ item.memberName || '未知会员' }}</text>
              <text class="member-sub" v-if="item.memberNo">编号 {{ item.memberNo }}</text>
              <text class="member-sub" v-if="item.segmentType">分层 {{ item.segmentType }}</text>
            </view>
            <view class="member-side">
              <text class="member-status" :class="execStatusClass(item.executeStatus)">{{ execStatusText(item.executeStatus) }}</text>
              <button class="feedback-btn" v-if="canExecute(item)" @tap="openFeedback(item)">反馈</button>
            </view>
          </view>
        </view>
        <view class="empty-inline" v-else>
          <text class="empty-inline-text">暂无候选会员</text>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty" v-if="!actionInfo && !members.length && !loading">
        <text class="empty-title">暂无动作详情</text>
        <text class="empty-sub">请从增长动作列表进入</text>
      </view>
    </scroll-view>

    <!-- 执行反馈弹层 -->
    <view class="modal-mask" v-if="feedbackVisible" @tap="closeFeedback">
      <view class="modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">执行反馈</text>
          <text class="modal-close" @tap="closeFeedback">×</text>
        </view>
        <view class="modal-body">
          <view class="modal-member" v-if="currentMember">
            <text class="modal-member-name">{{ currentMember.memberName || '未知会员' }}</text>
            <text class="modal-member-sub" v-if="currentMember.memberNo">{{ currentMember.memberNo }}</text>
          </view>
          <view class="form-row">
            <text class="form-label">执行结果</text>
            <view class="form-options">
              <view class="form-option" :class="{ active: feedbackForm.executeStatus === '1' }" @tap="feedbackForm.executeStatus = '1'">
                <text>成功</text>
              </view>
              <view class="form-option" :class="{ active: feedbackForm.executeStatus === '2' }" @tap="feedbackForm.executeStatus = '2'">
                <text>失败</text>
              </view>
              <view class="form-option" :class="{ active: feedbackForm.executeStatus === '3' }" @tap="feedbackForm.executeStatus = '3'">
                <text>待跟进</text>
              </view>
            </view>
          </view>
          <view class="form-row">
            <text class="form-label">备注</text>
            <textarea class="form-textarea" v-model="feedbackForm.executeNote" placeholder="请输入执行备注（可选）" maxlength="200" />
          </view>
        </view>
        <view class="modal-footer">
          <button class="modal-btn cancel" @tap="closeFeedback">取消</button>
          <button class="modal-btn confirm" :disabled="submitting" @tap="submitFeedback">{{ submitting ? '提交中' : '提交' }}</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'

export default {
  data() {
    return {
      actionId: null,
      actionInfo: null,
      members: [],
      loading: false,
      refreshing: false,
      feedbackVisible: false,
      currentMember: null,
      submitting: false,
      feedbackForm: {
        executeStatus: '1',
        executeNote: ''
      }
    }
  },
  onLoad(options) {
    this.actionId = options?.actionId || null
    if (this.actionId) {
      this.loadMembers()
    }
  },
  onShow() {
    if (this.actionId && !this.members.length) {
      this.loadMembers()
    }
  },
  methods: {
    async loadMembers() {
      if (!this.actionId) return
      this.loading = true
      try {
        const res = await request({
          url: '/member/growth-action/members',
          method: 'GET',
          data: { actionId: this.actionId }
        })
        const list = res.data || res || []
        this.members = Array.isArray(list) ? list : []
        // 从成员中提取动作信息（如果存在）
        if (this.members.length && !this.actionInfo) {
          const first = this.members[0]
          this.actionInfo = {
            actionTitle: first.actionTitle,
            actionType: first.actionType,
            pressureLevel: first.pressureLevel,
            actionReason: first.actionReason,
            suggestedScript: first.suggestedScript,
            candidateCount: this.members.length,
            executedCount: this.members.filter(m => m.executeStatus === '1' || m.executeStatus === 1).length
          }
        }
      } catch (e) {
        console.log('action members load failed', e)
        this.members = []
      } finally {
        this.loading = false
      }
    },
    onRefresh() {
      this.refreshing = true
      this.loadMembers().finally(() => {
        this.refreshing = false
      })
    },
    canExecute(item) {
      const s = item.executeStatus
      return s !== '1' && s !== 1 && s !== '2' && s !== 2
    },
    execStatusText(val) {
      if (val === '1' || val === 1) return '已成功'
      if (val === '2' || val === 2) return '已失败'
      if (val === '3' || val === 3) return '待跟进'
      return '待执行'
    },
    execStatusClass(val) {
      if (val === '1' || val === 1) return 'success'
      if (val === '2' || val === 2) return 'danger'
      if (val === '3' || val === 3) return 'warning'
      return 'muted'
    },
    openFeedback(member) {
      this.currentMember = member
      this.feedbackForm = {
        executeStatus: '1',
        executeNote: ''
      }
      this.feedbackVisible = true
    },
    closeFeedback() {
      this.feedbackVisible = false
      this.currentMember = null
    },
    async submitFeedback() {
      if (!this.currentMember || !this.actionId) return
      if (!this.feedbackForm.executeStatus) {
        uni.showToast({ title: '请选择执行结果', icon: 'none' })
        return
      }
      this.submitting = true
      try {
        await request({
          url: '/member/growth-action/execute',
          method: 'POST',
          data: {
            actionId: Number(this.actionId),
            memberId: this.currentMember.memberId,
            executeStatus: this.feedbackForm.executeStatus,
            executeNote: this.feedbackForm.executeNote || ''
          }
        })
        uni.showToast({ title: '反馈成功', icon: 'success' })
        // 更新本地状态
        const target = this.members.find(m => m.memberId === this.currentMember.memberId)
        if (target) {
          target.executeStatus = this.feedbackForm.executeStatus
        }
        this.closeFeedback()
      } catch (e) {
        console.log('feedback submit failed', e)
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
}

.hero {
  margin: 24rpx 28rpx 0;
  padding: 32rpx 30rpx;
  background: linear-gradient(135deg, #0C4A6E, #0EA5E9);
  border-radius: 24rpx;
  box-shadow: 0 12rpx 32rpx rgba(14, 165, 233, 0.18);
}

.hero-title {
  font-size: 38rpx;
  font-weight: 800;
  color: #FFFFFF;
  display: block;
}

.hero-sub {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.72);
  margin-top: 8rpx;
  display: block;
}

.scroll {
  height: calc(100vh - 200rpx);
  padding: 24rpx 28rpx 46rpx;
  box-sizing: border-box;
}

.section-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 16rpx rgba(42, 111, 151, 0.06);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.section-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
  flex: 1;
}

.section-count {
  font-size: 22rpx;
  color: #94A3B8;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #F0F4F8;
}

.info-label {
  font-size: 24rpx;
  color: #94A3B8;
}

.info-value {
  font-size: 24rpx;
  font-weight: 600;
  color: #1A2332;
  text-align: right;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-block {
  margin-top: 16rpx;
  padding: 16rpx;
  background: #F8FBFD;
  border-radius: 12rpx;
}

.info-block .info-label {
  display: block;
  margin-bottom: 8rpx;
}

.info-text {
  font-size: 24rpx;
  color: #1A2332;
  line-height: 36rpx;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.member-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 24rpx;
  background: #F8FBFD;
  border-radius: 16rpx;
  gap: 16rpx;
}

.member-main {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 26rpx;
  font-weight: 700;
  color: #1A2332;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-sub {
  font-size: 22rpx;
  color: #94A3B8;
  margin-top: 4rpx;
  display: block;
}

.member-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8rpx;
  flex-shrink: 0;
}

.member-status {
  font-size: 20rpx;
  font-weight: 600;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  background: rgba(148, 163, 184, 0.12);
  color: #64748B;
}

.member-status.success {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.member-status.danger {
  background: rgba(239, 68, 68, 0.1);
  color: #EF4444;
}

.member-status.warning {
  background: rgba(245, 158, 11, 0.1);
  color: #F59E0B;
}

.member-status.muted {
  background: rgba(148, 163, 184, 0.12);
  color: #94A3B8;
}

.feedback-btn {
  font-size: 22rpx;
  font-weight: 600;
  color: #FFFFFF;
  background: #2A6F97;
  border-radius: 999rpx;
  padding: 4rpx 20rpx;
  line-height: 40rpx;
  height: 48rpx;
  min-height: 48rpx;
  border: none;
}

.feedback-btn::after {
  border: none;
}

.empty-inline {
  padding: 32rpx 0;
  text-align: center;
}

.empty-inline-text {
  font-size: 24rpx;
  color: #94A3B8;
}

.empty {
  padding: 80rpx 30rpx;
  text-align: center;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1A2332;
  display: block;
}

.empty-sub {
  font-size: 24rpx;
  color: #94A3B8;
  margin-top: 12rpx;
  display: block;
}

/* ===== 反馈弹层 ===== */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 100;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.modal {
  width: 100%;
  max-width: 750rpx;
  background: #FFFFFF;
  border-radius: 28rpx 28rpx 0 0;
  padding: 32rpx 28rpx 48rpx;
  box-sizing: border-box;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A2332;
}

.modal-close {
  font-size: 44rpx;
  color: #94A3B8;
  line-height: 1;
  padding: 0 8rpx;
}

.modal-body {
  margin-bottom: 24rpx;
}

.modal-member {
  padding: 16rpx 20rpx;
  background: #F8FBFD;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
}

.modal-member-name {
  font-size: 26rpx;
  font-weight: 700;
  color: #1A2332;
  display: block;
}

.modal-member-sub {
  font-size: 22rpx;
  color: #94A3B8;
  margin-top: 4rpx;
  display: block;
}

.form-row {
  margin-bottom: 20rpx;
}

.form-label {
  font-size: 24rpx;
  font-weight: 600;
  color: #1A2332;
  display: block;
  margin-bottom: 12rpx;
}

.form-options {
  display: flex;
  gap: 12rpx;
}

.form-option {
  flex: 1;
  padding: 16rpx 0;
  text-align: center;
  background: #F0F4F8;
  border-radius: 12rpx;
  font-size: 24rpx;
  color: #5A6B7F;
  border: 2rpx solid transparent;
  box-sizing: border-box;
}

.form-option.active {
  background: rgba(42, 111, 151, 0.08);
  color: #2A6F97;
  border-color: #2A6F97;
  font-weight: 700;
}

.form-textarea {
  width: 100%;
  min-height: 120rpx;
  padding: 16rpx;
  background: #F8FBFD;
  border-radius: 12rpx;
  font-size: 24rpx;
  color: #1A2332;
  box-sizing: border-box;
}

.modal-footer {
  display: flex;
  gap: 16rpx;
}

.modal-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 700;
  border: none;
  padding: 0;
}

.modal-btn::after {
  border: none;
}

.modal-btn.cancel {
  background: #F0F4F8;
  color: #5A6B7F;
}

.modal-btn.confirm {
  background: #2A6F97;
  color: #FFFFFF;
}

.modal-btn.confirm[disabled] {
  background: rgba(42, 111, 151, 0.4);
  color: rgba(255, 255, 255, 0.8);
}
</style>
