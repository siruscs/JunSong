<template>
  <!-- ════════ 会员等级配置列表页（参考销售记录页面 UI 风格） ════════ -->
  <view class="page" v-if="authorized">
    <!-- 顶部标题栏：渐变背景 + 左边框 -->
    <view class="hero">
      <text class="eyebrow">会员服务</text>
      <text class="hero-title">等级配置</text>
    </view>

    <!-- 部门范围条 -->
    <view class="work-scope" :class="{ 'work-scope-disabled': !switchable }" :hover-class="switchable ? 'work-scope-hover' : ''" hover-stay-time="80" hover-start-time="30" @tap="openDeptSwitcher">
      <view class="work-scope-mark" :class="{ 'work-scope-mark-disabled': !switchable }"></view>
      <view class="work-scope-copy">
        <text class="work-scope-label">{{ scopeLabel }}</text>
        <text class="work-scope-name">{{ currentDeptName || '未选择部门' }}</text>
      </view>
    </view>

    <view class="scroll-pad"></view>
    <view class="bottom-bar">
      <button v-if="can('add')" class="add-button" @tap="openCreate">＋ 新增</button>
    </view>

    <!-- 滚动列表区 -->
    <scroll-view scroll-y class="scroll">
      <!-- 加载中 -->
      <view class="section-card list-card state-card" v-if="loading">
        <view class="empty">正在加载…</view>
      </view>
      <!-- 加载错误 -->
      <view class="section-card list-card state-card" v-else-if="error">
        <view class="empty error">{{ error }}</view>
        <view class="state-actions"><button @tap="load">重新加载</button></view>
      </view>
      <!-- 等级明细列表 -->
      <view class="section-card list-card" v-else-if="rows.length">
        <view class="section-header">
          <view class="section-dot" style="background:#10B981"></view>
          <text class="section-title">等级明细</text>
        </view>
        <!-- 等级卡片（参考费用记录两行式） -->
        <view class="level-item" v-for="item in rows" :key="item.typeId" @tap="openView(item)">
          <view class="level-bar"></view>
          <view class="level-body">
            <view class="level-row1">
              <text class="level-name">{{ item.typeName || '-' }}</text>
              <text class="level-status-tag" v-if="String(item.status) === '1'">（停用）</text>
              <text class="level-growth">升级成长值 {{ item.minGrowth ?? '-' }}</text>
            </view>
            <view class="level-row2">
              <text class="level-meta">办卡费用 ¥{{ money(item.cardFee) }}</text>
              <text class="level-meta">签到积分 {{ item.signInPoints ?? '-' }}</text>
              <text class="level-meta">积分倍率 {{ item.pointsRate ?? '-' }}</text>
              <text class="arrow-icon">›</text>
            </view>
          </view>
        </view>
      </view>
      <!-- 空状态 -->
      <view class="section-card list-card state-card" v-else>
        <view class="empty">暂无等级配置</view>
      </view>
    </scroll-view>

    <!-- ════════ 底部弹出表单面板（新增/查看/编辑） ════════ -->
    <view class="sheet-mask" v-if="panel" @tap="panel=false">
      <view class="sheet-panel" @tap.stop>
        <view class="sheet-title">{{ viewOnly ? '查看等级配置' : (editing ? '编辑等级配置' : '新增等级配置') }}</view>

        <!-- 等级名称 -->
        <view class="sheet-row sheet-row-stack">
          <text class="sheet-label">等级名称</text>
          <input class="sheet-input" v-model="form.typeName" :disabled="viewOnly" placeholder="例如：会员卡" />
        </view>
        <!-- 等级编码 -->
        <view class="sheet-row sheet-row-stack">
          <text class="sheet-label">等级编码</text>
          <input class="sheet-input" v-model="form.typeCode" :disabled="viewOnly || editing" placeholder="例如：formal" />
        </view>
        <!-- 办卡费用 + 折扣率 -->
        <view class="sheet-grid-2col">
          <view class="sheet-row sheet-row-stack">
            <text class="sheet-label">办卡费用</text>
            <input class="sheet-input" v-model="form.cardFee" :disabled="viewOnly" type="digit" placeholder="0.00" @input="limit('cardFee',$event.detail.value,2)" />
          </view>
          <view class="sheet-row sheet-row-stack">
            <text class="sheet-label">折扣率</text>
            <input class="sheet-input" v-model="form.discountRate" :disabled="viewOnly" type="digit" placeholder="例如：9.5" @input="limit('discountRate',$event.detail.value,2)" />
          </view>
        </view>
        <!-- 积分倍率 + 升级成长值 -->
        <view class="sheet-grid-2col">
          <view class="sheet-row sheet-row-stack">
            <text class="sheet-label">积分倍率</text>
            <input class="sheet-input" v-model="form.pointsRate" :disabled="viewOnly" type="digit" placeholder="1.00" @input="limit('pointsRate',$event.detail.value,2)" />
          </view>
          <view class="sheet-row sheet-row-stack">
            <text class="sheet-label">升级成长值</text>
            <input class="sheet-input" v-model="form.minGrowth" :disabled="viewOnly" type="number" placeholder="0" />
          </view>
        </view>
        <!-- 签到积分 -->
        <view class="sheet-row sheet-row-stack">
          <text class="sheet-label">签到积分</text>
          <input class="sheet-input" v-model="form.signInPoints" :disabled="viewOnly" type="digit" placeholder="0.000" @input="limit('signInPoints',$event.detail.value,3)" />
        </view>
        <!-- 状态 -->
        <view class="sheet-row sheet-row-stack">
          <text class="sheet-label">状态</text>
          <picker :disabled="viewOnly" :range="statuses" range-key="label" :value="statusIndex" @change="form.status=statuses[Number($event.detail.value)].value">
            <view class="sheet-picker">{{ statuses[statusIndex]?.label || '请选择' }}<text class="sheet-picker-arrow">▸</text></view>
          </picker>
        </view>

        <!-- 底部按钮 -->
        <view class="sheet-actions">
          <button v-if="!viewOnly" class="sheet-cancel" @tap="panel=false">取消</button>
          <button v-if="!viewOnly" class="sheet-confirm" @tap="save">保存</button>
          <button v-if="viewOnly && can('edit')" class="sheet-confirm" @tap="openEdit(form)">编辑</button>
          <button v-if="viewOnly && can('sync')" class="sheet-cancel" @tap="openSync(form)">同步</button>
        </view>
      </view>
    </view>
    <dept-switcher
      v-model:visible="showDeptSwitcher"
      :current-dept-id="currentDeptId"
      :request-fn="request"
      @change="onDeptSwitcherChanged"
    />
  </view>
</template>

<script>
import { createMemberLevel, listMemberLevels, updateMemberLevel } from '@/api/memberLevel.js'
import { hasActionPermission, requireModulePermission } from '@/utils/permission.js'
import { workContext } from '@/utils/workContext.js'
import DeptSwitcher from '@/components/DeptSwitcher.vue'
import { applyWorkScopeToPage, openDeptSwitcher, handleDeptChanged } from '@/utils/listWorkScope.js'
import { request } from '@/api/index.js'

const blank = () => ({ typeId: '', typeName: '', typeCode: '', cardFee: '', discountRate: '', pointsRate: '', minGrowth: '', signInPoints: '', status: '0' })

export default {
  components: { DeptSwitcher },
  data() { return { authorized: false, showDeptSwitcher: false, scopeLabel: '暂无可用数据范围', contextVersion: 0, currentDeptId: null, currentDeptName: '未选择部门', switchable: false, deptCount: 0, deptName: '', rows: [], loading: false, error: '', panel: false, editing: false, viewOnly: false, form: blank(), statuses: [{ label: '正常', value: '0' }, { label: '停用', value: '1' }] } },
  computed: { statusIndex() { const i = this.statuses.findIndex(x => String(x.value) === String(this.form.status)); return i < 0 ? 0 : i } },
  onLoad() { this.authorized = requireModulePermission('memberLevel'); applyWorkScopeToPage(this); if (this.authorized) this.load() },
  onShow() { const { departmentChanged } = applyWorkScopeToPage(this); if (departmentChanged && this.authorized) this.load() },
  methods: {
    openDeptSwitcher() { return openDeptSwitcher(this) },
    onDeptSwitcherChanged() { return handleDeptChanged(this, () => this.load()) },
    can(action) { return hasActionPermission('memberLevel', action) },
    unwrap(response) { return response?.rows || response?.data?.rows || response?.data || [] },
    money(value) { return Number(value || 0).toFixed(2) },
    limit(key, value, precision) { const s = String(value || '').replace(/[^\d.]/g, '').replace(/\.(?=.*\.)/g, ''); this.form[key] = s.includes('.') ? `${s.split('.')[0]}.${s.split('.')[1].slice(0, precision)}` : s },
    async load() { this.loading = true; this.error = ''; try { this.rows = this.unwrap(await listMemberLevels({ pageNum: 1, pageSize: 200, deptId: workContext.snapshot().currentDeptId })) } catch (e) { this.error = e?.msg || e?.message || '等级配置加载失败' } finally { this.loading = false } },
    openCreate() { this.form = blank(); this.editing = false; this.viewOnly = false; this.panel = true },
    openView(row) { this.form = { ...blank(), ...row }; this.editing = true; this.viewOnly = true; this.panel = true },
    openEdit(row) { this.form = { ...blank(), ...row }; this.editing = true; this.viewOnly = false; this.panel = true },
    openSync(row) { uni.navigateTo({ url: `/pages/config-sync/index?type=LEVEL&sourceRecordId=${row.typeId}` }) },
    async save() { if (!this.form.typeName || !this.form.typeCode) return uni.showToast({ title: '请填写等级名称和编码', icon: 'none' }); const payload = { ...this.form, cardFee: Number(this.form.cardFee || 0), discountRate: Number(this.form.discountRate || 0), pointsRate: Number(this.form.pointsRate || 0), minGrowth: Number(this.form.minGrowth || 0), signInPoints: Number(this.form.signInPoints || 0), idempotencyKey: `mp-level-${this.editing ? this.form.typeId : 'new'}-${Date.now()}` }; try { if (this.editing) await updateMemberLevel(payload); else await createMemberLevel(payload); uni.showToast({ title: '等级配置已保存', icon: 'success' }); this.panel = false; this.load() } catch (e) { uni.showToast({ title: e?.msg || e?.message || '保存失败', icon: 'none' }) } }
  }
}
</script>

<style scoped>
/* ──────────────────────────────────────────────
 * 会员等级配置页皮肤：与销售记录/会员购买保持一致
 * ────────────────────────────────────────────── */
.page{display:flex;flex-direction:column;height:100vh;width:100vw;max-width:750rpx;margin:0 auto;background:#e7eff7;color:#1e293b;box-sizing:border-box;overflow:hidden}

/* ── 顶部标题栏（左边框 + 浅蓝渐变） ── */
.hero{margin:22rpx 30rpx 0;padding:28rpx 30rpx 30rpx;border-left:5rpx solid #1687f5;border-radius:20rpx;background:linear-gradient(110deg,#d9eaff,#f7faff);box-shadow:0 8rpx 22rpx rgba(46,82,120,.08)}
.eyebrow{display:block;color:#1687f5;font-size:24rpx;font-weight:600}
.hero-title{display:block;margin-top:10rpx;color:#1e293b;font-size:38rpx;font-weight:700}

/* ── 部门范围条 ── */
.work-scope{display:flex;align-items:center;margin:24rpx 30rpx;min-height:44rpx;padding:6rpx 0}
.work-scope-hover{background:#eaf3ff;border-radius:8rpx}
.work-scope-mark{width:14rpx;height:14rpx;margin-right:14rpx;border-radius:50%;background:#087CF0}
.work-scope-mark-disabled{background:#087CF0}
.work-scope-copy{display:flex;align-items:baseline;gap:8rpx;color:#708196;font-size:24rpx}
.work-scope-name{color:#1F2937;font-size:28rpx;font-weight:700}

/* ── 通用卡片容器（section-card） ── */
.section-card{background:#fff;border-radius:20rpx;padding:28rpx;margin-top:24rpx;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);box-sizing:border-box;overflow:hidden}
.section-header{display:flex;align-items:center;gap:12rpx;margin-bottom:18rpx}
.section-dot{width:12rpx;height:12rpx;border-radius:50%;flex-shrink:0}
.section-title{font-size:28rpx;font-weight:700;color:#1A2332;flex:1}
.section-link{font-size:22rpx;color:#94A3B8}

/* ── 浮动底部操作栏 ── */
.scroll-pad{height:16rpx;margin:16rpx 0 0}
.bottom-bar{position:fixed;left:0;right:0;bottom:0;display:flex;justify-content:center;gap:16rpx;padding:20rpx 24rpx;padding-bottom:calc(20rpx + env(safe-area-inset-bottom));background:rgba(255,255,255,.96);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border-top:1rpx solid #E2E8F0;z-index:10}
.bottom-bar .add-button{width:320rpx;height:84rpx;line-height:84rpx;background:linear-gradient(135deg,#087CF0,#5AA9E8);color:#FFF;font-size:28rpx;border-radius:999rpx;text-align:center;box-shadow:0 6rpx 20rpx rgba(8,124,240,.25);border:0;padding:0}
.bottom-bar .add-button::after{border:none}
.scroll{padding-bottom:160rpx!important}

/* ── 滚动列表区 ── */
.scroll{flex:1;width:100%;min-height:0;padding:0 30rpx 160rpx!important;box-sizing:border-box;overflow-x:hidden}
.list-card{margin-top:16rpx!important;padding:20rpx 28rpx!important}
.state-card{padding:28rpx 28rpx!important}

/* ── 等级卡片（参考费用记录样式：左色条 + 圆角卡片 + 阴影） ── */
.level-item{display:flex;margin-bottom:18rpx;background:#fff;border-radius:22rpx;border:1rpx solid rgba(226,232,240,.9);box-shadow:0 8rpx 26rpx rgba(8,124,240,.06);overflow:hidden}
.level-item:last-child{margin-bottom:0}
.level-bar{width:6rpx;background:linear-gradient(180deg,#087CF0,#5AA9E8);flex-shrink:0}
.level-body{flex:1;min-width:0;padding:22rpx 24rpx;box-sizing:border-box}
.level-row1{display:flex;align-items:center;gap:18rpx}
.level-name{flex:none;min-width:0;font-size:32rpx;line-height:42rpx;font-weight:800;color:#102A3A;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.level-status-tag{flex:none;font-size:24rpx;color:#EF4444;font-weight:600}
.level-growth{margin-left:auto;flex-shrink:0;font-size:23rpx;line-height:32rpx;color:#087CF0;font-weight:600;background:#edf5ff;padding:4rpx 14rpx;border-radius:999rpx}
.level-row2{display:flex;align-items:center;gap:18rpx;margin-top:12rpx;min-width:0;flex-wrap:wrap}
.level-meta{flex-shrink:0;font-size:23rpx;line-height:32rpx;color:#708196;font-weight:600}
.level-actions{display:flex;gap:8rpx;margin-left:auto}
.level-actions button{border:0;border-radius:10rpx;font-size:22rpx;padding:6rpx 14rpx;background:#EEF3F8;color:#334155;margin:0;line-height:1.6}
.level-actions button::after{border:none}
.level-actions .sync{color:#087cf0!important;background:#edf5ff}
.arrow-icon{font-size:36rpx;color:#CBD5E1;margin-left:auto}

/* ── 空状态 / 错误 / 加载 ── */
.empty{text-align:center;color:#94a3b8;padding:56rpx 0;font-size:23rpx}
.empty.error{color:#d64545}
.state-actions{display:flex;justify-content:center;margin-top:18rpx}
.state-actions button{border:0;border-radius:12rpx;padding:10rpx 28rpx;background:#087CF0;color:#fff;font-size:24rpx}
.state-actions button::after{border:none}

/* ════════════════════════════════════════════════
 * 底部弹出面板（新增/查看/编辑表单）
 * ════════════════════════════════════════════════ */
.sheet-mask{position:fixed;left:0;right:0;top:0;bottom:0;z-index:200;display:flex;align-items:flex-end;background:rgba(15,23,42,.45)}
.sheet-panel{width:100%;max-height:88vh;overflow-y:auto;padding:30rpx 28rpx calc(30rpx + env(safe-area-inset-bottom));border-radius:28rpx 28rpx 0 0;background:#fff;box-sizing:border-box;-webkit-overflow-scrolling:touch}
.sheet-title{font-size:34rpx;font-weight:800;color:#1A2332;margin-bottom:16rpx}

/* 弹出面板 表单行 */
.sheet-row{display:flex;align-items:center;padding:18rpx 0;border-bottom:1rpx solid #E8EEF5}
.sheet-row:last-of-type{border-bottom:0}
.sheet-row-stack{flex-direction:column;align-items:stretch;padding:20rpx 0}
.sheet-label{font-size:24rpx;color:#5A6B7F;margin-bottom:12rpx}
.sheet-row-stack .sheet-label{width:auto;margin-bottom:12rpx}

/* 表单控件：固定高度 84rpx，box-sizing:border-box!important 解决输入框挤压 */
.sheet-input{width:100%;box-sizing:border-box!important;text-align:left;padding:0 24rpx;border:1rpx solid #E2E8F0;border-radius:14rpx;background:#F8FAFC;font-size:28rpx;height:84rpx;line-height:84rpx;color:#1A2332}
.sheet-input[disabled]{background:#F1F5F9;color:#94A3B8}
.sheet-picker{box-sizing:border-box!important;width:100%;text-align:left;justify-content:space-between;padding:0 24rpx;border:1rpx solid #E2E8F0;border-radius:14rpx;background:#F8FAFC;font-size:28rpx;height:84rpx;display:flex;align-items:center;color:#1A2332}
.sheet-picker-arrow{margin-left:8rpx;color:#CBD5E1;font-size:22rpx;flex-shrink:0}
.sheet-grid-2col{display:grid;grid-template-columns:1fr 1fr;gap:14rpx;box-sizing:border-box}

/* 弹出面板 底部按钮 */
.sheet-actions{display:flex;gap:16rpx;margin-top:22rpx;padding-top:8rpx}
.sheet-cancel,.sheet-confirm{flex:1;height:88rpx;line-height:88rpx;border-radius:999rpx;font-size:28rpx;text-align:center;border:none;margin:0;padding:0}
.sheet-cancel{background:#F1F5F9;color:#475569}
.sheet-confirm{background:linear-gradient(135deg,#087CF0,#5AA9E8);color:#fff;box-shadow:0 6rpx 20rpx rgba(8,124,240,.25)}
.sheet-cancel::after,.sheet-confirm::after{border:none}
.sheet-confirm[disabled]{opacity:.5}
</style>
