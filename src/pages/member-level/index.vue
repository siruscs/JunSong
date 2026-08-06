<template>
  <view class="page" v-if="authorized">
    <view class="header"><text class="eyebrow">会员服务 · {{ deptName }}</text><text class="title">会员等级配置</text><text class="subtitle">等级编码稳定，会员正在使用的等级不允许直接覆盖</text></view>
    <view class="toolbar"><input v-model="keyword" placeholder="搜索等级名称或编码" confirm-type="search" @confirm="load" /><button class="primary" v-if="can('add')" @tap="openCreate">新增等级</button></view>
    <view class="state" v-if="loading">正在加载…</view>
    <view class="state error" v-else-if="error">{{ error }}<button @tap="load">重新加载</button></view>
    <view class="card" v-for="item in filteredRows" :key="item.typeId">
      <view class="card-head"><view><text class="name">{{ item.typeName || '-' }}</text><text class="code">{{ item.typeCode || '-' }}</text></view><text class="status" :class="{ off: String(item.status) !== '0' }">{{ String(item.status) === '0' ? '正常' : '停用' }}</text></view>
      <view class="metrics"><view><text>办卡费用</text><strong>¥{{ money(item.cardFee) }}</strong></view><view><text>折扣率</text><strong>{{ item.discountRate ?? '-' }}</strong></view><view><text>积分倍率</text><strong>{{ item.pointsRate ?? '-' }}</strong></view><view><text>升级成长值</text><strong>{{ item.minGrowth ?? '-' }}</strong></view></view>
      <view class="actions"><button @tap="openView(item)">查看</button><button v-if="can('edit')" @tap="openEdit(item)">编辑</button><button v-if="can('sync')" class="sync" @tap="openSync(item)">同步到其他机构</button></view>
    </view>
    <view class="state" v-if="!loading && !filteredRows.length">暂无会员等级配置</view>
    <view class="mask" v-if="panel" @tap="panel=''">
      <view class="sheet" @tap.stop><view class="sheet-title">{{ viewOnly ? '查看等级配置' : (editing ? '编辑等级配置' : '新增等级配置') }}</view>
        <view class="field"><text>等级名称</text><input v-model="form.typeName" :disabled="viewOnly" placeholder="例如：会员卡" /></view>
        <view class="field"><text>等级编码</text><input v-model="form.typeCode" :disabled="viewOnly || editing" placeholder="例如：formal" /></view>
        <view class="field-row"><view class="field"><text>办卡费用</text><input v-model="form.cardFee" :disabled="viewOnly" type="digit" placeholder="0.00" @input="limit('cardFee',$event.detail.value,2)" /></view><view class="field"><text>折扣率</text><input v-model="form.discountRate" :disabled="viewOnly" type="digit" placeholder="例如：9.5" @input="limit('discountRate',$event.detail.value,2)" /></view></view>
        <view class="field-row"><view class="field"><text>积分倍率</text><input v-model="form.pointsRate" :disabled="viewOnly" type="digit" placeholder="1.00" @input="limit('pointsRate',$event.detail.value,2)" /></view><view class="field"><text>升级成长值</text><input v-model="form.minGrowth" :disabled="viewOnly" type="number" placeholder="0" /></view></view>
        <view class="field"><text>签到积分</text><input v-model="form.signInPoints" :disabled="viewOnly" type="digit" placeholder="0.000" @input="limit('signInPoints',$event.detail.value,3)" /></view>
        <view class="field"><text>状态</text><picker :disabled="viewOnly" :range="statuses" range-key="label" :value="statusIndex" @change="form.status=statuses[Number($event.detail.value)].value"><view class="control">{{ statuses[statusIndex]?.label || '请选择' }}</view></picker></view>
        <view class="sheet-actions"><button @tap="panel=''">关闭</button><button v-if="!viewOnly" class="primary" @tap="save">保存</button></view>
      </view>
    </view>
  </view>
</template>

<script>
import { createMemberLevel, listMemberLevels, updateMemberLevel } from '@/api/memberLevel.js'
import { hasActionPermission, requireModulePermission } from '@/utils/permission.js'
import { workContext } from '@/utils/workContext.js'

const blank = () => ({ typeId: '', typeName: '', typeCode: '', cardFee: '', discountRate: '', pointsRate: '', minGrowth: '', signInPoints: '', status: '0' })

export default {
  data() { return { authorized: false, deptName: '', keyword: '', rows: [], loading: false, error: '', panel: false, editing: false, viewOnly: false, form: blank(), statuses: [{ label: '正常', value: '0' }, { label: '停用', value: '1' }] } },
  computed: { statusIndex() { const i = this.statuses.findIndex(x => String(x.value) === String(this.form.status)); return i < 0 ? 0 : i }, filteredRows() { const key = this.keyword.trim().toLowerCase(); return key ? this.rows.filter(x => String(x.typeName || '').toLowerCase().includes(key) || String(x.typeCode || '').toLowerCase().includes(key)) : this.rows } },
  onLoad() { this.authorized = requireModulePermission('memberLevel'); const scope = workContext.snapshot(); this.deptName = scope.currentDept?.name || scope.currentDept?.deptName || '未选择机构'; if (this.authorized) this.load() },
  methods: {
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
.page{min-height:100vh;padding:24rpx;background:#e8eef5;color:#1a2332;box-sizing:border-box}.header{padding:30rpx;border-left:6rpx solid #087cf0;border-radius:18rpx;background:linear-gradient(110deg,#d9eaff,#f8fbff);box-shadow:0 8rpx 22rpx rgba(46,82,120,.08)}.eyebrow,.subtitle{display:block;color:#8190a1;font-size:23rpx}.eyebrow{color:#087cf0;font-weight:600}.title{display:block;margin-top:10rpx;font-size:38rpx;font-weight:700}.subtitle{margin-top:10rpx}.toolbar{display:flex;gap:14rpx;margin:18rpx 0}.toolbar input{flex:1;min-width:0;padding:20rpx;border:1rpx solid #d5e0ec;border-radius:12rpx;background:#fff}.toolbar button,.actions button,.sheet-actions button{border:0;border-radius:12rpx;background:#fff;color:#334155}.primary{background:#087cf0!important;color:#fff!important}.card{margin-bottom:16rpx;padding:24rpx;border:1rpx solid #d5e0ec;border-radius:18rpx;background:#fff;box-shadow:0 5rpx 18rpx rgba(45,72,98,.06)}.card-head,.actions,.sheet-actions{display:flex;align-items:center;justify-content:space-between;gap:12rpx}.name{display:block;font-size:30rpx;font-weight:700}.code{display:block;margin-top:6rpx;color:#94a3b8;font-size:22rpx}.status{padding:6rpx 14rpx;border-radius:20rpx;background:#e8f8ef;color:#16865a;font-size:21rpx}.status.off{background:#f1f3f5;color:#8190a1}.metrics{display:flex;margin-top:20rpx;padding:16rpx 0;border-top:1rpx solid #edf2f7;border-bottom:1rpx solid #edf2f7}.metrics>view{flex:1}.metrics text{display:block;color:#94a3b8;font-size:20rpx}.metrics strong{display:block;margin-top:6rpx;font-size:24rpx}.actions{justify-content:flex-end;margin-top:18rpx}.actions button{padding:8rpx 16rpx;font-size:22rpx}.sync{color:#087cf0!important}.state{padding:60rpx 20rpx;text-align:center;color:#94a3b8;font-size:25rpx}.state button{display:block;margin:20rpx auto;padding:10rpx 24rpx;border:0;border-radius:10rpx;background:#087cf0;color:#fff}.error{color:#d64545}.mask{position:fixed;inset:0;z-index:20;padding:30rpx 24rpx;background:rgba(15,23,42,.45);overflow:auto}.sheet{padding:26rpx;border-radius:20rpx;background:#fff}.sheet-title{font-size:32rpx;font-weight:700}.field{margin-top:18rpx;flex:1}.field>text{display:block;margin-bottom:8rpx;color:#475569;font-size:23rpx}.field input,.control{box-sizing:border-box;width:100%;min-height:74rpx;padding:18rpx;border:1rpx solid #d5e0ec;border-radius:12rpx;background:#fff}.control{background:#f8fafc;color:#64748b}.field-row{display:flex;gap:14rpx}.sheet-actions{margin-top:24rpx;justify-content:flex-end}.sheet-actions button{min-width:150rpx;padding:12rpx 20rpx}
</style>
