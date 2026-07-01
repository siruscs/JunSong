<template>
  <main class="open-portal">
    <header class="portal-nav">
      <OpenBrand />
      <nav class="nav-links" aria-label="页面导航">
        <a href="#capabilities">能力</a>
        <RouterLink to="/open-platform/docs">开发文档</RouterLink>
      </nav>
      <div class="nav-actions">
        <RouterLink class="text-link" to="/login">登录</RouterLink>
        <RouterLink class="nav-button" to="/login?redirect=/open/app">
          控制台
          <el-icon><ArrowRight /></el-icon>
        </RouterLink>
      </div>
    </header>

    <section class="hero-section">
      <div class="hero-copy">
        <p class="section-kicker">OPEN PLATFORM</p>
        <h1>开放能力的公共入口</h1>
        <p class="hero-subtitle">面向商户、开发者和交付伙伴，统一说明可开放能力、接入路径、密钥治理和生产边界。</p>
        <div class="hero-actions">
          <a class="primary-action" href="#capabilities">
            查看能力
            <el-icon><ArrowRight /></el-icon>
          </a>
          <RouterLink class="secondary-action" to="/open-platform/docs">进入开发文档</RouterLink>
        </div>
        <div class="hero-brief">
          <article v-for="item in heroBrief" :key="item.title">
            <span>{{ item.label }}</span>
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </article>
        </div>
      </div>

      <aside class="network-panel" aria-label="开放平台能力网络">
        <div class="network-header">
          <span>接入工作台</span>
          <strong>从文档到生产</strong>
        </div>
        <div class="access-panel">
          <div class="access-core">
            <span>Base URL</span>
            <strong>/prod-api/openapi/v1</strong>
          </div>
          <div v-for="node in gatewayNodes" :key="node.label" class="access-row">
            <div>
              <el-icon><component :is="node.icon" /></el-icon>
              <strong>{{ node.label }}</strong>
            </div>
            <span>{{ node.desc }}</span>
          </div>
        </div>
        <div class="network-footer">
          <div v-for="metric in heroSignals" :key="metric.label">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </div>
        </div>
      </aside>
    </section>

    <section class="entry-section">
      <div class="section-heading compact">
        <h2>正式接入从开发文档开始</h2>
        <p>首页只负责帮你判断方向。接口查询、签名调试、接入样例和申请路径统一放在开发文档体系里。</p>
      </div>
      <div class="entry-grid">
        <RouterLink v-for="entry in docEntries" :key="entry.title" class="entry-card" :to="entry.to">
          <el-icon><component :is="entry.icon" /></el-icon>
          <div>
            <span>{{ entry.label }}</span>
            <h3>{{ entry.title }}</h3>
            <p>{{ entry.desc }}</p>
          </div>
          <el-icon class="entry-arrow"><ArrowRight /></el-icon>
        </RouterLink>
      </div>
    </section>

    <section id="capabilities" class="capability-section">
      <div class="section-heading">
        <h2>当前先开放确定能力</h2>
        <p>首页只展示可以对外说明的能力范围。高风险模块继续走审批、白名单和场景评审。</p>
      </div>

      <div class="capability-layout">
        <div class="capability-grid">
          <article
            v-for="capability in openCapabilities"
            :key="capability.title"
            class="capability-card"
            :class="capability.className"
          >
            <div class="card-head">
              <el-icon><component :is="capability.icon" /></el-icon>
              <span>{{ capability.status }}</span>
            </div>
            <h3>{{ capability.title }}</h3>
            <p>{{ capability.summary }}</p>
            <div class="capability-tags">
              <span v-for="point in capability.points" :key="point">{{ point }}</span>
            </div>
          </article>
        </div>

        <aside class="controlled-panel">
          <div class="controlled-copy">
            <span>受控开放</span>
            <h3>高风险能力先做场景审批</h3>
            <p>财务和低代码能力不放进默认开放矩阵，必须先确认客户、字段、额度、审计和回滚策略。</p>
          </div>
          <div class="controlled-list">
            <div v-for="item in controlledCapabilities" :key="item.title" class="controlled-item">
              <span>{{ item.status }}</span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.summary }}</p>
            </div>
          </div>
        </aside>
      </div>
    </section>

    <section id="flow" class="journey-section">
      <div class="section-heading">
        <h2>接入路径必须可控</h2>
        <p>测试 Key 只用于联调，生产 Key 经过审批后启用。每个应用都绑定租户、联系人和回调地址。</p>
      </div>
      <div class="flow-track">
        <article v-for="step in accessFlow" :key="step.title" class="flow-card">
          <span>{{ step.verb }}</span>
          <h3>{{ step.title }}</h3>
          <p>{{ step.desc }}</p>
        </article>
      </div>
    </section>

    <section id="governance" class="governance-section">
      <div class="governance-copy">
        <h2>开放能力，不开放后台菜单</h2>
        <p>平台按租户、应用、能力和密钥治理访问范围，新租户不会默认拿到平台运营类菜单。</p>
      </div>
      <div class="governance-grid">
        <div v-for="item in governance" :key="item.title" class="governance-item">
          <el-icon><component :is="item.icon" /></el-icon>
          <div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="boundary-band">
      <div>
        <h2>后台菜单和小程序权限分开治理</h2>
        <p>新租户默认不开放租户管理、开放平台后台管理和系统后台移动办公菜单。小程序权限中的移动办公仍保留。</p>
      </div>
      <RouterLink class="primary-action" to="/login?redirect=/open/app">
        进入控制台
        <el-icon><ArrowRight /></el-icon>
      </RouterLink>
      <RouterLink class="secondary-action is-on-band" to="/open-platform/docs">查看开发文档</RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import OpenBrand from '../components/OpenBrand.vue'
import {
  ArrowRight,
  Bell,
  Connection,
  Document,
  Finished,
  Guide,
  Key,
  Link,
  Lock,
  Management,
  Monitor,
  Stamp,
  Tickets,
  User,
} from '@element-plus/icons-vue'

const gatewayNodes = [
  { label: '接口目录', desc: '确认路径、scope 和开放状态', icon: Document },
  { label: '签名调试', desc: '生成签名串和 curl 请求', icon: Key },
  { label: '接入样例', desc: '复制 Node.js、Java 和 curl 样例', icon: Monitor },
  { label: '申请接入', desc: '提交应用、租户和回调信息', icon: Bell },
]

const heroSignals = [
  { label: '鉴权方式', value: 'Key + Secret' },
  { label: '接入方式', value: 'API + Webhook' },
  { label: '生产启用', value: '审批后开放' },
]

const heroBrief = [
  { label: '01', title: '先判断能力', desc: '确认会员、流程、业务申请和基础能力是否覆盖你的场景。' },
  { label: '02', title: '再进入文档', desc: '接口目录、签名规则、样例代码和申请入口都在开发文档中完成。' },
  { label: '03', title: '最后走审批', desc: '生产 Key、写入能力和高风险模块按租户、应用和 scope 审核。' },
]

const docEntries = [
  {
    label: 'DOCS',
    title: '开发文档',
    desc: '查看鉴权、接口目录、开放边界和上线路径。',
    to: '/open-platform/docs',
    icon: Document,
  },
  {
    label: 'SIGN',
    title: '签名调试',
    desc: '在浏览器本地生成 HMAC 签名和请求头。',
    to: '/open-platform/debug',
    icon: Key,
  },
  {
    label: 'CODE',
    title: '接入样例',
    desc: '复制服务端签名、curl 调用和异常处理样例。',
    to: '/open-platform/samples',
    icon: Monitor,
  },
  {
    label: 'APPLY',
    title: '申请接入',
    desc: '确认应用类型、租户、联系人和回调地址。',
    to: '/open-platform/apply',
    icon: Bell,
  },
]

const openCapabilities = [
  {
    title: '会员能力',
    status: '已开放',
    icon: User,
    className: 'is-featured',
    summary: '开放会员资料、积分、兑换、活动和运营统计，适合商户系统和私域工具接入。',
    points: ['会员列表', '积分规则', '运营看板'],
  },
  {
    title: '流程能力',
    status: '已开放',
    icon: Finished,
    className: 'is-process',
    summary: '让外部系统读取流程、展示待办，并在授权身份下处理审批动作。',
    points: ['流程定义', '任务处理', '审批历史'],
  },
  {
    title: '业务申请能力',
    status: '已开放',
    icon: Tickets,
    className: 'is-request',
    summary: '支持门店开办等业务从外部入口创建、提交、查询和撤回。',
    points: ['创建申请', '按单号查询', '提交撤回'],
  },
  {
    title: '平台基础能力',
    status: '已开放',
    icon: Connection,
    className: 'is-foundation',
    summary: '应用、密钥、SDK、Webhook 和元数据管理组成统一开放底座。',
    points: ['应用申请', '回调地址', '额度权限'],
  },
]

const controlledCapabilities = [
  {
    title: '财务能力',
    status: '内测中',
    summary: '先评估字段权限、审计和额度策略。',
  },
  {
    title: '低代码能力',
    status: '规划中',
    summary: '先建立模板审核、沙箱发布和回滚机制。',
  },
]

const accessFlow = [
  { verb: '了解', title: '确认开放能力', desc: '先确认当前能力状态、数据范围和使用边界。' },
  { verb: '申请', title: '提交应用信息', desc: '登记应用、租户、联系人和回调地址。' },
  { verb: '联调', title: '获取测试 Key', desc: '在测试额度内验证接口、签名和回调。' },
  { verb: '验证', title: '完成业务验收', desc: '确认错误码、事件通知和审批流程闭环。' },
  { verb: '上线', title: '启用生产 Key', desc: '审批通过后启用正式凭证和生产额度。' },
]

const governance = [
  { title: '测试 Key 不进生产', desc: '测试凭证只用于联调环境，默认低额度。', icon: Key },
  { title: '生产 Key 必须审批', desc: '生产凭证启用前需要审核应用和能力范围。', icon: Stamp },
  { title: '应用绑定租户', desc: '调用数据始终限制在授权租户和能力内。', icon: Lock },
  { title: '回调地址受控', desc: 'Webhook 订阅绑定应用，并校验回调地址。', icon: Link },
  { title: '权限额度分级', desc: '不同能力可以设置不同访问权限、频次和额度。', icon: Management },
  { title: '高风险能力隔离', desc: '财务与低代码能力先白名单内测，不默认开放。', icon: Guide },
]
</script>

<style scoped lang="scss">
.open-portal {
  --surface: #f8fbff;
  --surface-elevated: #ffffff;
  --surface-muted: #eef4fb;
  --ink: #172033;
  --ink-soft: #667085;
  --ink-muted: #8a95a5;
  --accent: var(--theme-primary, #2c6975);
  --accent-strong: var(--theme-primary-dark, #1e4d56);
  --accent-soft: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.11);
  --line: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.14);
  --radius: 18px;

  position: relative;
  isolation: isolate;
  min-height: 100dvh;
  overflow-x: hidden;
  color: var(--ink);
  background:
    radial-gradient(circle at 12% 6%, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.18), transparent 30%),
    radial-gradient(circle at 84% 12%, color-mix(in srgb, var(--theme-primary-light, #68b2a0) 18%, transparent), transparent 28%),
    linear-gradient(135deg, color-mix(in srgb, var(--theme-login-bg-start, #1a3d45) 12%, #f8fbff 88%) 0%, color-mix(in srgb, var(--theme-login-bg-end, #2c4a4f) 14%, #eef4fb 86%) 52%, #f9fbff 100%);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.open-portal::before {
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

.open-portal > * {
  position: relative;
  z-index: 1;
}

.portal-nav {
  position: sticky;
  top: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto minmax(220px, 1fr);
  align-items: center;
  gap: 24px;
  height: 68px;
  padding: 0 clamp(20px, 5vw, 76px);
  border-bottom: 1px solid var(--line);
  background: rgba(248, 251, 255, 0.86);
  backdrop-filter: blur(18px);
}

.nav-links,
.nav-actions,
.hero-actions,
.primary-action,
.secondary-action,
.nav-button,
.card-head,
.network-header,
.network-footer,
.capability-tags,
.entry-card {
  display: flex;
  align-items: center;
}

.nav-links a,
.text-link,
.nav-button,
.primary-action,
.secondary-action {
  white-space: nowrap;
}

.nav-links {
  justify-content: center;
  gap: 24px;

  a {
    color: var(--ink-soft);
    text-decoration: none;
    font-size: 14px;
    font-weight: 400;
    transition: color 0.18s ease, transform 0.18s ease;

    &:hover {
      color: var(--accent);
      transform: translateY(-1px);
    }
  }
}

.nav-actions {
  justify-content: flex-end;
  gap: 14px;
}

.text-link {
  color: var(--ink-soft);
  font-size: 14px;
  text-decoration: none;
}

.nav-button,
.primary-action,
.secondary-action,
.role-card a {
  border-radius: 999px;
  font-weight: 750;
  text-decoration: none;
  transition: transform 0.16s ease, border-color 0.16s ease, background-color 0.16s ease;

  &:active {
    transform: translateY(1px);
  }
}

.nav-button {
  justify-content: center;
  gap: 6px;
  min-height: 40px;
  padding: 0 18px;
  color: #ffffff;
  background: var(--accent);
}

.hero-section {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 0.82fr);
  gap: clamp(36px, 6vw, 92px);
  min-height: min(820px, calc(100dvh - 68px));
  padding: clamp(46px, 6vw, 76px) clamp(20px, 5vw, 76px) 58px;
}

.hero-copy {
  align-self: center;
  max-width: 760px;

  h1 {
    max-width: 700px;
    margin: 16px 0 18px;
    font-size: clamp(44px, 6vw, 74px);
    line-height: 1.04;
    letter-spacing: 0;
  }
}

.section-kicker {
  margin: 0;
  color: var(--accent);
  font-size: 12px;
  font-weight: 850;
  letter-spacing: 0.1em;
}

.hero-subtitle {
  max-width: 620px;
  margin: 0;
  color: var(--ink-soft);
  font-size: 19px;
  line-height: 1.7;
}

.hero-actions {
  gap: 14px;
  margin-top: 28px;
  flex-wrap: wrap;
}

.hero-brief {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 28px;

  article {
    min-height: 150px;
    padding: 18px;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: rgba(255, 255, 255, 0.66);
  }

  span {
    color: var(--accent);
    font-size: 12px;
    font-weight: 850;
  }

  strong {
    display: block;
    margin: 14px 0 8px;
    font-size: 18px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    font-size: 14px;
    line-height: 1.68;
  }
}

.primary-action,
.secondary-action {
  justify-content: center;
  gap: 8px;
  min-height: 46px;
  padding: 0 22px;
}

.primary-action {
  color: #ffffff;
  background: var(--accent);

  &.light {
    color: var(--accent-strong);
    background: #ffffff;
  }
}

.secondary-action {
  color: var(--accent-strong);
  border: 1px solid rgba(15, 107, 86, 0.28);
  background: rgba(255, 255, 255, 0.72);

  &.is-on-band {
    color: var(--ink);
    border-color: var(--line);
    background: transparent;
  }
}

.network-panel {
  align-self: center;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background:
    linear-gradient(150deg, rgba(255, 255, 255, 0.92), rgba(232, 239, 236, 0.72)),
    var(--surface-elevated);
  box-shadow: 0 30px 80px rgba(25, 64, 53, 0.14);
}

.network-header {
  justify-content: space-between;
  color: var(--ink-muted);
  font-size: 13px;

  strong {
    color: var(--accent);
  }
}

.access-panel {
  display: grid;
  gap: 10px;
  margin: 18px 0;
}

.access-core {
  padding: 22px;
  border-radius: calc(var(--radius) - 4px);
  color: #ffffff;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 92%, #14211d 8%), var(--accent-strong));

  span {
    display: block;
    color: rgba(255, 255, 255, 0.7);
    font-size: 13px;
  }

  strong {
    display: block;
    margin-top: 10px;
    font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
    font-size: clamp(20px, 2.4vw, 28px);
    overflow-wrap: anywhere;
  }
}

.access-row {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);

  div {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .el-icon {
    width: 34px;
    height: 34px;
    color: var(--accent);
    border-radius: 12px;
    background: var(--accent-soft);
    font-size: 20px;
  }

  strong {
    font-size: 18px;
  }

  span {
    color: var(--ink-soft);
    line-height: 1.62;
  }
}

.network-map {
  position: relative;
  min-height: 360px;
  margin: 22px 0;
  border: 1px solid var(--line);
  border-radius: calc(var(--radius) - 4px);
  background:
    linear-gradient(var(--line) 1px, transparent 1px),
    linear-gradient(90deg, var(--line) 1px, transparent 1px),
    #f7faf8;
  background-size: 42px 42px;
  overflow: hidden;
}

.network-map::before,
.network-map::after {
  content: "";
  position: absolute;
  inset: 18% 14%;
  border: 1px solid rgba(15, 107, 86, 0.18);
  border-radius: 50%;
}

.network-map::after {
  inset: 31% 27%;
}

.network-core,
.network-node {
  position: absolute;
  display: grid;
  place-items: center;
  text-align: center;
  border: 1px solid rgba(15, 107, 86, 0.18);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 36px rgba(25, 64, 53, 0.12);
}

.network-core {
  top: 50%;
  left: 50%;
  width: 132px;
  height: 132px;
  transform: translate(-50%, -50%);
  color: #ffffff;
  background: var(--accent);

  .el-icon {
    font-size: 34px;
  }

  span {
    margin-top: 6px;
    font-weight: 800;
  }
}

.network-node {
  width: 112px;
  height: 88px;
  color: var(--ink);
  font-size: 13px;
  font-weight: 750;

  .el-icon {
    color: var(--accent);
    font-size: 24px;
  }
}

.node-docs {
  top: 32px;
  left: 36px;
}

.node-key {
  top: 34px;
  right: 36px;
}

.node-webhook {
  right: 44px;
  bottom: 36px;
}

.node-console {
  bottom: 34px;
  left: 46px;
}

.network-footer {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border-radius: 14px;
  background: var(--line);

  div {
    padding: 16px;
    background: #14211d;
  }

  span {
    display: block;
    color: rgba(255, 255, 255, 0.62);
    font-size: 12px;
  }

  strong {
    display: block;
    margin-top: 6px;
    color: #ffffff;
    font-size: 14px;
  }
}

.capability-section,
.entry-section,
.api-catalog-section,
.role-section,
.flow-section,
.journey-section,
.governance-section,
.boundary-band {
  padding: 82px clamp(20px, 5vw, 76px);
}

.section-heading {
  max-width: 720px;
  margin-bottom: 32px;

  h2 {
    margin: 0 0 14px;
    font-size: clamp(30px, 4.4vw, 50px);
    line-height: 1.12;
    letter-spacing: 0;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    font-size: 17px;
    line-height: 1.75;
  }
}

.section-heading.compact {
  max-width: 820px;
}

.entry-section {
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  background:
    linear-gradient(135deg, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.08), transparent 46%),
    rgba(255, 255, 255, 0.38);
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.entry-card {
  position: relative;
  gap: 16px;
  min-height: 190px;
  align-items: flex-start;
  padding: 22px;
  color: var(--ink);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.78);
  text-decoration: none;
  transition: transform 0.18s ease, border-color 0.18s ease, background-color 0.18s ease;

  > .el-icon:first-child {
    flex: 0 0 auto;
    width: 44px;
    height: 44px;
    color: var(--accent);
    border-radius: 14px;
    background: var(--accent-soft);
    font-size: 23px;
  }

  span {
    color: var(--accent);
    font-size: 12px;
    font-weight: 850;
  }

  h3 {
    margin: 12px 0 10px;
    font-size: 22px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.68;
  }

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.26);
    background: rgba(255, 255, 255, 0.9);
  }

  &:active {
    transform: translateY(1px);
  }
}

.entry-arrow {
  position: absolute;
  right: 18px;
  bottom: 18px;
  color: var(--accent);
}

.capability-layout {
  display: grid;
  gap: 18px;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.capability-card,
.controlled-panel,
.catalog-filter,
.endpoint-panel,
.catalog-note,
.role-card,
.flow-card,
.governance-item {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.78);
}

.capability-card {
  display: flex;
  flex-direction: column;
  min-height: 320px;
  padding: 24px;

  h3 {
    margin: 30px 0 12px;
    font-size: 23px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.75;
  }
}

.capability-card.is-featured {
  background:
    linear-gradient(145deg, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.11), rgba(255, 255, 255, 0.84)),
    var(--surface-elevated);
}

.capability-card.is-process {
  background:
    linear-gradient(145deg, rgba(72, 122, 191, 0.09), rgba(255, 255, 255, 0.84)),
    var(--surface-elevated);
}

.capability-card.is-request {
  background:
    linear-gradient(145deg, rgba(185, 130, 57, 0.09), rgba(255, 255, 255, 0.84)),
    var(--surface-elevated);
}

.capability-card.is-foundation {
  background:
    linear-gradient(145deg, rgba(88, 104, 128, 0.09), rgba(255, 255, 255, 0.84)),
    var(--surface-elevated);
}

.controlled-panel {
  display: grid;
  grid-template-columns: minmax(260px, 0.38fr) minmax(0, 0.62fr);
  gap: 22px;
  align-items: stretch;
  padding: 24px;
  background: #14211d;
  color: #ffffff;

  h3 {
    margin: 8px 0 12px;
    font-size: 26px;
  }

  p {
    margin: 0;
    color: rgba(255, 255, 255, 0.7);
    line-height: 1.75;
  }
}

.controlled-copy > span {
  color: #8edac8;
  font-size: 13px;
  font-weight: 850;
}

.controlled-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.controlled-item {
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.06);

  span {
    color: #8edac8;
    font-size: 13px;
    font-weight: 800;
  }

  strong {
    display: block;
    margin: 8px 0;
    font-size: 19px;
  }

  p {
    margin: 0;
    color: rgba(255, 255, 255, 0.68);
    line-height: 1.65;
  }
}

.card-head {
  justify-content: space-between;

  .el-icon {
    width: 44px;
    height: 44px;
    color: var(--accent);
    border-radius: 14px;
    background: var(--accent-soft);
    font-size: 23px;
  }

  span {
    padding: 6px 11px;
    color: var(--accent-strong);
    border-radius: 999px;
    background: var(--accent-soft);
    font-size: 13px;
    font-weight: 800;
  }
}

.capability-tags {
  gap: 8px;
  flex-wrap: wrap;
  margin-top: auto;
  padding-top: 22px;

  span {
    padding: 8px 10px;
    color: var(--accent-strong);
    border: 1px solid rgba(15, 107, 86, 0.18);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.64);
    font-size: 13px;
    font-weight: 700;
  }
}

.catalog-shell {
  display: grid;
  grid-template-columns: minmax(190px, 0.22fr) minmax(0, 0.56fr) minmax(240px, 0.22fr);
  gap: 16px;
  align-items: start;
}

.catalog-filter {
  display: grid;
  gap: 8px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.72);
}

.catalog-filter button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  padding: 0 14px;
  color: var(--ink-soft);
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  font: inherit;
  font-weight: 750;
  cursor: pointer;
  transition: color 0.16s ease, border-color 0.16s ease, background-color 0.16s ease, transform 0.16s ease;

  &:hover,
  &.active {
    color: var(--accent-strong);
    border-color: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.2);
    background: var(--accent-soft);
  }

  &:active {
    transform: translateY(1px);
  }

  strong {
    color: var(--accent);
    font-size: 13px;
  }
}

.endpoint-panel {
  padding: 18px;
  background: rgba(255, 255, 255, 0.78);
}

.endpoint-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);

  div {
    display: grid;
    gap: 4px;
  }

  strong {
    color: var(--ink);
    font-size: 20px;
  }

  span {
    color: var(--ink-muted);
    font-size: 13px;
  }

  a {
    color: var(--accent-strong);
    font-size: 14px;
    font-weight: 800;
    text-decoration: none;
    white-space: nowrap;
  }
}

.endpoint-list {
  display: grid;
  gap: 10px;
  padding-top: 16px;
}

.endpoint-item {
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.7)),
    var(--surface-elevated);

  p {
    margin: 12px 0 0;
    color: var(--ink-soft);
    line-height: 1.65;
  }
}

.endpoint-main,
.endpoint-meta,
.catalog-note div {
  display: flex;
  align-items: center;
}

.endpoint-main {
  gap: 12px;
  min-width: 0;

  code {
    min-width: 0;
    color: var(--ink);
    font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
    font-size: 14px;
    overflow-wrap: anywhere;
  }
}

.method-chip {
  min-width: 54px;
  padding: 6px 9px;
  color: #ffffff;
  border-radius: 999px;
  background: var(--accent);
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  font-weight: 850;
  text-align: center;

  &.post {
    background: var(--accent-strong);
  }
}

.endpoint-meta {
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;

  span,
  strong {
    padding: 7px 10px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 800;
  }

  span {
    color: var(--ink-soft);
    border: 1px solid var(--line);
    background: rgba(255, 255, 255, 0.62);
  }

  strong {
    color: var(--accent-strong);
    background: var(--accent-soft);
  }
}

.catalog-note {
  padding: 22px;
  color: #ffffff;
  background:
    linear-gradient(145deg, color-mix(in srgb, var(--theme-primary-dark, #1e4d56) 88%, #10172a 12%), var(--accent));

  h3 {
    margin: 0 0 12px;
    font-size: 23px;
  }

  p {
    margin: 0;
    color: rgba(255, 255, 255, 0.72);
    line-height: 1.72;
  }

  div {
    gap: 10px;
    flex-wrap: wrap;
    margin-top: 22px;
  }

  a {
    min-height: 38px;
    padding: 0 14px;
    color: #ffffff;
    border: 1px solid rgba(255, 255, 255, 0.24);
    border-radius: 999px;
    font-size: 13px;
    font-weight: 800;
    line-height: 38px;
    text-decoration: none;
  }
}

.role-section {
  display: grid;
  grid-template-columns: minmax(260px, 0.36fr) minmax(0, 0.64fr);
  gap: 38px;
  background:
    linear-gradient(135deg, rgba(15, 107, 86, 0.08), transparent 42%),
    var(--surface-muted);
  color: var(--ink);
}

.role-intro {
  align-self: start;

  h2 {
    margin: 0 0 14px;
    font-size: clamp(30px, 4.4vw, 48px);
    line-height: 1.12;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.75;
  }
}

.role-layout {
  display: grid;
  gap: 14px;
}

.role-card {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 18px;
  padding: 24px;
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.66);

  .el-icon {
    color: var(--accent);
    font-size: 30px;
  }

  h3 {
    margin: 0 0 9px;
    font-size: 23px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.7;
  }

  a {
    display: inline-flex;
    margin-top: 16px;
    color: var(--accent-strong);
    border-bottom: 1px solid rgba(15, 107, 86, 0.32);
  }
}

.flow-track {
  display: grid;
  grid-template-columns: repeat(5, minmax(180px, 1fr));
  gap: 12px;
}

.flow-card {
  min-height: 230px;
  padding: 22px;
  background: var(--surface-elevated);

  span {
    color: var(--accent);
    font-size: 15px;
    font-weight: 850;
  }

  h3 {
    margin: 28px 0 10px;
    font-size: 21px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.7;
  }
}

.governance-section {
  display: grid;
  grid-template-columns: minmax(300px, 0.34fr) minmax(0, 0.66fr);
  gap: 42px;
}

.governance-copy {
  h2 {
    margin: 0 0 14px;
    font-size: clamp(30px, 4.4vw, 48px);
    line-height: 1.12;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.75;
  }
}

.governance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.governance-item {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 16px;
  padding: 22px;

  .el-icon {
    color: var(--accent);
    font-size: 28px;
  }

  h3 {
    margin: 0 0 8px;
    font-size: 19px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.7;
  }
}

.boundary-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 30px;
  border-top: 1px solid var(--line);
  background:
    linear-gradient(135deg, rgba(15, 107, 86, 0.1), transparent 46%),
    var(--surface-elevated);

  h2 {
    max-width: 760px;
    margin: 0 0 12px;
    color: var(--ink);
    font-size: clamp(28px, 4.2vw, 46px);
    line-height: 1.12;
  }

  p {
    max-width: 860px;
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.78;
  }
}

@media (prefers-color-scheme: dark) {
  .open-portal {
    --surface: #111a17;
    --surface-elevated: #18231f;
    --surface-muted: #202e29;
    --ink: #edf6f2;
    --ink-soft: #b7c7c1;
    --ink-muted: #8fa09a;
    --accent: #6fd1bc;
    --accent-strong: #d9fff4;
    --accent-soft: rgba(111, 209, 188, 0.14);
    --line: rgba(237, 246, 242, 0.12);

    background:
      radial-gradient(circle at 12% 6%, rgba(111, 209, 188, 0.14), transparent 30%),
      linear-gradient(180deg, #0d1412 0%, var(--surface) 48%, #101815 100%);
  }

  .portal-nav {
    background: rgba(13, 20, 18, 0.9);
  }

  .hero-brief article,
  .access-row,
  .entry-card {
    background: rgba(24, 35, 31, 0.82);
  }

  .network-panel,
  .capability-card,
  .catalog-filter,
  .endpoint-panel,
  .endpoint-item,
  .flow-card,
  .governance-item {
    background: rgba(24, 35, 31, 0.82);
  }

  .network-map {
    background:
      linear-gradient(var(--line) 1px, transparent 1px),
      linear-gradient(90deg, var(--line) 1px, transparent 1px),
      #121c18;
  }

  .network-node {
    background: rgba(24, 35, 31, 0.92);
  }

  .network-footer div,
  .controlled-panel {
    background: #0d1412;
  }

  .role-section {
    background:
      linear-gradient(135deg, rgba(111, 209, 188, 0.08), transparent 42%),
      var(--surface-muted);
  }

  .role-card {
    background: rgba(24, 35, 31, 0.82);
  }

  .capability-tags span {
    background: rgba(111, 209, 188, 0.08);
  }

  .endpoint-item {
    background:
      linear-gradient(135deg, rgba(24, 35, 31, 0.92), rgba(24, 35, 31, 0.72)),
      var(--surface-elevated);
  }

  .endpoint-meta span {
    background: rgba(111, 209, 188, 0.08);
  }

  .entry-section {
    background:
      linear-gradient(135deg, rgba(111, 209, 188, 0.08), transparent 46%),
      rgba(24, 35, 31, 0.46);
  }
}

@media (max-width: 1180px) {
  .portal-nav {
    grid-template-columns: minmax(180px, 0.8fr) auto minmax(160px, 0.8fr);
    gap: 18px;
  }

  .nav-links {
    gap: 20px;
  }

  .capability-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .entry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .flow-track {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .flow-card:last-child {
    grid-column: span 2;
  }
}

@media (max-width: 1024px) {
  .portal-nav {
    grid-template-columns: 1fr;
    justify-items: center;
    height: auto;
    min-height: 86px;
    padding-top: 12px;
    padding-bottom: 12px;
  }

  .nav-links {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 10px 22px;
  }

  .nav-actions {
    display: none;
  }

  .hero-section,
  .entry-grid,
  .catalog-shell,
  .role-section,
  .governance-section {
    grid-template-columns: 1fr;
  }

  .hero-section {
    min-height: auto;
  }

  .capability-card.is-featured,
  .controlled-panel,
  .capability-card.is-process,
  .capability-card.is-request,
  .capability-card.is-foundation {
    grid-column: auto;
  }

  .controlled-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .portal-nav {
    height: auto;
    min-height: 84px;
    padding: 12px 18px;
  }

  .nav-links {
    gap: 8px 18px;
  }

  .text-link {
    display: none;
  }

  .hero-section,
  .entry-section,
  .capability-section,
  .api-catalog-section,
  .role-section,
  .flow-section,
  .journey-section,
  .governance-section,
  .boundary-band {
    padding: 46px 18px;
  }

  .hero-copy h1 {
    font-size: 40px;
  }

  .hero-subtitle {
    font-size: 17px;
  }

  .hero-brief,
  .entry-grid {
    grid-template-columns: 1fr;
  }

  .hero-brief article,
  .entry-card {
    min-height: auto;
  }

  .network-panel {
    padding: 16px;
  }

  .network-map {
    min-height: 420px;
  }

  .network-core {
    width: 116px;
    height: 116px;
  }

  .network-node {
    width: 104px;
    height: 82px;
  }

  .node-docs {
    top: 28px;
    left: 20px;
  }

  .node-key {
    top: 28px;
    right: 20px;
  }

  .node-webhook {
    right: 20px;
    bottom: 28px;
  }

  .node-console {
    bottom: 28px;
    left: 20px;
  }

  .network-footer,
  .capability-grid,
  .capability-layout,
  .controlled-list,
  .flow-track,
  .governance-grid {
    grid-template-columns: 1fr;
  }

  .catalog-filter {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .catalog-filter button {
    min-height: 44px;
  }

  .endpoint-toolbar,
  .endpoint-main {
    align-items: flex-start;
    flex-direction: column;
  }

  .capability-card.is-featured,
  .controlled-panel,
  .capability-card.is-process,
  .capability-card.is-request,
  .capability-card.is-foundation,
  .flow-card:last-child {
    grid-column: span 1;
  }

  .capability-card,
  .controlled-panel,
  .flow-card {
    min-height: auto;
  }

  .role-card,
  .governance-item {
    grid-template-columns: 1fr;
  }

  .boundary-band {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
