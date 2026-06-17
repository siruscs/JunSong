<template>
  <view class="page">
    <!-- 搜索栏 -->
    <view class="search-wrap">
      <input
        class="search"
        v-model="keyword"
        placeholder="搜索用户名或手机号"
        confirm-type="search"
        @confirm="refresh"
      />
      <button class="search-button" @tap="refresh">查询</button>
    </view>

    <!-- 用户列表 -->
    <scroll-view
      scroll-y
      class="scroll"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="refresh"
      @scrolltolower="loadMore"
    >
      <view
        class="user-item"
        hover-class="user-item--active"
        v-for="item in rows"
        :key="item.userId"
        @tap="openDetail(item)"
      >
        <view class="avatar" :style="{ background: avatarColor(item.userName) }">
          {{ firstChar(item.userName) }}
        </view>
        <view class="user-info">
          <view class="user-name-row">
            <text class="user-name">{{ item.userName || '-' }}</text>
            <text class="nick-name">{{ item.nickName || '-' }}</text>
          </view>
          <view class="user-meta">
            <text class="user-phone">{{ item.phonenumber || '-' }}</text>
            <view class="status-pill" :class="item.status === '0' ? 'status-ok' : 'status-disabled'">
              {{ item.status === '0' ? '正常' : '停用' }}
            </view>
          </view>
        </view>
        <text class="arrow">›</text>
      </view>

      <view class="empty" v-if="!loading && rows.length === 0">
        <view class="empty-title">暂无用户</view>
        <view class="empty-subtitle">点击右下角新增用户</view>
      </view>
      <view class="loading" v-if="loading">加载中</view>
      <view class="loading" v-if="finished && rows.length > 0">没有更多了</view>
    </scroll-view>

    <!-- 新增按钮 -->
    <view class="fab" v-if="canAdd" @tap="addUser">
      <text class="fab-icon">＋</text>
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { isAdmin } from '@/utils/permission.js'

export default {
  data() {
    return {
      keyword: '',
      pageNum: 1,
      pageSize: 10,
      rows: [],
      loading: false,
      refreshing: false,
      finished: false,
      canAdd: false
    }
  },
  onLoad() {
    if (!isAdmin()) {
      uni.showToast({ title: '暂无管理权限', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 500)
      return
    }
    this.canAdd = true
    this.refresh()
  },
  onShow() {
    if (this.canAdd) {
      this.refresh()
    }
  },
  methods: {
    firstChar(name) {
      if (!name) return '?'
      return name.charAt(0).toUpperCase()
    },
    avatarColor(name) {
      const colors = ['#2A6F97', '#3A8DB8', '#8EC8D2', '#059669', '#0284c7', '#7c3aed', '#db2777', '#ea580c']
      if (!name) return colors[0]
      let hash = 0
      for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash)
      }
      return colors[Math.abs(hash) % colors.length]
    },
    buildQuery() {
      const query = { pageNum: this.pageNum, pageSize: this.pageSize }
      const val = this.keyword.trim()
      if (val) {
        if (/^\d+$/.test(val)) {
          query.phonenumber = val
        } else {
          query.userName = val
        }
      }
      return query
    },
    async fetchList(reset) {
      this.loading = true
      try {
        const res = await request({ url: '/system/user/list', method: 'GET', data: this.buildQuery() })
        const list = res.rows || res.data || []
        this.rows = reset ? list : this.rows.concat(list)
        this.finished = list.length < this.pageSize
      } catch (e) {
        console.error('加载用户列表失败', e)
      } finally {
        this.loading = false
      }
    },
    async refresh() {
      this.pageNum = 1
      this.finished = false
      this.refreshing = true
      await this.fetchList(true)
      this.refreshing = false
    },
    async loadMore() {
      if (this.loading || this.finished) return
      this.pageNum += 1
      await this.fetchList(false)
    },
    openDetail(item) {
      uni.navigateTo({ url: `/pages/user/detail?id=${item.userId}` })
    },
    addUser() {
      uni.navigateTo({ url: '/pages/user/form' })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F0F4F8;
}

.search-wrap {
  display: flex;
  gap: 12rpx;
  padding: 20rpx 28rpx;
  background: #FFFFFF;
  box-shadow: 0 2rpx 8rpx rgba(42, 111, 151, 0.04);
}

.search {
  flex: 1;
  height: 80rpx;
  padding: 0 28rpx;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
  border-radius: 999rpx;
  font-size: 26rpx;
}

.search-button {
  width: 108rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #2A6F97, #3A8DB8);
  color: #FFFFFF;
  font-size: 26rpx;
  border-radius: 999rpx;
}

.scroll {
  height: calc(100vh - 120rpx);
  padding: 16rpx 0 30rpx;
}

.user-item {
  display: flex;
  align-items: center;
  margin: 0 28rpx 12rpx;
  padding: 24rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(42, 111, 151, 0.04);
}

.user-item--active {
  background: #F0F4F8;
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  font-weight: 700;
  color: #FFFFFF;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  margin-left: 20rpx;
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.user-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1A2332;
}

.nick-name {
  font-size: 26rpx;
  color: #5A6B7F;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 8rpx;
}

.user-phone {
  font-size: 24rpx;
  color: #94A3B8;
}

.status-pill {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  font-weight: 500;
}

.status-ok {
  background: #D1FAE5;
  color: #065F46;
}

.status-disabled {
  background: #FEE2E2;
  color: #991B1B;
}

.arrow {
  font-size: 36rpx;
  color: #CBD5E1;
  font-weight: 300;
  flex-shrink: 0;
  margin-left: 12rpx;
}

.fab {
  position: fixed;
  right: 36rpx;
  bottom: 80rpx;
  width: 108rpx;
  height: 108rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #2A6F97, #3A8DB8);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(42, 111, 151, 0.3);
  z-index: 100;
}

.fab-icon {
  font-size: 48rpx;
  color: #FFFFFF;
  font-weight: 300;
}

.empty,
.loading {
  padding: 60rpx;
  text-align: center;
  color: #94A3B8;
  font-size: 26rpx;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1A2332;
}

.empty-subtitle {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #94A3B8;
}
</style>
