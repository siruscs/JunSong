<template>
  <view class="page">
    <view class="hero-card">
      <view class="hero-icon">📋</view>
      <view class="hero-info">
        <view class="hero-title">现场作业</view>
        <view class="hero-meta">{{ currentDeptName }} · 扫码 / 盘点 / 凭证</view>
      </view>
    </view>

    <!-- 扫码区 -->
    <view class="section-card">
      <view class="section-header">
        <view class="section-dot required"></view>
        <text class="section-title">扫码查找</text>
      </view>
      <view class="scan-tabs">
        <view class="scan-tab" :class="{ active: scanTab === 'product' }" @tap="scanTab = 'product'">商品</view>
        <view class="scan-tab" :class="{ active: scanTab === 'member' }" @tap="scanTab = 'member'">会员</view>
      </view>
      <view class="scan-row">
        <input class="scan-input" v-model="scanCode" :placeholder="scanTab === 'product' ? '商品编码或条码' : '会员编号'" confirm-type="search" @confirm="doScan" />
        <button class="scan-btn" @tap="doScan">查找</button>
      </view>
      <view class="scan-result" v-if="scanResult">
        <view class="scan-result-row">
          <text class="scan-result-label">{{ scanTab === 'product' ? '商品' : '会员' }}</text>
          <text class="scan-result-value">{{ scanResult.productName || scanResult.memberName || '-' }}</text>
        </view>
        <view class="scan-result-row" v-if="scanTab === 'product'">
          <text class="scan-result-label">库存</text>
          <text class="scan-result-value">{{ scanResult.stockNum ?? 0 }}</text>
        </view>
      </view>
      <view class="scan-empty" v-if="scanSearched && !scanResult">
        未找到匹配结果
      </view>
    </view>

    <!-- 盘点区（仅商品扫码后显示） -->
    <view class="section-card" v-if="scanTab === 'product' && scanResult">
      <view class="section-header">
        <view class="section-dot required"></view>
        <text class="section-title">库存盘点</text>
      </view>
      <view class="form-row">
        <text class="form-label">系统库存</text>
        <text class="form-value">{{ scanResult.stockNum ?? 0 }}</text>
      </view>
      <view class="form-row">
        <text class="form-label">实际数量</text>
        <input class="form-input" type="digit" v-model="takeForm.actualQuantity" placeholder="0.000" />
      </view>
      <view class="form-row">
        <text class="form-label">差异原因</text>
        <input class="form-input" v-model="takeForm.reason" placeholder="盘盈盘亏原因（必填）" />
      </view>
      <button class="submit-btn" :disabled="submitting" @tap="submitTake">{{ submitting ? '提交中...' : '提交盘点' }}</button>
    </view>

    <!-- 凭证上传区 -->
    <view class="section-card">
      <view class="section-header">
        <view class="section-dot required"></view>
        <text class="section-title">费用凭证</text>
      </view>
      <view class="upload-row">
        <button class="upload-btn" @tap="chooseImage">拍照/选择</button>
        <text class="upload-hint" v-if="!attachmentUrl">未上传凭证</text>
        <text class="upload-done" v-else>已上传 ✓</text>
      </view>
      <view class="upload-preview" v-if="attachmentUrl">
        <image class="preview-img" :src="attachmentUrl" mode="aspectFit" />
      </view>
    </view>
  </view>
</template>

<script>
import { findProductByCode, findMemberByNo } from '@/api/scan.js'
import { uploadAttachment } from '@/api/attachment.js'
import { submitStockTake, generateTakeNo } from '@/api/stocktake.js'
import { saveDraft, loadDraft, clearDraft } from '@/utils/draftStore.js'
import { refreshAfterTaskAction } from '@/utils/taskCenter.js'
import { workContext } from '@/utils/workContext.js'
import { isUnknownWriteOutcome } from '@/utils/operationState.js'

export default {
  data() {
    return {
      scanTab: 'product',
      scanCode: '',
      scanResult: null,
      scanSearched: false,
      takeForm: {
        actualQuantity: '',
        reason: '',
        takeNo: generateTakeNo()
      },
      attachmentUrl: '',
      submitting: false,
      contextVersion: null
    }
  },
  computed: {
    currentDeptName() {
      return workContext.snapshot().currentDept?.name || '-'
    },
    currentDeptId() {
      return workContext.snapshot().currentDeptId
    }
  },
  onLoad() {
    this.contextVersion = workContext.captureVersion()
    this.restoreDraft()
  },
  onShow() {
    const currentVersion = workContext.captureVersion()
    if (this.contextVersion !== null && this.contextVersion !== currentVersion) {
      this.contextVersion = currentVersion
      this.scanResult = null
      this.scanSearched = false
      this.scanCode = ''
      this.restoreDraft()
    }
  },
  methods: {
    restoreDraft() {
      const draft = loadDraft('fieldWork', this.currentDeptId, this.currentUserId())
      if (draft) {
        this.takeForm.actualQuantity = draft.actualQuantity || ''
        this.takeForm.reason = draft.reason || ''
      }
    },
    currentUserId() {
      const user = workContext.snapshot().user || uni.getStorageSync('userInfo') || {}
      return user.userId || user.id || user.user?.userId || null
    },
    async doScan() {
      if (!this.scanCode) return
      this.scanSearched = false
      this.scanResult = null
      try {
        const deptId = this.currentDeptId
        if (this.scanTab === 'product') {
          this.scanResult = await findProductByCode(this.scanCode, deptId)
        } else {
          this.scanResult = await findMemberByNo(this.scanCode)
        }
        this.scanSearched = true
      } catch (err) {
        this.scanSearched = true
        if (err?.code !== 'AUTH_EXPIRED' && err?.code !== 'PERMISSION_DENIED') {
          uni.showToast({ title: '查找失败', icon: 'none' })
        }
      }
    },
    chooseImage() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const filePath = res.tempFilePaths[0]
          this.doUpload(filePath)
        }
      })
    },
    async doUpload(filePath) {
      uni.showLoading({ title: '上传中...' })
      try {
        const result = await uploadAttachment(filePath, 'expense', this.takeForm.takeNo)
        this.attachmentUrl = result.url
        uni.showToast({ title: '上传成功', icon: 'success' })
      } catch (err) {
        if (err?.code === 'AUTH_EXPIRED') {
          uni.showToast({ title: '登录已超时', icon: 'none' })
        } else if (err?.code === 'PERMISSION_DENIED') {
          uni.showToast({ title: '暂无上传权限', icon: 'none' })
        } else if (err?.code === 'REQUEST_TIMEOUT') {
          uni.showToast({ title: '上传超时，请重试', icon: 'none' })
        } else {
          uni.showToast({ title: err?.msg || '上传失败', icon: 'none' })
        }
      } finally {
        uni.hideLoading()
      }
    },
    async submitTake() {
      if (this.submitting) return
      if (!this.scanResult) {
        uni.showToast({ title: '请先扫码选择商品', icon: 'none' })
        return
      }
      const actual = parseInt(this.takeForm.actualQuantity)
      if (isNaN(actual) || actual < 0) {
        uni.showToast({ title: '盘点数量不能为负', icon: 'none' })
        return
      }
      const expected = this.scanResult.stockNum ?? 0
      if (actual !== expected && !this.takeForm.reason) {
        uni.showToast({ title: '盘盈盘亏必须填写原因', icon: 'none' })
        return
      }

      this.submitting = true
      // 保存草稿（非敏感字段）
      saveDraft('fieldWork', this.currentDeptId, {
        actualQuantity: this.takeForm.actualQuantity,
        reason: this.takeForm.reason
      }, this.currentUserId())

      try {
        await submitStockTake({
          takeNo: this.takeForm.takeNo,
          deptId: this.currentDeptId,
          productId: this.scanResult.productId,
          actualQuantity: actual,
          expectedQuantity: expected,
          reason: this.takeForm.reason
        })

        // 提交成功后清理草稿
        clearDraft('fieldWork', this.currentDeptId, this.currentUserId())

        // 刷新任务列表和指标
        await refreshAfterTaskAction({
          refreshMetrics: () => {
            const pages = getCurrentPages()
            const home = pages.find((p) => p.route === 'pages/index/index')
            if (home && typeof home.loadDashboard === 'function') {
              home.loadDashboard()
            }
          }
        })

        uni.showToast({ title: '盘点已提交', icon: 'success' })

        // 重置表单，生成新 takeNo 防止幂等冲突
        this.takeForm.actualQuantity = ''
        this.takeForm.reason = ''
        this.takeForm.takeNo = generateTakeNo()
        this.scanResult = null
        this.scanSearched = false
        this.scanCode = ''
      } catch (err) {
        if (isUnknownWriteOutcome(err)) {
          // 超时/网络错误：可能已成功，保留 takeNo 供用户确认
          uni.showModal({
            title: '提交结果未知',
            content: '网络超时，盘点可能已提交。请刷新库存确认，不要重复提交相同单号。',
            showCancel: false
          })
        } else if (err?.code === 403 || err?.msg?.includes('已存在')) {
          // 幂等拒绝：takeNo 已存在，生成新单号
          uni.showModal({
            title: '提交失败',
            content: err.msg || '盘点单号已存在，请重试',
            showCancel: false,
            success: () => {
              this.takeForm.takeNo = generateTakeNo()
            }
          })
        } else if (err?.code === 'AUTH_EXPIRED') {
          // 401 由 authSession 处理，不重复提示
        } else if (err?.code === 'PERMISSION_DENIED') {
          uni.showToast({ title: '暂无盘点权限', icon: 'none' })
        } else {
          uni.showToast({ title: err?.msg || '提交失败', icon: 'none' })
        }
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style>
.page { padding: 24rpx; background: #f5f7fa; min-height: 100vh; }
.hero-card { display: flex; align-items: center; padding: 32rpx; background: #fff; border-radius: 16rpx; margin-bottom: 24rpx; }
.hero-icon { font-size: 48rpx; margin-right: 24rpx; }
.hero-title { font-size: 36rpx; font-weight: 600; color: #1e293b; }
.hero-meta { font-size: 24rpx; color: #64748b; margin-top: 8rpx; }

.section-card { background: #fff; border-radius: 16rpx; padding: 32rpx; margin-bottom: 24rpx; }
.section-header { display: flex; align-items: center; margin-bottom: 24rpx; }
.section-dot { width: 12rpx; height: 12rpx; border-radius: 50%; margin-right: 12rpx; }
.section-dot.required { background: #ef4444; }
.section-title { font-size: 30rpx; font-weight: 600; color: #1e293b; }

.scan-tabs { display: flex; margin-bottom: 24rpx; }
.scan-tab { padding: 12rpx 32rpx; margin-right: 16rpx; background: #f1f5f9; border-radius: 8rpx; font-size: 26rpx; color: #64748b; }
.scan-tab.active { background: #087CF0; color: #fff; }

.scan-row { display: flex; align-items: center; }
.scan-input { flex: 1; height: 72rpx; background: #f8fafc; border: 1rpx solid #e2e8f0; border-radius: 8rpx; padding: 0 20rpx; font-size: 28rpx; }
.scan-btn { margin-left: 16rpx; padding: 0 32rpx; height: 72rpx; line-height: 72rpx; background: #087CF0; color: #fff; border-radius: 8rpx; font-size: 28rpx; }

.scan-result { margin-top: 24rpx; padding: 24rpx; background: #f0f9ff; border-radius: 8rpx; }
.scan-result-row { display: flex; justify-content: space-between; margin-bottom: 12rpx; }
.scan-result-label { font-size: 26rpx; color: #64748b; }
.scan-result-value { font-size: 26rpx; color: #1e293b; font-weight: 500; }
.scan-empty { margin-top: 24rpx; text-align: center; color: #94a3b8; font-size: 26rpx; }

.form-row { display: flex; align-items: center; margin-bottom: 24rpx; }
.form-label { width: 180rpx; font-size: 28rpx; color: #475569; }
.form-value { font-size: 28rpx; color: #1e293b; font-weight: 500; }
.form-input { flex: 1; height: 72rpx; background: #f8fafc; border: 1rpx solid #e2e8f0; border-radius: 8rpx; padding: 0 20rpx; font-size: 28rpx; }

.submit-btn { width: 100%; height: 88rpx; line-height: 88rpx; background: #087CF0; color: #fff; border-radius: 8rpx; font-size: 30rpx; margin-top: 16rpx; }
.submit-btn[disabled] { background: #cbd5e1; }

.upload-row { display: flex; align-items: center; }
.upload-btn { padding: 0 32rpx; height: 72rpx; line-height: 72rpx; background: #087CF0; color: #fff; border-radius: 8rpx; font-size: 28rpx; }
.upload-hint { margin-left: 24rpx; font-size: 26rpx; color: #94a3b8; }
.upload-done { margin-left: 24rpx; font-size: 26rpx; color: #10b981; }
.upload-preview { margin-top: 24rpx; }
.preview-img { width: 100%; height: 300rpx; border-radius: 8rpx; }
</style>
