<template>
  <view class="page">
    <view v-if="initializing" class="loading-overlay"><text>加载中...</text></view>
    <view v-else-if="loadError" class="loading-overlay error-overlay"><text>{{ loadError }}</text><button class="btn-primary" @tap="retryInit">重新加载</button></view>
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
      <view class="section-subtitle">{{ deptSummary }}</view>
      <view class="checkbox-group dept-checkbox-group">
        <view
          class="checkbox-item dept-checkbox-item"
          v-for="dept in depts"
          :key="dept.id || dept.deptId"
          @tap="toggleDept(dept.id || dept.deptId)"
        >
          <view class="checkbox-box" :class="{ checked: isDeptSelected(dept.id || dept.deptId) }">
            <text class="checkbox-icon" v-if="isDeptSelected(dept.id || dept.deptId)">✓</text>
          </view>
          <text class="checkbox-label">{{ dept.displayLabel || dept.label || dept.deptName }}</text>
        </view>
      </view>
    </view>

    <!-- 底部占位 -->
    <view class="footer-placeholder"></view>

    <!-- 固定底部 -->
    <view class="footer">
      <button class="btn-primary" :disabled="saving" @tap="submit">
        <text class="btn-icon">✓</text> {{ saving ? '保存中' : '保存' }}
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
        deptId: '',
        deptIds: []
      },
      roles: [],
      depts: [],
      initializing: false,
      loadError: '',
      saving: false,
      saved: false,
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
    deptSummary() {
      if (!this.form.deptIds.length) return '请选择所属部门'
      const names = this.depts
        .filter(d => this.isDeptSelected(d.id || d.deptId))
        .map(d => d.label || d.deptName)
      if (names.length <= 2) return names.join('、')
      return `已选择${names.length}个部门`
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

    this.initialize()
  },
  methods: {
    async initialize() {
      this.initializing = true
      this.loadError = ''
      try {
        await Promise.all([this.loadRoles(), this.loadDepts(), this.id ? this.loadUser() : Promise.resolve()])
      } catch (e) {
        this.loadError = e?.msg || e?.message || '用户信息加载失败'
      } finally { this.initializing = false }
    },
    retryInit() { this.initialize() },
    onSexChange(e) {
      this.form.sex = this.sexOptions[e.detail.value].value
    },
    onStatusChange(e) {
      this.form.status = this.statusOptions[e.detail.value].value
    },
    isDeptSelected(deptId) {
      return this.form.deptIds.some(id => String(id) === String(deptId))
    },
    toggleDept(deptId) {
      const idx = this.form.deptIds.findIndex(id => String(id) === String(deptId))
      if (idx >= 0) {
        this.form.deptIds.splice(idx, 1)
      } else {
        this.form.deptIds.push(deptId)
      }
      this.form.deptId = this.form.deptIds[0] || ''
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
    firstNonEmptyArray(...values) {
      return values.find(value => Array.isArray(value) && value.length) || []
    },
    async loadUser() {
      try {
        const res = await request({ url: `/system/user/${this.id}`, method: 'GET' })
        const data = res?.data || res
        if (!data || typeof data !== 'object' || Array.isArray(data) || !Object.keys(data).length) {
          throw new Error('用户详情为空，无法编辑')
        }
        this.form.userName = data.userName || ''
        this.form.nickName = data.nickName || ''
        this.form.phonenumber = data.phonenumber || ''
        this.form.email = data.email || ''
        this.form.sex = data.sex || ''
        this.form.status = data.status || '0'
        const deptIds = this.firstNonEmptyArray(data.deptIds, res.deptIds, data.deptId ? [data.deptId] : [])
        this.form.deptIds = deptIds
        this.form.deptId = data.deptId || this.form.deptIds[0] || ''
        this.form.roleIds = res.roleIds || (data.roles || []).map(r => r.roleId)
      } catch (e) {
        console.error('加载用户信息失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
        throw e
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
      if (!this.form.deptIds.length) {
        uni.showToast({ title: '请选择所属部门', icon: 'none' })
        return false
      }
      return true
    },
    async submit() {
      if (this.saving || this.saved) return
      if (!this.validate()) return

      const payload = {
        userName: this.form.userName,
        nickName: this.form.nickName,
        phonenumber: this.form.phonenumber,
        email: this.form.email,
        sex: this.form.sex,
        status: this.form.status,
        roleIds: this.form.roleIds,
        deptId: this.form.deptId || this.form.deptIds[0] || '',
        deptIds: this.form.deptIds
      }

      this.saving = true
      try {
        if (this.id) {
          payload.userId = this.id
          await request({ url: '/system/user', method: 'PUT', data: payload })
        } else {
          payload.password = this.form.password
          await request({ url: '/system/user', method: 'POST', data: payload })
        }
        this.saved = true
        uni.showToast({ title: '保存成功' })
        setTimeout(() => uni.navigateBack(), 500)
      } catch (e) {
        console.error('保存失败', e)
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  padding: 0 0 140rpx;
  background: #E8EEF5;
}

.loading-overlay {
  position: fixed;
  z-index: 20;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24rpx;
  background: rgba(232, 238, 245, .96);
  color: #64748B;
  font-size: 28rpx;
}

.error-overlay { color: #B42318; }

.hero-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 34rpx 28rpx;
  background: linear-gradient(135deg, #087CF0, #5AA9E8, #A8C7E5);
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
  box-shadow: 0 2rpx 16rpx rgba(8, 124, 240, 0.06);
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

.section-subtitle {
  margin-bottom: 18rpx;
  font-size: 24rpx;
  color: #64748B;
  line-height: 1.4;
}

.form-item {
  padding-top: 22rpx;
}

.form-item + .form-item {
  border-top: 1rpx solid #E8EEF5;
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
  border-color: #087CF0;
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
  background: #087CF0;
  border-color: #087CF0;
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

.dept-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.dept-checkbox-item {
  width: 100%;
  box-sizing: border-box;
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
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
  box-shadow: 0 6rpx 20rpx rgba(8, 124, 240, 0.25);
}

.btn-icon {
  font-size: 28rpx;
}
</style>
