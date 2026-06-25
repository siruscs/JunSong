package com.junsong.workflow.lowcode.service;

import java.util.List;
import java.util.Map;

/**
 * 低代码报表服务（基于 NATIVE 存储）。
 *
 * @author junsong
 */
public interface LcReportService
{
    /**
     * 查询 NATIVE 表数据列表
     */
    List<Map<String, Object>> queryNativeData(String bizCode, Map<String, Object> params);

    /**
     * 查询 NATIVE 表统计信息
     */
    Map<String, Object> queryNativeStatistics(String bizCode, Map<String, Object> params);

    /**
     * 查询 NATIVE 表列信息
     */
    List<Map<String, Object>> queryNativeColumns(String bizCode);
}
