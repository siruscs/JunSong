<template>
  <view class="page">
    <!-- 英雄卡片 -->
    <view class="hero-card">
      <view class="hero-icon">{{ id ? '✎' : '＋' }}</view>
      <view class="hero-info">
        <view class="hero-title">{{ id ? '编辑用户' : '新增用户' }}</view>
        <view class="hero-meta">用户管理 · 请完善必要信息后保存</view>
      </view>
    </view>

    <!-- 必填信息 -->
    <view class="section-card">
      <view class="section-header">
        <view class="section-dot required"></view>
        <text class="section-title">必填信息</text>
      </view>
      <view class="form-item">
        <view class="label-row">
          <text class="label">用户名</text>
          <text class="required-tag">*</text>
        </view>
        <input class="control input" v-model="form.userName" placeholder="请输入用户名" :disabled="!!id" />
      </view>
      <view class="form-item">
        <view class="label-row">
          <text class="label">昵称</text>
          <text class="required-tag">*</text>
        </view>
        <input class="control input" v-model="form.nickName" placeholder="请输入昵称" />
      </view>
      <view class="form-item" v-if="!id">
        <view class="label-row">
          <text class="label">密码</text>
          <text class="required-tag">*</text>
        </view>
        <input class="control input" v-model="form.password" type="text" password placeholder="请输入密码" />
      </view>
    </view>

    <!-- 其他信息 -->
    <view class="section-card">
      <view class="section-header">
        <view class="section-dot"></view>
        <text class="section-title">其他信息</text>
      </view>
      <view class="form-item">
        <view class="label-row">
          <text class="label">手机号</text>
        </view>
        <input class="control input" v-model="form.phonenumber" type="number" placeholder="请输入手机号" />
      </view>
      <view class="form-item">
        <view class="label-row">
          <text class="label">邮箱</text>
        </view>
        <input class="control input" v-model="form.email" placeholder="请输入邮箱" />
      </view>
      <view class="form-item">
        <view class="label-row">
          <text class="label">性别</text>
        </view>
        <picker :range="sexLabels" @change="onSexChange">
          <view class="control picker" :class="{ 'has-value': form.sex }">
            <text class="picker-text">{{ sexLabel || '请选择' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>
      <view class="form-item">
        <view class="label-row">
          <text class="label">状态</text>
        </view>
        <picker :range="statusLabels" @change="onStatusChange">
          <view class="control picker" :class="{ 'has-value': form.status }">
            <text class="picker-text">{{ statusLabel || '请选择' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>
    </view>

    <!-- 角色选择 -->
    <view class="section-card" v-if="roles.length">
      <view class="section-header">
        <view class="section-dot"></view>
        <text class="section-title">角色分配</text>
      </view>
      <view class="checkbox-group">
        <view
          class="checkbox-item"
          v-for="role in roles"
          :key="role.roleId"
          @tap="toggleRole(role.roleId)"
        >
          <view class="checkbox-box" :class="{ checked: form.roleIds.includes(role.roleId) }">
            <text class="checkbox-icon" v-if="form.roleIds.includes(role.roleId)">✓</text>
          </view>
          <text class="checkbox-label">{{ role.roleName }}</text>
        </view>
      </view>
    </view>

    <!-- 部门选择 -->
    <view class="section-card" v-if="depts.length">
      <view class="section-header">
        <view class="section-dot"></view>
        <text class="section-title">所属部门</text>
      </view>
      <picker :range="deptLabels" @change="onDeptChange">
        <view class="control picker" :class="{ 'has-value': form.deptId }">
          <text class="picker-text">{{ deptLabel || '请选择部门' }}</text>
          <text class="picker-arrow">›</text>
        </view>
      </picker>
    </view>

    <!-- 底部占位 -->
    <view class="footer-placeholder"></view>

    <!-- 固定底部 -->
    <view class="footer">
      <button class="btn-primary" @tap="submit">
        <text class="btn-icon">✓</text> 保存
      </button>
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { isAdmin } from '@/utils/permission.js'

export default {
  data() {
    return {
      id: '',
      form: {
        userName: '',
        nickName: '',
        password: '',
        phonenumber: '',
        email: '',
        sex: '',
        status: '0',
        roleIds: [],
        deptId: ''
      },
      roles: [],
      depts: [],
      sexOptions: [
        { label: '男', value: '0' },
        { label: '女', value: '1' },
        { label: '未知', value: '2' }
      ],
      statusOptions: [
        { label: '正常', value: '0' },
        { label: '停用', value: '1' }
      ]
    }
  },
  computed: {
    sexLabels() {
      return this.sexOptions.map(o => o.label)
    },
    statusLabels() {
      return this.statusOptions.map(o => o.label)
    },
    sexLabel() {
      const opt = this.sexOptions.find(o => o.value === this.form.sex)
      return opt ? opt.label : ''
    },
    statusLabel() {
      const opt = this.statusOptions.find(o => o.value === this.form.status)
      return opt ? opt.label : ''
    },
    deptLabels() {
      return this.depts.map(d => d.displayLabel || d.label || d.deptName)
    },
    deptLabel() {
      const dept = this.depts.find(d => String(d.id || d.deptId) === String(this.form.deptId))
      return dept ? (dept.displayLabel || dept.label || dept.deptName) : ''
    }
  },
  onLoad(options) {
    if (!isAdmin()) {
      uni.showToast({ title: '暂无管理权限', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 500)
      return
    }

    this.id = options.id || ''
    uni.setNavigationBarTitle({ title: this.id ? '编辑用户' : '新增用户' })

    this.loadRoles()
    this.loadDepts()

    if (this.id) {
      this.loadUser()
    }
  },
  methods: {
    onSexChange(e) {
      this.form.sex = this.sexOptions[e.detail.value].value
    },
    onStatusChange(e) {
      this.form.status = this.statusOptions[e.detail.value].value
    },
    onDeptChange(e) {
      const dept = this.depts[e.detail.value]
      if (dept) {
        this.form.deptId = dept.id || dept.deptId
      }
    },
    toggleRole(roleId) {
      const idx = this.form.roleIds.indexOf(roleId)
      if (idx >= 0) {
        this.form.roleIds.splice(idx, 1)
      } else {
        this.form.roleIds.push(roleId)
      }
    },
    async loadRoles() {
      try {
        const res = await request({ url: '/system/role/optionselect', method: 'GET' })
        this.roles = res.data || []
      } catch (e) {
        console.error('加载角色列表失败', e)
      }
    },
    flattenDepts(list = [], level = 0) {
      return list.reduce((items, dept) => {
        const id = dept.id || dept.deptId
        const label = dept.label || dept.deptName || ''
        items.push({
          ...dept,
          id,
          label,
          displayLabel: `${'　'.repeat(level)}${level ? '└ ' : ''}${label}`
        })
        const children = dept.children || []
        if (children.length) {
          items.push(...this.flattenDepts(children, level + 1))
        }
        return items
      }, [])
    },
    async loadDepts() {
      try {
        const res = await request({ url: '/system/user/deptTree', method: 'GET' })
        this.depts = this.flattenDepts(res.data || res.depts || [])
      } catch (e) {
        console.error('加载部门列表失败', e)
      }
    },
    async loadUser() {
      try {
        const res = await request({ url: `/system/user/${this.id}`, method: 'GET' })
        const data = res.data || res
        this.form.userName = data.userName || ''
        this.form.nickName = data.nickName || ''
        this.form.phonenumber = data.phonenumber || ''
        this.form.email = data.email || ''
        this.form.sex = data.sex || ''
        this.form.status = data.status || '0'
        this.form.deptId = data.deptId || ''
        this.form.roleIds = (data.roles || []).map(r => r.roleId)
      } catch (e) {
        console.error('加载用户信息失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    validate() {
      if (!this.form.userName.trim()) {
        uni.showToast({ title: '请填写用户名', icon: 'none' })
        return false
      }
      if (!this.form.nickName.trim()) {
        uni.showToast({ title: '请填写昵称', icon: 'none' })
        return false
      }
      if (!this.id && !this.form.password) {
        uni.showToast({ title: '请填写密码', icon: 'none' })
        return false
      }
      return true
    },
    async submit() {
      if (!this.validate()) return

      const payload = {
        userName: this.form.userName,
        nickName: this.form.nickName,
        phonenumber: this.form.phonenumber,
        email: this.form.email,
        sex: this.form.sex,
        status: this.form.status,
        roleIds: this.form.roleIds,
        deptId: this.form.deptId
      }

      try {
        if (this.id) {
          payload.userId = this.id
          await request({ url: '/system/user', method: 'PUT', data: payload })
        } else {
          payload.password = this.form.password
          await request({ url: '/system/user', method: 'POST', data: payload })
        }
        uni.showToast({ title: '保存成功' })
        setTimeout(() => uni.navigateBack(), 500)
      } catch (e) {
        console.error('保存失败', e)
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 0 0 140rpx;
  background: #F0F4F8;
}

.hero-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 34rpx 28rpx;
  background: linear-gradient(135deg, #2A6F97, #3A8DB8, #8EC8D2);
  border-radius: 0 0 24rpx 24rpx;
}

.hero-icon {
  width: 76rpx;
  height: 76rpx;
  line-height: 76rpx;
  text-align: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20rpx;
  font-size: 38rpx;
  color: #FFFFFF;
  flex-shrink: 0;
}

.hero-info {
  flex: 1;
}

.hero-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.hero-meta {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
}

.section-card {
  margin: 22rpx 28rpx 0;
  padding: 28rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 16rpx rgba(42, 111, 151, 0.06);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.section-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #94A3B8;
}

.section-dot.required {
  background: #EF4444;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
}

.form-item {
  padding-top: 22rpx;
}

.form-item + .form-item {
  border-top: 1rpx solid #F0F4F8;
  padding-top: 22rpx;
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
  font-size: 28rpx;
  font-weight: 700;
}

.control {
  width: 100%;
  min-height: 82rpx;
  padding: 0 24rpx;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
  border-radius: 14rpx;
  font-size: 28rpx;
  color: #1A2332;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.control:focus {
  border-color: #2A6F97;
  background: #FFFFFF;
}

.picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  line-height: 82rpx;
}

.picker-text {
  flex: 1;
  color: #1A2332;
}

.picker:not(.has-value) .picker-text {
  color: #94A3B8;
}

.picker-arrow {
  font-size: 36rpx;
  color: #CBD5E1;
  font-weight: 300;
}

/* 角色复选框 */
.checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.checkbox-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 12rpx 20rpx;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
  border-radius: 12rpx;
}

.checkbox-box {
  width: 36rpx;
  height: 36rpx;
  border: 2rpx solid #CBD5E1;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.checkbox-box.checked {
  background: #2A6F97;
  border-color: #2A6F97;
}

.checkbox-icon {
  font-size: 24rpx;
  color: #FFFFFF;
  font-weight: 700;
}

.checkbox-label {
  font-size: 26rpx;
  color: #334155;
}

/* 底部占位 */
.footer-placeholder {
  height: 140rpx;
}

/* 固定底部 */
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
  border-top: 1rpx solid #E2E8F0;
}

.btn-primary {
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
  background: linear-gradient(135deg, #2A6F97, #3A8DB8);
  color: #FFFFFF;
  box-shadow: 0 6rpx 20rpx rgba(42, 111, 151, 0.25);
}

.btn-icon {
  font-size: 28rpx;
}
</style>
