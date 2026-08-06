<template>
  <view class="page" v-if="authorized">
    <!-- ════════ hero 标题区（渐变背景 + 图标 + 标题） ════════ -->
    <view class="hero-card">
      <view class="hero-icon">{{ id ? '✎' : '＋' }}</view>
      <view class="hero-info">
        <view class="hero-title">{{ readOnly ? '查看' : (id ? '编辑' : '新增') }}销售政策</view>
        <view class="hero-meta">政策编号由服务端自动生成，套餐档位按本次政策独立保存</view>
      </view>
    </view>

    <!-- ════════ 必填信息区（政策编号、政策名称、核算周期、商品） ════════ -->
    <view class="section-card">
      <view class="section-header">
        <view class="section-dot required"></view>
        <text class="section-title">必填信息</text>
      </view>

      <!-- 政策编号（只读，服务端生成） -->
      <view class="form-item">
        <view class="label-row"><text class="label">政策编号</text></view>
        <view class="control readonly-control">{{ form.policyNo || '保存后自动生成' }}</view>
      </view>

      <!-- 政策名称 -->
      <view class="form-item">
        <view class="label-row"><text class="label">政策名称</text><text class="required-tag">*</text></view>
        <input class="control input" v-model="form.policyName" :disabled="readOnly" placeholder="例如：石斛六月销售政策" />
      </view>

      <!-- 核算周期（picker） -->
      <view class="form-item">
        <view class="label-row"><text class="label">核算周期</text><text class="required-tag">*</text></view>
        <picker class="native-picker" :disabled="readOnly" :range="periods" range-key="label" :value="periodIndex" @change="selectPeriod">
          <view class="control picker" :class="{ 'has-value': form.periodId }">
            <text class="picker-text">{{ form.periodId ? periods[periodIndex]?.label : '请选择当前机构核算周期' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <!-- 商品（picker） -->
      <view class="form-item">
        <view class="label-row"><text class="label">商品</text><text class="required-tag">*</text></view>
        <picker class="native-picker" :disabled="readOnly" :range="products" range-key="productName" :value="productIndex" @change="selectProduct">
          <view class="control picker" :class="{ 'has-value': form.productId }">
            <text class="picker-text">{{ form.productId ? products[productIndex]?.productName : '请选择商品' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>
    </view>

    <!-- ════════ 套餐档位区（卡片式布局，每个档位独立一个卡片） ════════ -->
    <view class="section-card">
      <view class="section-header">
        <view class="section-dot required"></view>
        <text class="section-title">套餐档位</text>
        <text class="section-count">{{ form.packages.length }}个</text>
        <view class="add-tier-btn" v-if="!readOnly" @tap="addTier">
          <text class="add-tier-icon">＋</text>
          <text class="add-tier-text">添加档位</text>
        </view>
      </view>

      <!-- 套餐档位列表（每个档位独立卡片） -->
      <view class="tier-item" v-for="(tier, index) in form.packages" :key="index">
        <view class="tier-header">
          <text class="tier-title">档位 {{ index + 1 }}</text>
          <text class="tier-delete" v-if="!readOnly" @tap="removeTier(index)">删除</text>
        </view>
        <view class="tier-fields">
          <!-- 档位名称 -->
          <view class="form-item">
            <view class="label-row"><text class="label">档位名称</text><text class="required-tag">*</text></view>
            <input class="control input" v-model="tier.packageName" :disabled="readOnly" placeholder="例如：5盒" />
          </view>
          <!-- 购买数量 / 赠送数量（两列） -->
          <view class="tier-grid-2col">
            <view class="form-item">
              <view class="label-row"><text class="label">购买数量</text><text class="required-tag">*</text></view>
              <input class="control input" v-model="tier.purchaseQuantity" :disabled="readOnly" type="digit" placeholder="0.000" @input="limit(tier,'purchaseQuantity',$event.detail.value,3)" />
            </view>
            <view class="form-item">
              <view class="label-row"><text class="label">赠送数量</text></view>
              <input class="control input" v-model="tier.giftQuantity" :disabled="readOnly" type="digit" placeholder="0.000" @input="limit(tier,'giftQuantity',$event.detail.value,3)" />
            </view>
          </view>
          <!-- 套餐价（2位小数） -->
          <view class="form-item">
            <view class="label-row"><text class="label">套餐价</text><text class="required-tag">*</text></view>
            <input class="control input" v-model="tier.packagePrice" :disabled="readOnly" type="digit" placeholder="0.00" @input="limit(tier,'packagePrice',$event.detail.value,2)" />
          </view>
        </view>
      </view>

      <!-- 空态提示 -->
      <view class="empty-inline" v-if="!form.packages.length">请添加至少一个套餐档位；零散购买不在此处配置。</view>
    </view>

    <!-- ════════ 底部操作栏（返回 / 保存政策） ════════ -->
    <view class="footer-placeholder"></view>
    <view class="footer">
      <button class="btn-secondary" @tap="back">
        <text class="btn-icon">←</text> 返回
      </button>
      <button class="btn-primary" v-if="!readOnly" @tap="save">
        <text class="btn-icon">✓</text> 保存政策
      </button>
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { hasActionPermission, requireModulePermission } from '@/utils/permission.js'
import { workContext } from '@/utils/workContext.js'

export default {
  data() {
    return {
      authorized: false, id: '', readOnly: false, deptId: '',
      products: [], periods: [],
      form: { policyNo: '', policyName: '', periodId: '', productId: '', status: '1', packages: [] }
    }
  },
  computed: {
    periodIndex() {
      const i = this.periods.findIndex(x => String(x.periodId) === String(this.form.periodId))
      return i < 0 ? 0 : i
    },
    productIndex() {
      const i = this.products.findIndex(x => String(x.productId) === String(this.form.productId))
      return i < 0 ? 0 : i
    }
  },
  onLoad(o) {
    this.id = o.id || ''
    this.readOnly = o.mode === 'view'
    this.authorized = requireModulePermission('campaignPolicy') && (this.readOnly ? hasActionPermission('campaignPolicy', 'query') : hasActionPermission('campaignPolicy', this.id ? 'edit' : 'add'))
    const s = workContext.snapshot()
    this.deptId = s.currentDeptId
    if (this.authorized) this.init()
  },
  methods: {
    unwrap(r) { return r?.rows || r?.data?.rows || r?.data || [] },
    date(v) { return v ? String(v).replace('T', ' ').slice(0, 19) : '-' },
    async init() {
      const [p, a] = await Promise.all([
        request({ url: '/finance/product/list', method: 'GET', data: { pageNum: 1, pageSize: 200, deptId: this.deptId } }),
        request({ url: '/finance/accountingPeriod/list', method: 'GET', data: { pageNum: 1, pageSize: 200, deptId: this.deptId } })
      ])
      this.products = this.unwrap(p)
      this.periods = this.unwrap(a).filter(x => String(x.deptId) === String(this.deptId)).map(x => ({ ...x, label: `${x.periodNo || x.periodId}（${this.date(x.startTime)} 至 ${x.endTime ? this.date(x.endTime) : '当前'}）` }))
      // 自动选中第一个周期
      if (this.periods.length && !this.form.periodId) {
        this.form.periodId = this.periods[0].periodId
      }
      if (this.id) {
        const r = await request({ url: `/member/campaign/policy/${this.id}`, method: 'GET' })
        const d = r.data || r
        this.form = { ...this.form, ...d, packages: (d.packages || []).map(x => ({ ...x })) }
      }
    },
    selectPeriod(e) { this.form.periodId = this.periods[Number(e.detail.value)]?.periodId || '' },
    selectProduct(e) { this.form.productId = this.products[Number(e.detail.value)]?.productId || '' },
    addTier() { this.form.packages.push({ packageName: '', purchaseQuantity: '', giftQuantity: '', packagePrice: '', sortNo: this.form.packages.length + 1 }) },
    removeTier(i) { this.form.packages.splice(i, 1) },
    limit(obj, key, value, precision) {
      let s = String(value || '').replace(/[^\d.]/g, '').replace(/\.(?=.*\.)/g, '')
      obj[key] = s.includes('.') ? s.split('.')[0] + '.' + s.split('.')[1].slice(0, precision) : s
    },
    async save() {
      if (!this.form.policyName || !this.form.periodId || !this.form.productId) return uni.showToast({ title: '请填写政策名称、核算周期和商品', icon: 'none' })
      if (!this.form.packages.length) return uni.showToast({ title: '请至少添加一个套餐档位', icon: 'none' })
      for (const p of this.form.packages) {
        if (!p.packageName || !(Number(p.purchaseQuantity) > 0) || Number(p.giftQuantity) < 0 || !(Number(p.packagePrice) > 0)) return uni.showToast({ title: '请完整填写套餐档位，数量3位小数、金额2位小数', icon: 'none' })
        p.totalQuantity = Number(p.purchaseQuantity) + Number(p.giftQuantity)
      }
      const body = { ...this.form, packages: this.form.packages.map((p, i) => ({ ...p, sortNo: i + 1 })) }
      await request({ url: this.id ? `/member/campaign/policy/${this.id}` : '/member/campaign/policy', method: this.id ? 'PUT' : 'POST', data: body })
      uni.showToast({ title: '销售政策已保存', icon: 'success' })
      setTimeout(() => this.back(), 400)
    },
    back() { uni.navigateBack() }
  }
}
</script>

<style scoped>
/* ════════════════════════════════════════════════
 * 销售政策表单页 - 参考销售记录 form/index.vue UI 风格
 * 颜色主题：#087CF0 蓝
 * hero-card 渐变：linear-gradient(135deg,#C7DCF2,#EAF3FC)
 * 所有表单控件 box-sizing:border-box!important，固定高度 84rpx
 * ════════════════════════════════════════════════ */

/* ── 页面容器 ── */
.page {
  min-height: 100vh;
  padding: 0 0 160rpx;
  background: #E8EEF5;
}

/* ── hero 标题区（渐变背景 + 图标 + 标题） ── */
.hero-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 36rpx 28rpx;
  background: linear-gradient(135deg, #C7DCF2 0%, #EAF3FC 100%);
  border-top: 1rpx solid #B7D1EB;
  border-radius: 0 0 24rpx 24rpx;
}

.hero-icon {
  width: 72rpx;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 18rpx;
  font-size: 36rpx;
  color: #FFFFFF;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-info {
  flex: 1;
  min-width: 0;
}

.hero-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1F2D3D;
}

.hero-meta {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #6E8197;
}

/* ── section 卡片容器 ── */
.section-card {
  margin: 20rpx 28rpx 0;
  padding: 28rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  border: 1rpx solid #D5E0EC;
  box-shadow: 0 5rpx 18rpx rgba(45, 72, 98, 0.07);
  box-sizing: border-box;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 20rpx;
}

.section-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #087CF0;
  flex-shrink: 0;
}

.section-dot.required {
  background: #EF4444;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A2332;
  flex: 1;
}

.section-count {
  margin-left: auto;
  font-size: 22rpx;
  color: #94A3B8;
}

/* ── 添加档位按钮（在 section-header 右侧） ── */
.add-tier-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 18rpx;
  margin-left: 8rpx;
  background: #EFF6FF;
  border-radius: 999rpx;
  border: 1rpx solid #BFDBFE;
  flex-shrink: 0;
}

.add-tier-icon {
  font-size: 28rpx;
  color: #087CF0;
  font-weight: 700;
  line-height: 1;
}

.add-tier-text {
  font-size: 22rpx;
  color: #087CF0;
  font-weight: 500;
}

/* ── 表单项（label + control） ── */
.form-item {
  padding-top: 20rpx;
  border-top: 1rpx solid #E8EEF5;
}

.form-item + .form-item {
  border-top: 1rpx solid #E8EEF5;
  padding-top: 20rpx;
}

.form-item:first-child {
  border-top: 0;
  padding-top: 0;
}

.label-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 12rpx;
}

.label {
  font-size: 24rpx;
  color: #5A6B7F;
  font-weight: 500;
}

.required-tag {
  color: #EF4444;
  font-size: 26rpx;
  font-weight: 700;
}

/* ── 表单控件（统一高度 84rpx、box-sizing:border-box!important） ── */
.control {
  width: 100%;
  min-height: 84rpx;
  padding: 0 24rpx;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
  border-radius: 14rpx;
  font-size: 28rpx;
  color: #1A2332;
  box-sizing: border-box !important;
  transition: border-color 0.2s;
}

.control.input {
  display: block;
  width: 100%;
  height: 84rpx;
  line-height: 84rpx;
}

.control.input:focus {
  border-color: #087CF0;
  background: #FFFFFF;
}

.control:focus {
  border-color: #087CF0;
  background: #FFFFFF;
}

/* 只读展示控件（政策编号） */
.control.readonly-control {
  display: flex;
  align-items: center;
  height: 84rpx;
  line-height: 84rpx;
  color: #94A3B8;
  background: #F1F5F9;
}

/* picker 控件 */
.control.picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  line-height: 84rpx;
}

.picker-text {
  flex: 1;
  color: #1A2332;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.control.picker:not(.has-value) .picker-text {
  color: #94A3B8;
}

.picker-arrow {
  font-size: 32rpx;
  color: #CBD5E1;
  font-weight: 300;
  margin-left: 8rpx;
  flex-shrink: 0;
}

.native-picker {
  width: 100%;
  box-sizing: border-box !important;
}

/* ── 套餐档位卡片（每个档位独立一个卡片） ── */
.tier-item {
  margin-top: 20rpx;
  padding: 24rpx 20rpx 8rpx;
  background: #F8FAFC;
  border-radius: 16rpx;
  border: 1rpx solid #E2E8F0;
  box-sizing: border-box;
}

.tier-item:first-of-type {
  margin-top: 0;
}

.tier-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
  padding-bottom: 12rpx;
  border-bottom: 1rpx solid #E2E8F0;
}

.tier-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1A2332;
}

.tier-delete {
  font-size: 24rpx;
  color: #EF4444;
  padding: 4rpx 12rpx;
}

.tier-fields {
  /* 档位内字段区，复用 form-item 样式 */
}

/* 档位内两列网格（购买数量 / 赠送数量） */
.tier-grid-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  box-sizing: border-box;
}

/* 档位内的 form-item 顶部不需要分隔线（卡片内已有视觉分隔） */
.tier-item .form-item {
  border-top: 0;
  padding-top: 16rpx;
}

/* ── 空态提示 ── */
.empty-inline {
  padding: 32rpx 0 8rpx;
  text-align: center;
  color: #94A3B8;
  font-size: 24rpx;
}

/* ── 底部操作栏（固定底部 + backdrop-filter） ── */
.footer-placeholder {
  height: 140rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 16rpx;
  padding: 18rpx 24rpx;
  padding-bottom: calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1rpx solid #E2E8F0;
  z-index: 100;
}

.btn-primary,
.btn-secondary {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 28rpx;
  border-radius: 999rpx;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  border: none;
  margin: 0;
  padding: 0;
}

.btn-primary::after,
.btn-secondary::after {
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
  box-shadow: 0 6rpx 20rpx rgba(8, 124, 240, 0.25);
}

.btn-secondary {
  background: #FFFFFF;
  color: #5A6B7F;
  border: 1rpx solid #E2E8F0;
}

.btn-icon {
  font-size: 28rpx;
}
</style>
