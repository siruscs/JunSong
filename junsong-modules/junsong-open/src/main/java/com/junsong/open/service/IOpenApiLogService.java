package com.junsong.open.service;

import java.util.List;
import com.junsong.open.domain.OpenApiLog;

/**
 * 开放平台 API 调用日志 服务层接口
 *
 * @author junsong
 */
public interface IOpenApiLogService
{
    /**
     * 查询 API 调用日志列表
     */
    List<OpenApiLog> selectOpenApiLogList(OpenApiLog openApiLog);

    /**
     * 新增 API 调用日志
     */
    int insertOpenApiLog(OpenApiLog openApiLog);
}
