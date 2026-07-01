package com.junsong.open.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.open.domain.OpenApiLog;
import com.junsong.open.mapper.OpenApiLogMapper;
import com.junsong.open.service.IOpenApiLogService;

/**
 * 开放平台 API 调用日志 服务层实现
 *
 * @author junsong
 */
@Service
public class OpenApiLogServiceImpl implements IOpenApiLogService
{
    @Autowired
    private OpenApiLogMapper openApiLogMapper;

    @Override
    public List<OpenApiLog> selectOpenApiLogList(OpenApiLog openApiLog)
    {
        return openApiLogMapper.selectOpenApiLogList(openApiLog);
    }

    @Override
    public int insertOpenApiLog(OpenApiLog openApiLog)
    {
        return openApiLogMapper.insertOpenApiLog(openApiLog);
    }
}
