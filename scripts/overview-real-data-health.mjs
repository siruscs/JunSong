#!/usr/bin/env node

/**
 * R7-F 概览真实性健康检查
 *
 * 防止系统/会员/财务概览页退回假数据（硬编码的假指标），确保三个概览页：
 *   - 调用真实后端 dashboard API（而非内置假数据数组）
 *   - 不包含硬编码的假指标（如 value: 1288, trend: '+12%' 这样的数组）
 *   - 包含错误状态处理（loadError / catch / v-if 错误显示）
 *
 * 规则中"当前尚未实现"的项标记为 WARNING 而非 FAIL，避免阻断回归。
 *
 * 用法:
 *   node scripts/overview-real-data-health.mjs
 */

import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const OVERVIEW_FILES = {
  finance: 'junsong-ui-v3/src/views/finance/overview/index.vue',
  member: 'junsong-ui-v3/src/views/member/overview/index.vue',
  system: 'junsong-ui-v3/src/views/system/overview/index.vue',
}

// 匹配硬编码的假指标数组，例如 { value: 1288, trend: '+12%' }
const FAKE_METRIC_PATTERN = /value\s*:\s*\d+,\s*trend\s*:\s*['"]\s*[+-]?\d+\s*%?['"]/

// 匹配错误状态处理：loadError / .catch( / v-if="...error" / el-alert ... type="error"
const ERROR_STATE_PATTERN = /loadError|\.catch\s*\(|v-if=.*error|el-alert.*error/i

function readOverview(name, relativePath, errors, cwd = process.cwd()) {
  const fullPath = path.join(cwd, relativePath)
  if (!fs.existsSync(fullPath)) {
    errors.push(`[${name}] overview file missing: ${relativePath}`)
    return null
  }
  return fs.readFileSync(fullPath, 'utf8')
}

/**
 * 执行三概览真实性检查。
 * @returns {{passed: boolean, errors: string[], warnings: string[]}}
 *          passed 仅在 errors 为空时为 true；warnings 不影响 passed。
 */
export function checkOverviewRealData({ cwd = process.cwd() } = {}) {
  const errors = []
  const warnings = []

  // === 财务概览 === R8-P0: 细化真实接口断言，防止后续重构把现金流/复盘任务拿掉
  const financeSrc = readOverview('finance', OVERVIEW_FILES.finance, errors, cwd)
  if (financeSrc) {
    const financeRequiredApis = [
      { pattern: /\/finance\/dashboard\/operation/, name: '/finance/dashboard/operation' },
      { pattern: /\/finance\/dashboard\/alerts/, name: '/finance/dashboard/alerts' },
      { pattern: /\/finance\/dashboard\/review-tasks/, name: '/finance/dashboard/review-tasks' },
      { pattern: /\/finance\/cashflow\/dashboard/, name: '/finance/cashflow/dashboard' },
    ]
    for (const api of financeRequiredApis) {
      if (!api.pattern.test(financeSrc)) {
        errors.push(`[finance] overview does not call ${api.name}`)
      }
    }
    if (FAKE_METRIC_PATTERN.test(financeSrc)) {
      errors.push('[finance] overview contains hardcoded fake metrics (value/trend literals)')
    }
    if (!ERROR_STATE_PATTERN.test(financeSrc)) {
      warnings.push(
        '[finance] overview missing error state handling (loadError / catch / v-if error)',
      )
    }

    // === R9-E: Weekly board checks (admin dashboard index.vue) ===
    const adminIndexPath = 'junsong-ui-v3/src/views/index.vue'
    const adminIndexFull = path.join(cwd, adminIndexPath)
    if (fs.existsSync(adminIndexFull)) {
      const adminIndexSrc = fs.readFileSync(adminIndexFull, 'utf8')
      if (!/getWeeklyReviewBoard|weekly-board/i.test(adminIndexSrc)) {
        warnings.push(
          '[finance] admin dashboard index.vue does not reference getWeeklyReviewBoard or weekly-board path',
        )
      }
      const weeklyVoFields = ['weekStart', 'weekEnd', 'salesChangeRate']
      const missingFields = weeklyVoFields.filter((f) => !adminIndexSrc.includes(f))
      if (missingFields.length > 0) {
        warnings.push(
          `[finance] admin dashboard index.vue missing WeeklyReviewBoardVO fields: ${missingFields.join(', ')}`,
        )
      }
    }
  }

  // === 会员概览 === R8-P0: 断言至少调用 3 类真实能力（stats/trend/operation/points-summary/report）
  const memberSrc = readOverview('member', OVERVIEW_FILES.member, errors, cwd)
  if (memberSrc) {
    const memberApiChecks = [
      { pattern: /\/member\/dashboard\/stats|getDashboardStats/i, name: '/member/dashboard/stats' },
      { pattern: /\/member\/dashboard\/trend|getDashboardTrend/i, name: '/member/dashboard/trend' },
      { pattern: /\/member\/dashboard\/points-summary|pointsOperation|pointsSummary/i, name: '/member/dashboard/points-summary' },
      { pattern: /\/member\/dashboard\/operation|getDashboardOperation/i, name: '/member/dashboard/operation' },
      { pattern: /\/member\/report\/contribution/, name: '/member/report/contribution' },
    ]
    let memberApiCount = 0
    const memberMissing = []
    for (const api of memberApiChecks) {
      if (api.pattern.test(memberSrc)) {
        memberApiCount++
      } else {
        memberMissing.push(api.name)
      }
    }
    if (memberApiCount < 3) {
      errors.push(
        `[member] overview calls only ${memberApiCount}/5 real APIs, need at least 3. Missing: ${memberMissing.join(', ')}`,
      )
    }
    // points-summary 不允许降级为 TODO
    if (!/points-summary|pointsOperation|pointsSummary/i.test(memberSrc)) {
      errors.push(
        '[member] overview does not call a points-summary API (expected /points-summary or pointsOperation)',
      )
    }
    if (FAKE_METRIC_PATTERN.test(memberSrc)) {
      errors.push('[member] overview contains hardcoded fake metrics (value/trend literals)')
    }
    if (!ERROR_STATE_PATTERN.test(memberSrc)) {
      warnings.push(
        '[member] overview missing error state handling (loadError / catch / v-if error)',
      )
    }
  }

  // === 系统概览 === R8-P0: 断言至少调用 stats/health/governance 三个真实接口
  const systemSrc = readOverview('system', OVERVIEW_FILES.system, errors, cwd)
  if (systemSrc) {
    const systemRequiredApis = [
      { pattern: /getDashboardStats|\/system\/dashboard\/stats/, name: 'getDashboardStats' },
      { pattern: /getDashboardHealth|\/system\/dashboard\/health/, name: 'getDashboardHealth' },
      { pattern: /getDashboardGovernance|\/system\/dashboard\/governance/, name: 'getDashboardGovernance' },
    ]
    for (const api of systemRequiredApis) {
      if (!api.pattern.test(systemSrc)) {
        errors.push(`[system] overview does not call ${api.name}`)
      }
    }
    if (FAKE_METRIC_PATTERN.test(systemSrc)) {
      errors.push('[system] overview contains hardcoded fake metrics (value/trend literals)')
    }
    if (!ERROR_STATE_PATTERN.test(systemSrc)) {
      warnings.push(
        '[system] overview missing error state handling (loadError / catch / v-if error)',
      )
    }
  }

  return {
    passed: errors.length === 0,
    errors,
    warnings,
  }
}

function isCliEntry() {
  return (
    process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  )
}

if (isCliEntry()) {
  const result = checkOverviewRealData()

  for (const w of result.warnings) {
    console.warn(`[overview-health] WARNING: ${w}`)
  }

  if (result.passed) {
    const warnSuffix = result.warnings.length
      ? ` (${result.warnings.length} warning(s))`
      : ''
    console.log(`[overview-health] PASS: overview real-data checks passed${warnSuffix}`)
    process.exit(0)
  }

  console.error(`[overview-health] FAIL: ${result.errors.length} error(s)`)
  for (const e of result.errors) {
    console.error(`  - ${e}`)
  }
  process.exit(1)
}

export default checkOverviewRealData
