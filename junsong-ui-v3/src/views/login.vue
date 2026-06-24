<template>
  <div class="login">
    <div class="grid-bg"></div>
    <div class="gradient-bg"></div>

    <div class="login-container">
      <div class="login-decoration">
        <div class="motion-network" aria-hidden="true">
          <span class="line line-a"></span>
          <span class="line line-b"></span>
          <span class="line line-c"></span>
          <span class="node node-a"></span>
          <span class="node node-b"></span>
          <span class="node node-c"></span>
        </div>

        <div class="decoration-content">
          <div class="brand-top">
            <div class="brand-mark" v-html="brandMarkSvg" />
            <div>
              <p class="brand-name"><span class="brand-en">Genesis</span><span class="brand-dot">·</span><span class="brand-cn">峻松</span></p>
              <p class="brand-subtitle">Genesis Cloud · Healthcare Management</p>
            </div>
          </div>

          <div class="brand-main">
            <p class="eyebrow">统一运营工作台</p>
            <h1 class="decoration-title">让会员、财务与门店协同更清晰</h1>
            <p class="decoration-copy">以更干净的视觉层级承载日常业务，登录后直达数据、流程和待办。</p>

            <div class="signal-board">
              <div class="signal-card">
                <b>稳定</b>
                <span>关键业务在线可用</span>
              </div>
              <div class="signal-card">
                <b>协同</b>
                <span>会员服务与财务流程跟进</span>
              </div>
              <div class="signal-card">
                <b>安全</b>
                <span>多机构身份识别与验证码校验</span>
              </div>
            </div>
          </div>

          <div class="brand-foot">
            <div>
              <i></i>
              <span>多机构身份识别</span>
            </div>
            <div>
              <i></i>
              <span>主题色自动适配</span>
            </div>
            <div>
              <i></i>
              <span>验证码安全校验</span>
            </div>
          </div>
        </div>
      </div>

      <div class="login-form-wrapper">
        <div class="login-form">
          <div class="form-header">
            <p class="form-kicker">欢迎回来</p>
            <h2 class="form-title">登录工作台</h2>
          </div>

          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-content" @keyup.enter.prevent="handleLogin">
            <el-form-item prop="username">
              <label class="field-label">用户名</label>
              <div class="input-wrapper">
                <el-input
                  v-model="loginForm.username"
                  type="text"
                  auto-complete="off"
                  placeholder="请输入用户名"
                  class="login-input"
                  @blur="handleUsernameBlur"
                >
                  <template #prefix>
                    <svg-icon icon-class="user" class="input-icon"></svg-icon>
                  </template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item v-if="showDeptSelect" prop="deptId">
              <label class="field-label">所属部门</label>
              <div class="input-wrapper">
                <svg-icon icon-class="tree" class="select-icon"></svg-icon>
                <el-select
                  v-model="loginForm.deptId"
                  placeholder="请选择部门"
                  class="login-input dept-select"
                  popper-class="login-dept-popper"
                >
                  <el-option
                    v-for="dept in userDepts"
                    :key="dept.deptId"
                    :label="dept.deptName"
                    :value="dept.deptId"
                  />
                </el-select>
              </div>
            </el-form-item>
            <el-form-item prop="password">
              <label class="field-label">密码</label>
              <div class="input-wrapper">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  auto-complete="off"
                  placeholder="请输入密码"
                  class="login-input"
                >
                  <template #prefix>
                    <svg-icon icon-class="password" class="input-icon"></svg-icon>
                  </template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item prop="code" v-if="captchaEnabled">
              <label class="field-label">验证码</label>
              <div class="input-wrapper captcha-wrapper">
                <el-input
                  v-model="loginForm.code"
                  auto-complete="off"
                  placeholder="请输入验证码"
                  class="login-input captcha-input"
                >
                  <template #prefix>
                    <svg-icon icon-class="validCode" class="input-icon"></svg-icon>
                  </template>
                </el-input>
                <div class="login-code" @click="getCode">
                  <img v-if="codeUrl" :src="codeUrl" class="login-code-img" title="点击刷新">
                  <span v-else class="login-code-error">
                    {{ codeLoading ? '加载中...' : '验证码加载失败，点击重试' }}
                  </span>
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <div class="form-options">
                <el-checkbox v-model="loginForm.rememberMe" class="remember-checkbox" v-if="!preventSavePassword">
                  <span class="checkbox-label">记住密码</span>
                </el-checkbox>
                <div v-if="register">
                  <router-link class="register-link" :to="'/register'">立即注册</router-link>
                </div>
              </div>
            </el-form-item>
            <el-form-item class="login-button-item">
              <el-button
                :loading="loading"
                :disabled="loading"
                size="large"
                type="primary"
                class="login-button"
                @click="handleLogin"
              >
                <span v-if="!loading">
                  <span class="button-text">进入系统</span>
                </span>
                <span v-else>
                  <span class="loading-text">登录中</span>
                  <span class="loading-dots">
                    <span></span><span></span><span></span>
                  </span>
                </span>
              </el-button>
            </el-form-item>
          </el-form>

          <div class="login-assist">
            <div>
              <b>机构选择</b>
              <span>用户名识别多机构后展示部门选择。</span>
            </div>
            <div>
              <b>主题适配</b>
              <span>登录背景、按钮、验证码跟随当前主题。</span>
            </div>
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
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { getCodeImg as getCodeImgApi, getUserDepts as getUserDeptsApi } from '@/api/login'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import brandMarkSvg from '@/assets/logo/junsong-mark-theme.svg?raw'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const settingsStore = useSettingsStore()

const title = computed(() => import.meta.env.VITE_APP_TITLE)
const footerContent = computed(() => `© 2026 Genesis · 峻松管理系统. All Rights Reserved.`)
const codeUrl = ref('')
const codeLoading = ref(false)
const loading = ref(false)
const captchaEnabled = ref(true)
const register = ref(true)
const userDepts = ref<any[]>([])
const showDeptSelect = ref(false)
const preventSavePassword = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false,
  code: '',
  uuid: '',
  deptId: null as number | null,
})

const loginRules = computed(() => ({
  username: [{ required: true, message: '请输入您的账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入您的密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
}))

const loginFormRef = ref()

function getCode() {
  codeLoading.value = true
  getCodeImgApi()
    .then((res: any) => {
      captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
      if (captchaEnabled.value) {
        codeUrl.value = 'data:image/gif;base64,' + res.img
        loginForm.uuid = res.uuid
      } else {
        codeUrl.value = ''
        loginForm.uuid = ''
      }
      if (res.preventSavePassword) {
        preventSavePassword.value = true
        Cookies.remove('username')
        Cookies.remove('password')
        Cookies.remove('rememberMe')
        loginForm.password = ''
        loginForm.rememberMe = false
      }
    })
    .catch(() => {
      codeUrl.value = ''
      loginForm.uuid = ''
    })
    .finally(() => {
      codeLoading.value = false
    })
}

function getCookie() {
  const username = Cookies.get('username')
  const password = Cookies.get('password')
  const rememberMe = Cookies.get('rememberMe')
  if (username !== undefined) loginForm.username = username
  if (password !== undefined) loginForm.password = decrypt(password)
  loginForm.rememberMe = rememberMe === undefined ? false : Boolean(rememberMe)
}

function handleUsernameBlur() {
  if (loginForm.username && loginForm.username.trim()) {
    getUserDeptsApi(loginForm.username)
      .then((res: any) => {
        if (res.code === 200 && res.data && res.data.length > 0) {
          userDepts.value = res.data
          if (res.data.length > 1) {
            showDeptSelect.value = true
            loginForm.deptId = null
          } else {
            showDeptSelect.value = false
            loginForm.deptId = res.data[0].deptId
          }
        } else {
          showDeptSelect.value = false
          userDepts.value = []
          loginForm.deptId = null
        }
      })
      .catch(() => {
        showDeptSelect.value = false
        userDepts.value = []
        loginForm.deptId = null
      })
  }
}

function handleLogin() {
  if (loading.value) return
  if (!loginFormRef.value) return
  ;(loginFormRef.value as any).validate((valid: boolean) => {
    if (valid) {
      if (captchaEnabled.value && !loginForm.uuid) {
        getCode()
        return
      }
      loading.value = true
      if (loginForm.rememberMe && !preventSavePassword.value) {
        Cookies.set('username', loginForm.username, { expires: 30 })
        Cookies.set('password', encrypt(loginForm.password), { expires: 30 })
        Cookies.set('rememberMe', String(loginForm.rememberMe), { expires: 30 })
      } else {
        Cookies.remove('username')
        Cookies.remove('password')
        Cookies.remove('rememberMe')
      }
      userStore
        .login({
          username: loginForm.username,
          password: loginForm.password,
          code: loginForm.code,
          uuid: loginForm.uuid,
          deptId: loginForm.deptId,
        })
        .then(async () => {
          try {
            await router.push({ path: route.query.redirect ? route.query.redirect as string : '/' })
          } finally {
            loading.value = false
          }
        })
        .catch(() => {
          loading.value = false
          if (captchaEnabled.value) {
            getCode()
          }
        })
    }
  })
}

onMounted(() => {
  getCode()
  getCookie()
})
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.login {
  --login-ink: #172033;
  --login-muted: #667085;
  --login-line: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.14);
  --login-surface: rgba(255, 255, 255, 0.86);
  --login-surface-solid: #ffffff;
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
  animation: grid-drift 18s linear infinite;
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
  animation: aurora-drift 16s ease-in-out infinite alternate;
}

.login-container {
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
  animation: shell-rise 0.78s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

.login-decoration {
  position: relative;
  display: flex;
  min-width: 0;
  overflow: hidden;
  color: #fff;
  background:
    linear-gradient(145deg, color-mix(in srgb, var(--theme-primary-dark, #1e4d56) 92%, #10172a 8%), color-mix(in srgb, var(--theme-primary, #2c6975) 82%, var(--theme-login-panel-mid, #68b2a0) 18%) 48%, color-mix(in srgb, var(--theme-primary-light, #68b2a0) 70%, var(--theme-login-panel-end, #cde0c9) 30%)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), transparent);
}

.login-decoration::before {
  position: absolute;
  inset: 34px 18px auto auto;
  width: 360px;
  height: 360px;
  content: "";
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.18), transparent 60%);
  animation: slow-orbit 14s ease-in-out infinite alternate;
}

.login-decoration::after {
  position: absolute;
  right: -80px;
  bottom: -120px;
  width: 420px;
  height: 420px;
  content: "JS";
  color: rgba(255, 255, 255, 0.1);
  font-size: 168px;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 420px;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 50%;
  animation: monogram-float 10s ease-in-out infinite;
}

.motion-network {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.72;
}

.motion-network .line {
  position: absolute;
  height: 1px;
  overflow: hidden;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.24), transparent);
  transform-origin: left center;
}

.motion-network .line::after {
  position: absolute;
  inset: 0 auto 0 -30%;
  width: 30%;
  content: "";
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.9), transparent);
  animation: line-run 4.8s ease-in-out infinite;
}

.line-a {
  top: 24%;
  left: 48%;
  width: 34%;
  transform: rotate(21deg);
}

.line-b {
  top: 58%;
  left: 42%;
  width: 42%;
  transform: rotate(-16deg);
}

.line-c {
  top: 72%;
  left: 12%;
  width: 36%;
  transform: rotate(8deg);
}

.motion-network .node {
  position: absolute;
  width: 9px;
  height: 9px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.35);
  box-shadow: 0 0 20px rgba(255, 255, 255, 0.5);
  animation: node-float 5.4s ease-in-out infinite;
}

.node-a {
  top: 22%;
  left: 74%;
}

.node-b {
  top: 56%;
  left: 39%;
  animation-delay: 0.8s;
}

.node-c {
  top: 71%;
  left: 48%;
  animation-delay: 1.3s;
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

.logo-icon {
  width: 28px;
  height: 28px;
  color: #fff;
}

.brand-name {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: 12px;
  font-weight: 600;
  letter-spacing: 0;
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
  animation: pulse-dot 2.4s ease-in-out infinite;
}

.decoration-title {
  max-width: 560px;
  margin: 0;
  color: #fff;
  font-size: clamp(40px, 5vw, 64px);
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.03;
}

.decoration-copy {
  max-width: 460px;
  margin: 22px 0 0;
  color: rgba(255, 255, 255, 0.74);
  font-size: 16px;
  line-height: 1.8;
}

.signal-board {
  display: grid;
  width: min(480px, 100%);
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 34px;
}

.signal-card {
  position: relative;
  min-height: 104px;
  padding: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.18);
  animation: card-in 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

.signal-card:nth-child(2) {
  animation-delay: 0.08s;
}

.signal-card:nth-child(3) {
  animation-delay: 0.16s;
}

.signal-card::after {
  position: absolute;
  inset: 0 auto 0 -80%;
  width: 55%;
  content: "";
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.18), transparent);
  transform: skewX(-18deg);
  animation: scan-card 5.8s ease-in-out infinite;
}

.signal-card b {
  display: block;
  margin-bottom: 10px;
  font-size: 24px;
  letter-spacing: 0;
}

.signal-card span {
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;
  line-height: 1.55;
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

.brand-foot i {
  flex: 0 0 auto;
}

.login-form-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 46px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0.78)),
    radial-gradient(circle at 20% 8%, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.1), transparent 34%);
}

.login-form {
  width: 100%;
  max-width: 420px;
  margin: 0 auto;
  animation: card-in 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) 0.12s both;
}

.form-header {
  margin-bottom: 28px;
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
  letter-spacing: 0;
}

.login-content {
  width: 100%;
  margin: 0;
}

.login-content :deep(.el-form-item) {
  display: block;
  width: 100%;
  margin-bottom: 16px;
}

.login-content :deep(.el-form-item__content) {
  display: block;
  width: 100%;
  margin-left: 0 !important;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  color: #344055;
  font-size: 13px;
  font-weight: 650;
  line-height: 1.2;
}

.input-wrapper {
  position: relative;
  width: 100%;
}

.login-input {
  width: 100% !important;
}

.login-input :deep(.el-input__wrapper),
.dept-select :deep(.el-select__wrapper) {
  width: 100%;
  min-height: 50px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 0 0 1px var(--login-line) inset, 0 10px 24px rgba(33, 45, 77, 0.06);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.login-input :deep(.el-input__wrapper:hover),
.dept-select :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.32) inset, 0 10px 24px rgba(33, 45, 77, 0.06);
}

.login-input :deep(.el-input__wrapper.is-focus),
.dept-select :deep(.el-select__wrapper.is-focused) {
  transform: translateY(-1px);
  box-shadow: 0 0 0 1px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.52) inset, 0 0 0 3px var(--theme-focus-shadow, rgba(44, 105, 117, 0.15)), 0 12px 28px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.14);
}

.login-input :deep(.el-input__inner),
.dept-select :deep(.el-select__selected-item),
.dept-select :deep(.el-select__placeholder) {
  color: var(--login-ink);
  font-size: 14px;
}

.login-input :deep(.el-input__inner::placeholder),
.dept-select :deep(.el-select__placeholder.is-transparent) {
  color: #7b8495;
}

.login-input :deep(.el-input__prefix) {
  left: 16px;
  color: var(--theme-primary, #2c6975);
}

.login-input :deep(.input-icon),
.select-icon {
  width: 16px;
  height: 16px;
  color: var(--theme-primary, #2c6975);
}

.select-icon {
  position: absolute;
  top: 17px;
  left: 16px;
  z-index: 2;
  pointer-events: none;
}

.dept-select :deep(.el-select__wrapper) {
  padding-left: 44px;
}

.dept-select :deep(.el-select__caret) {
  color: var(--theme-primary, #2c6975);
}

.captcha-wrapper {
  display: grid;
  grid-template-columns: 1fr 116px;
  gap: 12px;
}

.login-code {
  position: relative;
  display: grid;
  min-height: 50px;
  overflow: hidden;
  place-items: center;
  border: 1px solid rgba(var(--theme-primary-rgb, 44, 105, 117), 0.2);
  border-radius: 14px;
  background:
    repeating-linear-gradient(-18deg, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.1) 0 8px, color-mix(in srgb, var(--theme-primary-light, #68b2a0) 14%, transparent) 8px 16px),
    #f7f9ff;
}

.login-code::after {
  position: absolute;
  inset: 0 auto 0 -58%;
  width: 48%;
  content: "";
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.78), transparent);
  transform: skewX(-18deg);
  animation: captcha-shine 4.2s ease-in-out infinite;
}

.login-code-img {
  position: relative;
  z-index: 1;
  width: 116px;
  height: 50px;
  cursor: pointer;
  object-fit: cover;
  mix-blend-mode: multiply;
  opacity: 0.9;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.login-code-img:hover {
  opacity: 1;
  transform: scale(1.03);
}

.login-code-error {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 116px;
  min-height: 50px;
  padding: 0 8px;
  color: var(--el-color-danger);
  cursor: pointer;
  font-size: 12px;
  line-height: 1.25;
  text-align: center;
}

.form-options {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 22px;
  color: var(--login-muted);
}

.remember-checkbox :deep(.el-checkbox__label) {
  color: var(--login-muted);
  font-size: 13px;
}

.remember-checkbox :deep(.el-checkbox__inner) {
  border-color: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.32);
  border-radius: 5px;
}

.remember-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: var(--theme-primary, #2c6975);
  border-color: var(--theme-primary, #2c6975);
}

.remember-checkbox :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: var(--theme-primary, #2c6975);
}

.register-link {
  color: var(--theme-primary, #2c6975);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

.register-link:hover {
  color: var(--theme-primary-dark, #1e4d56);
}

.login-button-item {
  width: 100%;
  margin-top: 6px;
  margin-bottom: 0 !important;
}

.login-button-item :deep(.el-form-item__content) {
  width: 100%;
  margin-left: 0 !important;
}

.login-button {
  position: relative;
  display: flex;
  width: 100% !important;
  min-height: 52px;
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
  letter-spacing: 0;
  transition: box-shadow 0.2s ease, transform 0.2s ease, filter 0.2s ease;
}

.login-button::after {
  position: absolute;
  inset: 0 auto 0 -40%;
  width: 32%;
  content: "";
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.36), transparent);
  transform: skewX(-16deg);
  animation: button-shine 5s ease-in-out infinite;
}

.login-button:hover {
  filter: saturate(1.08);
  transform: translateY(-1px);
  box-shadow: 0 22px 38px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.34);
}

.login-button:active {
  transform: translateY(0) scale(0.99);
}

.login-button :deep(.el-button__text),
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

.login-assist {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 22px;
}

.login-assist div {
  padding: 13px 14px;
  border: 1px solid rgba(28, 39, 63, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.64);
  color: var(--login-muted);
  font-size: 12px;
  line-height: 1.55;
}

.login-assist b {
  display: block;
  margin-bottom: 3px;
  color: var(--login-ink);
  font-size: 13px;
}

.el-login-footer {
  position: relative;
  z-index: 2;
  width: min(1120px, 100%);
  color: rgba(23, 32, 51, 0.5);
  font-size: 12px;
  line-height: 1.4;
  text-align: center;
}

.footer-text {
  text-shadow: 0 1px 10px rgba(255, 255, 255, 0.52);
}

:global(.login-dept-popper) {
  overflow: hidden;
  border: 1px solid rgba(var(--theme-primary-rgb, 44, 105, 117), 0.22) !important;
  border-radius: 14px !important;
  box-shadow: 0 14px 36px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.18), 0 10px 28px rgba(0, 0, 0, 0.12) !important;
}

:global(.login-dept-popper .el-select-dropdown__wrap) {
  max-height: 236px;
}

:global(.login-dept-popper .el-select-dropdown__item) {
  height: 36px;
  margin: 4px 8px;
  border-radius: 8px;
  color: var(--login-ink);
  line-height: 36px;
  transition: color 0.18s ease, background 0.18s ease;
}

:global(.login-dept-popper .el-select-dropdown__item.hover),
:global(.login-dept-popper .el-select-dropdown__item:hover) {
  color: var(--theme-primary, #2c6975);
  background: var(--theme-hover-bg, rgba(44, 105, 117, 0.1));
}

:global(.login-dept-popper .el-select-dropdown__item.selected) {
  color: #fff;
  background: linear-gradient(135deg, var(--theme-primary-light, #68b2a0), var(--theme-primary, #2c6975));
  font-weight: 600;
}

:global(.login-dept-popper .el-popper__arrow::before) {
  border-color: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.22) !important;
  background: #fff !important;
}

@keyframes shell-rise {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes grid-drift {
  from {
    background-position: 0 0, 0 0;
  }
  to {
    background-position: 42px 84px, 84px 42px;
  }
}

@keyframes aurora-drift {
  from {
    transform: translate3d(-16px, -8px, 0) scale(1);
  }
  to {
    transform: translate3d(18px, 12px, 0) scale(1.04);
  }
}

@keyframes slow-orbit {
  from {
    transform: translate3d(0, 0, 0) rotate(0deg);
  }
  to {
    transform: translate3d(-18px, 12px, 0) rotate(10deg);
  }
}

@keyframes monogram-float {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(-12px, -16px, 0);
  }
}

@keyframes mark-glow {
  0%,
  100% {
    box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.24), 0 0 0 rgba(255, 255, 255, 0);
  }
  50% {
    box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.34), 0 0 28px rgba(255, 255, 255, 0.24);
  }
}

@keyframes pulse-dot {
  0%,
  100% {
    opacity: 0.62;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.28);
  }
}

@keyframes line-run {
  0%,
  24% {
    transform: translateX(0);
  }
  70%,
  100% {
    transform: translateX(430%);
  }
}

@keyframes node-float {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(10px, -8px, 0) scale(1.18);
  }
}

@keyframes scan-card {
  0%,
  34% {
    transform: translateX(0) skewX(-18deg);
  }
  72%,
  100% {
    transform: translateX(330%) skewX(-18deg);
  }
}

@keyframes captcha-shine {
  0%,
  42% {
    transform: translateX(0) skewX(-18deg);
  }
  72%,
  100% {
    transform: translateX(360%) skewX(-18deg);
  }
}

@keyframes button-shine {
  0%,
  48% {
    transform: translateX(0) skewX(-16deg);
  }
  72%,
  100% {
    transform: translateX(460%) skewX(-16deg);
  }
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
  .login {
    position: relative;
    min-height: 100dvh;
    padding: 20px 20px 16px;
  }

  .login-container {
    grid-template-columns: 1fr;
    min-height: auto;
    max-height: none;
  }

  .login-decoration {
    min-height: 420px;
  }

  .login-form-wrapper {
    padding: 34px 22px 44px;
  }

  .brand-foot {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .login {
    padding: 12px;
  }

  .login-container {
    border-radius: 22px;
  }

  .decoration-content {
    padding: 28px;
  }

  .signal-board,
  .login-assist,
  .captcha-wrapper {
    grid-template-columns: 1fr;
  }

  .login-code,
  .login-code-img {
    width: 100%;
  }

  .el-login-footer {
    width: 100%;
  }
}

@media (max-height: 760px) and (min-width: 961px) {
  .login {
    padding-top: 18px;
    row-gap: 10px;
  }

  .login-container {
    min-height: 0;
  }

  .decoration-content {
    padding: 32px;
  }

  .brand-main {
    padding: 42px 0 30px;
  }

  .decoration-title {
    font-size: clamp(34px, 4vw, 48px);
  }

  .signal-board {
    margin-top: 24px;
  }

  .signal-card {
    min-height: 88px;
    padding: 13px;
  }

  .login-form-wrapper {
    padding: 30px 38px;
  }

  .form-header {
    margin-bottom: 20px;
  }

  .login-content :deep(.el-form-item) {
    margin-bottom: 12px;
  }

  .login-input :deep(.el-input__wrapper),
  .dept-select :deep(.el-select__wrapper),
  .login-code,
  .login-code-img {
    min-height: 46px;
    height: 46px;
  }

  .login-assist {
    margin-top: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation: none !important;
    transition: none !important;
  }
}
</style>
