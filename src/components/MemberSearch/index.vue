<template>
  <view class="member-search-section">
    <view class="member-search-header">
      <text class="section-title">会员查找</text>
      <text class="member-search-hint">支持编号、姓名或手机号</text>
    </view>
    <view class="member-search-row">
      <input
        class="member-search-input"
        v-model="keyword"
        confirm-type="search"
        placeholder="输入会员编号、姓名或手机号"
        @confirm="search"
      />
      <button class="member-search-btn" :disabled="loading" @tap="search">
        {{ loading ? '搜索中' : '搜索' }}
      </button>
    </view>
    <view v-if="selected" class="selected-member">
      <view>
        <text class="selected-member-name">{{ selected.memberName || '-' }}</text>
        <text class="selected-member-no">{{ selected.memberNo || '-' }}</text>
      </view>
      <text class="selected-member-clear" @tap="clear">重新选择</text>
    </view>
    <view v-if="results.length" class="member-result-list">
      <view
        class="member-result"
        v-for="member in results"
        :key="member.memberId"
        @tap="pick(member)"
      >
        <view>
          <text class="member-result-name">{{ member.memberName || '-' }}</text>
          <text class="member-result-no">{{ member.memberNo || '-' }}</text>
        </view>
        <text class="member-result-arrow">›</text>
      </view>
    </view>
    <view class="member-result-empty" v-if="searched && !loading && !results.length">
      未找到匹配会员
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { resolveMemberSearchField } from '@/utils/memberWorkflow.js'

export default {
  name: 'MemberSearch',
  props: {
    deptId: { type: [String, Number], default: '' },
    /** 初始选中的会员对象 */
    initial: { type: Object, default: null }
  },
  emits: ['select', 'clear'],
  data() {
    return {
      keyword: '',
      results: [],
      loading: false,
      searched: false,
      selected: this.initial ? { ...this.initial } : null
    }
  },
  watch: {
    initial(val) {
      this.selected = val ? { ...val } : null
      if (val) {
        this.keyword = `${val.memberNo || ''} ${val.memberName || ''}`.trim()
      }
    }
  },
  methods: {
    async search() {
      const kw = String(this.keyword || '').trim()
      if (!kw) {
        uni.showToast({ title: '请输入会员编号、姓名或手机号', icon: 'none' })
        return
      }
      this.loading = true
      this.searched = true
      try {
        // resolveMemberSearchField 会把纯数字路由到 phone，但会员编号也可能是纯数字
        // 对纯数字关键词同时按 memberNo 和 phone 查询，合并去重，确保会员编号可被搜到
        const key = resolveMemberSearchField(kw)
        const keys = key === 'phone' && /^\d+$/.test(kw) ? ['memberNo', 'phone'] : [key]
        const requests = keys.map((k) => {
          const params = { pageNum: 1, pageSize: 20, [k]: kw }
          if (this.deptId) params.deptId = this.deptId
          return request({ url: '/member/member/list', method: 'GET', data: params, silent: true })
        })
        const responses = await Promise.all(requests)
        const seen = new Set()
        const merged = []
        for (const res of responses) {
          const rows = res?.rows || res?.data?.rows || res?.data || []
          if (!Array.isArray(rows)) continue
          for (const m of rows) {
            const id = m?.memberId
            if (id && !seen.has(id)) { seen.add(id); merged.push(m) }
          }
        }
        this.results = merged
      } catch (e) {
        this.results = []
      } finally {
        this.loading = false
      }
    },
    pick(member) {
      this.selected = { ...member }
      this.keyword = `${member.memberNo || ''} ${member.memberName || ''}`.trim()
      this.results = []
      this.searched = false
      this.$emit('select', member)
    },
    clear() {
      this.selected = null
      this.keyword = ''
      this.results = []
      this.searched = false
      this.$emit('clear')
    }
  }
}
</script>

<style scoped>
.member-search-section{margin-top:20rpx;padding:20rpx;background:#f8fbff;border:1rpx solid #e1eaf5;border-radius:14rpx}
.member-search-header{display:flex;align-items:center;justify-content:space-between}
.member-search-hint{font-size:22rpx;color:#94a3b8}
.member-search-row{display:flex;align-items:center;gap:12rpx;margin-top:14rpx;width:100%;box-sizing:border-box}
.member-search-input{flex:1;min-width:0;height:84rpx;line-height:84rpx;padding:0 24rpx;border:2rpx solid #E2E8F0;border-radius:14rpx;background:#F5F8FA;font-size:28rpx;color:#1A2332;box-sizing:border-box!important}
.member-search-btn{flex:none;width:140rpx;height:84rpx;line-height:84rpx;margin:0;padding:0;border-radius:14rpx;background:#087CF0;color:#fff;font-size:26rpx;text-align:center;border:none}
.member-search-btn::after{border:none}
.member-search-btn[disabled]{opacity:.6}
.selected-member{display:flex;align-items:center;justify-content:space-between;margin-top:14rpx;padding:16rpx;border-radius:12rpx;background:#edf6ff}
.selected-member-name,.member-result-name{display:block;color:#1e293b;font-weight:600}
.selected-member-no,.member-result-no{display:block;margin-top:5rpx;color:#64748b;font-size:22rpx}
.selected-member-clear{color:#087cf0;font-size:23rpx}
.member-result-list{margin-top:12rpx;background:#fff;border-radius:12rpx}
.member-result{display:flex;align-items:center;justify-content:space-between;padding:16rpx;border-bottom:1rpx solid #eef2f7}
.member-result-arrow{color:#94a3b8;font-size:34rpx}
.member-result-empty{padding:18rpx 0;color:#94a3b8;font-size:23rpx;text-align:center}
</style>
