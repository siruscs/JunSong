import request from '../request'

/**
 * 查询用户的微信小程序绑定状态
 * 独立权限 system:user:unbindMp，不返回 openid/unionid 等敏感标识
 */
export function getUserBindings(userId: number) {
  return request({
    url: '/system/user/' + userId + '/mp-binding',
    method: 'get',
  })
}

/**
 * 管理员解绑用户的微信绑定
 * 独立权限 system:user:unbindMp，写审计日志
 */
export function adminUnbind(userId: number, reason?: string) {
  return request({
    url: '/system/user/' + userId + '/mp-binding',
    method: 'delete',
    params: reason ? { reason } : {},
  })
}

/**
 * 一键使当前租户的所有微信登录会话失效
 * 独立权限 system:user:wechatSession:revokeAll，写审计日志
 * 不解除绑定，用户下次仍可微信登录
 */
export function revokeAllWechatSessions(reason?: string) {
  return request({
    url: '/system/wechat-session/revoke-all',
    method: 'post',
    params: reason ? { reason } : {},
  })
}

/**
 * 查询当前租户的微信会话版本号
 */
export function getWechatSessionEpoch() {
  return request({
    url: '/system/wechat-session/epoch',
    method: 'get',
  })
}
