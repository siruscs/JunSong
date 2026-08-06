<template>
  <view class="page">
    <view class="load-state" v-if="loading">加载盘点任务...</view>
    <view class="load-error" v-else-if="loadError">
      <text class="empty-title">加载失败</text>
      <text class="empty-subtitle">{{ loadError }}</text>
      <button class="retry-button" @tap="loadDetail">重新加载</button>
    </view>

    <template v-else-if="detail">
      <!-- 头部 -->
      <view class="header-card">
        <view class="header-row">
          <text class="take-no">{{ detail.takeNo }}</text>
          <text class="status-tag" :class="statusClass(detail.status)">{{ statusLabel(detail.status) }}</text>
        </view>
        <view class="header-meta">
          <text>门店：{{ detail.deptName || detail.deptId }}</text>
          <text v-if="detail.counterUserName">盘点人：{{ detail.counterUserName }}</text>
        </view>
        <view class="header-meta">
          <text v-if="detail.freezeTime">冻结：{{ formatTime(detail.freezeTime) }}</text>
          <text v-if="detail.submittedTime">提交：{{ formatTime(detail.submittedTime) }}</text>
        </view>
        <view class="header-meta" v-if="detail.reversalReason">
          <text>冲销原因：{{ detail.reversalReason }}</text>
        </view>
      </view>

      <!-- 操作按钮区 -->
      <view class="action-bar">
        <button
          v-if="detail.status === 'DRAFT'"
          class="action-btn primary"
          :disabled="actionLoading"
          @tap="handleStart"
        >启动盘点</button>
        <button
          v-if="detail.status === 'COUNTING'"
          class="action-btn success"
          :disabled="actionLoading"
          @tap="handleSubmit"
        >提交盘点</button>
        <button
          v-if="detail.status === 'APPROVED'"
          class="action-btn success"
          :disabled="actionLoading"
          @tap="handlePost"
        >过账</button>
        <button
          v-if="canCancel"
          class="action-btn danger-plain"
          :disabled="actionLoading"
          @tap="handleCancel"
        >取消</button>
        <button
          v-if="detail.status === 'POSTED'"
          class="action-btn danger"
          :disabled="actionLoading"
          @tap="openReverseDialog"
        >整单冲销</button>
      </view>

      <!-- 盲盘提示 -->
      <view class="blind-notice" v-if="detail.hideExpected">
        盲盘模式：期望数量、方差、成本由系统在提交后计算，盘点时不可见。
      </view>

      <!-- 行项目列表 -->
      <view class="section-title">行项目（{{ detail.items.length }} 项）</view>
      <view class="item-card" v-for="item in detail.items" :key="item.itemId">
        <view class="item-head">
          <text class="item-name">{{ item.productName }}</text>
          <text class="item-counted" v-if="item.countedBy">已盘点</text>
        </view>

        <!-- 期望数量（盲盘时隐藏） -->
        <view class="item-row" v-if="!detail.hideExpected">
          <text class="item-label">期望数量</text>
          <text class="item-value">{{ formatNum(item.expectedQuantity) }}</text>
        </view>
        <view class="item-row" v-if="!detail.hideExpected">
          <text class="item-label">冻结后移动</text>
          <text class="item-value">{{ formatNum(item.movementQuantityAfterFreeze) }}</text>
        </view>

        <!-- 实际数量录入 -->
        <view class="item-row" v-if="detail.status === 'COUNTING' && !item.countedBy">
          <text class="item-label">实际数量</text>
          <input
            class="item-input"
            type="digit"
            v-model="editMap[item.itemId].actualQuantity"
            placeholder="输入实际数量"
          />
        </view>
        <view class="item-row" v-else>
          <text class="item-label">实际数量</text>
          <text class="item-value">{{ formatNum(item.actualQuantity) }}</text>
        </view>

        <!-- 复盘数量录入 -->
        <view class="item-row" v-if="detail.status === 'RECOUNTING' && !item.recountedBy">
          <text class="item-label">复盘数量</text>
          <input
            class="item-input"
            type="digit"
            v-model="editMap[item.itemId].recountQuantity"
            placeholder="0.000"
          />
        </view>
        <view class="item-row" v-else-if="item.recountQuantity !== null && item.recountQuantity !== undefined">
          <text class="item-label">复盘数量</text>
          <text class="item-value">{{ formatNum(item.recountQuantity) }}</text>
        </view>

        <view class="item-row" v-if="!detail.hideExpected && item.finalQuantity !== null">
          <text class="item-label">最终数量</text>
          <text class="item-value">{{ formatNum(item.finalQuantity) }}</text>
        </view>
        <view class="item-row" v-if="!detail.hideExpected && item.varianceQuantity !== null">
          <text class="item-label">方差数量</text>
          <text class="item-value" :class="varianceClass(item.varianceQuantity)">{{ formatNum(item.varianceQuantity) }}</text>
        </view>
        <view class="item-row" v-if="!detail.hideExpected && item.varianceAmount !== null">
          <text class="item-label">方差金额</text>
          <text class="item-value" :class="varianceClass(item.varianceAmount)">{{ formatMoney(item.varianceAmount) }}</text>
        </view>

        <!-- 原因代码选择（COUNTING/RECOUNTING 时） -->
        <view class="item-row" v-if="canEditReason(item)">
          <text class="item-label">原因代码</text>
          <picker
            class="item-picker"
            :range="reasonLabels"
            range-key="label"
            :value="reasonIndex(item.itemId)"
            @change="onReasonChange(item.itemId, $event)"
          >
            <view class="picker-value">{{ currentReasonLabel(item.itemId) || '请选择' }}</view>
          </picker>
        </view>
        <view class="item-row" v-else-if="item.reasonCode">
          <text class="item-label">原因</text>
          <text class="item-value">{{ reasonLabel(item.reasonCode) }}</text>
        </view>
        <view class="item-row" v-if="canEditReason(item)">
          <text class="item-label">原因说明</text>
          <input
            class="item-input"
            type="text"
            v-model="editMap[item.itemId].reason"
            placeholder="选填，原因说明"
            maxlength="200"
          />
        </view>

        <!-- 保存按钮 -->
        <view class="item-actions" v-if="detail.status === 'COUNTING' && !item.countedBy">
          <button
            class="save-btn"
            :disabled="savingMap[item.itemId]"
            @tap="saveCount(item)"
          >{{ savingMap[item.itemId] ? '保存中...' : '保存' }}</button>
        </view>
        <view class="item-actions" v-if="detail.status === 'RECOUNTING' && !item.recountedBy">
          <button
            class="save-btn"
            :disabled="savingMap[item.itemId]"
            @tap="saveRecount(item)"
          >{{ savingMap[item.itemId] ? '保存中...' : '保存复盘' }}</button>
        </view>
      </view>

      <!-- 历史记录 -->
      <view class="section-title">操作历史</view>
      <view class="history-card" v-for="h in detail.histories" :key="h.historyId">
        <view class="history-head">
          <text class="history-action">{{ h.action }}</text>
          <text class="history-time">{{ formatTime(h.createTime) }}</text>
        </view>
        <view class="history-transition" v-if="h.fromStatus || h.toStatus">
          {{ statusLabel(h.fromStatus || '新建') }} → {{ statusLabel(h.toStatus) }}
        </view>
        <view class="history-operator">操作人：{{ h.operator || '-' }}</view>
        <view class="history-comment" v-if="h.comment">{{ h.comment }}</view>
      </view>
      <view class="empty" v-if="!detail.histories || detail.histories.length === 0">
        <text class="empty-subtitle">暂无历史记录</text>
      </view>
    </template>

    <!-- 冲销对话框 -->
    <view class="dialog-mask" v-if="reverseDialogVisible" @tap="closeReverseDialog"></view>
    <view class="dialog-panel" v-if="reverseDialogVisible">
      <view class="dialog-title">整单冲销</view>
      <view class="dialog-warning">
        冲销将生成红字库存与成本台账，撤回本次盘点过账影响。该操作不可撤销。
      </view>
      <view class="dialog-field">
        <text class="dialog-label">冲销原因</text>
        <textarea
          class="dialog-textarea"
          v-model="reverseForm.reason"
          placeholder="请输入冲销原因（必填）"
          maxlength="500"
        />
      </view>
      <view class="dialog-field">
        <text class="dialog-label">幂等键</text>
        <input class="dialog-input" v-model="reverseForm.idempotencyKey" placeholder="幂等键（必填）" />
      </view>
      <view class="dialog-actions">
        <button class="dialog-btn" @tap="closeReverseDialog">取消</button>
        <button class="dialog-btn danger" :disabled="actionLoading" @tap="submitReverse">确认冲销</button>
      </view>
    </view>
  </view>
</template>

<script>
import {
  getStocktakeDetail,
  startStocktake,
  submitStocktake,
  postStocktake,
  cancelStocktake,
  reverseStocktake,
  countItem,
  recountItem,
  buildIdempotencyKey
} from '@/api/stocktake.js'
import { hasExactPermission } from '@/utils/permission.js'

const REASON_OPTIONS = [
  { label: '过期', value: 'EXPIRED' },
  { label: '破损', value: 'DAMAGED' },
  { label: '盗窃', value: 'THEFT' },
  { label: '称重损耗', value: 'WEIGHING' },
  { label: '操作损耗', value: 'OPERATION' },
  { label: '漏记交易', value: 'MISSING_TRANSACTION' },
  { label: '其他', value: 'OTHER' }
]

const STATUS_LABELS = {
  DRAFT: '草稿',
  COUNTING: '盘点中',
  SUBMITTED: '已提交',
  RECOUNTING: '复盘中',
  APPROVED: '已审批',
  POSTED: '已过账',
  REVERSED: '已冲销',
  CANCELLED: '已取消'
}

export default {
  data() {
    return {
      stocktakeId: null,
      detail: null,
      loading: true,
      loadError: '',
      actionLoading: false,
      editMap: {},
      savingMap: {},
      reverseDialogVisible: false,
      reverseForm: { reason: '', idempotencyKey: '' },
      reasonLabels: REASON_OPTIONS
    }
  },
  computed: {
    canCancel() {
      const s = this.detail?.status
      return ['DRAFT', 'COUNTING', 'SUBMITTED', 'RECOUNTING', 'APPROVED'].includes(s)
    }
  },
  onLoad(options) {
    this.stocktakeId = Number(options.id)
    if (!this.stocktakeId || Number.isNaN(this.stocktakeId)) {
      uni.showToast({ title: '无效的盘点任务ID', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 500)
      return
    }
    if (!hasExactPermission('finance:stocktake:query')) {
      uni.showToast({ title: '暂无盘点详情查看权限', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 500)
      return
    }
    this.loadDetail()
  },
  methods: {
    statusLabel(status) {
      return STATUS_LABELS[status] || status || '-'
    },
    statusClass(status) {
      if (['POSTED', 'APPROVED'].includes(status)) return 'status-success'
      if (['REVERSED', 'CANCELLED'].includes(status)) return 'status-danger'
      if (['COUNTING', 'RECOUNTING'].includes(status)) return 'status-warning'
      return 'status-info'
    },
    formatNum(value) {
      if (value === null || value === undefined) return '-'
      return Number(value).toFixed(3)
    },
    formatMoney(value) {
      if (value === null || value === undefined) return '-'
      return '¥' + Number(value).toFixed(2)
    },
    formatTime(value) {
      if (!value) return ''
      return String(value).replace('T', ' ').slice(0, 16)
    },
    varianceClass(value) {
      if (value === null || value === undefined || Number(value) === 0) return ''
      return Number(value) > 0 ? 'text-success' : 'text-danger'
    },
    reasonLabel(value) {
      if (!value) return '-'
      return REASON_OPTIONS.find((o) => o.value === value)?.label || value
    },
    canEditReason(item) {
      const s = this.detail?.status
      return (s === 'COUNTING' && !item.countedBy) || (s === 'RECOUNTING' && !item.recountedBy)
    },
    reasonIndex(itemId) {
      const state = this.editMap[itemId]
      if (!state || !state.reasonCode) return -1
      return REASON_OPTIONS.findIndex((o) => o.value === state.reasonCode)
    },
    currentReasonLabel(itemId) {
      const state = this.editMap[itemId]
      if (!state || !state.reasonCode) return ''
      return this.reasonLabel(state.reasonCode)
    },
    onReasonChange(itemId, e) {
      const idx = Number(e.detail.value)
      if (idx >= 0 && this.editMap[itemId]) {
        this.editMap[itemId].reasonCode = REASON_OPTIONS[idx].value
      }
    },
    initEditMap() {
      if (!this.detail || !this.detail.items) return
      this.detail.items.forEach((item) => {
        const existing = this.editMap[item.itemId]
        if (!existing || existing.version !== item.version) {
          this.editMap[item.itemId] = {
            actualQuantity: item.actualQuantity ?? '',
            recountQuantity: item.recountQuantity ?? '',
            reasonCode: item.reasonCode || '',
            reason: item.reason || '',
            version: item.version
          }
        }
      })
    },
    async loadDetail() {
      this.loading = true
      this.loadError = ''
      try {
        const res = await getStocktakeDetail(this.stocktakeId)
        this.detail = res.data || res
        this.initEditMap()
      } catch (err) {
        this.loadError = err?.msg || err?.errMsg || '请求失败'
      } finally {
        this.loading = false
      }
    },
    async handleStart() {
      if (!hasExactPermission('finance:stocktake:count')) {
        uni.showToast({ title: '暂无启动盘点权限', icon: 'none' })
        return
      }
      const confirmed = await this.confirm('确认启动盘点？启动后将冻结库存快照。')
      if (!confirmed) return
      this.actionLoading = true
      try {
        await startStocktake(this.stocktakeId, this.detail.version)
        uni.showToast({ title: '盘点已启动', icon: 'success' })
        await this.loadDetail()
      } catch (err) {
        uni.showToast({ title: err?.msg || '启动失败', icon: 'none' })
      } finally {
        this.actionLoading = false
      }
    },
    async handleSubmit() {
      if (!hasExactPermission('finance:stocktake:submit')) {
        uni.showToast({ title: '暂无提交盘点权限', icon: 'none' })
        return
      }
      const confirmed = await this.confirm('确认提交盘点？提交后将生成方差并可能触发复盘。')
      if (!confirmed) return
      this.actionLoading = true
      try {
        await submitStocktake(this.stocktakeId, this.detail.version)
        uni.showToast({ title: '盘点已提交', icon: 'success' })
        await this.loadDetail()
      } catch (err) {
        uni.showToast({ title: err?.msg || '提交失败', icon: 'none' })
      } finally {
        this.actionLoading = false
      }
    },
    async handlePost() {
      if (!hasExactPermission('finance:stocktake:post')) {
        uni.showToast({ title: '暂无过账权限', icon: 'none' })
        return
      }
      const confirmed = await this.confirm('确认过账？过账将原子更新库存数量与移动平均成本。')
      if (!confirmed) return
      this.actionLoading = true
      try {
        await postStocktake(this.stocktakeId, this.detail.version)
        uni.showToast({ title: '过账成功', icon: 'success' })
        await this.loadDetail()
      } catch (err) {
        uni.showToast({ title: err?.msg || '过账失败', icon: 'none' })
      } finally {
        this.actionLoading = false
      }
    },
    async handleCancel() {
      if (!hasExactPermission('finance:stocktake:add')) {
        uni.showToast({ title: '暂无取消权限', icon: 'none' })
        return
      }
      const confirmed = await this.confirm('确认取消该盘点任务？取消后不可恢复。')
      if (!confirmed) return
      this.actionLoading = true
      try {
        await cancelStocktake(this.stocktakeId, this.detail.version)
        uni.showToast({ title: '已取消', icon: 'success' })
        await this.loadDetail()
      } catch (err) {
        uni.showToast({ title: err?.msg || '取消失败', icon: 'none' })
      } finally {
        this.actionLoading = false
      }
    },
    openReverseDialog() {
      if (!hasExactPermission('finance:stocktake:reverse')) {
        uni.showToast({ title: '暂无冲销权限', icon: 'none' })
        return
      }
      this.reverseForm.reason = ''
      this.reverseForm.idempotencyKey = this.createIdempotencyKey()
      this.reverseDialogVisible = true
    },
    closeReverseDialog() {
      this.reverseDialogVisible = false
    },
    createIdempotencyKey() {
      // 与 PC 一致的幂等键生成策略
      return `mp-reverse-${this.stocktakeId}-${Date.now()}-${Math.random().toString(36).slice(2)}`
    },
    async submitReverse() {
      if (!this.reverseForm.reason) {
        uni.showToast({ title: '请输入冲销原因', icon: 'none' })
        return
      }
      if (!this.reverseForm.idempotencyKey) {
        uni.showToast({ title: '幂等键不能为空', icon: 'none' })
        return
      }
      this.actionLoading = true
      try {
        await reverseStocktake(this.stocktakeId, {
          reason: this.reverseForm.reason,
          idempotencyKey: this.reverseForm.idempotencyKey,
          version: this.detail.version
        })
        uni.showToast({ title: '冲销成功', icon: 'success' })
        this.reverseDialogVisible = false
        await this.loadDetail()
      } catch (err) {
        uni.showToast({ title: err?.msg || '冲销失败', icon: 'none' })
      } finally {
        this.actionLoading = false
      }
    },
    async saveCount(item) {
      if (!hasExactPermission('finance:stocktake:count')) {
        uni.showToast({ title: '暂无行录入权限', icon: 'none' })
        return
      }
      const state = this.editMap[item.itemId]
      if (!state || state.actualQuantity === '' || state.actualQuantity === null) {
        uni.showToast({ title: '请输入实际数量', icon: 'none' })
        return
      }
      const actualQuantity = Number(state.actualQuantity)
      if (Number.isNaN(actualQuantity) || actualQuantity < 0) {
        uni.showToast({ title: '实际数量必须 ≥ 0', icon: 'none' })
        return
      }
      // 盲盘时不强制原因（方差不可见，由服务端校验）
      this.savingMap[item.itemId] = true
      try {
        await countItem(this.stocktakeId, item.itemId, {
          actualQuantity,
          reasonCode: state.reasonCode || undefined,
          reason: state.reason || undefined,
          idempotencyKey: buildIdempotencyKey(this.stocktakeId, item.productId, 'count', item.version),
          version: item.version
        })
        uni.showToast({ title: '行录入已保存', icon: 'success' })
        await this.loadDetail()
      } catch (err) {
        uni.showToast({ title: err?.msg || '保存失败', icon: 'none' })
      } finally {
        this.savingMap[item.itemId] = false
      }
    },
    async saveRecount(item) {
      if (!hasExactPermission('finance:stocktake:recount')) {
        uni.showToast({ title: '暂无复盘权限', icon: 'none' })
        return
      }
      const state = this.editMap[item.itemId]
      if (!state || state.recountQuantity === '' || state.recountQuantity === null) {
        uni.showToast({ title: '请输入复盘数量', icon: 'none' })
        return
      }
      const recountQuantity = Number(state.recountQuantity)
      if (Number.isNaN(recountQuantity) || recountQuantity < 0) {
        uni.showToast({ title: '复盘数量必须 ≥ 0', icon: 'none' })
        return
      }
      this.savingMap[item.itemId] = true
      try {
        await recountItem(this.stocktakeId, item.itemId, {
          recountQuantity,
          reasonCode: state.reasonCode || undefined,
          reason: state.reason || undefined,
          idempotencyKey: buildIdempotencyKey(this.stocktakeId, item.productId, 'recount', item.version),
          version: item.version
        })
        uni.showToast({ title: '复盘录入已保存', icon: 'success' })
        await this.loadDetail()
      } catch (err) {
        uni.showToast({ title: err?.msg || '保存失败', icon: 'none' })
      } finally {
        this.savingMap[item.itemId] = false
      }
    },
    confirm(message) {
      return new Promise((resolve) => {
        uni.showModal({
          title: '提示',
          content: message,
          success: (res) => resolve(!!res.confirm),
          fail: () => resolve(false)
        })
      })
    }
  }
}
</script>

<style>
.page { min-height: 100vh; background: #f5f7fb; padding: 12px; }
.load-state { padding: 40px; text-align: center; color: #8c8c8c; }
.load-error { display: flex; flex-direction: column; align-items: center; padding: 40px 20px; gap: 8px; }
.empty-title { font-size: 15px; color: #18202f; font-weight: 600; }
.empty-subtitle { font-size: 13px; color: #8c8c8c; }
.retry-button { margin-top: 8px; padding: 6px 16px; background: #087CF0; color: #fff; border-radius: 6px; font-size: 13px; }

.header-card { background: #fff; border-radius: 10px; padding: 14px; margin-bottom: 12px; }
.header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.take-no { font-size: 16px; font-weight: 700; color: #18202f; }
.status-tag { font-size: 12px; padding: 2px 8px; border-radius: 10px; }
.status-success { background: #f6ffed; color: #52c41a; }
.status-warning { background: #fff7e6; color: #fa8c16; }
.status-danger { background: #fff1f0; color: #f5222d; }
.status-info { background: #f0f2f5; color: #595959; }
.header-meta { display: flex; gap: 12px; font-size: 13px; color: #595959; margin-bottom: 4px; flex-wrap: wrap; }

.action-bar { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
.action-btn { flex: 1; min-width: 80px; padding: 8px 12px; border-radius: 6px; font-size: 14px; text-align: center; border: none; }
.action-btn.primary { background: #087CF0; color: #fff; }
.action-btn.success { background: #52c41a; color: #fff; }
.action-btn.danger { background: #f5222d; color: #fff; }
.action-btn.danger-plain { background: #fff; color: #f5222d; border: 1px solid #f5222d; }
.action-btn[disabled] { opacity: 0.5; }

.blind-notice { background: #fff7e6; color: #fa8c16; padding: 10px 12px; border-radius: 6px; font-size: 13px; margin-bottom: 12px; border-left: 3px solid #fa8c16; }

.section-title { font-size: 14px; font-weight: 600; color: #18202f; margin: 12px 0 8px; }
.item-card { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 8px; }
.item-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.item-name { font-size: 14px; font-weight: 600; color: #18202f; flex: 1; }
.item-counted { font-size: 11px; color: #52c41a; padding: 2px 6px; background: #f6ffed; border-radius: 8px; }
.item-row { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; font-size: 13px; }
.item-label { color: #8c8c8c; }
.item-value { color: #18202f; font-weight: 500; }
.item-input { flex: 1; max-width: 60%; padding: 6px 8px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; text-align: right; }
.item-picker { flex: 1; max-width: 60%; }
.picker-value { padding: 6px 8px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; text-align: right; color: #595959; }
.item-actions { display: flex; justify-content: flex-end; margin-top: 8px; }
.save-btn { padding: 6px 16px; background: #087CF0; color: #fff; border-radius: 4px; font-size: 13px; border: none; }
.save-btn[disabled] { opacity: 0.5; }

.text-success { color: #52c41a; font-weight: 600; }
.text-danger { color: #f5222d; font-weight: 600; }

.history-card { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 8px; }
.history-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.history-action { font-size: 14px; font-weight: 600; color: #18202f; }
.history-time { font-size: 12px; color: #8c8c8c; }
.history-transition { font-size: 12px; color: #595959; margin-bottom: 4px; }
.history-operator { font-size: 12px; color: #8c8c8c; }
.history-comment { font-size: 12px; color: #595959; margin-top: 4px; }

.empty { display: flex; flex-direction: column; align-items: center; padding: 20px; gap: 4px; }

.dialog-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 999; }
.dialog-panel { position: fixed; left: 16px; right: 16px; top: 50%; transform: translateY(-50%); background: #fff; border-radius: 10px; padding: 16px; z-index: 1000; }
.dialog-title { font-size: 16px; font-weight: 600; color: #18202f; margin-bottom: 12px; }
.dialog-warning { background: #fff7e6; color: #fa8c16; padding: 8px 10px; border-radius: 4px; font-size: 12px; margin-bottom: 12px; }
.dialog-field { margin-bottom: 12px; }
.dialog-label { display: block; font-size: 13px; color: #595959; margin-bottom: 4px; }
.dialog-input { width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; box-sizing: border-box; }
.dialog-textarea { width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; min-height: 60px; box-sizing: border-box; }
.dialog-actions { display: flex; gap: 8px; }
.dialog-btn { flex: 1; padding: 8px; border-radius: 6px; font-size: 14px; border: none; }
.dialog-btn.danger { background: #f5222d; color: #fff; }
.dialog-btn[disabled] { opacity: 0.5; }
</style>
