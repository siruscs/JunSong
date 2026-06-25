package com.junsong.workflow.lowcode.service;

import java.io.IOException;
import java.io.InputStream;

/**
 * 低代码业务数据 Excel 导入导出服务。
 *
 * @author junsong
 */
public interface LcBizExcelService
{
    /**
     * 导出业务数据为 Excel
     */
    byte[] exportBizData(String bizCode) throws IOException;

    /**
     * 生成导入模板
     */
    byte[] generateTemplate(String bizCode) throws IOException;

    /**
     * 从 Excel 导入业务数据
     */
    int importBizData(String bizCode, InputStream inputStream) throws IOException;
}
