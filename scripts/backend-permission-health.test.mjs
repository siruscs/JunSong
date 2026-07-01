import test from 'node:test'
import assert from 'node:assert/strict'
import { isEndpointWhitelisted } from './backend-permission-health.mjs'

// ========== Management controllers must NOT be whole-class whitelisted ==========

test('OpenContractController management endpoints are not whitelisted as whole controller', () => {
  assert.equal(isEndpointWhitelisted('OpenContractController', 'add'), false)
  assert.equal(isEndpointWhitelisted('OpenContractController', 'edit'), false)
  assert.equal(isEndpointWhitelisted('OpenContractController', 'remove'), false)
  assert.equal(isEndpointWhitelisted('OpenContractController', 'list'), false)
  assert.equal(isEndpointWhitelisted('OpenContractController', 'getInfo'), false)
  assert.equal(isEndpointWhitelisted('OpenContractController', 'activate'), false)
  assert.equal(isEndpointWhitelisted('OpenContractController', 'terminate'), false)
})

test('OpenIsvController management endpoints are not whitelisted as whole controller', () => {
  assert.equal(isEndpointWhitelisted('OpenIsvController', 'list'), false)
  assert.equal(isEndpointWhitelisted('OpenIsvController', 'getInfo'), false)
  assert.equal(isEndpointWhitelisted('OpenIsvController', 'add'), false)
  assert.equal(isEndpointWhitelisted('OpenIsvController', 'edit'), false)
  assert.equal(isEndpointWhitelisted('OpenIsvController', 'approve'), false)
  assert.equal(isEndpointWhitelisted('OpenIsvController', 'reject'), false)
  assert.equal(isEndpointWhitelisted('OpenIsvController', 'remove'), false)
})

test('OpenApiLogController management endpoints are not whitelisted as whole controller', () => {
  assert.equal(isEndpointWhitelisted('OpenApiLogController', 'list'), false)
  assert.equal(isEndpointWhitelisted('OpenApiLogController', 'export'), false)
})

test('LcReportController endpoints are not whitelisted as whole controller', () => {
  assert.equal(isEndpointWhitelisted('LcReportController', 'dataList'), false)
  assert.equal(isEndpointWhitelisted('LcReportController', 'statistics'), false)
  assert.equal(isEndpointWhitelisted('LcReportController', 'columns'), false)
})

// ========== Legitimate public endpoints remain whitelisted ==========

test('public ISV self-registration remains whitelisted', () => {
  assert.equal(isEndpointWhitelisted('OpenIsvController', 'register'), true)
})

// ========== Gateway-level API key auth controllers remain whitelisted ==========

test('open platform gateway-auth controllers remain whitelisted', () => {
  assert.equal(isEndpointWhitelisted('OpenApiController', 'invoke'), true)
  assert.equal(isEndpointWhitelisted('OpenMpController', 'query'), true)
  assert.equal(isEndpointWhitelisted('OpenInternalController', 'sync'), true)
  assert.equal(isEndpointWhitelisted('OpenWebhookController', 'deliver'), true)
})

// ========== Auth / captcha / utility remain whitelisted ==========

test('auth and captcha controllers remain whitelisted', () => {
  assert.equal(isEndpointWhitelisted('SysLoginController', 'login'), true)
  assert.equal(isEndpointWhitelisted('CaptchaController', 'captchaImage'), true)
})

test('user self-service and utility endpoints remain whitelisted', () => {
  assert.equal(isEndpointWhitelisted('SysDeptController', 'treeselect'), true)
  assert.equal(isEndpointWhitelisted('SysDictDataController', 'getType'), true)
  assert.equal(isEndpointWhitelisted('SysMenuController', 'getRouters'), true)
  assert.equal(isEndpointWhitelisted('SysPostController', 'optionselect'), true)
})

test('wildcard utility endpoints remain whitelisted', () => {
  assert.equal(isEndpointWhitelisted('SysFileController', 'download'), true)
  assert.equal(isEndpointWhitelisted('AnyController', 'health'), true)
  assert.equal(isEndpointWhitelisted('AnyController', 'feignRoute'), true)
})
