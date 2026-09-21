<template>
  <view class="page">
    <!-- Hero -->
    <view class="hero">
      <view>
        <text class="eyebrow">会员服务</text>
        <text class="hero-title">体验人数录入</text>
      </view>
      <view class="hero-btn" @tap="goReport">
        <text class="hero-btn-text">生成汇报</text>
      </view>
    </view>

    <!-- 日期切换 -->
    <view class="date-bar">
      <view class="date-nav" @tap="prevDay">
        <text class="nav-chevron">‹</text>
        <text class="nav-label">前一天</text>
      </view>
      <picker mode="date" :value="currentDate" @change="onDateChange">
        <view class="date-picker">
          <text class="date-icon">📅</text>
          <text class="date-value">{{ displayDate }}</text>
          <text class="weekday">{{ weekday }}</text>
        </view>
      </picker>
      <view class="date-nav" @tap="nextDay">
        <text class="nav-label">后一天</text>
        <text class="nav-chevron">›</text>
      </view>
    </view>

    <!-- 顶部汇总 -->
    <view class="summary-card" v-if="!loading">
      <view class="summary-main">
        <text class="summary-number">{{ view.totalExperience || 0 }}</text>
        <text class="summary-label">今日体验总人数</text>
      </view>
      <view class="summary-side">
        <text class="summary-side-top">已录 {{ view.filledCount || 0 }} / {{ view.sessionCount || 0 }} 场</text>
        <text class="summary-side-hint" v-if="view.noConfig">⚠️ 请先在 PC 端配置体验场次</text>
      </view>
    </view>

    <!-- 场次录入区 -->
    <scroll-view scroll-y class="scroll" refresher-enabled
                 :refresher-triggered="refreshing" @refresherrefresh="loadData">
      <!-- 加载中 -->
      <view v-if="loading" class="empty-state">
        <text class="empty-hint">加载中…</text>
      </view>

      <!-- 未配置场次 -->
      <view v-else-if="view.noConfig" class="empty-state">
        <text class="empty-icon">⏰</text>
        <text class="empty-title">暂无场次配置</text>
        <text class="empty-hint">请在 PC 端「会员服务 → 体验场次配置」先配置每日场次</text>
      </view>

      <!-- 场次列表 -->
      <view v-else class="session-list">
        <view class="session-card" v-for="s in view.sessions" :key="s.sessionNo">
          <view class="session-no-col">
            <text class="session-no">第{{ s.sessionNo }}场</text>
          </view>
          <view class="session-time-col">
            <text class="session-time">{{ s.timeStart }} - {{ s.timeEnd }}</text>
          </view>
          <view class="session-input-col">
            <input class="session-input" type="number" maxlength="5"
                   :value="inputValues[s.sessionNo] ?? ''"
                   placeholder="0"
                   @input="e => onInput(s.sessionNo, e.detail.value)" />
            <text class="session-unit">人</text>
          </view>
        </view>

        <!-- 已汇总数字（用户填写时前端实时累加，让他看到实时效果） -->
        <view class="live-total" v-if="dirtyCount > 0">
          <text class="live-total-label">本次未保存合计</text>
          <text class="live-total-value">{{ liveTotal }}</text>
          <text class="live-total-save-hint">（点击下方保存后正式入库）</text>
        </view>
      </view>
    </scroll-view>

    <!-- 底部保存按钮 -->
    <view class="bottom-bar" v-if="!view.noConfig">
      <button class="save-btn" :class="{ 'save-btn-disabled': dirtyCount === 0 || saving }"
              :disabled="dirtyCount === 0 || saving"
              @tap="onSave">
        <text v-if="saving">保存中…</text>
        <text v-else>💾 保存今日场次（变更 {{ dirtyCount }} 场）</text>
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
const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

export default {
  name: 'ExperienceRecord',
  components: { StateView },
  data() {
    const today = formatYMD(new Date())
    return {
      currentDate: today,          // yyyy-MM-dd
      view: { sessions: [], totalExperience: 0, filledCount: 0, sessionCount: 0, noConfig: false },
      inputValues: {},             // { sessionNo: number_string }
      originalValues: {},          // 保存前快照，用于对比 dirty
      loading: false,
      refreshing: false,
      saving: false
    }
  },

  computed: {
    displayDate() { return this.currentDate },
    weekday() {
      const d = new Date(this.currentDate)
      return WEEKDAYS[d.getDay()] || ''
    },
    dirtyCount() {
      let cnt = 0
      for (const sn in this.inputValues) {
        const cur = this.inputValues[sn] === '' ? null : Number(this.inputValues[sn])
        const old = this.originalValues[sn] ?? null
        // '' 和 null 视为一致
        const curNorm = cur === null ? null : cur
        const oldNorm = old === null ? null : old
        if (curNorm !== oldNorm) cnt++
      }
      return cnt
    },
    liveTotal() {
      // 用 "已保存的总量 - 已保存场次贡献 + 当前输入贡献" 来算
      let base = 0; let changed = 0
      for (const s of this.view.sessions) {
        const savedVal = s.experienceCount ?? 0
        base += savedVal
        const cur = this.inputValues[s.sessionNo]
        const curNum = (cur === undefined || cur === '') ? null : Number(cur)
        const orig = this.originalValues[s.sessionNo] ?? null
        // 如果已改了，替换 base 里的贡献；如果没改，用 base 原值
        if (curNum !== orig) {
          changed += curNum === null ? 0 : curNum
        } else {
          changed += savedVal
        }
      }
      return changed
    }
  },

  onLoad() {
    this.loadData()
  },

  methods: {
    goReport() {
      uni.navigateTo({ url: '/pages/experience-report/index' })
    },

    // === 日期 ===
    prevDay() {
      const d = new Date(this.currentDate); d.setDate(d.getDate() - 1)
      this.currentDate = formatYMD(d); this.loadData()
    },
    nextDay() {
      const d = new Date(this.currentDate); d.setDate(d.getDate() + 1)
      this.currentDate = formatYMD(d); this.loadData()
    },
    onDateChange(e) {
      if (e.detail.value === this.currentDate) return
      this.currentDate = e.detail.value
      this.loadData()
    },

    // === 数据加载 ===
    async loadData() {
      this.loading = true; this.refreshing = true
      try {
        const res = await request({
          url: '/member/experience/dailyRecord/byDate',
          method: 'GET',
          data: { date: this.currentDate }
        })
        const v = res.data || res || {}
        this.view = {
          sessions: v.sessions || [],
          totalExperience: v.totalExperience || 0,
          filledCount: v.filledCount || 0,
          sessionCount: v.sessionCount || 0,
          noConfig: !!v.noConfig
        }
        // 把 view.sessions 的值同步到 inputValues / originalValues
        const inputs = {}; const original = {}
        for (const s of this.view.sessions) {
          inputs[s.sessionNo] = (s.experienceCount == null ? '' : String(s.experienceCount))
          original[s.sessionNo] = s.experienceCount == null ? null : s.experienceCount
        }
        this.inputValues = inputs
        this.originalValues = original
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false; this.refreshing = false
      }
    },

    // === 输入 ===
    onInput(sessionNo, value) {
      // 只允许数字，去掉非数字
      const cleaned = String(value).replace(/[^\d]/g, '')
      this.inputValues[sessionNo] = cleaned
    },

    // === 保存 ===
    async onSave() {
      if (this.saving) return
      // 只提交已变更的场次
      const records = []
      for (const s of this.view.sessions) {
        const sn = s.sessionNo
        const curRaw = this.inputValues[sn]
        const curNum = (curRaw === undefined || curRaw === '') ? null : Number(curRaw)
        const orig = this.originalValues[sn] ?? null
        if (curNum !== orig) {
          records.push({ sessionNo: sn, experienceCount: curNum === null ? 0 : curNum })
        }
      }
      if (records.length === 0) {
        uni.showToast({ title: '没有变更', icon: 'none' }); return
      }

      this.saving = true
      try {
        const res = await request({
          url: '/member/experience/dailyRecord/batchSave',
          method: 'POST',
          data: { recordDate: this.currentDate, records }
        })
        if (res.code === 200) {
          uni.showToast({ title: `已保存 ${res.saved || records.length} 场`, icon: 'success' })
          // 重新拉取最新数据
          this.originalValues = {}  // 清一下让 loadData 重置
          await this.loadData()
        } else {
          uni.showToast({ title: res.msg || '保存失败', icon: 'none' })
        }
      } catch (e) {
        uni.showToast({ title: '保存失败', icon: 'none' })
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
/* ---- 复用的 work-scope / hero 样式，来自 list/index.vue ---- */
.page { min-height: 100vh; background: #F5F7FA; padding-bottom: 160rpx; }
.hero { padding: 40rpx 32rpx 20rpx; display: flex; justify-content: space-between; align-items: center; }
.eyebrow { font-size: 24rpx; color: #8A94A6; display: block; margin-bottom: 4rpx; }
.hero-title { font-size: 44rpx; font-weight: 700; color: #1A2332; display: block; }
.hero-btn { padding: 14rpx 28rpx; border-radius: 32rpx; background: #E8F3FF; }
.hero-btn-text { font-size: 26rpx; color: #1687F5; font-weight: 600; }

/* ---- 日期切换 ---- */
.date-bar {
  display: flex; align-items: center; justify-content: space-between;
  margin: 0 32rpx 20rpx; padding: 20rpx 24rpx;
  background: #FFFFFF; border-radius: 16rpx; border: 1rpx solid #E5EAF2;
}
.date-nav { display: flex; align-items: center; padding: 8rpx 20rpx; }
.nav-chevron { font-size: 40rpx; color: #087CF0; font-weight: 700; }
.nav-label { font-size: 24rpx; color: #087CF0; margin: 0 8rpx; }
.date-picker { display: flex; align-items: center; }
.date-icon { font-size: 32rpx; margin-right: 8rpx; }
.date-value { font-size: 32rpx; font-weight: 700; color: #1A2332; margin-right: 12rpx; }
.weekday { font-size: 24rpx; color: #8A94A6; }

/* ---- 顶部汇总 ---- */
.summary-card {
  display: flex; align-items: center; justify-content: space-between;
  margin: 0 32rpx 20rpx; padding: 28rpx 32rpx;
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  border-radius: 20rpx; color: #FFFFFF;
  box-shadow: 0 8rpx 24rpx rgba(8, 124, 240, 0.25);
}
.summary-main { display: flex; align-items: baseline; }
.summary-number { font-size: 72rpx; font-weight: 800; line-height: 1; margin-right: 12rpx; }
.summary-label { font-size: 26rpx; opacity: 0.9; }
.summary-side { text-align: right; }
.summary-side-top { font-size: 28rpx; font-weight: 600; display: block; }
.summary-side-hint { font-size: 22rpx; opacity: 0.8; display: block; margin-top: 4rpx; }

/* ---- 场次卡片列表 ---- */
.scroll { height: calc(100vh - 460rpx); padding: 0 32rpx; }
.session-list { padding-bottom: 40rpx; }
.session-card {
  display: flex; align-items: center;
  background: #FFFFFF; border-radius: 16rpx;
  padding: 24rpx 28rpx; margin-bottom: 16rpx;
  border: 1rpx solid #E5EAF2;
}
.session-no-col { width: 110rpx; flex-shrink: 0; }
.session-no { font-size: 26rpx; color: #087CF0; font-weight: 700; }
.session-time-col { flex: 1; min-width: 0; }
.session-time { font-size: 28rpx; color: #1A2332; font-weight: 600; }
.session-input-col { display: flex; align-items: center; width: 250rpx; flex-shrink: 0; }
.session-input {
  width: 180rpx; height: 80rpx; line-height: 80rpx; text-align: center;
  font-size: 38rpx; font-weight: 700; color: #1A2332;
  padding: 0 12rpx; background: #F5F7FA; border-radius: 10rpx;
  border: 1rpx solid transparent;
  box-sizing: border-box;
}
.session-input:focus { border-color: #087CF0; background: #FFFFFF; }
.session-unit { font-size: 26rpx; color: #8A94A6; margin-left: 8rpx; }

/* ---- 实时合计 ---- */
.live-total {
  display: flex; align-items: center; justify-content: space-between;
  background: #FFF7ED; border-radius: 12rpx; padding: 20rpx 24rpx;
  border: 1rpx solid #FCD34D; margin-top: 16rpx;
}
.live-total-label { font-size: 26rpx; color: #92400E; }
.live-total-value { font-size: 40rpx; font-weight: 800; color: #B45309; }
.live-total-save-hint { font-size: 22rpx; color: #B45309; opacity: 0.8; }

/* ---- 空态 ---- */
.empty-state {
  text-align: center; padding: 120rpx 40rpx;
}
.empty-icon { font-size: 80rpx; display: block; margin-bottom: 24rpx; }
.empty-title { font-size: 32rpx; color: #1A2332; font-weight: 600; display: block; margin-bottom: 12rpx; }
.empty-hint { font-size: 26rpx; color: #8A94A6; display: block; line-height: 1.6; }

/* ---- 底部保存 ---- */
.bottom-bar {
  position: fixed; bottom: 0; left: 0; right: 0;
  padding: 20rpx 32rpx; padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #FFFFFF; border-top: 1rpx solid #E5EAF2;
  box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.06);
}
.save-btn {
  width: 100%; height: 88rpx; line-height: 88rpx;
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF; font-size: 30rpx; font-weight: 700;
  border-radius: 44rpx; border: none;
  box-shadow: 0 6rpx 16rpx rgba(8, 124, 240, 0.3);
}
.save-btn-disabled {
  background: #C8CDD6; color: #FFFFFF;
  box-shadow: none; opacity: 0.7;
}
</style>
