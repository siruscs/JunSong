<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view>
          <text class="hero-title">功能清单</text>
          <text class="hero-date">{{ currentDate }}</text>
        </view>
        <view class="hero-count">
          <text class="hero-count-num">{{ totalAuthorized }}</text>
          <text class="hero-count-label">个入口</text>
        </view>
      </view>
      <view class="hero-note">
        <text>按 PC 端业务分组同步展示，当前只显示你有权限操作的功能。</text>
      </view>
      <view class="hero-stats">
        <view class="hero-stat" v-for="(stat, idx) in quickStats" :key="idx">
          <text class="hero-stat-num">{{ stat.value }}</text>
          <text class="hero-stat-label">{{ stat.label }}</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="scroll" v-if="authorizedGroups.length || showMemberGrowthSection">
      <view class="section" v-for="group in authorizedGroups" :key="group.name">
        <view class="section-header">
          <view>
            <text class="section-title">{{ group.name }}</text>
            <text class="section-sub">{{ group.items.length }} 个功能</text>
          </view>
          <view class="section-mark" :style="{ background: getGroupColor(group.name) }"></view>
        </view>
        <view class="grid">
          <view
            class="tile"
            :class="{ featured: isFeatured(item.key) }"
            hover-class="tile--active"
            v-for="item in group.items"
            :key="item.key"
            @tap="openModule(item.key)"
          >
            <view class="tile-icon" :style="{ background: getModuleBg(item.key) }">
              <text class="tile-icon-text">{{ getModuleLetter(item.key) }}</text>
            </view>
            <text class="tile-title">{{ item.title }}</text>
            <text class="tile-desc">{{ getModuleDesc(item.key) }}</text>
          </view>
        </view>
      </view>

      <!-- 会员运营快捷入口（R1-R25 同步） -->
      <view class="section" v-if="showMemberGrowthSection">
        <view class="section-header">
          <view>
            <text class="section-title">会员运营</text>
            <text class="section-sub">{{ memberGrowthEntries.length }} 个功能</text>
          </view>
          <view class="section-mark" style="background:#8B5CF6"></view>
        </view>
        <view class="grid">
          <view
            class="tile"
            hover-class="tile--active"
            v-for="entry in memberGrowthEntries"
            :key="entry.key"
            @tap="openMemberPage(entry.key)"
          >
            <view class="tile-icon" :style="{ background: entry.bg }">
              <text class="tile-icon-text">{{ entry.letter }}</text>
            </view>
            <text class="tile-title">{{ entry.title }}</text>
            <text class="tile-desc">{{ entry.desc }}</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="empty" v-else>
      <view class="empty-mark">权</view>
      <text class="empty-title">暂无可用功能</text>
      <text class="empty-sub">请确认账号已分配小程序模块权限，或重新登录刷新权限。</text>
    </view>
  </view>
</template>

<script>
import { groups, modules } from '@/config/modules.js'
import { filterAuthorizedGroups, hasModulePermission } from '@/utils/permission.js'

const MODULE_BG = {
  member: 'rgba(42,111,151,0.08)', pointsGoods: 'rgba(59,130,246,0.08)', pointsRule: 'rgba(20,184,166,0.08)', pointsRecord: 'rgba(42,111,151,0.08)',
  pointsExchange: 'rgba(139,92,246,0.08)', seckill: 'rgba(249,115,22,0.08)', seckillRecord: 'rgba(234,88,12,0.08)',
  expense: 'rgba(239,68,68,0.08)', advance: 'rgba(139,92,246,0.08)', product: 'rgba(59,130,246,0.08)',
  supplier: 'rgba(107,114,128,0.08)', purchase: 'rgba(249,115,22,0.08)', sale: 'rgba(16,185,129,0.08)',
  investorPayment: 'rgba(236,72,153,0.08)', investor: 'rgba(14,165,233,0.08)', investRecord: 'rgba(16,185,129,0.08)',
  deptProfitConfig: 'rgba(107,114,128,0.08)', accountingPeriod: 'rgba(245,158,11,0.08)', profitShare: 'rgba(244,63,94,0.08)',
  costAccounting: 'rgba(6,182,212,0.08)', verificationRecord: 'rgba(59,130,246,0.08)', userManage: 'rgba(99,102,241,0.08)', deptManage: 'rgba(34,197,94,0.08)',
  wfTodo: 'rgba(16,185,129,0.08)', wfDone: 'rgba(16,185,129,0.08)', wfNotify: 'rgba(16,185,129,0.08)'
}

const MODULE_LETTER = {
  member: '👤', pointsGoods: '🎁', pointsRule: '📋', pointsRecord: '📝', pointsExchange: '🔄',
  seckill: '⚡', seckillRecord: '🏃', expense: '💰', advance: '💵',
  product: '📦', supplier: '🏪', purchase: '🛒', sale: '📈',
  investorPayment: '💸', investor: '🤝', investRecord: '💎',
  deptProfitConfig: '⚙️', accountingPeriod: '📅', profitShare: '📊',
  costAccounting: '🧮', verificationRecord: '✅', userManage: '👥', deptManage: '🏢',
  wfTodo: '📥', wfDone: '📤', wfNotify: '🔔'
}

const MODULE_ICON_COLOR = {
  member: '#2A6F97', pointsGoods: '#3B82F6', pointsRule: '#0F766E', pointsRecord: '#2A6F97',
  pointsExchange: '#8B5CF6', seckill: '#F97316', seckillRecord: '#EA580C',
  expense: '#EF4444', advance: '#8B5CF6', product: '#3B82F6',
  supplier: '#6B7280', purchase: '#F97316', sale: '#10B981',
  investorPayment: '#EC4899', investor: '#0EA5E9', investRecord: '#10B981',
  deptProfitConfig: '#6B7280', accountingPeriod: '#F59E0B', profitShare: '#F43F5E',
  costAccounting: '#06B6D4', verificationRecord: '#3B82F6', userManage: '#6366F1', deptManage: '#22C55E',
  wfTodo: '#10B981', wfDone: '#10B981', wfNotify: '#10B981'
}

const MODULE_DESC = {
  member: '会员建档、状态维护',
  pointsGoods: '积分礼品和库存',
  pointsRule: '积分计算规则',
  pointsRecord: '积分增减明细',
  pointsExchange: '兑换领取记录',
  seckill: '活动份额与价格',
  seckillRecord: '参与和领取记录',
  expense: '拍照识别和核销',
  advance: '借支登记与状态',
  product: '商品价格和库存',
  supplier: '供应商联系人',
  purchase: '进货单和收货',
  sale: '销售登记和收款',
  investorPayment: '投资人返款记录',
  investor: '投资人档案',
  investRecord: '投资款流水',
  deptProfitConfig: '店长分润比例',
  accountingPeriod: '回本检测和结转',
  profitShare: '分润结转记录',
  costAccounting: '成本核算预览',
  verificationRecord: '费用核销批次记录',
  userManage: '账号和状态维护',
  deptManage: '部门层级和负责人',
  wfTodo: '待办审批任务处理',
  wfDone: '已办审批任务记录',
  wfNotify: '审批消息通知'
}

const GROUP_COLOR = {
  '会员服务': '#2A6F97',
  '财务管理': '#F59E0B',
  '系统管理': '#6366F1',
  '移动办公': '#10B981'
}

const FEATURED_KEYS = ['member', 'expense', 'sale', 'costAccounting', 'accountingPeriod', 'userManage', 'wfTodo']

export default {
  data() {
    return {
      modules: [],
      currentDate: '',
      quickStats: [
        { label: '今日待办', value: 0 },
        { label: '本周新增', value: 0 },
        { label: '已处理', value: 0 }
      ]
    }
  },
  computed: {
    authorizedGroups() {
      return filterAuthorizedGroups(groups, this.modules)
    },
    totalAuthorized() {
      return this.authorizedGroups.reduce((total, group) => total + group.items.length, 0)
    },
    // 会员运营快捷入口分组（R1-R25 同步）
    showMemberGrowthSection() {
      return hasModulePermission('member', this.modules)
    },
    memberGrowthEntries() {
      const entries = [
        { key: 'dashboard', title: '会员运营看板', desc: '会员增长与分层洞察', bg: 'rgba(42,111,151,0.08)', color: '#2A6F97', letter: '📊' },
        { key: 'growth', title: '成长体系', desc: '等级、成长值与签到', bg: 'rgba(139,92,246,0.08)', color: '#8B5CF6', letter: '🌟' },
        { key: 'actions', title: '增长动作', desc: '待执行与已完成动作', bg: 'rgba(14,165,233,0.08)', color: '#0EA5E9', letter: '🎯' }
      ]
      // 有积分记录或积分兑换权限时加入积分运营入口
      if (hasModulePermission('pointsRecord', this.modules) || hasModulePermission('pointsExchange', this.modules)) {
        entries.push({ key: 'points', title: '积分运营', desc: '积分流水与待领取兑换', bg: 'rgba(245,158,11,0.08)', color: '#F59E0B', letter: '🎯' })
      }
      return entries
    }
  },
  onShow() {
    this.modules = uni.getStorageSync('modules') || []
    this.loadQuickStats()
  },
  created() {
    const d = new Date()
    const weekDays = ['日', '一', '二', '三', '四', '五', '六']
    this.currentDate = `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${weekDays[d.getDay()]}`
  },
  methods: {
    loadQuickStats() {
      const tasks = uni.getStorageSync('todoTasks') || []
      const weekStart = new Date()
      weekStart.setDate(weekStart.getDate() - weekStart.getDay())
      const done = tasks.filter(t => t.status === 'done').length
      const pending = tasks.filter(t => t.status !== 'done').length
      const weekNew = tasks.filter(t => t.createTime && t.createTime >= weekStart.toISOString().slice(0, 10)).length
      this.quickStats = [
        { label: '今日待办', value: pending },
        { label: '本周新增', value: weekNew },
        { label: '已处理', value: done }
      ]
    },
    getGroupColor(name) {
      return GROUP_COLOR[name] || '#2A6F97'
    },
    getModuleBg(key) {
      return MODULE_BG[key] || 'rgba(148,163,184,0.08)'
    },
    getModuleLetter(key) {
      return MODULE_LETTER[key] || key.charAt(0).toUpperCase()
    },
    getModuleColor(key) {
      return MODULE_ICON_COLOR[key] || '#94A3B8'
    },
    getModuleDesc(key) {
      return MODULE_DESC[key] || '查看和维护数据'
    },
    isFeatured(key) {
      return FEATURED_KEYS.includes(key)
    },
    openModule(key) {
      if (!hasModulePermission(key, this.modules)) {
        uni.showToast({ title: '暂无该功能权限', icon: 'none' })
        return
      }
      const mod = modules[key]
      if (mod && mod.customPage) {
        uni.navigateTo({ url: mod.customPage })
      } else if (key === 'userManage') {
        uni.navigateTo({ url: '/pages/user/index' })
      } else if (key === 'deptManage') {
        uni.navigateTo({ url: '/pages/dept/index' })
      } else {
        uni.navigateTo({ url: '/pages/list/index?module=' + key })
      }
    },
    // 跳转会员运营子页面（R1-R25 同步）
    openMemberPage(key) {
      if (!hasModulePermission('member', this.modules) && key !== 'points') {
        uni.showToast({ title: '暂无该功能权限', icon: 'none' })
        return
      }
      if (key === 'points') {
        if (!hasModulePermission('pointsRecord', this.modules) && !hasModulePermission('pointsExchange', this.modules)) {
          uni.showToast({ title: '暂无该功能权限', icon: 'none' })
          return
        }
      }
      uni.navigateTo({ url: '/pages/member/' + key })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #EAF2F4;
  overflow: hidden;
}

.hero {
  margin: 16rpx 20rpx 0;
  padding: 20rpx 24rpx;
  background: linear-gradient(135deg, #173B57, #2A6F97);
  border-radius: 20rpx;
  box-shadow: 0 12rpx 32rpx rgba(42, 111, 151, 0.12);
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.hero-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #FFFFFF;
  display: block;
}

.hero-date {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.68);
  margin-top: 4rpx;
  display: block;
}

.hero-count {
  width: 80rpx;
  height: 80rpx;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.12);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-count-num {
  font-size: 28rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.hero-count-label {
  margin-top: 1rpx;
  font-size: 16rpx;
  color: rgba(255, 255, 255, 0.66);
}

.hero-note {
  margin-top: 14rpx;
  padding: 12rpx 16rpx;
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.75);
  font-size: 20rpx;
  line-height: 28rpx;
}

.hero-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 14rpx;
  padding-top: 14rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.12);
}

.hero-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.hero-stat-num {
  font-size: 28rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.hero-stat-label {
  margin-top: 4rpx;
  font-size: 18rpx;
  color: rgba(255, 255, 255, 0.65);
}

.scroll {
  height: calc(100vh - 180rpx);
  padding: 20rpx 20rpx 40rpx;
  box-sizing: border-box;
}

.section {
  margin-bottom: 34rpx;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
  padding: 0 4rpx;
}

.section-mark {
  width: 46rpx;
  height: 10rpx;
  border-radius: 999rpx;
  opacity: 0.85;
}

.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #102A3A;
}

.section-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #708196;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
}

.tile {
  min-height: 200rpx;
  padding: 24rpx 16rpx 20rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 28rpx rgba(42, 111, 151, 0.08);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.tile.featured {
  background: linear-gradient(180deg, #FFFFFF, #F7FBFC);
  border: 1rpx solid rgba(42, 111, 151, 0.1);
}

.tile--active {
  transform: scale(0.96);
  opacity: 0.8;
}

.tile-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;
}

.tile-icon-text {
  font-size: 48rpx;
  line-height: 1;
}

.tile-title {
  display: block;
  font-size: 24rpx;
  font-weight: 700;
  color: #1A2332;
  line-height: 34rpx;
}

.tile-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  line-height: 28rpx;
  color: #94A3B8;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.empty {
  margin: 120rpx 44rpx 0;
  padding: 52rpx 38rpx;
  border-radius: 28rpx;
  background: #FFFFFF;
  text-align: center;
  box-shadow: 0 12rpx 36rpx rgba(42, 111, 151, 0.08);
}

.empty-mark {
  width: 96rpx;
  height: 96rpx;
  line-height: 96rpx;
  margin: 0 auto 22rpx;
  border-radius: 28rpx;
  background: #ECF4F7;
  color: #2A6F97;
  font-size: 36rpx;
  font-weight: 800;
}

.empty-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #102A3A;
}

.empty-sub {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 36rpx;
  color: #708196;
}
</style>
