package com.junsong.finance.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.finance.domain.FinStockInitBatch;
import com.junsong.finance.domain.vo.StockInitApproveRequest;
import com.junsong.finance.domain.vo.StockInitCreateRequest;
import com.junsong.finance.domain.vo.StockInitPostRequest;
import com.junsong.finance.domain.vo.StockInitQuery;
import com.junsong.finance.service.IFinStockInitService;

/**
 * 期初库存 Controller。
 *
 * 端点：/stockInit
 *
 * 权限分离（CRUD 与业务操作独立）：
 * - finance:stockInit:add      创建 / 校验 / 提交
 * - finance:stockInit:list     列表
 * - finance:stockInit:query    详情
 * - finance:stockInit:approve  审批
 * - finance:stockInit:post     过账（数量与成本原子）
 * - finance:stockInit:export   导出
 *
 * @author junsong
 */
@RestController
@RequestMapping("/stockInit")
public class FinStockInitController extends BaseController {

    @Autowired
    private IFinStockInitService finStockInitService;

    @RequiresPermissions("finance:stockInit:add")
    @Log(title = "期初库存-创建", businessType = BusinessType.INSERT)
    @Idempotent(scene = "stockInit:create", highRisk = true, ttlSeconds = 2592000)
    @PostMapping
    public AjaxResult create(@RequestBody StockInitCreateRequest request) {
        Long batchId = finStockInitService.createStockInit(request);
        return AjaxResult.success(batchId);
    }

    @RequiresPermissions("finance:stockInit:add")
    @Log(title = "库存调整-修改", businessType = BusinessType.UPDATE)
    @PutMapping("/{batchId}")
    public AjaxResult update(@PathVariable Long batchId, @RequestBody StockInitCreateRequest request) {
        return toAjax(finStockInitService.updateStockInit(batchId, request));
    }

    @RequiresPermissions("finance:stockInit:list")
    @GetMapping
    public TableDataInfo list(StockInitQuery query) {
        startPage();
        List<FinStockInitBatch> list = finStockInitService.listStockInit(query);
        return getDataTable(list);
    }

    @RequiresPermissions("finance:stockInit:export")
    @Log(title = "期初库存-导出", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockInitQuery query) {
        List<FinStockInitBatch> list = finStockInitService.listStockInit(query);
        ExcelUtil<FinStockInitBatch> util = new ExcelUtil<FinStockInitBatch>(FinStockInitBatch.class);
        util.exportExcel(response, list, "期初库存数据");
    }

    @RequiresPermissions("finance:stockInit:query")
    @GetMapping("/{batchId}")
    public AjaxResult detail(@PathVariable Long batchId) {
        return AjaxResult.success(finStockInitService.getStockInitDetail(batchId));
    }

    @RequiresPermissions("finance:stockInit:remove")
    @Log(title = "库存调整-删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{batchId}")
    public AjaxResult delete(@PathVariable Long batchId,
                             @org.springframework.web.bind.annotation.RequestParam Integer version) {
        return toAjax(finStockInitService.deleteStockInit(batchId, version));
    }

    @RequiresPermissions("finance:stockInit:add")
    @Log(title = "期初库存-校验", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "stockInit:validate")
    @PutMapping("/{batchId}/validate")
    public AjaxResult validate(@PathVariable Long batchId,
                                @org.springframework.web.bind.annotation.RequestParam Integer version) {
        return toAjax(finStockInitService.validateStockInit(batchId, version));
    }

    @RequiresPermissions("finance:stockInit:add")
    @Log(title = "期初库存-提交", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "stockInit:submit", highRisk = true, ttlSeconds = 2592000)
    @PutMapping("/{batchId}/submit")
    public AjaxResult submit(@PathVariable Long batchId,
                              @org.springframework.web.bind.annotation.RequestParam Integer version) {
        return toAjax(finStockInitService.submitStockInit(batchId, version));
    }

    @RequiresPermissions("finance:stockInit:approve")
    @Log(title = "期初库存-审批", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "stockInit:approve", highRisk = true, ttlSeconds = 2592000)
    @PutMapping("/{batchId}/approve")
    public AjaxResult approve(@PathVariable Long batchId,
                               @RequestBody StockInitApproveRequest request) {
        return toAjax(finStockInitService.approveStockInit(batchId, request));
    }

    @RequiresPermissions("finance:stockInit:post")
    @Log(title = "期初库存-过账", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "stockInit:post", highRisk = true, ttlSeconds = 2592000)
    @PutMapping("/{batchId}/post")
    public AjaxResult post(@PathVariable Long batchId,
                           @RequestBody StockInitPostRequest request) {
        return toAjax(finStockInitService.postStockInit(batchId, request));
    }
}
