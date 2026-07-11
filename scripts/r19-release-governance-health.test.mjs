import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('R19 required closure inputs exist', () => {
  read('docs/superpowers/plans/2026-07-03-r19-plus-optimization-roadmap.zh-CN.md')
  read('docs/superpowers/plans/2026-07-03-r18-r1-r18-closure-execution-report.zh-CN.md')
  read('docs/superpowers/plans/2026-07-03-r1-r18-release-inventory.zh-CN.md')
  read('docs/superpowers/plans/2026-07-03-r1-r18-risk-and-backlog.zh-CN.md')
  read('scripts/r18-r1-r18-closure-health.test.mjs')
})

test('R19 deliverables exist', () => {
  read('docs/superpowers/plans/2026-07-03-r19-release-governance-checklist.zh-CN.md')
  read('docs/superpowers/plans/2026-07-03-r19-backlog-decision.zh-CN.md')
  read('docs/superpowers/plans/2026-07-03-r19-release-governance-execution-report.zh-CN.md')
  read('scripts/r19-release-report.mjs')
})

test('R19 stays on release governance mainline', () => {
  const plan = read('docs/superpowers/plans/2026-07-03-next-phase-r19-release-governance-task-list.zh-CN.md')
  assert.match(plan, /不新增 R20 指标字典/)
  assert.match(plan, /不新增 R21 定时任务/)
  assert.match(plan, /不新增 R22 动作中心/)
  assert.match(plan, /不新增 R23 开放平台/)
  assert.match(plan, /偏航判定/)
})

test('R19 checklist covers DEV PROD SQL rollback and smoke', () => {
  const checklist = read('docs/superpowers/plans/2026-07-03-r19-release-governance-checklist.zh-CN.md')
  assert.match(checklist, /DEV 发布前检查/)
  assert.match(checklist, /PROD 发布前检查/)
  assert.match(checklist, /SQL 执行顺序/)
  assert.match(checklist, /回滚/)
  assert.match(checklist, /冒烟/)
})

test('R19 backlog decision table separates do defer and reject', () => {
  const decision = read('docs/superpowers/plans/2026-07-03-r19-backlog-decision.zh-CN.md')
  assert.match(decision, /DO_R19/)
  assert.match(decision, /DEFER_R20_PLUS/)
  assert.match(decision, /REJECT/)
  assert.match(decision, /体验优化/)
  assert.match(decision, /性能增强/)
})

test('R19 report includes Codex review request', () => {
  const report = read('docs/superpowers/plans/2026-07-03-r19-release-governance-execution-report.zh-CN.md')
  assert.match(report, /Codex 复核请求/)
  assert.match(report, /主线偏航检查/)
  assert.match(report, /发布治理/)
  assert.match(report, /Backlog 决策/)
})
