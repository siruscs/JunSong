package com.junsong.workflow.lowcode.controller;

import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.workflow.lowcode.service.LcReportService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 低代码报表 Controller（基于 NATIVE 存储）。
 *
 * <p>为 NATIVE 模式的业务对象提供动态数据查询和统计报表能力。</p>
 *
 * @author junsong
 */
@Validated
@RestController
@RequestMapping("/lowcode/report")
public class LcReportController extends BaseController
{
    @Autowired
    private LcReportService reportService;

    /**
     * NATIVE 表数据列表查询（支持动态字段过滤）
     */
    @PreAuthorize("@ss.hasPermi('lowcode:report:list')")
    @GetMapping("/{bizCode}/data")
    public R<TableDataInfo> dataList(
        @PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode,
        @RequestParam Map<String, Object> params)
    {
        startPage();
        List<Map<String, Object>> list = reportService.queryNativeData(bizCode, params);
        return R.ok(getDataTable(list));
    }

    /**
     * NATIVE 表简单统计（计数、总和）
     */
    @PreAuthorize("@ss.hasPermi('lowcode:report:stat')")
    @GetMapping("/{bizCode}/stat")
    public R<Map<String, Object>> statistics(
        @PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode,
        @RequestParam Map<String, Object> params)
    {
        Map<String, Object> stat = reportService.queryNativeStatistics(bizCode, params);
        return R.ok(stat);
    }

    /**
     * 获取 NATIVE 表的列信息（用于前端报表配置）
     */
    @PreAuthorize("@ss.hasPermi('lowcode:report:list')")
    @GetMapping("/{bizCode}/columns")
    public R<List<Map<String, Object>>> columns(
        @PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode)
    {
        List<Map<String, Object>> columns = reportService.queryNativeColumns(bizCode);
        return R.ok(columns);
    }
}
