package com.junsong.open.mapper;

import java.util.List;
import com.junsong.open.domain.OpenApiLog;

/**
 * API调用日志Mapper
 *
 * @author junsong
 */
public interface OpenApiLogMapper
{
    public List<OpenApiLog> selectOpenApiLogList(OpenApiLog openApiLog);

    public int insertOpenApiLog(OpenApiLog openApiLog);
}
