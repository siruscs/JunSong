<template>
  <view class="detail-page">
    <!-- 加载状态 -->
    <view v-if="loading" class="loading-wrap">
      <uni-load-more status="loading" />
    </view>

    <template v-else-if="record">
      <!-- 英雄卡片 -->
      <view class="hero-card">
        <view class="hero-bg"></view>
        <view class="hero-content">
          <view v-if="heroGroup" class="hero-eyebrow">{{ heroGroup }}</view>
          <view class="hero-title">{{ heroTitle }}</view>
          <view v-if="heroValue" class="hero-value">{{ heroValue }}</view>
          <view v-if="heroMeta" class="hero-meta">{{ heroMeta }}</view>
        </view>
      </view>

      <!-- 主要字段区域 -->
      <view v-if="primaryFields.length" class="section-card">
        <view class="section-title">概要信息</view>
        <view class="highlight-grid">
          <view
            v-for="(field, idx) in primaryFields"
            :key="idx"
            class="highlight-item"
            :class="{ 'phone-callable': isPhoneField(field) }"
            @tap="callPhone(field)"
          >
            <view class="highlight-label">{{ field.label }}</view>
            <view class="highlight-value" :class="field.class">{{ field.value }}</view>
          </view>
        </view>
      </view>

      <!-- 次要字段区域 -->
      <view v-if="secondaryFields.length" class="section-card">
        <view class="section-title">详细信息</view>
        <view
          v-for="(field, fIdx) in secondaryFields"
          :key="fIdx"
          class="field-row"
          :class="{ 'phone-callable': isPhoneField(field) }"
          @tap="callPhone(field)"
        >
          <view class="field-label">{{ field.label }}</view>
          <view class="field-value" :class="field.class">{{ field.value }}</view>
        </view>
      </view>

      <!-- 收款区域（销售模块） -->
      <view v-if="showPayment" class="section-card">
        <view class="section-title">收款信息</view>
        <view class="payment-form">
          <view class="payment-row">
            <text class="payment-label">收款金额</text>
            <input
              v-model="paymentAmount"
              type="digit"
              class="payment-input"
              placeholder="请输入金额"
            />
          </view>
          <view class="payment-row">
            <text class="payment-label">收款方式</text>
            <picker
              :range="paymentMethodLabels"
              @change="onPaymentMethodChange"
            >
              <view class="payment-picker">
                {{ selectedPaymentLabel || '请选择' }}
                <text class="picker-arrow">▸</text>
              </view>
            </picker>
          </view>
          <view class="payment-row">
            <text class="payment-label">备注</text>
            <input
              v-model="paymentRemark"
              class="payment-input"
              placeholder="选填"
            />
          </view>
          <button class="payment-btn" @tap="handlePayment">确认收款</button>
        </view>
      </view>

      <!-- 底部占位 -->
      <view class="footer-placeholder"></view>
    </template>

    <!-- 空状态 -->
    <view v-else class="empty-wrap">
      <text class="empty-text">暂无数据</text>
    </view>

    <!-- 固定底部操作栏 -->
    <view v-if="record && hasAnyAction" class="footer-bar">
      <button
        v-if="canEdit"
        class="action-btn edit-btn"
        @tap="handleEdit"
      >编辑</button>
      <button
        v-if="canDelete"
        class="action-btn delete-btn"
        @tap="handleDelete"
      >删除</button>
      <button
        v-for="(action, idx) in customActions"
        :key="idx"
        class="action-btn custom-btn"
        @tap="handleCustomAction(action)"
      >{{ action.name }}</button>
    </view>

    <view class="claim-mask" v-if="claimPanelOpen" @tap="closeClaim">
      <view class="claim-panel" @tap.stop>
        <view class="claim-panel-title">领取份额</view>
        <view class="claim-panel-sub">{{ record?.memberName || '-' }}，剩余 {{ record?.remainingShares || 0 }} 份</view>
        <input class="claim-input" v-model="claimForm.claimShares" type="number" placeholder="输入本次领取数量" />
        <view class="claim-time-row">
          <picker mode="date" :value="claimForm.claimDate" @change="onClaimDateChange">
            <view class="claim-time-picker">{{ claimForm.claimDate }}</view>
          </picker>
          <picker mode="time" :value="claimForm.claimTime" @change="onClaimTimeChange">
            <view class="claim-time-picker">{{ claimForm.claimTime }}</view>
          </picker>
        </view>
        <textarea class="claim-remark" v-model="claimForm.remark" placeholder="备注，可不填" />
        <view class="claim-panel-actions">
          <button class="claim-cancel" @tap="closeClaim">取消</button>
          <button class="claim-confirm" @tap="submitClaim">确认领取</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getModule, displayValue, formatDisplayValue, getValueTone, paymentMethods } from '@/config/modules.js'
import { getData, deleteData, request, actionRequest } from '@/api/index.js'
import { hasActionPermission, requireModulePermission } from '@/utils/permission.js'

export default {
  data() {
    return {
      moduleKey: '',
      recordId: '',
      loading: true,
      record: null,
      config: null,
      // 收款状态
      paymentAmount: '',
      paymentMethodIndex: -1,
      paymentRemark: '',
      claimPanelOpen: false,
      claimForm: {
        claimShares: '',
        claimDate: '',
        claimTime: '',
        remark: ''
      }
    }
  },
  computed: {
    // 收款方式列表（paymentMethods 是字符串数组）
    paymentMethodLabels() {
      return paymentMethods.map((item) => typeof item === 'string' ? item : item.label)
    },
    selectedPaymentLabel() {
      if (this.paymentMethodIndex < 0) return ''
      const item = paymentMethods[this.paymentMethodIndex]
      return typeof item === 'string' ? item : item?.label || ''
    },
    // 英雄卡片 - 分组名
    heroGroup() {
      if (!this.record || !this.config) return ''
      return this.config.group || ''
    },
    // 英雄卡片 - 标题：优先显示记录的主要标识字段
    heroTitle() {
      if (!this.record || !this.config) return ''
      const searchKey = this.config.searchKey
      if (searchKey && this.record[searchKey]) {
        return String(this.record[searchKey])
      }
      // 回退：尝试 summary 中第一个有值的字段
      const summaryKeys = this.config.summary || []
      for (const key of summaryKeys) {
        if (this.record[key] !== undefined && this.record[key] !== null && this.record[key] !== '') {
          const field = this.config.fields.find(f => f.key === key)
          return formatDisplayValue(field || { key, options: [] }, this.record[key])
        }
      }
      return this.config.title || ''
    },
    // 英雄卡片 - 金额值
    heroValue() {
      if (!this.record || !this.config) return ''
      const valueKeys = ['expenseAmount', 'saleAmount', 'totalAmount', 'amount', 'advanceAmount', 'investAmount', 'netProfit', 'seckillPrice', 'pointsPrice', 'availablePoints']
      for (const key of valueKeys) {
        if (this.record[key] !== undefined && this.record[key] !== null && this.record[key] !== '') {
          const field = this.config.fields.find(f => f.key === key) || { key }
          return formatDisplayValue(field, this.record[key])
        }
      }
      return ''
    },
    // 英雄卡片 - 附加信息
    heroMeta() {
      if (!this.record || !this.config) return ''
      // 查找日期字段作为附加信息
      const dateField = this.config.fields.find(f => f.type === 'date' && this.record[f.key])
      if (dateField) {
        return dateField.label + '：' + this.record[dateField.key]
      }
      // 查找状态字段
      const statusField = this.config.fields.find(f => f.key === 'status' && this.record[f.key] !== undefined)
      if (statusField) {
        return statusField.label + '：' + formatDisplayValue(statusField, this.record[statusField.key])
      }
      return ''
    },
    // 主要字段（summary 配置中的字段）
    primaryFields() {
      if (!this.record || !this.config || !this.config.summary) return []
      return this.config.summary.map(key => {
        const field = this.config.fields.find(f => f.key === key) || { key, label: key, options: [] }
        return {
            key,
            label: field.label || key,
            value: typeof field.formatter === 'function' ? field.formatter(this.record) || '-' : formatDisplayValue(field, this.record[key]),
            rawValue: this.record[key],
            type: field.type,
            class: this.getStatusClass(field)
          }
      })
    },
    // 次要字段（fields 中排除 summary 和 hidden 的字段）
    secondaryFields() {
      if (!this.record || !this.config || !this.config.fields) return []
      const summaryKeys = (this.config.summary || []).map(k => k)
      return this.config.fields
        .filter(f => !f.hidden && !summaryKeys.includes(f.key))
        .map(field => ({
          key: field.key,
          label: field.label || field.key,
          value: formatDisplayValue(field, this.record[field.key]),
          rawValue: this.record[field.key],
          type: field.type,
          class: this.getStatusClass(field)
        }))
    },
    // 是否显示收款区域
    showPayment() {
      if (!this.config || !this.config.payment) return false
      if (this.moduleKey !== 'sale') return false
      return hasActionPermission(this.moduleKey, 'payment')
    },
    // 投资人返款锁定判断
    isInvestorPaymentLocked() {
      if (this.moduleKey !== 'investorPayment') return false
      return this.record?.sourceType === '1'
    },
    canEdit() {
      if (this.isInvestorPaymentLocked) return false
      return hasActionPermission(this.moduleKey, 'edit')
    },
    canDelete() {
      if (this.isInvestorPaymentLocked) return false
      return hasActionPermission(this.moduleKey, 'remove')
    },
    // 自定义操作按钮 - 使用 action.action 匹配权限
    customActions() {
      if (!this.config || !this.config.actions) return []
      return this.config.actions.filter(action => {
        const permKey = action.action || action.key || ''
        if (this.moduleKey === 'seckillRecord' && action.url?.includes('/claim/')) {
          return hasActionPermission(this.moduleKey, permKey) && this.canClaimRecord()
        }
        return hasActionPermission(this.moduleKey, permKey)
      })
    },
    hasAnyAction() {
      return this.canEdit || this.canDelete || this.customActions.length > 0
    }
  },
  onLoad(options) {
    this.moduleKey = options.module || ''
    this.recordId = options.id || ''

    if (!this.moduleKey || !this.recordId) {
      uni.showToast({ title: '参数错误', icon: 'none' })
      this.loading = false
      return
    }

    this.config = getModule(this.moduleKey)
    if (!this.config) {
      uni.showToast({ title: '模块不存在', icon: 'none' })
      this.loading = false
      return
    }

    if (!requireModulePermission(this.moduleKey)) {
      this.loading = false
      return
    }

    this.loadDetail()
  },
  methods: {
    // 获取状态样式类
    getStatusClass(field) {
      if (!this.record) return ''
      return getValueTone(field, this.record[field.key])
    },
    isPhoneField(field) {
      const key = String(field.key || '').toLowerCase()
      const label = String(field.label || '')
      const value = String(field.rawValue || field.value || '')
      return field.type === 'phone' || key.includes('phone') || label.includes('手机') || /^1\d{10}$/.test(value.replace(/\D/g, ''))
    },
    callPhone(field) {
      if (!this.isPhoneField(field)) return
      const phoneNumber = String(field.rawValue || field.value || '').replace(/\D/g, '')
      if (!/^1\d{10}$/.test(phoneNumber)) {
        uni.showToast({ title: '手机号码格式不正确', icon: 'none' })
        return
      }
      uni.makePhoneCall({ phoneNumber })
    },
    canClaimRecord() {
      if (!this.record) return false
      const remaining = Number(this.record.remainingShares ?? this.record.shares ?? 0)
      return String(this.record.status) !== '2' && remaining > 0
    },
    todayStr() {
      const d = new Date()
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
    },
    nowTimeStr() {
      const d = new Date()
      return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0')
    },
    // 收款方式选择
    onPaymentMethodChange(e) {
      this.paymentMethodIndex = e.detail.value
    },
    // 加载数据
    async loadDetail() {
      this.loading = true
      try {
        const path = this.config.detailPath || this.config.path
        const res = await getData(path, this.recordId)
        this.record = res.data || res
      } catch (e) {
        console.error('加载详情失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    // 编辑
    handleEdit() {
      uni.navigateTo({
        url: `/pages/form/index?module=${this.moduleKey}&id=${this.recordId}`
      })
    },
    // 删除
    handleDelete() {
      uni.showModal({
        title: '确认删除',
        content: '删除后不可恢复，是否继续？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await deleteData(this.config.path, this.recordId)
            uni.showToast({ title: '删除成功', icon: 'success' })
            setTimeout(() => {
              uni.navigateBack()
            }, 1500)
          } catch (e) {
            console.error('删除失败', e)
            uni.showToast({ title: '删除失败', icon: 'none' })
          }
        }
      })
    },
    // 自定义操作 - 使用 actionRequest 统一处理
    async handleCustomAction(action) {
      if (this.moduleKey === 'seckillRecord' && action.url?.includes('/claim/')) {
        this.openClaim()
        return
      }
      try {
        uni.showLoading({ title: '处理中...' })
        // 优先使用 actionRequest 处理 URL 中的 {id} 占位符
        if (action.url && action.url.includes('{')) {
          await actionRequest(action, this.record)
        } else if (action.bodyFactory) {
          // 使用 bodyFactory 构建请求体
          const data = action.bodyFactory(this.record)
          await request({
            url: action.url,
            method: action.method || 'PUT',
            data
          })
        } else if (action.body === 'ids') {
          await actionRequest(action, this.record)
        } else {
          await request({
            url: action.url,
            method: action.method || 'POST',
            data: { id: this.recordId }
          })
        }
        uni.hideLoading()
        uni.showToast({ title: action.successMsg || '操作成功', icon: 'success' })
        if (action.reload !== false) {
          this.loadDetail()
        }
      } catch (e) {
        uni.hideLoading()
        console.error('操作失败', e)
        uni.showToast({ title: action.errorMsg || '操作失败', icon: 'none' })
      }
    },
    openClaim() {
      if (!this.canClaimRecord()) {
        uni.showToast({ title: '当前记录无需领取', icon: 'none' })
        return
      }
      this.claimForm = {
        claimShares: String(this.record.remainingShares || 1),
        claimDate: this.todayStr(),
        claimTime: this.nowTimeStr(),
        remark: ''
      }
      this.claimPanelOpen = true
    },
    closeClaim() {
      this.claimPanelOpen = false
      this.claimForm = { claimShares: '', claimDate: '', claimTime: '', remark: '' }
    },
    onClaimDateChange(e) {
      this.claimForm.claimDate = e.detail.value
    },
    onClaimTimeChange(e) {
      this.claimForm.claimTime = e.detail.value
    },
    async submitClaim() {
      const claimShares = Number(this.claimForm.claimShares)
      if (!claimShares || claimShares <= 0) {
        uni.showToast({ title: '请输入领取数量', icon: 'none' })
        return
      }
      try {
        uni.showLoading({ title: '领取中...' })
        await request({
          url: '/member/seckillRecord/claim/' + this.recordId,
          method: 'PUT',
          data: {
            claimShares,
            claimTime: `${this.claimForm.claimDate} ${this.claimForm.claimTime}:00`,
            remark: this.claimForm.remark
          }
        })
        uni.hideLoading()
        uni.showToast({ title: '领取成功', icon: 'success' })
        this.closeClaim()
        this.loadDetail()
      } catch (e) {
        uni.hideLoading()
        console.error('领取失败', e)
        uni.showToast({ title: '领取失败', icon: 'none' })
      }
    },
    // 收款
    async handlePayment() {
      const amount = parseFloat(this.paymentAmount)
      if (!amount || amount <= 0) {
        return uni.showToast({ title: '请输入有效金额', icon: 'none' })
      }
      if (this.paymentMethodIndex < 0) {
        return uni.showToast({ title: '请选择收款方式', icon: 'none' })
      }
      const item = paymentMethods[this.paymentMethodIndex]
      const method = typeof item === 'string' ? item : item?.value
      try {
        uni.showLoading({ title: '提交中...' })
        await request({
          url: '/finance/sale/payment/' + this.recordId,
          method: 'POST',
          data: {
            amount,
            paymentMethod: method,
            remark: this.paymentRemark
          }
        })
        uni.hideLoading()
        uni.showToast({ title: '收款成功', icon: 'success' })
        this.paymentAmount = ''
        this.paymentMethodIndex = -1
        this.paymentRemark = ''
        this.loadDetail()
      } catch (e) {
        uni.hideLoading()
        console.error('收款失败', e)
        uni.showToast({ title: '收款失败', icon: 'none' })
      }
    }
  }
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background-color: #F0F4F8;
  padding-bottom: env(safe-area-inset-bottom);
}

/* 加载 & 空状态 */
.loading-wrap,
.empty-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

.empty-text {
  font-size: 28rpx;
  color: #94A3B8;
}

/* 英雄卡片 */
.hero-card {
  position: relative;
  margin: 24rpx 28rpx;
  border-radius: 20rpx;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #2A6F97, #3A8DB8, #8EC8D2);
  border-radius: 20rpx;
}

.hero-content {
  position: relative;
  z-index: 1;
  padding: 40rpx 36rpx;
}

.hero-eyebrow {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 12rpx;
  letter-spacing: 2rpx;
  text-transform: uppercase;
}

.hero-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #FFFFFF;
  margin-bottom: 16rpx;
  line-height: 1.4;
}

.hero-value {
  font-size: 52rpx;
  font-weight: 700;
  color: #FFFFFF;
  margin-bottom: 12rpx;
}

.hero-meta {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
}

/* 通用卡片 */
.section-card {
  margin: 0 28rpx 20rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 28rpx 32rpx;
  box-shadow: 0 2rpx 16rpx rgba(42, 111, 151, 0.06);
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A2332;
  margin-bottom: 20rpx;
  padding-left: 16rpx;
  border-left: 4rpx solid #2A6F97;
}

/* 高亮网格 */
.highlight-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.highlight-item {
  flex: 1;
  min-width: 45%;
  background: #F5F8FA;
  border-radius: 12rpx;
  padding: 18rpx 20rpx;
}

.highlight-item.phone-callable {
  background: #EEF8FB;
}

.highlight-label {
  font-size: 22rpx;
  color: #94A3B8;
  margin-bottom: 6rpx;
}

.highlight-value {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A2332;
}

.phone-callable .highlight-value,
.phone-callable .field-value {
  color: #2A6F97;
  font-weight: 800;
}

/* 状态样式 */
.highlight-value[class*="status-"],
.field-value[class*="status-"] {
  display: inline-block;
  font-size: 24rpx;
  font-weight: 500;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}

.highlight-value.status-ok,
.field-value.status-ok {
  background: #D1FAE5;
  color: #065F46;
}

.highlight-value.status-warn,
.field-value.status-warn {
  background: #FEF3C7;
  color: #92400E;
}

.highlight-value.status-danger,
.field-value.status-danger {
  background: #FEE2E2;
  color: #991B1B;
}

.highlight-value.status-info,
.field-value.status-info {
  background: #E0F2FE;
  color: #075985;
}

.highlight-value.tone-money,
.field-value.tone-money {
  color: #B45309;
}

.highlight-value.tone-points,
.field-value.tone-points {
  color: #2A6F97;
}

.highlight-value.tone-percent,
.field-value.tone-percent {
  color: #7C3AED;
}

.highlight-value.tone-danger,
.field-value.tone-danger {
  color: #B91C1C;
}

/* 字段列表 */
.field-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #F0F4F8;
}

.field-row:last-child {
  border-bottom: none;
}

.field-row.phone-callable {
  margin: 6rpx 0;
  padding: 18rpx 20rpx;
  background: #EEF8FB;
  border-bottom: none;
  border-radius: 16rpx;
}

.field-label {
  font-size: 26rpx;
  color: #5A6B7F;
  flex-shrink: 0;
  margin-right: 24rpx;
}

.field-value {
  font-size: 26rpx;
  color: #1A2332;
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

/* 收款区域 */
.payment-form {
  padding: 0;
}

.payment-row {
  display: flex;
  align-items: center;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #F0F4F8;
}

.payment-label {
  font-size: 26rpx;
  color: #1A2332;
  width: 160rpx;
  flex-shrink: 0;
}

.payment-input {
  flex: 1;
  font-size: 26rpx;
  color: #1A2332;
  text-align: right;
  padding: 8rpx 0;
}

.payment-picker {
  flex: 1;
  font-size: 26rpx;
  color: #1A2332;
  text-align: right;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.picker-arrow {
  margin-left: 8rpx;
  color: #CBD5E1;
  font-size: 22rpx;
}

.payment-btn {
  margin-top: 28rpx;
  background: linear-gradient(135deg, #2A6F97, #3A8DB8);
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 500;
  border-radius: 16rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border: none;
}

.payment-btn::after {
  border: none;
}

/* 底部占位 */
.footer-placeholder {
  height: 140rpx;
}

/* 固定底部操作栏 */
.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20rpx;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #FFFFFF;
  box-shadow: 0 -2rpx 16rpx rgba(42, 111, 151, 0.06);
  z-index: 100;
}

.action-btn {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  font-size: 28rpx;
  font-weight: 500;
  border-radius: 16rpx;
  text-align: center;
  border: none;
  margin: 0;
  padding: 0;
}

.action-btn::after {
  border: none;
}

.edit-btn {
  background: #F0F4F8;
  color: #2A6F97;
}

.delete-btn {
  background: #FEF2F2;
  color: #EF4444;
}

.custom-btn {
  background: #EFF6FF;
  color: #2A6F97;
}

.claim-mask {
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

.claim-panel {
  width: 100%;
  padding: 30rpx 28rpx calc(30rpx + env(safe-area-inset-bottom));
  border-radius: 28rpx 28rpx 0 0;
  background: #FFFFFF;
  box-sizing: border-box;
}

.claim-panel-title {
  font-size: 34rpx;
  font-weight: 800;
  color: #1A2332;
}

.claim-panel-sub {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #708196;
}

.claim-input,
.claim-remark {
  width: 100%;
  margin-top: 18rpx;
  padding: 0 20rpx;
  background: #F8FAFC;
  border: 1rpx solid #E2E8F0;
  border-radius: 18rpx;
  box-sizing: border-box;
  font-size: 26rpx;
}

.claim-input {
  height: 78rpx;
}

.claim-time-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
  margin-top: 18rpx;
}

.claim-time-picker {
  min-width: 0;
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 18rpx;
  border-radius: 18rpx;
  background: #FFF7ED;
  color: #9A3412;
  font-size: 25rpx;
  font-weight: 700;
  text-align: center;
  box-sizing: border-box;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.claim-remark {
  height: 136rpx;
  padding-top: 18rpx;
}

.claim-panel-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 22rpx;
}

.claim-cancel,
.claim-confirm {
  flex: 1;
  height: 78rpx;
  line-height: 78rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
}

.claim-cancel {
  background: #F1F5F9;
  color: #475569;
}

.claim-confirm {
  background: #EA580C;
  color: #FFFFFF;
}
</style>
