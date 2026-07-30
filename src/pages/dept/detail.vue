<template>
  <view class="page">
    <view class="form-card" v-if="dept">
      <view class="form-header">
        <text class="form-title">{{ isEdit ? '编辑部门' : '部门详情' }}</text>
      </view>

      <view class="form-item">
        <text class="form-label">上级部门</text>
        <text class="form-value">{{ parentName || '无' }}</text>
      </view>

      <view class="form-item">
        <text class="form-label">部门名称</text>
        <text class="form-value">{{ dept.deptName || '-' }}</text>
      </view>

      <view class="form-item">
        <text class="form-label">显示排序</text>
        <text class="form-value">{{ dept.orderNum || '-' }}</text>
      </view>

      <view class="form-item">
        <text class="form-label">联系电话</text>
        <text class="form-value">{{ dept.phone || '-' }}</text>
      </view>

      <view class="form-item">
        <text class="form-label">邮箱</text>
        <text class="form-value">{{ dept.email || '-' }}</text>
      </view>

      <view class="form-item">
        <text class="form-label">所在地址</text>
        <text class="form-value">{{ fullAddress }}</text>
      </view>

      <view class="form-item">
        <text class="form-label">详细地址</text>
        <text class="form-value">{{ dept.detailAddress || '-' }}</text>
      </view>

      <view class="form-item">
        <text class="form-label">状态</text>
        <view class="status-tag" :class="dept.status === '0' ? 'status-ok' : 'status-disabled'">
          {{ dept.status === '0' ? '正常' : '停用' }}
        </view>
      </view>

      <view class="form-actions">
        <button class="action-btn edit-btn" v-if="hasEditPermission" @tap="goEdit">编辑</button>
        <button class="action-btn delete-btn" v-if="hasDeletePermission" @tap="showDeleteConfirm">删除</button>
      </view>
    </view>

    <view class="loading" v-if="loading">加载中...</view>
    <view class="empty" v-if="!loading && !dept">
      <text>部门不存在</text>
    </view>

    <view class="delete-mask" v-if="showDelete" @tap="cancelDelete">
      <view class="delete-panel" @tap.stop>
        <view class="delete-title">确认删除</view>
        <view class="delete-content">确定要删除部门「{{ dept?.deptName || '' }}」吗？</view>
        <view class="delete-actions">
          <button class="delete-cancel" @tap="cancelDelete">取消</button>
          <button class="delete-confirm" @tap="confirmDelete">确认删除</button>
        </view>
      </view>
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
      dept: null,
      loading: false,
      showDelete: false,
      parentName: ''
    }
  },
  computed: {
    isEdit() {
      return false
    },
    hasEditPermission() {
      return hasActionPermission('deptManage', 'edit')
    },
    hasDeletePermission() {
      return hasActionPermission('deptManage', 'remove')
    },
    fullAddress() {
      if (!this.dept) return '-'
      const parts = [
        this.dept.provinceName,
        this.dept.cityName,
        this.dept.districtName,
        this.dept.streetName
      ].filter(Boolean)
      return parts.length > 0 ? parts.join(' / ') : '-'
    }
  },
  async onLoad(options) {
    this.deptId = options.id
    await this.loadDept()
  },
  methods: {
    async loadDept() {
      if (!this.deptId) return
      this.loading = true
      try {
        const res = await request({ url: '/system/dept/' + this.deptId, method: 'GET' })
        this.dept = res.data || res
        if (this.dept.parentId && this.dept.parentId !== 0) {
          await this.loadParentName()
        }
      } catch (e) {
        console.error('加载部门详情失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    async loadParentName() {
      try {
        const res = await request({ url: '/system/dept/' + this.dept.parentId, method: 'GET' })
        this.parentName = res.data?.deptName || res?.deptName || ''
      } catch (e) {
        this.parentName = ''
      }
    },
    goEdit() {
      uni.navigateTo({ url: `/pages/dept/form?id=${this.deptId}` })
    },
    showDeleteConfirm() {
      this.showDelete = true
    },
    cancelDelete() {
      this.showDelete = false
    },
    async confirmDelete() {
      try {
        await request({ url: '/system/dept/' + this.deptId, method: 'DELETE' })
        uni.showToast({ title: '删除成功' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1000)
      } catch (e) {
        console.error('删除失败', e)
      } finally {
        this.showDelete = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #E8EEF5;
  padding: 24rpx 28rpx;
  box-sizing: border-box;
}

.form-card {
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 2rpx 16rpx rgba(8, 124, 240, 0.06);
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
  display: flex;
  align-items: flex-start;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #F8FAFC;
}

.form-item:last-of-type {
  border-bottom: none;
}

.form-label {
  width: 160rpx;
  flex-shrink: 0;
  font-size: 26rpx;
  color: #64748B;
  font-weight: 600;
}

.form-value {
  flex: 1;
  font-size: 26rpx;
  color: #1A2332;
  word-break: break-all;
}

.status-tag {
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.status-tag.status-ok {
  background: #D1FAE5;
  color: #065F46;
}

.status-tag.status-disabled {
  background: #FEE2E2;
  color: #991B1B;
}

.form-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 32rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #F1F5F9;
}

.action-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 600;
}

.edit-btn {
  background: #F1F5F9;
  color: #475569;
}

.delete-btn {
  background: #FEF2F2;
  color: #DC2626;
}

.loading,
.empty {
  padding: 60rpx;
  text-align: center;
  color: #94A3B8;
  font-size: 26rpx;
}

.delete-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.45);
}

.delete-panel {
  width: 560rpx;
  padding: 40rpx;
  border-radius: 24rpx;
  background: #FFFFFF;
}

.delete-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A2332;
  text-align: center;
}

.delete-content {
  margin-top: 20rpx;
  font-size: 26rpx;
  color: #64748B;
  text-align: center;
}

.delete-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 32rpx;
}

.delete-cancel,
.delete-confirm {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
}

.delete-cancel {
  background: #F1F5F9;
  color: #475569;
}

.delete-confirm {
  background: #DC2626;
  color: #FFFFFF;
}
</style>
