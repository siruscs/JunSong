<template>
  <view class="page">
    <!-- Hero -->
    <view class="hero">
      <view>
        <text class="eyebrow">会员服务</text>
        <text class="hero-title">体验人数汇报</text>
      </view>
      <view class="hero-btn" @tap="goRecord">
        <text class="hero-btn-text">去录入</text>
      </view>
    </view>

    <!-- 日期 -->
    <view class="date-bar">
      <picker mode="date" :value="currentDate" @change="onDateChange">
        <view class="date-picker">
          <text class="date-icon">📅</text>
          <text class="date-value">{{ currentDate }}</text>
          <text class="date-hint">点此切换日期（回看历史汇报）</text>
        </view>
      </picker>
    </view>

    <!-- 模式切换 -->
    <view class="mode-tabs">
      <view class="mode-tab" :class="{ active: mode === 'morning' }" @tap="switchMode('morning')">
        <text class="mode-tab-title">上午汇报</text>
        <text class="mode-tab-sub">中午发 · 昨天全天+今天上午</text>
      </view>
      <view class="mode-tab" :class="{ active: mode === 'full' }" @tap="switchMode('full')">
        <text class="mode-tab-title">全天汇报</text>
        <text class="mode-tab-sub">晚上发 · 昨天+今天+差值</text>
      </view>
    </view>

    <!-- 加载/错误态 -->
    <StateView v-if="status !== 'normal'" :status="status" @retry="loadData" />

    <scroll-view v-else scroll-y class="scroll">
      <!-- 自动数据（只读） -->
      <view class="auto-card">
        <view class="auto-card-title">
          <text class="auto-card-mark"></text>
          <text>自动取数（来自体验人数录入）</text>
        </view>
        <view class="auto-grid">
          <view class="auto-item">
            <text class="auto-num">{{ summary.yesterdayTotal ?? 0 }}</text>
            <text class="auto-label">昨天全天</text>
          </view>
          <view class="auto-item" v-if="mode === 'morning'">
            <text class="auto-num">{{ summary.todayMorningTotal ?? 0 }}</text>
            <text class="auto-label">今天上午</text>
          </view>
          <template v-else>
            <view class="auto-item">
              <text class="auto-num">{{ summary.todayTotal ?? 0 }}</text>
              <text class="auto-label">今天全天</text>
            </view>
            <view class="auto-item">
              <text class="auto-num" :class="{ 'auto-num-down': (summary.diff ?? 0) < 0 }">{{ summary.diff ?? 0 }}</text>
              <text class="auto-label">比昨天</text>
            </view>
          </template>
        </view>
        <view class="auto-saved-hint" v-if="savedHint">{{ savedHint }}</view>
      </view>

      <!-- 手动录入 -->
      <view class="form-card">
        <view class="form-title">
          <text class="auto-card-mark"></text>
          <text>汇报信息（{{ mode === 'morning' ? '上午' : '全天' }}）</text>
        </view>

        <view class="form-row">
          <text class="form-label">店长姓名</text>
          <input class="form-input" v-model="form.managerName" placeholder="自动带当前用户，可修改" />
        </view>

        <template v-if="mode === 'morning'">
          <view class="form-row form-row-col">
            <text class="form-label">3.新人留人情况（自动带入前一天）</text>
            <textarea class="form-textarea" v-model="form.answerNewcomer" placeholder="如：最近上的新人基本都在" :maxlength="200" />
          </view>
          <view class="form-row form-row-col">
            <text class="form-label">5.大课氛围 / 定棚顾客反应（自动带入前一天）</text>
            <textarea class="form-textarea" v-model="form.answerAtmos" placeholder="如：还可以。顾客反应挺好" :maxlength="200" />
          </view>
          <view class="form-row form-row-col">
            <text class="form-label">6.店面问题反馈</text>
            <textarea class="form-textarea" v-model="form.morningIssue" placeholder="今天遇到的问题、需要支持的事项" :maxlength="500" />
          </view>
        </template>

        <template v-else>
          <view class="form-row">
            <text class="form-label">主管</text>
            <input class="form-input" v-model="form.supervisor" placeholder="选填" :maxlength="30" />
          </view>
          <view class="form-row form-row-inline">
            <view class="form-inline-item">
              <text class="form-label">老带新人数</text>
              <input class="form-input" type="number" v-model="form.referralCount" placeholder="0" maxlength="5" />
            </view>
            <view class="form-inline-item">
              <text class="form-label">发传单数量</text>
              <input class="form-input" type="number" v-model="form.flyerCount" placeholder="0" maxlength="6" />
            </view>
          </view>
          <view class="form-row form-row-col">
            <text class="form-label">今天遇到的问题</text>
            <textarea class="form-textarea" v-model="form.fullIssue" placeholder="今天遇到的问题" :maxlength="500" />
          </view>
        </template>
      </view>

      <!-- 实时预览 -->
      <view class="preview-card">
        <view class="form-title">
          <text class="auto-card-mark"></text>
          <text>汇报文字预览</text>
        </view>
        <view class="preview-body">
          <text class="preview-text" user-select>{{ reportText }}</text>
        </view>
      </view>

      <view class="bottom-space"></view>
    </scroll-view>

    <!-- 底部：保存并复制 -->
    <view class="bottom-bar">
      <button class="copy-btn" :class="{ 'copy-btn-disabled': copying }" :disabled="copying" @tap="onCopy">
        <text v-if="copying">处理中…</text>
        <text v-else>📋 {{ isSaved ? '复制汇报文字（再次保存）' : '保存并复制汇报文字' }}</text>
      </button>
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import StateView from '@/components/StateView.vue'

function formatYMD(d) {
  if (!d) return ''
  const dt = typeof d === 'string' ? new Date(d) : d
  const y = dt.getFullYear()
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const day = String(dt.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export default {
  name: 'ExperienceReport',
  components: { StateView },
  data() {
    return {
      currentDate: formatYMD(new Date()),
      mode: 'morning',                // morning | full
      summary: {},                    // 自动取数
      savedHint: '',                  // "已保存"提示
      isSaved: false,                 // 当前模式下当天是否已保存过
      status: 'loading',
      copying: false,

      // 手动项（morning/full 共用 managerName，其余按模式）
      form: {
        managerName: '',
        answerNewcomer: '',
        answerAtmos: '',
        morningIssue: '',
        supervisor: '',
        referralCount: '',
        flyerCount: '',
        fullIssue: ''
      }
    }
  },

  computed: {
    reportText() {
      const f = this.form
      const d = this.currentDate ? this.currentDate.split('-') : []
      const m = d.length === 3 ? parseInt(d[1], 10) : ''
      const day = d.length === 3 ? parseInt(d[2], 10) : ''
      const name = (f.managerName || '').trim()

      if (this.mode === 'morning') {
        const lines = [
          `时间：${m}月${day}日`,
          `店长:${name}`,
          `1.昨天全天人数：${this.summary.yesterdayTotal ?? 0}`,
          `2.今天上午人数：${this.summary.todayMorningTotal ?? 0}`,
          `3.门店新人留人情况如何`,
          (f.answerNewcomer || '').trim(),
          `5.今天大课氛围如何，今天定棚顾客的反应？`,
          (f.answerAtmos || '').trim(),
          `6.店面问题反馈：`,
          (f.morningIssue || '').trim()
        ]
        return lines.join('\n')
      }

      const diff = this.summary.diff ?? 0
      const rc = (f.referralCount === '' || f.referralCount == null) ? 0 : Number(f.referralCount)
      const fc = (f.flyerCount === '' || f.flyerCount == null) ? 0 : Number(f.flyerCount)
      const lines = [
        `日期：${m}.${day}`,
        `店长：${name}`,
        `主管:${(f.supervisor || '').trim()}`,
        `昨天人数:${this.summary.yesterdayTotal ?? 0}`,
        `今天人数：${this.summary.todayTotal ?? 0}`,
        `比昨天多几人：${diff}`,
        `老带新人数：${rc}`,
        `发传单数量：${fc}`,
        `今天遇到的问题：${(f.fullIssue || '').trim()}`
      ]
      return lines.join('\n')
    }
  },

  onLoad() {
    const user = uni.getStorageSync('userInfo') || {}
    this.form.managerName = user.nickName || user.userName || user.username || ''
    this.loadData()
  },

  onShow() {
    // 从「去录入」返回时只刷新自动取数，不打断已填写的表单
    if (!this._loaded) return
    this.refreshSummary()
  },

  methods: {
    goRecord() {
      uni.navigateTo({ url: '/pages/experience-record/index' })
    },

    switchMode(m) {
      if (this.mode === m) return
      this.mode = m
      this.loadSavedState()
    },

    onDateChange(e) {
      if (e.detail.value === this.currentDate) return
      this.currentDate = e.detail.value
      this.loadData()
    },

    // === 数据加载 ===
    // 仅刷新自动取数（onShow 从录入页返回时用，不重置表单）
    async refreshSummary() {
      try {
        const res = await request({ url: '/member/experience/report/summary', method: 'GET', data: { date: this.currentDate } })
        this.summary = (res.data || res) || {}
      } catch (e) { /* 静默，不打断填写 */ }
    },

    async loadData() {
      this.status = 'loading'
      try {
        const [summaryRes, detailRes] = await Promise.all([
          request({ url: '/member/experience/report/summary', method: 'GET', data: { date: this.currentDate } }),
          request({ url: '/member/experience/report/detail', method: 'GET', data: { date: this.currentDate } })
        ])
        this.summary = (summaryRes.data || summaryRes) || {}
        this._detail = (detailRes.data || detailRes) || {}
        this.loadSavedState()
        this.status = 'normal'
      } catch (e) {
        this.status = 'error'
      } finally {
        this._loaded = true
      }
    },

    // 回显当天已保存内容；未保存时用前一天手动项预填 3/5
    loadSavedState() {
      const detail = this._detail || {}
      const today = detail.today || null
      const ym = detail.yesterdayManual || {}
      this.isSaved = false
      this.savedHint = ''
      if (!today) {
        // 当天还没保存过：3/5 预填前一天（只预填空位，避免覆盖用户已输入内容）
        if (this.mode === 'morning') {
          if (!this.form.answerNewcomer) this.form.answerNewcomer = ym.answerNewcomer || ''
          if (!this.form.answerAtmos) this.form.answerAtmos = ym.answerAtmos || ''
        }
        return
      }
      // 回显已保存内容
      if (this.mode === 'morning') {
        this.isSaved = !!today.morningText
        if (this.isSaved) {
          this.form.managerName = today.morningManagerName || this.form.managerName
          this.form.answerNewcomer = today.morningAnswerNewcomer || ''
          this.form.answerAtmos = today.morningAnswerAtmos || ''
          this.form.morningIssue = today.morningIssue || ''
          this.savedHint = `已于 ${String(today.morningTime || '').slice(0, 16)} 提交过上午汇报，修改后再次保存将覆盖`
        } else {
          if (!this.form.answerNewcomer) this.form.answerNewcomer = ym.answerNewcomer || ''
          if (!this.form.answerAtmos) this.form.answerAtmos = ym.answerAtmos || ''
        }
      } else {
        this.isSaved = !!today.fullText
        if (this.isSaved) {
          this.form.managerName = today.fullManagerName || this.form.managerName
          this.form.supervisor = today.fullSupervisor || ''
          this.form.referralCount = today.fullReferralCount == null ? '' : String(today.fullReferralCount)
          this.form.flyerCount = today.fullFlyerCount == null ? '' : String(today.fullFlyerCount)
          this.form.fullIssue = today.fullIssue || ''
          this.savedHint = `已于 ${String(today.fullTime || '').slice(0, 16)} 提交过全天汇报，修改后再次保存将覆盖`
        }
      }
    },

    // === 保存 + 复制 ===
    async onCopy() {
      if (this.copying) return
      this.copying = true
      try {
        // 1) 先保存（含快照与最终文本）
        const f = this.form
        const payload = { type: this.mode, reportDate: this.currentDate }
        if (this.mode === 'morning') {
          payload.morningManagerName = (f.managerName || '').trim()
          payload.morningAnswerNewcomer = (f.answerNewcomer || '').trim()
          payload.morningAnswerAtmos = (f.answerAtmos || '').trim()
          payload.morningIssue = (f.morningIssue || '').trim()
          payload.morningYesterdayTotal = this.summary.yesterdayTotal ?? 0
          payload.morningTodayAmTotal = this.summary.todayMorningTotal ?? 0
          payload.morningText = this.reportText
        } else {
          payload.fullManagerName = (f.managerName || '').trim()
          payload.fullSupervisor = (f.supervisor || '').trim()
          payload.fullReferralCount = (f.referralCount === '' || f.referralCount == null) ? 0 : Number(f.referralCount)
          payload.fullFlyerCount = (f.flyerCount === '' || f.flyerCount == null) ? 0 : Number(f.flyerCount)
          payload.fullIssue = (f.fullIssue || '').trim()
          payload.fullYesterdayTotal = this.summary.yesterdayTotal ?? 0
          payload.fullTodayTotal = this.summary.todayTotal ?? 0
          payload.fullDiff = this.summary.diff ?? 0
          payload.fullText = this.reportText
        }
        try {
          await request({ url: '/member/experience/report/save', method: 'POST', data: payload })
        } catch (e) {
          // 保存失败：明确提示保存环节，透出真实原因
          const msg = (e && (e.msg || e.message || e.errMsg)) || '保存失败，请重试'
          uni.showToast({ title: `保存失败：${msg}`, icon: 'none' })
          return
        }

        // 2) 复制到剪贴板（失败不影响已保存结果，提供手动兜底）
        try {
          await new Promise((resolve, reject) => {
            uni.setClipboardData({
              data: this.reportText,
              success: resolve,
              fail: reject
            })
          })
        } catch (e) {
          const reason = (e && (e.errMsg || e.msg || e.message)) || ''
          this.isSaved = true
          this.savedHint = '已保存。自动复制失败，请长按下方预览文字手动复制'
          uni.showModal({
            title: '已保存，复制失败',
            content: `汇报已保存成功，但自动复制到剪贴板失败${reason ? '（' + reason + '）' : ''}。请长按预览区的文字，手动选择复制。`,
            showCancel: false,
            confirmText: '知道了'
          })
          return
        }
        this.isSaved = true
        this.savedHint = '已保存并复制，去微信粘贴给领导吧'
        uni.showToast({ title: '已复制汇报文字', icon: 'success' })
      } finally {
        this.copying = false
      }
    }
  }
}
</script>

<style scoped>
.page{display:flex;flex-direction:column;height:100vh;background:#f5f7fa}
.hero{display:flex;align-items:center;justify-content:space-between;padding:34rpx 30rpx 20rpx;background:linear-gradient(135deg,#1687f5,#5aa9e8)}
.eyebrow{display:block;font-size:22rpx;color:rgba(255,255,255,.75);letter-spacing:2rpx}
.hero-title{display:block;margin-top:6rpx;font-size:40rpx;font-weight:700;color:#fff}
.hero-btn{padding:12rpx 26rpx;border-radius:32rpx;background:rgba(255,255,255,.2)}
.hero-btn-text{font-size:24rpx;color:#fff}

.date-bar{margin:20rpx 24rpx 0}
.date-picker{display:flex;align-items:center;padding:20rpx 24rpx;border-radius:16rpx;background:#fff;box-shadow:0 2rpx 10rpx rgba(22,42,84,.06)}
.date-icon{margin-right:12rpx;font-size:28rpx}
.date-value{font-size:30rpx;font-weight:600;color:#1a2332}
.date-hint{margin-left:auto;font-size:22rpx;color:#94a3b8}

.mode-tabs{display:flex;gap:16rpx;margin:20rpx 24rpx 0}
.mode-tab{flex:1;display:flex;flex-direction:column;align-items:center;padding:20rpx 0;border-radius:16rpx;background:#fff;box-shadow:0 2rpx 10rpx rgba(22,42,84,.06);border:3rpx solid transparent}
.mode-tab.active{border-color:#1687f5;background:#f0f7ff}
.mode-tab-title{font-size:30rpx;font-weight:700;color:#1a2332}
.mode-tab.active .mode-tab-title{color:#1687f5}
.mode-tab-sub{margin-top:6rpx;font-size:20rpx;color:#94a3b8}

.scroll{flex:1;margin-top:20rpx;padding:0 24rpx;box-sizing:border-box}
.auto-card{background:#fff;border-radius:16rpx;padding:24rpx;box-shadow:0 2rpx 10rpx rgba(22,42,84,.06)}
.auto-card-title{display:flex;align-items:center;font-size:26rpx;font-weight:600;color:#1a2332}
.auto-card-mark{width:8rpx;height:28rpx;border-radius:4rpx;background:linear-gradient(180deg,#1687f5,#5aa9e8);margin-right:14rpx}
.auto-grid{display:flex;margin-top:20rpx}
.auto-item{flex:1;display:flex;flex-direction:column;align-items:center}
.auto-num{font-size:52rpx;font-weight:800;color:#1687f5}
.auto-num-down{color:#ef4444}
.auto-label{margin-top:6rpx;font-size:24rpx;color:#64748b}
.auto-saved-hint{margin-top:16rpx;font-size:22rpx;color:#16a34a}

.form-card{margin-top:20rpx;background:#fff;border-radius:16rpx;padding:24rpx;box-shadow:0 2rpx 10rpx rgba(22,42,84,.06)}
.form-title{display:flex;align-items:center;font-size:26rpx;font-weight:600;color:#1a2332;margin-bottom:8rpx}
.form-row{display:flex;align-items:center;padding:18rpx 0;border-bottom:1rpx solid #f1f5f9}
.form-row-col{flex-direction:column;align-items:stretch}
.form-row-inline{gap:20rpx}
.form-inline-item{flex:1;display:flex;align-items:center}
.form-label{width:220rpx;font-size:26rpx;color:#475569;flex-shrink:0}
.form-row-col .form-label{width:auto;margin-bottom:12rpx}
.form-inline-item .form-label{width:150rpx;font-size:24rpx}
.form-input{flex:1;height:64rpx;padding:0 20rpx;border-radius:12rpx;background:#f5f7fa;font-size:28rpx;color:#1a2332}
.form-textarea{width:100%;min-height:120rpx;padding:16rpx 20rpx;border-radius:12rpx;background:#f5f7fa;font-size:26rpx;color:#1a2332;box-sizing:border-box}

.preview-card{margin-top:20rpx;background:#fff;border-radius:16rpx;padding:24rpx;box-shadow:0 2rpx 10rpx rgba(22,42,84,.06)}
.preview-body{margin-top:16rpx;padding:20rpx;border-radius:12rpx;background:#f8fafc;border:1rpx solid #e2e8f0}
.preview-text{font-size:26rpx;line-height:1.7;color:#1a2332;white-space:pre-wrap;word-break:break-all}

.bottom-space{height:140rpx}
.bottom-bar{position:fixed;left:0;right:0;bottom:0;padding:16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));background:rgba(245,247,250,.95);border-top:1rpx solid #e2e8f0}
.copy-btn{height:88rpx;line-height:88rpx;border-radius:44rpx;background:linear-gradient(135deg,#1687f5,#5aa9e8);color:#fff;font-size:30rpx;font-weight:600;border:none}
.copy-btn-disabled{opacity:.6}
</style>
