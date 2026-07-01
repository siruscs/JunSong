package com.junsong.open.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.open.domain.OpenApiLog;
import com.junsong.open.service.IOpenApiLogService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 开放平台 API 调用日志 Controller
 *
 * @author junsong
 */
@RestController
@RequestMapping("/apiLog")
public class OpenApiLogController extends BaseController
{
    @Autowired
    private IOpenApiLogService openApiLogService;

    /**
     * 查询 API 调用日志列表
     */
    @GetMapping("/list")
    @RequiresPermissions("open:log:list")
    public TableDataInfo list(OpenApiLog openApiLog)
    {
        startPage();
        List<OpenApiLog> list = openApiLogService.selectOpenApiLogList(openApiLog);
        return getDataTable(list);
    }

    /**
     * 导出 API 调用日志
     */
    @PostMapping("/export")
    @RequiresPermissions("open:log:export")
    public void export(HttpServletResponse response, OpenApiLog openApiLog)
    {
        List<OpenApiLog> list = openApiLogService.selectOpenApiLogList(openApiLog);
        ExcelUtil<OpenApiLog> util = new ExcelUtil<>(OpenApiLog.class);
        util.exportExcel(response, list, "API调用日志");
    }
}
