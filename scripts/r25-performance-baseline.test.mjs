import test from 'node:test'
import assert from 'node:assert/strict'
import { createR25PerformanceChecks, summarizeSamples } from './r25-performance-baseline.mjs'

test('createR25PerformanceChecks includes R25 critical endpoints', () => {
  const checks = createR25PerformanceChecks()
  const paths = checks.map(check => check.path)
  assert.ok(paths.includes('/prod-api/finance/predictive-ops/dashboard'))
  assert.ok(paths.includes('/prod-api/member/growth-action/dashboard'))
  assert.ok(paths.includes('/prod-api/open/app/list'))
})

test('summarizeSamples computes p95 and regression status', () => {
  const result = summarizeSamples({
    name: 'R24 predictive ops',
    thresholdMs: 100,
    samples: [10, 20, 30, 40, 50, 120]
  })
  assert.equal(result.p95Ms, 120)
  assert.equal(result.status, 'REGRESSION')
})
