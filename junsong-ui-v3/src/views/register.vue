<template>
  <div class="register">
    <div class="grid-bg"></div>
    <div class="gradient-bg"></div>

    <div class="register-container">
      <div class="register-decoration">
        <div class="decoration-content">
          <div class="brand-top">
            <div class="brand-mark" v-html="brandMarkSvg" />
            <div>
              <p class="brand-name"><span class="brand-en">Genesis</span><span class="brand-dot">·</span><span class="brand-cn">峻松</span></p>
              <p class="brand-subtitle">Genesis Cloud · Healthcare Management</p>
            </div>
          </div>

          <div class="brand-main">
            <p class="eyebrow">新用户注册</p>
            <h1 class="decoration-title">加入工作台</h1>
            <p class="decoration-copy">填写以下信息完成注册，注册后需管理员审核分配部门即可登录系统。</p>
          </div>

          <div class="brand-foot">
            <div>
              <i></i>
              <span>邀请码验证</span>
            </div>
            <div>
              <i></i>
              <span>管理员审核</span>
            </div>
            <div>
              <i></i>
              <span>手机号登录</span>
            </div>
          </div>
        </div>
      </div>

      <div class="register-form-wrapper">
        <div class="register-form">
          <div class="form-header">
            <p class="form-kicker">欢迎加入</p>
            <h2 class="form-title">注册账号</h2>
          </div>

          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" class="register-content" @keyup.enter.prevent="handleRegister">
            <el-form-item prop="username">
              <label class="field-label">用户名</label>
              <div class="input-wrapper">
                <el-input
                  v-model="registerForm.username"
                  type="text"
                  auto-complete="off"
                  placeholder="请输入用户名（2-20个字符）"
                  class="register-input"
                >
                  <template #prefix>
                    <svg-icon icon-class="user" class="input-icon"></svg-icon>
                  </template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item prop="nickName">
              <label class="field-label">姓名</label>
              <div class="input-wrapper">
                <el-input
                  v-model="registerForm.nickName"
                  type="text"
                  auto-complete="off"
                  placeholder="请输入真实姓名"
                  class="register-input"
                >
                  <template #prefix>
                    <svg-icon icon-class="user" class="input-icon"></svg-icon>
                  </template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item prop="phonenumber">
              <label class="field-label">手机号码</label>
              <div class="input-wrapper">
                <el-input
                  v-model="registerForm.phonenumber"
                  type="text"
                  auto-complete="off"
                  placeholder="请输入手机号码（可用于登录）"
                  class="register-input"
                >
                  <template #prefix>
                    <svg-icon icon-class="phone" class="input-icon"></svg-icon>
                  </template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item prop="idCard">
              <label class="field-label">身份证号 <span v-if="idCardRequired" class="required-mark">*</span></label>
              <div class="input-wrapper">
                <el-input
                  v-model="registerForm.idCard"
                  type="text"
                  auto-complete="off"
                  :placeholder="idCardRequired ? '请输入身份证号' : '请输入身份证号（选填）'"
                  class="register-input"
                >
                  <template #prefix>
                    <svg-icon icon-class="validCode" class="input-icon"></svg-icon>
                  </template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item prop="password">
              <label class="field-label">密码</label>
              <div class="input-wrapper">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  auto-complete="off"
                  placeholder="请输入密码（5-20个字符）"
                  class="register-input"
                >
                  <template #prefix>
                    <svg-icon icon-class="password" class="input-icon"></svg-icon>
                  </template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <label class="field-label">确认密码</label>
              <div class="input-wrapper">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  auto-complete="off"
                  placeholder="请再次输入密码"
                  class="register-input"
                >
                  <template #prefix>
                    <svg-icon icon-class="password" class="input-icon"></svg-icon>
                  </template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item v-if="inviteCodeRequired" prop="inviteCode">
              <label class="field-label">邀请码 <span class="required-mark">*</span></label>
              <div class="input-wrapper">
                <el-input
                  v-model="registerForm.inviteCode"
                  type="text"
                  auto-complete="off"
                  placeholder="请输入邀请码"
                  class="register-input"
                >
                  <template #prefix>
                    <svg-icon icon-class="validCode" class="input-icon"></svg-icon>
                  </template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item class="register-button-item">
              <el-button
                :loading="loading"
                :disabled="loading"
                size="large"
                type="primary"
                class="register-button"
                @click="handleRegister"
              >
                <span v-if="!loading" class="button-text">提交注册</span>
                <span v-else>
                  <span class="loading-text">注册中</span>
                  <span class="loading-dots">
                    <span></span><span></span><span></span>
                  </span>
                </span>
              </el-button>
            </el-form-item>
          </el-form>

          <div class="register-assist">
            <router-link class="login-link" :to="'/login'">已有账号？返回登录</router-link>
          </div>
        </div>
      </div>
    </div>

    <div class="el-login-footer">
      <span class="footer-text">{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { register as registerApi } from '@/api/login'
import { ElMessage } from 'element-plus'
import brandMarkSvg from '@/assets/logo/junsong-mark-theme.svg?raw'

const router = useRouter()

const footerContent = computed(() => `© 2026 Genesis · 峻松管理系统. All Rights Reserved.`)
const loading = ref(false)
const idCardRequired = ref(false)
const inviteCodeRequired = ref(false)

const registerForm = reactive({
  username: '',
  nickName: '',
  phonenumber: '',
  idCard: '',
  password: '',
  confirmPassword: '',
  inviteCode: '',
})

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validatePhone = (rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('手机号不能为空'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}

const registerRules = computed(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度必须在2到20个字符之间', trigger: 'blur' },
  ],
  nickName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phonenumber: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  idCard: idCardRequired.value
    ? [{ required: true, message: '请输入身份证号', trigger: 'blur' }]
    : [],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 5, max: 20, message: '密码长度必须在5到20个字符之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  inviteCode: inviteCodeRequired.value
    ? [{ required: true, message: '请输入邀请码', trigger: 'blur' }]
    : [],
}))

const registerFormRef = ref()

function handleRegister() {
  if (loading.value) return
  if (!registerFormRef.value) return
  ;(registerFormRef.value as any).validate((valid: boolean) => {
    if (valid) {
      loading.value = true
      registerApi({
        username: registerForm.username,
        password: registerForm.password,
        nickName: registerForm.nickName,
        phonenumber: registerForm.phonenumber,
        idCard: registerForm.idCard,
        inviteCode: registerForm.inviteCode,
      })
        .then(() => {
          ElMessage.success('注册成功，请等待管理员审核分配部门')
          router.push('/login')
        })
        .catch(() => {
          loading.value = false
        })
    }
  })
}

onMounted(() => {
  // 默认不显示身份证必填和邀请码必填，后端会校验
  // 如果后端配置了必填，后端会返回错误信息
})
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.register {
  --login-ink: #172033;
  --login-muted: #667085;
  --login-line: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.14);
  --login-surface: rgba(255, 255, 255, 0.86);
  --login-shadow: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.2);
  position: fixed;
  inset: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  row-gap: 14px;
  min-height: 100dvh;
  align-items: center;
  justify-items: center;
  padding: 32px 42px 18px;
  overflow-x: hidden;
  overflow-y: auto;
  color: var(--login-ink);
  background:
    radial-gradient(circle at 12% 14%, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.28), transparent 28%),
    radial-gradient(circle at 84% 18%, color-mix(in srgb, var(--theme-primary-light, #68b2a0) 22%, transparent), transparent 28%),
    linear-gradient(135deg, color-mix(in srgb, var(--theme-login-bg-start, #1a3d45) 14%, #f8fbff 86%) 0%, color-mix(in srgb, var(--theme-login-bg-end, #2c4a4f) 18%, #eef4fb 82%) 48%, #f9fbff 100%);
}

.grid-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(var(--theme-primary-rgb, 44, 105, 117), 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.05) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(180deg, transparent 0%, black 18%, black 78%, transparent 100%);
}

.gradient-bg {
  position: absolute;
  inset: 8% 6%;
  pointer-events: none;
  background:
    radial-gradient(circle at 22% 36%, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.2), transparent 28%),
    radial-gradient(circle at 78% 62%, color-mix(in srgb, var(--theme-primary-light, #68b2a0) 18%, transparent), transparent 30%);
  filter: blur(36px);
  opacity: 0.78;
}

.register-container {
  position: relative;
  z-index: 1;
  display: grid;
  width: min(1120px, 100%);
  min-height: min(680px, calc(100dvh - 108px));
  max-height: calc(100dvh - 86px);
  grid-template-columns: minmax(0, 1.05fr) minmax(400px, 0.72fr);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.68);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.46);
  box-shadow: 0 30px 90px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.2);
  backdrop-filter: blur(24px);
}

.register-decoration {
  position: relative;
  display: flex;
  min-width: 0;
  overflow: hidden;
  color: #fff;
  background:
    linear-gradient(145deg, color-mix(in srgb, var(--theme-primary-dark, #1e4d56) 92%, #10172a 8%), color-mix(in srgb, var(--theme-primary, #2c6975) 82%, var(--theme-login-panel-mid, #68b2a0) 18%) 48%, color-mix(in srgb, var(--theme-primary-light, #68b2a0) 70%, var(--theme-login-panel-end, #cde0c9) 30%)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), transparent);
}

.decoration-content {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  flex-direction: column;
  justify-content: space-between;
  padding: 44px;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  display: grid;
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 12px;
  overflow: hidden;
  color: var(--theme-primary, #0071e3);
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.24);
}

.brand-mark :deep(svg) {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 12px;
}

.brand-name {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: 12px;
  font-weight: 600;
}

.brand-en {
  font-family: 'Cormorant Garamond', 'Source Han Serif SC', serif;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: #ffffff;
}

.brand-dot {
  font-size: 20px;
  line-height: 1;
  color: rgba(255, 255, 255, 0.7);
}

.brand-cn {
  font-family: 'PingFang SC', sans-serif;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  color: #ffffff;
  opacity: 0.92;
}

.brand-subtitle {
  margin: 3px 0 0;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

.brand-main {
  width: min(520px, 100%);
  padding: 68px 0 48px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 18px;
  color: rgba(255, 255, 255, 0.76);
  font-size: 13px;
  font-weight: 600;
}

.eyebrow::before,
.brand-foot i {
  width: 8px;
  height: 8px;
  content: "";
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 0 18px rgba(255, 255, 255, 0.62);
}

.decoration-title {
  max-width: 560px;
  margin: 0;
  color: #fff;
  font-size: clamp(40px, 5vw, 64px);
  font-weight: 800;
  line-height: 1.03;
}

.decoration-copy {
  max-width: 460px;
  margin: 22px 0 0;
  color: rgba(255, 255, 255, 0.74);
  font-size: 16px;
  line-height: 1.8;
}

.brand-foot {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

.brand-foot div {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.register-form-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0.78)),
    radial-gradient(circle at 20% 8%, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.1), transparent 34%);
  overflow-y: auto;
}

.register-form {
  width: 100%;
  max-width: 420px;
  margin: 0 auto;
}

.form-header {
  margin-bottom: 20px;
}

.form-kicker {
  margin: 0 0 8px;
  color: var(--login-muted);
  font-size: 14px;
}

.form-title {
  margin: 0;
  color: var(--login-ink);
  font-size: 30px;
  font-weight: 780;
}

.register-content {
  width: 100%;
  margin: 0;
}

.register-content :deep(.el-form-item) {
  display: block;
  width: 100%;
  margin-bottom: 14px;
}

.register-content :deep(.el-form-item__content) {
  display: block;
  width: 100%;
  margin-left: 0 !important;
}

.field-label {
  display: block;
  margin-bottom: 6px;
  color: #344055;
  font-size: 13px;
  font-weight: 650;
  line-height: 1.2;
}

.required-mark {
  color: #f56c6c;
}

.input-wrapper {
  position: relative;
  width: 100%;
}

.register-input {
  width: 100% !important;
}

.register-input :deep(.el-input__wrapper) {
  width: 100%;
  min-height: 46px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 0 0 1px var(--login-line) inset, 0 10px 24px rgba(33, 45, 77, 0.06);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.register-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.32) inset, 0 10px 24px rgba(33, 45, 77, 0.06);
}

.register-input :deep(.el-input__wrapper.is-focus) {
  transform: translateY(-1px);
  box-shadow: 0 0 0 1px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.52) inset, 0 0 0 3px var(--theme-focus-shadow, rgba(44, 105, 117, 0.15)), 0 12px 28px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.14);
}

.register-input :deep(.el-input__inner) {
  color: var(--login-ink);
  font-size: 14px;
}

.register-input :deep(.el-input__inner::placeholder) {
  color: #7b8495;
}

.register-input :deep(.el-input__prefix) {
  left: 16px;
  color: var(--theme-primary, #2c6975);
}

.register-input :deep(.input-icon) {
  width: 16px;
  height: 16px;
  color: var(--theme-primary, #2c6975);
}

.register-button-item {
  width: 100%;
  margin-top: 6px;
  margin-bottom: 0 !important;
}

.register-button-item :deep(.el-form-item__content) {
  width: 100%;
  margin-left: 0 !important;
}

.register-button {
  position: relative;
  display: flex;
  width: 100% !important;
  min-height: 50px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 0;
  border-radius: 15px;
  color: #fff;
  background: linear-gradient(135deg, var(--theme-primary, #2c6975), var(--theme-primary-light, #68b2a0));
  box-shadow: 0 18px 32px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.28);
  font-size: 15px;
  font-weight: 750;
  transition: box-shadow 0.2s ease, transform 0.2s ease, filter 0.2s ease;
}

.register-button:hover {
  filter: saturate(1.08);
  transform: translateY(-1px);
  box-shadow: 0 22px 38px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.34);
}

.button-text,
.loading-text {
  position: relative;
  z-index: 1;
  color: #fff;
  font-weight: 750;
}

.loading-dots {
  position: relative;
  z-index: 1;
  display: inline-flex;
  gap: 4px;
  margin-left: 8px;
}

.loading-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
  animation: loading-dot 1.4s ease-in-out infinite both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

.register-assist {
  margin-top: 18px;
  text-align: center;
}

.login-link {
  color: var(--theme-primary, #2c6975);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

.login-link:hover {
  color: var(--theme-primary-dark, #1e4d56);
}

.el-login-footer {
  position: relative;
  z-index: 2;
  width: min(1120px, 100%);
  color: rgba(23, 32, 51, 0.5);
  font-size: 12px;
  text-align: center;
}

.footer-text {
  text-shadow: 0 1px 10px rgba(255, 255, 255, 0.52);
}

@keyframes loading-dot {
  0%,
  80%,
  100% {
    opacity: 0.5;
    transform: scale(0);
  }
  40% {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 960px) {
  .register {
    position: relative;
    min-height: 100dvh;
    padding: 20px 20px 16px;
  }

  .register-container {
    grid-template-columns: 1fr;
    min-height: auto;
    max-height: none;
  }

  .register-decoration {
    min-height: 280px;
  }

  .register-form-wrapper {
    padding: 34px 22px 44px;
  }

  .brand-foot {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .register {
    padding: 12px;
  }

  .register-container {
    border-radius: 22px;
  }

  .decoration-content {
    padding: 28px;
  }

  .el-login-footer {
    width: 100%;
  }
}
</style>
