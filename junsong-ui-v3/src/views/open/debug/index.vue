<template>
  <main class="debug-page">
    <header class="debug-nav">
      <OpenBrand />
      <nav aria-label="签名调试导航">
        <RouterLink to="/open-platform">平台首页</RouterLink>
        <RouterLink to="/open-platform/docs">开发文档</RouterLink>
        <RouterLink to="/open-platform/samples">接入样例</RouterLink>
        <RouterLink to="/open-platform/apply">申请接入</RouterLink>
        <RouterLink to="/login?redirect=/open/app">控制台</RouterLink>
      </nav>
    </header>

    <section class="debug-hero">
      <div>
        <p class="kicker">SIGNATURE LAB</p>
        <h1>开放 API 签名调试</h1>
        <p>在浏览器本地生成 HMAC-SHA256 签名，核对签名串、请求头和 curl 示例。AppSecret 不会提交到服务端。</p>
      </div>
      <aside class="trust-panel">
        <span>本地计算</span>
        <strong>不上传密钥</strong>
        <p>调试页只负责生成签名材料。真实调用仍经过网关的 AppKey、Nonce、时间戳和额度校验。</p>
      </aside>
    </section>

    <section class="debug-layout">
      <form class="input-panel" @submit.prevent="generateSignature">
        <div class="panel-heading">
          <span>01</span>
          <h2>请求材料</h2>
        </div>

        <div class="field-grid">
          <label>
            <span>AppKey</span>
            <input v-model="form.appKey" autocomplete="off" placeholder="从控制台复制 AppKey" />
          </label>
          <label>
            <span>AppSecret</span>
            <input v-model="form.appSecret" autocomplete="off" type="password" placeholder="仅在本地参与计算" />
          </label>
          <label>
            <span>Method</span>
            <select v-model="form.method">
              <option v-for="method in methods" :key="method" :value="method">{{ method }}</option>
            </select>
          </label>
          <label>
            <span>Path</span>
            <input v-model="form.path" placeholder="/members" />
          </label>
          <label>
            <span>Timestamp</span>
            <div class="inline-input">
              <input v-model="form.timestamp" />
              <button type="button" @click="refreshTimestamp">刷新</button>
            </div>
          </label>
          <label>
            <span>Nonce</span>
            <div class="inline-input">
              <input v-model="form.nonce" />
              <button type="button" @click="refreshNonce">生成</button>
            </div>
          </label>
          <label class="wide-field">
            <span>Body</span>
            <textarea v-model="form.body" placeholder="GET 请求通常留空，POST 请求填原始 JSON 字符串。" />
          </label>
        </div>

        <div class="action-row">
          <button class="primary-action" type="submit">
            生成签名
            <el-icon><ArrowRight /></el-icon>
          </button>
          <button class="secondary-action" type="button" @click="loadPostSample">载入 POST 示例</button>
          <button class="secondary-action" type="button" @click="resetSample">恢复默认</button>
        </div>
      </form>

      <aside class="result-panel">
        <div class="panel-heading">
          <span>02</span>
          <h2>生成结果</h2>
        </div>
        <div class="result-stack">
          <article>
            <div class="result-title">
              <h3>签名串</h3>
              <button type="button" @click="copyText(signString)">复制</button>
            </div>
            <pre>{{ signString }}</pre>
          </article>
          <article>
            <div class="result-title">
              <h3>请求头</h3>
              <button type="button" @click="copyText(headersPreview)">复制</button>
            </div>
            <pre>{{ headersPreview }}</pre>
          </article>
          <article>
            <div class="result-title">
              <h3>curl 示例</h3>
              <button type="button" @click="copyText(curlPreview)">复制</button>
            </div>
            <pre>{{ curlPreview }}</pre>
          </article>
        </div>
      </aside>
    </section>

    <section class="explain-section">
      <article v-for="item in explainCards" :key="item.title">
        <el-icon><component :is="item.icon" /></el-icon>
        <h3>{{ item.title }}</h3>
        <p>{{ item.desc }}</p>
      </article>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Clock, Key, Lock, SetUp } from '@element-plus/icons-vue'
import OpenBrand from '../components/OpenBrand.vue'

const methods = ['GET', 'POST', 'PUT', 'DELETE']

const form = reactive({
  appKey: 'demo_app_key',
  appSecret: 'demo_app_secret',
  method: 'GET',
  path: '/members',
  timestamp: '',
  nonce: '',
  body: '',
})

const signature = ref('')

const normalizedPath = computed(() => {
  const trimmed = form.path.trim()
  if (!trimmed) return '/'
  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
})

const signString = computed(() => `${form.method}${normalizedPath.value}${form.timestamp}${form.nonce}${form.body}`)

const headersPreview = computed(() => [
  `X-App-Key: ${form.appKey}`,
  `X-App-Timestamp: ${form.timestamp}`,
  `X-App-Nonce: ${form.nonce}`,
  `X-App-Signature: ${signature.value || '点击生成签名'}`,
].join('\n'))

const curlPreview = computed(() => {
  const bodyPart = form.body.trim() ? ` \\\n  -H 'Content-Type: application/json' \\\n  -d '${escapeSingleQuotes(form.body)}'` : ''
  return [
    `curl -X ${form.method} 'http://127.0.0.1/prod-api/openapi/v1${normalizedPath.value}' \\`,
    `  -H 'X-App-Key: ${form.appKey}' \\`,
    `  -H 'X-App-Timestamp: ${form.timestamp}' \\`,
    `  -H 'X-App-Nonce: ${form.nonce}' \\`,
    `  -H 'X-App-Signature: ${signature.value || '点击生成签名'}'${bodyPart}`,
  ].join('\n')
})

const explainCards = [
  {
    title: '时间戳窗口',
    desc: '服务端允许约 5 分钟时间差，联调机器需要保持时间同步。',
    icon: Clock,
  },
  {
    title: 'Nonce 防重放',
    desc: '同一个 AppKey 下 10 分钟内不要重复使用同一个 Nonce。',
    icon: Lock,
  },
  {
    title: 'Body 原样参与签名',
    desc: 'POST 和 PUT 请求要使用实际发送的原始 body 字符串。',
    icon: SetUp,
  },
  {
    title: 'Key 分环境',
    desc: '测试 Key 只用于联调，生产 Key 需要审批后启用。',
    icon: Key,
  },
]

function refreshTimestamp() {
  form.timestamp = String(Date.now())
}

function refreshNonce() {
  form.nonce = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 10)}`
}

function resetSample() {
  form.method = 'GET'
  form.path = '/members'
  form.body = ''
  refreshTimestamp()
  refreshNonce()
  generateSignature()
}

function loadPostSample() {
  form.method = 'POST'
  form.path = '/store-openings'
  form.body = JSON.stringify({ storeName: '测试门店', contactName: '张三', contactPhone: '13800000000' })
  refreshTimestamp()
  refreshNonce()
  generateSignature()
}

async function generateSignature() {
  signature.value = await hmacSha256(form.appSecret, signString.value)
}

async function hmacSha256(secret: string, message: string) {
  const encoder = new TextEncoder()
  const key = await crypto.subtle.importKey('raw', encoder.encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign'])
  const buffer = await crypto.subtle.sign('HMAC', key, encoder.encode(message))
  return Array.from(new Uint8Array(buffer)).map((byte) => byte.toString(16).padStart(2, '0')).join('')
}

function escapeSingleQuotes(value: string) {
  return value.replace(/'/g, "'\\''")
}

async function copyText(value: string) {
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('浏览器未允许复制，请手动选择文本')
  }
}

onMounted(() => {
  refreshTimestamp()
  refreshNonce()
  generateSignature()
})
</script>

<style scoped lang="scss">
.debug-page {
  --surface: #f8fbff;
  --surface-elevated: #ffffff;
  --ink: #172033;
  --ink-soft: #667085;
  --accent: var(--theme-primary, #2c6975);
  --accent-2: var(--theme-primary-dark, #1e4d56);
  --line: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.14);
  --shadow: 0 26px 80px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.14);

  position: relative;
  isolation: isolate;
  min-height: 100dvh;
  overflow-x: hidden;
  color: var(--ink);
  background:
    radial-gradient(circle at 12% 8%, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.16), transparent 30%),
    radial-gradient(circle at 84% 18%, color-mix(in srgb, var(--theme-primary-light, #68b2a0) 16%, transparent), transparent 28%),
    linear-gradient(135deg, color-mix(in srgb, var(--theme-login-bg-start, #1a3d45) 10%, #f8fbff 90%) 0%, #eef4fb 52%, #f9fbff 100%);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.debug-page::before {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  content: "";
  background-image:
    linear-gradient(rgba(var(--theme-primary-rgb, 44, 105, 117), 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.05) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(180deg, transparent 0%, black 16%, black 82%, transparent 100%);
}

.debug-page > * {
  position: relative;
  z-index: 1;
}

.debug-nav {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 68px;
  padding: 0 clamp(20px, 5vw, 76px);
  border-bottom: 1px solid var(--line);
  background: rgba(248, 251, 255, 0.88);
  backdrop-filter: blur(16px);

  nav {
    display: flex;
    flex-wrap: wrap;
    gap: 22px;
  }

  a {
    color: var(--ink-soft);
    text-decoration: none;
    white-space: nowrap;
  }
}

.debug-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 420px);
  gap: clamp(28px, 5vw, 68px);
  align-items: end;
  padding: 86px clamp(20px, 5vw, 76px) 58px;

  h1 {
    max-width: 820px;
    margin: 12px 0 18px;
    font-size: clamp(44px, 6vw, 76px);
    line-height: 0.98;
    letter-spacing: 0;
  }

  p {
    max-width: 720px;
    margin: 0;
    color: var(--ink-soft);
    font-size: 18px;
    line-height: 1.76;
  }
}

.kicker {
  margin: 0;
  color: var(--accent-2) !important;
  font-size: 13px !important;
  font-weight: 850;
  letter-spacing: 0.1em;
}

.trust-panel,
.input-panel,
.result-panel,
.explain-section article {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: var(--shadow);
}

.trust-panel {
  padding: 28px;

  span {
    color: var(--accent-2);
    font-size: 13px;
    font-weight: 850;
  }

  strong {
    display: block;
    margin: 10px 0 12px;
    font-size: 32px;
    line-height: 1.08;
  }
}

.debug-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(360px, 1.05fr);
  gap: 24px;
  padding: 0 clamp(20px, 5vw, 76px) 42px;
}

.input-panel,
.result-panel {
  padding: 28px;
}

.panel-heading {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;

  span {
    display: grid;
    width: 36px;
    height: 36px;
    place-items: center;
    color: #ffffff;
    border-radius: 50%;
    background: var(--accent);
    font-size: 13px;
    font-weight: 850;
  }

  h2 {
    margin: 0;
    font-size: 26px;
  }
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;

  label {
    display: grid;
    gap: 8px;
  }

  span {
    color: var(--ink-soft);
    font-size: 14px;
    font-weight: 760;
  }

  input,
  select,
  textarea {
    width: 100%;
    min-height: 44px;
    padding: 0 13px;
    color: var(--ink);
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #ffffff;
    outline: none;
  }

  textarea {
    min-height: 128px;
    padding-top: 12px;
    resize: vertical;
    line-height: 1.7;
  }
}

.wide-field {
  grid-column: 1 / -1;
}

.inline-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 72px;
  gap: 8px;

  button {
    color: var(--accent);
    border: 1px solid rgba(15, 107, 86, 0.28);
    border-radius: 8px;
    background: rgba(15, 107, 86, 0.08);
    font-weight: 760;
  }
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  font-weight: 780;
  cursor: pointer;
}

.primary-action {
  color: #ffffff;
  border: 0;
  background: var(--accent);
}

.secondary-action {
  color: var(--ink);
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.68);
}

.result-stack {
  display: grid;
  gap: 16px;
}

.result-stack article {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #111a17;
}

.result-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);

  h3 {
    margin: 0;
    color: #ffffff;
    font-size: 15px;
  }

  button {
    color: #d9fff4;
    border: 1px solid rgba(217, 255, 244, 0.24);
    border-radius: 999px;
    background: transparent;
    font-size: 12px;
    font-weight: 760;
    cursor: pointer;
  }
}

pre {
  min-height: 72px;
  max-height: 260px;
  margin: 0;
  padding: 16px;
  overflow: auto;
  color: #eafff8;
  font-size: 13px;
  line-height: 1.72;
  white-space: pre-wrap;
  word-break: break-all;
}

.explain-section {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  padding: 0 clamp(20px, 5vw, 76px) 76px;

  article {
    min-height: 210px;
    padding: 24px;
    box-shadow: none;
  }

  .el-icon {
    color: var(--accent);
    font-size: 30px;
  }

  h3 {
    margin: 24px 0 10px;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.72;
  }
}

@media (max-width: 1080px) {
  .debug-hero,
  .debug-layout,
  .explain-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .debug-nav {
    align-items: flex-start;
    flex-direction: column;
    padding-top: 16px;
    padding-bottom: 16px;
  }

  .debug-hero {
    padding-top: 54px;

    h1 {
      font-size: 42px;
    }
  }

  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
