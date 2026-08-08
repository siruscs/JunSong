<template>
  <!-- 完全对齐 pages/index/index.vue 与 pages/login/index.vue 的部门选择体验：点击式 sheet 列表 -->
  <view v-if="visible" class="dept-modal-mask" @tap="handleClose">
    <view class="dept-modal" @tap.stop>
      <view class="dept-modal-head">
        <text class="dept-modal-title">{{ titleText }}</text>
        <text class="dept-modal-sub">请选择要切换的部门</text>
      </view>
      <scroll-view scroll-y class="dept-list">
        <view
          v-for="(dept, idx) in normalizedDepts"
          :key="dept.id || '__idx_' + idx"
          class="dept-list-item"
          :class="{ active: String(dept.id) === String(pendingDeptId) }"
          @tap="pickDept(dept)"
        >
          <view class="dept-item-mark"><text class="dept-item-mark-text">{{ dept.markChar }}</text></view>
          <view class="dept-item-body">
            <text class="dept-item-name">{{ dept.name }}</text>
            <text v-if="dept.leader" class="dept-item-meta">{{ dept.leader }}</text>
          </view>
          <view v-if="String(dept.id) === String(pendingDeptId)" class="dept-item-check">✓</view>
        </view>
      </scroll-view>
      <view class="dept-modal-foot">
        <button class="dept-btn-cancel" @tap="handleClose">取消</button>
        <button class="dept-btn-confirm" :disabled="!pendingDeptId || switching" @tap="confirmSwitch">确认切换</button>
      </view>
    </view>
  </view>
</template>

<script>
import { workContext } from '@/utils/workContext.js'

function shortToast(title, icon = 'none') {
  try { uni.showToast({ title, icon, duration: 2000 }) } catch (_) { /* ignore */ }
}

export default {
  name: 'DeptSwitcher',
  props: {
    // 是否显示弹窗。父组件 v-model:visible / :visible.sync 均可控制
    visible: { type: Boolean, default: false },
    // 可选部门列表。不传则使用 workContext.snapshot().depts
    depts: { type: Array, default: null },
    // 当前选中的部门 id；不传则使用 workContext.snapshot().currentDeptId
    currentDeptId: { default: null },
    // 请求函数（推荐由父组件注入 @/api/index.js 的 request）。不传则使用原生 uni.request 兜底
    requestFn: { type: Function, default: null },
    // 弹窗标题
    titleText: { type: String, default: '选择门店' }
  },
  data() {
    return {
      pendingDeptId: null,
      switching: false
    }
  },
  computed: {
    snapshot() { return workContext.snapshot() },
    rawDepts() {
      if (Array.isArray(this.depts) && this.depts.length > 0) return this.depts
      return Array.isArray(this.snapshot.depts) ? this.snapshot.depts : []
    },
    normalizedDepts() {
      const seen = new Set()
      return this.rawDepts
        .map((dept) => {
          if (!dept) return null
          const id = dept.id ?? dept.deptId
          let name = dept.name || dept.deptName
          if (typeof name !== 'string') name = name ? String(name) : ''
          name = name.trim()
          if (id === undefined || id === null || id === '' || !name) return null
          const key = String(id)
          if (seen.has(key)) return null
          seen.add(key)
          const leader = dept.leader || dept.leaderName || ''
          const markText = (name || '门').replace(/\s/g, '').charAt(0) || '门'
          return {
            id,
            name,
            leader: typeof leader === 'string' ? leader.trim() : '',
            markChar: markText
          }
        })
        .filter(Boolean)
    }
  },
  watch: {
    visible: {
      immediate: false,
      handler(val) {
        if (val) {
          const cur = this.currentDeptId !== null && this.currentDeptId !== undefined
            ? this.currentDeptId
            : this.snapshot.currentDeptId
          this.pendingDeptId = cur !== undefined && cur !== null ? cur : null
          this.switching = false
        }
      }
    }
  },
  methods: {
    fallbackRequest(opts) {
      return new Promise((resolve, reject) => {
        if (typeof uni === 'undefined') return reject(new Error('uni runtime not available'))
        const getStorage = (key) => { try { return uni.getStorageSync ? uni.getStorageSync(key) : '' } catch (_) { return '' } }
        const base = getStorage('baseUrl') || 'https://www.junsong.vip/prod-api'
        const token = getStorage('access_token') || getStorage('Admin-Token') || getStorage('token') || ''
        const url = /^https?:/.test(opts.url || '') ? opts.url : base + opts.url
        const header = Object.assign({ 'Content-Type': 'application/json;charset=utf-8' }, token ? { Authorization: token.startsWith('Bearer ') || token.startsWith('bearer ') ? token : 'Bearer ' + token } : {})
        uni.request({
          url,
          method: opts.method || 'GET',
          data: opts.data,
          timeout: opts.timeout || 30000,
          header,
          success: (r) => {
            const d = r.data || {}
            const ok = r.statusCode >= 200 && r.statusCode < 300 && (d.code === 200 || d.code === undefined || d.code === 0 || String(d.code) === '200')
            ok ? resolve(d) : reject(Object.assign(new Error(d.msg || d.message || `HTTP ${r.statusCode}`), { code: d.code }))
          },
          fail: (e) => reject(new Error((e && e.errMsg) || '网络错误'))
        })
      })
    },
    pickDept(dept) {
      this.pendingDeptId = dept.id
    },
    handleClose() {
      this.$emit('update:visible', false)
      this.$emit('close')
    },
    async confirmSwitch() {
      const targetId = this.pendingDeptId
      if (targetId === null || targetId === undefined) { shortToast('请先选择部门'); return }
      const curId = this.currentDeptId !== null && this.currentDeptId !== undefined
        ? String(this.currentDeptId)
        : String(this.snapshot.currentDeptId ?? '')
      if (String(targetId) === curId) {
        shortToast('当前已是该部门')
        this.handleClose()
        return
      }
      if (this.switching) return
      const target = this.normalizedDepts.find((d) => String(d.id) === String(targetId))
      if (!target) { shortToast('所选部门无效'); return }
      this.switching = true
      try { uni.showLoading({ title: '切换中...', mask: true }) } catch (_) { /* ignore */ }
      const requester = typeof this.requestFn === 'function' ? this.requestFn : this.fallbackRequest.bind(this)
      try {
        await requester({
          url: `/system/user/switchDept/${target.id}`,
          method: 'POST',
          silent: true,
          timeout: 12000
        })
        workContext.selectDept(target.id)
        uni.hideLoading()
        this.$emit('update:visible', false)
        this.$emit('change', { id: target.id, name: target.name, dept: target })
        shortToast(`已切换至${target.name}`, 'success')
      } catch (err) {
        try { uni.hideLoading() } catch (_) { /* ignore */ }
        const msg = (err && (err.msg || err.message)) ? (err.msg || err.message) : '部门切换失败'
        shortToast(msg.length > 24 ? msg.slice(0, 24) : msg)
      } finally {
        this.switching = false
      }
    }
  }
}
</script>

<style scoped>
/* ===== 部门选择弹窗（样式与 pages/index/index.vue 保持完全一致） ===== */
.dept-modal-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 999;
  display: flex;
  align-items: flex-end;
  background: rgba(15, 23, 42, 0.5);
}

.dept-modal {
  width: 100%;
  border-radius: 32rpx 32rpx 0 0;
  background: #FFFFFF;
  padding: 0 0 env(safe-area-inset-bottom);
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}

.dept-modal-head {
  padding: 32rpx 36rpx 20rpx;
  text-align: center;
}

.dept-modal-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1A2332;
  display: block;
}

.dept-modal-sub {
  font-size: 24rpx;
  color: #94A3B8;
  margin-top: 8rpx;
  display: block;
}

.dept-list {
  max-height: 50vh;
  padding: 0 36rpx 0 20rpx;
  box-sizing: border-box;
}

.dept-list-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 40rpx 24rpx 20rpx;
  border-radius: 16rpx;
  margin-bottom: 8rpx;
  border: 2rpx solid transparent;
  transition: all 0.15s;
  box-sizing: border-box;
  overflow: visible;
}

.dept-list-item.active {
  background: #EAF4F8;
  border: 2rpx solid #087CF0;
}

.dept-item-mark {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dept-item-mark-text {
  font-size: 26rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.dept-item-body {
  flex: 1;
  min-width: 0;
}

.dept-item-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1A2332;
  display: block;
  word-break: break-all;
}

.dept-item-meta {
  font-size: 22rpx;
  color: #94A3B8;
  margin-top: 4rpx;
  display: block;
}

.dept-item-check {
  font-size: 32rpx;
  color: #087CF0;
  font-weight: 700;
  flex-shrink: 0;
  width: 44rpx;
  margin-right: 4rpx;
  text-align: center;
  line-height: 1;
}

.dept-modal-foot {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 28rpx 28rpx;
  border-top: 1rpx solid #F1F5F9;
}

.dept-btn-cancel,
.dept-btn-confirm {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  margin: 0;
  padding: 0;
}

.dept-btn-cancel {
  background: #F1F5F9;
  color: #475569;
}

.dept-btn-confirm {
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
}

.dept-btn-cancel::after,
.dept-btn-confirm::after {
  border: none;
}

.dept-btn-confirm[disabled] {
  opacity: 0.55;
}
</style>
