<template>
  <main class="samples-page">
    <header class="samples-nav">
      <OpenBrand />
      <nav aria-label="接入样例导航">
        <RouterLink to="/open-platform">平台首页</RouterLink>
        <RouterLink to="/open-platform/docs">开发文档</RouterLink>
        <RouterLink to="/open-platform/debug">签名调试</RouterLink>
        <RouterLink to="/open-platform/apply">申请接入</RouterLink>
        <RouterLink to="/login?redirect=/open/app">控制台</RouterLink>
      </nav>
    </header>

    <section class="samples-hero">
      <div>
        <p class="kicker">INTEGRATION SAMPLES</p>
        <h1>接入样例与代码片段</h1>
        <p>这里放的是可复制的参考实现，帮助开发者快速完成签名、请求头、curl 调试和生产上线检查。</p>
      </div>
      <aside class="hero-panel">
        <span>建议路径</span>
        <strong>先调签名，再写 SDK</strong>
        <p>先用签名调试页验证材料，再把同样的签名规则封装进自己的服务端。</p>
        <RouterLink to="/open-platform/debug">打开签名调试</RouterLink>
      </aside>
    </section>

    <section class="quickstart-section">
      <div class="section-title">
        <span>01</span>
        <h2>最快接入路径</h2>
      </div>
      <div class="quick-grid">
        <article v-for="item in quickSteps" :key="item.title">
          <el-icon><component :is="item.icon" /></el-icon>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>

    <section class="code-section">
      <div class="section-title">
        <span>02</span>
        <h2>签名代码样例</h2>
      </div>
      <div class="tabs">
        <button
          v-for="sample in samples"
          :key="sample.key"
          type="button"
          :class="{ active: activeSample === sample.key }"
          @click="activeSample = sample.key"
        >
          {{ sample.label }}
        </button>
      </div>
      <article class="code-panel">
        <div class="code-head">
          <div>
            <h3>{{ currentSample.title }}</h3>
            <p>{{ currentSample.desc }}</p>
          </div>
          <button type="button" @click="copyText(currentSample.code)">复制代码</button>
        </div>
        <pre><code>{{ currentSample.code }}</code></pre>
      </article>
    </section>

    <section class="errors-section">
      <div class="section-title">
        <span>03</span>
        <h2>常见问题排查</h2>
      </div>
      <div class="error-grid">
        <article v-for="item in errorCards" :key="item.title">
          <span>{{ item.code }}</span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>

    <section class="release-section">
      <div>
        <h2>上线前检查</h2>
        <p>生产 Key 启用前，建议完成时间同步、Nonce 缓存、重试策略、错误日志脱敏和日配额预估。</p>
      </div>
      <RouterLink class="primary-action" to="/open-platform/apply">
        申请生产接入
        <el-icon><ArrowRight /></el-icon>
      </RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Clock, Connection, Document, Key, Lock, SetUp } from '@element-plus/icons-vue'
import OpenBrand from '../components/OpenBrand.vue'

const quickSteps = [
  {
    title: '创建应用',
    desc: '登录控制台创建开放应用，确认租户、联系人、回调地址和接入能力。',
    icon: Document,
  },
  {
    title: '获取测试 Key',
    desc: '用测试 Key 在低额度环境中验证签名、时间戳、Nonce 和接口响应。',
    icon: Key,
  },
  {
    title: '封装签名',
    desc: '在服务端封装 method、path、timestamp、nonce、body 的签名逻辑。',
    icon: SetUp,
  },
  {
    title: '申请生产',
    desc: '完成联调后提交生产审批，启用正式 Key、额度和能力范围。',
    icon: Connection,
  },
]

const samples = [
  {
    key: 'javascript',
    label: 'JavaScript',
    title: 'Node.js HMAC 签名',
    desc: '适合服务端 Node.js、BFF 或云函数场景。',
    code: `import crypto from 'node:crypto'

const appKey = process.env.GENESIS_APP_KEY
const appSecret = process.env.GENESIS_APP_SECRET
const method = 'GET'
const path = '/members'
const timestamp = Date.now().toString()
const nonce = crypto.randomBytes(12).toString('hex')
const body = ''

const signStr = method + path + timestamp + nonce + body
const signature = crypto
  .createHmac('sha256', appSecret)
  .update(signStr, 'utf8')
  .digest('hex')

const headers = {
  'X-App-Key': appKey,
  'X-App-Timestamp': timestamp,
  'X-App-Nonce': nonce,
  'X-App-Signature': signature,
}`,
  },
  {
    key: 'java',
    label: 'Java',
    title: 'Java HMAC 签名',
    desc: '适合后端服务、定时任务、企业集成服务。',
    code: `import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

String method = "GET";
String path = "/members";
String timestamp = String.valueOf(System.currentTimeMillis());
String nonce = UUID.randomUUID().toString().replace("-", "");
String body = "";
String signStr = method + path + timestamp + nonce + body;

Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
String signature = HexFormat.of().formatHex(mac.doFinal(signStr.getBytes(StandardCharsets.UTF_8)));`,
  },
  {
    key: 'curl',
    label: 'curl',
    title: 'curl 请求模板',
    desc: '适合联调时粘贴签名调试页生成的请求头。',
    code: `curl -X GET 'http://127.0.0.1/prod-api/openapi/v1/members' \\
  -H 'X-App-Key: your_app_key' \\
  -H 'X-App-Timestamp: 1790000000000' \\
  -H 'X-App-Nonce: nonce_123456' \\
  -H 'X-App-Signature: generated_signature'`,
  },
]

const errorCards = [
  {
    code: '401',
    title: '缺少认证头',
    desc: '检查四个请求头是否完整：AppKey、Timestamp、Nonce、Signature。',
  },
  {
    code: '401',
    title: '签名不匹配',
    desc: '确认 path、method、body 和发送请求时完全一致，body 不要二次格式化。',
  },
  {
    code: '401',
    title: '时间戳过期',
    desc: '服务端允许约 5 分钟时间差，生产服务器需要配置时间同步。',
  },
  {
    code: '429',
    title: '超过配额',
    desc: '测试 Key 默认低额度，生产访问需要申请正式额度和能力范围。',
  },
]

const activeSample = ref(samples[0].key)

const currentSample = computed(() => samples.find((sample) => sample.key === activeSample.value) || samples[0])

async function copyText(value: string) {
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('浏览器未允许复制，请手动选择文本')
  }
}
</script>

<style scoped lang="scss">
.samples-page {
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

.samples-page::before {
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

.samples-page > * {
  position: relative;
  z-index: 1;
}

.samples-nav {
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
    gap: 20px;
  }

  a {
    color: var(--ink-soft);
    text-decoration: none;
    white-space: nowrap;
  }
}

.samples-hero {
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
    max-width: 700px;
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

.hero-panel,
.quick-grid article,
.code-panel,
.error-grid article {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: var(--shadow);
}

.hero-panel {
  padding: 28px;

  span {
    color: var(--accent-2);
    font-size: 13px;
    font-weight: 850;
  }

  strong {
    display: block;
    margin: 10px 0 12px;
    font-size: 30px;
    line-height: 1.1;
  }

  a {
    display: inline-flex;
    min-height: 42px;
    align-items: center;
    margin-top: 22px;
    padding: 0 18px;
    color: #ffffff;
    border-radius: 999px;
    background: var(--accent);
    font-weight: 780;
    text-decoration: none;
  }
}

.quickstart-section,
.code-section,
.errors-section,
.release-section {
  padding: 0 clamp(20px, 5vw, 76px) 52px;
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
    font-weight: 850;
  }

  h2 {
    margin: 0;
    font-size: 28px;
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;

  article {
    min-height: 220px;
    padding: 24px;
    box-shadow: none;
  }

  .el-icon {
    color: var(--accent);
    font-size: 30px;
  }

  h3 {
    margin: 28px 0 10px;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.72;
  }
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;

  button {
    min-height: 38px;
    padding: 0 16px;
    color: var(--ink-soft);
    border: 1px solid var(--line);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.68);
    font-weight: 780;
    cursor: pointer;

    &.active {
      color: #ffffff;
      border-color: var(--accent);
      background: var(--accent);
    }
  }
}

.code-panel {
  overflow: hidden;
  box-shadow: none;
}

.code-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 22px;
  border-bottom: 1px solid var(--line);

  h3 {
    margin: 0 0 8px;
    font-size: 22px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
  }

  button {
    align-self: start;
    min-height: 38px;
    padding: 0 14px;
    color: var(--accent);
    border: 1px solid rgba(15, 107, 86, 0.28);
    border-radius: 999px;
    background: rgba(15, 107, 86, 0.08);
    font-weight: 780;
    cursor: pointer;
  }
}

pre {
  max-height: 500px;
  margin: 0;
  padding: 22px;
  overflow: auto;
  color: #eafff8;
  background: #111a17;
  font-size: 13px;
  line-height: 1.74;
}

.error-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;

  article {
    min-height: 190px;
    padding: 24px;
    box-shadow: none;
  }

  span {
    color: var(--accent-2);
    font-size: 13px;
    font-weight: 850;
  }

  h3 {
    margin: 18px 0 10px;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.72;
  }
}

.release-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 24px;
  padding-top: 56px;
  border-top: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.5);

  h2 {
    margin: 0 0 10px;
    font-size: clamp(30px, 4vw, 46px);
  }

  p {
    max-width: 780px;
    margin: 0;
    color: var(--ink-soft);
    line-height: 1.76;
  }
}

.primary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 46px;
  padding: 0 20px;
  color: #ffffff;
  border-radius: 999px;
  background: var(--accent);
  font-weight: 780;
  text-decoration: none;
}

@media (max-width: 1100px) {
  .samples-hero,
  .quick-grid,
  .error-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .samples-nav {
    align-items: flex-start;
    flex-direction: column;
    padding-top: 16px;
    padding-bottom: 16px;
  }

  .samples-hero,
  .quick-grid,
  .error-grid {
    grid-template-columns: 1fr;
  }

  .samples-hero {
    padding-top: 54px;

    h1 {
      font-size: 42px;
    }
  }

  .code-head {
    flex-direction: column;
  }
}
</style>
