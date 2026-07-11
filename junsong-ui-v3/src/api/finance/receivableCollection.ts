import request from '../request'

export function getReceivableCollectionDashboard(data?: Record<string, any>) {
  return request({
    url: '/finance/receivable-collection/dashboard',
    method: 'post',
    data,
  })
}

export function listReceivableCollections(data?: Record<string, any>) {
  return request({
    url: '/finance/receivable-collection/list',
    method: 'post',
    data,
  })
}

export function syncReceivableCollections(data?: Record<string, any>) {
  return request({
    url: '/finance/receivable-collection/sync',
    method: 'post',
    data,
  })
}

export function followReceivableCollection(collectionId: number | string, data: Record<string, any>) {
  return request({
    url: `/finance/receivable-collection/${collectionId}/follow`,
    method: 'post',
    data,
  })
}
