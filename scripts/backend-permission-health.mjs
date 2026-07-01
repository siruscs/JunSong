#!/usr/bin/env node

/**
 * 后端接口权限扫描工具
 *
 * 扫描 junsong-modules 下所有 Controller 文件，
 * 检测缺少 @RequiresPermissions 注解的公开接口。
 *
 * 用法:
 *   node scripts/backend-permission-health.mjs          # 文本报告
 *   node scripts/backend-permission-health.mjs --json    # JSON 输出
 */

import { readFileSync, readdirSync, statSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const ROOT = path.resolve(__dirname, '..')

// ========================== Configuration ==========================

const HTTP_MAPPING_ANNOTATIONS = [
  '@GetMapping',
  '@PostMapping',
  '@PutMapping',
  '@DeleteMapping',
  '@PatchMapping',
  '@RequestMapping', // method-level without explicit HTTP method
]

/**
 * Whitelist: endpoints that are intentionally public (no @RequiresPermissions).
 *
 * Patterns are matched against "ClassName.methodName".
 * Supports wildcard: '*.feignXxx' or 'SysLoginController.*'.
 */
const WHITELIST_PATTERNS = [
  // ========== Authentication / Captcha ==========
  'SysLoginController.*',
  'SysRegisterController.*',
  'CaptchaController.*',

  // ========== Open platform (gateway-level API key auth, not @RequiresPermissions) ==========
  'OpenApiController.*',
  'OpenMpController.*',
  'OpenInternalController.*',
  'OpenAppController.*',
  'OpenMemberController.*',
  'OpenStoreOpeningController.*',
  'OpenWorkflowController.*',
  'OpenFoundationController.*',
  'OpenWebhookController.*',

  // ========== Workflow module (uses Spring Security @PreAuthorize, not custom annotation) ==========
  'HistoryController.*',
  'MobileWorkflowController.*',
  'ProcessDefinitionController.*',
  'ProcessInstanceController.*',
  'TaskController.*',
  'WorkflowAnalyticsController.*',
  'WorkflowFieldPermissionController.*',
  'WorkflowHealthController.*',
  'WorkflowInterveneController.*',
  'WorkflowTimeoutController.*',
  'WorkflowVersionController.*',

  // ========== Lowcode module (dynamic business controllers) ==========
  'LcBizController.*',
  'LcBizExcelController.*',
  'LcBizImportExportController.*',
  'LcMetadataController.*',

  // ========== User self-service (logged-in users access own data) ==========
  'SysProfileController.*',
  'SysNotificationController.*',
  'SysUserController.deptsForLogin',
  'SysNoticeController.getInfo',
  'SysNoticeController.listTop',
  'SysNoticeController.markRead',
  'SysNoticeController.markReadAll',
  'SysUserController.getInfo',
  'SysUserController.switchDept',

  // ========== Mini-program API (MP token auth, not admin @RequiresPermissions) ==========
  'MemMpController.*',

  // ========== Intentionally public utility endpoints (form selects, tree data) ==========
  'SysDeptController.treeselect',
  'SysDeptController.roleDeptTreeselect',
  'SysDictDataController.getType',
  'SysDictTypeController.optionselect',
  'SysMenuController.treeselect',
  'SysMenuController.roleMenuTreeselect',
  'SysMenuController.getRouters',
  'SysPostController.optionselect',
  'SysRegionController.*',
  'MemPointsRuleController.getEffectiveRule',

  // ========== ISV self-registration (public) ==========
  'OpenIsvController.register',

  // ========== Store opening public query ==========
  'SysStoreOpeningController.getByOrderNo',

  // ========== Member self-service ==========
  'MemRefundApplyController.getByRefundNo',

  // ========== File serving ==========
  '*.download*',
  '*.getResource*',

  // ========== Internal Feign / Health ==========
  '*.feignRoute*',
  '*.internal*',
  '*.getByAppKey*',
  '*.health*',
  '*.info*',

  // ========== Wechat callback ==========
  '*.notify*',
  '*.callback*',
]

/**
 * Check whether a specific endpoint is whitelisted (intentionally public).
 * Exported for unit testing — scanner logic must reuse this function.
 *
 * @param {string} className  - Controller class name (e.g. 'OpenIsvController')
 * @param {string} methodName - Method name (e.g. 'register')
 * @returns {boolean}
 */
export function isEndpointWhitelisted(className, methodName) {
  const fullName = `${className}.${methodName}`
  return WHITELIST_PATTERNS.some(pattern => {
    if (pattern.includes('*')) {
      const escaped = pattern.replace(/\./g, '\\.').replace(/\*/g, '.*')
      return new RegExp(`^${escaped}$`).test(fullName)
    }
    return pattern === fullName
  })
}

// ========================== File Scanning ==========================

function findControllerFiles(dir) {
  const results = []
  let entries
  try {
    entries = readdirSync(dir)
  } catch {
    return results
  }
  for (const entry of entries) {
    const full = path.join(dir, entry)
    try {
      const st = statSync(full)
      if (st.isDirectory() && !entry.startsWith('.')) {
        results.push(...findControllerFiles(full))
      } else if (entry.endsWith('Controller.java')) {
        results.push(full)
      }
    } catch {
      // skip inaccessible
    }
  }
  return results
}

// ========================== Parsing Helpers ==========================

function extractStringValues(raw) {
  return [...raw.matchAll(/"([^"]*)"/g)].map(m => m[1])
}

function extractBasePath(line) {
  const m = line.match(/@RequestMapping\s*\(([^)]*)\)/)
  if (!m) return ''
  const values = extractStringValues(m[1])
  return values[0] || ''
}

/**
 * Given the lines array and an index where a HTTP mapping annotation is,
 * look backwards to collect the annotation block for that method.
 * Stops at blank lines, closing braces, or method body lines.
 */
function getAnnotationBlock(lines, mappingIndex) {
  const block = []
  for (let i = mappingIndex - 1; i >= Math.max(0, mappingIndex - 12); i--) {
    const t = lines[i].trim()
    if (t === '' || t === '}' || t.startsWith('//') || t.startsWith('*') || t.startsWith('/*')) break
    if (!t.startsWith('@') && !t.startsWith('*') && !t.endsWith(')') && !t.endsWith(',') && !t.endsWith('(')) break
    block.unshift(lines[i])
  }
  return block
}

/**
 * Look forward from the HTTP mapping annotation to collect annotations
 * placed between the mapping and the method declaration.
 * Some controllers use: @GetMapping → @RequiresPermissions → public ReturnType method()
 */
function getForwardAnnotationBlock(lines, mappingIndex) {
  const block = []
  for (let i = mappingIndex + 1; i < Math.min(lines.length, mappingIndex + 8); i++) {
    const t = lines[i].trim()
    if (t === '' || t === '}' || t.startsWith('//') || t.startsWith('*') || t.startsWith('/*')) break
    // Stop at method declaration
    if (/^\s*(?:public|private|protected)\s/.test(t)) break
    if (t.startsWith('@')) {
      block.push(lines[i])
    } else if (t.endsWith(')') || t.endsWith(',') || t.endsWith('(')) {
      // continuation of a multi-line annotation argument
      block.push(lines[i])
    }
  }
  return block
}

// ========================== Core Scanner ==========================

/**
 * P0 目标控制器：第一批必须全部受保护的控制器。
 * --targeted 模式下仅扫描这些控制器。
 */
const TARGETED_CONTROLLERS = [
  'WebhookSubscriptionController',
  'SysConfigController',
  'FinanceReportController',
  'ExpenseOcrController',
  'MemberReportController',
]

export function scanControllerFile(filePath) {
  let content
  try {
    content = readFileSync(filePath, 'utf8')
  } catch {
    return []
  }

  const lines = content.split('\n')
  const relPath = path.relative(ROOT, filePath)
  const endpoints = []

  // --- Extract class-level info ---
  let className = ''
  let classBasePath = ''
  let classLineIndex = -1

  for (let i = 0; i < lines.length; i++) {
    const trimmed = lines[i].trim()

    if (!className) {
      const cm = trimmed.match(/(?:public\s+)?class\s+(\w+)/)
      if (cm) className = cm[1]
    }

    if (trimmed.startsWith('@RequestMapping') && classLineIndex === -1) {
      classBasePath = extractBasePath(trimmed)
      classLineIndex = i
    }

    if (className && classLineIndex >= 0) break
  }

  if (!className) return []

  // --- Scan for endpoint methods (only after class declaration) ---
  const startFrom = classLineIndex >= 0 ? classLineIndex + 1 : 0

  for (let i = startFrom; i < lines.length; i++) {
    const trimmed = lines[i].trim()

    // Skip comments
    if (trimmed.startsWith('//') || trimmed.startsWith('*') || trimmed.startsWith('/*')) continue

    // Check if this line has an HTTP mapping annotation
    let isHttpMapping = false
    for (const ann of HTTP_MAPPING_ANNOTATIONS) {
      if (trimmed.startsWith(ann + '(') || trimmed.startsWith(ann + ' ') || trimmed === ann) {
        isHttpMapping = true
        break
      }
    }

    if (!isHttpMapping) continue

    // Extract endpoint path from the mapping annotation
    const endpointPath = extractStringValues(trimmed)[0] || ''

    // Get annotation block preceding this mapping
    const block = getAnnotationBlock(lines, i)
    // Also collect annotations between mapping and method declaration
    const forwardBlock = getForwardAnnotationBlock(lines, i)
    const blockText = [...block, ...forwardBlock].join('\n')

    // Check for @InnerAuth (internal Feign endpoint)
    const hasInnerAuth = /@InnerAuth/.test(blockText)

    // Extract @RequiresPermissions values
    let permissions = []
    const permMatch = blockText.match(/@RequiresPermissions\s*\(\s*(?:value\s*=\s*)?([^)]+)\)/)
    if (permMatch) {
      permissions = extractStringValues(permMatch[1])
    }

    // Check for logical operator
    const hasOrLogical = /logical\s*=\s*Logical\.OR/.test(blockText)

    // Build full path
    const fullPath = classBasePath + endpointPath

    // Find method name (look forward from mapping annotation)
    let methodName = 'unknown'
    for (let j = i + 1; j < Math.min(lines.length, i + 6); j++) {
      const mm = lines[j].match(/public\s+\S+(?:<[^>]+>)?\s+(\w+)\s*\(/)
      if (mm) {
        methodName = mm[1]
        break
      }
    }

    // Check whitelist (reuse exported function)
    const isWhitelisted = isEndpointWhitelisted(className, methodName)

    endpoints.push({
      file: relPath,
      controller: className,
      method: methodName,
      httpMapping: trimmed.split(/\s*\(/)[0].replace('@', ''),
      fullPath,
      line: i + 1,
      permissions,
      hasPermission: permissions.length > 0,
      hasInnerAuth,
      hasOrLogical,
      isWhitelisted,
    })
  }

  return endpoints
}

export function scanAllControllers(modulesDir) {
  const files = findControllerFiles(modulesDir)
  return files.flatMap(scanControllerFile)
}

// ========================== Main ==========================

export function runPermissionHealthCheck(modulesDir, { targeted = false } = {}) {
  const allEndpoints = scanAllControllers(modulesDir)
  const scoped = targeted
    ? allEndpoints.filter(e => TARGETED_CONTROLLERS.includes(e.controller))
    : allEndpoints
  const missing = scoped.filter(
    e => !e.hasPermission && !e.isWhitelisted && !e.hasInnerAuth
  )
  return { allEndpoints: scoped, missing }
}

function main() {
  const jsonMode = process.argv.includes('--json')
  const targetedMode = process.argv.includes('--targeted')
  const modulesDir = path.join(ROOT, 'junsong-modules')

  const { allEndpoints, missing } = runPermissionHealthCheck(modulesDir, { targeted: targetedMode })

  if (jsonMode) {
    const report = {
      ok: missing.length === 0,
      totalEndpoints: allEndpoints.length,
      missingCount: missing.length,
      missing: missing.map(m => ({
        controller: m.controller,
        method: m.method,
        path: m.fullPath,
        httpMapping: m.httpMapping,
        file: `${m.file}:${m.line}`,
      })),
    }
    console.log(JSON.stringify(report, null, 2))
    process.exit(report.ok ? 0 : 1)
  }

  // Text report
  console.log('[permission-health] Scanning controllers...\n')
  console.log(`Total endpoints found: ${allEndpoints.length}`)
  console.log(`Missing @RequiresPermissions: ${missing.length}`)

  if (missing.length > 0) {
    console.log('\n[FAIL] The following endpoints lack @RequiresPermissions:\n')
    const grouped = {}
    for (const m of missing) {
      if (!grouped[m.controller]) grouped[m.controller] = []
      grouped[m.controller].push(m)
    }
    for (const [ctrl, methods] of Object.entries(grouped)) {
      console.log(`  ${ctrl}:`)
      for (const m of methods) {
        console.log(`    ${m.httpMapping} ${m.fullPath}  (${m.file}:${m.line})`)
      }
      console.log('')
    }
    process.exit(1)
  }

  console.log('\n[PASS] All scanned endpoints have @RequiresPermissions or are whitelisted.')
  process.exit(0)
}

function isCliEntry() {
  return process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
}

if (isCliEntry()) {
  main()
}
