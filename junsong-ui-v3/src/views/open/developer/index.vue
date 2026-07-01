<template>
  <main class="open-docs">
    <header class="docs-nav">
      <OpenBrand />
      <nav aria-label="文档导航">
        <RouterLink to="/open-platform">平台首页</RouterLink>
        <a href="#apis">接口目录</a>
        <RouterLink to="/open-platform/debug">签名调试</RouterLink>
        <RouterLink to="/open-platform/samples">接入样例</RouterLink>
      </nav>
      <div class="nav-actions">
        <RouterLink class="ghost-link" to="/open-platform/debug">调试签名</RouterLink>
        <RouterLink class="ghost-link" to="/open-platform/samples">接入样例</RouterLink>
        <RouterLink class="ghost-link" to="/open-platform/apply">申请接入</RouterLink>
        <RouterLink class="console-link" to="/login?redirect=/open/app">控制台</RouterLink>
      </div>
    </header>

    <section class="docs-hero">
      <div>
        <p class="kicker">DEVELOPER GUIDE</p>
        <h1>开发者接入文档</h1>
        <p>这里先给外部开发者一条能走通的路径：申请应用、获取 Key、签名请求、调用接口、配置回调、申请生产。</p>
      </div>
      <aside class="quick-panel">
        <span>Base URL</span>
        <strong>/prod-api/openapi/v1</strong>
        <RouterLink to="/open-platform/debug">打开签名调试</RouterLink>
        <RouterLink to="/open-platform/samples">查看接入样例</RouterLink>
        <a href="/prod-api/open/v3/api-docs" target="_blank" rel="noreferrer">查看 OpenAPI JSON</a>
      </aside>
    </section>

    <section id="auth" class="auth-section">
      <div class="section-copy">
        <h2>请求鉴权</h2>
        <p>开放 API 使用 AppKey 和 AppSecret 计算 HMAC-SHA256 签名。时间戳有效期为 5 分钟，Nonce 会缓存 10 分钟防重放。</p>
      </div>
      <div class="code-card">
        <div v-for="header in authHeaders" :key="header.name" class="header-row">
          <code>{{ header.name }}</code>
          <span>{{ header.desc }}</span>
        </div>
      </div>
    </section>

    <section class="sign-section">
      <div class="sign-rule">
        <h2>签名串</h2>
        <p>按照 method + path + timestamp + nonce + body 拼接后，用 AppSecret 做 HMAC-SHA256。</p>
      </div>
      <pre><code>signStr = method + path + timestamp + nonce + body
signature = HMAC_SHA256(appSecret, signStr)</code></pre>
    </section>

    <section id="apis" class="api-section">
      <div class="section-copy">
        <h2>接口目录</h2>
        <p>首批开放能力聚焦会员、流程和业务申请。财务与低代码能力进入受控开放，不在默认接口目录里承诺。</p>
      </div>

      <div class="api-tools">
        <label class="api-search">
          <span>搜索接口</span>
          <input v-model.trim="apiKeyword" type="search" placeholder="输入路径、说明或权限范围" />
        </label>
        <div class="api-filter-bar" aria-label="按能力筛选接口">
          <button
            v-for="filter in capabilityFilters"
            :key="filter.value"
            type="button"
            :class="{ active: selectedCapability === filter.value }"
            @click="selectedCapability = filter.value"
          >
            <span>{{ filter.label }}</span>
            <strong>{{ filter.count }}</strong>
          </button>
        </div>
        <p class="api-result-copy">当前显示 {{ filteredEndpointCount }} 个接口</p>
      </div>

      <div class="api-groups">
        <article v-for="group in filteredApiGroups" :key="group.title" class="api-group">
          <div class="group-head">
            <el-icon><component :is="group.icon" /></el-icon>
            <div>
              <h3>{{ group.title }}</h3>
              <p>{{ group.summary }}</p>
            </div>
          </div>
          <div class="endpoint-list">
            <div v-for="endpoint in group.endpoints" :key="endpoint.method + endpoint.path" class="endpoint-row">
              <span :class="['method', endpoint.method.toLowerCase()]">{{ endpoint.method }}</span>
              <code>{{ endpoint.path }}</code>
              <p>{{ endpoint.desc }}</p>
              <div class="endpoint-tags">
                <span>{{ endpoint.scope }}</span>
                <strong>{{ endpoint.status }}</strong>
              </div>
            </div>
          </div>
        </article>
      </div>
      <div v-if="filteredEndpointCount === 0" class="empty-api">
        <h3>没有匹配的接口</h3>
        <p>换一个关键词，或切回全部接口查看当前开放目录。</p>
      </div>
    </section>

    <section id="flow" class="flow-section">
      <h2>接入流程</h2>
      <div class="flow-grid">
        <article v-for="item in accessFlow" :key="item.title">
          <span>{{ item.verb }}</span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>

    <section id="limits" class="limits-section">
      <div>
        <h2>开放边界</h2>
        <p>开放平台不等于开放后台菜单。生产 Key、财务能力、低代码能力和写入类接口，都需要按租户、应用和能力单独审批。</p>
      </div>
      <RouterLink class="primary-link" to="/open-platform/apply">申请接入</RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Connection, Finished, Tickets, User } from '@element-plus/icons-vue'
import OpenBrand from '../components/OpenBrand.vue'
import { buildCapabilityFilters, groupOpenApiCatalog, type OpenCapabilityKey } from '../data/catalog'

const authHeaders = [
  { name: 'X-App-Key', desc: '应用公开标识，从开放平台控制台获取。' },
  { name: 'X-App-Timestamp', desc: '毫秒时间戳，和服务端时间差不能超过 5 分钟。' },
  { name: 'X-App-Nonce', desc: '随机字符串，同一个 AppKey 下 10 分钟内不可重复。' },
  { name: 'X-App-Signature', desc: 'HMAC-SHA256 签名结果，使用 AppSecret 计算。' },
]

const capabilityIcons: Record<OpenCapabilityKey, any> = {
  member: User,
  workflow: Finished,
  request: Tickets,
  foundation: Connection,
}

const apiGroups = groupOpenApiCatalog().map((group) => ({
  title: group.label,
  summary: group.summary,
  icon: capabilityIcons[group.key],
  key: group.key,
  endpoints: group.endpoints,
}))

const apiKeyword = ref('')
const selectedCapability = ref<OpenCapabilityKey | 'all'>('all')
const capabilityFilters = computed(buildCapabilityFilters)

const filteredApiGroups = computed(() => {
  const keyword = apiKeyword.value.toLowerCase()
  return apiGroups
    .filter((group) => selectedCapability.value === 'all' || group.key === selectedCapability.value)
    .map((group) => ({
      ...group,
      endpoints: group.endpoints.filter((endpoint) => {
        if (!keyword) return true
        return [endpoint.path, endpoint.desc, endpoint.scope, endpoint.status, endpoint.method]
          .some((field) => field.toLowerCase().includes(keyword))
      }),
    }))
    .filter((group) => group.endpoints.length > 0)
})

const filteredEndpointCount = computed(() =>
  filteredApiGroups.value.reduce((total, group) => total + group.endpoints.length, 0)
)

const accessFlow = [
  { verb: '申请', title: '提交开放应用', desc: '登记应用、租户、联系人和回调地址。' },
  { verb: '测试', title: '获取测试 Key', desc: '使用低额度 Key 完成联调。' },
  { verb: '签名', title: '接入鉴权', desc: '按签名规则添加请求头，避免重放请求。' },
  { verb: '上线', title: '申请生产 Key', desc: '审批通过后启用生产能力和正式额度。' },
]
</script>

<style scoped lang="scss">
.open-docs {
  --surface: #f8fbff;
  --surface-elevated: #ffffff;
  --ink: #172033;
  --ink-soft: #667085;
  --accent: var(--theme-primary, #2c6975);
  --accent-soft: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.1);
  --line: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.14);
  --radius: 18px;

  position: relative;
  isolation: isolate;
  min-height: 100dvh;
  overflow-x: hidden;
  color: var(--ink);
  background:
    radial-gradient(circle at 12% 4%, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.16), transparent 28%),
    radial-gradient(circle at 84% 14%, color-mix(in srgb, var(--theme-primary-light, #68b2a0) 16%, transparent), transparent 26%),
    linear-gradient(135deg, color-mix(in srgb, var(--theme-login-bg-start, #1a3d45) 10%, #f8fbff 90%) 0%, #eef4fb 54%, #f9fbff 100%);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.open-docs::before {
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

.open-docs > * {
  position: relative;
  z-index: 1;
}

.docs-nav {
  position: sticky;
  top: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto minmax(160px, 1fr);
  align-items: center;
  gap: 24px;
  min-height: 68px;
  padding: 0 clamp(20px, 5vw, 76px);
  border-bottom: 1px solid var(--line);
  background: rgba(248, 251, 255, 0.88);
  backdrop-filter: blur(18px);

  nav {
    display: flex;
    gap: 24px;
  }

  a {
    color: var(--ink-soft);
    text-decoration: none;
    white-space: nowrap;
  }
}

.nav-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.console-link,
.ghost-link,
.primary-link {
  justify-self: end;
  min-height: 40px;
  padding: 0 18px;
  border-radius: 999px;
  font-weight: 750;
  line-height: 40px;
}

.console-link,
.primary-link {
  color: #ffffff !important;
  background: var(--accent);
}

.ghost-link {
  color: var(--ink) !important;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.66);
}

.docs-hero,
.auth-section,
.sign-section,
.api-section,
.flow-section,
.limits-section {
  padding: 76px clamp(20px, 5vw, 76px);
}

.docs-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.42fr);
  gap: 40px;
  align-items: end;

  h1 {
    max-width: 760px;
    margin: 14px 0 18px;
    font-size: clamp(42px, 6vw, 72px);
    line-height: 1.04;
    letter-spacing: 0;
  }

  p {
    max-width: 720px;
    margin: 0;
    color: var(--ink-soft);
    font-size: 18px;
    line-height: 1.75;
  }
}

.kicker {
  color: var(--accent) !important;
  font-size: 12px !important;
  font-weight: 850;
  letter-spacing: 0.1em;
}

.quick-panel,
.code-card,
.api-group,
.flow-grid article {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.78);
}

.quick-panel {
  padding: 24px;

  span {
    color: var(--ink-soft);
    font-size: 13px;
  }

  strong {
    display: block;
    margin: 12px 0 22px;
    font-size: 24px;
  }

  a {
    color: var(--accent);
    font-weight: 750;
    text-decoration: none;
  }
}

.auth-section,
.sign-section,
.limits-section {
  display: grid;
  grid-template-columns: minmax(280px, 0.36fr) minmax(0, 0.64fr);
  gap: 34px;
}

.section-copy,
.sign-rule {
  h2 {
    margin: 0 0 12px;
    font-size: clamp(30px, 4vw, 46px);
    line-height: 1.12;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.75;
  }
}

.code-card {
  padding: 8px;
}

.header-row {
  display: grid;
  grid-template-columns: minmax(190px, 0.35fr) 1fr;
  gap: 18px;
  padding: 16px;
  border-radius: 12px;

  &:nth-child(odd) {
    background: var(--accent-soft);
  }

  code {
    color: var(--accent);
    font-weight: 800;
  }

  span {
    color: var(--ink-soft);
  }
}

pre {
  overflow: auto;
  margin: 0;
  padding: 24px;
  color: #eafff8;
  border-radius: var(--radius);
  background: #121d19;
  line-height: 1.8;
}

.api-groups {
  display: grid;
  gap: 18px;
}

.api-tools {
  display: grid;
  grid-template-columns: minmax(260px, 0.34fr) minmax(0, 1fr) auto;
  gap: 14px;
  align-items: end;
  margin: 0 0 22px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.72);
}

.api-search {
  display: grid;
  gap: 8px;

  span {
    color: var(--ink);
    font-size: 13px;
    font-weight: 800;
  }

  input {
    width: 100%;
    min-height: 42px;
    padding: 0 13px;
    color: var(--ink);
    border: 1px solid var(--line);
    border-radius: 12px;
    outline: none;
    background: rgba(255, 255, 255, 0.74);
    font: inherit;

    &::placeholder {
      color: #7a8797;
    }

    &:focus {
      border-color: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.42);
      box-shadow: 0 0 0 3px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.12);
    }
  }
}

.api-filter-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.api-filter-bar button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 13px;
  color: var(--ink-soft);
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.58);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: color 0.16s ease, border-color 0.16s ease, background-color 0.16s ease, transform 0.16s ease;

  &:hover,
  &.active {
    color: var(--accent);
    border-color: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.22);
    background: var(--accent-soft);
  }

  &:active {
    transform: translateY(1px);
  }

  strong {
    font-size: 12px;
  }
}

.api-result-copy {
  margin: 0;
  color: var(--ink-soft);
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}

.api-group {
  padding: 22px;
}

.empty-api {
  padding: 30px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.72);

  h3 {
    margin: 0 0 8px;
    font-size: 22px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
  }
}

.group-head {
  display: grid;
  grid-template-columns: 46px 1fr;
  gap: 16px;
  align-items: start;
  margin-bottom: 18px;

  .el-icon {
    color: var(--accent);
    font-size: 30px;
  }

  h3 {
    margin: 0 0 8px;
    font-size: 23px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
  }
}

.endpoint-list {
  display: grid;
  gap: 8px;
}

.endpoint-row {
  display: grid;
  grid-template-columns: 72px minmax(220px, 0.32fr) minmax(220px, 1fr) minmax(180px, 0.22fr);
  gap: 14px;
  align-items: center;
  padding: 12px 0;
  border-top: 1px solid var(--line);

  code {
    color: var(--ink);
    font-weight: 750;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
  }
}

.endpoint-tags {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;

  span,
  strong {
    padding: 6px 9px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 800;
    white-space: nowrap;
  }

  span {
    color: var(--ink-soft);
    border: 1px solid var(--line);
    background: rgba(255, 255, 255, 0.62);
  }

  strong {
    color: var(--accent);
    background: var(--accent-soft);
  }
}

.method {
  width: 58px;
  padding: 5px 0;
  color: var(--accent);
  border-radius: 999px;
  background: var(--accent-soft);
  font-size: 12px;
  font-weight: 850;
  text-align: center;

  &.post {
    color: #7a4b0b;
    background: rgba(180, 117, 24, 0.13);
  }
}

.flow-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;

  article {
    min-height: 210px;
    padding: 22px;
  }

  span {
    color: var(--accent);
    font-weight: 850;
  }

  h3 {
    margin: 26px 0 10px;
    font-size: 21px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.7;
  }
}

.limits-section {
  align-items: center;
  border-top: 1px solid var(--line);
  background: var(--surface-elevated);

  h2 {
    margin: 0 0 12px;
    font-size: clamp(30px, 4vw, 46px);
  }

  p {
    max-width: 780px;
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.75;
  }
}

@media (prefers-color-scheme: dark) {
  .open-docs {
    --surface: #111a17;
    --surface-elevated: #18231f;
    --ink: #edf6f2;
    --ink-soft: #b7c7c1;
    --accent: #6fd1bc;
    --accent-soft: rgba(111, 209, 188, 0.14);
    --line: rgba(237, 246, 242, 0.12);
  }

  .docs-nav {
    background: rgba(17, 26, 23, 0.9);
  }

  .quick-panel,
  .code-card,
  .api-tools,
  .api-group,
  .empty-api,
  .flow-grid article {
    background: rgba(24, 35, 31, 0.82);
  }

  .api-search input,
  .api-filter-bar button {
    background: rgba(17, 26, 23, 0.72);
  }
}

@media (max-width: 1024px) {
  .docs-nav {
    grid-template-columns: 1fr auto;

    nav {
      display: none;
    }
  }

  .docs-hero,
  .auth-section,
  .sign-section,
  .limits-section {
    grid-template-columns: 1fr;
  }

  .flow-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .api-tools {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .docs-nav,
  .docs-hero,
  .auth-section,
  .sign-section,
  .api-section,
  .flow-section,
  .limits-section {
    padding-left: 18px;
    padding-right: 18px;
  }

  .docs-nav {
    min-height: 66px;

    .ghost-link {
      display: none;
    }
  }

  .docs-hero h1 {
    font-size: 40px;
  }

  .header-row,
  .endpoint-row,
  .group-head,
  .flow-grid {
    grid-template-columns: 1fr;
  }

  .endpoint-tags {
    justify-content: flex-start;
  }
}
</style>
