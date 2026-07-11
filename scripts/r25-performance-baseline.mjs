export function createR25PerformanceChecks() {
  return [
    { name: 'system dashboard health', method: 'GET', path: '/prod-api/system/dashboard/health', thresholdMs: 1200, body: null },
    { name: 'finance operation overview', method: 'POST', path: '/prod-api/finance/overview/operation', thresholdMs: 1800, body: {} },
    { name: 'receivable collection dashboard', method: 'POST', path: '/prod-api/finance/receivable-collection/dashboard', thresholdMs: 1800, body: {} },
    { name: 'R24 predictive ops', method: 'POST', path: '/prod-api/finance/predictive-ops/dashboard', thresholdMs: 2200, body: { windowDays: 7 } },
    { name: 'member growth action dashboard', method: 'POST', path: '/prod-api/member/growth-action/dashboard', thresholdMs: 1800, body: {} },
    { name: 'open app list', method: 'GET', path: '/prod-api/open/app/list', thresholdMs: 1500, body: null }
  ]
}

export function summarizeSamples({ name, thresholdMs, samples }) {
  const sorted = [...samples].sort((a, b) => a - b)
  const index = Math.max(0, Math.ceil(sorted.length * 0.95) - 1)
  const p95Ms = sorted[index] || 0
  return {
    name,
    thresholdMs,
    samples,
    p95Ms,
    status: p95Ms <= thresholdMs ? 'PASS' : 'REGRESSION'
  }
}

async function runCheck(baseUrl, token, check) {
  const url = `${baseUrl}${check.path}`
  const headers = {
    'Content-Type': 'application/json',
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const samples = []
  const rounds = 5
  for (let i = 0; i < rounds; i++) {
    const start = performance.now()
    try {
      const res = await fetch(url, {
        method: check.method,
        headers,
        body: check.body ? JSON.stringify(check.body) : null,
      })
      const text = await res.text()
      if (!res.ok) {
        samples.push(99999)
        continue
      }
      try {
        const body = JSON.parse(text)
        if (body && body.code !== undefined && body.code !== 200) {
          samples.push(99999)
          continue
        }
      } catch (_) {
        // 非 JSON 响应（如健康检查纯文本），res.ok 已校验通过
      }
    } catch (e) {
      samples.push(99999)
      continue
    }
    samples.push(Math.round(performance.now() - start))
  }
  return summarizeSamples({ name: check.name, thresholdMs: check.thresholdMs, samples })
}

async function main() {
  const baseUrl = process.env.R25_BASE_URL || 'http://127.0.0.1'
  const token = process.env.R25_TOKEN
  const checks = createR25PerformanceChecks()

  if (!token) {
    console.log('R25_TOKEN not provided, printing checks only')
    console.log(JSON.stringify(checks, null, 2))
    process.exit(0)
  }

  console.log(`Running R25 performance baseline against ${baseUrl}...`)
  const results = []
  for (const check of checks) {
    const result = await runCheck(baseUrl, token, check)
    results.push(result)
    console.log(`  ${result.name}: p95=${result.p95Ms}ms threshold=${result.thresholdMs}ms status=${result.status}`)
  }
  console.log('\n' + JSON.stringify(results, null, 2))
  const hasRegression = results.some(r => r.status === 'REGRESSION')
  process.exit(hasRegression ? 1 : 0)
}

const isMain = process.argv[1] && import.meta.url.endsWith(process.argv[1].replace(/^[^.]/, ''))
if (isMain || process.argv[1]?.endsWith('r25-performance-baseline.mjs')) {
  main()
}
