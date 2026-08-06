<template>
  <view class="page" v-if="authorized">
    <view class="hero"><text class="eyebrow">会员服务</text><text class="hero-title">购买记录</text></view>
    <view class="work-scope"><view class="work-scope-mark"></view><view class="work-scope-copy"><text class="work-scope-label">当前部门 · </text><text class="work-scope-name">{{ currentDeptName || '当前部门' }}</text></view></view>
    <view class="section-card filters-card">
      <view class="section-header"><view class="section-dot" style="background:#087CF0"></view><text class="section-title">筛选购买</text><text class="section-link">共 {{ total }} 条</text></view>
      <view class="filter-row filter-row-tools">
        <input class="filter-kw" v-model="keyword" placeholder="顾客姓名" confirm-type="search" @confirm="load" />
        <button class="filter-button" @tap="load">查询</button>
        <button class="filter-button filter-button-ghost" @tap="toggleMoreFilters">{{ showMoreFilters ? '收起' : '更多' }}</button>
      </view>
      <block v-if="showMoreFilters">
        <view class="filter-row">
          <picker class="filter-type-picker" :range="customerTypeFilters" range-key="label" :value="customerTypeFilterIndex" @change="selectCustomerTypeFilter"><view class="filter-picker">{{ customerTypeFilters[customerTypeFilterIndex]?.label || '全部顾客类型' }}<text class="filter-chevron">⌄</text></view></picker>
          <picker class="filter-type-picker" :range="paymentStatusFilters" range-key="label" :value="paymentStatusFilterIndex" @change="selectPaymentStatusFilter"><view class="filter-picker">{{ paymentStatusFilters[paymentStatusFilterIndex]?.label || '全部收款状态' }}<text class="filter-chevron">⌄</text></view></picker>
        </view>
        <view class="filter-row">
          <view class="date-controls"><picker mode="date" :value="filters.beginTime" @change="filters.beginTime=$event.detail.value"><view class="filter-date">{{ filters.beginTime || '开始日期' }}</view></picker><text class="date-separator">至</text><picker mode="date" :value="filters.endTime" @change="filters.endTime=$event.detail.value"><view class="filter-date">{{ filters.endTime || '结束日期' }}</view></picker></view>
        </view>
        <view class="filter-row" style="justify-content:flex-end">
          <button class="filter-button filter-button-ghost" @tap="resetFilters">重置</button>
        </view>
      </block>
    </view>
    <view class="summary-bar" v-if="summary"><view><text class="summary-value">{{ summary.purchaseOrderCount || 0 }}</text><text class="summary-label">购买单数</text></view><view><text class="summary-value">{{ quantity(summary.purchaseQuantity) }}</text><text class="summary-label">购买数量</text></view><view><text class="summary-value">{{ quantity(summary.giftQuantity) }}</text><text class="summary-label">赠送数量</text></view></view>
    <view class="summary-bar summary-bar-secondary" v-if="summary"><view><text class="summary-value primary">¥{{ money(summary.totalAmount) }}</text><text class="summary-label">应收金额</text></view><view><text class="summary-value success">¥{{ money(summary.paidAmount) }}</text><text class="summary-label">已收金额</text></view><view><text class="summary-value warning">¥{{ money(summary.receivableAmount) }}</text><text class="summary-label">待缴金额</text></view></view>
    <view class="scroll-pad"></view>
    <view class="bottom-bar">
      <button v-if="can('add')" class="add-button" @tap="openCreate">＋ 新增</button>
    </view>
    <scroll-view scroll-y class="scroll" @scrolltolower="nextPage">
      <view class="section-card list-card" v-if="rows.length">
        <view class="section-header"><view class="section-dot" style="background:#10B981"></view><text class="section-title">购买明细</text></view>
        <view class="purchase-card" v-for="row in rows" :key="row.purchaseId" @tap="openDetail(row)">
          <view class="purchase-head"><view class="purchase-title-row"><text class="purchase-title">{{ row.customerName || '未登记顾客' }}</text><text class="purchase-tag">{{ customerTypeText(row.customerType) }}</text></view><text class="purchase-amount">¥{{ money(row.totalAmount) }}</text></view>
          <view class="purchase-customer-row"><text class="purchase-meta">购买日期 {{ dateText(row.purchaseDate) }}</text></view>
          <view class="purchase-metrics"><text>购买 {{ quantity(row.purchaseQuantity) }}</text><text>赠送 {{ quantity(row.giftQuantity) }}</text><text class="paid">已收 ¥{{ money(row.paidAmount) }}</text><text class="debt">待缴 ¥{{ money(row.receivableAmount) }}</text></view>
          <view class="purchase-footer"><text class="purchase-status">{{ paymentStatusText(row.paymentStatus) }} · {{ deliveryStatusText(row.deliveryStatus) }}</text><text class="arrow-icon">›</text></view>
        </view>
        <view class="pagination" v-if="rows.length">
          <button :disabled="pageNum <= 1" @tap="prevPage">上一页</button>
          <text class="page-info">{{ pageNum }} / {{ totalPages }}</text>
          <button :disabled="pageNum >= totalPages" @tap="nextPage">下一页</button>
        </view>
      </view>
      <view class="section-card list-card state-card" v-else>
        <view class="empty">暂无购买记录</view>
      </view>
    </scroll-view>

    <!-- ════════ 详情面板（参考销售记录 detail/index.vue） ════════ -->
    <view class="overlay-mask" v-if="panel === 'detail'" @tap="closePanel">
      <view class="detail-page" @tap.stop>
        <view class="detail-hero">
          <view class="detail-hero-bg"></view>
          <view class="detail-hero-content">
            <view class="detail-hero-eyebrow">会员购买 · {{ customerTypeText(detail.customerType) }}</view>
            <view class="detail-hero-title">{{ detail.purchaseNo || `购买单 #${detail.purchaseId}` }}</view>
            <view class="detail-hero-value">¥{{ money(detail.totalAmount) }}</view>
            <view class="detail-hero-meta">购买日期 {{ dateText(detail.purchaseDate) }}</view>
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
            <view class="detail-summary-row"><text class="detail-summary-label">总数量</text><text class="detail-summary-value">{{ quantity(detail.purchaseQuantity) }}（赠 {{ quantity(detail.giftQuantity) }}）</text></view>
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

        <view class="detail-footer-placeholder"></view>
        <view class="detail-footer-bar" v-if="detail && detail.purchaseId">
          <button v-if="can('edit')" class="detail-action-btn primary-btn" @tap="switchToEdit">编辑</button>
          <button v-if="can('payment') && Number(detail.receivableAmount || 0) > 0" class="detail-action-btn payment-btn" @tap="openPayment(detail)">收款</button>
          <button v-if="can('delivery')" class="detail-action-btn delivery-btn" @tap="openDelivery(detail)">领取</button>
          <button v-if="can('return')" class="detail-action-btn return-btn" @tap="openReturn(detail)">退货</button>
          <button v-if="hasReturn(detail)" class="detail-action-btn" @tap="openReturnDetail(detail)">退货详情</button>
          <button v-if="can('bind') && detail.customerType === 'WALK_IN' && !detail.memberId" class="detail-action-btn" @tap="openBind(detail)">绑定</button>
          <button v-if="can('cancel') && String(detail.orderStatus) !== 'CANCELLED' && String(detail.deliveryStatus) !== '2'" class="detail-action-btn danger-btn" @tap="cancel(detail)">作废</button>
        </view>
      </view>
    </view>

    <!-- ════════ 新建/编辑面板（参考销售记录 form/index.vue） ════════ -->
    <view class="overlay-mask" v-if="panel === 'create' || panel === 'edit'" @tap="closePanel">
      <view class="form-page" @tap.stop>
        <view class="form-hero">
          <view class="form-hero-icon">{{ panel === 'edit' ? '✎' : '＋' }}</view>
          <view class="form-hero-info">
            <view class="form-hero-title">{{ panel === 'edit' ? '编辑' : '新增' }}购买单</view>
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
          <button class="form-btn-primary" @tap="panel === 'edit' ? saveEdit() : saveCreate()">{{ panel === 'edit' ? '保存编辑' : '保存购买单' }}</button>
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
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import MemberSearch from '@/components/MemberSearch/index.vue'
import { hasActionPermission, requireModulePermission } from '@/utils/permission.js'
import { workContext } from '@/utils/workContext.js'

const newPurchaseForm = () => ({ purchaseDate: '', periodId: '', customerType: 'MEMBER', customerName: '', customerPhone: '', remark: '', item: { productId: '', purchaseQuantity: '', unitPrice: '', giftQuantity: '' } })

export default {
  components: { MemberSearch },
  data() { return { authorized: false, currentDeptName: '', currentDeptId: '', rows: [], loading: false, keyword: '', showMoreFilters: false, panel: '', detail: {}, active: {}, form: newPurchaseForm(), products: [], periods: [], policies: [], filters: { customerType: '', paymentStatus: '', beginTime: '', endTime: '' }, summary: null, pageNum: 1, pageSize: 20, total: 0, bindTarget: {}, bindForm: { memberId: '' }, returnPurchaseIds: [], paymentMethods: [{ label: '现金', value: 'CASH' }, { label: '微信支付', value: 'WECHAT' }, { label: '支付宝', value: 'ALIPAY' }, { label: '银行转账', value: 'BANK' }, { label: '其他', value: 'OTHER' }], paymentIndex: 0, paymentForm: { paymentAmount: '' }, deliveryItems: [], deliveryIndex: 0, deliveryForm: { saleDeliveryQuantity: '', giftDeliveryQuantity: '', receiverName: '' }, remarkCollapsed: false, showCustomerTypePicker: false } },
  computed: { panelTitle() { return ({ create: '新建购买单', detail: '购买单详情', edit: '编辑购买单', payment: '登记收款', delivery: '登记领取', bind: '绑定会员' })[this.panel] }, customerTypes() { return [{ label: '会员', value: 'MEMBER' }, { label: '非会员', value: 'CUSTOMER' }, { label: '散客', value: 'WALK_IN' }] }, customerTypeIndex() { const i = this.customerTypes.findIndex(x => x.value === this.form.customerType); return i < 0 ? 0 : i }, productIndex() { const i = this.products.findIndex(x => String(x.productId) === String(this.form.item.productId)); return i < 0 ? 0 : i }, periodIndex() { const i = this.periods.findIndex(x => String(x.periodId) === String(this.form.periodId)); return i < 0 ? 0 : i }, policyIndex() { const i = this.policies.findIndex(x => String(x.policyId) === String(this.form.item.policyId)); return i < 0 ? 0 : i }, packages() { return (this.policies[this.policyIndex]?.packages || []).map((x, i) => ({ ...x, label: `${x.packageName || `档位${i + 1}`}：买${this.quantity(x.purchaseQuantity)}送${this.quantity(x.giftQuantity)} · ¥${this.money(x.packagePrice)}` })) }, packageIndex() { const i = this.packages.findIndex(x => String(x.packageId) === String(this.form.item.packageId)); return i < 0 ? 0 : i }, selectedProduct() { return this.form.item.productId ? this.products[this.productIndex] : null }, selectedPolicy() { return this.form.item.policyId ? this.policies[this.policyIndex] : null }, selectedPackage() { return this.form.item.packageId ? this.packages[this.packageIndex] : null }, selectedPeriod() { return this.form.periodId ? this.periods[this.periodIndex] : null }, customerTypeFilters() { return [{ label: '全部', value: '' }, { label: '会员', value: 'MEMBER' }, { label: '非会员', value: 'CUSTOMER' }, { label: '散客', value: 'WALK_IN' }] }, paymentStatusFilters() { return [{ label: '全部', value: '' }, { label: '未收款', value: '0' }, { label: '部分收款', value: '1' }, { label: '已收清', value: '2' }] }, customerTypeFilterIndex() { const i = this.customerTypeFilters.findIndex(x => x.value === this.filters.customerType); return i < 0 ? 0 : i }, paymentStatusFilterIndex() { const i = this.paymentStatusFilters.findIndex(x => x.value === this.filters.paymentStatus); return i < 0 ? 0 : i }, totalPages() { return Math.max(1, Math.ceil(Number(this.total || 0) / Number(this.pageSize || 1))) } },
  onLoad() { this.authorized = requireModulePermission('memberPurchase'); const scope = workContext.snapshot(); this.currentDeptId = scope.currentDeptId; this.currentDeptName = scope.currentDept?.name || scope.currentDept?.deptName || '未选择机构'; if (this.authorized) { this.loadOptions(); this.load() } },
  methods: {
    emptyForm() { return { ...newPurchaseForm(), purchaseDate: this.today() } },
    can(action) { return hasActionPermission('memberPurchase', action) }, today() { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` }, isoDateTime(value) { if (!value) return new Date().toISOString(); const text = String(value); if (/^\d{4}-\d{2}-\d{2}$/.test(text)) return new Date(`${text}T00:00:00`).toISOString(); return new Date(text.replace(' ', 'T')).toISOString() }, dateText(v) { return v ? String(v).replace('T',' ').slice(0,19) : '-' }, money(v) { return Number(v || 0).toFixed(2) }, quantity(v) { return Number(v || 0).toFixed(3).replace(/\.0+$/, '').replace(/(\.\d*?)0+$/, '$1') }, customerTypeText(v) { return ({ MEMBER: '会员', CUSTOMER: '非会员', WALK_IN: '散客' })[v] || v || '-' }, paymentStatusText(v) { return ({ '0':'未收款','1':'部分收款','2':'已收清','3':'已退款' })[String(v)] || '未知' }, deliveryStatusText(v) { return ({ '0':'未领取','1':'部分领取','2':'全部领取' })[String(v)] || '未知' }, paymentMethodText(v) { return this.paymentMethods.find(x => x.value === v)?.label || ({ WECHAT:'微信支付', ALIPAY:'支付宝', CASH:'现金', BANK:'银行转账' })[v] || v || '-' }, paymentStatusClass(v) { const s = String(v ?? ''); return s === '2' ? 'status-ok' : s === '1' ? 'status-info' : 'status-warn' }, deliveryStatusClass(v) { const s = String(v ?? ''); return s === '2' ? 'status-ok' : s === '1' ? 'status-info' : 'status-warn' },
    unwrap(res) { return res?.rows || res?.data?.rows || res?.data || [] }, async loadOptions() { try { const [p, a] = await Promise.all([request({ url:'/finance/product/list', method:'GET', data:{ pageNum:1, pageSize:200, deptId:this.currentDeptId }, silent:true }), request({ url:'/finance/accountingPeriod/list', method:'GET', data:{ pageNum:1, pageSize:200, deptId:this.currentDeptId }, silent:true })]); this.products = this.unwrap(p); this.periods = this.unwrap(a).filter(x => String(x.deptId) === String(this.currentDeptId)).map(x => ({ ...x, label: `${x.periodNo || x.periodId}（${this.dateText(x.startTime)} 至 ${x.endTime ? this.dateText(x.endTime) : '当前'}）` })); if (this.periods.length && !this.form.periodId) { this.form.periodId = this.periods[0].periodId } } catch (e) { this.products = []; this.periods = [] } }, async loadPolicies() { if (!this.form.item.productId || !this.form.periodId) { this.policies = []; return }; const res = await request({ url:'/member/campaign/policy/list', method:'GET', data:{ pageNum:1, pageSize:200, deptId:this.currentDeptId, productId:this.form.item.productId, periodId:this.form.periodId, status:'1' } }); const rows = this.unwrap(res).filter(x => String(x.productId) === String(this.form.item.productId) && String(x.periodId) === String(this.form.periodId)); const detailed = await Promise.all(rows.map(async (policy) => { if (Array.isArray(policy.packages) && policy.packages.length) return policy; try { const detail = await request({ url:`/member/campaign/policy/${policy.policyId}`, method:'GET' }); return detail?.data || detail || policy } catch (e) { return policy } })); this.policies = [{ policyId:'', policyName:'不参加活动，按单价购买', packages:[] }, ...detailed] }, async load() { this.loading = true; try { const res = await request({ url:'/member/purchase/list', method:'GET', data:this.listParams() }); this.rows = this.unwrap(res); this.total = Number(res?.total ?? 0) || 0 } finally { this.loading = false } this.loadStatistics(); if (this.pageNum === 1) this.loadReturns() }, listParams() { const p = { pageNum:this.pageNum, pageSize:this.pageSize, deptId:this.currentDeptId, customerName:this.keyword || undefined, customerType:this.filters.customerType || undefined, paymentStatus:this.filters.paymentStatus || undefined, beginTime:this.filters.beginTime || undefined, endTime:this.filters.endTime || undefined }; Object.keys(p).forEach(k => p[k] === undefined && delete p[k]); return p }, async loadStatistics() { try { const res = await request({ url:'/member/purchase/statistics', method:'GET', data:this.listParams() }); this.summary = res?.data || res || {} } catch (e) {} }, async loadReturns() { try { const res = await request({ url:'/member/purchase-return/list', method:'GET', data:{ pageNum:1, pageSize:200, deptId:this.currentDeptId }, silent:true }); this.returnPurchaseIds = Array.from(new Set(this.unwrap(res).map(x => String(x.purchaseId)))) } catch (e) { this.returnPurchaseIds = [] } }, hasReturn(row) { return this.returnPurchaseIds.includes(String(row?.purchaseId)) },
    selectCustomerType(e) { this.form.customerType = this.customerTypes[Number(e.detail.value)].value; if (this.form.customerType !== 'MEMBER') { this.form.memberId = ''; this.form.memberNo = ''; this.form.memberName = '' } }, selectMember(member) { this.form.memberId = member?.memberId || ''; this.form.memberNo = member?.memberNo || ''; this.form.memberName = member?.memberName || ''; this.form.customerName = member?.memberName || ''; this.form.customerPhone = member?.phone || '' }, clearMember() { this.form.memberId = ''; this.form.memberNo = ''; this.form.memberName = '' }, selectCustomerTypeFilter(e) { this.filters.customerType = this.customerTypeFilters[Number(e.detail.value)].value; this.pageNum = 1; this.load() }, selectPaymentStatusFilter(e) { this.filters.paymentStatus = this.paymentStatusFilters[Number(e.detail.value)].value; this.pageNum = 1; this.load() }, resetFilters() { this.keyword = ''; this.filters = { customerType: '', paymentStatus: '', beginTime: '', endTime: '' }; this.pageNum = 1; this.load() }, toggleMoreFilters() { this.showMoreFilters = !this.showMoreFilters }, prevPage() { if (this.pageNum <= 1) return; this.pageNum--; this.load() }, nextPage() { if (this.pageNum >= this.totalPages) return; this.pageNum++; this.load() }, selectProduct(e) { const p = this.products[Number(e.detail.value)]; this.form.item.productId = p?.productId || ''; this.form.item.policyId = ''; this.form.item.packageId = ''; this.form.item.giftQuantity = ''; if (p && (p.salePrice != null || p.defaultSalePrice != null || p.price != null)) { this.form.item.unitPrice = this.money(p.salePrice ?? p.defaultSalePrice ?? p.price) } this.loadPolicies() }, selectPolicy(e) { const policy = this.policies[Number(e.detail.value)]; this.form.item.policyId = policy?.policyId || ''; this.form.item.packageId = ''; if (policy?.packages?.length === 1) this.selectPackage({ detail: { value: 0 } }) }, selectPackage(e) { const pkg = this.packages[Number(e.detail.value)]; this.form.item.packageId = pkg?.packageId || ''; if (pkg) { this.form.item.purchaseQuantity = this.quantity(pkg.purchaseQuantity); this.form.item.giftQuantity = this.quantity(pkg.giftQuantity); this.form.item.unitPrice = this.money(Number(pkg.packagePrice || 0) / Number(pkg.purchaseQuantity || 1)) } }, selectPeriod(e) { this.form.periodId = this.periods[Number(e.detail.value)]?.periodId || ''; this.loadPolicies() }, limit(key, value, precision) { const s = String(value || '').replace(/[^\d.]/g,'').replace(/\.(?=.*\.)/g,''); this.form.item[key] = s.includes('.') ? s.split('.')[0] + '.' + s.split('.')[1].slice(0, precision) : s }, limitPayment(e) { this.paymentForm.paymentAmount = String(e.detail.value || '').replace(/[^\d.]/g,'').replace(/\.(?=.*\.)/g,'').replace(/(\.\d{2}).*/,'$1') },
    openCreate() { this.showCustomerTypePicker = true }, pickCustomerType(e) { const idx = Number(e.detail.value); this.form = this.emptyForm(); this.form.customerType = this.customerTypes[idx].value; if (this.periods.length) { this.form.periodId = this.periods[0].periodId } this.showCustomerTypePicker = false; this.panel = 'create' }, openReturn(row) { uni.navigateTo({ url:`/pages/member-purchase-return/index?purchaseId=${row.purchaseId}` }) }, openReturnDetail(row) { uni.navigateTo({ url:`/pages/member-purchase-return/index?purchaseId=${row.purchaseId}` }) }, openBind(row) { this.bindTarget = row; this.bindForm = { memberId: '' }; this.panel = 'bind' }, selectBindMember(member) { this.bindForm = { memberId: member?.memberId || '', memberName: member?.memberName || '' } }, clearBindMember() { this.bindForm = { memberId: '' } }, async confirmBind() { if (!this.bindForm.memberId) return uni.showToast({ title:'请选择要绑定的会员', icon:'none' }); await request({ url:`/member/purchase/${this.bindTarget.purchaseId}/bind-member/${this.bindForm.memberId}`, method:'PUT' }); uni.showToast({ title:'绑定成功', icon:'success' }); this.closePanel(); this.load() }, async openDetail(row) { const res = await request({ url:`/member/purchase/${row.purchaseId}`, method:'GET' }); this.detail = res.data || res; this.form = { ...this.emptyForm(), ...this.detail, item: { ...(this.detail.items?.[0] || {}) } }; this.active = row; this.panel = 'detail' }, async openEdit(row) { await this.openDetail(row); this.panel = 'edit' }, switchToEdit() { this.panel = 'edit' }, closePanel() { this.panel = '' },
    async saveCreate() { const f = this.form; if (!f.periodId || !f.item.productId || Number(f.item.purchaseQuantity) <= 0 || Number(f.item.unitPrice) <= 0) return uni.showToast({ title:'请完整填写周期、商品、数量和单价', icon:'none' }); if (f.customerType === 'MEMBER' && !f.memberId) return uni.showToast({ title:'请选择会员', icon:'none' }); await request({ url:'/member/purchase', method:'POST', data:{ ...f, identityConfirmed:false, purchaseDate:f.purchaseDate, items:[{ ...f.item, giftQuantity:Number(f.item.giftQuantity || 0) }], idempotencyKey:`mp-purchase-${Date.now()}` } }); uni.showToast({ title:'购买单已保存', icon:'success' }); this.closePanel(); this.load() },
    async saveEdit() { await request({ url:`/member/purchase/${this.detail.purchaseId}`, method:'PUT', data:{ customerName:this.form.customerName, customerPhone:this.form.customerPhone, purchaseDate:this.form.purchaseDate, remark:this.form.remark, items:[this.form.item] } }); uni.showToast({ title:'购买单已保存', icon:'success' }); this.closePanel(); this.load() },
    async openPayment(row) { this.active = row; this.paymentForm = { paymentAmount: this.money(row.receivableAmount) }; this.paymentIndex = 0; this.panel = 'payment' }, async savePayment() { const n = Number(this.paymentForm.paymentAmount); if (!(n > 0) || n > Number(this.active.receivableAmount || 0)) return uni.showToast({ title:'收款金额必须大于0且不能超过待缴金额', icon:'none' }); await request({ url:`/member/purchase/${this.active.purchaseId}/payment`, method:'POST', data:{ paymentAmount:n, paymentMethod:this.paymentMethods[this.paymentIndex].value, paymentDate:this.isoDateTime(this.today()), idempotencyKey:`mp-payment-${Date.now()}` } }); uni.showToast({ title:'收款成功', icon:'success' }); this.closePanel(); this.load() },
    async openDelivery(row) { const res = await request({ url:`/member/purchase/${row.purchaseId}`, method:'GET' }); const d = res.data || res; this.active = row; this.deliveryItems = (d.items || []).map(x => ({ ...x, label:`${x.productNameSnapshot || '商品'}（待领取${this.quantity(x.remainingQuantity)}）` })).filter(x => Number(x.remainingQuantity || 0) > 0); this.deliveryIndex = 0; const first = this.deliveryItems[0]; this.deliveryForm = { saleDeliveryQuantity:this.quantity(Math.max(0, Number(first?.purchaseQuantity || 0) - Number(first?.deliveredSaleQuantity || 0))), giftDeliveryQuantity:this.quantity(Math.max(0, Number(first?.giftQuantity || 0) - Number(first?.deliveredGiftQuantity || 0))), receiverName:d.customerName || '' }; this.panel = 'delivery' }, selectDeliveryItem(e) { this.deliveryIndex = Number(e.detail.value); const item = this.deliveryItems[this.deliveryIndex]; this.deliveryForm.saleDeliveryQuantity = this.quantity(Math.max(0, Number(item?.purchaseQuantity || 0) - Number(item?.deliveredSaleQuantity || 0))); this.deliveryForm.giftDeliveryQuantity = this.quantity(Math.max(0, Number(item?.giftQuantity || 0) - Number(item?.deliveredGiftQuantity || 0))) }, async saveDelivery() { const item = this.deliveryItems[this.deliveryIndex]; const sale = Number(this.deliveryForm.saleDeliveryQuantity || 0); const gift = Number(this.deliveryForm.giftDeliveryQuantity || 0); const saleRemaining = Number(item?.purchaseQuantity || 0) - Number(item?.deliveredSaleQuantity || 0); const giftRemaining = Number(item?.giftQuantity || 0) - Number(item?.deliveredGiftQuantity || 0); if (!item || sale + gift <= 0 || sale > saleRemaining || gift > giftRemaining) return uni.showToast({ title:'领取数量不能超过剩余可领取数量', icon:'none' }); await request({ url:`/member/purchase/${this.active.purchaseId}/delivery`, method:'POST', data:{ itemId:item.itemId, saleDeliveryQuantity:sale, giftDeliveryQuantity:gift, totalDeliveryQuantity:sale+gift, deliveryDate:this.isoDateTime(this.today()), receiverName:this.deliveryForm.receiverName, idempotencyKey:`mp-delivery-${Date.now()}` } }); uni.showToast({ title:'领取成功', icon:'success' }); this.closePanel(); this.load() },
    async cancel(row) { const ok = await new Promise(resolve => uni.showModal({ title:'确认作废', content:`确认作废购买单 ${row.purchaseNo}？`, success:r=>resolve(r.confirm) })); if (!ok) return; await request({ url:`/member/purchase/${row.purchaseId}/cancel`, method:'PUT' }); uni.showToast({ title:'购买单已作废', icon:'success' }); this.load() }
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
.work-scope{display:flex;align-items:center;margin:20rpx 30rpx 0;min-height:44rpx}
.work-scope-mark{width:14rpx;height:14rpx;margin-right:16rpx;border-radius:50%;background:#1687f5}
.work-scope-copy{display:flex;align-items:baseline;color:#8192a6;font-size:24rpx}
.work-scope-name{margin-left:4rpx;color:#26384d;font-size:27rpx;font-weight:700}

/* ── 通用卡片容器（section-card） ── */
.section-card{background:#fff;border-radius:20rpx;padding:28rpx;margin-top:24rpx;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);box-sizing:border-box;overflow:hidden}
.section-header{display:flex;align-items:center;gap:12rpx;margin-bottom:18rpx}
.section-dot{width:12rpx;height:12rpx;border-radius:50%;flex-shrink:0}
.section-title{font-size:28rpx;font-weight:700;color:#1A2332;flex:1}
.section-link{font-size:22rpx;color:#94A3B8}

/* ── 筛选区卡片 ── */
.filters-card{margin:16rpx 30rpx 0!important;padding:22rpx 24rpx!important}
.filter-row{display:flex;align-items:center;gap:10rpx;min-height:66rpx;width:100%;box-sizing:border-box}
.filter-row+.filter-row{margin-top:10rpx}
.filter-row-tools{gap:12rpx;margin-top:14rpx}
.filter-type-picker{flex:1;min-width:0}
.filter-picker,.filter-date,.filter-kw{box-sizing:border-box!important;padding:16rpx 14rpx;height:64rpx;line-height:32rpx;border:1rpx solid #D5E0EC;border-radius:12rpx;background:#F8FBFD;color:#5A6B7F;font-size:22rpx;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;width:100%}
.filter-picker{display:flex;align-items:center;justify-content:space-between}
.filter-chevron{color:#94a3b8;font-size:25rpx;margin-left:8rpx}
.date-controls{display:flex;align-items:center;flex:1;min-width:0;gap:8rpx;width:100%}
.date-controls picker{flex:1;min-width:0}
.filter-date{padding:16rpx 8rpx;height:64rpx;line-height:32rpx;text-align:center;font-size:21rpx;min-width:0}
.date-separator{flex:none;color:#94a3b8;font-size:19rpx}
.filter-kw{flex:1;min-width:0}
.filter-kw-member{flex:1;min-width:0}
.filter-button{flex:none;margin:0;padding:0 16rpx;height:64rpx;line-height:64rpx;border:0;border-radius:32rpx;background:#087CF0;color:#fff;font-size:22rpx;white-space:nowrap}
.filter-button-ghost{background:#EEF3F8;color:#334155}

/* ── 汇总条（两行三栏分栏） ── */
.summary-bar{display:flex;margin:16rpx 30rpx 0;padding:18rpx 8rpx;background:#fff;border-radius:18rpx;border:1rpx solid #dbe6f1;box-sizing:border-box}
.summary-bar>view{flex:1;text-align:center;border-right:1rpx solid #edf1f5;min-width:0}
.summary-bar>view:last-child{border-right:0}
.summary-value{display:block;color:#1687f5;font-size:28rpx;font-weight:700;font-variant-numeric:tabular-nums;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.summary-value.success{color:#10B981}
.summary-value.warning{color:#F59E0B}
.summary-label{display:block;margin-top:6rpx;color:#98a9ba;font-size:20rpx}
.summary-bar-secondary{margin-top:10rpx}

/* ── 浮动底部操作栏 ── */
.scroll-pad{height:16rpx;margin:16rpx 0 0}
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

/* ── 购买单卡片（分隔线式，不是独立盒阴影） ── */
.purchase-card{padding:22rpx 0;border-bottom:1rpx solid #e7edf3}
.purchase-card:last-of-type{border-bottom:0}
.purchase-head{display:flex;justify-content:space-between;align-items:flex-start;gap:14rpx}
.purchase-title-row{display:flex;align-items:center;gap:12rpx;min-width:0}
.purchase-title{font-size:28rpx;font-weight:700;color:#1A2332}
.purchase-tag{color:#1687f5;background:#edf5ff;padding:6rpx 14rpx;border-radius:20rpx;font-size:22rpx;flex:none}
.purchase-amount{font-size:32rpx;font-weight:700;color:#e6535b;flex:none;font-variant-numeric:tabular-nums}
.purchase-customer-row{margin-top:14rpx}
.purchase-customer{display:block;font-size:28rpx;font-weight:600;color:#1e293b}
.purchase-meta{display:block;color:#64748b;font-size:23rpx;margin-top:6rpx}
.purchase-metrics{display:flex;justify-content:flex-start;flex-wrap:wrap;gap:18rpx;color:#64748b;font-size:23rpx;padding:16rpx 0 0;margin-top:14rpx;border-top:1rpx solid #f0f3f7}
.purchase-metrics .paid{color:#22a06b}
.purchase-metrics .debt{color:#d98b21}
.purchase-footer{margin-top:16rpx;color:#64748b;font-size:22rpx;display:flex;align-items:flex-end;justify-content:space-between;gap:14rpx}
.purchase-status{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.row-buttons{display:flex;flex-wrap:wrap;justify-content:flex-end;gap:8rpx;flex:none}
.row-buttons button{border:0;border-radius:10rpx;font-size:22rpx;padding:6rpx 14rpx;background:#EEF3F8;color:#334155;margin:0;line-height:1.6}
.row-buttons .warn{color:#b76b00!important;background:#FFF6E8}
.row-buttons .success{color:#16865a!important;background:#E8F8EF}
.row-buttons .danger{color:#d64545!important;background:#FDECEE}

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
.overlay-mask{position:fixed;inset:0;background:rgba(15,23,42,.45);z-index:50;overflow:hidden}

/* ────────────────── 详情页 ────────────────── */
.detail-page{height:100vh;background:#E8EEF5;padding-bottom:calc(40rpx + env(safe-area-inset-bottom));box-sizing:border-box;overflow-y:auto;-webkit-overflow-scrolling:touch}

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
.detail-highlight-value.tone-success{color:#059669}
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

/* 详情页 底部操作栏（参考 footer-bar） */
.detail-footer-placeholder{height:140rpx}
.detail-footer-bar{position:fixed;left:0;right:0;bottom:0;display:flex;align-items:center;justify-content:center;gap:16rpx;padding:16rpx 24rpx;padding-bottom:calc(16rpx + env(safe-area-inset-bottom));background:#fff;box-shadow:0 -2rpx 16rpx rgba(8,124,240,.06);z-index:100;flex-wrap:wrap}
.detail-action-btn{flex:1;height:76rpx;line-height:76rpx;font-size:28rpx;font-weight:500;border-radius:16rpx;text-align:center;border:none;margin:0;padding:0;min-width:calc(25% - 12rpx)}
.detail-action-btn::after{border:none}
.detail-action-btn.edit-btn{background:#E8EEF5;color:#087CF0;flex:0 0 auto;min-width:140rpx;padding:0 24rpx}
.detail-action-btn.primary-btn{background:#087CF0;color:#fff}
.detail-action-btn.payment-btn{background:#FEF3C7;color:#92400E}
.detail-action-btn.delivery-btn{background:#D1FAE5;color:#065F46}
.detail-action-btn.return-btn{background:#FEE2E2;color:#991B1B}
.detail-action-btn.danger-btn{background:#FECACA;color:#7F1D1D}

/* ────────────────── 表单页 ────────────────── */
.form-page{min-height:100vh;background:#E8EEF5;padding-bottom:calc(40rpx + env(safe-area-inset-bottom));box-sizing:border-box}

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
