<template>
  <view class="page">
    <view class="hero-card">
      <view class="hero-avatar" @tap="chooseAvatar">
        <image class="hero-avatar-img" v-if="form.avatar" :src="avatarUrl" mode="aspectFill" />
        <text class="hero-avatar-text" v-else>{{ avatarChar }}</text>
        <view class="avatar-edit">更换</view>
      </view>
      <view class="hero-info">
        <text class="hero-title">个人资料</text>
        <text class="hero-sub">维护昵称、手机号、邮箱等基础信息</text>
      </view>
    </view>

    <view class="section-card">
      <view class="form-item">
        <text class="label">用户名</text>
        <input class="control input disabled" :value="form.userName" disabled />
      </view>
      <view class="form-item">
        <text class="label">昵称</text>
        <input class="control input" v-model="form.nickName" placeholder="请输入昵称" />
      </view>
      <view class="form-item">
        <text class="label">手机号</text>
        <input class="control input" v-model="form.phonenumber" type="number" placeholder="请输入手机号" />
      </view>
      <view class="form-item">
        <text class="label">邮箱</text>
        <input class="control input" v-model="form.email" placeholder="请输入邮箱" />
      </view>
      <view class="form-item">
        <text class="label">性别</text>
        <picker :range="sexLabels" @change="onSexChange">
          <view class="control picker" :class="{ 'has-value': form.sex !== '' }">
            <text class="picker-text">{{ sexLabel || '请选择' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>
    </view>

    <view class="meta-card" v-if="roleGroup || postGroup">
      <view class="meta-row" v-if="roleGroup">
        <text class="meta-label">所属角色</text>
        <text class="meta-value">{{ roleGroup }}</text>
      </view>
      <view class="meta-row" v-if="postGroup">
        <text class="meta-label">岗位</text>
        <text class="meta-value">{{ postGroup }}</text>
      </view>
    </view>

    <view class="footer-placeholder"></view>
    <view class="footer">
      <button class="btn-primary" :loading="saving" @tap="submit">保存资料</button>
    </view>
  </view>
</template>

<script>
import { request, getBaseUrl, getToken } from '@/api/index.js'

export default {
  data() {
    return {
      loading: false,
      saving: false,
      roleGroup: '',
      postGroup: '',
      form: {
        userName: '',
        nickName: '',
        phonenumber: '',
        email: '',
        sex: '',
        avatar: ''
      },
      sexOptions: [
        { label: '男', value: '0' },
        { label: '女', value: '1' },
        { label: '未知', value: '2' }
      ]
    }
  },
  computed: {
    avatarChar() {
      const name = this.form.nickName || this.form.userName || ''
      return name.charAt(0).toUpperCase() || '?'
    },
    avatarUrl() {
      const url = this.form.avatar || ''
      if (!url) return ''
      if (url.startsWith('http://') || url.startsWith('https://')) return url
      const baseUrl = getBaseUrl()
      if (url.startsWith('/statics/')) {
        return baseUrl.replace(/\/prod-api$/, '').replace(/\/dev-api$/, '') + url
      }
      return baseUrl + url
    },
    sexLabels() {
      return this.sexOptions.map(item => item.label)
    },
    sexLabel() {
      const item = this.sexOptions.find(item => item.value === this.form.sex)
      return item ? item.label : ''
    }
  },
  onLoad() {
    this.loadProfile()
  },
  methods: {
    onSexChange(e) {
      const item = this.sexOptions[e.detail.value]
      if (item) this.form.sex = item.value
    },
    chooseAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const filePath = res.tempFilePaths && res.tempFilePaths[0]
          if (filePath) this.uploadAvatar(filePath)
        }
      })
    },
    uploadAvatar(filePath) {
      uni.showLoading({ title: '上传中' })
      uni.uploadFile({
        url: getBaseUrl() + '/system/user/profile/avatar',
        filePath,
        name: 'avatarfile',
        header: {
          Authorization: 'Bearer ' + getToken()
        },
        success: (res) => {
          let data = {}
          try {
            data = typeof res.data === 'string' ? JSON.parse(res.data) : (res.data || {})
          } catch (e) {
            data = {}
          }
          if (res.statusCode >= 200 && res.statusCode < 300 && (data.code === undefined || data.code === 200)) {
            this.form.avatar = data.imgUrl || data.url || filePath
            const userInfo = uni.getStorageSync('userInfo') || {}
            uni.setStorageSync('userInfo', { ...userInfo, avatar: this.form.avatar })
            uni.showToast({ title: '头像已更新' })
            return
          }
          uni.showToast({ title: data.msg || data.message || '头像上传失败', icon: 'none' })
        },
        fail: () => {
          uni.showToast({ title: '头像上传失败', icon: 'none' })
        },
        complete: () => {
          uni.hideLoading()
        }
      })
    },
    async loadProfile() {
      this.loading = true
      try {
        const res = await request({ url: '/system/user/profile', method: 'GET' })
        const data = res.data || res || {}
        this.roleGroup = res.roleGroup || ''
        this.postGroup = res.postGroup || ''
        this.form.userName = data.userName || data.username || ''
        this.form.nickName = data.nickName || ''
        this.form.phonenumber = data.phonenumber || ''
        this.form.email = data.email || ''
        this.form.sex = data.sex || ''
        const storedUserInfo = uni.getStorageSync('userInfo') || {}
        this.form.avatar = data.avatar || storedUserInfo.avatar || ''
      } catch (e) {
        console.error('加载个人资料失败', e)
      } finally {
        this.loading = false
      }
    },
    validate() {
      if (!this.form.nickName.trim()) {
        uni.showToast({ title: '请输入昵称', icon: 'none' })
        return false
      }
      return true
    },
    async submit() {
      if (!this.validate()) return
      this.saving = true
      try {
        await request({
          url: '/system/user/profile',
          method: 'PUT',
          data: {
            nickName: this.form.nickName,
            phonenumber: this.form.phonenumber,
            email: this.form.email,
            sex: this.form.sex
          }
        })
        const userInfo = uni.getStorageSync('userInfo') || {}
        uni.setStorageSync('userInfo', {
          ...userInfo,
          username: this.form.userName || userInfo.username,
          userName: this.form.userName || userInfo.userName,
          nickName: this.form.nickName,
          avatar: this.form.avatar,
          phonenumber: this.form.phonenumber,
          email: this.form.email,
          sex: this.form.sex
        })
        uni.showToast({ title: '保存成功' })
        setTimeout(() => uni.navigateBack(), 500)
      } catch (e) {
        console.error('保存个人资料失败', e)
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

.hero-avatar {
  position: relative;
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.hero-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.hero-avatar-text {
  font-size: 40rpx;
  font-weight: 800;
  color: #FFFFFF;
}

.avatar-edit {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 34rpx;
  line-height: 34rpx;
  text-align: center;
  background: rgba(23, 59, 87, 0.72);
  color: #FFFFFF;
  font-size: 18rpx;
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
.meta-card {
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

.disabled {
  color: #94A3B8;
}

.picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.picker-text {
  color: #94A3B8;
}

.picker.has-value .picker-text {
  color: #1A2332;
}

.picker-arrow {
  color: #CBD5E1;
  font-size: 34rpx;
}

.meta-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
}

.meta-row + .meta-row {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #E8EEF5;
}

.meta-label {
  font-size: 24rpx;
  color: #94A3B8;
  flex-shrink: 0;
}

.meta-value {
  font-size: 26rpx;
  color: #1A2332;
  font-weight: 600;
  text-align: right;
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
