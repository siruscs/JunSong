<template>
  <main class="module-overview">
    <section class="overview-hero">
      <div>
        <p class="kicker">{{ kicker }}</p>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
      <div class="hero-actions">
        <RouterLink v-for="action in primaryActions" :key="action.to" :to="action.to">
          {{ action.title }}
        </RouterLink>
      </div>
    </section>

    <section class="metric-grid" aria-label="模块关键指标">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <p>{{ metric.desc }}</p>
      </article>
    </section>

    <section class="overview-grid">
      <article class="panel flow-panel">
        <div class="panel-head">
          <span>WORKFLOW</span>
          <h2>{{ flowTitle }}</h2>
        </div>
        <div class="flow-list">
          <div v-for="(item, index) in flows" :key="item.title" class="flow-item">
            <em>{{ String(index + 1).padStart(2, '0') }}</em>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </article>

      <article class="panel governance-panel">
        <div class="panel-head">
          <span>GOVERNANCE</span>
          <h2>{{ governanceTitle }}</h2>
        </div>
        <div class="governance-list">
          <div v-for="item in governance" :key="item.title" class="governance-item">
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </article>
    </section>

    <section class="operations-grid" aria-label="模块运营状态">
      <article class="panel signal-panel">
        <div class="panel-head compact">
          <span>SIGNAL</span>
          <h2>状态信号</h2>
        </div>
        <div class="signal-list">
          <div v-for="signal in signals" :key="signal.title" class="signal-item" :data-level="signal.level">
            <div>
              <strong>{{ signal.title }}</strong>
              <p>{{ signal.desc }}</p>
            </div>
            <em>{{ signal.status }}</em>
          </div>
        </div>
      </article>

      <article class="panel watch-panel">
        <div class="panel-head compact">
          <span>WATCH</span>
          <h2>巡检清单</h2>
        </div>
        <ul class="check-list">
          <li v-for="item in watchlist" :key="item.title">
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </li>
        </ul>
      </article>

      <article class="panel cadence-panel">
        <div class="panel-head compact">
          <span>CADENCE</span>
          <h2>运营节奏</h2>
        </div>
        <div class="cadence-list">
          <div v-for="item in cadence" :key="item.title" class="cadence-item">
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </article>
    </section>

    <section class="link-section">
      <div class="section-head">
        <div>
          <span>ENTRY</span>
          <h2>常用入口</h2>
        </div>
        <p>保留原有业务页面，概览页只负责把最常用的管理动作聚合起来。</p>
      </div>
      <div class="link-grid">
        <RouterLink v-for="link in quickLinks" :key="link.to" :to="link.to" class="quick-link">
          <span>{{ link.group }}</span>
          <strong>{{ link.title }}</strong>
          <p>{{ link.desc }}</p>
        </RouterLink>
      </div>
    </section>

    <section class="next-section">
      <div>
        <span>NEXT</span>
        <h2>下一步演进</h2>
      </div>
      <ol>
        <li v-for="item in nextSteps" :key="item">{{ item }}</li>
      </ol>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

interface OverviewMetric {
  label: string
  value: string
  desc: string
}

interface OverviewTextItem {
  title: string
  desc: string
}

interface OverviewLink {
  group: string
  title: string
  desc: string
  to: string
  primary?: boolean
}

interface OverviewSignal {
  title: string
  desc: string
  status: string
  level: 'stable' | 'watch' | 'risk'
}

const props = defineProps<{
  kicker: string
  title: string
  description: string
  flowTitle: string
  governanceTitle: string
  metrics: OverviewMetric[]
  flows: OverviewTextItem[]
  governance: OverviewTextItem[]
  signals: OverviewSignal[]
  watchlist: OverviewTextItem[]
  cadence: OverviewTextItem[]
  quickLinks: OverviewLink[]
  nextSteps: string[]
}>()

const primaryActions = computed(() => {
  const selected = props.quickLinks.filter((item) => item.primary).slice(0, 3)
  return selected.length ? selected : props.quickLinks.slice(0, 3)
})
</script>

<style scoped lang="scss">
.module-overview {
  min-height: calc(100vh - 84px);
  padding: 22px;
  background: #f5f7fb;
  color: #18202f;
}

.overview-hero,
.metric-card,
.panel,
.link-section,
.next-section {
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  background: #ffffff;
}

.overview-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: end;
  padding: 24px;

  h1 {
    margin: 8px 0 10px;
    font-size: 26px;
    font-weight: 700;
    letter-spacing: 0;
  }

  p {
    max-width: 760px;
    margin: 0;
    color: #667085;
    font-size: 14px;
    line-height: 1.7;
  }
}

.kicker,
.panel-head span,
.section-head span,
.next-section span,
.quick-link span,
.metric-card span {
  color: #7a879c;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;

  a {
    display: inline-flex;
    align-items: center;
    min-height: 34px;
    padding: 0 14px;
    border: 1px solid #d7deea;
    border-radius: 6px;
    color: #24324b;
    font-size: 13px;
    font-weight: 650;
    text-decoration: none;
    background: #f8fafc;
  }
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.metric-card {
  padding: 18px;

  strong {
    display: block;
    margin-top: 10px;
    color: #111827;
    font-size: 28px;
    line-height: 1;
  }

  p {
    margin: 10px 0 0;
    color: #667085;
    font-size: 13px;
    line-height: 1.55;
  }
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(340px, 0.8fr);
  gap: 14px;
  margin-top: 14px;
}

.operations-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(280px, 0.95fr) minmax(260px, 0.75fr);
  gap: 14px;
  margin-top: 14px;
}

.panel {
  padding: 20px;
}

.panel-head,
.section-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 16px;

  h2 {
    margin: 6px 0 0;
    font-size: 18px;
    font-weight: 700;
  }

  &.compact {
    margin-bottom: 12px;
  }
}

.flow-list,
.governance-list,
.signal-list,
.cadence-list {
  display: grid;
  gap: 12px;
}

.flow-item,
.governance-item,
.signal-item,
.cadence-item,
.check-list li {
  border-radius: 8px;
  background: #f8fafc;
}

.flow-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 12px;
  padding: 14px;

  em {
    color: #335c67;
    font-style: normal;
    font-size: 13px;
    font-weight: 800;
  }
}

.flow-item strong,
.governance-item strong,
.quick-link strong,
.signal-item strong,
.cadence-item strong,
.check-list strong {
  color: #202939;
  font-size: 14px;
  font-weight: 700;
}

.flow-item p,
.governance-item p,
.quick-link p,
.section-head p,
.signal-item p,
.cadence-item p,
.check-list p {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.6;
}

.governance-item {
  padding: 14px;
  border-left: 3px solid #335c67;
}

.signal-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: flex-start;
  padding: 14px;

  em {
    display: inline-flex;
    align-items: center;
    min-height: 24px;
    padding: 0 9px;
    border-radius: 999px;
    font-style: normal;
    font-size: 12px;
    font-weight: 700;
    white-space: nowrap;
  }

  &[data-level='stable'] em {
    color: #25614a;
    background: #e8f5ef;
  }

  &[data-level='watch'] em {
    color: #7a4b00;
    background: #fff2d9;
  }

  &[data-level='risk'] em {
    color: #9b1c1c;
    background: #fde7e7;
  }
}

.check-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    position: relative;
    min-height: 86px;
    padding: 14px 14px 14px 34px;

    &::before {
      position: absolute;
      top: 18px;
      left: 14px;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #335c67;
      content: '';
    }
  }
}

.cadence-item {
  padding: 14px;

  strong {
    display: inline-flex;
    min-width: 48px;
    color: #335c67;
  }
}

.link-section,
.next-section {
  margin-top: 14px;
  padding: 20px;
}

.link-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.quick-link {
  min-height: 128px;
  padding: 15px;
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  color: inherit;
  text-decoration: none;
  background: #fbfcff;
  transition: border-color 0.16s ease, transform 0.16s ease;

  &:hover {
    border-color: #9cb2c7;
    transform: translateY(-1px);
  }
}

.next-section {
  display: grid;
  grid-template-columns: minmax(180px, 0.24fr) minmax(0, 0.76fr);
  gap: 24px;

  h2 {
    margin: 6px 0 0;
    font-size: 18px;
  }

  ol {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px 18px;
    margin: 0;
    padding-left: 20px;
  }

  li {
    color: #38445a;
    font-size: 14px;
    line-height: 1.6;
  }
}

@media (max-width: 1180px) {
  .link-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 920px) {
  .overview-hero,
  .overview-grid,
  .operations-grid,
  .next-section {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .module-overview {
    padding: 14px;
  }

  .link-grid,
  .next-section ol {
    grid-template-columns: 1fr;
  }
}
</style>
