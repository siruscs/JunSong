<template>
  <view class="page" v-if="config">
    <view class="hero-card">
      <view class="hero-icon">{{ id ? '✎' : '＋' }}</view>
      <view class="hero-info">
        <view class="hero-title">{{ id ? '编辑' : '新增' }}{{ config.title }}</view>
        <view class="hero-meta">{{ config.group }} · 请完善必要信息后保存</view>
      </view>
    </view>

    <view class="section-card member-search-card" v-if="isSeckillRecordCreate">
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
        <text class="selected-member-tag">当前会员</text>
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

    <view class="section-card" v-if="requiredFields.length">
      <view class="section-header">
        <view class="section-dot required"></view>
        <text class="section-title">必填信息</text>
        <view class="ocr-btn" v-if="moduleKey === 'expense' && !id" @tap="chooseOcrImage">
          <text class="ocr-icon">📷</text>
          <text class="ocr-label">拍照识别</text>
        </view>
      </view>
      <view class="ocr-preview" v-if="ocrImageUrl">
        <image class="ocr-image" :src="ocrImageUrl" mode="aspectFit" />
        <view class="ocr-overlay" v-if="ocrLoading">
          <text class="ocr-loading-text">识别中...</text>
        </view>
      </view>
      <view class="form-item" v-for="field in requiredFields" :key="field.key">
        <view class="label-row">
          <text class="label">{{ field.label }}</text>
          <text class="required-tag">*</text>
        </view>
        <picker v-if="field.type === 'date'" mode="date" :value="form[field.key] || ''" @change="setValue(field.key, $event.detail.value)">
          <view class="control picker" :class="{ 'has-value': form[field.key] }">
            <text class="picker-text">{{ form[field.key] || '请选择日期' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
        <picker v-else-if="field.type === 'select'" :range="optionLabels(field)" :value="optionIndex(field)" @change="selectValue(field, $event.detail.value)">
          <view class="control picker" :class="{ 'has-value': form[field.key] }">
            <text class="picker-text">{{ displayOption(field) }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
        <picker v-else-if="field.type === 'region'" mode="multiSelector" :range="regionRange" :value="regionIndex" @columnchange="onRegionColumnChange" @change="onRegionChange">
          <view class="control picker" :class="{ 'has-value': regionText }">
            <text class="picker-text">{{ regionText || '请选择省市区街道' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
        <textarea v-else-if="field.type === 'textarea'" class="control textarea" v-model="form[field.key]" maxlength="-1" :placeholder="'请输入' + field.label" />
        <input v-else class="control input" v-model="form[field.key]" :type="inputType(field)" :disabled="isReadonlyField(field)" :placeholder="'请输入' + field.label" @input="onFieldInput(field.key, $event.detail.value)" />
      </view>
    </view>

    <view class="section-card" v-if="optionalFields.length">
      <view class="section-header">
        <view class="section-dot"></view>
        <text class="section-title">其他信息</text>
        <text class="section-count">{{ optionalFields.length }}项</text>
      </view>
      <view class="form-item" v-for="field in optionalFields" :key="field.key">
        <view class="label-row">
          <text class="label">{{ field.label }}</text>
        </view>
        <picker v-if="field.type === 'date'" mode="date" :value="form[field.key] || ''" @change="setValue(field.key, $event.detail.value)">
          <view class="control picker" :class="{ 'has-value': form[field.key] }">
            <text class="picker-text">{{ form[field.key] || '请选择日期' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
        <picker v-else-if="field.type === 'select'" :range="optionLabels(field)" :value="optionIndex(field)" @change="selectValue(field, $event.detail.value)">
          <view class="control picker" :class="{ 'has-value': form[field.key] }">
            <text class="picker-text">{{ displayOption(field) }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
        <picker v-else-if="field.type === 'region'" mode="multiSelector" :range="regionRange" :value="regionIndex" @columnchange="onRegionColumnChange" @change="onRegionChange">
          <view class="control picker" :class="{ 'has-value': regionText }">
            <text class="picker-text">{{ regionText || '请选择省市区街道' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
        <textarea v-else-if="field.type === 'textarea'" class="control textarea" v-model="form[field.key]" maxlength="-1" :placeholder="'请输入' + field.label" />
        <input v-else class="control input" v-model="form[field.key]" :type="inputType(field)" :disabled="isReadonlyField(field)" :placeholder="'请输入' + field.label" @input="onFieldInput(field.key, $event.detail.value)" />
      </view>
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
      <button class="btn-primary" v-if="canSubmit" @tap="submit">
        <text class="btn-icon">✓</text> {{ mode === 'preview' ? '生成核算' : '保存' }}
      </button>
    </view>
  </view>
</template>

<script>
import { getModule, displayValue } from '@/config/modules.js'
import { getData, addData, updateData, request } from '@/api/index.js'
import { hasActionPermission, requireModulePermission } from '@/utils/permission.js'

export default {
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
      memberKeyword: '',
      memberResults: [],
      memberLoading: false,
      memberSearched: false,
      regionIndex: [0, 0, 0, 0],
      regionRange: [[], [], [], []],
      regionOptions: []
    }
  },
  computed: {
    isSeckillRecordCreate() {
      return this.moduleKey === 'seckillRecord' && !this.id
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
    }
  },
  onLoad(options) {
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
      this.initForm()
    }
  },
  methods: {
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
        this.loadNextMemberNo()
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
      if (this.moduleKey === 'seckillRecord' && !this.id) {
        this.form.seckillId = this.routeOptions.seckillId || ''
        this.form.seckillPrice = this.routeOptions.seckillPrice || ''
        this.form.seckillDate = this.routeOptions.seckillDate || this.todayStr()
        this.form.shares = this.form.shares || 1
        this.form.status = '0'
        this.calculateSeckillTotal()
      }
      if (this.id) this.loadInfo()
      this.loadDictOptions()
    },
    todayStr() {
      const d = new Date()
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
    },
    shouldHideFormField(field) {
      if (field.hidden) return true
      if (this.isSeckillRecordCreate && ['seckillId', 'memberId', 'memberNo', 'memberName', 'status'].includes(field.key)) return true
      return false
    },
    isReadonlyField(field) {
      return this.isSeckillRecordCreate && field.key === 'totalAmount'
    },
    onFieldInput(key, value) {
      this.form[key] = value
      if (this.isSeckillRecordCreate && key === 'shares') this.calculateSeckillTotal()
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
      uni.showToast({ title: '已选择会员', icon: 'none' })
    },
    async loadNextMemberNo() {
      try {
        const res = await request({ url: '/member/member/nextNo', method: 'GET' })
        const no = res.msg || res.data || ''
        this.form.memberNo = no
      } catch (e) {
        console.log('获取会员编号失败', e)
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
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    setValue(key, value) {
      this.form[key] = value
    },
    inputType(field) {
      if (field.type === 'number') return 'digit'
      if (field.type === 'phone') return 'number'
      return 'text'
    },
    optionItems(field) {
      return (field.options || []).map((item) => typeof item === 'string' ? { label: item, value: item } : item)
    },
    optionLabels(field) {
      return this.optionItems(field).map((item) => item.label)
    },
    optionIndex(field) {
      const index = this.optionItems(field).findIndex((item) => String(item.value) === String(this.form[field.key]))
      return index < 0 ? 0 : index
    },
    selectValue(field, index) {
      const item = this.optionItems(field)[Number(index)]
      if (item) this.form[field.key] = item.value
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
      const val = displayValue(field, this.form[field.key])
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
      const missing = this.config.fields.find((field) => field.required && !this.shouldHideFormField(field) && !this.form[field.key] && this.form[field.key] !== 0)
      if (missing) {
        uni.showToast({ title: '请填写' + missing.label, icon: 'none' })
        return false
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
      return data
    },
    async submit() {
      if (!this.canSubmit) {
        uni.showToast({ title: '暂无提交权限', icon: 'none' })
        return
      }
      if (this.isSeckillRecordCreate) this.calculateSeckillTotal()
      if (!this.validate()) return
      try {
        const submitData = this.buildSubmitData()
        if (this.id) {
          submitData[this.config.idKey] = this.id
          await updateData(this.config.path, submitData)
          uni.showToast({ title: '保存成功' })
          setTimeout(() => uni.navigateBack(), 500)
        } else {
          const res = await addData(this.config.path, submitData)
          const savedData = res.data || submitData
          if (this.moduleKey === 'member') {
            uni.showModal({
              title: '会员信息',
              content: `会员姓名：${savedData.memberName || this.form.memberName}\n会员编号：${savedData.memberNo || this.form.memberNo}`,
              showCancel: false,
              confirmText: '知道了',
              success: () => {
                setTimeout(() => uni.navigateBack(), 500)
              }
            })
          } else {
            uni.showToast({ title: '保存成功' })
            setTimeout(() => uni.navigateBack(), 500)
          }
        }
      } catch (e) {
        console.error('保存失败', e)
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
        const baseUrl = uni.getStorageSync('baseUrl') || 'http://192.168.1.8:8081'
        const token = uni.getStorageSync('token')
        const uploadRes = await new Promise((resolve, reject) => {
          uni.uploadFile({
            url: baseUrl + '/finance/expense/ocr',
            filePath,
            name: 'file',
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
  background: #F0F4F8;
}

.hero-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 36rpx 28rpx;
  background: linear-gradient(135deg, #2A6F97, #3A8DB8, #8EC8D2);
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
  color: #FFFFFF;
}

.hero-meta {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
}

.section-card,
.preview-card {
  margin: 20rpx 28rpx 0;
  padding: 28rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 16rpx rgba(42, 111, 151, 0.06);
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
  background: #2A6F97;
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
  border-top: 1rpx solid #F0F4F8;
}

.form-item + .form-item {
  border-top: 1rpx solid #F0F4F8;
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

.control:focus {
  border-color: #2A6F97;
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
  background: linear-gradient(135deg, #2A6F97, #3A8DB8);
  color: #FFFFFF;
  box-shadow: 0 6rpx 20rpx rgba(42, 111, 151, 0.25);
}

.btn-secondary {
  background: #FFFFFF;
  color: #5A6B7F;
  border: 1rpx solid #E2E8F0;
}

.btn-icon {
  font-size: 28rpx;
}

.ocr-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  margin-left: auto;
  padding: 6rpx 16rpx;
  background: #2A6F97;
  border-radius: 999rpx;
}

.ocr-icon {
  font-size: 22rpx;
}

.ocr-label {
  font-size: 22rpx;
  color: #FFFFFF;
}

.ocr-preview {
  position: relative;
  margin-bottom: 20rpx;
  border-radius: 14rpx;
  overflow: hidden;
  background: #F5F8FA;
}

.ocr-image {
  width: 100%;
  height: 300rpx;
}

.ocr-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
}

.ocr-loading-text {
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 500;
}
</style>
