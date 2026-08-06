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
            :class="{ 'phone-callable': isPhoneField(field), 'sensitive-toggle': field.sensitive }"
            @tap="onFieldTap(field)"
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
          :class="{ 'phone-callable': isPhoneField(field), 'sensitive-toggle': field.sensitive, 'image-field-row': field.type === 'image', 'textarea-field-row': field.type === 'textarea' }"
          @tap="onFieldTap(field)"
        >
          <view class="field-label">{{ field.label }}</view>
          <view v-if="field.type === 'image' && field.rawValue" class="field-image-wrap">
            <image class="field-image" :src="resolveImageUrl(field.rawValue)" mode="aspectFit" @tap="previewImage(field.rawValue)" />
          </view>
          <view v-else-if="field.type === 'textarea'" class="field-value field-value-textarea">{{ field.value }}</view>
          <view v-else class="field-value" :class="field.class">{{ field.value }}</view>
        </view>
      </view>

      <!-- 进货单商品明细 -->
      <view v-if="moduleKey === 'purchase' && purchaseDetails.length" class="section-card">
        <view class="section-title">商品明细</view>
        <view class="detail-list">
          <view class="detail-item" v-for="(detail, index) in purchaseDetails" :key="index">
            <view class="detail-item-header">
              <text class="detail-item-title">商品{{ index + 1 }}</text>
            </view>
            <view class="detail-row">
              <text class="detail-label">商品</text>
              <text class="detail-value-text">{{ detail.productName || '-' }}</text>
            </view>
            <view class="detail-row">
              <text class="detail-label">单位</text>
              <text class="detail-value-text">{{ detail.unit || '-' }}</text>
            </view>
            <view class="detail-row">
              <text class="detail-label">数量</text>
              <text class="detail-value-text">{{ detail.quantity || 0 }}</text>
            </view>
            <view class="detail-row">
              <text class="detail-label">单价</text>
              <text class="detail-value-text">{{ detail.price || 0 }}</text>
            </view>
            <view class="detail-row">
              <text class="detail-label">金额</text>
              <text class="detail-value-text amount">{{ detail.isGift === '1' ? '赠品' : '¥' + (detail.amount || 0) }}</text>
            </view>
          </view>
        </view>
        <view class="detail-summary">
          <view class="detail-summary-row">
            <text class="detail-summary-label">总数量</text>
            <text class="detail-summary-value">{{ record.totalQuantity || 0 }}</text>
          </view>
          <view class="detail-summary-row">
            <text class="detail-summary-label">总金额</text>
            <text class="detail-summary-value">¥{{ record.totalAmount || 0 }}</text>
          </view>
        </view>
      </view>

      <!-- 销售缴款记录 -->
      <view v-if="moduleKey === 'sale' && record.payments && record.payments.length" class="section-card payment-history-section">
        <view class="section-title">缴款记录</view>
        <view v-for="payment in record.payments" :key="payment.paymentId" class="payment-history-item">
          <view class="payment-history-main">
            <text class="payment-history-no">{{ payment.paymentNo || '-' }}</text>
            <text class="payment-history-amount">¥{{ moneyText(payment.paymentAmount) }}</text>
          </view>
          <view class="payment-history-meta">
            <text>{{ paymentDateText(payment.createTime) }}</text>
            <text>{{ paymentMethodText(payment.paymentMethod) }}</text>
          </view>
          <view v-if="payment.remark" class="payment-history-remark">备注：{{ payment.remark }}</view>
          <button
            v-if="canEditPayment"
            class="payment-edit-button"
            @tap.stop="openPaymentEdit(payment)"
          >修改</button>
        </view>
      </view>

      <!-- 底部占位 -->
      <view class="footer-placeholder"></view>
    </template>

    <!-- 空状态 -->
    <view v-else class="empty-wrap">
      <text class="empty-text">暂无数据</text>
    </view>

    <view v-if="canUnverifyExpense && expenseCapability && !expenseCapability.canUnverify" class="unverify-disabled-reason">
      {{ expenseCapability.operationDisabledReason }}
    </view>

    <!-- 固定底部操作栏 -->
    <view v-if="record && hasAnyAction" class="footer-bar">
      <button
        v-if="canVerifyExpense"
        class="action-btn custom-btn"
        @tap="openExpenseVerify"
      >核销</button>
      <button
        v-if="canUnverifyExpense"
        class="action-btn delete-btn"
        :disabled="!expenseCapability.canUnverify"
        @tap="openExpenseUnverify"
      >反核销</button>
      <button
        v-if="canPayment"
        class="action-btn payment-action-btn"
        @tap="openPayment"
      >缴款</button>
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

    <view class="claim-mask" v-if="paymentPanelOpen" @tap="closePayment">
      <view class="payment-panel" @tap.stop>
        <view class="claim-panel-title">{{ editingPaymentId ? '修改缴款' : '缴款' }}</view>
        <view class="payment-summary">
          <view class="payment-summary-row"><text>销售单号</text><text>{{ record?.saleNo || '-' }}</text></view>
          <view class="payment-summary-row"><text>销售金额</text><text>¥{{ moneyText(record?.saleAmount) }}</text></view>
          <view class="payment-summary-row"><text>累计已缴</text><text>¥{{ moneyText(record?.paidAmount) }}</text></view>
          <view class="payment-summary-row remaining"><text>{{ isReturnSale ? '剩余应退' : '剩余应收' }}</text><text>¥{{ moneyText(remainingAmount) }}</text></view>
        </view>
        <view class="payment-row payment-row-stack">
          <text class="payment-label">缴款日期</text>
          <picker mode="date" :value="paymentForm.paymentDate" @change="onPaymentDateChange">
            <view class="payment-picker">{{ paymentForm.paymentDate || '请选择' }}<text class="picker-arrow">▸</text></view>
          </picker>
        </view>
        <view class="payment-row payment-row-stack">
          <text class="payment-label">缴款金额</text>
          <input v-model="paymentForm.paymentAmount" type="digit" class="payment-input" placeholder="0.00，支持正负数" />
        </view>
        <view class="payment-row payment-row-stack">
          <text class="payment-label">付款方式</text>
          <picker :range="paymentMethodLabels" :value="paymentForm.paymentMethodIndex" @change="onPaymentMethodChange">
            <view class="payment-picker">{{ selectedPaymentLabel || '请选择' }}<text class="picker-arrow">▸</text></view>
          </picker>
        </view>
        <view class="payment-row payment-row-stack">
          <text class="payment-label">备注</text>
          <input v-model="paymentForm.remark" class="payment-input" placeholder="选填" />
        </view>
        <view class="claim-panel-actions">
          <button class="claim-cancel" @tap="closePayment">取消</button>
          <button class="claim-confirm" :disabled="paymentSubmitting" @tap="submitPayment">{{ paymentSubmitting ? '提交中' : (editingPaymentId ? '保存修改' : '确认缴款') }}</button>
        </view>
      </view>
    </view>

    <view class="claim-mask" v-if="claimPanelOpen" @tap="closeClaim">
      <view class="claim-panel" @tap.stop>
        <view class="claim-panel-title">领取份额</view>
        <view class="claim-panel-sub">{{ record?.memberName || '-' }}，剩余 {{ record?.remainingShares || 0 }} 份</view>
        <input class="claim-input" v-model="claimForm.claimShares" type="digit" placeholder="0.000" />
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
          <button class="claim-confirm" :disabled="claimSubmitting" @tap="submitClaim">{{ claimSubmitting ? '领取中' : '确认领取' }}</button>
        </view>
      </view>
    </view>

    <view class="claim-mask" v-if="breakEvenModalOpen" @tap="closeBreakEven">
      <view class="break-even-panel" @tap.stop>
        <view class="break-even-header">
          <text class="break-even-title">试算回本结果</text>
        </view>
        <view v-if="breakEvenResult" class="break-even-content">
          <view class="break-even-status" :class="breakEvenResult.isBreakEven ? 'break-even-success' : 'break-even-warning'">
            <text class="break-even-icon">{{ breakEvenResult.isBreakEven ? '✓' : '!' }}</text>
            <text class="break-even-status-text">{{ breakEvenResult.isBreakEven ? '已达到回本条件' : '暂未达到回本条件' }}</text>
          </view>
          <view class="break-even-detail">
            <view class="break-even-row">
              <text class="break-even-label">销售缴款总额</text>
              <text class="break-even-value positive">¥{{ fmtMoney(breakEvenResult.totalSalePayment) }}</text>
            </view>
            <view class="break-even-row">
              <text class="break-even-label">成本合计（费用+进货+借支未核销）</text>
              <text class="break-even-value negative">¥{{ fmtMoney(breakEvenResult.costTotal) }}</text>
            </view>
            <view v-if="!breakEvenResult.isBreakEven" class="break-even-row">
              <text class="break-even-label">距回本还差</text>
              <text class="break-even-value negative">¥{{ fmtMoney(breakEvenResult.gap) }}</text>
            </view>
            <view class="break-even-row">
              <text class="break-even-label">净利润</text>
              <text class="break-even-value" :class="Number(breakEvenResult.netProfit) >= 0 ? 'positive' : 'negative'">¥{{ fmtMoney(breakEvenResult.netProfit) }}</text>
            </view>
          </view>
          <view class="break-even-actions">
            <button class="break-even-btn" @tap="closeBreakEven">确 定</button>
          </view>
        </view>
      </view>
    </view>

    <view class="claim-mask" v-if="startTimeAdjustModalOpen" @tap="closeStartTimeAdjust">
      <view class="claim-panel" @tap.stop>
        <view class="claim-panel-title">起始时间调整</view>
        <view class="payment-row payment-row-stack">
          <text class="payment-label">新的起始时间</text>
          <picker mode="date" :value="startTimeAdjustForm.startDate" @change="onStartTimeDateChange">
            <view class="payment-picker">{{ startTimeAdjustForm.startDate || '请选择日期' }}<text class="picker-arrow">▸</text></view>
          </picker>
        </view>
        <textarea class="claim-remark" v-model="startTimeAdjustForm.reason" placeholder="请输入调整原因" />
        <view class="claim-panel-actions">
          <button class="claim-cancel" @tap="closeStartTimeAdjust">取消</button>
          <button class="claim-confirm" :disabled="startTimeAdjustSubmitting" @tap="submitStartTimeAdjust">{{ startTimeAdjustSubmitting ? '提交中' : '确认调整' }}</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import miniProgramShare from '@/mixins/miniProgramShare.js'
import { getModule, displayValue, formatDisplayValue, getValueTone } from '@/config/modules.js'
import { getData, deleteData, request, actionRequest, getBaseUrl } from '@/api/index.js'
import { hasActionPermission, requireModulePermission } from '@/utils/permission.js'
import { isUnknownWriteOutcome } from '@/utils/operationState.js'

export default {
  mixins: [miniProgramShare],
  data() {
    return {
      moduleKey: '',
      recordId: '',
      loading: true,
      record: null,
      config: null,
      sensitiveVisible: {},
      paymentPanelOpen: false,
      paymentSubmitting: false,
      editingPaymentId: '',
      paymentForm: {
        paymentDate: '',
        paymentAmount: '',
        paymentMethodIndex: -1,
        remark: ''
      },
      claimPanelOpen: false,
      claimSubmitting: false,
      claimForm: {
        claimShares: '',
        claimDate: '',
        claimTime: '',
        remark: ''
      },
      breakEvenModalOpen: false,
      breakEvenResult: null,
      startTimeAdjustModalOpen: false,
      startTimeAdjustSubmitting: false,
      startTimeAdjustForm: { startDate: '', reason: '' },
      dictPaymentMethods: [],
      supplierOptions: [],
      reverseRequestId: '',
      pendingUnverify: null,
      expenseCapability: { canUnverify: false, batchId: null, operationDisabledReason: '正在检查反核销条件…' },
      unverifySubmitting: false
    }
  },
  computed: {
    // 付款方式列表（从SYS_DICT动态加载）
    paymentMethodLabels() {
      return this.dictPaymentMethods.map((item) => item.dictLabel)
    },
    selectedPaymentLabel() {
      if (this.paymentForm.paymentMethodIndex < 0) return ''
      return this.dictPaymentMethods[this.paymentForm.paymentMethodIndex]?.dictLabel || ''
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
          return formatDisplayValue(field || { key, options: [] }, this.record[key], this.record)
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
          return formatDisplayValue(field, this.record[key], this.record)
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
        return statusField.label + '：' + formatDisplayValue(statusField, this.record[statusField.key], this.record)
      }
      return ''
    },
    // 主要字段（summary 配置中的字段）
    primaryFields() {
      if (!this.record || !this.config || !this.config.summary) return []
      const fields = this.config.summary.filter(key => !(this.moduleKey === 'sale' && key === 'status')).map(key => {
        const field = this.config.fields.find(f => f.key === key) || { key, label: key, options: [] }
        const rawValue = this.moduleKey === 'purchase' && key === 'supplierName'
          ? this.purchaseSupplierName
          : this.record[key]
        const isSensitive = field.sensitive
        const isVisible = isSensitive && this.sensitiveVisible[key]
        let displayVal
        if (isSensitive && !isVisible) {
          displayVal = this.maskValue(field.type, rawValue)
        } else if (typeof field.formatter === 'function') {
          displayVal = field.formatter(this.record) || ''
        } else if (rawValue === undefined || rawValue === null || rawValue === '') {
          displayVal = ''
        } else {
          displayVal = formatDisplayValue(field, rawValue, this.record)
        }
        return {
            key,
            label: field.label || key,
            value: displayVal,
            rawValue,
            type: field.type,
            sensitive: isSensitive,
            class: this.getStatusClass(field)
          }
      })
      if (this.moduleKey === 'sale') {
        fields.push({
          key: 'status',
          label: '缴款状态',
          value: this.salePaymentStatusText,
          rawValue: this.record.status,
          type: 'select',
          sensitive: false,
          class: this.salePaymentStatusClass
        }, {
          key: 'giftUnitPrice',
          label: '加赠单价',
          value: this.giftUnitPriceText,
          rawValue: null,
          type: 'number',
          sensitive: false,
          class: ''
        })
      }
      return fields
    },
    // 次要字段（fields 中排除 summary 和 hidden 的字段）
    secondaryFields() {
      if (!this.record || !this.config || !this.config.fields) return []
      const summaryKeys = (this.config.summary || []).map(k => k)
      return this.config.fields
        .filter(f => !f.hidden && !summaryKeys.includes(f.key))
        .map(field => {
          const rawValue = this.moduleKey === 'purchase' && field.key === 'supplierId'
            ? this.purchaseSupplierName
            : this.record[field.key]
          const isSensitive = field.sensitive
          const isVisible = isSensitive && this.sensitiveVisible[field.key]
          let displayVal
          if (isSensitive && !isVisible) {
            displayVal = this.maskValue(field.type, rawValue)
          } else if (typeof field.formatter === 'function') {
            displayVal = field.formatter(this.record) || '-'
          } else if (rawValue === undefined || rawValue === null || rawValue === '') {
            displayVal = ''
          } else {
            displayVal = formatDisplayValue(field, rawValue, this.record)
          }
          return {
            key: field.key,
            label: field.label || field.key,
            value: displayVal,
            rawValue,
            type: field.type,
            sensitive: isSensitive,
            class: this.getStatusClass(field)
          }
        })
    },
    remainingAmount() {
      if (this.moduleKey !== 'sale' || !this.record) return 0
      const saleAmount = Number(this.record.saleAmount || 0)
      const paidAmount = Number(this.record.paidAmount || 0)
      if (saleAmount < 0) {
        return -(Math.abs(saleAmount) - Math.abs(paidAmount))
      }
      return saleAmount - paidAmount
    },
    isReturnSale() {
      return this.moduleKey === 'sale' && Number(this.record?.saleAmount || 0) < 0
    },
    canPayment() {
      if (this.moduleKey !== 'sale' || !this.record) return false
      const saleAbs = Math.abs(Number(this.record.saleAmount || 0))
      const remainingCents = Math.round(Math.abs(Number(this.remainingAmount)) * 100)
      return saleAbs > 0
        && remainingCents > 0
        && hasActionPermission(this.moduleKey, 'payment')
    },
    canEditPayment() {
      return this.moduleKey === 'sale'
        && Array.isArray(this.record?.payments)
        && hasActionPermission('sale', 'paymentEdit')
    },
    // 投资人返款锁定判断
    isInvestorPaymentLocked() {
      if (this.moduleKey !== 'investorPayment') return false
      return this.record?.sourceType === '1'
    },
    // 费用已核销锁定判断
    isExpenseVerified() {
      if (this.moduleKey !== 'expense') return false
      return String(this.record?.status) === '1'
    },
    canVerifyExpense() {
      return this.moduleKey === 'expense' && !this.isExpenseVerified && hasActionPermission('expense', 'verify')
    },
    canUnverifyExpense() {
      return this.moduleKey === 'expense' && this.isExpenseVerified && hasActionPermission('expense', 'unverify')
    },
    canEdit() {
      if (this.moduleKey === 'accountingPeriod') return false
      if (this.isInvestorPaymentLocked) return false
      if (this.isExpenseVerified) return false
      return hasActionPermission(this.moduleKey, 'edit')
    },
    canDelete() {
      if (this.isInvestorPaymentLocked) return false
      if (this.isExpenseVerified) return false
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
        if (this.isExpenseVerified && permKey === 'verify') return false
        return hasActionPermission(this.moduleKey, permKey)
      })
    },
    hasAnyAction() {
      return this.canPayment || this.canVerifyExpense || this.canUnverifyExpense || this.canEdit || this.canDelete || this.customActions.length > 0
    },
    purchaseDetails() {
      if (!this.record || !this.record.details) return []
      return this.record.details
    },
    purchaseSupplierName() {
      if (this.moduleKey !== 'purchase' || !this.record) return '-'
      if (this.record.supplierName) return String(this.record.supplierName)
      if (this.record.supplier?.supplierName) return String(this.record.supplier.supplierName)
      const hit = this.supplierOptions.find(item => String(item.supplierId) === String(this.record.supplierId))
      return hit?.supplierName || (this.record.supplierId !== undefined && this.record.supplierId !== null ? String(this.record.supplierId) : '-')
    },
    giftUnitPriceText() {
      if (!this.record) return '-'
      const totalQuantity = Number(this.record.saleQuantity || 0) + Number(this.record.giftQuantity || 0)
      // 退货单 saleQuantity 为负，totalQuantity 可能为负；只要非零就应计算单价
      return totalQuantity !== 0
        ? `¥${(Number(this.record.saleAmount || 0) / totalQuantity).toFixed(2)}`
        : '-'
    },
    salePaymentStatusText() {
      if (!this.record || this.moduleKey !== 'sale') return '-'
      const field = this.config?.fields?.find(item => item.key === 'status') || { key: 'status', type: 'select', options: [] }
      return formatDisplayValue(field, this.record.status, this.record)
    },
    salePaymentStatusClass() {
      const status = String(this.record?.status ?? '')
      if (status === '2') return 'status-ok'
      if (status === '1') return 'status-info'
      return 'status-warn'
    }
  },
  onLoad(options) {
    this.expenseCapability = { canUnverify: false, batchId: null, operationDisabledReason: '正在检查反核销条件…' }
    this.pendingUnverify = null
    this.reverseRequestId = ''
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
    this.loadDictPaymentMethods()
  },
  onShow() {
    if (this.moduleKey && this.recordId && this.record) this.loadDetail()
  },
  methods: {
    createRequestId(prefix) {
      return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`
    },
    openExpenseVerify() {
      uni.navigateTo({ url: `/pages/expense-verify/index?expenseIds=${this.recordId}` })
    },
    async openExpenseUnverify() {
      if (this.unverifySubmitting) return
      try {
        const capability = this.expenseCapability
        if (!capability.canUnverify) {
          uni.showModal({ title: '无法反核销', content: capability.operationDisabledReason || '当前费用不可反核销', showCancel: false })
          return
        }
        if (!this.pendingUnverify) {
          const modal = await new Promise((resolve) => uni.showModal({ title: '反核销', editable: true, placeholderText: '请输入反核销原因', success: resolve }))
          if (!modal.confirm) {
            this.reverseRequestId = ''
            this.pendingUnverify = null
            return
          }
          const reason = String(modal.content || '').trim()
          if (!reason) return uni.showToast({ title: '请输入反核销原因', icon: 'none' })
          this.reverseRequestId = this.createRequestId('unverify')
          this.pendingUnverify = { batchId: capability.batchId, reason, requestId: this.reverseRequestId }
        }
        const pending = this.pendingUnverify
        if (String(pending.batchId) !== String(capability.batchId)) {
          this.pendingUnverify = null
          this.reverseRequestId = ''
          return uni.showToast({ title: '核销批次已变化，请重新操作', icon: 'none' })
        }
        this.unverifySubmitting = true
        await request({
          url: `/finance/expense/unverify/${pending.batchId}`,
          method: 'PUT',
          data: { reason: pending.reason, requestId: pending.requestId },
          silent: true
        })
        this.reverseRequestId = ''
        this.pendingUnverify = null
        uni.showToast({ title: '反核销成功', icon: 'success' })
        await this.loadDetail()
      } catch (error) {
        if (isUnknownWriteOutcome(error)) {
          const modal = await new Promise((resolve) => uni.showModal({
            title: '确认反核销结果',
            content: '网络中断，无法确认反核销结果。再次确认将复用同一请求编号，不会重复处理。',
            confirmText: '确认结果',
            cancelText: '刷新详情',
            success: resolve,
            fail: () => resolve({ confirm: false })
          }))
          if (modal.confirm) {
            this.unverifySubmitting = false
            await this.openExpenseUnverify()
            return
          }
          await this.loadDetail()
          if (!this.canUnverifyExpense) {
            this.pendingUnverify = null
            this.reverseRequestId = ''
          }
          return
        }
        this.pendingUnverify = null
        this.reverseRequestId = ''
        uni.showToast({ title: error?.msg || error?.message || '反核销失败', icon: 'none' })
        await this.loadDetail()
      } finally {
        this.unverifySubmitting = false
      }
    },
    async loadExpenseCapability() {
      this.expenseCapability = { canUnverify: false, batchId: null, operationDisabledReason: '正在检查反核销条件…' }
      if (!this.canUnverifyExpense) return
      try {
        const response = await request({ url: `/finance/expense/${this.recordId}/capability`, method: 'GET', silent: true })
        const capability = response.data || response
        this.expenseCapability = {
          canUnverify: capability.canUnverify === true,
          batchId: capability.batchId || null,
          operationDisabledReason: capability.canUnverify === true ? '' : (capability.operationDisabledReason || '当前费用不可反核销')
        }
      } catch (error) {
        this.expenseCapability = { canUnverify: false, batchId: null, operationDisabledReason: '反核销条件检查失败，请稍后刷新重试' }
      }
    },
    resolveImageUrl(url) {
      if (!url) return ''
      if (url.startsWith('http://') || url.startsWith('https://')) return url
      if (url.startsWith('/statics/')) {
        const baseUrl = getBaseUrl()
        return baseUrl.replace(/\/prod-api$/, '').replace(/\/dev-api$/, '') + url
      }
      return getBaseUrl() + url
    },
    previewImage(url) {
      const fullUrl = this.resolveImageUrl(url)
      uni.previewImage({ urls: [fullUrl], current: fullUrl })
    },
    async loadDictPaymentMethods() {
      try {
        const res = await request({ url: '/system/dict/data/type/finance_payment_method', method: 'GET' })
        this.dictPaymentMethods = res.data || []
      } catch (e) {
        console.error('加载付款方式字典失败', e)
      }
    },
    async loadSupplierOptions() {
      if (this.moduleKey !== 'purchase' || !this.record?.supplierId) return
      try {
        const res = await request({
          url: '/finance/supplier/list',
          method: 'GET',
          data: { pageNum: 1, pageSize: 200, deptId: this.record.deptId || undefined },
          silent: true
        })
        this.supplierOptions = res.rows || res.data || []
      } catch (e) {
        this.supplierOptions = []
      }
    },
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
    onFieldTap(field) {
      if (field.sensitive) {
        const key = field.key
        this.$set(this.sensitiveVisible, key, !this.sensitiveVisible[key])
        return
      }
      if (this.isPhoneField(field)) {
        this.callPhone(field)
      }
    },
    maskValue(type, value) {
      if (!value && value !== 0) return ''
      const str = String(value)
      if (type === 'phone' || /^1\d{10}$/.test(str.replace(/\D/g, ''))) {
        return str
      }
      if (type === 'idcard' || str.length === 18 || str.length === 15) {
        if (str.length >= 6) {
          return str.slice(0, 6) + '***'
        }
        return '***'
      }
      if (str.length <= 2) return '*'.repeat(str.length)
      if (str.length <= 6) return str[0] + '****' + str[str.length - 1]
      return str.slice(0, 3) + '****' + str.slice(-3)
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
      this.paymentForm.paymentMethodIndex = Number(e.detail.value)
    },
    onPaymentDateChange(e) {
      this.paymentForm.paymentDate = e.detail.value
    },
    // 加载数据
    async loadDetail() {
      this.loading = true
      try {
        const path = this.config.detailPath || this.config.path
        const res = await getData(path, this.recordId)
        this.record = res.data || res
        if (this.record && this.record.period) {
          this.record = { ...this.record, ...this.record.period }
        }
        await this.loadSupplierOptions()
        await this.loadExpenseCapability()
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
      if (this.moduleKey === 'accountingPeriod' && action.action === 'checkBreakEven') {
        await this.handleBreakEvenCheck()
        return
      }
      if (this.moduleKey === 'accountingPeriod' && action.action === 'adjustStartTime') {
        this.openStartTimeAdjust()
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
    async handleBreakEvenCheck() {
      try {
        uni.showLoading({ title: '计算中...' })
        const deptId = this.record?.deptId || this.recordId
        const res = await request({ url: `/finance/accountingPeriod/current/${deptId}/trialBreakEven`, method: 'POST' })
        const period = res.data || res
        const costTotal = Number(period.totalVerifiedExpense || 0) + Number(period.totalPurchase || 0) + Number(period.totalUnverifiedAdvance || 0)
        const salePayment = Number(period.totalSalePayment || 0)
        const isBreakEven = salePayment >= costTotal
        this.breakEvenResult = {
          isBreakEven: isBreakEven,
          totalSalePayment: period.totalSalePayment,
          costTotal: costTotal,
          gap: Math.max(costTotal - salePayment, 0),
          netProfit: period.netProfit
        }
        this.breakEvenModalOpen = true
      } catch (e) {
        console.error('回本检测失败', e)
        uni.showToast({ title: '回本检测失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
    closeBreakEven() {
      this.breakEvenModalOpen = false
      this.breakEvenResult = null
    },
    openStartTimeAdjust() {
      const current = String(this.record?.startTime || '').slice(0, 10)
      this.startTimeAdjustForm = { startDate: current, reason: '' }
      this.startTimeAdjustModalOpen = true
    },
    closeStartTimeAdjust() {
      if (this.startTimeAdjustSubmitting) return
      this.startTimeAdjustModalOpen = false
    },
    onStartTimeDateChange(e) {
      this.startTimeAdjustForm.startDate = e.detail.value
    },
    async submitStartTimeAdjust() {
      const startDate = String(this.startTimeAdjustForm.startDate || '').trim()
      const reason = String(this.startTimeAdjustForm.reason || '').trim()
      if (!startDate || !reason) {
        uni.showToast({ title: '请选择起始时间并填写原因', icon: 'none' })
        return
      }
      this.startTimeAdjustSubmitting = true
      try {
        await request({
          url: `/finance/accountingPeriod/${this.recordId}/opsAdjustStartTime`,
          method: 'POST',
          data: { startTime: `${startDate} 00:00:00`, endTime: this.record?.endTime, reason }
        })
        uni.showToast({ title: '调整成功', icon: 'success' })
        this.startTimeAdjustModalOpen = false
        await this.loadDetail()
      } catch (e) {
        console.error('起始时间调整失败', e)
        uni.showToast({ title: e?.message || '调整失败', icon: 'none' })
      } finally {
        this.startTimeAdjustSubmitting = false
      }
    },
    fmtMoney(val) {
      if (!val && val !== 0) return '0'
      const n = Number(val)
      if (isNaN(n)) return '0'
      if (n >= 10000) return (n / 10000).toFixed(1) + '万'
      return n.toFixed(n % 1 === 0 ? 0 : 2)
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
      if (this.claimSubmitting) return
      const claimShares = Number(this.claimForm.claimShares)
      if (!claimShares || claimShares <= 0) {
        uni.showToast({ title: '请输入领取数量', icon: 'none' })
        return
      }
      this.claimSubmitting = true
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
      } finally {
        this.claimSubmitting = false
      }
    },
    openPayment() {
      if (!this.canPayment) {
        uni.showToast({ title: '当前销售单无需缴款或暂无权限', icon: 'none' })
        return
      }
      this.paymentForm = {
        paymentDate: this.todayStr(),
        paymentAmount: this.remainingAmount.toFixed(2),
        paymentMethodIndex: -1,
        remark: ''
      }
      this.editingPaymentId = ''
      this.paymentPanelOpen = true
    },
    openPaymentEdit(payment) {
      if (!this.canEditPayment || !payment?.paymentId) return
      const methodIndex = this.dictPaymentMethods.findIndex((item) => String(item.dictValue) === String(payment.paymentMethod))
      this.editingPaymentId = payment.paymentId
      this.paymentForm = {
        paymentDate: String(payment.paymentDate || payment.createTime || '').slice(0, 10),
        paymentAmount: Number(payment.paymentAmount || 0).toFixed(2),
        paymentMethodIndex: methodIndex,
        remark: payment.remark || ''
      }
      this.paymentPanelOpen = true
    },
    closePayment() {
      this.paymentPanelOpen = false
      this.editingPaymentId = ''
      this.paymentForm = { paymentDate: '', paymentAmount: '', paymentMethodIndex: -1, remark: '' }
    },
    moneyText(value) {
      const amount = Number(value || 0)
      return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
    },
    paymentDateText(value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').replace(/\.\d{3}Z?$/, '').replace(/Z$/, '')
    },
    paymentMethodText(value) {
      if (!value) return '-'
      return this.dictPaymentMethods.find((item) => String(item.dictValue) === String(value))?.dictLabel || value
    },
    async submitPayment() {
      if (this.paymentSubmitting) return
      const paymentAmount = Number(this.paymentForm.paymentAmount)
      if (!paymentAmount || paymentAmount === 0) {
        return uni.showToast({ title: '缴款金额不能为0', icon: 'none' })
      }
      if (!this.editingPaymentId) {
        // 新增缴款用绝对值校验：不能超过剩余额度；修改由后端按销售单重新校验。
        const paymentAbs = Math.abs(Math.round(paymentAmount * 100))
        const remainingCents = Math.abs(Math.round(this.remainingAmount * 100))
        if (paymentAbs > remainingCents) {
          return uni.showToast({ title: '缴款金额超过剩余额度', icon: 'none' })
        }
      }
      if (!this.paymentForm.paymentDate) {
        return uni.showToast({ title: '请选择缴款日期', icon: 'none' })
      }
      if (this.paymentForm.paymentMethodIndex < 0) {
        return uni.showToast({ title: '请选择付款方式', icon: 'none' })
      }
      const method = this.dictPaymentMethods[this.paymentForm.paymentMethodIndex]?.dictValue || ''
      this.paymentSubmitting = true
      try {
        uni.showLoading({ title: '提交中...' })
        await request({
          url: '/finance/sale/payment/' + (this.editingPaymentId || this.recordId),
          method: this.editingPaymentId ? 'PUT' : 'POST',
          data: {
            paymentAmount,
            paymentMethod: method,
            paymentDate: this.paymentForm.paymentDate,
            remark: this.paymentForm.remark
          }
        })
        uni.hideLoading()
        uni.showToast({ title: this.editingPaymentId ? '缴款修改成功' : '缴款成功', icon: 'success' })
        this.closePayment()
        await this.loadDetail()
      } catch (e) {
        uni.hideLoading()
        if (isUnknownWriteOutcome(e)) {
          this.closePayment()
          uni.showModal({
            title: '缴款结果待确认',
            content: '网络中断，正在刷新销售详情确认缴款结果，请勿重复提交。',
            showCancel: false
          })
          await this.loadDetail()
        } else {
          uni.showToast({ title: e?.msg || '缴款失败', icon: 'none' })
        }
      } finally {
        this.paymentSubmitting = false
      }
    }
  }
}
</script>

<style scoped>
.unverify-disabled-reason {
  margin: 20rpx 28rpx 150rpx;
  padding: 20rpx 24rpx;
  border: 2rpx solid #fed7aa;
  border-radius: 14rpx;
  background: #fff7ed;
  color: #c2410c;
  font-size: 24rpx;
  line-height: 1.5;
}
.detail-page {
  min-height: 100vh;
  background-color: #E8EEF5;
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
  background: linear-gradient(135deg, #087CF0, #5AA9E8, #A8C7E5);
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
  box-shadow: 0 2rpx 16rpx rgba(8, 124, 240, 0.06);
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A2332;
  margin-bottom: 20rpx;
  padding-left: 16rpx;
  border-left: 4rpx solid #087CF0;
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
  color: #087CF0;
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
  color: #087CF0;
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
  border-bottom: 1rpx solid #E8EEF5;
}

.image-field-row {
  flex-direction: column;
  align-items: flex-start;
  gap: 12rpx;
}

/* textarea 字段：上下排列，保留换行格式 */
.textarea-field-row {
  flex-direction: column;
  align-items: flex-start;
  gap: 8rpx;
}

.field-value-textarea {
  width: 100%;
  text-align: left;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  font-weight: 400;
  color: #2C3E50;
}

.field-image-wrap {
  width: 100%;
}

.field-image {
  width: 100%;
  height: 320rpx;
  border-radius: 12rpx;
  background: #F5F8FA;
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

/* 缴款面板 */
.payment-panel {
  width: 92%;
  max-width: 680rpx;
  max-height: 88vh;
  overflow-y: auto;
  padding: 40rpx 36rpx;
  border-radius: 24rpx;
  background: #FFFFFF;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.payment-summary {
  margin: 24rpx 0 12rpx;
  padding: 20rpx 24rpx;
  border-radius: 16rpx;
  background: #F6F9FC;
}

.payment-summary-row {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
  color: #5A6B7F;
  font-size: 25rpx;
}

.payment-summary-row.remaining {
  color: #C26A1B;
  font-weight: 600;
}

.payment-row {
  display: flex;
  align-items: center;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #E8EEF5;
}

/* 上下排列变体：标签在上，控件在下，加宽点击区域 */
.payment-row-stack {
  flex-direction: column;
  align-items: stretch;
  padding: 20rpx 0;
}

.payment-row-stack .payment-label {
  width: auto;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: #5A6B7F;
}

.payment-row-stack .payment-input {
  text-align: left;
  padding: 16rpx 20rpx;
  border: 1rpx solid #E2E8F0;
  border-radius: 12rpx;
  background: #F8FAFC;
  font-size: 30rpx;
  min-height: 72rpx;
}

.payment-row-stack .payment-picker {
  text-align: left;
  justify-content: space-between;
  padding: 16rpx 20rpx;
  border: 1rpx solid #E2E8F0;
  border-radius: 12rpx;
  background: #F8FAFC;
  font-size: 30rpx;
  min-height: 72rpx;
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

.payment-action-btn {
  background: #087CF0;
  color: #FFFFFF;
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
  box-shadow: 0 -2rpx 16rpx rgba(8, 124, 240, 0.06);
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
  background: #E8EEF5;
  color: #087CF0;
}

.delete-btn {
  background: #FEF2F2;
  color: #EF4444;
}

.custom-btn {
  background: #EFF6FF;
  color: #087CF0;
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

/* ===== 回本检测弹窗 ===== */
.break-even-panel {
  width: 600rpx;
  margin: auto;
  border-radius: 24rpx;
  background: #FFFFFF;
  overflow: hidden;
}

.break-even-header {
  padding: 28rpx 32rpx;
  border-bottom: 2rpx solid #F1F5F9;
}

.break-even-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A2332;
}

.break-even-content {
  padding: 32rpx;
}

.break-even-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  margin-bottom: 28rpx;
}

.break-even-success {
  background: #DCFCE7;
}

.break-even-warning {
  background: #FEF3C7;
}

.break-even-icon {
  font-size: 40rpx;
  font-weight: 800;
}

.break-even-success .break-even-icon {
  color: #16A34A;
}

.break-even-warning .break-even-icon {
  color: #D97706;
}

.break-even-status-text {
  font-size: 30rpx;
  font-weight: 700;
}

.break-even-success .break-even-status-text {
  color: #16A34A;
}

.break-even-warning .break-even-status-text {
  color: #D97706;
}

.break-even-detail {
  margin-bottom: 28rpx;
}

.break-even-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 2rpx solid #F8FAFC;
}

.break-even-row:last-child {
  border-bottom: none;
}

.break-even-label {
  font-size: 26rpx;
  color: #64748B;
}

.break-even-value {
  font-size: 28rpx;
  font-weight: 700;
}

.break-even-value.positive {
  color: #10B981;
}

.break-even-value.negative {
  color: #EF4444;
}

.break-even-actions {
  text-align: center;
}

.break-even-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 999rpx;
  background: #087CF0;
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 600;
}

/* 进货单商品明细样式 */
.detail-list {
  margin-top: 16rpx;
}

.detail-item {
  background: #F5F8FA;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.detail-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #E2E8F0;
  margin-bottom: 16rpx;
}

.detail-item-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
}

.detail-row {
  display: flex;
  align-items: center;
  min-height: 72rpx;
  padding: 16rpx 0;
  box-sizing: border-box;
  line-height: 34rpx;
}

.sale-status-price-row {
  gap: 24rpx;
}

.sale-status-cell {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.sale-status-cell .detail-label {
  width: auto;
  margin-right: 16rpx;
}

.detail-label {
  width: 140rpx;
  font-size: 26rpx;
  color: #64748B;
  flex-shrink: 0;
}

.detail-value-text {
  font-size: 26rpx;
  color: #1A2332;
  flex: 1;
}

.detail-value-text.amount {
  font-weight: 700;
  color: #087CF0;
}

.detail-summary {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 2rpx solid #E2E8F0;
}

.detail-summary-row {
  display: flex;
  align-items: center;
  padding: 12rpx 0;
}

.detail-summary-label {
  flex: 1;
  font-size: 28rpx;
  color: #1A2332;
  font-weight: 500;
}

.detail-summary-value {
  font-size: 32rpx;
  color: #087CF0;
  font-weight: 700;
}

.payment-history-item {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #E2E8F0;
}

.payment-history-item:last-child {
  border-bottom: 0;
}

.payment-history-main,
.payment-history-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.payment-history-no,
.payment-history-meta,
.payment-history-remark {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.payment-history-no {
  color: #1A2332;
  font-size: 27rpx;
  font-weight: 600;
}

.payment-history-amount {
  color: #087CF0;
  font-size: 30rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.payment-history-meta {
  margin-top: 10rpx;
  color: #64748B;
  font-size: 24rpx;
}

.payment-history-remark {
  margin-top: 10rpx;
  color: #475569;
  font-size: 25rpx;
  white-space: pre-wrap;
}
</style>
