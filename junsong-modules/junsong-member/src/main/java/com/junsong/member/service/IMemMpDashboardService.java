package com.junsong.member.service;

import java.util.Map;

/**
 * 小程序移动端首页聚合看板服务。
 *
 * 提供按当前登录用户的租户 + 授权门店范围聚合的经营、会员、成长、积分、
 * 等级、分层、活动、财务等移动端首页指标。
 *
 * 多租户与部门边界：
 * - tenantId 取自 TenantContext.getTenantId()，由 HeaderInterceptor 装填。
 * - deptIds 由 RemoteUserService.getUserDeptList 取得授权门店列表；
 *   admin 返回空列表（不限）；非 admin 与请求 deptIds 求交集；
 *   无可见部门时返回哨兵 [-1L]。
 *
 * 权限边界：
 * - 调用方按当前用户可访问的模块权限决定是否调用对应分组；
 * - 未授权模块不返回对应分组，避免小程序端误展示。
 */
public interface IMemMpDashboardService {

    /**
     * 获取小程序首页聚合看板数据。
     *
     * 返回结构（按权限裁剪）：
     * - tenantId / deptId / deptName：上下文标识
     * - member / growth / points / level / segment / activity / finance：分组指标
     *
     * @param accessibleModules 当前用户可访问的小程序模块 key 列表
     * @return 聚合看板数据
     */
    Map<String, Object> getOverview(java.util.List<String> accessibleModules);
}
