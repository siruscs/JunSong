<template>
  <view class="page">
    <view class="bg-decor">
      <view class="halo halo-a"></view>
      <view class="halo halo-b"></view>
      <view class="ledger-line l1"></view>
      <view class="ledger-line l2"></view>
    </view>

    <view class="content" :style="{ paddingTop: (statusBarHeight + 36) + 'px' }">
      <view class="brand anim-fade-up">
        <view class="brand-mark">
          <image class="brand-logo-img" src="/static/logo.png" mode="aspectFit" />
        </view>
        <view class="brand-copy">
          <text class="brand-name">松·云助手</text>
          <text class="brand-sub">把今日经营收进掌心</text>
        </view>
      </view>

      <view class="daily-card anim-fade-up" style="animation-delay: 0.08s">
        <view class="daily-main">
          <text class="daily-title">登录后继续处理会员、费用和销售。</text>
          <text class="daily-sub">部门权限会自动带入，只展示你负责的门店功能。</text>
        </view>
        <view class="daily-chip">
          <text>移动运营</text>
        </view>
      </view>

      <view class="form-card anim-slide-up" style="animation-delay: 0.16s">
        <view class="form-head">
          <text class="form-title">账号登录</text>
        </view>

        <view class="form-item">
          <view class="input-wrap">
            <view class="input-icon-wrap"><text class="input-icon-text">人</text></view>
            <input class="input-field" v-model="form.username" placeholder="请输入用户名" placeholder-class="input-placeholder" />
          </view>
        </view>
        <view class="form-item">
          <view class="input-wrap">
            <view class="input-icon-wrap"><text class="input-icon-text">密</text></view>
            <input class="input-field" v-model="form.password" password placeholder="请输入密码" placeholder-class="input-placeholder" />
          </view>
        </view>

        <button class="btn-login" hover-class="btn-login--active" :disabled="loading" @tap="handleLogin">
          <text v-if="!loading">进入工作台</text>
          <text v-else>{{ loadingText }}</text>
        </button>

        <view class="trust-row">
          <view class="trust-item">
            <text class="trust-num">会员</text>
            <text class="trust-label">建档积分</text>
          </view>
          <view class="trust-item">
            <text class="trust-num">财务</text>
            <text class="trust-label">费用销售</text>
          </view>
          <view class="trust-item">
            <text class="trust-num">权限</text>
            <text class="trust-label">按店隔离</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 部门选择弹窗 -->
    <view v-if="showDeptPicker" class="dept-modal-mask" @tap="cancelDeptPick">
      <view class="dept-modal" @tap.stop>
        <view class="dept-modal-head">
          <text class="dept-modal-title">选择门店</text>
          <text class="dept-modal-sub">您有多个门店，请选择要进入的部门</text>
        </view>
        <scroll-view scroll-y class="dept-list">
          <view
            v-for="(dept, idx) in depts"
            :key="dept.deptId || dept.id || idx"
            class="dept-list-item"
            :class="{ active: String(dept.deptId || dept.id) === String(selectedDeptId) }"
            @tap="pickDept(dept)"
          >
            <view class="dept-item-mark"><text class="dept-item-mark-text">店</text></view>
            <view class="dept-item-body">
              <text class="dept-item-name">{{ dept.deptName || dept.name }}</text>
              <text v-if="dept.leader" class="dept-item-meta">{{ dept.leader }}</text>
            </view>
            <view v-if="String(dept.deptId || dept.id) === String(selectedDeptId)" class="dept-item-check">✓</view>
          </view>
        </scroll-view>
        <view class="dept-modal-foot">
          <button class="btn-secondary" @tap="cancelDeptPick">取消</button>
          <button class="btn-primary" :disabled="!selectedDeptId" @tap="confirmDept">确认进入</button>
        </view>
      </view>
    </view>

    <view class="safe-bottom"></view>
  </view>
</template>

<script>
import { request, setToken, getToken, getBaseUrl } from '@/api/index.js'

export default {
  data() {
    return {
      statusBarHeight: 0,
      form: {
        username: '',
        password: ''
      },
      depts: [],
      deptIndex: 0,
      selectedDeptId: null,
      showDeptPicker: false,
      tempToken: null,
      loading: false,
      loadingText: '正在登录...',
      connStatus: 'idle',
      connText: '点击检测后端连接'
    }
  },
  onLoad() {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 20
    this.testConnection()
  },
  methods: {
    async testConnection() {
      this.connStatus = 'testing'
      this.connText = '正在检测连接...'
      try {
        const [err, res] = await new Promise((resolve) => {
          uni.request({
            url: getBaseUrl() + '/auth/mp/login',
            method: 'POST',
            data: {},
            header: { 'Content-Type': 'application/json' },
            complete: (r) => resolve([null, r]),
            fail: (e) => resolve([e, null])
          })
        })
        if (res && res.statusCode) {
          this.connStatus = 'ok'
          this.connText = '后端连接正常 · ' + getBaseUrl()
        } else {
          this.connStatus = 'fail'
          this.connText = '连接失败 · 点击重试或检查设置'
        }
      } catch (e) {
        this.connStatus = 'fail'
        this.connText = '连接失败 · 点击重试或检查设置'
      }
    },
    pickDept(dept) {
      this.selectedDeptId = dept.deptId || dept.id
    },
    cancelDeptPick() {
      this.showDeptPicker = false
      this.loading = false
    },
    async confirmDept() {
      if (!this.selectedDeptId) {
        uni.showToast({ title: '请选择门店', icon: 'none' })
        return
      }
      this.loading = true
      this.loadingText = '切换门店中...'
      try {
        await request({
          url: '/system/user/switchDept/' + this.selectedDeptId,
          method: 'POST',
          noRedirect: true
        })
        this.showDeptPicker = false
        await this.completeLogin()
      } catch (e) {
        console.error('切换部门失败', e)
        this.loading = false
      }
    },
    async completeLogin() {
      try {
        const userRes = await request({ url: '/member/mp/userinfo', method: 'GET' })
        const userInfo = userRes.data || userRes
        uni.setStorageSync('userInfo', userInfo)
        uni.setStorageSync('modules', userInfo.modules || [])
        uni.setStorageSync('permissions', userInfo.permissions || [])
      } catch (e) {
        console.log('获取用户信息失败', e)
        uni.setStorageSync('userInfo', { username: this.form.username, deptId: this.selectedDeptId })
        uni.setStorageSync('modules', [])
        uni.setStorageSync('permissions', [])
      }

      uni.showToast({ title: '登录成功' })
      setTimeout(() => {
        uni.reLaunch({ url: '/pages/index/index' })
      }, 500)
      this.loading = false
    },
    async handleLogin() {
      if (!this.form.username) {
        uni.showToast({ title: '请输入用户名', icon: 'none' })
        return
      }
      if (!this.form.password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }
      this.loading = true
      this.loadingText = '正在登录...'
      setToken('')
      uni.removeStorageSync('userInfo')
      uni.removeStorageSync('modules')
      uni.removeStorageSync('permissions')

      try {
        // 第一步：用户名密码登录（小程序专用接口 /auth/mp/login，已在网关白名单）
        const loginRes = await request({
          url: '/auth/mp/login',
          method: 'POST',
          noRedirect: true,
          data: {
            username: this.form.username,
            password: this.form.password
          }
        })
        const tokenData = loginRes.data || loginRes
        const accessToken = tokenData.access_token
        if (!accessToken) {
          uni.showToast({ title: '登录返回数据异常', icon: 'none' })
          console.error('登录响应:', JSON.stringify(loginRes))
          this.loading = false
          return
        }
        setToken(accessToken)
        this.tempToken = accessToken

        // 第二步：用已获取的 token 调 /system/user/getInfo（含部门列表）
        this.loadingText = '加载用户信息...'
        try {
          const infoRes = await request({
            url: '/system/user/getInfo',
            method: 'GET',
            noRedirect: true
          })
          const info = infoRes || infoRes.data || {}
          const deptList = (info.depts || info.data?.depts || info.user?.depts || [])
          const currentDeptId = info.currentDeptId || info.data?.currentDeptId || info.user?.deptId
          const userObj = info.user || info.data?.user || {}

          // 保存用户基础信息
          const baseInfo = {
            username: this.form.username,
            nickName: userObj.nickName || userObj.userName || this.form.username,
            deptId: currentDeptId,
            currentDeptId: currentDeptId
          }
          uni.setStorageSync('userInfo', baseInfo)

          // 判断部门数量：单部门直接进入，多部门弹窗选择
          if (deptList.length <= 1) {
            if (deptList.length === 1) {
              this.selectedDeptId = deptList[0].deptId || deptList[0].id
            }
            await this.completeLogin()
          } else {
            this.depts = deptList
            this.selectedDeptId = currentDeptId || (deptList[0] && (deptList[0].deptId || deptList[0].id))
            this.loading = false
            this.showDeptPicker = true
          }
        } catch (e) {
          console.log('获取用户信息失败，直接进入', e)
          uni.setStorageSync('userInfo', { username: this.form.username })
          uni.setStorageSync('modules', [])
          uni.setStorageSync('permissions', [])
          uni.showToast({ title: '登录成功' })
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/index/index' })
          }, 500)
          this.loading = false
        }
      } catch (e) {
        console.error('登录失败详情:', e)
        this.loading = false
      }
    },
    goSettings() {
      uni.navigateTo({ url: '/pages/settings/index' })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #EAF2F4;
  position: relative;
  overflow: hidden;
}

/* ===== 入场动画 ===== */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(40rpx); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(80rpx); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes softFloat {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(0, 22rpx, 0) scale(1.03); }
}

.anim-fade-up {
  opacity: 0;
  animation: fadeInUp 0.7s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.anim-slide-up {
  opacity: 0;
  animation: slideUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.bg-decor {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.halo {
  position: absolute;
  border-radius: 50%;
  filter: blur(2rpx);
  animation: softFloat 9s ease-in-out infinite;
}

.halo-a {
  width: 720rpx;
  height: 720rpx;
  top: -180rpx;
  right: -260rpx;
  background: radial-gradient(circle, rgba(42, 111, 151, 0.24), rgba(42, 111, 151, 0));
}

.halo-b {
  width: 520rpx;
  height: 520rpx;
  bottom: 80rpx;
  left: -220rpx;
  background: radial-gradient(circle, rgba(142, 200, 210, 0.38), rgba(142, 200, 210, 0));
  animation-delay: 1.2s;
}

.ledger-line {
  position: absolute;
  left: 44rpx;
  right: 44rpx;
  height: 1rpx;
  background: rgba(42, 111, 151, 0.08);
}

.l1 { top: 260rpx; }
.l2 { top: 640rpx; }

.content {
  position: relative;
  z-index: 1;
  padding-left: 36rpx;
  padding-right: 36rpx;
}

.brand {
  display: flex;
  align-items: center;
  gap: 22rpx;
}

.brand-mark {
  position: relative;
  width: 108rpx;
  height: 108rpx;
  flex-shrink: 0;
}

.brand-logo-img {
  width: 100%;
  height: 100%;
  display: block;
}

.brand-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-size: 42rpx;
  font-weight: 700;
  color: #102A3A;
  letter-spacing: 1rpx;
}

.brand-sub {
  font-size: 24rpx;
  color: #5A6B7F;
  margin-top: 8rpx;
}

.daily-card {
  margin-top: 44rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.68);
  border: 1rpx solid rgba(255, 255, 255, 0.86);
  box-shadow: 0 16rpx 48rpx rgba(42, 111, 151, 0.1);
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
}

.daily-main { flex: 1; }

.daily-title {
  display: block;
  font-size: 30rpx;
  line-height: 42rpx;
  font-weight: 700;
  color: #102A3A;
}

.daily-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 23rpx;
  line-height: 34rpx;
  color: #5A6B7F;
}

.daily-chip {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #DCEFF4;
  color: #2A6F97;
  font-size: 22rpx;
  font-weight: 600;
}

.form-card {
  position: relative;
  margin-top: 28rpx;
  padding: 34rpx 30rpx 30rpx;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 32rpx;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 24rpx 64rpx rgba(23, 59, 87, 0.14);
}

.form-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28rpx;
}

.form-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #102A3A;
}

.settings-pill {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #F0F4F8;
}

.settings-text {
  font-size: 22rpx;
  color: #5A6B7F;
}

.form-item {
  margin-bottom: 22rpx;
}

.input-wrap {
  display: flex;
  align-items: center;
  min-height: 94rpx;
  background: #F7FAFC;
  border: 2rpx solid #E2E8F0;
  border-radius: 22rpx;
  padding: 0 24rpx;
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
}

.input-wrap:focus-within {
  border-color: #2A6F97;
  background: #FFFFFF;
  box-shadow: 0 0 0 4rpx rgba(42, 111, 151, 0.1);
}

.input-icon-wrap {
  width: 48rpx;
  height: 48rpx;
  border-radius: 16rpx;
  background: rgba(42, 111, 151, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.input-icon-text {
  font-size: 22rpx;
  font-weight: 700;
  color: #2A6F97;
}

.input-field {
  flex: 1;
  height: 94rpx;
  font-size: 28rpx;
  color: #1A2332;
}

.input-placeholder {
  color: #94A3B8;
}

/* 登录按钮 */
.btn-login {
  margin-top: 30rpx;
  height: 96rpx;
  line-height: 96rpx;
  background: linear-gradient(135deg, #173B57, #2A6F97);
  color: #FFFFFF;
  font-size: 30rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
  border-radius: 999rpx;
  text-align: center;
  box-shadow: 0 8rpx 24rpx rgba(42, 111, 151, 0.3);
  border: none;
  padding: 0;
  position: relative;
  transition: transform 0.15s, box-shadow 0.15s;
}

.btn-login--active,
.btn-login:active {
  transform: scale(0.97);
  box-shadow: 0 4rpx 16rpx rgba(42, 111, 151, 0.2);
}

.btn-login::after {
  border: none;
}

.btn-login[disabled] {
  opacity: 0.5;
  box-shadow: none;
}

.conn-bar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-top: 24rpx;
  padding: 16rpx 24rpx;
  border-radius: 18rpx;
  gap: 10rpx;
  background: #F7FAFC;
}

.conn-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.conn-text {
  font-size: 22rpx;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conn-bar.idle .conn-dot { background: #94A3B8; }
.conn-bar.idle .conn-text { color: #94A3B8; }
.conn-bar.testing .conn-dot { background: #F59E0B; animation: blink 1s infinite; }
.conn-bar.testing .conn-text { color: #B45309; }
.conn-bar.ok .conn-dot { background: #10B981; }
.conn-bar.ok .conn-text { color: #047857; }
.conn-bar.fail .conn-dot { background: #EF4444; }
.conn-bar.fail .conn-text { color: #B91C1C; }

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.trust-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  margin-top: 24rpx;
}

.trust-item {
  padding: 18rpx 10rpx;
  border-radius: 18rpx;
  background: #F7FAFC;
  text-align: center;
}

.trust-num {
  display: block;
  font-size: 24rpx;
  font-weight: 700;
  color: #173B57;
}

.trust-label {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: #94A3B8;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}

/* ===== 部门选择弹窗 ===== */
.dept-modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(16, 42, 58, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.dept-modal {
  width: 86%;
  max-width: 640rpx;
  background: #FFFFFF;
  border-radius: 28rpx;
  overflow: hidden;
  box-shadow: 0 24rpx 64rpx rgba(16, 42, 58, 0.2);
}

.dept-modal-head {
  padding: 30rpx 28rpx 20rpx;
  text-align: center;
}

.dept-modal-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #102A3A;
}

.dept-modal-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #5A6B7F;
}

.dept-list {
  max-height: 560rpx;
  padding: 0 40rpx 0 20rpx;
  box-sizing: border-box;
}

.dept-list-item {
  display: flex;
  align-items: center;
  padding: 20rpx 44rpx 20rpx 16rpx;
  border-radius: 18rpx;
  background: #F7FAFC;
  margin-bottom: 12rpx;
  border: 2rpx solid transparent;
  transition: all 0.15s;
  box-sizing: border-box;
  overflow: visible;
}

.dept-list-item.active {
  background: #EAF4F8;
  border-color: #2A6F97;
}

.dept-item-mark {
  width: 56rpx;
  height: 56rpx;
  border-radius: 16rpx;
  background: #DCEFF4;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.dept-item-mark-text {
  font-size: 22rpx;
  font-weight: 700;
  color: #2A6F97;
}

.dept-item-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.dept-item-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A2332;
}

.dept-item-meta {
  font-size: 20rpx;
  color: #94A3B8;
  margin-top: 4rpx;
}

.dept-item-check {
  font-size: 32rpx;
  font-weight: 700;
  color: #2A6F97;
  margin-left: 16rpx;
  margin-right: 4rpx;
  flex-shrink: 0;
  width: 44rpx;
  text-align: center;
  line-height: 1;
}

.dept-modal-foot {
  display: flex;
  gap: 16rpx;
  padding: 20rpx 28rpx 28rpx;
}

.btn-secondary,
.btn-primary {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 600;
  border: none;
}

.btn-secondary {
  background: #F0F4F8;
  color: #5A6B7F;
}

.btn-primary {
  background: linear-gradient(135deg, #173B57, #2A6F97);
  color: #FFFFFF;
}

.btn-primary[disabled] {
  opacity: 0.5;
}

.btn-secondary::after,
.btn-primary::after {
  border: none;
}
</style>
