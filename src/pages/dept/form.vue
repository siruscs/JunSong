<template>
  <view class="page">
    <view class="form-card">
      <view class="form-header">
        <text class="form-title">{{ isAdd ? '新增部门' : '编辑部门' }}</text>
      </view>

      <view class="form-item">
        <text class="form-label required">上级部门</text>
        <picker class="form-picker" :range="parentOptions" range-key="deptName" :value="selectedParentIndex" @change="onParentChange">
          <view class="picker-value">
            <text>{{ selectedParent?.deptName || '请选择上级部门' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label required">部门名称</text>
        <input class="form-input" v-model="form.deptName" placeholder="请输入部门名称" />
      </view>

      <view class="form-item">
        <text class="form-label required">显示排序</text>
        <input class="form-input" v-model="form.orderNum" type="number" placeholder="请输入排序号" />
      </view>

      <view class="form-item">
        <text class="form-label">负责人</text>
        <input class="form-input" v-model="form.leader" placeholder="请输入负责人姓名" />
      </view>

      <view class="form-item">
        <text class="form-label">联系电话</text>
        <input class="form-input" v-model="form.phone" type="number" placeholder="请输入联系电话" />
      </view>

      <view class="form-item">
        <text class="form-label">邮箱</text>
        <input class="form-input" v-model="form.email" placeholder="请输入邮箱地址" />
      </view>

      <view class="form-item">
        <text class="form-label">详细地址</text>
        <textarea class="form-textarea" v-model="form.detailAddress" placeholder="请输入详细地址" />
      </view>

      <view class="form-item">
        <text class="form-label">状态</text>
        <view class="radio-group">
          <view 
            class="radio-item" 
            :class="{ active: form.status === '0' }" 
            @tap="form.status = '0'"
          >
            <view class="radio-dot"></view>
            <text>正常</text>
          </view>
          <view 
            class="radio-item" 
            :class="{ active: form.status === '1' }" 
            @tap="form.status = '1'"
          >
            <view class="radio-dot"></view>
            <text>停用</text>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <button class="btn-cancel" @tap="goBack">取消</button>
      <button class="btn-submit" @tap="submitForm">保存</button>
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { hasActionPermission } from '@/utils/permission.js'

export default {
  data() {
    return {
      deptId: null,
      isAdd: true,
      parentOptions: [],
      form: {
        parentId: 0,
        deptName: '',
        orderNum: '',
        leader: '',
        phone: '',
        email: '',
        detailAddress: '',
        status: '0'
      }
    }
  },
  computed: {
    selectedParentIndex() {
      const index = this.parentOptions.findIndex(item => String(item.deptId) === String(this.form.parentId))
      return index >= 0 ? index : 0
    },
    selectedParent() {
      return this.parentOptions[this.selectedParentIndex] || null
    }
  },
  async onLoad(options) {
    this.deptId = options.id
    this.isAdd = !this.deptId
    await this.loadParentOptions()
    if (!this.isAdd) {
      await this.loadDept()
    }
  },
  methods: {
    async loadParentOptions() {
      try {
        const res = await request({ url: '/system/dept/list', method: 'GET' })
        const list = res.data || res.rows || []
        this.parentOptions = [{ deptId: 0, deptName: '无（顶级部门）' }, ...list]
      } catch (e) {
        console.error('加载上级部门选项失败', e)
        this.parentOptions = [{ deptId: 0, deptName: '无（顶级部门）' }]
      }
    },
    async loadDept() {
      if (!this.deptId) return
      try {
        const res = await request({ url: '/system/dept/' + this.deptId, method: 'GET' })
        const data = res.data || res
        this.form = {
          parentId: data.parentId || 0,
          deptName: data.deptName || '',
          orderNum: data.orderNum || '',
          leader: data.leader || '',
          phone: data.phone || '',
          email: data.email || '',
          detailAddress: data.detailAddress || '',
          status: data.status || '0'
        }
      } catch (e) {
        console.error('加载部门信息失败', e)
      }
    },
    onParentChange(e) {
      const item = this.parentOptions[Number(e.detail.value)]
      this.form.parentId = item?.deptId || 0
    },
    validateForm() {
      if (!this.form.deptName.trim()) {
        uni.showToast({ title: '请输入部门名称', icon: 'none' })
        return false
      }
      if (!this.form.orderNum) {
        uni.showToast({ title: '请输入排序号', icon: 'none' })
        return false
      }
      return true
    },
    async submitForm() {
      if (!this.validateForm()) return
      if (!this.hasPermission()) {
        uni.showToast({ title: '暂无权限', icon: 'none' })
        return
      }

      try {
        const data = {
          ...this.form,
          orderNum: Number(this.form.orderNum)
        }
        if (this.isAdd) {
          await request({ url: '/system/dept', method: 'POST', data })
          uni.showToast({ title: '新增成功' })
        } else {
          data.deptId = this.deptId
          await request({ url: '/system/dept', method: 'PUT', data })
          uni.showToast({ title: '修改成功' })
        }
        setTimeout(() => {
          uni.navigateBack({ delta: 2 })
        }, 1000)
      } catch (e) {
        console.error('保存失败', e)
        uni.showToast({ title: '保存失败', icon: 'none' })
      }
    },
    hasPermission() {
      return this.isAdd 
        ? hasActionPermission('deptManage', 'add')
        : hasActionPermission('deptManage', 'edit')
    },
    goBack() {
      uni.navigateBack()
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F0F4F8;
  padding: 24rpx 28rpx;
  padding-bottom: 160rpx;
  box-sizing: border-box;
}

.form-card {
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 2rpx 16rpx rgba(42, 111, 151, 0.06);
}

.form-header {
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid #F1F5F9;
  margin-bottom: 24rpx;
}

.form-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1A2332;
}

.form-item {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #F8FAFC;
}

.form-item:last-of-type {
  border-bottom: none;
}

.form-label {
  display: block;
  font-size: 26rpx;
  color: #64748B;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.form-label.required::after {
  content: '*';
  color: #EF4444;
  margin-left: 6rpx;
}

.form-input {
  width: 100%;
  height: 80rpx;
  padding: 0 20rpx;
  background: #F8FAFC;
  border: 1rpx solid #E2E8F0;
  border-radius: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}

.form-textarea {
  width: 100%;
  height: 160rpx;
  padding: 16rpx 20rpx;
  background: #F8FAFC;
  border: 1rpx solid #E2E8F0;
  border-radius: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}

.form-picker {
  width: 100%;
}

.picker-value {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 80rpx;
  padding: 0 20rpx;
  background: #F8FAFC;
  border: 1rpx solid #E2E8F0;
  border-radius: 16rpx;
  font-size: 26rpx;
  color: #1A2332;
}

.picker-arrow {
  font-size: 32rpx;
  color: #94A3B8;
}

.radio-group {
  display: flex;
  gap: 40rpx;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 12rpx 20rpx;
  border-radius: 12rpx;
  transition: all 0.2s;
}

.radio-item.active {
  background: rgba(42, 111, 151, 0.08);
}

.radio-dot {
  width: 32rpx;
  height: 32rpx;
  border: 2rpx solid #CBD5E1;
  border-radius: 50%;
  position: relative;
}

.radio-item.active .radio-dot {
  border-color: #2A6F97;
}

.radio-item.active .radio-dot::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 16rpx;
  height: 16rpx;
  background: #2A6F97;
  border-radius: 50%;
}

.radio-item text {
  font-size: 26rpx;
  color: #1A2332;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 20rpx;
  padding: 20rpx 28rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #FFFFFF;
  border-top: 1rpx solid #E2E8F0;
}

.btn-cancel,
.btn-submit {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 600;
}

.btn-cancel {
  background: #F1F5F9;
  color: #475569;
}

.btn-submit {
  background: linear-gradient(135deg, #2A6F97, #3A8DB8);
  color: #FFFFFF;
}
</style>