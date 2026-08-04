<template>
  <StateView v-if="formState !== 'normal'" :status="formState" :message="loadError" @retry="retryInit" />
  <view class="page" v-else-if="config">
    <view class="hero-card">
      <view class="hero-icon">{{ id ? '✎' : '＋' }}</view>
      <view class="hero-info">
        <view class="hero-title">{{ id ? '编辑' : '新增' }}{{ config.title }}</view>
        <view class="hero-meta">{{ config.group }} · 请完善必要信息后保存</view>
      </view>
    </view>

    <view class="section-card member-search-card" v-if="showMemberSearch">
      <view class="section-header">
        <view class="section-dot required"></view>
        <text class="section-title">选择会员</text>
        <text class="section-count" v-if="form.memberId">已选择</text>
      </view>
      <view class="member-search-row">
        <input
          class="member-search-input"
          v-model="memberKeyword"
          confirm-type="search"
          placeholder="输入会员编号或姓名"
          @confirm="searchMembers"
        />
        <button class="member-search-btn" @tap="searchMembers">搜索</button>
      </view>
      <view class="selected-member" v-if="form.memberId">
        <view>
          <text class="selected-member-name">{{ form.memberName || '-' }}</text>
          <text class="selected-member-no">{{ form.memberNo || '-' }}</text>
        </view>
        <text class="selected-member-tag" v-if="moduleKey === 'pointsExchange' && memberPoints !== ''">剩余积分 {{ memberPoints }}</text>
        <text class="selected-member-tag" v-else>当前会员</text>
      </view>
      <view class="member-result-list" v-if="memberResults.length">
        <view class="member-result" v-for="member in memberResults" :key="member.memberId" @tap="selectMember(member)">
          <view>
            <text class="member-result-name">{{ member.memberName || '-' }}</text>
            <text class="member-result-no">{{ member.memberNo || '-' }}</text>
          </view>
          <text class="member-result-arrow">›</text>
        </view>
      </view>
      <view class="member-result-empty" v-if="memberSearched && !memberLoading && memberResults.length === 0">
        未找到匹配会员
      </view>
    </view>

    <view class="section-card" v-if="pointsCalc">
      <view class="section-header">
        <view class="section-dot preview"></view>
        <text class="section-title">积分核算</text>
      </view>
      <view class="points-calc-grid">
        <view class="points-calc-row">
          <text class="points-calc-label">需要积分</text>
          <text class="points-calc-value">{{ pointsCalc.deduct }}</text>
        </view>
        <view class="points-calc-row">
          <text class="points-calc-label">可用积分</text>
          <text class="points-calc-value">{{ pointsCalc.memberPts }}</text>
        </view>
        <view class="points-calc-row">
          <text class="points-calc-label">实际扣减</text>
          <text class="points-calc-value">{{ pointsCalc.actualDeduct }}</text>
        </view>
        <view class="points-calc-row alert" v-if="pointsCalc.insufficient">
          <text class="points-calc-label">积分不足，需补差价</text>
          <text class="points-calc-value warning">{{ pointsCalc.extra }} 积分对应金额</text>
        </view>
      </view>
    </view>

    <view class="section-card" v-if="requiredFields.length">
      <view class="section-header">
        <view class="section-dot required"></view>
        <text class="section-title">必填信息</text>
        <ExpenseForm v-if="moduleKey === 'expense' && !id" :image-url="ocrImageUrl" :loading="ocrLoading" @choose-ocr-image="chooseOcrImage" />
      </view>
      <FormField v-for="field in requiredFields" :key="field.key" :field="field" required :value="form[field.key]" :options="optionLabels(field)" :selected-index="optionIndex(field)" :display-text="displayOption(field)" :region-range="regionRange" :region-index="regionIndex" :region-text="regionText" :input-type="inputType(field)" :readonly="isReadonlyField(field)" :placeholder="fieldPlaceholder(field)" :image-url="resolveImageUrl(form[field.key])" @set-value="setValueFromField(field.key, $event)" @select-value="selectValueFromField(field, $event)" @region-column-change="onRegionColumnChange" @region-change="onRegionChange" @input-value="inputValueFromField(field.key, $event)" @choose-image="chooseImage(field.key)" />
    </view>

    <PurchaseDetailsForm v-if="moduleKey === 'purchase'" :details="form.details" :products="productOptions" :total-quantity="form.totalQuantity" :total-amount="form.totalAmount" @add="addPurchaseDetail" @remove="deletePurchaseDetail" @product-change="onPurchaseProductChange" @quantity-input="onDetailQuantityInput" @quantity-blur="onDetailQuantityBlur" @amount="calculateDetailAmount" @gift="onPurchaseGift" />

    <view class="section-card" v-if="optionalFields.length">
      <view class="section-header collapsible" @tap="optionalCollapsed = !optionalCollapsed">
        <view class="section-dot"></view>
        <text class="section-title">其他信息</text>
        <text class="section-count">{{ optionalFields.length }}项</text>
        <text class="collapse-arrow" :class="{ collapsed: optionalCollapsed }">›</text>
      </view>
      <FormField v-for="field in optionalFields" v-show="!optionalCollapsed" :key="field.key" :field="field" :value="form[field.key]" :options="optionLabels(field)" :selected-index="optionIndex(field)" :display-text="displayOption(field)" :region-range="regionRange" :region-index="regionIndex" :region-text="regionText" :input-type="inputType(field)" :readonly="isReadonlyField(field)" :placeholder="fieldPlaceholder(field)" :image-url="resolveImageUrl(form[field.key])" @set-value="setValueFromField(field.key, $event)" @select-value="selectValueFromField(field, $event)" @region-column-change="onRegionColumnChange" @region-change="onRegionChange" @input-value="inputValueFromField(field.key, $event)" @choose-image="chooseImage(field.key)" />
    </view>

    <view class="preview-card" v-if="previewData">
      <view class="section-header">
        <view class="section-dot preview"></view>
        <text class="section-title">核算预览</text>
      </view>
      <view class="preview-grid">
        <view class="preview-item" v-for="key in readonlyKeys" :key="key">
          <text class="preview-label">{{ readonlyLabel(key) }}</text>
          <text class="preview-value">{{ previewData[key] === undefined || previewData[key] === null ? '-' : previewData[key] }}</text>
        </view>
      </view>
    </view>

    <view class="footer">
      <button class="btn-secondary" v-if="mode === 'preview'" @tap="preview">
        <text class="btn-icon">◉</text> 预览
      </button>
      <button class="btn-primary" v-if="canSubmit" :disabled="submitting" @tap="submit">
        <text class="btn-icon">✓</text> {{ submitting ? '保存中' : (mode === 'preview' ? '生成核算' : '保存') }}
      </button>
    </view>

    <view class="member-success-mask" v-if="memberCreateSuccess">
      <view class="member-success-dialog">
        <text class="member-success-title">会员新增成功</text>
        <view class="member-success-row">
          <text class="member-success-label">会员姓名</text>
          <text class="member-success-name">{{ memberCreateSuccess.memberName }}</text>
        </view>
        <view class="member-success-row">
          <text class="member-success-label">会员编号</text>
          <text class="member-success-no">{{ memberCreateSuccess.memberNo }}</text>
        </view>
        <button class="member-success-confirm" @tap="closeMemberCreateSuccess">知道了</button>
      </view>
    </view>
  </view>
</template>

<script>
import miniProgramShare from '@/mixins/miniProgramShare.js'
import { getModule, displayValue } from '@/config/modules.js'
import { getData, addData, updateData, request } from '@/api/index.js'
import { hasActionPermission, requireModulePermission } from '@/utils/permission.js'
import { isUnknownWriteOutcome } from '@/utils/operationState.js'
import { validateMemberContact } from '@/utils/memberWorkflow.js'
import { saveDraft, loadDraft, clearDraft } from '@/utils/draftStore.js'
import PurchaseDetailsForm from './form-modules/PurchaseDetailsForm.vue'
import FormField from './form-modules/FormField.vue'
import ExpenseForm from './form-modules/ExpenseForm.vue'
import StateView from '@/components/StateView.vue'

export default {
  mixins: [miniProgramShare],
  components: { PurchaseDetailsForm, FormField, ExpenseForm, StateView },
  data() {
    return {
      moduleKey: '',
      config: null,
      id: '',
      mode: '',
      routeOptions: {},
      form: {},
      previewData: null,
      dictCache: {},
      ocrImageUrl: '',
      ocrLoading: false,
      submitting: false,
      submitted: false,
      memberCreateSuccess: null,
      memberKeyword: '',
      memberResults: [],
      memberLoading: false,
      memberSearched: false,
      optionalCollapsed: false,
      memberPoints: '',
      pointsGoodsPrice: 0,
      productOptions: [],
      regionIndex: [0, 0, 0, 0],
      regionRange: [[], [], [], []],
      regionOptions: [],
      draftRestoring: true,
      initializing: false,
      loadError: ''
    }
  },
  computed: {
    isSeckillRecordCreate() {
      return this.moduleKey === 'seckillRecord' && !this.id
    },
    showMemberSearch() {
      return (this.isSeckillRecordCreate || (this.moduleKey === 'pointsExchange' && !this.id))
    },
    pointsCalc() {
      if (this.moduleKey !== 'pointsExchange' || !this.form.goodsId) return null
      const goodsPrice = Number(this.pointsGoodsPrice || 0)
      const qty = Number(this.form.quantity || 0)
      const memberPts = Number(this.memberPoints || 0)
      if (!goodsPrice || !qty) return null
      const deduct = goodsPrice * qty
      const actualDeduct = Math.min(deduct, memberPts)
      const insufficient = memberPts < deduct
      const extra = insufficient ? Math.ceil((deduct - memberPts)) : 0
      return { deduct, actualDeduct, insufficient, extra, memberPts }
    },
    productLabels() {
      return this.productOptions.map((p) => p.productName || '')
    },
    requiredFields() {
      return this.config?.fields.filter((f) => f.required && !this.shouldHideFormField(f)) || []
    },
    optionalFields() {
      return this.config?.fields.filter((f) => !f.required && !this.shouldHideFormField(f)) || []
    },
    readonlyKeys() {
      return this.config?.readonlyFields || []
    },
    canSubmit() {
      return hasActionPermission(this.moduleKey, this.id ? 'edit' : 'add')
    },
    regionText() {
      return [this.form.provinceName, this.form.cityName, this.form.districtName, this.form.streetName].filter(Boolean).join(' / ')
    },
    formState() {
      if (this.initializing) return 'loading'
      if (this.loadError) return 'error'
      return this.config ? 'normal' : 'empty'
    }
  },
  watch: {
    form: {
      deep: true,
      handler(value) {
        if (!this.id && this.moduleKey && !this.draftRestoring && !this.submitted) {
          const deptId = this.getCurrentDeptId()
          const userId = this.getCurrentUserId()
          if (deptId) saveDraft(this.moduleKey, deptId, value, userId)
        }
      }
    }
  },
  onLoad(options) {
    this.initializing = true
    this.moduleKey = options.module
      this.config = getModule(this.moduleKey)
      this.id = options.id || ''
      this.mode = options.mode || ''
      this.routeOptions = options || {}
    if (this.config && requireModulePermission(this.moduleKey)) {
      if (!hasActionPermission(this.moduleKey, this.id ? 'edit' : 'add')) {
        uni.showToast({ title: this.id ? '暂无编辑权限' : '暂无新增权限', icon: 'none' })
        setTimeout(() => uni.navigateBack(), 500)
        return
      }
      uni.setNavigationBarTitle({ title: this.id ? '编辑' + this.config.title : '新增' + this.config.title })
      this.initForm().catch(e => { this.loadError = e?.msg || '表单加载失败' }).finally(() => { this.initializing = false })
    } else {
      this.initializing = false
      this.loadError = '暂无可用表单'
    }
  },
  methods: {
    retryInit() { this.loadError = ''; this.initializing = true; this.initForm().catch(e => { this.loadError = e?.msg || '表单加载失败' }).finally(() => { this.initializing = false }) },
    async initForm() {
      this.config.fields.forEach((field) => {
        this.form[field.key] = ''
      })
      if (this.hasRegionField()) await this.loadRegionOptions()
      this.resetRegionPicker()
      if (this.moduleKey === 'deptManage' && !this.id) {
        this.form.parentId = 0
        this.form.orderNum = 0
        this.form.status = '0'
      }
      if (this.moduleKey === 'member' && !this.id) {
        this.form.joinDate = this.todayStr()
        this.form.status = '0'
      }
      if (this.moduleKey === 'expense' && !this.id) {
        this.form.expenseDate = this.todayStr()
        this.form.expenseType = '开支'
        this.form.status = '0'
      }
      if (this.moduleKey === 'investorPayment' && !this.id) {
        this.form.paymentDate = this.todayStr()
        this.form.paymentType = 'return'
        this.form.sourceType = '0'
        this.form.paymentStatus = '1'
      }
      if (this.moduleKey === 'investor' && !this.id) {
        this.form.status = '0'
      }
      if (this.moduleKey === 'deptProfitConfig' && !this.id) {
        this.form.autoCreateInvestorPayment = '1'
        this.form.status = '0'
      }
      if (this.moduleKey === 'investRecord' && !this.id) {
        this.form.investTime = this.todayStr()
      }
      if (this.moduleKey === 'pointsGoods' && !this.id) {
        this.form.status = '0'
      }
      if (this.moduleKey === 'product' && !this.id) {
        this.form.status = '0'
      }
      if (this.moduleKey === 'supplier' && !this.id) {
        this.form.status = '0'
      }
      if (this.moduleKey === 'purchase') {
        if (!this.id) {
          this.form.purchaseDate = this.todayStr()
          this.form.status = '2'
          this.form.totalAmount = 0
          this.form.totalQuantity = 0
        }
        if (!this.form.details) this.form.details = []
        this.optionalCollapsed = true
        this.loadProductOptions()
      }
      if (this.moduleKey === 'pointsExchange' && !this.id) {
        this.form.exchangeDate = this.todayStr()
        this.form.quantity = 1
        this.form.status = '0'
      }
      if (this.moduleKey === 'seckillRecord' && !this.id) {
        this.form.seckillId = this.routeOptions.seckillId || ''
        this.form.seckillPrice = this.routeOptions.seckillPrice || ''
        this.form.seckillDate = this.routeOptions.seckillDate || this.todayStr()
        this.form.shares = this.form.shares || 1
        this.form.status = '0'
        this.calculateSeckillTotal()
      }
      if (this.moduleKey === 'seckill' && !this.id) {
        this.form.seckillType = '1'
        this.form.seckillDate = this.todayStr()
        this.form.endDate = this.todayStr()
        this.form.status = '0'
        this.form.totalShares = 0
        this.form.remainShares = 0
      }
      if (this.moduleKey === 'sale' && !this.id) {
        this.form.saleDate = this.todayStr()
        this.form.status = '0'
      }
      if (this.moduleKey === 'advance' && !this.id) {
        this.form.advanceDate = this.todayStr()
        this.form.status = '0'
      }
      if (!this.id) await this.restoreDraft()
      this.draftRestoring = false
      if (this.id) await this.loadInfo()
      this.loadDictOptions()
    },
    async restoreDraft() {
      const deptId = this.getCurrentDeptId()
      if (!deptId) return
      const userId = this.getCurrentUserId()
      const draft = loadDraft(this.moduleKey, deptId, userId)
      if (!draft || !Object.keys(draft).length) return
      await new Promise((resolve) => uni.showModal({ title: '发现未完成草稿', content: '检测到未完成的填写，是否恢复？', confirmText: '恢复', cancelText: '放弃', success: (result) => { if (result.confirm) this.form = { ...this.form, ...draft }; else clearDraft(this.moduleKey, deptId, userId); resolve() }, fail: resolve }))
    },
    todayStr() {
      const d = new Date()
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
    },
    shouldHideFormField(field) {
      if (field.hidden) return true
      if (field.formHidden) return true
      if (this.isSeckillRecordCreate && ['seckillId', 'memberId', 'memberNo', 'memberName', 'status'].includes(field.key)) return true
      if (this.moduleKey === 'pointsExchange' && !this.id && ['memberId', 'memberName', 'pointsDeducted', 'status', 'exchangeNo'].includes(field.key)) return true
      return false
    },
    isReadonlyField(field) {
      if (this.config?.readonlyFields?.includes(field.key)) return true
      if (this.moduleKey === 'member' && !this.id && field.serverGenerated) return true
      return this.isSeckillRecordCreate && field.key === 'totalAmount'
    },
    fieldPlaceholder(field) {
      if (this.moduleKey === 'member' && !this.id && field.serverGenerated) return '系统自动生成'
      return '请输入' + field.label
    },
    onFieldInput(key, value) {
      // 数量类字段：输入过程只做字符过滤（允许数字、单个小数点、开头负号），字符过滤后立刻触发总量重算
      if (['saleQuantity', 'giftQuantity', 'totalQuantity'].includes(key)) {
        this.form[key] = this.sanitizeDecimal(value, true)
        if (this.moduleKey === 'sale' && (key === 'saleQuantity' || key === 'giftQuantity')) {
          // 销售单：销售/赠品数量变化时，立即安全重算 totalQuantity = saleQuantity + giftQuantity，默认 3 位小数
          const s = this.toNum3(this.form.saleQuantity)
          const g = this.toNum3(this.form.giftQuantity)
          this.form.totalQuantity = Number((s + g).toFixed(3))
        }
      } else {
        this.form[key] = value
      }
      if (this.isSeckillRecordCreate && key === 'shares') this.calculateSeckillTotal()
      if (this.moduleKey === 'pointsExchange' && key === 'quantity') this.syncPointsDeducted()
    },
    // 小数字符过滤：允许数字、单个小数点、可选首位负号；只保留3位小数位以内。空、'-'、'0.'、'.'都视为合法过程态。
    sanitizeDecimal(raw, allowNegative = false) {
      if (raw === null || raw === undefined) return ''
      let s = String(raw)
      // 1) 只保留数字、小数点、首位负号
      let sign = ''
      if (allowNegative && s.startsWith('-')) { sign = '-'; s = s.slice(1) }
      let dotIdx = s.indexOf('.')
      if (dotIdx >= 0) {
        s = s.slice(0, dotIdx).replace(/[^\d]/g, '') + '.' + s.slice(dotIdx + 1).replace(/[^\d]/g, '')
        // 只保留一个小数点
        const parts = s.split('.')
        s = parts[0] + (parts.length > 1 ? '.' + parts.slice(1).join('').slice(0, 3) : '')
      } else {
        s = s.replace(/[^\d]/g, '')
      }
      return sign + s
    },
    // 安全解析合法数值：过程态（''、'-'、'.'、'0.') → 0；返回 Number，支持 3 位小数。改用 Number.toFixed(3) 消除 IEEE 754 舍入误差
    finalizeDecimal(raw) {
      let s = String(raw == null ? '' : raw).trim()
      if (s === '' || s === '-' || s === '.') return 0
      if (s.startsWith('-.')) s = '-0.' + s.slice(2)
      if (s.startsWith('.')) s = '0' + s
      if (s.endsWith('.')) s = s.slice(0, -1)
      // 兜底：任何非数字/./- 的字符全部移除
      s = s.replace(/[^0-9.\-]/g, '')
      const n = Number(s)
      if (!Number.isFinite(n)) return 0
      return Number(n.toFixed(3))
    },
    calculateSeckillTotal() {
      const price = Number(this.form.seckillPrice || 0)
      const shares = Number(this.form.shares || 0)
      if (price && shares) {
        this.form.totalAmount = (price * shares).toFixed(2)
      } else {
        this.form.totalAmount = ''
      }
    },
    async searchMembers() {
      const keyword = String(this.memberKeyword || '').trim()
      if (!keyword) {
        uni.showToast({ title: '请输入会员编号或姓名', icon: 'none' })
        return
      }
      this.memberLoading = true
      this.memberSearched = true
      try {
        const firstKey = /^[A-Za-z0-9]/.test(keyword) ? 'memberNo' : 'memberName'
        let res = await request({ url: '/member/member/list', method: 'GET', data: { pageNum: 1, pageSize: 20, [firstKey]: keyword } })
        let list = res.rows || res.data || []
        if ((!list || list.length === 0) && firstKey === 'memberNo') {
          res = await request({ url: '/member/member/list', method: 'GET', data: { pageNum: 1, pageSize: 20, memberName: keyword } })
          list = res.rows || res.data || []
        }
        this.memberResults = Array.isArray(list) ? list : []
      } catch (e) {
        console.error('搜索会员失败', e)
        this.memberResults = []
      } finally {
        this.memberLoading = false
      }
    },
    selectMember(member) {
      this.form.memberId = member.memberId
      this.form.memberNo = member.memberNo
      this.form.memberName = member.memberName
      this.memberKeyword = `${member.memberNo || ''} ${member.memberName || ''}`.trim()
      this.memberResults = []
      if (this.moduleKey === 'pointsExchange') {
        this.fetchMemberPoints(member.memberId)
      }
      uni.showToast({ title: '已选择会员', icon: 'none' })
    },
    async fetchMemberPoints(memberId) {
      try {
        const res = await request({ url: '/member/pointsRecord/list', method: 'GET', data: { memberId, pageNum: 1, pageSize: 1 } })
        const rows = res.rows || res.data || []
        this.memberPoints = rows[0]?.balance ?? ''
        if (this.memberPoints === '') this.memberPoints = 0
      } catch (e) {
        console.error('获取会员积分失败', e)
        this.memberPoints = 0
      }
      this.syncPointsDeducted()
    },
    syncPointsDeducted() {
      if (this.moduleKey !== 'pointsExchange') return
      if (this.pointsCalc) {
        this.form.pointsDeducted = this.pointsCalc.actualDeduct
        if (this.pointsCalc.insufficient) {
          this.form.extraAmount = this.pointsCalc.extra
        }
      }
    },
    async loadDictOptions() {
      const dictFields = this.config.fields.filter((f) => f.dictType)
      for (const field of dictFields) {
        try {
          let list = this.dictCache[field.dictType]
          if (!list) {
            const res = await request({ url: '/system/dict/data/type/' + field.dictType, method: 'GET' })
            list = res.data || []
            this.dictCache[field.dictType] = list
          }
          field.options = list.map((d) => ({ label: d.dictLabel, value: d.dictValue, raw: d }))
          this.applyDictDefault(field, list)
        } catch (e) {
          console.log('加载字典失败:', field.dictType, e)
        }
      }
      // 加载部门员工下拉（借款人）
      const staffFields = this.config.fields.filter((f) => f.remoteDeptStaff && f.type === 'select')
      for (const field of staffFields) {
        try {
          const deptId = this.getCurrentDeptId()
          if (!deptId) continue
          const res = await request({ url: '/system/userDept/staff/' + deptId, method: 'GET' })
          const list = res.data || []
          const labelKey = field.remoteLabel || 'nickName'
          const valueKey = field.remoteValue || 'nickName'
          field.options = list.map((d) => ({ label: d[labelKey], value: d[valueKey], raw: d }))
        } catch (e) {
          console.log('加载部门员工失败:', e)
        }
      }
      // 加载远程下拉选项（如商品列表）
      const remoteFields = this.config.fields.filter((f) => f.remoteUrl && f.type === 'select')
      for (const field of remoteFields) {
        try {
          const params = { pageNum: 1, pageSize: 200 }
          if (field.remoteFilterDept) {
            const deptId = this.getCurrentDeptId()
            if (deptId) params.deptId = deptId
          }
          if (field.remoteFilterStatus) params.status = field.remoteFilterStatus
          const cacheKey = 'remote:' + field.remoteUrl + (field.remoteFilterDept ? ':dept' : '') + (field.remoteFilterStatus || '')
          let list = this.dictCache[cacheKey]
          if (!list) {
            const res = await request({ url: field.remoteUrl, method: 'GET', data: params })
            list = res.rows || res.data || []
            this.dictCache[cacheKey] = list
          }
          const labelKey = field.remoteLabel || 'name'
          const valueKey = field.remoteValue || 'id'
          field.options = list.map((d) => ({ label: d[labelKey], value: d[valueKey], raw: d }))
        } catch (e) {
          console.log('加载远程选项失败:', field.remoteUrl, e)
        }
      }
    },
    getCurrentDeptId() {
      const userInfo = uni.getStorageSync('userInfo') || {}
      return userInfo.currentDeptId || userInfo.deptId || null
    },
    getCurrentUserId() {
      const userInfo = uni.getStorageSync('userInfo') || {}
      return userInfo.userId || userInfo.id || userInfo.user?.userId || null
    },
    closeMemberCreateSuccess() {
      this.memberCreateSuccess = null
      setTimeout(() => uni.navigateBack(), 300)
    },
    applyDictDefault(field, list) {
      if (this.id || field.key !== 'paymentMethod' || this.form[field.key]) return
      const defaultItem = list.find((item) => item.isDefault === 'Y')
      if (defaultItem) this.form[field.key] = defaultItem.dictValue
    },
    async loadInfo() {
      try {
        const path = this.config.detailPath || this.config.path
        const res = await getData(path, this.id)
        this.form = { ...this.form, ...(res.data || res) }
        this.syncRegionPickerFromForm()
      } catch (e) {
        console.error('加载详情失败', e)
        this.loadError = e?.msg || '加载详情失败'
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    setValue(key, value) {
      this.form[key] = value
    },
    setValueFromField(key, value) {
      this.setValue(key, value)
    },
    inputValueFromField(key, value) {
      this.onFieldInput(key, value)
    },
    inputType(field) {
      if (field.allowNegative) return 'text'
      if (field.type === 'number') return 'digit'
      if (field.type === 'phone') return 'number'
      return 'text'
    },
    optionItems(field) {
      // 从config中查找正确的field引用，防止v-for中field对象引用错误
      const configField = this.config?.fields.find(f => f.key === field.key) || field
      return (configField.options || []).map((item) => typeof item === 'string' ? { label: item, value: item } : item)
    },
    optionLabels(field) {
      return this.optionItems(field).map((item) => item.label)
    },
    optionIndex(field) {
      const configField = this.config?.fields.find(f => f.key === field.key) || field
      const index = this.optionItems(configField).findIndex((item) => String(item.value) === String(this.form[configField.key]))
      return index < 0 ? 0 : index
    },
    selectValueFromField(field, index) {
      this.selectValue(field, index)
    },
    selectValue(field, index) {
      const item = this.optionItems(field)[Number(index)]
      if (!item) return
      this.form[field.key] = item.value
      // 选择商品时同步填充商品名称
      if (field.remoteUrl && field.remoteLabel && item.raw) {
        this.form[field.remoteLabel] = item.raw[field.remoteLabel] || ''
      }
      // 积分兑换：选择物品后获取积分价格
      if (this.moduleKey === 'pointsExchange' && field.key === 'goodsId' && item.raw) {
        this.pointsGoodsPrice = Number(item.raw.pointsPrice || 0)
        this.form.goodsName = item.raw.goodsName || ''
        this.syncPointsDeducted()
      }
      // 投资款记录：选择投资人后同步deptId
      if (this.moduleKey === 'investRecord' && field.key === 'investorId' && item.raw) {
        this.form.deptId = item.raw.deptId || this.getCurrentDeptId()
      }
      // 费用表单：切换费用类别时不应影响付款方式
      if (this.moduleKey === 'expense' && field.key === 'expenseType') {
        const savedPaymentMethod = this.form.paymentMethod
        this.$nextTick(() => {
          if (this.form.paymentMethod !== savedPaymentMethod) {
            this.form.paymentMethod = savedPaymentMethod
          }
        })
      }
    },
    hasRegionField() {
      return (this.config?.fields || []).some((field) => field.type === 'region')
    },
    async loadRegionOptions() {
      const res = await request({ url: '/system/region/tree', method: 'GET' })
      this.regionOptions = res.data || []
    },
    resetRegionPicker() {
      this.regionIndex = [0, 0, 0, 0]
      this.refreshRegionRange()
    },
    refreshRegionRange() {
      const provinces = this.regionOptions
      const cities = provinces[this.regionIndex[0]]?.children || []
      const districts = cities[this.regionIndex[1]]?.children || []
      const streets = districts[this.regionIndex[2]]?.children || []
      this.regionRange = [
        provinces.map((item) => item.label),
        cities.map((item) => item.label),
        districts.map((item) => item.label),
        streets.map((item) => item.label)
      ]
      this.regionIndex = [
        Math.min(this.regionIndex[0], Math.max(provinces.length - 1, 0)),
        Math.min(this.regionIndex[1], Math.max(cities.length - 1, 0)),
        Math.min(this.regionIndex[2], Math.max(districts.length - 1, 0)),
        Math.min(this.regionIndex[3], Math.max(streets.length - 1, 0))
      ]
    },
    onRegionColumnChange(event) {
      const column = Number(event.detail.column)
      const value = Number(event.detail.value)
      const next = [...this.regionIndex]
      next[column] = value
      for (let i = column + 1; i < next.length; i += 1) next[i] = 0
      this.regionIndex = next
      this.refreshRegionRange()
    },
    onRegionChange(event) {
      this.regionIndex = event.detail.value || [0, 0, 0, 0]
      this.refreshRegionRange()
      this.applyRegionByIndex()
    },
    applyRegionByIndex() {
      const province = this.regionOptions[this.regionIndex[0]]
      const city = province?.children?.[this.regionIndex[1]]
      const district = city?.children?.[this.regionIndex[2]]
      const street = district?.children?.[this.regionIndex[3]]
      this.form.provinceCode = province?.value || ''
      this.form.provinceName = province?.label || ''
      this.form.cityCode = city?.value || ''
      this.form.cityName = city?.label || ''
      this.form.districtCode = district?.value || ''
      this.form.districtName = district?.label || ''
      this.form.streetCode = street?.value || ''
      this.form.streetName = street?.label || ''
    },
    findRegionIndex(options, value) {
      return Math.max(options.findIndex((item) => String(item.value) === String(value)), 0)
    },
    syncRegionPickerFromForm() {
      const provinceIndex = this.findRegionIndex(this.regionOptions, this.form.provinceCode)
      const cities = this.regionOptions[provinceIndex]?.children || []
      const cityIndex = this.findRegionIndex(cities, this.form.cityCode)
      const districts = cities[cityIndex]?.children || []
      const districtIndex = this.findRegionIndex(districts, this.form.districtCode)
      const streets = districts[districtIndex]?.children || []
      const streetIndex = this.findRegionIndex(streets, this.form.streetCode)
      this.regionIndex = [provinceIndex, cityIndex, districtIndex, streetIndex]
      this.refreshRegionRange()
    },
    displayOption(field) {
      // 从config中查找正确的field引用，防止v-for中field对象引用错误
      const configField = this.config?.fields.find(f => f.key === field.key) || field
      const val = displayValue(configField, this.form[configField.key])
      return val === '-' ? '请选择' : val
    },
    readonlyLabel(key) {
      const map = {
        totalExpense: '总花销费用',
        totalPurchase: '总进货金额',
        totalSale: '总销售金额',
        totalPayment: '总缴款金额',
        totalInvest: '投资来源金额',
        currentAdvance: '当前借支金额',
        periodNo: '当前周期',
        totalVerifiedExpense: '已核销费用',
        totalSalePayment: '销售缴款',
        totalUnverifiedAdvance: '借支未核销',
        netProfit: '净利',
        managerProfitRate: '店长分润比例',
        managerProfitAmount: '店长分润',
        investorProfitAmount: '投资人返款',
        returnSituation: '回本情况'
      }
      return map[key] || key
    },
    validate() {
      if (this.isSeckillRecordCreate && !this.form.memberId) {
        uni.showToast({ title: '请先选择会员', icon: 'none' })
        return false
      }
      if (this.moduleKey === 'pointsExchange' && !this.id && !this.form.memberId) {
        uni.showToast({ title: '请先选择会员', icon: 'none' })
        return false
      }
      if (this.moduleKey === 'member') {
        const contactError = validateMemberContact(this.form)
        if (contactError) {
          uni.showToast({ title: contactError, icon: 'none' })
          return false
        }
      }
      const missing = this.config.fields.find((field) => field.required && !this.shouldHideFormField(field) && !this.form[field.key] && this.form[field.key] !== 0)
      if (missing) {
        uni.showToast({ title: '请填写' + missing.label, icon: 'none' })
        return false
      }
      if (this.moduleKey === 'purchase') {
        if (!this.form.details || this.form.details.length === 0) {
          uni.showToast({ title: '请添加商品明细', icon: 'none' })
          return false
        }
        const invalid = this.form.details.find((d) => !d.productId)
        if (invalid) {
          uni.showToast({ title: '请选择所有商品', icon: 'none' })
          return false
        }
      }
      return true
    },
    async preview() {
      if (!this.validate()) return
      try {
        const res = await request({
          url: '/finance/costAccounting/preview',
          method: 'GET',
          data: { startDate: this.form.startDate, endDate: this.form.endDate }
        })
        this.previewData = res.data || res
      } catch (e) {
        console.error('预览失败', e)
      }
    },
    buildSubmitData() {
      const data = { ...this.form }
      ;(this.config.fields || []).forEach((field) => {
        if (field.virtual) delete data[field.key]
      })
      // 投资款记录新增时强制设置当前部门ID
      if (this.moduleKey === 'investRecord' && !this.id) {
        data.deptId = this.getCurrentDeptId()
      }
      if (this.moduleKey === 'member' && !this.id) delete data.memberNo
      // 所有数量类字段统一收敛成合法小数值（3位小数内）—— 发送给后端前最后一层兜底
      if (typeof data.saleQuantity !== 'undefined') data.saleQuantity = this.toNum3(data.saleQuantity)
      if (typeof data.giftQuantity !== 'undefined') data.giftQuantity = this.toNum3(data.giftQuantity)
      if (typeof data.totalQuantity !== 'undefined') data.totalQuantity = this.toNum3(data.totalQuantity, true)
      if (Array.isArray(data.details)) {
        data.details = data.details.map((d) => ({
          ...d,
          quantity: this.toNum3(d.quantity, true),
          price: d.price === '' || d.price === null || d.price === undefined ? 0 : Number(Number(d.price).toFixed(2)),
          amount: d.amount === '' || d.amount === null || d.amount === undefined ? 0 : Number(Number(d.amount).toFixed(2))
        }))
      }
      return data
    },
    async submit() {
      if (!this.canSubmit) {
        uni.showToast({ title: '暂无提交权限', icon: 'none' })
        return
      }
      if (this.submitting || this.submitted) return
      if (this.isSeckillRecordCreate) this.calculateSeckillTotal()
      if (!this.validate()) return
      this.submitting = true
      try {
        const submitData = this.buildSubmitData()
        if (this.id) {
          submitData[this.config.idKey] = this.id
          await updateData(this.config.path, submitData)
          clearDraft(this.moduleKey, this.getCurrentDeptId(), this.getCurrentUserId())
          this.submitted = true
          uni.showToast({ title: '保存成功' })
          setTimeout(() => uni.navigateBack(), 500)
        } else {
          const res = await addData(this.config.path, submitData)
          clearDraft(this.moduleKey, this.getCurrentDeptId(), this.getCurrentUserId())
          this.submitted = true
          const savedData = res.data || submitData
          if (this.moduleKey === 'member') {
            this.memberCreateSuccess = {
              memberName: savedData.memberName || this.form.memberName || '-',
              memberNo: savedData.memberNo || this.form.memberNo || '-'
            }
          } else {
            uni.showToast({ title: '保存成功' })
            setTimeout(() => uni.navigateBack(), 500)
          }
        }
      } catch (e) {
        if (isUnknownWriteOutcome(e)) {
          uni.showModal({
            title: '保存结果待确认',
            content: '网络中断，系统暂时无法确认是否保存成功。请返回列表刷新核对后，再决定是否重新提交。',
            showCancel: false
          })
        }
      } finally {
        this.submitting = false
      }
    },
    async loadProductOptions() {
      try {
        const params = { pageNum: 1, pageSize: 200, status: '0' }
        const deptId = this.getCurrentDeptId()
        if (deptId) params.deptId = deptId
        const res = await request({ url: '/finance/product/list', method: 'GET', data: params })
        this.productOptions = res.rows || res.data || []
      } catch (e) {
        console.error('加载商品列表失败', e)
        this.productOptions = []
      }
    },
    productIndex(productId) {
      const idx = this.productOptions.findIndex((p) => p.productId === productId)
      return idx < 0 ? 0 : idx
    },
    addPurchaseDetail() {
      if (!this.form.details) this.form.details = []
      this.form.details.push({
        detailId: undefined,
        purchaseId: undefined,
        productId: undefined,
        productName: undefined,
        unit: undefined,
        quantity: '',
        price: '',
        amount: 0,
        isGift: '0'
      })
    },
    deletePurchaseDetail(index) {
      this.form.details.splice(index, 1)
      this.calculatePurchaseTotal()
    },
    onDetailProductChange(index, pickerIndex) {
      const product = this.productOptions[Number(pickerIndex)]
      if (!product) return
      const detail = this.form.details[index]
      detail.productId = product.productId
      detail.productName = product.productName
      detail.unit = product.unit
      detail.price = Number(product.purchasePrice || 0)
      this.calculateDetailAmount(index)
    },
    onPurchaseProductChange(payload) {
      this.onDetailProductChange(payload.index, payload.value)
    },
    onDetailQuantityInput(index) {
      const detail = this.form.details[index]
      if (!detail) return
      // 注意：不在 input 过程中直接覆盖 detail.quantity（避免 v-model 双向绑定竞争，真机上会造成小数点被吞）。
      // 过程态显示由 input 自身维护；失焦时通过 finalizeDecimal 收敛为合法 Number。
      // 这里只触发重新计算（计算函数内部会通过 toNum3 做安全解析）
      this.calculateDetailAmount(index)
    },
    onDetailQuantityBlur(index) {
      const detail = this.form.details[index]
      if (!detail) return
      // 失焦：统一收敛为合法小数值 Number
      detail.quantity = this.finalizeDecimal(detail.quantity)
      // 同时把 price 也转为 Number，避免之后 calculate 因字符串歧义算错
      if (detail.price !== '' && detail.price !== null && detail.price !== undefined) {
        detail.price = Number(Number(detail.price).toFixed(2))
      }
      this.calculateDetailAmount(index)
    },
    // 内部：安全的 3 位小数解析。支持字符串过程态（"1."、".55"、"-."）、Number、null/空串，统一返回合法 Number（默认 0）
    toNum3(raw, allowNegative = false) {
      if (raw === null || raw === undefined) return 0
      let s = String(raw).trim()
      if (s === '' || s === '-' || s === '.') return 0
      let sign = 1
      if (s.startsWith('-')) { if (allowNegative) sign = -1; s = s.slice(1) }
      if (s.startsWith('.')) s = '0' + s
      if (s.endsWith('.')) s = s.slice(0, -1)
      s = s.replace(/[^0-9.]/g, '')
      // 只保留一个小数点：取第一个 "." 为界
      const firstDot = s.indexOf('.')
      if (firstDot >= 0) {
        const intPart = s.slice(0, firstDot)
        const fracPart = s.slice(firstDot + 1).replace(/\./g, '').slice(0, 3)
        s = intPart + '.' + fracPart
      }
      const n = Number(s)
      if (!Number.isFinite(n) || n < 0) return allowNegative ? sign * 0 : 0
      return Number((sign * n).toFixed(3))
    },
    calculateDetailAmount(index) {
      const detail = this.form.details[index]
      if (!detail) return
      // 全部安全解析：允许负数数量（红冲/返库场景）
      const qty = this.toNum3(detail.quantity, true)
      if (detail.isGift === '1') {
        detail.price = 0
        detail.amount = 0
      } else {
        const price = Number(Number(detail.price || 0).toFixed(2)) || 0
        detail.amount = Number((price * qty).toFixed(2))
      }
      this.calculatePurchaseTotal()
    },
    toggleGift(index, checked) {
      const detail = this.form.details[index]
      if (!detail) return
      detail.isGift = checked ? '1' : '0'
      this.calculateDetailAmount(index)
    },
    onPurchaseGift(payload) {
      this.toggleGift(payload.index, payload.value)
    },
    calculatePurchaseTotal() {
      if (!this.form.details || !this.form.details.length) {
        this.form.totalAmount = 0
        this.form.totalQuantity = 0
        return
      }
      let totalAmount = 0
      let totalQuantity = 0
      for (const d of this.form.details) {
        if (d.isGift !== '1') {
          totalAmount += Number(Number(d.amount || 0).toFixed(2)) || 0
        }
        totalQuantity += this.toNum3(d.quantity, true)
      }
      this.form.totalAmount = Number(totalAmount.toFixed(2))
      this.form.totalQuantity = Number(totalQuantity.toFixed(3))
    },
    chooseImage(key) {
      uni.showActionSheet({
        itemList: ['📷 拍照', '🖼 从相册选择'],
        success: (res) => {
          const sourceType = res.tapIndex === 0 ? ['camera'] : ['album']
          uni.chooseImage({
            count: 1,
            sourceType,
            sizeType: ['original', 'compressed'],
            success: (imgRes) => {
              this.uploadImage(key, imgRes.tempFilePaths[0])
            }
          })
        }
      })
    },
    resolveImageUrl(url) {
      if (!url) return ''
      if (url.startsWith('http://') || url.startsWith('https://')) return url
      if (url.startsWith('/statics/')) {
        const baseUrl = uni.getStorageSync('baseUrl') || 'https://www.junsong.vip/prod-api'
        const origin = baseUrl.replace(/\/prod-api$/, '').replace(/\/dev-api$/, '')
        return origin + url
      }
      const baseUrl = uni.getStorageSync('baseUrl') || 'https://www.junsong.vip/prod-api'
      return baseUrl + url
    },
    async uploadImage(key, filePath) {
      uni.showLoading({ title: '上传中...' })
      try {
        const baseUrl = uni.getStorageSync('baseUrl') || 'https://www.junsong.vip/prod-api'
        const token = uni.getStorageSync('token')
        const uploadRes = await new Promise((resolve, reject) => {
          uni.uploadFile({
            url: baseUrl + '/file/upload',
            filePath,
            name: 'file',
            timeout: 30000,
            header: { Authorization: 'Bearer ' + token },
            success: (r) => {
              try { resolve(JSON.parse(r.data)) } catch (e) { reject(e) }
            },
            fail: reject
          })
        })
        if (uploadRes.code === 200 && uploadRes.data && uploadRes.data.url) {
          this.form[key] = uploadRes.data.url
          uni.showToast({ title: '上传成功', icon: 'none' })
        } else {
          uni.showToast({ title: uploadRes.msg || '上传失败', icon: 'none' })
        }
      } catch (e) {
        console.error('上传图片失败', e)
        uni.showToast({ title: '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
    chooseOcrImage() {
      uni.showActionSheet({
        itemList: ['📷 拍照识别', '🖼 从相册选择'],
        success: (res) => {
          const sourceType = res.tapIndex === 0 ? ['camera'] : ['album']
          uni.chooseImage({
            count: 1,
            sourceType,
            sizeType: ['original', 'compressed'],
            success: (imgRes) => {
              this.ocrImageUrl = imgRes.tempFilePaths[0]
              this.doOcr(imgRes.tempFilePaths[0])
            }
          })
        }
      })
    },
    async doOcr(filePath) {
      this.ocrLoading = true
      try {
        const baseUrl = uni.getStorageSync('baseUrl') || 'https://www.junsong.vip/prod-api'
        const token = uni.getStorageSync('token')
        const uploadRes = await new Promise((resolve, reject) => {
          uni.uploadFile({
            url: baseUrl + '/finance/expense/ocr',
            filePath,
            name: 'file',
            timeout: 30000,
            header: { Authorization: 'Bearer ' + token },
            success: (r) => {
              try { resolve(JSON.parse(r.data)) } catch (e) { reject(e) }
            },
            fail: reject
          })
        })
        if (uploadRes.code === 200 && uploadRes.data) {
          const data = uploadRes.data
          if (data.amount) this.form.expenseAmount = data.amount
          if (data.date) this.form.expenseDate = data.date
          if (data.content) this.form.expenseContent = data.content
          if (data.platform) this.form.expenseContent = (this.form.expenseContent ? this.form.expenseContent + ' ' : '') + '(' + data.platform + ')'
          uni.showToast({ title: '识别成功，请确认信息', icon: 'none' })
        } else {
          uni.showToast({ title: uploadRes.msg || '识别失败', icon: 'none' })
        }
      } catch (e) {
        console.error('OCR失败', e)
        uni.showToast({ title: '识别失败，请手动填写', icon: 'none' })
      } finally {
        this.ocrLoading = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 0 0 160rpx;
  background: #E8EEF5;
}

.hero-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 36rpx 28rpx;
  background: linear-gradient(135deg, #C7DCF2 0%, #EAF3FC 100%);
  border-top: 1rpx solid #B7D1EB;
  border-radius: 0 0 24rpx 24rpx;
}

.hero-icon {
  width: 72rpx;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 18rpx;
  font-size: 36rpx;
  color: #FFFFFF;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-info {
  flex: 1;
}

.hero-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1F2D3D;
}

.hero-meta {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #6E8197;
}

.section-card,
.preview-card {
  margin: 20rpx 28rpx 0;
  padding: 28rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  border: 1rpx solid #D5E0EC;
  box-shadow: 0 5rpx 18rpx rgba(45, 72, 98, 0.07);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 20rpx;
}

.section-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #087CF0;
}

.section-dot.required {
  background: #EF4444;
}

.section-dot.preview {
  background: #F59E0B;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A2332;
}

.section-count {
  margin-left: auto;
  font-size: 22rpx;
  color: #94A3B8;
}

.collapse-arrow {
  font-size: 32rpx;
  color: #94A3B8;
  transform: rotate(90deg);
  transition: transform 0.2s;
  margin-left: 8rpx;
}

.collapse-arrow.collapsed {
  transform: rotate(0deg);
}

.section-header.collapsible {
  padding: 4rpx 0;
}

.points-calc-grid {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.points-calc-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 16rpx;
  background: #F8FAFC;
  border-radius: 12rpx;
}

.points-calc-row.alert {
  background: #FEF2F2;
}

.points-calc-label {
  font-size: 26rpx;
  color: #475569;
}

.points-calc-value {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A2332;
}

.points-calc-value.warning {
  color: #DC2626;
}

.member-search-card {
  border: 1rpx solid #FED7AA;
  box-shadow: 0 10rpx 30rpx rgba(234, 88, 12, 0.08);
}

.member-search-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.member-search-input {
  flex: 1;
  min-width: 0;
  height: 80rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #FFF7ED;
  border: 1rpx solid #FED7AA;
  color: #1A2332;
  font-size: 26rpx;
  box-sizing: border-box;
}

.member-search-btn {
  width: 116rpx;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 999rpx;
  background: #EA580C;
  color: #FFFFFF;
  font-size: 26rpx;
  font-weight: 700;
}

.selected-member {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-top: 18rpx;
  padding: 20rpx 22rpx;
  border-radius: 18rpx;
  background: #F8FAFC;
  border: 1rpx solid #E2E8F0;
}

.selected-member-name {
  display: block;
  font-size: 30rpx;
  line-height: 38rpx;
  font-weight: 800;
  color: #102A3A;
}

.selected-member-no {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  color: #708196;
}

.selected-member-tag {
  flex-shrink: 0;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: #D1FAE5;
  color: #065F46;
  font-size: 21rpx;
  font-weight: 700;
}

.member-result-list {
  margin-top: 14rpx;
  border-top: 1rpx solid #F1F5F9;
}

.member-result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #F1F5F9;
}

.member-result-name {
  display: block;
  font-size: 28rpx;
  line-height: 36rpx;
  font-weight: 700;
  color: #1A2332;
}

.member-result-no {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #94A3B8;
}

.member-result-arrow {
  flex-shrink: 0;
  font-size: 36rpx;
  color: #FB923C;
}

.member-result-empty {
  margin-top: 16rpx;
  padding: 22rpx 0 2rpx;
  text-align: center;
  font-size: 24rpx;
  color: #94A3B8;
}

.form-item {
  padding-top: 20rpx;
  border-top: 1rpx solid #E8EEF5;
}

.form-item + .form-item {
  border-top: 1rpx solid #E8EEF5;
  padding-top: 20rpx;
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
  font-size: 26rpx;
  font-weight: 700;
}

.control {
  width: 100%;
  min-height: 84rpx;
  padding: 0 24rpx;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
  border-radius: 14rpx;
  font-size: 28rpx;
  color: #1A2332;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.control.input {
  display: block;
  width: 100%;
  height: 84rpx;
  line-height: 84rpx;
}

.control.input:focus {
  border-color: #087CF0;
  background: #FFFFFF;
}

.control:focus {
  border-color: #087CF0;
  background: #FFFFFF;
}

.picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  line-height: 84rpx;
}

.picker-text {
  flex: 1;
  color: #1A2332;
}

.picker:not(.has-value) .picker-text {
  color: #94A3B8;
}

.picker-arrow {
  font-size: 32rpx;
  color: #CBD5E1;
  font-weight: 300;
}

.textarea {
  height: 170rpx;
  padding-top: 20rpx;
}

.preview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
}

.preview-item {
  padding: 18rpx;
  background: #FFFBEB;
  border-radius: 12rpx;
  border: 1rpx solid #FEF3C7;
}

.preview-label,
.preview-value {
  display: block;
}

.preview-label {
  font-size: 22rpx;
  color: #92400E;
}

.preview-value {
  margin-top: 6rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #B45309;
}

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
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1rpx solid #E2E8F0;
}

.btn-primary,
.btn-secondary {
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
}

.btn-primary {
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
  box-shadow: 0 6rpx 20rpx rgba(8, 124, 240, 0.25);
}

.btn-secondary {
  background: #FFFFFF;
  color: #5A6B7F;
  border: 1rpx solid #E2E8F0;
}

.btn-icon {
  font-size: 28rpx;
}

.image-upload {
  width: 100%;
  box-sizing: border-box;
}

.image-preview {
  position: relative;
  width: 100%;
  height: 280rpx;
  border-radius: 12rpx;
  overflow: hidden;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
}

.upload-image {
  width: 100%;
  height: 100%;
}

.image-replace-text {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  background: rgba(0, 0, 0, 0.5);
  color: #FFFFFF;
  font-size: 22rpx;
}

.image-picker {
  width: 100%;
  height: 280rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  border-radius: 12rpx;
  border: 2rpx dashed #CBD5E1;
  background: #F5F8FA;
}

.image-picker-icon {
  font-size: 64rpx;
  color: #94A3B8;
  font-weight: 300;
  line-height: 1;
}

.image-picker-text {
  font-size: 24rpx;
  color: #94A3B8;
}

.add-detail-btn {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 18rpx;
  background: #EFF6FF;
  border-radius: 999rpx;
  border: 1rpx solid #BFDBFE;
}

.add-detail-icon {
  font-size: 28rpx;
  color: #087CF0;
  font-weight: 700;
}

.add-detail-text {
  font-size: 22rpx;
  color: #087CF0;
  font-weight: 500;
}

.detail-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.detail-item {
  padding: 20rpx;
  background: #F8FAFC;
  border-radius: 16rpx;
  border: 1rpx solid #E2E8F0;
}

.detail-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.detail-item-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1A2332;
}

.detail-delete {
  font-size: 24rpx;
  color: #EF4444;
  padding: 4rpx 12rpx;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 8rpx 0;
}

.detail-label {
  width: 80rpx;
  font-size: 24rpx;
  color: #64748B;
  flex-shrink: 0;
}

.detail-value-text {
  flex: 1;
  font-size: 26rpx;
  color: #1A2332;
  font-weight: 500;
}

.detail-value-text.amount {
  color: #087CF0;
  font-weight: 700;
}

.detail-control {
  width: 100%;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20rpx;
  background: #FFFFFF;
  border-radius: 10rpx;
  border: 1rpx solid #E2E8F0;
  box-sizing: border-box;
}

.detail-picker {
  flex: 1;
  width: 0;
}

.detail-control.has-value {
  border-color: #087CF0;
}

.detail-picker-text {
  font-size: 26rpx;
  color: #1A2332;
}

.detail-input {
  flex: 1;
  height: 64rpx;
  padding: 0 20rpx;
  background: #FFFFFF;
  border-radius: 10rpx;
  border: 1rpx solid #E2E8F0;
  font-size: 26rpx;
  color: #1A2332;
}

.detail-empty {
  padding: 40rpx 0;
  text-align: center;
}

.detail-empty-text {
  font-size: 24rpx;
  color: #94A3B8;
}

.detail-summary {
  margin-top: 20rpx;
  padding: 16rpx 20rpx;
  background: #F0F9FF;
  border-radius: 12rpx;
  border: 1rpx solid #BAE6FD;
}

.detail-summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6rpx 0;
}

.detail-summary-label {
  font-size: 26rpx;
  color: #475569;
}

.detail-summary-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #087CF0;
}

.member-success-mask {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  background: rgba(15, 23, 42, 0.58);
}

.member-success-dialog {
  width: 100%;
  padding: 48rpx 40rpx 32rpx;
  background: #FFFFFF;
  border-radius: 28rpx;
  box-sizing: border-box;
  box-shadow: 0 24rpx 60rpx rgba(15, 23, 42, 0.24);
}

.member-success-title {
  display: block;
  margin-bottom: 32rpx;
  text-align: center;
  font-size: 36rpx;
  font-weight: 800;
  color: #0F766E;
}

.member-success-row {
  display: flex;
  align-items: baseline;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #E2E8F0;
}

.member-success-label {
  width: 150rpx;
  flex: none;
  font-size: 26rpx;
  color: #64748B;
}

.member-success-name,
.member-success-no {
  flex: 1;
  font-size: 42rpx;
  line-height: 1.25;
  font-weight: 800;
  color: #DC2626;
  word-break: break-all;
}

.member-success-confirm {
  display: block;
  width: 100%;
  height: 78rpx;
  line-height: 78rpx;
  margin-top: 36rpx;
  padding: 0;
  background: #0F766E;
  color: #FFFFFF;
  font-size: 30rpx;
  font-weight: 700;
  border-radius: 999rpx;
}
</style>
