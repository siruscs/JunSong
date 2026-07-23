import request from '@/utils/request'

/** 统一经营指标响应（Phase 5） */
export interface OperatingMetric {
  code: string
  value: number
  unit: 'CNY' | 'COUNT' | 'PERCENT'
  period: {
    type: 'TODAY' | 'MONTH' | 'CURRENT_PERIOD' | 'CUSTOM'
    start: string
    end: string
  }
  scope: {
    deptIds: number[]
    tenantId: string
  }
  source: {
    module: 'FINANCE' | 'MEMBER' | 'STOCK' | 'SYSTEM'
    endpoint: string
  }
  drillDownRoute: string
}

export interface OperatingMetricQueryParams {
  deptIds?: number[]
  startTime?: string
  endTime?: string
  timeType?: string
}

/**
 * 获取统一经营指标列表（PC 和小程序共用同一端点）。
 * 后端负责租户/部门范围和口径统一。
 */
export function getOperatingMetrics(data?: OperatingMetricQueryParams) {
  return request({
    url: '/finance/operatingMetrics',
    method: 'post',
    data: data || {}
  })
}
