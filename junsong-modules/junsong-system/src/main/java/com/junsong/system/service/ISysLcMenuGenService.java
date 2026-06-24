package com.junsong.system.service;

import com.junsong.system.api.domain.LcMenuGenerateRequest;

/**
 * 低代码菜单自动生成 服务层
 */
public interface ISysLcMenuGenService
{
    /**
     * 为低代码业务生成菜单+按钮权限（幂等），返回生成的菜单 id
     *
     * @param request 生成入参
     * @return 菜单 id
     */
    Long generate(LcMenuGenerateRequest request);
}
