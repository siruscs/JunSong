import { request } from './index.js'

export function buildStockInitIdempotencyKey(batchId, action, version) {
  return `mp-stock-init-${batchId || 'new'}-${action}-${version ?? 0}`
}

export function listStockInit(params = {}) {
  return request({ url: '/finance/stockInit', method: 'GET', data: params })
}

export function getStockInitDetail(batchId) {
  return request({ url: `/finance/stockInit/${batchId}`, method: 'GET' })
}

export function createStockInit(data) {
  return request({ url: '/finance/stockInit', method: 'POST', data, contextSensitive: true })
}

export function updateStockInit(batchId, data) {
  return request({ url: `/finance/stockInit/${batchId}`, method: 'PUT', data, contextSensitive: true, idempotencyKey: buildStockInitIdempotencyKey(batchId, 'update', data.version) })
}

export function deleteStockInit(batchId, version) {
  return request({ url: `/finance/stockInit/${batchId}?version=${version}`, method: 'DELETE', contextSensitive: true, idempotencyKey: buildStockInitIdempotencyKey(batchId, 'delete', version) })
}

export function validateStockInit(batchId, version) {
  return request({ url: `/finance/stockInit/${batchId}/validate?version=${version}`, method: 'PUT', contextSensitive: true, idempotencyKey: buildStockInitIdempotencyKey(batchId, 'validate', version) })
}

export function submitStockInit(batchId, version) {
  return request({ url: `/finance/stockInit/${batchId}/submit?version=${version}`, method: 'PUT', contextSensitive: true, idempotencyKey: buildStockInitIdempotencyKey(batchId, 'submit', version) })
}

export function approveStockInit(batchId, version, decision = 'APPROVE') {
  return request({ url: `/finance/stockInit/${batchId}/approve`, method: 'PUT', data: { decision, version }, contextSensitive: true, idempotencyKey: buildStockInitIdempotencyKey(batchId, `approve-${decision}`, version) })
}

export function postStockInit(batchId, version) {
  const key = buildStockInitIdempotencyKey(batchId, 'post', version)
  return request({ url: `/finance/stockInit/${batchId}/post`, method: 'PUT', data: { version, postIdempotencyKey: key }, contextSensitive: true, idempotencyKey: key })
}
