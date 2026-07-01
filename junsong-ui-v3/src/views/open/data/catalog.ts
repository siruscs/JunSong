export type OpenCapabilityKey = 'member' | 'workflow' | 'request' | 'foundation'
export type OpenApiMethod = 'GET' | 'POST'

export interface OpenCapability {
  key: OpenCapabilityKey
  label: string
  summary: string
}

export interface OpenApiEndpoint {
  capability: OpenCapabilityKey
  method: OpenApiMethod
  path: string
  desc: string
  auth: string
  scope: string
  status: string
}

export interface OpenCapabilityFilter {
  label: string
  value: OpenCapabilityKey | 'all'
  count: number
}

export const openCapabilities: OpenCapability[] = [
  {
    key: 'member',
    label: '会员能力',
    summary: '读取会员、积分、兑换和运营看板数据。',
  },
  {
    key: 'workflow',
    label: '流程能力',
    summary: '读取流程定义、发起实例、处理待办任务。',
  },
  {
    key: 'request',
    label: '业务申请能力',
    summary: '围绕门店开办申请提供查询、创建、提交和撤回。',
  },
  {
    key: 'foundation',
    label: '平台基础能力',
    summary: '应用、Key、Webhook 与元数据由控制台统一管理。',
  },
]

export const openApiCatalog: OpenApiEndpoint[] = [
  // ── 会员能力 ──────────────────────────────────────
  {
    capability: 'member',
    method: 'GET',
    path: '/members',
    desc: '按授权租户查询会员基础资料，适合会员中台、私域工具和门店系统同步。',
    auth: 'AppKey 签名',
    scope: '会员只读',
    status: '已开放',
  },
  {
    capability: 'member',
    method: 'GET',
    path: '/members/{id}',
    desc: '读取单个会员详情，用于会员画像、门店服务和客户支持场景。',
    auth: 'AppKey 签名',
    scope: '会员只读',
    status: '已开放',
  },
  {
    capability: 'member',
    method: 'GET',
    path: '/members/no/{memberNo}',
    desc: '按会员编号精确查询会员，用于外部门店服务和客户支持场景。',
    auth: 'AppKey 签名',
    scope: '会员只读',
    status: '已开放',
  },
  {
    capability: 'member',
    method: 'GET',
    path: '/members/{memberId}/points',
    desc: '查询会员积分流水，支持外部会员中心展示积分变化。',
    auth: 'AppKey 签名',
    scope: '积分只读',
    status: '已开放',
  },
  {
    capability: 'member',
    method: 'GET',
    path: '/members/points-rules/effective',
    desc: '获取当前生效的积分规则，用于外部系统展示积分获取规则。',
    auth: 'AppKey 签名',
    scope: '积分只读',
    status: '已开放',
  },
  {
    capability: 'member',
    method: 'POST',
    path: '/members/{memberId}/points/records',
    desc: '写入积分变动记录，生产环境需要确认业务场景和幂等策略。',
    auth: 'AppKey 签名',
    scope: '积分写入',
    status: '审批开放',
  },
  {
    capability: 'member',
    method: 'POST',
    path: '/members/points-exchanges',
    desc: '积分兑换操作，生产环境需要确认兑换场景和幂等策略。',
    auth: 'AppKey 签名',
    scope: '积分写入',
    status: '审批开放',
  },
  {
    capability: 'member',
    method: 'GET',
    path: '/members/seckills/active',
    desc: '查询当前进行中的秒杀活动，用于外部营销系统同步活动信息。',
    auth: 'AppKey 签名',
    scope: '会员只读',
    status: '已开放',
  },
  {
    capability: 'member',
    method: 'GET',
    path: '/members/dashboard/stats',
    desc: '读取会员运营统计，适合外部经营看板或集团数据门户。',
    auth: 'AppKey 签名',
    scope: '统计只读',
    status: '已开放',
  },
  {
    capability: 'member',
    method: 'GET',
    path: '/members/dashboard/trend',
    desc: '读取会员运营趋势数据（近 7 天），适合外部经营看板展示变化趋势。',
    auth: 'AppKey 签名',
    scope: '统计只读',
    status: '已开放',
  },
  {
    capability: 'member',
    method: 'GET',
    path: '/members/dashboard/ranking',
    desc: '读取积分余额排行榜（TOP10），适合外部经营看板或集团数据门户。',
    auth: 'AppKey 签名',
    scope: '统计只读',
    status: '已开放',
  },
  // ── 流程能力 ──────────────────────────────────────
  {
    capability: 'workflow',
    method: 'GET',
    path: '/workflow/definitions',
    desc: '查询可发起的流程定义，帮助外部系统展示申请入口。',
    auth: 'AppKey 签名',
    scope: '流程只读',
    status: '已开放',
  },
  {
    capability: 'workflow',
    method: 'GET',
    path: '/workflow/definitions/{id}',
    desc: '读取流程定义详情，帮助外部系统了解流程表单字段和配置。',
    auth: 'AppKey 签名',
    scope: '流程只读',
    status: '已开放',
  },
  {
    capability: 'workflow',
    method: 'GET',
    path: '/workflow/definitions/{id}/diagram',
    desc: '获取流程图（含高亮节点），用于外部系统展示流程进度。',
    auth: 'AppKey 签名',
    scope: '流程只读',
    status: '已开放',
  },
  {
    capability: 'workflow',
    method: 'POST',
    path: '/workflow/instances',
    desc: '发起流程实例，提交前需要确认流程模板和业务表单字段。',
    auth: 'AppKey 签名',
    scope: '流程发起',
    status: '审批开放',
  },
  {
    capability: 'workflow',
    method: 'GET',
    path: '/workflow/instances',
    desc: '查询流程实例列表，用于外部系统跟踪已发起的流程进度。',
    auth: 'AppKey 签名',
    scope: '流程只读',
    status: '已开放',
  },
  {
    capability: 'workflow',
    method: 'GET',
    path: '/workflow/instances/{id}',
    desc: '获取流程实例详情，包含流程变量和当前节点信息。',
    auth: 'AppKey 签名',
    scope: '流程只读',
    status: '已开放',
  },
  {
    capability: 'workflow',
    method: 'GET',
    path: '/workflow/tasks/todo',
    desc: '读取授权身份下的待办任务，用于外部门户展示审批入口。',
    auth: 'AppKey 签名',
    scope: '流程只读',
    status: '已开放',
  },
  {
    capability: 'workflow',
    method: 'GET',
    path: '/workflow/tasks/done',
    desc: '读取已办任务列表，用于外部门户展示审批历史记录。',
    auth: 'AppKey 签名',
    scope: '流程只读',
    status: '已开放',
  },
  {
    capability: 'workflow',
    method: 'POST',
    path: '/workflow/tasks/{taskId}/approve',
    desc: '审批通过任务，调用前需要绑定办理人身份和流程权限。',
    auth: 'AppKey 签名',
    scope: '流程办理',
    status: '审批开放',
  },
  {
    capability: 'workflow',
    method: 'POST',
    path: '/workflow/tasks/{taskId}/reject',
    desc: '驳回任务并填写驳回原因，调用前需要绑定办理人身份和流程权限。',
    auth: 'AppKey 签名',
    scope: '流程办理',
    status: '审批开放',
  },
  {
    capability: 'workflow',
    method: 'POST',
    path: '/workflow/tasks/{taskId}/transfer',
    desc: '转办任务给其他办理人，调用前需要绑定办理人身份和目标办理人。',
    auth: 'AppKey 签名',
    scope: '流程办理',
    status: '审批开放',
  },
  {
    capability: 'workflow',
    method: 'GET',
    path: '/workflow/history/instances/{processInstanceId}/activities',
    desc: '读取流程历史流转记录和节点处理轨迹，用于外部业务单据展示。',
    auth: 'AppKey 签名',
    scope: '流程只读',
    status: '已开放',
  },
  {
    capability: 'workflow',
    method: 'GET',
    path: '/workflow/history/instances/{processInstanceId}/comments',
    desc: '读取流程审批意见和处理轨迹，用于外部业务单据展示。',
    auth: 'AppKey 签名',
    scope: '流程只读',
    status: '已开放',
  },
  {
    capability: 'workflow',
    method: 'GET',
    path: '/workflow/analytics/node-duration',
    desc: '流程节点耗时统计分析，用于外部系统监控流程效率。',
    auth: 'AppKey 签名',
    scope: '流程只读',
    status: '已开放',
  },
  // ── 业务申请能力（门店开办） ──────────────────────────────
  {
    capability: 'request',
    method: 'GET',
    path: '/store-openings',
    desc: '查询门店开办申请列表，用于外部业务台账同步。',
    auth: 'AppKey 签名',
    scope: '业务申请',
    status: '已开放',
  },
  {
    capability: 'request',
    method: 'GET',
    path: '/store-openings/{id}',
    desc: '读取门店开办申请详情，包含基础信息和审批状态。',
    auth: 'AppKey 签名',
    scope: '业务申请',
    status: '已开放',
  },
  {
    capability: 'request',
    method: 'GET',
    path: '/store-openings/orders/{orderNo}',
    desc: '按业务单号查询申请，用于跨系统对账和状态回写。',
    auth: 'AppKey 签名',
    scope: '业务申请',
    status: '已开放',
  },
  {
    capability: 'request',
    method: 'POST',
    path: '/store-openings',
    desc: '从外部入口创建门店开办申请，并返回业务单号。',
    auth: 'AppKey 签名',
    scope: '业务申请',
    status: '已开放',
  },
  {
    capability: 'request',
    method: 'POST',
    path: '/store-openings/{id}/submit',
    desc: '提交业务申请进入审批，调用方需要完成必填字段校验。',
    auth: 'AppKey 签名',
    scope: '业务申请',
    status: '审批开放',
  },
  {
    capability: 'request',
    method: 'POST',
    path: '/store-openings/{id}/withdraw',
    desc: '撤回已提交的门店开办申请，仅在审批完成前可用。',
    auth: 'AppKey 签名',
    scope: '业务申请',
    status: '已开放',
  },
  // ── 平台基础能力 ──────────────────────────────────
  {
    capability: 'foundation',
    method: 'GET',
    path: '/open/apps',
    desc: '查询授权应用基础信息，生产调用只返回当前租户可见应用。',
    auth: 'AppKey 签名',
    scope: '应用只读',
    status: '已开放',
  },
  {
    capability: 'foundation',
    method: 'GET',
    path: '/open/apps/{appId}/keys',
    desc: '查询应用 Key 概览，敏感字段不会通过公共接口返回。',
    auth: 'AppKey 签名',
    scope: '密钥只读',
    status: '审批开放',
  },
  {
    capability: 'foundation',
    method: 'POST',
    path: '/open/webhooks/subscriptions',
    desc: '登记应用回调地址和事件订阅，回调地址需要校验后启用。',
    auth: 'AppKey 签名',
    scope: '事件订阅',
    status: '已开放',
  },
]

export function buildCapabilityFilters(): OpenCapabilityFilter[] {
  return [
    { label: '全部接口', value: 'all', count: openApiCatalog.length },
    ...openCapabilities.map((capability) => ({
      label: capability.label,
      value: capability.key,
      count: openApiCatalog.filter((endpoint) => endpoint.capability === capability.key).length,
    })),
  ]
}

export function groupOpenApiCatalog() {
  return openCapabilities.map((capability) => ({
    ...capability,
    endpoints: openApiCatalog.filter((endpoint) => endpoint.capability === capability.key),
  }))
}
