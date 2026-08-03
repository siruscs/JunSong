import { request } from './index.js'

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
  return request({ url: `/finance/stockInit/${batchId}`, method: 'PUT', data, contextSensitive: true })
}

export function deleteStockInit(batchId, version) {
  return request({ url: `/finance/stockInit/${batchId}?version=${version}`, method: 'DELETE', contextSensitive: true })
}

export function validateStockInit(batchId, version) {
  return request({ url: `/finance/stockInit/${batchId}/validate?version=${version}`, method: 'PUT', contextSensitive: true })
}

export function submitStockInit(batchId, version) {
  return request({ url: `/finance/stockInit/${batchId}/submit?version=${version}`, method: 'PUT', contextSensitive: true })
}

export function approveStockInit(batchId, version, decision = 'APPROVE') {
  return request({ url: `/finance/stockInit/${batchId}/approve`, method: 'PUT', data: { decision, version }, contextSensitive: true })
}

export function postStockInit(batchId, version) {
  return request({ url: `/finance/stockInit/${batchId}/post`, method: 'PUT', data: { version, postIdempotencyKey: `mp-stock-init-${batchId}-${version}` }, contextSensitive: true })
}
