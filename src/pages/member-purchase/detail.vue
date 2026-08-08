<template>
  <view class="detail-page">
    <view v-if="loading" class="loading-wrap"><uni-load-more status="loading" /></view>

    <template v-if="detail && detail.purchaseId">
      <view class="detail-hero">
        <view class="detail-hero-bg"></view>
        <view class="detail-hero-content">
          <view class="detail-hero-eyebrow">会员购买 · {{ customerTypeText(detail.customerType) }}</view>
          <view class="detail-hero-title">{{ detail.purchaseNo || `购买单 #${detail.purchaseId}` }}</view>
          <view class="detail-hero-value">¥{{ money(detail.totalAmount) }}</view>
          <view class="detail-hero-meta">购买日期 {{ dateOnly(detail.purchaseDate) }}</view>
        </view>
      </view>

      <view class="detail-section">
        <view class="detail-section-title">概要信息</view>
        <view class="detail-highlight-grid">
          <view class="detail-highlight-item"><view class="detail-highlight-label">顾客姓名</view><view class="detail-highlight-value">{{ detail.customerName || '未登记顾客' }}</view></view>
          <view class="detail-highlight-item"><view class="detail-highlight-label">手机号</view><view class="detail-highlight-value">{{ detail.customerPhone || '-' }}</view></view>
          <view class="detail-highlight-item"><view class="detail-highlight-label">收款状态</view><view class="detail-highlight-value" :class="paymentStatusClass(detail.paymentStatus)">{{ paymentStatusText(detail.paymentStatus) }}</view></view>
          <view class="detail-highlight-item"><view class="detail-highlight-label">领取状态</view><view class="detail-highlight-value" :class="deliveryStatusClass(detail.deliveryStatus)">{{ deliveryStatusText(detail.deliveryStatus) }}</view></view>
          <view class="detail-highlight-item"><view class="detail-highlight-label">应收金额</view><view class="detail-highlight-value tone-money">¥{{ money(detail.totalAmount) }}</view></view>
          <view class="detail-highlight-item"><view class="detail-highlight-label">已收金额</view><view class="detail-highlight-value tone-success">¥{{ money(detail.paidAmount) }}</view></view>
          <view class="detail-highlight-item"><view class="detail-highlight-label">待缴金额</view><view class="detail-highlight-value tone-warning">¥{{ money(detail.receivableAmount) }}</view></view>
          <view class="detail-highlight-item"><view class="detail-highlight-label">核算周期</view><view class="detail-highlight-value">{{ detail.periodNo || detail.periodId || '-' }}</view></view>
        </view>
      </view>

      <view class="detail-section" v-if="(detail.items || []).length">
        <view class="detail-section-title">购买明细</view>
        <view class="detail-item" v-for="(item, idx) in detail.items || []" :key="idx">
          <view class="detail-item-header"><text class="detail-item-title">{{ item.productNameSnapshot || `商品${idx + 1}` }}</text></view>
          <view class="detail-row"><text class="detail-label">购买数量</text><text class="detail-value-text">{{ quantity(item.purchaseQuantity) }}</text></view>
          <view class="detail-row"><text class="detail-label">赠送数量</text><text class="detail-value-text">{{ quantity(item.giftQuantity) }}</text></view>
          <view class="detail-row"><text class="detail-label">单价</text><text class="detail-value-text">¥{{ money(item.unitPrice) }}</text></view>
          <view class="detail-row"><text class="detail-label">已领取</text><text class="detail-value-text">{{ quantity(item.deliveredSaleQuantity) }} / 赠 {{ quantity(item.deliveredGiftQuantity) }}</text></view>
          <view class="detail-row"><text class="detail-label">金额</text><text class="detail-value-text amount">¥{{ money(Number(item.purchaseQuantity || 0) * Number(item.unitPrice || 0)) }}</text></view>
        </view>
        <view class="detail-summary">
          <view class="detail-summary-row"><text class="detail-summary-label">总数量</text><text class="detail-summary-value">{{ quantity(totalPurchaseQty) }}（赠 {{ quantity(totalGiftQty) }}）</text></view>
          <view class="detail-summary-row"><text class="detail-summary-label">总金额</text><text class="detail-summary-value">¥{{ money(detail.totalAmount) }}</text></view>
        </view>
      </view>

      <view class="detail-section" v-if="detail.remark">
        <view class="detail-section-title">备注</view>
        <view class="detail-remark">{{ detail.remark }}</view>
      </view>

      <view class="detail-section payment-history-section">
        <view class="detail-section-title">收款记录</view>
        <view class="payment-history-item" v-for="p in detail.payments || []" :key="p.paymentId">
          <view class="payment-history-main"><text class="payment-history-no">{{ p.paymentNo || `缴款 #${p.paymentId}` }}</text><text class="payment-history-amount">¥{{ money(p.paymentAmount) }}</text></view>
          <view class="payment-history-meta"><text>{{ dateText(p.paymentDate) }}</text><text>{{ paymentMethodText(p.paymentMethod) }}</text></view>
          <view class="payment-history-meta" v-if="p.operatorName"><text>操作人 {{ p.operatorName }}</text></view>
        </view>
        <view class="detail-empty" v-if="!(detail.payments || []).length">暂无收款记录</view>
      </view>

      <view class="detail-section payment-history-section">
        <view class="detail-section-title">领取记录</view>
        <view class="payment-history-item" v-for="d in detail.deliveries || []" :key="d.deliveryId">
          <view class="payment-history-main"><text class="payment-history-no">{{ d.deliveryNo || `领取 #${d.deliveryId}` }}</text><text class="payment-history-amount">{{ quantity(d.totalDeliveryQuantity) }}</text></view>
          <view class="payment-history-meta"><text>{{ dateText(d.deliveryDate) }}</text><text>领取人 {{ d.receiverName || '-' }}</text></view>
          <view class="payment-history-meta" v-if="d.operatorName"><text>操作人 {{ d.operatorName }}</text></view>
        </view>
        <view class="detail-empty" v-if="!(detail.deliveries || []).length">暂无领取记录</view>
      </view>

      <view class="detail-section payment-history-section">
        <view class="detail-section-title">退费记录</view>
        <view class="payment-history-item" v-for="r in returns" :key="r.returnId" @tap="openReturnDetail(r)">
          <view class="payment-history-main">
            <text class="payment-history-no">{{ r.returnNo || `退货 #${r.returnId}` }}</text>
            <text class="payment-history-amount refund-amount">-¥{{ money(r.refundedAmount || r.refundAmount) }}</text>
          </view>
          <view class="payment-history-meta">
            <text>{{ dateText(r.returnDate) }}</text>
            <text>退 {{ quantity(r.totalReturnQuantity) }} · 原购 {{ quantity(r.purchaseQuantity) }}</text>
          </view>
          <view class="payment-history-meta" v-if="r.reason"><text>原因：{{ r.reason }}</text></view>
          <view class="return-status-line"><text class="compact-status" :class="returnStatusClass(r.status)">{{ returnStatusText(r.status) }}</text></view>
        </view>
        <view class="detail-empty" v-if="!returns.length">暂无退费记录</view>
      </view>

      <view class="detail-footer-placeholder"></view>

      <view class="detail-footer-bar" v-if="!panel">
        <button v-if="can('edit') && String(detail.orderStatus) === '1' && String(detail.deliveryStatus) !== '2'" class="detail-action-btn primary-btn" @tap="switchToEdit">编辑</button>
        <button v-if="can('payment') && Number(detail.receivableAmount || 0) > 0" class="detail-action-btn payment-btn" @tap="openPayment">收款</button>
        <button v-if="can('delivery') && String(detail.deliveryStatus) !== '2'" class="detail-action-btn delivery-btn" @tap="openDelivery">领取</button>
        <button v-if="can('bind') && detail.customerType === 'WALK_IN' && !detail.memberId" class="detail-action-btn bind-btn" @tap="openBind">绑定</button>
        <button v-if="can('cancel') && String(detail.orderStatus) !== '4' && String(detail.deliveryStatus) !== '2'" class="detail-action-btn danger-btn" @tap="cancelPurchase">作废</button>
      </view>
    </template>

    <!-- ════════ 编辑面板（覆盖整个详情页） ════════ -->
    <view class="overlay-mask" v-if="panel === 'edit'" @tap="closePanel">
      <view class="form-page" @tap.stop>
        <view class="form-hero">
          <view class="form-hero-icon">✎</view>
          <view class="form-hero-info">
            <view class="form-hero-title">编辑购买单</view>
            <view class="form-hero-meta">会员服务 · 请完善必要信息后保存</view>
          </view>
          <view class="form-hero-close" @tap="closePanel">✕</view>
        </view>
        <view class="form-section-card">
          <view class="form-section-header"><view class="form-section-dot required"></view><text class="form-section-title">必填信息</text></view>
          <view class="form-item">
            <view class="form-label-row"><text class="form-label">购买日期</text><text class="form-required-tag">*</text></view>
            <picker mode="date" :value="form.purchaseDate" @change="form.purchaseDate=$event.detail.value"><view class="form-control picker" :class="{ 'has-value': form.purchaseDate }"><text class="form-picker-text">{{ form.purchaseDate || '请选择购买日期' }}</text><text class="form-picker-arrow">›</text></view></picker>
          </view>
          <view class="form-item">
            <view class="form-label-row"><text class="form-label">顾客类型</text><text class="form-required-tag">*</text></view>
            <picker :range="customerTypes" range-key="label" :value="customerTypeIndex" @change="selectCustomerType"><view class="form-control picker" :class="{ 'has-value': form.customerType }"><text class="form-picker-text">{{ customerTypeText(form.customerType) }}</text><text class="form-picker-arrow">›</text></view></picker>
          </view>
          <view class="form-item">
            <view class="form-label-row"><text class="form-label">顾客姓名</text><text class="form-required-tag" v-if="form.customerType !== 'MEMBER'">*</text></view>
            <input class="form-control input" v-model="form.customerName" :placeholder="form.customerType === 'MEMBER' ? '由会员信息自动带入' : '请输入顾客姓名'" :readonly="form.customerType === 'MEMBER'" />
          </view>
          <view class="form-item">
            <view class="form-label-row"><text class="form-label">手机号</text></view>
            <input class="form-control input" v-model="form.customerPhone" type="number" placeholder="可不填" />
          </view>
          <view class="form-item">
            <view class="form-label-row"><text class="form-label">核算周期</text><text class="form-required-tag">*</text></view>
            <picker :range="periods" range-key="label" :value="periodIndex" @change="selectPeriod"><view class="form-control picker" :class="{ 'has-value': form.periodId }"><text class="form-picker-text">{{ selectedPeriod?.label || '请选择当前机构核算周期' }}</text><text class="form-picker-arrow">›</text></view></picker>
          </view>
        </view>
        <view class="form-section-card">
          <view class="form-section-header"><view class="form-section-dot required"></view><text class="form-section-title">购买明细</text><text class="form-section-count" v-if="form._allItems && form._allItems.length > 1">共{{form._allItems.length}}项 · 仅编辑首项</text></view>
          <view class="form-item">
            <view class="form-label-row"><text class="form-label">商品</text><text class="form-required-tag">*</text></view>
            <picker :range="products" range-key="productName" :value="productIndex" @change="selectProduct"><view class="form-control picker" :class="{ 'has-value': form.item.productId }"><text class="form-picker-text">{{ selectedProduct?.productName || '请选择商品' }}</text><text class="form-picker-arrow">›</text></view></picker>
          </view>
          <view class="form-item" v-if="form.item.productId && form.periodId">
            <view class="form-label-row"><text class="form-label">销售政策</text></view>
            <picker :range="policies" range-key="policyName" :value="policyIndex" @change="selectPolicy"><view class="form-control picker" :class="{ 'has-value': form.item.policyId }"><text class="form-picker-text">{{ selectedPolicy?.policyName || '不参加活动，按单价购买' }}</text><text class="form-picker-arrow">›</text></view></picker>
          </view>
          <view class="form-item" v-if="packages.length">
            <view class="form-label-row"><text class="form-label">购买套餐</text></view>
            <picker :range="packages" range-key="label" :value="packageIndex" @change="selectPackage"><view class="form-control picker" :class="{ 'has-value': form.item.packageId }"><text class="form-picker-text">{{ selectedPackage?.label || '请选择套餐档位' }}</text><text class="form-picker-arrow">›</text></view></picker>
          </view>
          <view class="form-grid-2col">
            <view class="form-item">
              <view class="form-label-row"><text class="form-label">购买数量</text><text class="form-required-tag">*</text></view>
              <input class="form-control input" v-model="form.item.purchaseQuantity" type="digit" placeholder="0.000" @input="limit('purchaseQuantity', $event.detail.value, 3)" />
            </view>
            <view class="form-item">
              <view class="form-label-row"><text class="form-label">赠送数量</text></view>
              <input class="form-control input" v-model="form.item.giftQuantity" type="digit" placeholder="0.000" @input="limit('giftQuantity', $event.detail.value, 3)" />
            </view>
          </view>
          <view class="form-item">
            <view class="form-label-row"><text class="form-label">单价</text><text class="form-required-tag">*</text></view>
            <input class="form-control input" v-model="form.item.unitPrice" type="digit" placeholder="0.00" @input="limit('unitPrice', $event.detail.value, 2)" />
          </view>
        </view>
        <view class="form-section-card">
          <view class="form-section-header collapsible" @tap="remarkCollapsed = !remarkCollapsed"><view class="form-section-dot"></view><text class="form-section-title">其他信息</text><text class="form-section-count">1项</text><text class="form-collapse-arrow" :class="{ collapsed: remarkCollapsed }">›</text></view>
          <view class="form-item" v-if="!remarkCollapsed">
            <view class="form-label-row"><text class="form-label">备注</text></view>
            <textarea class="form-control textarea" v-model="form.remark" placeholder="备注，可不填" />
          </view>
        </view>
        <view class="form-footer-placeholder"></view>
        <view class="form-footer">
          <button class="form-btn-secondary" @tap="closePanel">取消</button>
          <button class="form-btn-primary" @tap="saveEdit">保存编辑</button>
        </view>
      </view>
    </view>

    <!-- ════════ 收款/领取/绑定面板（底部弹出） ════════ -->
    <view class="sheet-mask" v-if="panel === 'payment' || panel === 'delivery' || panel === 'bind'" @tap="closePanel">
      <view class="sheet-panel" @tap.stop>
        <view class="sheet-title">{{ panelTitle }}</view>
        <template v-if="panel === 'payment'">
          <view class="sheet-summary">
            <view class="sheet-summary-row"><text>购买单号</text><text>{{ detail.purchaseNo || detail.purchaseId }}</text></view>
            <view class="sheet-summary-row"><text>应收金额</text><text>¥{{ money(detail.totalAmount) }}</text></view>
            <view class="sheet-summary-row"><text>累计已收</text><text>¥{{ money(detail.paidAmount) }}</text></view>
            <view class="sheet-summary-row remaining"><text>剩余应收</text><text>¥{{ money(detail.receivableAmount) }}</text></view>
          </view>
          <view class="sheet-row sheet-row-stack"><text class="sheet-label">缴款金额</text><input class="sheet-input" v-model="paymentForm.paymentAmount" type="digit" placeholder="0.00" @input="limitPayment" /></view>
          <view class="sheet-row sheet-row-stack"><text class="sheet-label">收款方式</text><picker :range="paymentMethods" range-key="label" :value="paymentIndex" @change="paymentIndex=Number($event.detail.value)"><view class="sheet-picker">{{ paymentMethods[paymentIndex]?.label || '请选择' }}<text class="sheet-picker-arrow">▸</text></view></picker></view>
        </template>
        <template v-else-if="panel === 'delivery'">
          <view class="sheet-sub">{{ detail.customerName || '未登记顾客' }}，请填写领取数量</view>
          <view class="sheet-row sheet-row-stack"><text class="sheet-label">商品明细</text><picker :range="deliveryItems" range-key="label" :value="deliveryIndex" @change="selectDeliveryItem"><view class="sheet-picker">{{ deliveryItems[deliveryIndex]?.label || '请选择' }}<text class="sheet-picker-arrow">▸</text></view></picker></view>
          <view class="sheet-grid-2col">
            <view class="sheet-row sheet-row-stack"><text class="sheet-label">销售领取</text><input class="sheet-input" v-model="deliveryForm.saleDeliveryQuantity" type="digit" placeholder="0.000" /></view>
            <view class="sheet-row sheet-row-stack"><text class="sheet-label">赠品领取</text><input class="sheet-input" v-model="deliveryForm.giftDeliveryQuantity" type="digit" placeholder="0.000" /></view>
          </view>
          <view class="sheet-row sheet-row-stack"><text class="sheet-label">领取人</text><input class="sheet-input" v-model="deliveryForm.receiverName" placeholder="请输入领取人" /></view>
        </template>
        <template v-else-if="panel === 'bind'">
          <view class="sheet-summary">
            <view class="sheet-summary-row"><text>原购买单</text><text>{{ detail.purchaseNo || detail.purchaseId }}</text></view>
            <view class="sheet-summary-row"><text>顾客</text><text>{{ detail.customerName || '未登记顾客' }}</text></view>
          </view>
          <MemberSearch :dept-id="detail.deptId || currentDeptId" @select="selectBindMember" @clear="clearBindMember" />
        </template>
        <view class="sheet-actions">
          <button class="sheet-cancel" @tap="closePanel">取消</button>
          <button class="sheet-confirm" :disabled="panel === 'bind' && !bindForm.memberId" @tap="panel === 'payment' ? savePayment() : panel === 'delivery' ? saveDelivery() : confirmBind()">{{ panel === 'payment' ? '确认收款' : panel === 'delivery' ? '确认领取' : '确认绑定' }}</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import MemberSearch from '@/components/MemberSearch/index.vue'
import { hasActionPermission } from '@/utils/permission.js'
import { workContext } from '@/utils/workContext.js'

const newPurchaseForm = () => ({ purchaseDate: '', periodId: '', customerType: 'MEMBER', customerName: '', customerPhone: '', remark: '', item: { productId: '', purchaseQuantity: '', unitPrice: '', giftQuantity: '' } })

export default {
  components: { MemberSearch },
  data() { return {
    loading: true,
    purchaseId: null,
    currentDeptId: null,
    currentDeptName: '',
    detail: {},
    panel: '',
    form: newPurchaseForm(),
    products: [],
    periods: [],
    policies: [],
    bindForm: { memberId: '' },
    paymentMethods: [{ label: '现金', value: 'CASH' }, { label: '微信支付', value: 'WECHAT' }, { label: '支付宝', value: 'ALIPAY' }, { label: '银行转账', value: 'BANK' }, { label: '其他', value: 'OTHER' }],
    paymentIndex: 0,
    paymentForm: { paymentAmount: '' },
    deliveryItems: [],
    deliveryIndex: 0,
    deliveryForm: { saleDeliveryQuantity: '', giftDeliveryQuantity: '', receiverName: '' },
    remarkCollapsed: false,
    returns: []
  } },
  computed: {
    panelTitle() { return ({ edit: '编辑购买单', payment: '登记收款', delivery: '登记领取', bind: '绑定会员' })[this.panel] },
    customerTypes() { return [{ label: '会员', value: 'MEMBER' }, { label: '非会员', value: 'CUSTOMER' }, { label: '散客', value: 'WALK_IN' }] },
    customerTypeIndex() { const i = this.customerTypes.findIndex(x => x.value === this.form.customerType); return i < 0 ? 0 : i },
    productIndex() { const i = this.products.findIndex(x => String(x.productId) === String(this.form.item.productId)); return i < 0 ? 0 : i },
    periodIndex() { const i = this.periods.findIndex(x => String(x.periodId) === String(this.form.periodId)); return i < 0 ? 0 : i },
    policyIndex() { const i = this.policies.findIndex(x => String(x.policyId) === String(this.form.item.policyId)); return i < 0 ? 0 : i },
    packages() { return (this.policies[this.policyIndex]?.packages || []).map((x, i) => ({ ...x, label: `${x.packageName || `档位${i + 1}`}：买${this.quantity(x.purchaseQuantity)}送${this.quantity(x.giftQuantity)} · ¥${this.money(x.packagePrice)}` })) },
    packageIndex() { const i = this.packages.findIndex(x => String(x.packageId) === String(this.form.item.packageId)); return i < 0 ? 0 : i },
    selectedProduct() { return this.form.item.productId ? this.products[this.productIndex] : null },
    selectedPolicy() { return this.form.item.policyId ? this.policies[this.policyIndex] : null },
    selectedPackage() { return this.form.item.packageId ? this.packages[this.packageIndex] : null },
    selectedPeriod() { return this.form.periodId ? this.periods[this.periodIndex] : null },
    totalPurchaseQty() { return (this.detail.items || []).reduce((s, i) => s + Number(i.purchaseQuantity || 0), 0) },
    totalGiftQty() { return (this.detail.items || []).reduce((s, i) => s + Number(i.giftQuantity || 0), 0) }
  },
  onLoad(options) {
    const scope = workContext.snapshot()
    this.currentDeptId = scope.currentDeptId
    this.currentDeptName = scope.currentDept?.name || scope.currentDept?.deptName || '未选择机构'
    this.purchaseId = options.purchaseId
    this.loadAll().then(() => {
      if (options.autoEdit === '1') this.panel = 'edit'
    })
  },
  onShow() {
    if (this.purchaseId) {
      this.loadDetail()
      this.loadReturns()
    }
  },
  methods: {
    can(action) { return hasActionPermission('memberPurchase', action) },
    today() { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` },
    isoDateTime(value) { if (!value) return new Date().toISOString(); const text = String(value); if (/^\d{4}-\d{2}-\d{2}$/.test(text)) return new Date(`${text}T00:00:00`).toISOString(); return new Date(text.replace(' ', 'T')).toISOString() },
    dateText(v) { return v ? String(v).replace('T',' ').slice(0,19) : '-' },
    dateOnly(v) { return v ? String(v).replace('T',' ').slice(0,10) : '-' },
    money(v) { return Number(v || 0).toFixed(2) },
    quantity(v) { return Number(v || 0).toFixed(3).replace(/\.0+$/, '').replace(/(\.\d*?)0+$/, '$1') },
    customerTypeText(v) { return ({ MEMBER: '会员', CUSTOMER: '非会员', WALK_IN: '散客' })[v] || v || '-' },
    paymentStatusText(v) { return ({ '0':'未收款','1':'部分收款','2':'已收清','3':'已退款' })[String(v)] || '未知' },
    deliveryStatusText(v) { return ({ '0':'未领取','1':'部分领取','2':'全部领取' })[String(v)] || '未知' },
    paymentMethodText(v) { return this.paymentMethods.find(x => x.value === v)?.label || ({ WECHAT:'微信支付', ALIPAY:'支付宝', CASH:'现金', BANK:'银行转账' })[v] || v || '-' },
    paymentStatusClass(v) { const s = String(v ?? ''); return s === '2' ? 'status-info' : s === '1' ? 'status-warn' : 'status-warn' },
    deliveryStatusClass(v) { const s = String(v ?? ''); return s === '2' ? 'status-info' : s === '1' ? 'status-warn' : 'status-warn' },
    unwrap(res) { return res?.rows || res?.data?.rows || res?.data || [] },

    async loadAll() {
      await Promise.all([this.loadDetail(), this.loadOptions(), this.loadReturns()])
      this.loading = false
    },
    async loadDetail() {
      const res = await request({ url: `/member/purchase/${this.purchaseId}`, method: 'GET' })
      this.detail = res.data || res
      const allItems = (this.detail.items || []).map(x => ({ ...x }))
      this.form = { ...newPurchaseForm(), ...this.detail, item: allItems[0] || {}, _allItems: allItems }
    },
    async loadReturns() {
      try {
        const res = await request({ url: '/member/purchase-return/list', method: 'GET', data: { pageNum: 1, pageSize: 200, purchaseId: this.purchaseId, deptId: this.currentDeptId }, silent: true })
        this.returns = this.unwrap(res) || []
      } catch (e) { this.returns = [] }
    },
    returnStatusText(s) { return ({ DRAFT: '草稿', CONFIRMED: '已确认', COMPLETED: '已完成', CANCELLED: '已作废' })[String(s || '')] || (String(s) || '-') },
    returnStatusClass(s) { const t = String(s || ''); return t === 'COMPLETED' ? 'status-danger' : t === 'CONFIRMED' ? 'status-warn' : t === 'CANCELLED' ? 'status-muted' : 'status-warn' },
    async loadOptions() {
      try {
        const [p, a] = await Promise.all([
          request({ url: '/finance/product/list', method: 'GET', data: { pageNum: 1, pageSize: 200, deptId: this.currentDeptId }, silent: true }),
          request({ url: '/finance/accountingPeriod/list', method: 'GET', data: { pageNum: 1, pageSize: 200, deptId: this.currentDeptId }, silent: true })
        ])
        this.products = this.unwrap(p)
        this.periods = this.unwrap(a).filter(x => String(x.deptId) === String(this.currentDeptId)).map(x => ({
          ...x, label: `${x.periodNo || x.periodId}（${this.dateText(x.startTime)} 至 ${x.endTime ? this.dateText(x.endTime) : '当前'}）`
        }))
      } catch (e) { this.products = []; this.periods = [] }
    },
    async loadPolicies() {
      if (!this.form.item.productId || !this.form.periodId) { this.policies = []; return }
      const res = await request({ url: '/member/campaign/policy/list', method: 'GET', data: { pageNum: 1, pageSize: 200, deptId: this.currentDeptId, productId: this.form.item.productId, periodId: this.form.periodId, status: '1' } })
      const rows = this.unwrap(res).filter(x => String(x.productId) === String(this.form.item.productId) && String(x.periodId) === String(this.form.periodId))
      const detailed = await Promise.all(rows.map(async (policy) => {
        if (Array.isArray(policy.packages) && policy.packages.length) return policy
        try { const detail = await request({ url: `/member/campaign/policy/${policy.policyId}`, method: 'GET' }); return detail?.data || detail || policy }
        catch (e) { return policy }
      }))
      this.policies = [{ policyId: '', policyName: '不参加活动，按单价购买', packages: [] }, ...detailed]
    },

    selectCustomerType(e) { this.form.customerType = this.customerTypes[Number(e.detail.value)].value },
    selectProduct(e) { const p = this.products[Number(e.detail.value)]; this.form.item.productId = p?.productId || ''; this.form.item.policyId = ''; this.form.item.packageId = ''; this.form.item.giftQuantity = ''; if (p && (p.salePrice != null || p.defaultSalePrice != null || p.price != null)) { this.form.item.unitPrice = this.money(p.salePrice ?? p.defaultSalePrice ?? p.price) } this.loadPolicies() },
    selectPolicy(e) { const policy = this.policies[Number(e.detail.value)]; this.form.item.policyId = policy?.policyId || ''; this.form.item.packageId = ''; if (policy?.packages?.length === 1) this.selectPackage({ detail: { value: 0 } }) },
    selectPackage(e) { const pkg = this.packages[Number(e.detail.value)]; this.form.item.packageId = pkg?.packageId || ''; if (pkg) { this.form.item.purchaseQuantity = this.quantity(pkg.purchaseQuantity); this.form.item.giftQuantity = this.quantity(pkg.giftQuantity); this.form.item.unitPrice = this.money(Number(pkg.packagePrice || 0) / Number(pkg.purchaseQuantity || 1)) } },
    selectPeriod(e) { this.form.periodId = this.periods[Number(e.detail.value)]?.periodId || ''; this.loadPolicies() },
    limit(key, value, precision) { const s = String(value || '').replace(/[^\d.]/g,'').replace(/\.(?=.*\.)/g,''); this.form.item[key] = s.includes('.') ? s.split('.')[0] + '.' + s.split('.')[1].slice(0, precision) : s },
    limitPayment(e) { this.paymentForm.paymentAmount = String(e.detail.value || '').replace(/[^\d.]/g,'').replace(/\.(?=.*\.)/g,'').replace(/(\.\d{2}).*/,'$1') },

    switchToEdit() { this.panel = 'edit' },
    closePanel() { this.panel = '' },

    async saveEdit() {
      const allItems = (this.form._allItems || []).map(x => ({ ...x }))
      if (allItems.length) allItems[0] = { ...allItems[0], ...this.form.item }
      await request({ url: `/member/purchase/${this.detail.purchaseId}`, method: 'PUT', data: { customerName: this.form.customerName, customerPhone: this.form.customerPhone, purchaseDate: this.form.purchaseDate, remark: this.form.remark, items: allItems } })
      uni.showToast({ title: '购买单已保存', icon: 'success' })
      this.panel = ''
      await this.loadDetail()
    },

    openPayment() { this.paymentForm = { paymentAmount: this.money(this.detail.receivableAmount) }; this.paymentIndex = 0; this.panel = 'payment' },
    async savePayment() {
      const n = Number(this.paymentForm.paymentAmount)
      if (!(n > 0) || n > Number(this.detail.receivableAmount || 0)) return uni.showToast({ title: '收款金额必须大于0且不能超过待缴金额', icon: 'none' })
      await request({ url: `/member/purchase/${this.detail.purchaseId}/payment`, method: 'POST', data: { paymentAmount: n, paymentMethod: this.paymentMethods[this.paymentIndex].value, paymentDate: this.isoDateTime(this.today()), idempotencyKey: `mp-payment-${Date.now()}` } })
      uni.showToast({ title: '收款成功', icon: 'success' })
      this.panel = ''
      await this.loadDetail()
    },

    async openDelivery() {
      this.deliveryItems = (this.detail.items || []).map(x => ({ ...x, label: `${x.productNameSnapshot || '商品'}（待领取${this.quantity(x.remainingQuantity)}）` })).filter(x => Number(x.remainingQuantity || 0) > 0)
      this.deliveryIndex = 0
      const first = this.deliveryItems[0]
      this.deliveryForm = {
        saleDeliveryQuantity: this.quantity(Math.max(0, Number(first?.purchaseQuantity || 0) - Number(first?.deliveredSaleQuantity || 0))),
        giftDeliveryQuantity: this.quantity(Math.max(0, Number(first?.giftQuantity || 0) - Number(first?.deliveredGiftQuantity || 0))),
        receiverName: this.detail.customerName || ''
      }
      this.panel = 'delivery'
    },
    selectDeliveryItem(e) {
      this.deliveryIndex = Number(e.detail.value)
      const item = this.deliveryItems[this.deliveryIndex]
      this.deliveryForm.saleDeliveryQuantity = this.quantity(Math.max(0, Number(item?.purchaseQuantity || 0) - Number(item?.deliveredSaleQuantity || 0)))
      this.deliveryForm.giftDeliveryQuantity = this.quantity(Math.max(0, Number(item?.giftQuantity || 0) - Number(item?.deliveredGiftQuantity || 0)))
    },
    async saveDelivery() {
      const item = this.deliveryItems[this.deliveryIndex]
      const sale = Number(this.deliveryForm.saleDeliveryQuantity || 0)
      const gift = Number(this.deliveryForm.giftDeliveryQuantity || 0)
      const saleRemaining = Number(item?.purchaseQuantity || 0) - Number(item?.deliveredSaleQuantity || 0)
      const giftRemaining = Number(item?.giftQuantity || 0) - Number(item?.deliveredGiftQuantity || 0)
      if (!item || sale + gift <= 0 || sale > saleRemaining || gift > giftRemaining) return uni.showToast({ title: '领取数量不能超过剩余可领取数量', icon: 'none' })
      await request({ url: `/member/purchase/${this.detail.purchaseId}/delivery`, method: 'POST', data: { itemId: item.itemId, saleDeliveryQuantity: sale, giftDeliveryQuantity: gift, totalDeliveryQuantity: sale + gift, deliveryDate: this.isoDateTime(this.today()), receiverName: this.deliveryForm.receiverName, idempotencyKey: `mp-delivery-${Date.now()}` } })
      uni.showToast({ title: '领取成功', icon: 'success' })
      this.panel = ''
      await this.loadDetail()
    },

    openBind() { this.bindForm = { memberId: '' }; this.panel = 'bind' },
    selectBindMember(member) { this.bindForm = { memberId: member?.memberId || '', memberName: member?.memberName || '' } },
    clearBindMember() { this.bindForm = { memberId: '' } },
    async confirmBind() {
      if (!this.bindForm.memberId) return uni.showToast({ title: '请选择要绑定的会员', icon: 'none' })
      await request({ url: `/member/purchase/${this.detail.purchaseId}/bind-member/${this.bindForm.memberId}`, method: 'PUT' })
      uni.showToast({ title: '绑定成功', icon: 'success' })
      this.panel = ''
      await this.loadDetail()
    },

    async cancelPurchase() {
      const ok = await new Promise(resolve => uni.showModal({ title: '确认作废', content: `确认作废购买单 ${this.detail.purchaseNo || this.detail.purchaseId}？`, success: r => resolve(r.confirm) }))
      if (!ok) return
      await request({ url: `/member/purchase/${this.detail.purchaseId}/cancel`, method: 'PUT' })
      uni.showToast({ title: '购买单已作废', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 300)
    },
    openReturnDetail(r) {
      uni.navigateTo({ url: `/pages/member-purchase-return/index?returnId=${r.returnId}&purchaseId=${this.purchaseId}` })
    }
  }
}
</script>

<style scoped>
.loading-wrap{display:flex;align-items:center;justify-content:center;padding:200rpx 0}

.detail-page{height:100vh;background:#E8EEF5;padding-bottom:calc(40rpx + env(safe-area-inset-bottom));box-sizing:border-box;overflow-y:auto;-webkit-overflow-scrolling:touch}

.detail-hero{position:relative;margin:24rpx 28rpx;border-radius:20rpx;overflow:hidden}
.detail-hero-bg{position:absolute;inset:0;background:linear-gradient(135deg,#087CF0,#5AA9E8,#A8C7E5);border-radius:20rpx}
.detail-hero-content{position:relative;z-index:1;padding:40rpx 36rpx}
.detail-hero-eyebrow{font-size:22rpx;color:rgba(255,255,255,.7);margin-bottom:12rpx;letter-spacing:2rpx}
.detail-hero-title{font-size:36rpx;font-weight:600;color:#fff;margin-bottom:16rpx;line-height:1.4;overflow-wrap:anywhere;word-break:break-word}
.detail-hero-value{font-size:52rpx;font-weight:700;color:#fff;margin-bottom:12rpx}
.detail-hero-meta{font-size:24rpx;color:rgba(255,255,255,.7)}

.detail-section{margin:20rpx 28rpx 0;background:#fff;border-radius:20rpx;padding:28rpx 32rpx;box-shadow:0 2rpx 16rpx rgba(8,124,240,.06);box-sizing:border-box}
.detail-section-title{font-size:28rpx;font-weight:600;color:#1A2332;margin-bottom:20rpx;padding-left:16rpx;border-left:4rpx solid #087CF0}

.detail-highlight-grid{display:flex;flex-wrap:wrap;gap:12rpx}
.detail-highlight-item{flex:1;min-width:45%;background:#F5F8FA;border-radius:12rpx;padding:18rpx 20rpx;box-sizing:border-box}
.detail-highlight-label{font-size:22rpx;color:#94A3B8;margin-bottom:6rpx}
.detail-highlight-value{font-size:28rpx;font-weight:600;color:#1A2332;overflow-wrap:anywhere;word-break:break-word}
.detail-highlight-value.status-warn{display:inline-block;background:#FEF3C7;color:#92400E;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.status-info{display:inline-block;background:#E0F2FE;color:#075985;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.tone-money{color:#B45309}
.detail-highlight-value.tone-success{color:#087CF0}
.detail-highlight-value.tone-warning{color:#C26A1B}

.detail-item{background:#F5F8FA;border-radius:16rpx;padding:24rpx;margin-bottom:20rpx;box-sizing:border-box}
.detail-item:last-child{margin-bottom:0}
.detail-item-header{display:flex;align-items:center;justify-content:space-between;padding-bottom:16rpx;border-bottom:1rpx solid #E2E8F0;margin-bottom:16rpx}
.detail-item-title{font-size:28rpx;font-weight:700;color:#1A2332;overflow-wrap:anywhere;word-break:break-word}
.detail-row{display:flex;align-items:center;min-height:72rpx;padding:16rpx 0;box-sizing:border-box;line-height:34rpx}
.detail-label{width:140rpx;font-size:26rpx;color:#64748B;flex-shrink:0}
.detail-value-text{font-size:26rpx;color:#1A2332;flex:1;overflow-wrap:anywhere;word-break:break-word}
.detail-value-text.amount{font-weight:700;color:#087CF0}

.detail-summary{margin-top:24rpx;padding-top:24rpx;border-top:2rpx solid #E2E8F0}
.detail-summary-row{display:flex;align-items:center;padding:12rpx 0}
.detail-summary-label{flex:1;font-size:28rpx;color:#1A2332;font-weight:500}
.detail-summary-value{font-size:32rpx;color:#087CF0;font-weight:700}

.detail-remark{font-size:26rpx;color:#475569;line-height:1.6;white-space:pre-wrap;word-break:break-word;background:#F5F8FA;border-radius:12rpx;padding:20rpx}

.payment-history-item{padding:20rpx 0;border-bottom:1rpx solid #E2E8F0}
.payment-history-item:last-child{border-bottom:0}
.payment-history-main,.payment-history-meta{display:flex;align-items:center;justify-content:space-between;gap:16rpx}
.payment-history-no,.payment-history-meta{overflow-wrap:anywhere;word-break:break-word}
.payment-history-no{color:#1A2332;font-size:27rpx;font-weight:600}
.payment-history-amount{color:#087CF0;font-size:30rpx;font-weight:700;flex-shrink:0}
.payment-history-meta{margin-top:10rpx;color:#64748B;font-size:24rpx}
.refund-amount{color:#DC2626}
.return-status-line{margin-top:10rpx;text-align:right}
.compact-status{display:inline-block;padding:4rpx 16rpx;border-radius:999rpx;font-size:22rpx;font-weight:600}
.compact-status.status-warn{background:#FEF3C7;color:#92400E}
.compact-status.status-info{background:#E0F2FE;color:#075985}
.compact-status.status-danger{background:#FEE2E2;color:#991B1B}
.compact-status.status-muted{background:#F1F5F9;color:#475569}
.detail-empty{text-align:center;color:#94A3B8;padding:32rpx 0;font-size:24rpx}

.detail-footer-placeholder{height:140rpx}
.detail-footer-bar{position:fixed;left:0;right:0;bottom:0;display:flex;align-items:center;justify-content:center;gap:16rpx;padding:16rpx 24rpx;padding-bottom:calc(16rpx + env(safe-area-inset-bottom));background:#fff;box-shadow:0 -2rpx 16rpx rgba(8,124,240,.06);z-index:100;flex-wrap:wrap}
.detail-action-btn{flex:1;height:76rpx;line-height:76rpx;font-size:28rpx;font-weight:500;border-radius:16rpx;text-align:center;border:none;margin:0;padding:0;min-width:calc(25% - 12rpx)}
.detail-action-btn::after{border:none}
.detail-action-btn.primary-btn{background:#087CF0;color:#fff}
.detail-action-btn.payment-btn{background:linear-gradient(135deg,#EA580C,#F59E0B);color:#fff;font-weight:600}
.detail-action-btn.delivery-btn{background:linear-gradient(135deg,#0284C7,#38BDF8);color:#fff;font-weight:600}
.detail-action-btn.bind-btn{background:linear-gradient(135deg,#7C3AED,#A855F7);color:#fff;font-weight:600}
.detail-action-btn.danger-btn{background:linear-gradient(135deg,#DC2626,#F87171);color:#fff;font-weight:600}

/* ── 全屏遮罩（编辑页） ── */
.overlay-mask{position:fixed;inset:0;background:rgba(15,23,42,.45);z-index:50;overflow:hidden}
.form-page{height:100vh;background:#E8EEF5;padding-bottom:calc(40rpx + env(safe-area-inset-bottom));box-sizing:border-box;overflow-y:auto;-webkit-overflow-scrolling:touch}

.form-hero{display:flex;align-items:center;gap:24rpx;padding:36rpx 28rpx;background:linear-gradient(135deg,#C7DCF2 0%,#EAF3FC 100%);border-top:1rpx solid #B7D1EB;border-radius:0 0 24rpx 24rpx;position:relative}
.form-hero-icon{width:72rpx;height:72rpx;line-height:72rpx;text-align:center;background:rgba(255,255,255,.15);border-radius:18rpx;font-size:36rpx;color:#fff;flex-shrink:0;display:flex;align-items:center;justify-content:center}
.form-hero-info{flex:1;min-width:0}
.form-hero-title{font-size:34rpx;font-weight:700;color:#1F2D3D}
.form-hero-meta{margin-top:6rpx;font-size:22rpx;color:#6E8197}
.form-hero-close{position:absolute;top:24rpx;right:24rpx;width:56rpx;height:56rpx;line-height:56rpx;text-align:center;border-radius:50%;background:rgba(255,255,255,.3);color:#5A6B7F;font-size:30rpx}

.form-section-card{margin:20rpx 28rpx 0;padding:28rpx;background:#fff;border-radius:20rpx;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);box-sizing:border-box}
.form-section-header{display:flex;align-items:center;gap:10rpx;margin-bottom:20rpx}
.form-section-header.collapsible{padding:4rpx 0}
.form-section-dot{width:8rpx;height:8rpx;border-radius:50%;background:#087CF0;flex-shrink:0}
.form-section-dot.required{background:#EF4444}
.form-section-title{font-size:28rpx;font-weight:600;color:#1A2332;flex:1}
.form-section-count{margin-left:auto;font-size:22rpx;color:#94A3B8}
.form-collapse-arrow{font-size:32rpx;color:#94A3B8;transform:rotate(90deg);transition:transform .2s;margin-left:8rpx}
.form-collapse-arrow.collapsed{transform:rotate(0deg)}

.form-item{padding-top:20rpx;border-top:1rpx solid #E8EEF5}
.form-item + .form-item{border-top:1rpx solid #E8EEF5;padding-top:20rpx}
.form-item:first-child{border-top:0;padding-top:0}
.form-label-row{display:flex;align-items:center;gap:8rpx;margin-bottom:12rpx}
.form-label{font-size:24rpx;color:#5A6B7F;font-weight:500}
.form-required-tag{color:#EF4444;font-size:26rpx;font-weight:700}

.form-control{width:100%;min-height:84rpx;padding:0 24rpx;background:#F5F8FA;border:2rpx solid #E2E8F0;border-radius:14rpx;font-size:28rpx;color:#1A2332;box-sizing:border-box!important;transition:border-color .2s}
.form-control.input{display:block;width:100%;height:84rpx;line-height:84rpx}
.form-control.textarea{height:170rpx;padding-top:20rpx;line-height:1.5}
.form-control.picker{display:flex;align-items:center;justify-content:space-between;line-height:84rpx}
.form-picker-text{flex:1;color:#1A2332;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.form-control.picker:not(.has-value) .form-picker-text{color:#94A3B8}
.form-picker-arrow{font-size:32rpx;color:#CBD5E1;font-weight:300;margin-left:8rpx;flex-shrink:0}

.form-grid-2col{display:grid;grid-template-columns:1fr 1fr;gap:18rpx;box-sizing:border-box}
.form-grid-2col .form-item{border-top:0!important;padding-top:0!important}

.form-footer-placeholder{height:140rpx}
.form-footer{position:fixed;left:0;right:0;bottom:0;display:flex;gap:16rpx;padding:18rpx 24rpx;padding-bottom:calc(18rpx + env(safe-area-inset-bottom));background:rgba(255,255,255,.96);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border-top:1rpx solid #E2E8F0;z-index:100}
.form-btn-primary,.form-btn-secondary{flex:1;height:88rpx;line-height:88rpx;font-size:28rpx;border-radius:999rpx;text-align:center;border:none;margin:0;padding:0}
.form-btn-primary{background:linear-gradient(135deg,#087CF0,#5AA9E8);color:#fff;box-shadow:0 6rpx 20rpx rgba(8,124,240,.25)}
.form-btn-secondary{background:#fff;color:#5A6B7F;border:1rpx solid #E2E8F0}
.form-btn-primary::after,.form-btn-secondary::after{border:none}
.form-btn-primary[disabled]{opacity:.5}

/* ── 底部弹出面板（收款/领取/绑定） ── */
.sheet-mask{position:fixed;left:0;right:0;top:0;bottom:0;z-index:200;display:flex;align-items:flex-end;background:rgba(15,23,42,.45)}
.sheet-panel{width:100%;max-height:88vh;overflow-y:auto;padding:30rpx 28rpx calc(30rpx + env(safe-area-inset-bottom));border-radius:28rpx 28rpx 0 0;background:#fff;box-sizing:border-box;-webkit-overflow-scrolling:touch}
.sheet-title{font-size:34rpx;font-weight:800;color:#1A2332;margin-bottom:16rpx}
.sheet-sub{margin-top:8rpx;font-size:24rpx;color:#708196;margin-bottom:16rpx}

.sheet-summary{margin:8rpx 0 16rpx;padding:20rpx 24rpx;border-radius:16rpx;background:#F6F9FC}
.sheet-summary-row{display:flex;justify-content:space-between;padding:10rpx 0;color:#5A6B7F;font-size:25rpx;gap:16rpx}
.sheet-summary-row text:last-child{font-weight:600;color:#1A2332;text-align:right;overflow-wrap:anywhere;word-break:break-word}
.sheet-summary-row.remaining text:last-child{color:#C26A1B}

.sheet-row{display:flex;align-items:center;padding:18rpx 0;border-bottom:1rpx solid #E8EEF5}
.sheet-row:last-of-type{border-bottom:0}
.sheet-row-stack{flex-direction:column;align-items:stretch;padding:20rpx 0}
.sheet-label{font-size:24rpx;color:#5A6B7F;margin-bottom:12rpx}
.sheet-row-stack .sheet-label{width:auto;margin-bottom:12rpx}
.sheet-input{width:100%;box-sizing:border-box!important;text-align:left;padding:16rpx 20rpx;border:1rpx solid #E2E8F0;border-radius:12rpx;background:#F8FAFC;font-size:30rpx;min-height:72rpx;line-height:40rpx;color:#1A2332}
.sheet-picker{text-align:left;justify-content:space-between;padding:16rpx 20rpx;border:1rpx solid #E2E8F0;border-radius:12rpx;background:#F8FAFC;font-size:30rpx;min-height:72rpx;display:flex;align-items:center;color:#1A2332}
.sheet-picker-arrow{margin-left:8rpx;color:#CBD5E1;font-size:22rpx;flex-shrink:0}
.sheet-grid-2col{display:grid;grid-template-columns:1fr 1fr;gap:14rpx;box-sizing:border-box}

.sheet-actions{display:flex;gap:16rpx;margin-top:22rpx;padding-top:8rpx}
.sheet-cancel,.sheet-confirm{flex:1;height:78rpx;line-height:78rpx;border-radius:999rpx;font-size:26rpx;text-align:center;border:none;margin:0;padding:0}
.sheet-cancel{background:#F1F5F9;color:#475569}
.sheet-confirm{background:#EA580C;color:#fff}
.sheet-cancel::after,.sheet-confirm::after{border:none}
.sheet-confirm[disabled]{opacity:.5}
</style>
