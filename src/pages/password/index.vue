<template>
  <view class="page">
    <view class="hero-card">
      <view class="hero-icon">密</view>
      <view class="hero-info">
        <text class="hero-title">修改密码</text>
        <text class="hero-sub">建议使用不少于 6 位的安全密码</text>
      </view>
    </view>

    <view class="section-card">
      <view class="form-item">
        <text class="label">当前密码</text>
        <input class="control input" v-model="form.oldPassword" type="text" password placeholder="请输入当前密码" />
      </view>
      <view class="form-item">
        <text class="label">新密码</text>
        <input class="control input" v-model="form.newPassword" type="text" password placeholder="请输入新密码" />
      </view>
      <view class="form-item">
        <text class="label">确认新密码</text>
        <input class="control input" v-model="form.confirmPassword" type="text" password placeholder="请再次输入新密码" />
      </view>
    </view>

    <view class="hint-card">
      <text class="hint-title">安全提示</text>
      <text class="hint-text">密码修改成功后，请使用新密码重新登录。不要与他人共享账号密码。</text>
    </view>

    <view class="footer-placeholder"></view>
    <view class="footer">
      <button class="btn-primary" :loading="saving" @tap="submit">确认修改</button>
    </view>
  </view>
</template>

<script>
import { request, setToken } from '@/api/index.js'

export default {
  data() {
    return {
      saving: false,
      form: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
    }
  },
  methods: {
    validate() {
      if (!this.form.oldPassword) {
        uni.showToast({ title: '请输入当前密码', icon: 'none' })
        return false
      }
      if (!this.form.newPassword) {
        uni.showToast({ title: '请输入新密码', icon: 'none' })
        return false
      }
      if (this.form.newPassword.length < 6) {
        uni.showToast({ title: '新密码至少 6 位', icon: 'none' })
        return false
      }
      if (this.form.newPassword !== this.form.confirmPassword) {
        uni.showToast({ title: '两次新密码不一致', icon: 'none' })
        return false
      }
      if (this.form.oldPassword === this.form.newPassword) {
        uni.showToast({ title: '新密码不能与当前密码相同', icon: 'none' })
        return false
      }
      return true
    },
    async submit() {
      if (!this.validate()) return
      this.saving = true
      try {
        await request({
          url: '/system/user/profile/updatePwd',
          method: 'PUT',
          data: {
            oldPassword: this.form.oldPassword,
            newPassword: this.form.newPassword
          }
        })
        uni.showToast({ title: '修改成功' })
        setToken('')
        uni.removeStorageSync('userInfo')
        uni.removeStorageSync('modules')
        uni.removeStorageSync('permissions')
        setTimeout(() => uni.reLaunch({ url: '/pages/login/index' }), 700)
      } catch (e) {
        console.error('修改密码失败', e)
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx 28rpx 150rpx;
  background: #E8EEF5;
  box-sizing: border-box;
}

.hero-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 34rpx 30rpx;
  background: linear-gradient(135deg, #123F73, #087CF0, #A8C7E5);
  border-radius: 24rpx;
  box-shadow: 0 12rpx 32rpx rgba(8, 124, 240, 0.18);
}

.hero-icon {
  width: 92rpx;
  height: 92rpx;
  line-height: 92rpx;
  text-align: center;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.2);
  color: #FFFFFF;
  font-size: 40rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.hero-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.hero-title {
  font-size: 36rpx;
  font-weight: 800;
  color: #FFFFFF;
}

.hero-sub {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.72);
}

.section-card,
.hint-card {
  margin-top: 24rpx;
  padding: 28rpx;
  background: #FFFFFF;
  border-radius: 22rpx;
  box-shadow: 0 2rpx 16rpx rgba(8, 124, 240, 0.06);
}

.form-item + .form-item {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #E8EEF5;
}

.label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: #5A6B7F;
  font-weight: 600;
}

.control {
  width: 100%;
  min-height: 84rpx;
  padding: 0 24rpx;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
  border-radius: 16rpx;
  font-size: 28rpx;
  color: #1A2332;
  box-sizing: border-box;
}

.hint-title {
  display: block;
  font-size: 26rpx;
  color: #087CF0;
  font-weight: 700;
}

.hint-text {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #5A6B7F;
  line-height: 1.6;
}

.footer-placeholder {
  height: 120rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  padding: 18rpx 28rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 -8rpx 24rpx rgba(8, 124, 240, 0.08);
  box-sizing: border-box;
}

.btn-primary {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
  font-size: 30rpx;
  font-weight: 700;
}
</style>
