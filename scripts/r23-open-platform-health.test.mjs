import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.existsSync(path) ? fs.readFileSync(path, 'utf8') : ''

test('R23 deliverables exist', () => {
  for (const path of [
    'sql/r23_open_platform_governance.sql',
    'scripts/r23-openapi-drift-check.mjs',
    'docs/open-platform/r23-api-inventory.zh-CN.md',
    'junsong-modules/junsong-open/src/main/java/com/junsong/open/context/OpenApiRequestContext.java',
    'junsong-modules/junsong-open/src/main/java/com/junsong/open/interceptor/OpenApiContextInterceptor.java',
    'docs/superpowers/plans/2026-07-04-r23-open-platform-external-integration-execution-report.zh-CN.md'
  ]) {
    assert.ok(fs.existsSync(path), `missing ${path}`)
  }
})

test('R23 gateway injects trusted open context headers', () => {
  const gateway = read('junsong-gateway/src/main/java/com/junsong/gateway/filter/ApiKeyAuthFilter.java')
  for (const header of [
    'X-Open-App-Id',
    'X-Open-App-Key',
    'X-Open-Tenant-Id',
    'X-Open-Key-Type',
    'X-Open-Request-Id',
    'X-Open-Auth-Version'
  ]) {
    assert.match(gateway, new RegExp(header), `missing gateway header ${header}`)
  }
  assert.match(gateway, /mutate\(\)[\s\S]*\.header\(/, 'gateway must mutate request headers before forwarding')
  assert.match(gateway, /TOO_MANY_REQUESTS|HttpStatus\.TOO_MANY_REQUESTS|429/, 'daily quota over-limit must return 429')
})

test('R23 open service no longer injects fixed admin identity', () => {
  const config = read('junsong-modules/junsong-open/src/main/java/com/junsong/open/config/OpenApiConfig.java')
  assert.doesNotMatch(config, /user_id"\s*,\s*"1"/, 'must not inject user_id=1')
  assert.doesNotMatch(config, /username"\s*,\s*"admin"/, 'must not inject admin username')
  assert.doesNotMatch(config, /user_key"\s*,\s*"openapi-internal"/, 'must not inject fixed openapi-internal user key')
  assert.match(config, /X-Open-App-Id|OpenApiRequestContextHolder/, 'must use trusted open context')
})

test('R23 internal secret response is minimal and gateway-parseable', () => {
  const controller = read('junsong-modules/junsong-open/src/main/java/com/junsong/open/controller/OpenInternalController.java')
  assert.match(controller, /appSecret/, 'internal response must include appSecret for gateway signing')
  assert.match(controller, /tenantId/, 'internal response must include tenantId')
  assert.match(controller, /appId/, 'internal response must include appId')
  assert.match(controller, /keyType/, 'internal response must include keyType')
  assert.doesNotMatch(controller, /return\s+AjaxResult\.success\(secret\)/, 'must not return whole OpenAppSecret object')
})

test('R23 open api log captures request identity and outcome', () => {
  const domain = read('junsong-modules/junsong-open/src/main/java/com/junsong/open/domain/OpenApiLog.java')
  const mapper = read('junsong-modules/junsong-open/src/main/resources/mapper/open/OpenApiLogMapper.xml')
  const corpus = `${domain}\n${mapper}`
  for (const field of ['requestId', 'errorCode', 'status', 'responseTime', 'tenantId', 'appId', 'appKey']) {
    assert.match(corpus, new RegExp(field), `missing log field ${field}`)
  }
})

// ── P1-2: 调用日志必须有真实写入链路 ──────────────────

test('R23 call log has a production write path (not just service definition)', () => {
  const interceptorDir = 'junsong-modules/junsong-open/src/main/java/com/junsong/open/interceptor'
  const files = fs.readdirSync(interceptorDir).map(f => read(`${interceptorDir}/${f}`)).join('\n')
  assert.match(
    files,
    /insertOpenApiLog|IOpenApiLogService/,
    'an interceptor/aspect must call insertOpenApiLog or IOpenApiLogService to persist calls'
  )
  assert.match(files, /afterCompletion|AfterReturning|@Around/, 'log write must hook into request lifecycle')
})

// ── P1-1: 下游服务必须信任 from-source: open-api ──────

test('R23 downstream permission bypass for trusted open-api source', () => {
  const aspect = read('junsong-common/junsong-common-security/src/main/java/com/junsong/common/security/aspect/PreAuthorizeAspect.java')
  assert.match(
    aspect,
    /from.source|FROM_SOURCE|open-api|OPEN_API/,
    'PreAuthorizeAspect must bypass @RequiresPermissions when from-source is open-api'
  )
})

test('R23 gateway strips spoofable internal headers from external requests', () => {
  const gateway = read('junsong-gateway/src/main/java/com/junsong/gateway/filter/ApiKeyAuthFilter.java')
  assert.match(
    gateway,
    /from-source|FROM_SOURCE/,
    'gateway must strip from-source header from external requests to prevent spoofing'
  )
})

// ── P1-3: 公共基础接口必须按租户隔离 ──────────────────

test('R23 foundation endpoints enforce tenant boundary', () => {
  const mapper = read('junsong-modules/junsong-open/src/main/resources/mapper/open/OpenAppMapper.xml')
  assert.match(
    mapper,
    /tenant_id\s*=\s*#\{tenantId\}/,
    'OpenAppMapper.selectOpenAppList must filter by tenant_id'
  )
  const controller = read('junsong-modules/junsong-open/src/main/java/com/junsong/open/controller/openapi/OpenFoundationController.java')
  assert.match(
    controller,
    /OpenApiRequestContextHolder|getTenantId/,
    'OpenFoundationController must use OpenApiRequestContextHolder to set tenantId'
  )
})

// ── P1-4: Webhook 订阅必须持久化 ──────────────────────

test('R23 webhook subscription is persisted (not fake success)', () => {
  const controller = read('junsong-modules/junsong-open/src/main/java/com/junsong/open/controller/openapi/OpenWebhookController.java')
  assert.doesNotMatch(
    controller,
    /TODO.*持久化|TODO.*persist/i,
    'webhook controller must not have TODO for persistence'
  )
  assert.match(
    controller,
    /Service|Mapper|Insert|insert/,
    'webhook controller must call a service/mapper to persist subscription'
  )
})

// ── P0: 内部接口不得公网暴露 ──────────────────────────

test('R23 internal endpoints are not in gateway public whitelist', () => {
  const bootstrap = read('junsong-gateway/src/main/resources/bootstrap.yml')
  const devNacos = read('docker/nacos/conf/junsong-gateway-dev.yml')
  for (const [label, content] of [['bootstrap.yml', bootstrap], ['junsong-gateway-dev.yml', devNacos]]) {
    const whitelistSection = content.split('whites:')[1] || ''
    assert.doesNotMatch(
      whitelistSection,
      /\/open\/internal\/\*\*/,
      `${label} must not whitelist /open/internal/** (AppSecret would be publicly exposed)`
    )
  }
})

test('R23 internal controller requires @InnerAuth (defense-in-depth)', () => {
  const controller = read('junsong-modules/junsong-open/src/main/java/com/junsong/open/controller/OpenInternalController.java')
  assert.match(
    controller,
    /@InnerAuth/,
    'OpenInternalController must use @InnerAuth to prevent unauthorized access even if whitelist is misconfigured'
  )
})

// ── 第三轮 P1: 部署边界安全（9208 不得发布到宿主机/公网）─────────

test('R23 open service 9208 port is not published to host in docker-compose', () => {
  const compose = read('docker/docker-compose.yml')
  // 精确匹配服务定义行（冒号后紧跟换行，避免匹配注释中的 junsong-modules-open:9208）
  const openServiceMatch = compose.match(/\n  junsong-modules-open:\n/)
  assert.ok(openServiceMatch, 'docker-compose.yml must define junsong-modules-open service')
  const openServiceStart = openServiceMatch.index
  // 截取从 junsong-modules-open 到下一个顶层服务（两个空格缩进的服务定义）或文件末尾
  const afterOpen = compose.slice(openServiceStart)
  // 找到下一个顶层 service（行首两个空格缩进的服务名）
  const nextServiceMatch = afterOpen.slice(1).match(/\n  [a-z][\w-]*:\n/)
  const openSection = nextServiceMatch ? afterOpen.slice(0, nextServiceMatch.index + 1) : afterOpen

  // 9208 端口不得通过 ports 发布到宿主机
  assert.doesNotMatch(
    openSection,
    /ports:\s*\n\s*-\s*["']?9208/,
    'junsong-modules-open must NOT publish 9208 via ports (use expose for Docker-internal only)'
  )
  // 必须使用 expose 声明 9208 仅内网可访问
  assert.match(
    openSection,
    /expose:\s*\n\s*-\s*["']?9208/,
    'junsong-modules-open must use expose (not ports) for 9208 to keep it Docker-internal'
  )
})

test('R23 deploy-open.sh does not publish 9208 to host', () => {
  const script = read('bin/deploy-open.sh')
  assert.doesNotMatch(
    script,
    /-p\s+9208:9208/,
    'deploy-open.sh must not use -p 9208:9208 (port must not be published to host)'
  )
})

test('R23 internal controller validates non-spoofable X-Inner-Token (defense-in-depth)', () => {
  const controller = read('junsong-modules/junsong-open/src/main/java/com/junsong/open/controller/OpenInternalController.java')
  // 必须读取 open.internal.secret 配置
  assert.match(
    controller,
    /open\.internal\.secret/,
    'OpenInternalController must read open.internal.secret config (non-spoofable service-to-service token)'
  )
  // 必须校验 X-Inner-Token 头
  assert.match(
    controller,
    /X-Inner-Token/,
    'OpenInternalController must validate X-Inner-Token header (not just from-source which is spoofable)'
  )
  // 必须在密钥未配置时 fail closed
  assert.match(
    controller,
    /fail closed|isEmpty\(innerToken\)|isEmpty\(.*token.*\)/i,
    'OpenInternalController must fail closed when open.internal.secret is empty (prevent misconfigured deployment)'
  )
})

test('R23 gateway sends X-Inner-Token when calling open internal endpoints', () => {
  const gateway = read('junsong-gateway/src/main/java/com/junsong/gateway/filter/ApiKeyAuthFilter.java')
  assert.match(
    gateway,
    /open\.internal\.secret/,
    'ApiKeyAuthFilter must read open.internal.secret config to send X-Inner-Token'
  )
  // 网关定义 X-Inner-Token 常量并在两处 WebClient 调用中引用
  assert.match(
    gateway,
    /HEADER_INNER_TOKEN\s*=\s*"X-Inner-Token"/,
    'ApiKeyAuthFilter must define HEADER_INNER_TOKEN constant for X-Inner-Token header'
  )
  // fetchAuthContext 方法体必须引用 HEADER_INNER_TOKEN
  const fetchAuthMethodIdx = gateway.indexOf('private Mono<AuthContext> fetchAuthContext')
  assert.ok(fetchAuthMethodIdx !== -1, 'fetchAuthContext method must exist')
  const fetchAuthSection = gateway.slice(fetchAuthMethodIdx)
  assert.match(
    fetchAuthSection,
    /HEADER_INNER_TOKEN/,
    'fetchAuthContext must include HEADER_INNER_TOKEN header'
  )
  // logGatewayRejection 方法体必须引用 HEADER_INNER_TOKEN
  const logRejectMethodIdx = gateway.indexOf('private void logGatewayRejection')
  assert.ok(logRejectMethodIdx !== -1, 'logGatewayRejection method must exist')
  const logRejectSection = gateway.slice(logRejectMethodIdx)
  assert.match(
    logRejectSection,
    /HEADER_INNER_TOKEN/,
    'logGatewayRejection must include HEADER_INNER_TOKEN header'
  )
})

test('R23 docker-compose injects OPEN_INTERNAL_SECRET into gateway and open service', () => {
  const compose = read('docker/docker-compose.yml')
  // gateway 和 open 服务都必须注入 OPEN_INTERNAL_SECRET
  const gatewaySection = compose.slice(compose.indexOf('junsong-gateway:'), compose.indexOf('junsong-auth:'))
  assert.match(
    gatewaySection,
    /OPEN_INTERNAL_SECRET/,
    'junsong-gateway must have OPEN_INTERNAL_SECRET env var to send X-Inner-Token'
  )
  const openSection = compose.slice(compose.indexOf('junsong-modules-open:'))
  assert.match(
    openSection,
    /OPEN_INTERNAL_SECRET/,
    'junsong-modules-open must have OPEN_INTERNAL_SECRET env var to validate X-Inner-Token'
  )
})

// ── 第四轮 P1/P2: PROD compose 部署边界 ──────────────

test('R23 prod compose requires OPEN_INTERNAL_SECRET (no public default)', () => {
  const prod = read('docker/docker-compose.prod.yml')
  assert.doesNotMatch(
    prod,
    /prod-inner-secret-change-me/,
    'docker-compose.prod.yml must NOT contain public default secret prod-inner-secret-change-me (X-Inner-Token would be publicly guessable)'
  )
  // 禁止 :- 默认值语法（除 NACOS_PASSWORD 等已有规则外，OPEN_INTERNAL_SECRET 必须用 :? 强制必填）
  const secretLines = prod.split('\n').filter(l => l.includes('OPEN_INTERNAL_SECRET'))
  for (const line of secretLines) {
    assert.doesNotMatch(
      line,
      /OPEN_INTERNAL_SECRET\$\{OPEN_INTERNAL_SECRET:-/,
      `OPEN_INTERNAL_SECRET in prod compose must use \${VAR:?msg} not \${VAR:-default}: ${line.trim()}`
    )
  }
  assert.ok(secretLines.length >= 3, 'prod compose must inject OPEN_INTERNAL_SECRET into all 3 services (gateway, finance, open)')
})

test('R23 prod compose open service uses expose (not ports) for 9208', () => {
  const prod = read('docker/docker-compose.prod.yml')
  const openServiceMatch = prod.match(/\n  junsong-modules-open:\n/)
  assert.ok(openServiceMatch, 'docker-compose.prod.yml must define junsong-modules-open service')
  const openServiceStart = openServiceMatch.index
  const afterOpen = prod.slice(openServiceStart)
  const nextServiceMatch = afterOpen.slice(1).match(/\n  [a-z][\w-]*:\n/)
  const openSection = nextServiceMatch ? afterOpen.slice(0, nextServiceMatch.index + 1) : afterOpen

  assert.doesNotMatch(
    openSection,
    /ports:\s*\n\s*-\s*["']?9208/,
    'prod junsong-modules-open must NOT publish 9208 via ports (Docker-internal only)'
  )
  assert.match(
    openSection,
    /expose:\s*\n\s*-\s*["']?9208/,
    'prod junsong-modules-open must use expose for 9208 (Docker-internal only)'
  )
})

test('R23 prod compose gateway links to junsong-modules-open (Docker-internal access)', () => {
  const prod = read('docker/docker-compose.prod.yml')
  const gatewaySection = prod.slice(prod.indexOf('junsong-gateway:'), prod.indexOf('junsong-auth:'))
  assert.match(
    gatewaySection,
    /-\s*junsong-modules-open/,
    'prod junsong-gateway must link to junsong-modules-open to call internal endpoint via Docker DNS'
  )
})

test('R23 contract drift check is registered and executable', () => {
  const admin = read('scripts/admin-health.mjs')
  assert.match(admin, /r23-open-platform-health\.test\.mjs/, 'R23 health must be registered in admin-health')
  assert.match(admin, /r23-openapi-drift-check\.mjs/, 'R23 drift check must be registered in admin-health')
})

test('R23 does not implement R24 R25 scopes', () => {
  const corpus = [
    read('scripts/r23-open-platform-health.test.mjs'),
    read('docs/superpowers/plans/2026-07-04-r23-open-platform-external-integration-execution-report.zh-CN.md'),
    read('junsong-gateway/src/main/java/com/junsong/gateway/filter/ApiKeyAuthFilter.java'),
    read('junsong-modules/junsong-open/src/main/java/com/junsong/open/config/OpenApiConfig.java')
  ].join('\n')
  const forbidden = [
    new RegExp(['prediction', 'v2'].join('\\s*'), 'i'),
    new RegExp(['what', 'if'].join('-'), 'i'),
    new RegExp(['machine', 'learning'].join('\\s*'), 'i'),
    new RegExp(['load', 'test', 'platform'].join('\\s*'), 'i'),
    new RegExp(['archive', 'platform'].join('\\s*'), 'i'),
    new RegExp(['billing', 'engine'].join('\\s*'), 'i'),
  ]
  for (const pattern of forbidden) {
    assert.doesNotMatch(corpus, pattern, `forbidden future scope leaked: ${pattern}`)
  }
})
