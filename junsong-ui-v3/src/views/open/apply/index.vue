<template>
  <main class="apply-page">
    <header class="apply-nav">
      <OpenBrand />
      <nav aria-label="申请页导航">
        <RouterLink to="/open-platform">平台首页</RouterLink>
        <RouterLink to="/open-platform/docs">开发文档</RouterLink>
        <RouterLink to="/open-platform/debug">签名调试</RouterLink>
        <RouterLink to="/open-platform/samples">接入样例</RouterLink>
        <RouterLink to="/login?redirect=/open/app">控制台</RouterLink>
      </nav>
    </header>

    <section class="apply-hero">
      <div class="hero-copy">
        <p class="kicker">ACCESS REQUEST</p>
        <h1>申请开放平台接入</h1>
        <p>先确认接入身份、应用场景和需要开放的能力。创建应用和发放 Key 仍在登录后的控制台完成，避免公共入口直接暴露后台能力。</p>
        <div class="hero-actions">
          <RouterLink class="primary-action" to="/login?redirect=/open/app">
            已有账号，进入控制台
            <el-icon><ArrowRight /></el-icon>
          </RouterLink>
          <RouterLink class="secondary-action" to="/register">没有账号，先注册</RouterLink>
        </div>
      </div>
      <aside class="status-panel">
        <span>默认策略</span>
        <strong>测试 Key 先行</strong>
        <p>生产 Key、写入接口、财务能力和低代码能力需要单独审批。</p>
      </aside>
    </section>

    <section class="request-grid">
      <article class="path-card">
        <div class="section-title">
          <span>01</span>
          <h2>选择接入路径</h2>
        </div>
        <div class="path-list">
          <div v-for="path in accessPaths" :key="path.title" class="path-item">
            <el-icon><component :is="path.icon" /></el-icon>
            <div>
              <h3>{{ path.title }}</h3>
              <p>{{ path.desc }}</p>
            </div>
          </div>
        </div>
      </article>

      <article class="checklist-card">
        <div class="section-title">
          <span>02</span>
          <h2>准备申请资料</h2>
        </div>
        <div class="check-list">
          <label v-for="item in checklist" :key="item" class="check-item">
            <input v-model="checkedItems" type="checkbox" :value="item" />
            <span>{{ item }}</span>
          </label>
        </div>
        <div class="progress-line">
          <span :style="{ width: readyPercent + '%' }"></span>
        </div>
        <p class="progress-copy">{{ checkedItems.length }} / {{ checklist.length }} 项已准备</p>
      </article>
    </section>

    <section class="form-section">
      <div class="section-title">
        <span>03</span>
        <h2>接入信息预填</h2>
      </div>
      <div class="form-layout">
        <div class="form-panel">
          <label>
            <span>应用名称</span>
            <input v-model="draft.appName" placeholder="例如：门店会员中台" />
          </label>
          <label>
            <span>接入方</span>
            <input v-model="draft.company" placeholder="公司、商户或服务商名称" />
          </label>
          <label>
            <span>联系人</span>
            <input v-model="draft.contact" placeholder="姓名和手机号" />
          </label>
          <label>
            <span>回调地址</span>
            <input v-model="draft.callbackUrl" placeholder="https://example.com/webhook" />
          </label>
          <label class="wide-field">
            <span>接入场景</span>
            <textarea v-model="draft.scenario" placeholder="说明要调用哪些能力，以及调用频率、数据用途和上线时间。" />
          </label>
        </div>

        <aside class="summary-panel">
          <span>申请摘要</span>
          <h3>{{ draft.appName || '待填写应用名称' }}</h3>
          <p>{{ draft.company || '待填写接入方' }}</p>
          <dl>
            <div>
              <dt>联系人</dt>
              <dd>{{ draft.contact || '未填写' }}</dd>
            </div>
            <div>
              <dt>回调地址</dt>
              <dd>{{ draft.callbackUrl || '未填写' }}</dd>
            </div>
            <div>
              <dt>资料准备</dt>
              <dd>{{ readyPercent }}%</dd>
            </div>
          </dl>
          <RouterLink class="primary-action" to="/login?redirect=/open/app">
            去控制台创建应用
            <el-icon><ArrowRight /></el-icon>
          </RouterLink>
        </aside>
      </div>
    </section>

    <section class="approval-section">
      <article v-for="step in approvalSteps" :key="step.title">
        <span>{{ step.no }}</span>
        <h3>{{ step.title }}</h3>
        <p>{{ step.desc }}</p>
      </article>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight, Connection, Finished, Lock, OfficeBuilding, User } from '@element-plus/icons-vue'
import OpenBrand from '../components/OpenBrand.vue'

const accessPaths = [
  {
    title: '商户自用应用',
    desc: '用于本租户会员、流程、门店申请等业务集成，测试 Key 通过后再申请生产 Key。',
    icon: OfficeBuilding,
  },
  {
    title: '服务商应用',
    desc: '为多个租户提供交付或运营服务，需要明确租户授权、数据范围和回调地址。',
    icon: Connection,
  },
  {
    title: '内部集成应用',
    desc: '用于集团内部系统打通，默认走最小权限和审计留痕。',
    icon: Lock,
  },
]

const checklist = [
  '应用名称和应用类型',
  '接入方主体和联系人',
  '需要调用的 API 能力',
  '测试环境回调地址',
  '数据用途和调用频率',
  '生产上线时间计划',
]

const checkedItems = ref<string[]>(['应用名称和应用类型'])

const readyPercent = computed(() => Math.round((checkedItems.value.length / checklist.length) * 100))

const draft = reactive({
  appName: '',
  company: '',
  contact: '',
  callbackUrl: '',
  scenario: '',
})

const approvalSteps = [
  {
    no: 'A',
    title: '提交应用',
    desc: '登录控制台创建开放应用，填写应用资料和回调地址。',
  },
  {
    no: 'B',
    title: '测试联调',
    desc: '使用测试 Key 调通签名、限流、Nonce 和核心接口。',
  },
  {
    no: 'C',
    title: '生产审批',
    desc: '确认租户授权、能力范围、字段边界和日配额后启用生产 Key。',
  },
]
</script>

<style scoped lang="scss">
.apply-page {
  --surface: #f8fbff;
  --surface-strong: #ffffff;
  --ink: #172033;
  --ink-soft: #667085;
  --accent: var(--theme-primary, #2c6975);
  --accent-2: var(--theme-primary-dark, #1e4d56);
  --line: rgba(var(--theme-primary-rgb, 44, 105, 117), 0.14);
  --shadow: 0 24px 80px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.14);

  position: relative;
  isolation: isolate;
  min-height: 100dvh;
  overflow-x: hidden;
  color: var(--ink);
  background:
    radial-gradient(circle at 12% 8%, rgba(var(--theme-primary-rgb, 44, 105, 117), 0.16), transparent 30%),
    radial-gradient(circle at 84% 16%, color-mix(in srgb, var(--theme-primary-light, #68b2a0) 16%, transparent), transparent 28%),
    linear-gradient(135deg, color-mix(in srgb, var(--theme-login-bg-start, #1a3d45) 10%, #f8fbff 90%) 0%, #eef4fb 52%, #f9fbff 100%);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.apply-page::before {
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

.apply-page > * {
  position: relative;
  z-index: 1;
}

.apply-nav {
  position: sticky;
  top: 0;
  z-index: 12;
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

.apply-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 420px);
  gap: clamp(28px, 5vw, 68px);
  align-items: end;
  padding: 88px clamp(20px, 5vw, 76px) 70px;
}

.kicker {
  margin: 0 0 16px;
  color: var(--accent-2);
  font-size: 13px;
  font-weight: 800;
}

.hero-copy {
  max-width: 820px;

  h1 {
    margin: 0 0 22px;
    font-size: clamp(44px, 6vw, 76px);
    font-weight: 820;
    line-height: 0.98;
    letter-spacing: 0;
  }

  p {
    max-width: 680px;
    margin: 0;
    color: var(--ink-soft);
    font-size: 18px;
    line-height: 1.8;
  }
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 34px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 46px;
  padding: 0 20px;
  border-radius: 999px;
  font-weight: 760;
  text-decoration: none;
}

.primary-action {
  color: #ffffff;
  background: var(--accent);
}

.secondary-action {
  color: var(--ink);
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.6);
}

.status-panel,
.path-card,
.checklist-card,
.summary-panel,
.approval-section article {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: var(--shadow);
}

.status-panel {
  padding: 28px;

  span {
    color: var(--accent-2);
    font-size: 13px;
    font-weight: 800;
  }

  strong {
    display: block;
    margin: 10px 0 14px;
    font-size: 32px;
    line-height: 1.1;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.8;
  }
}

.request-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(300px, 0.8fr);
  gap: 24px;
  padding: 0 clamp(20px, 5vw, 76px) 40px;
}

.path-card,
.checklist-card,
.form-section,
.approval-section {
  padding: 30px;
}

.section-title {
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
    font-weight: 820;
  }

  h2 {
    margin: 0;
    font-size: 26px;
    letter-spacing: 0;
  }
}

.path-list {
  display: grid;
  gap: 16px;
}

.path-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 16px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(247, 245, 239, 0.7);

  .el-icon {
    width: 42px;
    height: 42px;
    color: var(--accent);
    border-radius: 8px;
    background: rgba(22, 92, 74, 0.1);
    font-size: 22px;
  }

  h3 {
    margin: 0 0 8px;
    font-size: 18px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.7;
  }
}

.check-list {
  display: grid;
  gap: 12px;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  color: var(--ink);

  input {
    width: 18px;
    height: 18px;
    accent-color: var(--accent);
  }
}

.progress-line {
  height: 8px;
  margin-top: 24px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(21, 23, 21, 0.08);

  span {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, var(--accent), var(--accent-2));
    transition: width 0.2s ease;
  }
}

.progress-copy {
  margin: 12px 0 0;
  color: var(--ink-soft);
}

.form-section {
  margin: 0 clamp(20px, 5vw, 76px) 40px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.66);
}

.form-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.42fr);
  gap: 24px;
}

.form-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;

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
  textarea {
    width: 100%;
    min-height: 44px;
    padding: 0 14px;
    color: var(--ink);
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #ffffff;
    outline: none;
  }

  textarea {
    min-height: 116px;
    padding-top: 12px;
    resize: vertical;
    line-height: 1.7;
  }
}

.wide-field {
  grid-column: 1 / -1;
}

.summary-panel {
  padding: 24px;

  > span {
    color: var(--accent-2);
    font-size: 13px;
    font-weight: 820;
  }

  h3 {
    margin: 10px 0 8px;
    font-size: 28px;
  }

  p {
    margin: 0 0 20px;
    color: var(--ink-soft);
  }

  dl {
    display: grid;
    gap: 12px;
    margin: 0 0 24px;
  }

  div {
    display: grid;
    gap: 4px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--line);
  }

  dt {
    color: var(--ink-soft);
    font-size: 13px;
  }

  dd {
    margin: 0;
    word-break: break-word;
  }
}

.approval-section {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  padding: 0 clamp(20px, 5vw, 76px) 76px;

  article {
    padding: 24px;
    box-shadow: none;
  }

  span {
    display: inline-grid;
    width: 34px;
    height: 34px;
    place-items: center;
    color: #ffffff;
    border-radius: 50%;
    background: var(--accent-2);
    font-weight: 820;
  }

  h3 {
    margin: 18px 0 10px;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.7;
  }
}

@media (max-width: 980px) {
  .apply-hero,
  .request-grid,
  .form-layout,
  .approval-section {
    grid-template-columns: 1fr;
  }

  .form-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .apply-nav {
    align-items: flex-start;
    flex-direction: column;
    padding-top: 16px;
    padding-bottom: 16px;
  }

  .apply-hero {
    padding-top: 54px;
  }

  .hero-copy h1 {
    font-size: 42px;
  }

  .path-card,
  .checklist-card,
  .form-section {
    padding: 22px;
  }
}
</style>
