<template>
  <view class="page" v-if="authorized">
    <view class="hero"><text class="eyebrow">会员服务</text><text class="hero-title">购买记录</text></view>
    <view class="work-scope" :class="{ 'work-scope-disabled': !switchable }" :hover-class="switchable ? 'work-scope-hover' : ''" hover-stay-time="80" hover-start-time="30" @tap="openDeptSwitcher"><view class="work-scope-mark" :class="{ 'work-scope-mark-disabled': !switchable }"></view><view class="work-scope-copy"><text class="work-scope-label">{{ scopeLabel }}</text><text class="work-scope-name">{{ currentDeptName || '未选择部门' }}</text></view></view>
    <view class="summary-bar" v-if="summary">
      <view><text class="summary-value">{{ quantity(summary.purchaseQuantity) }}<text class="summary-gift">赠</text><text class="summary-gift-num">{{ quantity(summary.giftQuantity) }}</text></text><text class="summary-label">数量</text></view>
      <view><text class="summary-value primary"><text class="summary-currency">¥</text>{{ money(summary.totalAmount) }}</text><text class="summary-label">应收金额</text></view>
      <view><text class="summary-value success"><text class="summary-currency">¥</text>{{ money(summary.paidAmount) }}</text><text class="summary-label">已收金额</text></view>
      <view><text class="summary-value warning"><text class="summary-currency">¥</text>{{ money(summary.receivableAmount) }}</text><text class="summary-label">待缴金额</text></view>
    </view>
    <view class="list-header-bar">
      <text class="list-header-count">共 {{ total }} 条</text>
      <button class="filter-fab" @tap="openFilterSheet"><text class="filter-fab-icon">⌕</text>筛选<text v-if="activeFilterCount" class="filter-fab-badge">{{ activeFilterCount }}</text></button>
    </view>
    <view class="bottom-bar">
      <button v-if="can('add')" class="add-button" @tap="openCreate">＋ 新增</button>
    </view>
    <scroll-view scroll-y class="scroll" @scrolltolower="nextPage">
      <view class="record-card" v-for="row in rows" :key="row.purchaseId" @tap="openDetail(row)">
        <view class="card-bar"></view>
        <view class="card-body compact-body">
          <view class="compact-row1">
            <view class="compact-title-wrap">
              <text class="compact-title">{{ row._customerName }}</text>
              <text class="compact-type-tag" :class="row._customerTypeClass">{{ row._customerTypeText }}</text>
            </view>
            <text class="compact-amount">{{ row._totalAmountText }}</text>
          </view>
          <view class="compact-row2">
            <text class="compact-meta qty">购买 {{ row._purchaseQuantityText }}</text>
            <text class="compact-meta qty">赠送 {{ row._giftQuantityText }}</text>
            <text class="compact-meta date">{{ row._purchaseDateText }}</text>
          </view>
          <view class="compact-row3">
            <text class="compact-meta paid">已收 {{ row._paidAmountText }}</text>
            <text class="compact-meta debt" v-if="row._showDebt">待缴 {{ row._receivableAmountText }}</text>
            <text class="compact-status-group">
              <text v-if="row._returnTagText" class="compact-status" :class="row._returnTagClass" @tap.stop="hasReturn(row) && openReturnDetail(row)">退货详情 · {{ row._returnTagText }}</text>
              <text class="compact-status" :class="row._paymentStatusClass">{{ row._paymentStatusText }}</text>
              <text class="compact-status" :class="row._deliveryStatusClass">{{ row._deliveryStatusText }}</text>
            </text>
          </view>
          <view class="compact-row4" v-if="can('bind') && row._customerType === 'WALK_IN' && !row.memberId">
            <text class="bind-entry" @tap.stop="openBind(row)">绑定会员</text>
          </view>
        </view>
      </view>
      <view class="section-card list-card state-card" v-if="!rows.length">
        <view class="empty">暂无购买记录</view>
      </view>
    </scroll-view>

    <!-- ════════ 筛选条件浮动面板 ════════ -->
    <view class="sheet-mask" v-if="filterSheetOpen" @tap="closeFilterSheet">
      <view class="sheet-panel" @tap.stop>
        <view class="sheet-title">筛选购买</view>
        <view class="sheet-row sheet-row-stack"><text class="sheet-label">顾客姓名</text><input class="sheet-input" v-model="keyword" placeholder="请输入顾客姓名" confirm-type="search" /></view>
        <view class="sheet-row sheet-row-stack"><text class="sheet-label">顾客类型</text><picker :range="customerTypeFilters" range-key="label" :value="customerTypeFilterIndex" @change="selectCustomerTypeFilter"><view class="sheet-picker">{{ customerTypeFilters[customerTypeFilterIndex]?.label || '全部顾客类型' }}<text class="sheet-picker-arrow">▸</text></view></picker></view>
        <view class="sheet-row sheet-row-stack"><text class="sheet-label">收款状态</text><picker :range="paymentStatusFilters" range-key="label" :value="paymentStatusFilterIndex" @change="selectPaymentStatusFilter"><view class="sheet-picker">{{ paymentStatusFilters[paymentStatusFilterIndex]?.label || '全部收款状态' }}<text class="sheet-picker-arrow">▸</text></view></picker></view>
        <view class="sheet-row sheet-row-stack"><text class="sheet-label">购买日期</text><view class="sheet-date-row"><picker mode="date" :value="filters.beginTime" @change="filters.beginTime=$event.detail.value"><view class="sheet-picker sheet-picker-date">{{ filters.beginTime || '开始日期' }}</view></picker><text class="sheet-date-sep">至</text><picker mode="date" :value="filters.endTime" @change="filters.endTime=$event.detail.value"><view class="sheet-picker sheet-picker-date">{{ filters.endTime || '结束日期' }}</view></picker></view></view>
        <view class="sheet-actions">
          <button class="sheet-cancel" @tap="resetFilters">重置</button>
          <button class="sheet-confirm" @tap="applyFilterSheet">查询</button>
        </view>
      </view>
    </view>

    <!-- ════════ 新建/编辑面板（参考销售记录 form/index.vue） ════════ -->
    <view class="overlay-mask" v-if="panel === 'create'" @tap="closePanel">
      <view class="form-page" @tap.stop>
        <view class="form-hero">
          <view class="form-hero-icon">＋</view>
          <view class="form-hero-info">
            <view class="form-hero-title">新增购买单</view>
            <view class="form-hero-meta">会员服务 · 请完善必要信息后保存</view>
          </view>
          <view class="form-hero-close" @tap="closePanel">✕</view>
        </view>

        <view class="form-section-card member-search-card" v-if="panel === 'create' && form.customerType === 'MEMBER'">
          <view class="form-section-header"><view class="form-section-dot required"></view><text class="form-section-title">选择会员</text><text class="form-section-count" v-if="form.memberId">已选择</text></view>
          <MemberSearch :dept-id="currentDeptId" @select="selectMember" @clear="clearMember" />
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
          <view class="form-section-header"><view class="form-section-dot required"></view><text class="form-section-title">购买明细</text></view>
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

        <!-- 预留：统一表单字段组件（供将来替换 form-item 时复用） -->
        <FormField v-if="false" field="{ key: 'placeholder', label: '占位符', type: 'text' }" :model-value="{}" />

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
          <button class="form-btn-primary" @tap="saveCreate">保存购买单</button>
        </view>
      </view>
    </view>

    <!-- ════════ 收款/领取/绑定面板（参考销售记录 claim-panel 底部弹出） ════════ -->
    <view class="sheet-mask" v-if="panel === 'payment' || panel === 'delivery' || panel === 'bind'" @tap="closePanel">
      <view class="sheet-panel" @tap.stop>
        <view class="sheet-title">{{ panelTitle }}</view>

        <template v-if="panel === 'payment'">
          <view class="sheet-summary">
            <view class="sheet-summary-row"><text>购买单号</text><text>{{ active.purchaseNo || active.purchaseId }}</text></view>
            <view class="sheet-summary-row"><text>应收金额</text><text>¥{{ money(active.totalAmount) }}</text></view>
            <view class="sheet-summary-row"><text>累计已收</text><text>¥{{ money(active.paidAmount) }}</text></view>
            <view class="sheet-summary-row remaining"><text>剩余应收</text><text>¥{{ money(active.receivableAmount) }}</text></view>
          </view>
          <view class="sheet-row sheet-row-stack"><text class="sheet-label">缴款金额</text><input class="sheet-input" v-model="paymentForm.paymentAmount" type="digit" placeholder="0.00" @input="limitPayment" /></view>
          <view class="sheet-row sheet-row-stack"><text class="sheet-label">收款方式</text><picker :range="paymentMethods" range-key="label" :value="paymentIndex" @change="paymentIndex=Number($event.detail.value)"><view class="sheet-picker">{{ paymentMethods[paymentIndex]?.label || '请选择' }}<text class="sheet-picker-arrow">▸</text></view></picker></view>
        </template>

        <template v-else-if="panel === 'delivery'">
          <view class="sheet-sub">{{ active.customerName || '未登记顾客' }}，请填写领取数量</view>
          <view class="sheet-row sheet-row-stack"><text class="sheet-label">商品明细</text><picker :range="deliveryItems" range-key="label" :value="deliveryIndex" @change="selectDeliveryItem"><view class="sheet-picker">{{ deliveryItems[deliveryIndex]?.label || '请选择' }}<text class="sheet-picker-arrow">▸</text></view></picker></view>
          <view class="sheet-grid-2col">
            <view class="sheet-row sheet-row-stack"><text class="sheet-label">销售领取</text><input class="sheet-input" v-model="deliveryForm.saleDeliveryQuantity" type="digit" placeholder="0.000" /></view>
            <view class="sheet-row sheet-row-stack"><text class="sheet-label">赠品领取</text><input class="sheet-input" v-model="deliveryForm.giftDeliveryQuantity" type="digit" placeholder="0.000" /></view>
          </view>
          <view class="sheet-row sheet-row-stack"><text class="sheet-label">领取人</text><input class="sheet-input" v-model="deliveryForm.receiverName" placeholder="请输入领取人" /></view>
        </template>

        <template v-else-if="panel === 'bind'">
          <view class="sheet-summary">
            <view class="sheet-summary-row"><text>原购买单</text><text>{{ bindTarget.purchaseNo || bindTarget.purchaseId }}</text></view>
            <view class="sheet-summary-row"><text>顾客</text><text>{{ bindTarget.customerName || '未登记顾客' }}</text></view>
          </view>
          <MemberSearch :dept-id="bindTarget.deptId || currentDeptId" @select="selectBindMember" @clear="clearBindMember" />
        </template>

        <view class="sheet-actions">
          <button class="sheet-cancel" @tap="closePanel">取消</button>
          <button class="sheet-confirm" :disabled="panel === 'bind' && !bindForm.memberId" @tap="panel === 'payment' ? savePayment() : panel === 'delivery' ? saveDelivery() : confirmBind()">{{ panel === 'payment' ? '确认收款' : panel === 'delivery' ? '确认领取' : '确认绑定' }}</button>
        </view>
      </view>
    </view>

    <!-- ════════ 选择顾客类型弹窗 ════════ -->
    <view class="sheet-mask" v-if="showCustomerTypePicker" @tap="showCustomerTypePicker=false">
      <view class="sheet-panel" @tap.stop>
        <view class="sheet-title">请选择顾客类型</view>
        <view class="customer-type-list">
          <view class="customer-type-item" v-for="(item, idx) in customerTypes" :key="item.value" @tap="pickCustomerType({ detail: { value: idx } })">
            <text class="customer-type-label">{{ item.label }}</text>
            <text class="customer-type-arrow">›</text>
          </view>
        </view>
        <view class="sheet-actions">
          <button class="sheet-cancel" @tap="showCustomerTypePicker=false">取消</button>
        </view>
      </view>
    </view>
    <dept-switcher
      v-model:visible="showDeptSwitcher"
      :current-dept-id="currentDeptId"
      :request-fn="request"
      @change="onDeptSwitcherChanged"
    />
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import MemberSearch from '@/components/MemberSearch/index.vue'
import DeptSwitcher from '@/components/DeptSwitcher.vue'
import FormField from '@/components/FormField.vue'
import { hasActionPermission, requireModulePermission } from '@/utils/permission.js'
import { workContext } from '@/utils/workContext.js'
import { applyWorkScopeToPage, openDeptSwitcher, handleDeptChanged } from '@/utils/listWorkScope.js'

const newPurchaseForm = () => ({ purchaseDate: '', periodId: '', customerType: 'MEMBER', customerName: '', customerPhone: '', remark: '', item: { productId: '', purchaseQuantity: '', unitPrice: '', giftQuantity: '' } })

export default {
  components: { MemberSearch, DeptSwitcher, FormField },
  data() { return { authorized: false, showDeptSwitcher: false, scopeLabel: '暂无可用数据范围', contextVersion: 0, currentDeptName: '', currentDeptId: '', switchable: false, deptCount: 0, rows: [], loading: false, keyword: '', filterSheetOpen: false, panel: '', active: {}, form: newPurchaseForm(), products: [], periods: [], policies: [], filters: { customerType: '', paymentStatus: '', beginTime: '', endTime: '' }, summary: null, pageNum: 1, pageSize: 20, total: 0, bindTarget: {}, bindForm: { memberId: '' }, returnMap: {}, lastLoadKey: '', purchaseIdSet: new Set(), paymentMethods: [{ label: '现金', value: 'CASH' }, { label: '微信支付', value: 'WECHAT' }, { label: '支付宝', value: 'ALIPAY' }, { label: '银行转账', value: 'BANK' }, { label: '其他', value: 'OTHER' }], paymentIndex: 0, paymentForm: { paymentAmount: '' }, deliveryItems: [], deliveryIndex: 0, deliveryForm: { saleDeliveryQuantity: '', giftDeliveryQuantity: '', receiverName: '' }, remarkCollapsed: false, showCustomerTypePicker: false } },
  computed: { panelTitle() { return ({ create: '新建购买单', edit: '编辑购买单', payment: '登记收款', delivery: '登记领取', bind: '绑定会员' })[this.panel] }, customerTypes() { return [{ label: '会员', value: 'MEMBER' }, { label: '非会员', value: 'CUSTOMER' }, { label: '散客', value: 'WALK_IN' }] }, customerTypeIndex() { const i = this.customerTypes.findIndex(x => x.value === this.form.customerType); return i < 0 ? 0 : i }, productIndex() { const i = this.products.findIndex(x => String(x.productId) === String(this.form.item.productId)); return i < 0 ? 0 : i }, periodIndex() { const i = this.periods.findIndex(x => String(x.periodId) === String(this.form.periodId)); return i < 0 ? 0 : i }, policyIndex() { const i = this.policies.findIndex(x => String(x.policyId) === String(this.form.item.policyId)); return i < 0 ? 0 : i }, packages() { return (this.policies[this.policyIndex]?.packages || []).map((x, i) => ({ ...x, label: `${x.packageName || `档位${i + 1}`}：买${this.quantity(x.purchaseQuantity)}送${this.quantity(x.giftQuantity)} · ¥${this.money(x.packagePrice)}` })) }, packageIndex() { const i = this.packages.findIndex(x => String(x.packageId) === String(this.form.item.packageId)); return i < 0 ? 0 : i }, selectedProduct() { return this.form.item.productId ? this.products[this.productIndex] : null }, selectedPolicy() { return this.form.item.policyId ? this.policies[this.policyIndex] : null }, selectedPackage() { return this.form.item.packageId ? this.packages[this.packageIndex] : null }, selectedPeriod() { return this.form.periodId ? this.periods[this.periodIndex] : null }, customerTypeFilters() { return [{ label: '全部', value: '' }, { label: '会员', value: 'MEMBER' }, { label: '非会员', value: 'CUSTOMER' }, { label: '散客', value: 'WALK_IN' }] }, paymentStatusFilters() { return [{ label: '全部', value: '' }, { label: '未收款', value: '0' }, { label: '部分收款', value: '1' }, { label: '已收清', value: '2' }] }, customerTypeFilterIndex() { const i = this.customerTypeFilters.findIndex(x => x.value === this.filters.customerType); return i < 0 ? 0 : i }, paymentStatusFilterIndex() { const i = this.paymentStatusFilters.findIndex(x => x.value === this.filters.paymentStatus); return i < 0 ? 0 : i }, totalPages() { return Math.max(1, Math.ceil(Number(this.total || 0) / Number(this.pageSize || 1))) }, activeFilterCount() { let n = 0; if (this.keyword) n++; if (this.filters.customerType) n++; if (this.filters.paymentStatus) n++; if (this.filters.beginTime) n++; if (this.filters.endTime) n++; return n } },
  onLoad() {
    this.authorized = requireModulePermission('memberPurchase');
    applyWorkScopeToPage(this);
    uni.$on('memberPurchase:updated', this._onRowsUpdated);
    if (this.authorized) { this.loadOptions(); this.load() }
  },
  onShow() {
    if (!this.authorized) return
    const { departmentChanged } = applyWorkScopeToPage(this)
    const key = this._computeLoadKey()
    if (departmentChanged || key !== this.lastLoadKey || this.rows.length === 0) { this.loadOptions(); this.load() }
  },
  onUnload() { uni.$off('memberPurchase:updated', this._onRowsUpdated) },
  methods: {
    openDeptSwitcher() { return openDeptSwitcher(this) },
    onDeptSwitcherChanged() { return handleDeptChanged(this, () => { this.pageNum = 1; this.loadOptions(); this.load() }) },
    emptyForm() { return { ...newPurchaseForm(), purchaseDate: this.today() } },
    can(action) { return hasActionPermission('memberPurchase', action) }, today() { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` }, isoDateTime(value) { if (!value) return new Date().toISOString(); const text = String(value); if (/^\d{4}-\d{2}-\d{2}$/.test(text)) return new Date(`${text}T00:00:00`).toISOString(); return new Date(text.replace(' ', 'T')).toISOString() }, dateText(v) { return v ? String(v).replace('T',' ').slice(0,19) : '-' }, money(v) { return Number(v || 0).toFixed(2) }, quantity(v) { return Number(v || 0).toFixed(3).replace(/\.0+$/, '').replace(/(\.\d*?)0+$/, '$1') }, customerTypeText(v) { return ({ MEMBER: '会员', CUSTOMER: '非会员', WALK_IN: '散客' })[v] || v || '-' }, customerTypeClass(v) { return ({ MEMBER: 'type-member', CUSTOMER: 'type-customer', WALK_IN: 'type-walkin' })[v] || 'type-customer' }, paymentStatusText(v) { return ({ '0':'未收款','1':'部分收款','2':'已收清','3':'已退款' })[String(v)] || '未知' }, deliveryStatusText(v) { return ({ '0':'未领取','1':'部分领取','2':'全部领取' })[String(v)] || '未知' }, paymentMethodText(v) { return this.paymentMethods.find(x => x.value === v)?.label || ({ WECHAT:'微信支付', ALIPAY:'支付宝', CASH:'现金', BANK:'银行转账' })[v] || v || '-' }, paymentStatusClass(v) { const s = String(v ?? ''); return s === '2' ? 'status-info' : s === '1' ? 'status-warn' : 'status-warn' }, deliveryStatusClass(v) { const s = String(v ?? ''); return s === '2' ? 'status-info' : s === '1' ? 'status-warn' : 'status-warn' },
    _computeLoadKey() { return JSON.stringify([this.currentDeptId, this.keyword, this.filters.customerType, this.filters.paymentStatus, this.filters.beginTime, this.filters.endTime, this.pageNum, this.pageSize]) },
    _resolveReturnTag(pid, currentTotal) {
      const refund = Number(this.returnMap[String(pid)] || 0);
      if (refund <= 0) return { text: '', cls: '' };
      const originalTotal = Number(currentTotal || 0) + refund;
      return refund + 0.005 >= originalTotal ? { text: '已全退', cls: 'status-danger' } : { text: '部分退', cls: 'status-warn' };
    },
    buildViewRow(row) {
      const r = { ...(row || {}) };
      const rt = this._resolveReturnTag(r.purchaseId, r.totalAmount);
      r._customerName = r.customerName || '未登记顾客';
      r._customerType = r.customerType;
      r._customerTypeText = this.customerTypeText(r.customerType);
      r._customerTypeClass = this.customerTypeClass(r.customerType);
      r._totalAmountText = '¥' + this.money(r.totalAmount);
      r._purchaseQuantityText = this.quantity(r.purchaseQuantity);
      r._giftQuantityText = this.quantity(r.giftQuantity);
      r._purchaseDateText = this.dateText(r.purchaseDate);
      r._paidAmountText = '¥' + this.money(r.paidAmount);
      r._receivableAmountText = '¥' + this.money(r.receivableAmount);
      r._showDebt = Number(r.receivableAmount) > 0;
      r._returnTagText = rt.text;
      r._returnTagClass = rt.cls;
      r._paymentStatusText = this.paymentStatusText(r.paymentStatus);
      r._paymentStatusClass = this.paymentStatusClass(r.paymentStatus);
      r._deliveryStatusText = this.deliveryStatusText(r.deliveryStatus);
      r._deliveryStatusClass = this.deliveryStatusClass(r.deliveryStatus);
      return r;
    },
    rebuildAllViewRows() { this.rows = this.rows.map(r => this.buildViewRow(r)) },
    async _onRowsUpdated(purchaseIds) {
      if (!Array.isArray(purchaseIds) || !purchaseIds.length) return;
      for (const id of purchaseIds) {
        const idx = this.rows.findIndex(row => String(row.purchaseId) === String(id));
        if (idx >= 0) {
          try {
            const res = await request({ url: `/member/purchase/${id}`, method: 'GET' });
            const updated = this.buildViewRow(res.data || res);
            this.rows.splice(idx, 1, updated);
          } catch (e) {}
        }
      }
      await this.loadReturns();
    },
    unwrap(res) { return res?.rows || res?.data?.rows || res?.data || [] }, async loadOptions() { try { const [p, a] = await Promise.all([request({ url:'/finance/product/list', method:'GET', data:{ pageNum:1, pageSize:200, deptId:this.currentDeptId }, silent:true }), request({ url:'/finance/accountingPeriod/list', method:'GET', data:{ pageNum:1, pageSize:200, deptId:this.currentDeptId }, silent:true })]); this.products = this.unwrap(p); this.periods = this.unwrap(a).filter(x => String(x.deptId) === String(this.currentDeptId)).map(x => ({ ...x, label: `${x.periodNo || x.periodId}（${this.dateText(x.startTime)} 至 ${x.endTime ? this.dateText(x.endTime) : '当前'}）` })); if (this.periods.length && !this.form.periodId) { this.form.periodId = this.periods[0].periodId } } catch (e) { this.products = []; this.periods = [] } }, async loadPolicies() { if (!this.form.item.productId || !this.form.periodId) { this.policies = []; return }; const res = await request({ url:'/member/campaign/policy/list', method:'GET', data:{ pageNum:1, pageSize:200, deptId:this.currentDeptId, productId:this.form.item.productId, periodId:this.form.periodId, status:'1' } }); const rows = this.unwrap(res).filter(x => String(x.productId) === String(this.form.item.productId) && String(x.periodId) === String(this.form.periodId)); const detailed = await Promise.all(rows.map(async (policy) => { if (Array.isArray(policy.packages) && policy.packages.length) return policy; try { const detail = await request({ url:`/member/campaign/policy/${policy.policyId}`, method:'GET' }); return detail?.data || detail || policy } catch (e) { return policy } })); this.policies = [{ policyId:'', policyName:'不参加活动，按单价购买', packages:[] }, ...detailed] }, async load() { this.loading = true; try { const res = await request({ url:'/member/purchase/list', method:'GET', data:this.listParams() }); const raw = this.unwrap(res); this.rows = raw.map(r => this.buildViewRow(r)); this.total = Number(res?.total ?? 0) || 0; this.purchaseIdSet = new Set(raw.map(r => String(r.purchaseId))); this.lastLoadKey = this._computeLoadKey() } finally { this.loading = false } this.loadStatistics(); if (this.pageNum === 1) this.loadReturns() }, listParams() { const p = { pageNum:this.pageNum, pageSize:this.pageSize, deptId:this.currentDeptId, customerName:this.keyword || undefined, customerType:this.filters.customerType || undefined, paymentStatus:this.filters.paymentStatus || undefined, beginTime:this.filters.beginTime || undefined, endTime:this.filters.endTime || undefined }; Object.keys(p).forEach(k => p[k] === undefined && delete p[k]); return p }, async loadStatistics() { try { const res = await request({ url:'/member/purchase/statistics', method:'GET', data:this.listParams() }); this.summary = res?.data || res || {} } catch (e) {} }, async loadReturns() { try { const idSet = this.purchaseIdSet; const filterByCurrentPage = idSet && idSet.size > 0; const map = {}; let pageNum = 1; const pageSize = 200; while (true) { const res = await request({ url:'/member/purchase-return/list', method:'GET', data:{ pageNum, pageSize, deptId:this.currentDeptId }, silent:true }); const rows = this.unwrap(res) || []; rows.forEach(r => { const pid = String(r.purchaseId); if (filterByCurrentPage && !idSet.has(pid)) return; const amt = Number(r.refundedAmount || 0); if (amt > 0) map[pid] = (map[pid] || 0) + amt }); const total = Number(res?.total ?? rows.length) || 0; if (pageNum * pageSize >= total || rows.length < pageSize) break; pageNum++ } this.returnMap = map; this.rebuildAllViewRows() } catch (e) { this.returnMap = {} } }, hasReturn(row) { return Number(this.returnMap[String(row?.purchaseId)] || 0) > 0 }, returnTagText(row) { const refund = Number(this.returnMap[String(row?.purchaseId)] || 0); if (refund <= 0) return ''; const currentTotal = Number(row?.totalAmount || 0); const originalTotal = currentTotal + refund; return refund + 0.005 >= originalTotal ? '已全退' : '部分退' }, returnTagClass(row) { const refund = Number(this.returnMap[String(row?.purchaseId)] || 0); if (refund <= 0) return ''; const currentTotal = Number(row?.totalAmount || 0); const originalTotal = currentTotal + refund; return refund + 0.005 >= originalTotal ? 'status-danger' : 'status-warn' },
    selectCustomerType(e) { this.form.customerType = this.customerTypes[Number(e.detail.value)].value; if (this.form.customerType !== 'MEMBER') { this.form.memberId = ''; this.form.memberNo = ''; this.form.memberName = '' } }, selectMember(member) { this.form.memberId = member?.memberId || ''; this.form.memberNo = member?.memberNo || ''; this.form.memberName = member?.memberName || ''; this.form.customerName = member?.memberName || ''; this.form.customerPhone = member?.phone || '' }, clearMember() { this.form.memberId = ''; this.form.memberNo = ''; this.form.memberName = '' }, selectCustomerTypeFilter(e) { this.filters.customerType = this.customerTypeFilters[Number(e.detail.value)].value }, selectPaymentStatusFilter(e) { this.filters.paymentStatus = this.paymentStatusFilters[Number(e.detail.value)].value }, openFilterSheet() { this.filterSheetOpen = true }, closeFilterSheet() { this.filterSheetOpen = false }, applyFilterSheet() { this.pageNum = 1; this.filterSheetOpen = false; this.load() }, resetFilters() { this.keyword = ''; this.filters = { customerType: '', paymentStatus: '', beginTime: '', endTime: '' }; this.pageNum = 1; this.load() }, prevPage() { if (this.pageNum <= 1) return; this.pageNum--; this.load() }, nextPage() { if (this.pageNum >= this.totalPages) return; this.pageNum++; this.load() }, selectProduct(e) { const p = this.products[Number(e.detail.value)]; this.form.item.productId = p?.productId || ''; this.form.item.policyId = ''; this.form.item.packageId = ''; this.form.item.giftQuantity = ''; if (p && (p.salePrice != null || p.defaultSalePrice != null || p.price != null)) { this.form.item.unitPrice = this.money(p.salePrice ?? p.defaultSalePrice ?? p.price) } this.loadPolicies() }, selectPolicy(e) { const policy = this.policies[Number(e.detail.value)]; this.form.item.policyId = policy?.policyId || ''; this.form.item.packageId = ''; if (policy?.packages?.length === 1) this.selectPackage({ detail: { value: 0 } }) }, selectPackage(e) { const pkg = this.packages[Number(e.detail.value)]; this.form.item.packageId = pkg?.packageId || ''; if (pkg) { this.form.item.purchaseQuantity = this.quantity(pkg.purchaseQuantity); this.form.item.giftQuantity = this.quantity(pkg.giftQuantity); this.form.item.unitPrice = this.money(Number(pkg.packagePrice || 0) / Number(pkg.purchaseQuantity || 1)) } }, selectPeriod(e) { this.form.periodId = this.periods[Number(e.detail.value)]?.periodId || ''; this.loadPolicies() }, limit(key, value, precision) { const s = String(value || '').replace(/[^\d.]/g,'').replace(/\.(?=.*\.)/g,''); this.form.item[key] = s.includes('.') ? s.split('.')[0] + '.' + s.split('.')[1].slice(0, precision) : s }, limitPayment(e) { this.paymentForm.paymentAmount = String(e.detail.value || '').replace(/[^\d.]/g,'').replace(/\.(?=.*\.)/g,'').replace(/(\.\d{2}).*/,'$1') },
    openCreate() { this.showCustomerTypePicker = true }, pickCustomerType(e) { const idx = Number(e.detail.value); this.form = this.emptyForm(); this.form.customerType = this.customerTypes[idx].value; if (this.periods.length) { this.form.periodId = this.periods[0].periodId } this.showCustomerTypePicker = false; this.panel = 'create' }, openReturn(row) { uni.navigateTo({ url:`/pages/member-purchase-return/index?purchaseId=${row.purchaseId}` }) }, openReturnDetail(row) { uni.navigateTo({ url:`/pages/member-purchase-return/index?purchaseId=${row.purchaseId}` }) }, openBind(row) { this.bindTarget = row; this.bindForm = { memberId: '' }; this.panel = 'bind' }, selectBindMember(member) { this.bindForm = { memberId: member?.memberId || '', memberName: member?.memberName || '' } }, clearBindMember() { this.bindForm = { memberId: '' } }, async confirmBind() { if (!this.bindForm.memberId) return uni.showToast({ title:'请选择要绑定的会员', icon:'none' }); await request({ url:`/member/purchase/${this.bindTarget.purchaseId}/bind-member/${this.bindForm.memberId}`, method:'PUT' }); uni.showToast({ title:'绑定成功', icon:'success' }); this.closePanel(); uni.$emit('memberPurchase:updated', [this.bindTarget.purchaseId]) }, async openDetail(row) { uni.navigateTo({ url: `/pages/member-purchase/detail?purchaseId=${row.purchaseId}` }) }, async openEdit(row) { uni.navigateTo({ url: `/pages/member-purchase/detail?purchaseId=${row.purchaseId}&autoEdit=1` }) }, switchToEdit() { this.panel = 'edit' }, closePanel() { this.panel = '' },
    async saveCreate() { const f = this.form; if (!f.periodId || !f.item.productId || Number(f.item.purchaseQuantity) <= 0 || Number(f.item.unitPrice) <= 0) return uni.showToast({ title:'请完整填写周期、商品、数量和单价', icon:'none' }); if (f.customerType === 'MEMBER' && !f.memberId) return uni.showToast({ title:'请选择会员', icon:'none' }); await request({ url:'/member/purchase', method:'POST', data:{ ...f, identityConfirmed:false, purchaseDate:f.purchaseDate, items:[{ ...f.item, giftQuantity:Number(f.item.giftQuantity || 0) }], idempotencyKey:`mp-purchase-${Date.now()}` } }); uni.showToast({ title:'购买单已保存', icon:'success' }); this.closePanel(); this.load() },
    async saveEdit() { await request({ url:`/member/purchase/${this.detail.purchaseId}`, method:'PUT', data:{ customerName:this.form.customerName, customerPhone:this.form.customerPhone, purchaseDate:this.form.purchaseDate, remark:this.form.remark, items:[this.form.item] } }); uni.showToast({ title:'购买单已保存', icon:'success' }); this.closePanel(); uni.$emit('memberPurchase:updated', [this.detail.purchaseId]) },
    async openPayment(row) { this.active = row; this.paymentForm = { paymentAmount: this.money(row.receivableAmount) }; this.paymentIndex = 0; this.panel = 'payment' }, async savePayment() { const n = Number(this.paymentForm.paymentAmount); if (!(n > 0) || n > Number(this.active.receivableAmount || 0)) return uni.showToast({ title:'收款金额必须大于0且不能超过待缴金额', icon:'none' }); await request({ url:`/member/purchase/${this.active.purchaseId}/payment`, method:'POST', data:{ paymentAmount:n, paymentMethod:this.paymentMethods[this.paymentIndex].value, paymentDate:this.isoDateTime(this.today()), idempotencyKey:`mp-payment-${Date.now()}` } }); uni.showToast({ title:'收款成功', icon:'success' }); this.closePanel(); uni.$emit('memberPurchase:updated', [this.active.purchaseId]) },
    async openDelivery(row) { const res = await request({ url:`/member/purchase/${row.purchaseId}`, method:'GET' }); const d = res.data || res; this.active = row; this.deliveryItems = (d.items || []).map(x => ({ ...x, label:`${x.productNameSnapshot || '商品'}（待领取${this.quantity(x.remainingQuantity)}）` })).filter(x => Number(x.remainingQuantity || 0) > 0); this.deliveryIndex = 0; const first = this.deliveryItems[0]; this.deliveryForm = { saleDeliveryQuantity:this.quantity(Math.max(0, Number(first?.purchaseQuantity || 0) - Number(first?.deliveredSaleQuantity || 0))), giftDeliveryQuantity:this.quantity(Math.max(0, Number(first?.giftQuantity || 0) - Number(first?.deliveredGiftQuantity || 0))), receiverName:d.customerName || '' }; this.panel = 'delivery' }, selectDeliveryItem(e) { this.deliveryIndex = Number(e.detail.value); const item = this.deliveryItems[this.deliveryIndex]; this.deliveryForm.saleDeliveryQuantity = this.quantity(Math.max(0, Number(item?.purchaseQuantity || 0) - Number(item?.deliveredSaleQuantity || 0))); this.deliveryForm.giftDeliveryQuantity = this.quantity(Math.max(0, Number(item?.giftQuantity || 0) - Number(item?.deliveredGiftQuantity || 0))) }, async saveDelivery() { const item = this.deliveryItems[this.deliveryIndex]; const sale = Number(this.deliveryForm.saleDeliveryQuantity || 0); const gift = Number(this.deliveryForm.giftDeliveryQuantity || 0); const saleRemaining = Number(item?.purchaseQuantity || 0) - Number(item?.deliveredSaleQuantity || 0); const giftRemaining = Number(item?.giftQuantity || 0) - Number(item?.deliveredGiftQuantity || 0); if (!item || sale + gift <= 0 || sale > saleRemaining || gift > giftRemaining) return uni.showToast({ title:'领取数量不能超过剩余可领取数量', icon:'none' }); await request({ url:`/member/purchase/${this.active.purchaseId}/delivery`, method:'POST', data:{ itemId:item.itemId, saleDeliveryQuantity:sale, giftDeliveryQuantity:gift, totalDeliveryQuantity:sale+gift, deliveryDate:this.isoDateTime(this.today()), receiverName:this.deliveryForm.receiverName, idempotencyKey:`mp-delivery-${Date.now()}` } }); uni.showToast({ title:'领取成功', icon:'success' }); this.closePanel(); uni.$emit('memberPurchase:updated', [this.active.purchaseId]) },
    async cancel(row) { const ok = await new Promise(resolve => uni.showModal({ title:'确认作废', content:`确认作废购买单 ${row.purchaseNo}？`, success:r=>resolve(r.confirm) })); if (!ok) return; await request({ url:`/member/purchase/${row.purchaseId}/cancel`, method:'PUT' }); uni.showToast({ title:'购买单已作废', icon:'success' }); uni.$emit('memberPurchase:updated', [row.purchaseId]) }
  }
}
</script>

<style scoped>
/* ──────────────────────────────────────────────
 * 通用业务页皮肤：与销售记录/库存流水保持一致
 * 避免使用首页式大面积 KPI 英雄区 / 独立卡片盒
 * ────────────────────────────────────────────── */
.page{display:flex;flex-direction:column;height:100vh;width:100vw;max-width:750rpx;margin:0 auto;background:#e7eff7;color:#1e293b;box-sizing:border-box;overflow:hidden}

/* ── 顶部标题栏（左边框 + 浅蓝渐变） ── */
.hero{margin:22rpx 30rpx 0;padding:28rpx 30rpx 30rpx;border-left:5rpx solid #1687f5;border-radius:20rpx;background:linear-gradient(110deg,#d9eaff,#f7faff);box-shadow:0 8rpx 22rpx rgba(46,82,120,.08)}
.eyebrow{display:block;color:#1687f5;font-size:24rpx;font-weight:600}
.hero-title{display:block;margin-top:10rpx;color:#1e293b;font-size:38rpx;font-weight:700}

/* ── 部门范围条 ── */
.work-scope{display:flex;align-items:center;margin:8rpx 30rpx 0;min-height:44rpx;padding:6rpx 0}
.work-scope-hover{background:#eaf3ff;border-radius:8rpx}
.work-scope-mark{width:14rpx;height:14rpx;margin-right:14rpx;border-radius:50%;background:#087CF0}
.work-scope-mark-disabled{background:#087CF0}
.work-scope-copy{display:flex;align-items:baseline;gap:8rpx;color:#708196;font-size:24rpx}
.work-scope-name{color:#1F2937;font-size:28rpx;font-weight:700}

/* ── 通用卡片容器（section-card） ── */
.section-card{background:#fff;border-radius:20rpx;padding:28rpx;margin-top:24rpx;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);box-sizing:border-box;overflow:hidden}
.section-header{display:flex;align-items:center;gap:12rpx;margin-bottom:18rpx}
.section-dot{width:12rpx;height:12rpx;border-radius:50%;flex-shrink:0}
.section-title{font-size:28rpx;font-weight:700;color:#1A2332;flex:1}
.section-link{font-size:22rpx;color:#94A3B8}

/* ── 列表顶部条（计数 + 筛选浮动按钮） ── */
.list-header-bar{display:flex;align-items:center;justify-content:space-between;margin:8rpx 30rpx 0;gap:16rpx}
.list-header-count{font-size:24rpx;color:#708196;font-weight:600}
.filter-fab{display:flex;align-items:center;gap:8rpx;height:64rpx;line-height:64rpx;padding:0 24rpx;border:0;border-radius:999rpx;background:linear-gradient(135deg,#C65A4A,#F2A88D);color:#fff;font-size:24rpx;font-weight:700;box-shadow:0 4rpx 14rpx rgba(198,90,74,.25);position:relative}
.filter-fab::after{border:none}
.filter-fab-icon{font-size:28rpx;font-weight:800}
.filter-fab-badge{min-width:32rpx;height:32rpx;line-height:32rpx;text-align:center;padding:0 8rpx;border-radius:999rpx;background:#fff;color:#C65A4A;font-size:20rpx;font-weight:800}

/* ── 顶部统计条：一行4格，数字加粗文字不加粗 ── */
.summary-bar{display:flex;margin:8rpx 30rpx 0;padding:20rpx 4rpx;background:#fff;border-radius:18rpx;border:1rpx solid #dbe6f1;box-sizing:border-box}
.summary-bar>view{flex:1;text-align:center;border-right:1rpx solid #edf1f5;min-width:0}
.summary-bar>view:last-child{border-right:0}
.summary-value{display:block;font-size:30rpx;font-weight:800;font-variant-numeric:tabular-nums;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;color:#102A3A}
.summary-value.primary{color:#C65A4A}
.summary-value.success{color:#087CF0}
.summary-value.warning{color:#B45309}
.summary-gift{display:inline-block;margin-left:8rpx;font-size:22rpx;font-weight:400;color:#64748B}
.summary-gift-num{font-size:30rpx;font-weight:800;color:#102A3A}
.summary-currency{font-size:24rpx;font-weight:400;margin-right:2rpx}
.summary-label{display:block;margin-top:6rpx;color:#98a9ba;font-size:20rpx;font-weight:400}

/* ── 浮动底部操作栏 ── */
.bottom-bar{position:fixed;left:0;right:0;bottom:0;display:flex;justify-content:center;gap:16rpx;padding:20rpx 24rpx;padding-bottom:calc(20rpx + env(safe-area-inset-bottom));background:rgba(255,255,255,0.96);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border-top:1rpx solid #E2E8F0;z-index:10}
.bottom-bar .add-button{width:320rpx;height:84rpx;line-height:84rpx;background:linear-gradient(135deg,#087CF0,#5AA9E8);color:#FFFFFF;font-size:28rpx;border-radius:999rpx;text-align:center;box-shadow:0 6rpx 20rpx rgba(8,124,240,.25);border:0;padding:0}
.bottom-bar .add-button::after{border:none}
.scroll{padding-bottom:160rpx!important}

/* ── 操作行（旧，保留兼容） ── */
.actions-row{display:flex;gap:14rpx;margin:18rpx 30rpx 0}
.action-primary,.action-ghost{border:0;border-radius:32rpx;padding:0 24rpx;height:64rpx;line-height:64rpx;font-size:24rpx}
.action-primary{background:#087CF0;color:#fff}
.action-ghost{background:#EEF3F8;color:#334155}

/* ── 滚动列表区 ── */
.scroll{flex:1;width:100%;min-height:0;padding:0 30rpx 160rpx!important;box-sizing:border-box;overflow-x:hidden}
.list-card{margin-top:16rpx!important;padding:20rpx 28rpx!important}
.state-card{padding:20rpx 28rpx 28rpx!important}

/* ── 记录卡片标准样式（record-card，参考费用记录配色） ── */
.record-card{display:flex;margin-bottom:18rpx;background:#FFFFFF;border-radius:22rpx;border:1rpx solid rgba(226,232,240,.9);box-shadow:0 8rpx 26rpx rgba(8,124,240,.06);overflow:hidden}
.card-bar{width:6rpx;background:linear-gradient(180deg,#C65A4A,#F2A88D);flex-shrink:0}
.card-body{flex:1;padding:24rpx 28rpx}
.card-header{display:flex;align-items:flex-start;justify-content:space-between;gap:20rpx}
.record-title{flex:1;font-size:30rpx;line-height:42rpx;font-weight:700;color:#1A2332}
.record-id{padding:4rpx 14rpx;background:#E8EEF5;color:#5A6B7F;font-size:20rpx;border-radius:999rpx;flex-shrink:0}
.summary-grid{display:grid;grid-template-columns:1fr 1fr;gap:12rpx;margin-top:16rpx}
.summary-item{display:flex;flex-direction:column;gap:6rpx}
.summary-label{font-size:22rpx;color:#94A3B8}
.record-summary-value{font-size:26rpx;color:#1A2332;font-weight:500}
.record-summary-value.tone-money{color:#B45309;font-weight:700}
.record-summary-value.tone-points{color:#087CF0;font-weight:700}
.card-footer{display:flex;justify-content:space-between;align-items:center;margin-top:16rpx;padding-top:14rpx;border-top:1rpx solid #E8EEF5}
.meta-text{font-size:24rpx;color:#94A3B8}
.arrow-icon{font-size:36rpx;color:#CBD5E1;font-weight:300}

/* ── 紧凑3行卡片样式（参考费用记录两行模式配色） ── */
.compact-body{flex:1;padding:22rpx 24rpx}
.compact-row1{display:flex;align-items:center;gap:18rpx}
.compact-title-wrap{flex:1;min-width:0;display:flex;align-items:center;gap:12rpx}
.compact-title{flex:1;min-width:0;font-size:32rpx;line-height:44rpx;font-weight:800;color:#102A3A;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.compact-type-tag{flex-shrink:0;padding:2rpx 12rpx;border-radius:999rpx;font-size:20rpx;line-height:30rpx;font-weight:700}
.compact-type-tag.type-member{background:#E0F2FE;color:#075985}
.compact-type-tag.type-customer{background:#F1F5F9;color:#475569}
.compact-type-tag.type-walkin{background:#FFEDD5;color:#C2410C}
.compact-amount{font-size:32rpx;line-height:44rpx;font-weight:800;color:#C65A4A;flex-shrink:0}
.compact-row2{display:flex;align-items:center;gap:18rpx;margin-top:12rpx}
.compact-row3{display:flex;align-items:center;gap:14rpx;margin-top:12rpx}
.compact-meta{flex-shrink:0;font-size:23rpx;line-height:32rpx;color:#708196;font-weight:600}
.compact-meta.date{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-align:right}
.compact-meta.paid{color:#087CF0;font-weight:700}
.compact-meta.debt{color:#B45309;font-weight:700}
.compact-meta.qty{font-size:24rpx;font-weight:600;color:#475569}
.compact-id{flex-shrink:0;font-size:21rpx;color:#5A6B7F;background:#E8EEF5;padding:2rpx 12rpx;border-radius:999rpx}
.compact-status-group{display:flex;align-items:center;gap:8rpx;flex-shrink:0;margin-left:auto}
.compact-status{flex-shrink:0;padding:4rpx 16rpx;border-radius:20rpx;font-size:22rpx;line-height:30rpx;font-weight:500}
.compact-status.status-ok{background:#E0F2FE;color:#075985}
.compact-status.status-warn{background:#FEF3C7;color:#92400E}
.compact-status.status-info{background:#E0F2FE;color:#075985}
.compact-status.status-danger{background:#FEE2E2;color:#991B1B}

/* ── 空状态 / 分页 ── */
.empty{text-align:center;color:#94a3b8;padding:56rpx 0;font-size:23rpx}
.pagination{display:flex;align-items:center;justify-content:center;gap:18rpx;padding:24rpx 0 0;color:#64748b;font-size:24rpx}
.pagination button{border:0;border-radius:12rpx;padding:10rpx 28rpx;background:#eef3f8;color:#334155;margin:0}
.pagination button[disabled]{opacity:.5}
.page-info{min-width:140rpx;text-align:center}

/* ── 弹窗面板 ── */
.mask{position:fixed;inset:0;background:rgba(15,23,42,.45);z-index:20;padding:40rpx 24rpx;overflow:auto;box-sizing:border-box}
.panel{background:#fff;border-radius:20rpx;padding:24rpx;margin:0 auto;max-height:calc(100vh - 80rpx);overflow:auto;border:1rpx solid #D5E0EC;box-shadow:0 10rpx 30rpx rgba(15,23,42,.12);box-sizing:border-box}
.panel-title{font-size:34rpx;font-weight:700;margin-bottom:20rpx;color:#1A2332}
.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:18rpx;box-sizing:border-box}
.field{margin-top:16rpx;width:100%;box-sizing:border-box}
.field.full{grid-column:1/-1}
.field text{display:block;color:#475569;font-size:24rpx;margin-bottom:8rpx}
.field input,.field textarea{box-sizing:border-box!important;width:100%;padding:16rpx 18rpx;border:1rpx solid #D5E0EC;border-radius:12rpx;background:#fff;color:#334155;font-size:24rpx;min-height:64rpx;line-height:32rpx}
.field textarea{min-height:120rpx;line-height:1.5}
.control,.readonly{box-sizing:border-box!important;width:100%;padding:16rpx 18rpx;border:1rpx solid #D5E0EC;border-radius:12rpx;color:#334155;background:#F8FBFD;min-height:64rpx;line-height:32rpx}
.form-section{padding:22rpx 0;border-bottom:1rpx solid #edf1f6}
.form-section:first-child{padding-top:0}
.form-section:last-of-type{border-bottom:0;padding-bottom:0}
.section-title{margin-top:24rpx;font-weight:700;color:#1e293b;font-size:26rpx}
.item-box{padding:18rpx;border:1rpx solid #e4ebf3;border-radius:14rpx;box-sizing:border-box;margin-top:12rpx}
.wide{width:100%;margin-top:22rpx;border:0;border-radius:12rpx;padding:0 24rpx;height:72rpx;line-height:72rpx;background:#087CF0;color:#fff;font-size:26rpx}
.history{border-top:1rpx solid #edf1f6;margin-top:24rpx}
.history-row{padding:14rpx 0;color:#64748b;font-size:23rpx;border-bottom:1rpx solid #f0f3f7;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}

/* ── 会员查找组件区（兼容现有 MemberSearch 样式） ── */
.member-search-section{margin-top:20rpx;padding:20rpx;background:#f8fbff;border:1rpx solid #e1eaf5;border-radius:14rpx;box-sizing:border-box}
.member-search-header{display:flex;align-items:center;justify-content:space-between}
.member-search-hint{font-size:22rpx;color:#94a3b8}
.member-search-row{display:flex;gap:12rpx;margin-top:14rpx;box-sizing:border-box}
.member-search-input{flex:1;min-width:0;padding:16rpx 18rpx;border:1rpx solid #dbe4ef;border-radius:12rpx;background:#fff;box-sizing:border-box!important;height:64rpx;line-height:32rpx}
.member-search-btn{flex:0 0 120rpx;margin:0;padding:0;border-radius:12rpx;background:#087cf0;color:#fff;font-size:24rpx;height:64rpx;line-height:64rpx}
.selected-member{display:flex;align-items:center;justify-content:space-between;margin-top:14rpx;padding:16rpx;border-radius:12rpx;background:#edf6ff;box-sizing:border-box}
.selected-member-name,.member-result-name{display:block;color:#1e293b;font-weight:600}
.selected-member-no,.member-result-no{display:block;margin-top:5rpx;color:#64748b;font-size:22rpx}
.selected-member-clear{color:#087cf0;font-size:23rpx}
.member-result-list{margin-top:12rpx;background:#fff;border-radius:12rpx}
.member-result{display:flex;align-items:center;justify-content:space-between;padding:16rpx;border-bottom:1rpx solid #eef2f7;box-sizing:border-box}
.member-result-arrow{color:#94a3b8;font-size:34rpx}
.member-result-empty{padding:18rpx 0;color:#94a3b8;font-size:23rpx;text-align:center}

/* ════════════════════════════════════════════════
 * 详情/表单/底部弹出面板样式
 * 参考销售记录 detail/index.vue 与 form/index.vue
 * ════════════════════════════════════════════════ */

/* ── 全屏遮罩（详情页/表单页） ── */
.overlay-mask{position:fixed;inset:0;background:#1a2332;z-index:50;overflow:hidden}

/* ────────────────── 详情页 ────────────────── */
.detail-page{height:100vh;background:#E8EEF5;padding-bottom:calc(40rpx + env(safe-area-inset-bottom));box-sizing:border-box;overflow-y:auto;-webkit-overflow-scrolling:touch;will-change:transform}
.detail-page.swipe-closing{pointer-events:none}

/* 详情页 hero 区（参考 hero-card） */
.detail-hero{position:relative;margin:24rpx 28rpx;border-radius:20rpx;overflow:hidden}
.detail-hero-bg{position:absolute;inset:0;background:linear-gradient(135deg,#087CF0,#5AA9E8,#A8C7E5);border-radius:20rpx}
.detail-hero-content{position:relative;z-index:1;padding:40rpx 36rpx}
.detail-hero-eyebrow{font-size:22rpx;color:rgba(255,255,255,.7);margin-bottom:12rpx;letter-spacing:2rpx}
.detail-hero-title{font-size:36rpx;font-weight:600;color:#fff;margin-bottom:16rpx;line-height:1.4;overflow-wrap:anywhere;word-break:break-word}
.detail-hero-value{font-size:52rpx;font-weight:700;color:#fff;margin-bottom:12rpx}
.detail-hero-meta{font-size:24rpx;color:rgba(255,255,255,.7)}
.detail-close{position:absolute;top:24rpx;right:24rpx;width:56rpx;height:56rpx;line-height:56rpx;text-align:center;border-radius:50%;background:rgba(0,0,0,.2);color:#fff;font-size:30rpx;z-index:2}

/* 详情页 section 区 */
.detail-section{margin:20rpx 28rpx 0;background:#fff;border-radius:20rpx;padding:28rpx 32rpx;box-shadow:0 2rpx 16rpx rgba(8,124,240,.06);box-sizing:border-box}
.detail-section-title{font-size:28rpx;font-weight:600;color:#1A2332;margin-bottom:20rpx;padding-left:16rpx;border-left:4rpx solid #087CF0}

/* 详情页 高亮网格（参考 highlight-grid） */
.detail-highlight-grid{display:flex;flex-wrap:wrap;gap:12rpx}
.detail-highlight-item{flex:1;min-width:45%;background:#F5F8FA;border-radius:12rpx;padding:18rpx 20rpx;box-sizing:border-box}
.detail-highlight-label{font-size:22rpx;color:#94A3B8;margin-bottom:6rpx}
.detail-highlight-value{font-size:28rpx;font-weight:600;color:#1A2332;overflow-wrap:anywhere;word-break:break-word}

/* 详情页 状态标签 */
.detail-highlight-value.status-ok{display:inline-block;background:#D1FAE5;color:#065F46;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.status-warn{display:inline-block;background:#FEF3C7;color:#92400E;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.status-info{display:inline-block;background:#E0F2FE;color:#075985;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.tone-money{color:#B45309}
.detail-highlight-value.tone-success{color:#087CF0}
.detail-highlight-value.tone-warning{color:#C26A1B}

/* 详情页 明细列表（参考 detail-list） */
.detail-item{background:#F5F8FA;border-radius:16rpx;padding:24rpx;margin-bottom:20rpx;box-sizing:border-box}
.detail-item:last-child{margin-bottom:0}
.detail-item-header{display:flex;align-items:center;justify-content:space-between;padding-bottom:16rpx;border-bottom:1rpx solid #E2E8F0;margin-bottom:16rpx}
.detail-item-title{font-size:28rpx;font-weight:700;color:#1A2332;overflow-wrap:anywhere;word-break:break-word}
.detail-row{display:flex;align-items:center;min-height:72rpx;padding:16rpx 0;box-sizing:border-box;line-height:34rpx}
.detail-label{width:140rpx;font-size:26rpx;color:#64748B;flex-shrink:0}
.detail-value-text{font-size:26rpx;color:#1A2332;flex:1;overflow-wrap:anywhere;word-break:break-word}
.detail-value-text.amount{font-weight:700;color:#087CF0}

/* 详情页 汇总 */
.detail-summary{margin-top:24rpx;padding-top:24rpx;border-top:2rpx solid #E2E8F0}
.detail-summary-row{display:flex;align-items:center;padding:12rpx 0}
.detail-summary-label{flex:1;font-size:28rpx;color:#1A2332;font-weight:500}
.detail-summary-value{font-size:32rpx;color:#087CF0;font-weight:700}

/* 详情页 备注 */
.detail-remark{font-size:26rpx;color:#475569;line-height:1.6;white-space:pre-wrap;word-break:break-word;background:#F5F8FA;border-radius:12rpx;padding:20rpx}

/* 详情页 收款/领取记录（参考 payment-history） */
.payment-history-item{padding:20rpx 0;border-bottom:1rpx solid #E2E8F0}
.payment-history-item:last-child{border-bottom:0}
.payment-history-main,.payment-history-meta{display:flex;align-items:center;justify-content:space-between;gap:16rpx}
.payment-history-no,.payment-history-meta{overflow-wrap:anywhere;word-break:break-word}
.payment-history-no{color:#1A2332;font-size:27rpx;font-weight:600}
.payment-history-amount{color:#087CF0;font-size:30rpx;font-weight:700;flex-shrink:0}
.payment-history-meta{margin-top:10rpx;color:#64748B;font-size:24rpx}
.detail-empty{text-align:center;color:#94A3B8;padding:32rpx 0;font-size:24rpx}

/* 详情页 底部占位（在 scroll 内给 footer-bar 留空间） */
.detail-footer-placeholder{height:140rpx}
/* 详情页 底部操作栏（参考 footer-bar） */
.detail-footer-bar{position:fixed;left:0;right:0;bottom:0;display:flex;align-items:center;justify-content:center;gap:16rpx;padding:16rpx 24rpx;padding-bottom:calc(16rpx + env(safe-area-inset-bottom));background:#fff;box-shadow:0 -2rpx 16rpx rgba(8,124,240,.06);z-index:100;flex-wrap:wrap}
.detail-action-btn{flex:1;height:76rpx;line-height:76rpx;font-size:28rpx;font-weight:500;border-radius:16rpx;text-align:center;border:none;margin:0;padding:0;min-width:calc(25% - 12rpx)}
.detail-action-btn::after{border:none}
.detail-action-btn.primary-btn{background:#087CF0;color:#fff}
.detail-action-btn.payment-btn{background:linear-gradient(135deg,#EA580C,#F59E0B);color:#fff;font-weight:600}
.detail-action-btn.delivery-btn{background:linear-gradient(135deg,#0284C7,#38BDF8);color:#fff;font-weight:600}
.detail-action-btn.bind-btn{background:linear-gradient(135deg,#7C3AED,#A855F7);color:#fff;font-weight:600}
.detail-action-btn.danger-btn{background:linear-gradient(135deg,#DC2626,#F87171);color:#fff;font-weight:600}

/* ────────────────── 表单页 ────────────────── */
.form-page{height:100vh;background:#E8EEF5;padding-bottom:calc(40rpx + env(safe-area-inset-bottom));box-sizing:border-box;overflow-y:auto;-webkit-overflow-scrolling:touch}

/* 表单页 hero 区（参考 form hero-card） */
.form-hero{display:flex;align-items:center;gap:24rpx;padding:36rpx 28rpx;background:linear-gradient(135deg,#C7DCF2 0%,#EAF3FC 100%);border-top:1rpx solid #B7D1EB;border-radius:0 0 24rpx 24rpx;position:relative}
.form-hero-icon{width:72rpx;height:72rpx;line-height:72rpx;text-align:center;background:rgba(255,255,255,.15);border-radius:18rpx;font-size:36rpx;color:#fff;flex-shrink:0;display:flex;align-items:center;justify-content:center}
.form-hero-info{flex:1;min-width:0}
.form-hero-title{font-size:34rpx;font-weight:700;color:#1F2D3D}
.form-hero-meta{margin-top:6rpx;font-size:22rpx;color:#6E8197}
.form-hero-close{position:absolute;top:24rpx;right:24rpx;width:56rpx;height:56rpx;line-height:56rpx;text-align:center;border-radius:50%;background:rgba(255,255,255,.3);color:#5A6B7F;font-size:30rpx}

/* 表单页 section 卡片（参考 section-card） */
.form-section-card{margin:20rpx 28rpx 0;padding:28rpx;background:#fff;border-radius:20rpx;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);box-sizing:border-box}
.form-section-card.member-search-card{border:1rpx solid #FED7AA;box-shadow:0 10rpx 30rpx rgba(234,88,12,.08)}
.form-section-header{display:flex;align-items:center;gap:10rpx;margin-bottom:20rpx}
.form-section-header.collapsible{padding:4rpx 0}
.form-section-dot{width:8rpx;height:8rpx;border-radius:50%;background:#087CF0;flex-shrink:0}
.form-section-dot.required{background:#EF4444}
.form-section-title{font-size:28rpx;font-weight:600;color:#1A2332;flex:1}
.form-section-count{margin-left:auto;font-size:22rpx;color:#94A3B8}
.form-collapse-arrow{font-size:32rpx;color:#94A3B8;transform:rotate(90deg);transition:transform .2s;margin-left:8rpx}
.form-collapse-arrow.collapsed{transform:rotate(0deg)}

/* 表单项（参考 form-item） */
.form-item{padding-top:20rpx;border-top:1rpx solid #E8EEF5}
.form-item + .form-item{border-top:1rpx solid #E8EEF5;padding-top:20rpx}
.form-item:first-child{border-top:0;padding-top:0}
.form-label-row{display:flex;align-items:center;gap:8rpx;margin-bottom:12rpx}
.form-label{font-size:24rpx;color:#5A6B7F;font-weight:500}
.form-required-tag{color:#EF4444;font-size:26rpx;font-weight:700}

/* 表单控件（参考 control，修复夺词典） */
.form-control{width:100%;min-height:84rpx;padding:0 24rpx;background:#F5F8FA;border:2rpx solid #E2E8F0;border-radius:14rpx;font-size:28rpx;color:#1A2332;box-sizing:border-box!important;transition:border-color .2s}
.form-control.input{display:block;width:100%;height:84rpx;line-height:84rpx}
.form-control.textarea{height:170rpx;padding-top:20rpx;line-height:1.5}
.form-control.picker{display:flex;align-items:center;justify-content:space-between;line-height:84rpx}
.form-picker-text{flex:1;color:#1A2332;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.form-control.picker:not(.has-value) .form-picker-text{color:#94A3B8}
.form-picker-arrow{font-size:32rpx;color:#CBD5E1;font-weight:300;margin-left:8rpx;flex-shrink:0}

/* 表单两列网格 */
.form-grid-2col{display:grid;grid-template-columns:1fr 1fr;gap:18rpx;box-sizing:border-box}
.form-grid-2col .form-item{border-top:0!important;padding-top:0!important}

/* 表单页 底部操作栏（参考 footer） */
.form-footer-placeholder{height:140rpx}
.form-footer{position:fixed;left:0;right:0;bottom:0;display:flex;gap:16rpx;padding:18rpx 24rpx;padding-bottom:calc(18rpx + env(safe-area-inset-bottom));background:rgba(255,255,255,.96);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border-top:1rpx solid #E2E8F0;z-index:100}
.form-btn-primary,.form-btn-secondary{flex:1;height:88rpx;line-height:88rpx;font-size:28rpx;border-radius:999rpx;text-align:center;border:none;margin:0;padding:0}
.form-btn-primary{background:linear-gradient(135deg,#087CF0,#5AA9E8);color:#fff;box-shadow:0 6rpx 20rpx rgba(8,124,240,.25)}
.form-btn-secondary{background:#fff;color:#5A6B7F;border:1rpx solid #E2E8F0}
.form-btn-primary::after,.form-btn-secondary::after{border:none}
.form-btn-primary[disabled]{opacity:.5}

/* ────────────────── 底部弹出面板（收款/领取/绑定） ────────────────── */
.sheet-mask{position:fixed;left:0;right:0;top:0;bottom:0;z-index:200;display:flex;align-items:flex-end;background:rgba(15,23,42,.45)}
.sheet-panel{width:100%;max-height:88vh;overflow-y:auto;padding:30rpx 28rpx calc(30rpx + env(safe-area-inset-bottom));border-radius:28rpx 28rpx 0 0;background:#fff;box-sizing:border-box;-webkit-overflow-scrolling:touch}
.sheet-title{font-size:34rpx;font-weight:800;color:#1A2332;margin-bottom:16rpx}
.sheet-sub{margin-top:8rpx;font-size:24rpx;color:#708196;margin-bottom:16rpx}

/* 弹出面板 汇总区（参考 payment-summary） */
.sheet-summary{margin:8rpx 0 16rpx;padding:20rpx 24rpx;border-radius:16rpx;background:#F6F9FC}
.sheet-summary-row{display:flex;justify-content:space-between;padding:10rpx 0;color:#5A6B7F;font-size:25rpx;gap:16rpx}
.sheet-summary-row text:last-child{font-weight:600;color:#1A2332;text-align:right;overflow-wrap:anywhere;word-break:break-word}
.sheet-summary-row.remaining text:last-child{color:#C26A1B}

/* 弹出面板 表单行（参考 payment-row-stack） */
.sheet-row{display:flex;align-items:center;padding:18rpx 0;border-bottom:1rpx solid #E8EEF5}
.sheet-row:last-of-type{border-bottom:0}
.sheet-row-stack{flex-direction:column;align-items:stretch;padding:20rpx 0}
.sheet-label{font-size:24rpx;color:#5A6B7F;margin-bottom:12rpx}
.sheet-row-stack .sheet-label{width:auto;margin-bottom:12rpx}
.sheet-input{width:100%;box-sizing:border-box!important;text-align:left;padding:16rpx 20rpx;border:1rpx solid #E2E8F0;border-radius:12rpx;background:#F8FAFC;font-size:30rpx;min-height:72rpx;line-height:40rpx;color:#1A2332}
.sheet-picker{text-align:left;justify-content:space-between;padding:16rpx 20rpx;border:1rpx solid #E2E8F0;border-radius:12rpx;background:#F8FAFC;font-size:30rpx;min-height:72rpx;display:flex;align-items:center;color:#1A2332}
.sheet-picker-arrow{margin-left:8rpx;color:#CBD5E1;font-size:22rpx;flex-shrink:0}
.sheet-date-row{display:flex;align-items:center;gap:12rpx}
.sheet-date-row picker{flex:1;min-width:0}
.sheet-picker-date{text-align:center;justify-content:center;padding:16rpx 8rpx;font-size:26rpx;min-height:72rpx}
.sheet-date-sep{flex-shrink:0;color:#94A3B8;font-size:22rpx}
.sheet-grid-2col{display:grid;grid-template-columns:1fr 1fr;gap:14rpx;box-sizing:border-box}

/* 弹出面板 底部按钮（参考 claim-panel-actions） */
.sheet-actions{display:flex;gap:16rpx;margin-top:22rpx;padding-top:8rpx}
.sheet-cancel,.sheet-confirm{flex:1;height:78rpx;line-height:78rpx;border-radius:999rpx;font-size:26rpx;text-align:center;border:none;margin:0;padding:0}
.sheet-cancel{background:#F1F5F9;color:#475569}
.sheet-confirm{background:#EA580C;color:#fff}
.sheet-cancel::after,.sheet-confirm::after{border:none}
.sheet-confirm[disabled]{opacity:.5}

/* ── 顾客类型选择列表 ── */
.customer-type-list{margin:8rpx 0 16rpx}
.customer-type-item{display:flex;align-items:center;justify-content:space-between;padding:28rpx 20rpx;border-bottom:1rpx solid #E8EEF5}
.customer-type-item:last-child{border-bottom:0}
.customer-type-label{font-size:30rpx;font-weight:600;color:#1A2332}
.customer-type-arrow{color:#CBD5E1;font-size:34rpx}
</style>
