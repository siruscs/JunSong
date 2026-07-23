<template>
  <view class="page">
    <view class="hero">
      <view>
        <view class="eyebrow">系统管理</view>
        <view class="hero-title">部门管理</view>
      </view>
    </view>

    <view class="search-wrap">
      <input class="search" v-model="queryValue" placeholder="搜索部门名称" confirm-type="search" @confirm="refresh" />
      <button class="search-button" @tap="refresh">查询</button>
    </view>

    <scroll-view scroll-y class="tree-scroll" :scroll-with-animation="true">
      <view class="tree-container">
        <view
          v-for="node in filteredTree"
          :key="node.deptId"
          class="tree-node"
          :class="{ 'tree-node--active': selectedId === node.deptId }"
          @tap="selectNode(node)"
        >
          <view class="node-indent" :style="{ paddingLeft: (node.level || 1) * 32 + 'rpx' }">
            <view class="node-expand" v-if="node.children && node.children.length > 0" @tap.stop="toggleExpand(node.deptId)">
              <text class="expand-icon" :class="{ expanded: expandedIds.includes(node.deptId) }">›</text>
            </view>
            <view class="node-expand-placeholder" v-else></view>
            <view class="node-content">
              <text class="node-name">{{ node.deptName }}</text>
              <text class="node-leader" v-if="node.leader">负责人: {{ node.leader }}</text>
            </view>
          </view>
          <view v-if="expandedIds.includes(node.deptId) && node.children && node.children.length > 0">
            <view
              v-for="child in node.children"
              :key="child.deptId"
              class="tree-node"
              :class="{ 'tree-node--active': selectedId === child.deptId }"
              @tap="selectNode(child)"
            >
              <view class="node-indent" :style="{ paddingLeft: ((child.level || 1) * 32 + 40) + 'rpx' }">
                <view class="node-expand" v-if="child.children && child.children.length > 0" @tap.stop="toggleExpand(child.deptId)">
                  <text class="expand-icon" :class="{ expanded: expandedIds.includes(child.deptId) }">›</text>
                </view>
                <view class="node-expand-placeholder" v-else></view>
                <view class="node-content">
                  <text class="node-name">{{ child.deptName }}</text>
                  <text class="node-leader" v-if="child.leader">负责人: {{ child.leader }}</text>
                </view>
              </view>
              <view v-if="expandedIds.includes(child.deptId) && child.children && child.children.length > 0">
                <view
                  v-for="grandchild in child.children"
                  :key="grandchild.deptId"
                  class="tree-node"
                  :class="{ 'tree-node--active': selectedId === grandchild.deptId }"
                  @tap="selectNode(grandchild)"
                >
                  <view class="node-indent" :style="{ paddingLeft: ((grandchild.level || 1) * 32 + 80) + 'rpx' }">
                    <view class="node-expand" v-if="grandchild.children && grandchild.children.length > 0" @tap.stop="toggleExpand(grandchild.deptId)">
                      <text class="expand-icon" :class="{ expanded: expandedIds.includes(grandchild.deptId) }">›</text>
                    </view>
                    <view class="node-expand-placeholder" v-else></view>
                    <view class="node-content">
                      <text class="node-name">{{ grandchild.deptName }}</text>
                      <text class="node-leader" v-if="grandchild.leader">负责人: {{ grandchild.leader }}</text>
                    </view>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view class="empty" v-if="!loading && (!treeData || treeData.length === 0)">
          <text>暂无部门数据</text>
        </view>
        <view class="loading" v-if="loading">加载中...</view>
      </view>
    </scroll-view>

    <view class="bottom-bar" v-if="hasAddPermission">
      <button class="add-button" @tap="addDept">＋ 新增部门</button>
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { hasActionPermission } from '@/utils/permission.js'

export default {
  data() {
    return {
      queryValue: '',
      treeData: [],
      loading: false,
      selectedId: null,
      expandedIds: []
    }
  },
  computed: {
    hasAddPermission() {
      return hasActionPermission('deptManage', 'add')
    },
    filteredTree() {
      if (!this.queryValue.trim()) return this.treeData
      const keyword = this.queryValue.toLowerCase()
      const filterNode = (nodes) => {
        return nodes.map(node => {
          const children = node.children ? filterNode(node.children) : []
          const matched = node.deptName?.toLowerCase().includes(keyword) || children.length > 0
          if (matched) {
            return { ...node, children }
          }
          return null
        }).filter(Boolean)
      }
      return filterNode(this.treeData)
    }
  },
  async onLoad() {
    await this.loadTree()
  },
  onShow() {
    this.loadTree()
  },
  methods: {
    async loadTree() {
      this.loading = true
      try {
        const res = await request({ url: '/system/dept/list', method: 'GET' })
        const list = res.data || res.rows || []
        this.treeData = this.buildTree(list)
      } catch (e) {
        console.error('加载部门树失败', e)
        this.treeData = []
      } finally {
        this.loading = false
      }
    },
    buildTree(list) {
      const map = {}
      const roots = []
      for (const item of list) {
        map[item.deptId] = { ...item, children: [] }
      }
      for (const item of list) {
        const node = map[item.deptId]
        const parentId = node.parentId
        if (parentId !== 0 && parentId !== null && parentId !== undefined && map[parentId]) {
          map[parentId].children.push(node)
        } else {
          roots.push(node)
        }
      }
      return roots
    },
    toggleExpand(deptId) {
      const index = this.expandedIds.indexOf(deptId)
      if (index > -1) {
        this.expandedIds.splice(index, 1)
      } else {
        this.expandedIds.push(deptId)
      }
    },
    selectNode(node) {
      this.selectedId = node.deptId
      uni.navigateTo({ url: `/pages/dept/detail?id=${node.deptId}` })
    },
    addDept() {
      uni.navigateTo({ url: '/pages/dept/form' })
    },
    refresh() {
      this.loadTree()
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #E8EEF5;
  width: 100vw;
  max-width: 100vw;
  overflow-x: hidden;
  box-sizing: border-box;
}

.hero {
  padding: 36rpx 28rpx 24rpx;
  background: #FFFFFF;
  border-left: 6rpx solid #087CF0;
  margin: 24rpx 28rpx 0;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 16rpx rgba(8, 124, 240, 0.06);
}

.eyebrow {
  font-size: 22rpx;
  color: #087CF0;
  font-weight: 600;
}

.hero-title {
  margin-top: 6rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #1A2332;
}

.search-wrap {
  display: flex;
  gap: 12rpx;
  margin: 16rpx 28rpx 0;
  padding: 0;
}

.search {
  flex: 1;
  min-width: 0;
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
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
  font-size: 26rpx;
  border-radius: 999rpx;
}

.tree-scroll {
  width: 100%;
  height: calc(100vh - 350rpx);
  padding: 20rpx 28rpx 150rpx;
  box-sizing: border-box;
}

.tree-container {
  padding-bottom: 40rpx;
}

.tree-node {
  margin-bottom: 8rpx;
}

.tree-node--active {
  background: rgba(8, 124, 240, 0.08);
  border-radius: 12rpx;
}

.node-indent {
  display: flex;
  align-items: center;
  padding: 16rpx 20rpx;
  gap: 12rpx;
}

.node-expand {
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.node-expand-placeholder {
  width: 44rpx;
  flex-shrink: 0;
}

.expand-icon {
  font-size: 32rpx;
  color: #94A3B8;
  transition: transform 0.2s;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.node-content {
  flex: 1;
  min-width: 0;
}

.node-name {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-leader {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #94A3B8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty,
.loading {
  padding: 60rpx;
  text-align: center;
  color: #94A3B8;
  font-size: 26rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1rpx solid #E2E8F0;
}

.add-button {
  width: 320rpx;
  height: 84rpx;
  line-height: 84rpx;
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
  font-size: 28rpx;
  border-radius: 999rpx;
  text-align: center;
  box-shadow: 0 6rpx 20rpx rgba(8, 124, 240, 0.25);
}
</style>