<template>
  <view class="page">
    <view v-if="loading" class="loading-state">
      <text class="loading-text">加载中...</text>
    </view>

    <template v-if="!loading && batch">
      <view class="summary-card">
        <view class="summary-head">
          <view><text class="summary-title">核销汇总</text><text class="summary-sub">{{ batch.batchNo || '核销批次' }}</text></view>
          <button class="export-button" :disabled="exporting" @tap.stop="exportImage">{{ exporting ? '生成中…' : '导出图片' }}</button>
        </view>
        <view class="summary-row"><text>费用（{{ expenseDetails.length }}笔）</text><text>¥{{ money(totalExpenseAmount) }}</text></view>
        <view class="summary-row"><text>借支（{{ advanceDetails.length }}笔）</text><text>¥{{ money(totalAdvanceAmount) }}</text></view>
        <view class="summary-row difference"><text>差额</text><text>¥{{ money(differenceAmount) }}</text></view>
        <text class="summary-explanation">{{ differenceExplanation }}</text>
      </view>
      <!-- 批次信息卡片 -->
      <view class="info-card">
        <view class="info-header">
          <text class="info-title">{{ batch.batchNo }}</text>
          <text class="status-tag" :class="statusClass(batch.status)">{{ statusText(batch.status) }}</text>
        </view>
        <view class="info-grid">
          <view class="info-item">
            <text class="info-label">核销单号</text>
            <text class="info-value">{{ batch.batchNo || '-' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">核销时间</text>
            <text class="info-value">{{ formatTime(batch.verifyTime) }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">类型</text>
            <text class="info-value">{{ batch.sourceType === 'LEGACY' ? '历史' : '正常' }}</text>
          </view>
        </view>
        <!-- 反核销信息 -->
        <view class="reverse-section" v-if="batch.status === 'REVERSED'">
          <view class="reverse-title">反核销信息</view>
          <view class="info-grid">
            <view class="info-item">
              <text class="info-label">反核销人</text>
              <text class="info-value">{{ batch.reverseBy || '-' }}</text>
            </view>
            <view class="info-item">
              <text class="info-label">反核销时间</text>
              <text class="info-value">{{ formatTime(batch.reverseTime) }}</text>
            </view>
          </view>
          <view class="reverse-reason" v-if="batch.reverseReason">
            <text class="info-label">原因</text>
            <text class="reason-text">{{ batch.reverseReason }}</text>
          </view>
        </view>
      </view>

      <!-- 费用明细 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">费用明细</text>
          <text class="section-count">{{ expenseDetails.length }}笔</text>
        </view>
        <view class="detail-list" v-if="expenseDetails.length">
          <view class="detail-card" v-for="(item, idx) in expenseDetails" :key="'e' + idx">
            <view class="detail-top">
              <text class="detail-no">{{ item.expenseNo || `费用 #${item.expenseId}` }}</text>
              <text class="detail-amount expense-color">¥{{ money(item.expenseAmount) }}</text>
            </view>
            <view class="detail-meta">
              <text class="meta-tag" v-if="item.expenseType">{{ item.expenseType }}</text>
              <text class="meta-text">{{ item.expenseDate || '' }}</text>
            </view>
            <text class="detail-content" v-if="item.expenseContent">{{ item.expenseContent }}</text>
          </view>
        </view>
        <view class="empty-inline" v-else>
          <text class="empty-inline-text">暂无费用明细</text>
        </view>
      </view>

      <!-- 借支明细 -->
      <view class="section" v-if="advanceDetails.length">
        <view class="section-header">
          <text class="section-title">借支明细</text>
          <text class="section-count">{{ advanceDetails.length }}笔</text>
        </view>
        <view class="detail-list">
          <view class="detail-card" v-for="(item, idx) in advanceDetails" :key="'a' + idx">
            <view class="detail-top">
              <text class="detail-no">{{ item.advanceNo || `借支 #${item.advanceId}` }}</text>
              <text class="detail-amount">¥{{ money(item.advanceAmount) }}</text>
            </view>
            <view class="detail-meta">
              <text class="meta-tag" :class="relationClass(item.relationType)">{{ relationText(item.relationType) }}</text>
              <text class="meta-tag generated" v-if="item.generatedFlag === '1'">生成</text>
              <text class="meta-text">{{ item.advanceDate || '' }}</text>
            </view>
            <text class="detail-content" v-if="item.purpose">{{ item.purpose }}</text>
          </view>
        </view>
      </view>
    </template>

    <view class="empty" v-if="!loading && !batch">
      <text class="empty-title">加载失败</text>
      <text class="empty-sub">无法获取核销批次详情</text>
    </view>
    <!-- 导出画布：尺寸由 exportLongImage → renderToCanvas({width, height}) 参数驱动，
         组件内部通过原生 setData 调整（Vue prop 会被 uni-app 编译成 u-p 透传，
         原生 Component 收不到，绑定无效）。 -->
    <wxml-to-canvas class="export-canvas"></wxml-to-canvas>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { requireModulePermission } from '@/utils/permission.js'
import { exportLongImage } from '@/utils/longImageExport.js'

export default {
  data() {
    return {
      batchId: null,
      batch: null,
      expenseDetails: [],
      advanceDetails: [],
      loading: true,
      exporting: false,
      exportCanvasComponent: null
    }
  },
  onReady() {
    this.exportCanvasComponent = this.getExportCanvasComponent()
  },
  onLoad(options) {
    requireModulePermission('verificationRecord')
    this.batchId = options.batchId
    if (this.batchId) {
      this.getDetail()
    } else {
      this.loading = false
    }
  },
  computed: {
    totalExpenseAmount() {
      return this.expenseDetails.reduce((sum, item) => sum + Number(item.expenseAmount || 0), 0)
    },
    totalAdvanceAmount() {
      return this.advanceDetails.reduce((sum, item) => sum + Number(item.advanceAmount || 0), 0)
    },
    differenceAmount() {
      return this.totalExpenseAmount - this.totalAdvanceAmount
    },
    differenceExplanation() {
      if (this.differenceAmount > 0) return `费用高于借支 ¥${this.money(this.differenceAmount)}，核销后形成应补款。`
      if (this.differenceAmount < 0) return `借支高于费用 ¥${this.money(Math.abs(this.differenceAmount))}，核销后生成未核销节余借支单。`
      return '费用与借支金额相等，无补款或节余。'
    }
  },
  methods: {
    getDetail() {
      this.loading = true
      request({ url: '/finance/verification-batch/' + this.batchId, method: 'GET' }).then(res => {
        const data = res.data || {}
        this.batch = data.batch || data
        this.expenseDetails = data.expenseDetails || []
        this.advanceDetails = data.advanceDetails || []
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    statusText(status) {
      return status === 'VERIFIED' ? '已核销' : status === 'REVERSED' ? '已反核销' : status
    },
    statusClass(status) {
      return status === 'VERIFIED' ? 'status-ok' : status === 'REVERSED' ? 'status-muted' : ''
    },
    diffClass(val) {
      const n = Number(val)
      return n > 0 ? 'diff-positive' : n < 0 ? 'diff-negative' : ''
    },
    async exportImage() {
      if (this.exporting) return
      if (!this.batch) {
        uni.showToast({ title: this.loading ? '详情加载中，请稍后重试' : '暂无可导出的核销详情', icon: 'none' })
        return
      }
      this.exporting = true
      uni.showLoading({ title: '准备导出…', mask: true })
      try {
        const style = this.exportStyle()
        // 画布尺寸由 exportLongImage 内部通过 renderToCanvas({width, height}) 参数下发，
        // 组件在原生实例上 setData 调整 buffer（页面侧 prop 绑定对原生组件无效）。
        const exportComponent = this.exportCanvasComponent || this.getExportCanvasComponent()
        if (!exportComponent?.renderToCanvas) throw new Error('导出插件未初始化，请稍后重试')
        await exportLongImage({
          title: '核销汇总',
          subtitle: `${this.batch.batchNo || '核销批次'} · ${this.statusText(this.batch.status)}`,
          summary: {
            expenseCount: this.expenseDetails.length,
            expenseAmount: this.money(this.totalExpenseAmount),
            advanceCount: this.advanceDetails.length,
            advanceAmount: this.money(this.totalAdvanceAmount),
            difference: this.money(this.differenceAmount),
            explanation: this.differenceExplanation
          },
          component: exportComponent,
          wxml: this.exportWxml(),
          style,
          sections: [
            { title: '单据信息', rows: [{ label: '核销单号', value: this.batch.batchNo || '-' }, { label: '核销时间', value: this.formatTime(this.batch.verifyTime) }, { label: '类型', value: this.batch.sourceType === 'LEGACY' ? '历史' : '正常' }] },
            { title: '费用明细', rows: this.expenseDetails.map((item) => ({ label: item.expenseNo || `费用 #${item.expenseId}`, value: `¥${this.money(item.expenseAmount)}`, meta: [item.expenseType, item.expenseDate, item.expenseContent].filter(Boolean).join(' · ') })) },
            { title: '借支明细', rows: this.advanceDetails.map((item) => ({ label: item.advanceNo || `借支 #${item.advanceId}`, value: `¥${this.money(item.advanceAmount)}`, meta: [this.relationText(item.relationType), item.advanceDate, item.purpose].filter(Boolean).join(' · ') })) }
          ]
        })
      } catch (error) {
        uni.showModal({ title: '导出失败', content: error?.message || '插件导出失败', showCancel: false })
      } finally {
        uni.hideLoading()
        this.exporting = false
      }
    },
    getExportCanvasComponent() {
      // 必须优先取原生组件实例：uni-app Vue3 的 $refs 指向包装代理，在代理上调用
      // this.setData 会触发 __treeManager__ undefined（真机已复现）。原生页面实例的
      // selectComponent 返回原生 Component 实例，其 setData 是合法路径 —— 组件内部
      // _applySize 依赖它调整画布尺寸。$refs 仅作为无 selectComponent 环境的兜底。
      const page = this.$scope?.$mp?.page || this.$mp?.page || this.$scope
      const native = page?.selectComponent?.('.export-canvas')
      if (native && typeof native.setData === 'function' && typeof native.renderToCanvas === 'function') {
        return native
      }
      const ref = this.$refs?.exportCanvas
      return ref?.renderToCanvas ? ref : null
    },
    exportWxml() {
      const esc = (value) => String(value ?? '-').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      // 方案：使用 marginTop 给每个容器的第一个子元素添加上方间距。
      // wxml-to-canvas 的 css-layout 引擎对 padding/position:absolute 支持有限，
      // 因此使用 flex 列布局 + 数字 marginTop/marginLeft 实现可靠的间距控制。
      const expenseCards = this.expenseDetails.map((item) =>
        `<view class="detailCard"><view class="detailTop"><text class="detailNo">${esc(item.expenseNo || `费用 #${item.expenseId}`)}</text><text class="detailAmount expenseAmount">¥${this.money(item.expenseAmount)}</text></view><text class="detailMeta">${esc([item.expenseType, item.expenseDate].filter(Boolean).join(' · '))}</text><text class="detailContent">${esc(item.expenseContent || '')}</text></view>`
      ).join('')
      const advanceCards = this.advanceDetails.map((item) =>
        `<view class="detailCard"><view class="detailTop"><text class="detailNo">${esc(item.advanceNo || `借支 #${item.advanceId}`)}</text><text class="detailAmount">¥${this.money(item.advanceAmount)}</text></view><text class="detailMeta">${esc([this.relationText(item.relationType), item.advanceDate].filter(Boolean).join(' · '))}</text><text class="detailContent">${esc(item.purpose || '')}</text></view>`
      ).join('')
      const infoCard = `<view class="infoCard"><view class="infoHeader"><text class="infoTitle">${esc(this.batch.batchNo || '-')}</text><text class="statusTag">${esc(this.statusText(this.batch.status))}</text></view><view class="infoGrid"><view class="infoItem"><text class="infoLabel">核销单号</text><text class="infoValue">${esc(this.batch.batchNo)}</text></view><view class="infoItem"><text class="infoLabel">核销时间</text><text class="infoValue">${esc(this.formatTime(this.batch.verifyTime))}</text></view><view class="infoItem"><text class="infoLabel">类型</text><text class="infoValue">${esc(this.batch.sourceType === 'LEGACY' ? '历史' : '正常')}</text></view></view></view>`
      const section = (title, count, cards, empty, key) =>
        `<view class="section ${key}Section"><view class="sectionHeader"><text class="sectionTitle">${title}</text><text class="sectionCount">${count}笔</text></view><view class="detailList ${key}List">${cards || `<text class="empty">${empty}</text>`}</view></view>`
      return `<view class="page"><view class="hero"><text class="heroTitle">核销汇总</text><text class="heroSub">${esc(this.batch.batchNo || '核销批次')} · ${esc(this.statusText(this.batch.status))}</text><text class="summaryExpense">费用（${this.expenseDetails.length}笔）　¥${this.money(this.totalExpenseAmount)}</text><text class="summaryAdvance">借支（${this.advanceDetails.length}笔）　¥${this.money(this.totalAdvanceAmount)}</text><text class="summaryDiff">差额　¥${this.money(this.differenceAmount)}</text><text class="explanation">${esc(this.differenceExplanation)}</text></view>${infoCard}${section('费用明细', this.expenseDetails.length, expenseCards, '暂无费用明细', 'expense')}${this.advanceDetails.length ? section('借支明细', this.advanceDetails.length, advanceCards, '暂无借支明细', 'advance') : ''}</view>`
    },
    exportStyle() {
      const expenseCount = this.expenseDetails.length
      const advanceCount = this.advanceDetails.length
      const expenseListHeight = expenseCount ? expenseCount * 86 : 36
      const advanceListHeight = advanceCount ? advanceCount * 86 : 36
      const expenseSectionHeight = 26 + expenseListHeight
      const advanceSectionHeight = 26 + advanceListHeight
      // 方案：给每个容器的第一个子元素添加 marginTop 作为顶部间距。
      // hero 内部结构（从上到下）：
      //   heroTitle:  24px + marginTop 16px（顶部间距）
      //   heroSub:    18px + marginTop 12px
      //   summaryExpense: 24px + marginTop 6px
      //   summaryAdvance: 24px + marginTop 4px
      //   summaryDiff: 26px + marginTop 4px
      //   explanation: 30px + marginTop 4px
      //   hero 总高度 = 16 + 24 + 12 + 18 + 6 + 24 + 4 + 24 + 4 + 26 + 4 + 30 = 192
      const heroHeight = 192
      // infoCard 内部结构：
      //   infoHeader: 24px + marginTop 14px（顶部间距）
      //   infoGrid: 78px + marginTop 10px
      //   infoCard 总高度 = 14 + 24 + 10 + 78 = 126
      const infoCardHeight = 126
      // detailCard 内部结构：
      //   detailTop: 22px + marginTop 14px（顶部间距）
      //   detailMeta: 18px + marginTop 2px
      //   detailContent: 20px + marginTop 2px
      //   detailCard 总高度 = 14 + 22 + 2 + 18 + 2 + 20 = 78
      const detailCardHeight = 78
      // 页面总高度 = 顶部间距 + hero + infoCard + sectionHeader + list + 底部间距
      const pageHeight = 12 + 14 + heroHeight + 14 + infoCardHeight + 14 + expenseSectionHeight + (advanceCount ? 14 + advanceSectionHeight : 0) + 12
      return {
        page: { width: 375, height: pageHeight, backgroundColor: '#E8EEF5', flexDirection: 'column' },
        // 顶部统计块使用与页面 .summary-hero 相同的渐变：
        // linear-gradient(135deg, #123F73 0%, #087CF0 72%, #5AA9E8 100%)
        // wxml-to-canvas 不支持渐变，backgroundGradient 由组件 drawView 扩展绘制。
        hero: { width: 351, height: heroHeight, marginTop: 12, marginLeft: 12, backgroundGradient: { colors: [{ offset: 0, color: '#123F73' }, { offset: 0.72, color: '#087CF0' }, { offset: 1, color: '#5AA9E8' }] }, borderRadius: 10, flexDirection: 'column' },
        heroTitle: { width: 319, height: 24, fontSize: 17, color: '#FFFFFF', marginTop: 16, marginLeft: 16 },
        heroSub: { width: 319, height: 18, fontSize: 11, color: '#D9E7F5', marginTop: 12, marginLeft: 16 },
        summaryExpense: { width: 319, height: 24, fontSize: 14, color: '#FFFFFF', marginTop: 6, marginLeft: 16 },
        summaryAdvance: { width: 319, height: 24, fontSize: 14, color: '#FFFFFF', marginTop: 4, marginLeft: 16 },
        summaryDiff: { width: 319, height: 26, fontSize: 16, color: '#FFFFFF', marginTop: 4, marginLeft: 16 },
        explanation: { width: 303, height: 30, fontSize: 10, lineBreak: 'char', color: '#FFFFFF', backgroundColor: '#24558A', marginTop: 4, marginLeft: 16 },
        infoCard: { width: 351, height: infoCardHeight, marginTop: 14, marginLeft: 12, backgroundColor: '#FFFFFF', borderRadius: 10, flexDirection: 'column' },
        infoHeader: { width: 323, height: 24, flexDirection: 'row', justifyContent: 'space-between', marginTop: 14, marginLeft: 14 },
        infoTitle: { width: 230, height: 22, fontSize: 16, color: '#1A2332' },
        statusTag: { width: 70, height: 22, fontSize: 11, color: '#10B981', backgroundColor: '#ECFDF5', textAlign: 'right' },
        infoGrid: { width: 323, height: 78, marginTop: 10, flexDirection: 'column', marginLeft: 14 },
        infoItem: { width: 323, height: 25, flexDirection: 'row', justifyContent: 'space-between' },
        infoLabel: { width: 84, height: 20, fontSize: 11, color: '#94A3B8' },
        infoValue: { width: 225, height: 20, fontSize: 13, color: '#1A2332', textAlign: 'right' },
        section: { width: 351, marginTop: 14, flexDirection: 'column' },
        expenseSection: { width: 351, height: expenseSectionHeight, marginTop: 14, marginLeft: 12, flexDirection: 'column' },
        advanceSection: { width: 351, height: advanceSectionHeight, marginTop: 14, marginLeft: 12, flexDirection: 'column' },
        sectionHeader: { width: 351, height: 26, flexDirection: 'row', justifyContent: 'space-between' },
        sectionTitle: { width: 230, height: 24, fontSize: 16, color: '#1A2332' },
        sectionCount: { width: 60, height: 22, fontSize: 11, color: '#94A3B8', textAlign: 'right' },
        detailList: { width: 351, flexDirection: 'column' },
        expenseList: { width: 351, height: expenseListHeight, flexDirection: 'column' },
        advanceList: { width: 351, height: advanceListHeight, flexDirection: 'column' },
        detailCard: { width: 351, height: detailCardHeight, marginTop: 8, backgroundColor: '#FFFFFF', borderRadius: 10, flexDirection: 'column' },
        detailTop: { width: 323, height: 22, flexDirection: 'row', justifyContent: 'space-between', marginTop: 14, marginLeft: 14 },
        detailNo: { width: 195, height: 20, fontSize: 14, color: '#1A2332' },
        detailAmount: { width: 96, height: 20, fontSize: 14, color: '#1A2332', textAlign: 'right' },
        expenseAmount: { color: '#F59E0B' },
        detailMeta: { width: 323, height: 18, fontSize: 11, color: '#94A3B8', marginTop: 2, marginLeft: 14 },
        detailContent: { width: 323, height: 20, fontSize: 11, color: '#5A6B7F', lineBreak: 'char', marginTop: 2, marginLeft: 14 },
        empty: { width: 351, height: 36, fontSize: 13, color: '#94A3B8' }
      }
    },
    relationText(type) {
      const map = { SOURCE: '原始借支', SUPPLEMENT: '补款', SURPLUS: '节余' }
      return map[type] || type || ''
    },
    relationClass(type) {
      return type === 'SUPPLEMENT' ? 'tag-warn' : type === 'SURPLUS' ? 'tag-ok' : ''
    },
    money(val) {
      const n = Number(val)
      return Number.isNaN(n) ? '0.00' : n.toFixed(2)
    },
    formatTime(t) {
      if (!t) return ''
      return String(t).replace('T', ' ').slice(0, 16)
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #E8EEF5;
  padding: 24rpx 28rpx 46rpx;
  box-sizing: border-box;
}

.summary-card {
  background: linear-gradient(135deg, #123F73 0%, #087CF0 72%, #5AA9E8 100%);
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 28rpx;
  color: #FFFFFF;
}

.summary-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.summary-head view {
  display: flex;
  flex-direction: column;
}

.summary-title {
  font-size: 30rpx;
  font-weight: 700;
}

.summary-sub {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 22rpx;
}

.export-button {
  margin: 0;
  padding: 0 18rpx;
  height: 56rpx;
  line-height: 56rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.55);
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.12);
  color: #FFFFFF;
  font-size: 22rpx;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14rpx 0;
  font-size: 28rpx;
  opacity: 0.95;
}

.summary-row.difference {
  font-weight: 700;
  font-size: 32rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.2);
  margin-top: 12rpx;
  padding-top: 20rpx;
}

.summary-explanation {
  display: block;
  margin-top: 16rpx;
  padding: 16rpx 20rpx;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12rpx;
  font-size: 24rpx;
  line-height: 1.5;
  opacity: 0.9;
}

.export-canvas {
  position: fixed;
  left: -10000px;
  top: 0;
  width: 375px;
  opacity: 1;
  pointer-events: none;
}

.export-fallback-canvas {
  position: fixed;
  left: -10000px;
  top: 0;
  width: 375px;
  height: 12000px;
  opacity: 1;
  pointer-events: none;
}

.loading-state {
  padding: 80rpx 0;
  text-align: center;
}

.loading-text {
  font-size: 26rpx;
  color: #94A3B8;
}

/* 批次信息卡片 */
.info-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  box-shadow: 0 2rpx 16rpx rgba(8, 124, 240, 0.06);
  margin-bottom: 24rpx;
}

.info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.info-title {
  font-size: 32rpx;
  font-weight: 800;
  color: #1A2332;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  font-size: 22rpx;
  font-weight: 600;
  padding: 4rpx 16rpx;
  border-radius: 10rpx;
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
  flex-shrink: 0;
}

.status-ok {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.status-muted {
  background: rgba(148, 163, 184, 0.12);
  color: #94A3B8;
}

.info-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.info-item {
  width: calc(50% - 8rpx);
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.info-label {
  font-size: 20rpx;
  color: #94A3B8;
}

.info-value {
  font-size: 24rpx;
  font-weight: 600;
  color: #1A2332;
}

.amount-row {
  display: flex;
  padding-top: 20rpx;
  border-top: 1rpx solid #F1F5F9;
}

.amount-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.amount-label {
  font-size: 20rpx;
  color: #94A3B8;
}

.amount-value {
  font-size: 30rpx;
  font-weight: 800;
  color: #1A2332;
  font-variant-numeric: tabular-nums;
}

.expense-color {
  color: #F59E0B;
}

.diff-positive {
  color: #EF4444;
}

.diff-negative {
  color: #10B981;
}

/* 反核销信息 */
.reverse-section {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #F1F5F9;
}

.reverse-title {
  font-size: 24rpx;
  font-weight: 700;
  color: #94A3B8;
  margin-bottom: 12rpx;
}

.reverse-reason {
  margin-top: 8rpx;
}

.reason-text {
  font-size: 24rpx;
  color: #5A6B7F;
  display: block;
  margin-top: 4rpx;
  line-height: 1.6;
}

/* 明细区块 */
.section {
  margin-bottom: 24rpx;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
  padding: 0 4rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
}

.section-count {
  font-size: 22rpx;
  color: #94A3B8;
}

.detail-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.detail-card {
  background: #FFFFFF;
  border-radius: 14rpx;
  padding: 20rpx 22rpx;
  box-shadow: 0 1rpx 8rpx rgba(8, 124, 240, 0.04);
}

.detail-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.detail-no {
  font-size: 26rpx;
  font-weight: 700;
  color: #1A2332;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-amount {
  font-size: 28rpx;
  font-weight: 800;
  color: #1A2332;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 6rpx;
}

.meta-tag {
  font-size: 20rpx;
  font-weight: 600;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  background: rgba(148, 163, 184, 0.1);
  color: #64748B;
}

.meta-tag.tag-ok {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.meta-tag.tag-warn {
  background: rgba(245, 158, 11, 0.1);
  color: #F59E0B;
}

.meta-tag.generated {
  background: rgba(245, 158, 11, 0.1);
  color: #F59E0B;
}

.meta-text {
  font-size: 20rpx;
  color: #94A3B8;
}

.detail-content {
  font-size: 22rpx;
  color: #5A6B7F;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-inline {
  padding: 40rpx 0;
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
</style>
