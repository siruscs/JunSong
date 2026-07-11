import request from '@/utils/request'

export function getEnterpriseHardeningDashboard() {
  return request({ url: '/system/hardening/dashboard', method: 'get' })
}

export function listAuditSnapshots(data: Record<string, any>) {
  return request({ url: '/system/hardening/audits', method: 'post', data })
}

export function previewArchive(data: Record<string, any>) {
  return request({ url: '/system/hardening/archive/preview', method: 'post', data })
}

export function runArchive(data: Record<string, any>) {
  return request({ url: '/system/hardening/archive/run', method: 'post', data })
}

export function listAlertEvents(data: Record<string, any>) {
  return request({ url: '/system/hardening/alerts', method: 'post', data })
}

export function ackAlert(eventId: number | string) {
  return request({ url: `/system/hardening/alerts/${eventId}/ack`, method: 'post' })
}

export function resolveAlert(eventId: number | string) {
  return request({ url: `/system/hardening/alerts/${eventId}/resolve`, method: 'post' })
}
