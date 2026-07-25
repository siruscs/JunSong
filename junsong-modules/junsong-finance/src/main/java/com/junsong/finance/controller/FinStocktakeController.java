package com.junsong.finance.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.FinStocktake;
import com.junsong.finance.domain.vo.StocktakeApprovalRequest;
import com.junsong.finance.domain.vo.StocktakeAssignRequest;
import com.junsong.finance.domain.vo.StocktakeCountRequest;
import com.junsong.finance.domain.vo.StocktakeCreateRequest;
import com.junsong.finance.domain.vo.StocktakeDetailVO;
import com.junsong.finance.domain.vo.StocktakeQuery;
import com.junsong.finance.domain.vo.StocktakeRecountRequest;
import com.junsong.finance.domain.vo.StocktakeReverseRequest;
import com.junsong.finance.service.IFinStocktakeService;

/**
 * 库存盘点 Controller（Task 3-7：创建、分配、列表、详情、启动、行录入、提交、复盘、审批、过账、取消、冲销）。
 *
 * 端点：/stocktakes
 *
 * 权限分离（CRUD 与业务操作独立）：
 * - finance:stocktake:add      创建 / 取消（过账前）
 * - finance:stocktake:list     列表
 * - finance:stocktake:query    详情
 * - finance:stocktake:assign   分配
 * - finance:stocktake:count    启动盘点 / 行录入
 * - finance:stocktake:submit   提交
 * - finance:stocktake:recount  复盘
 * - finance:stocktake:approve  审批
 * - finance:stocktake:post     过账（数量与成本原子）
 * - finance:stocktake:reverse  整单冲销
 *
 * @author junsong
 */
@RestController
@RequestMapping("/stocktakes")
public class FinStocktakeController extends BaseController {

    @Autowired
    private IFinStocktakeService finStocktakeService;

    @RequiresPermissions("finance:stocktake:add")
    @Log(title = "库存盘点-创建", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@RequestBody StocktakeCreateRequest request) {
        Long stocktakeId = finStocktakeService.createStocktake(request);
        return AjaxResult.success(stocktakeId);
    }

    @RequiresPermissions("finance:stocktake:list")
    @GetMapping
    public TableDataInfo list(StocktakeQuery query) {
        startPage();
        List<FinStocktake> list = finStocktakeService.listStocktakes(query);
        return getDataTable(list);
    }

    @RequiresPermissions("finance:stocktake:query")
    @GetMapping("/{stocktakeId}")
    public AjaxResult detail(@PathVariable Long stocktakeId) {
        StocktakeDetailVO vo = finStocktakeService.getStocktakeDetail(stocktakeId);
        return AjaxResult.success(vo);
    }

    @RequiresPermissions("finance:stocktake:assign")
    @Log(title = "库存盘点-分配", businessType = BusinessType.UPDATE)
    @PutMapping("/{stocktakeId}/assign")
    public AjaxResult assign(@PathVariable Long stocktakeId,
                              @RequestBody StocktakeAssignRequest request) {
        return toAjax(finStocktakeService.assignCounter(stocktakeId, request));
    }

    @RequiresPermissions("finance:stocktake:count")
    @Log(title = "库存盘点-启动", businessType = BusinessType.UPDATE)
    @PutMapping("/{stocktakeId}/start")
    public AjaxResult start(@PathVariable Long stocktakeId,
                             @org.springframework.web.bind.annotation.RequestParam Integer version) {
        return toAjax(finStocktakeService.startStocktake(stocktakeId, version));
    }

    @RequiresPermissions("finance:stocktake:count")
    @Log(title = "库存盘点-行录入", businessType = BusinessType.UPDATE)
    @PutMapping("/{stocktakeId}/items/{itemId}/count")
    public AjaxResult count(@PathVariable Long stocktakeId,
                             @PathVariable Long itemId,
                             @RequestBody StocktakeCountRequest request) {
        return toAjax(finStocktakeService.countItem(stocktakeId, itemId, request));
    }

    /**
     * 提交盘点（Task 5：校验所有行并生成临时方差，触发复盘阈值）。
     * 权限：finance:stocktake:submit
     */
    @RequiresPermissions("finance:stocktake:submit")
    @Log(title = "库存盘点-提交", businessType = BusinessType.UPDATE)
    @PutMapping("/{stocktakeId}/submit")
    public AjaxResult submit(@PathVariable Long stocktakeId,
                              @org.springframework.web.bind.annotation.RequestParam Integer version) {
        return toAjax(finStocktakeService.submitStocktake(stocktakeId, version));
    }

    /**
     * 复盘行录入（Task 5：独立复盘人录入，须与盘点人不同）。
     * 权限：finance:stocktake:recount
     */
    @RequiresPermissions("finance:stocktake:recount")
    @Log(title = "库存盘点-复盘", businessType = BusinessType.UPDATE)
    @PutMapping("/{stocktakeId}/items/{itemId}/recount")
    public AjaxResult recount(@PathVariable Long stocktakeId,
                               @PathVariable Long itemId,
                               @RequestBody StocktakeRecountRequest request) {
        return toAjax(finStocktakeService.recountItem(stocktakeId, itemId, request));
    }

    /**
     * 审批盘点（Task 5：确定 finalQuantity，流转至 APPROVED）。
     * 权限：finance:stocktake:approve
     */
    @RequiresPermissions("finance:stocktake:approve")
    @Log(title = "库存盘点-审批", businessType = BusinessType.UPDATE)
    @PutMapping("/{stocktakeId}/approve")
    public AjaxResult approve(@PathVariable Long stocktakeId,
                               @RequestBody StocktakeApprovalRequest request) {
        return toAjax(finStocktakeService.approveStocktake(stocktakeId, request));
    }

    /**
     * 过账盘点（Task 6：数量与移动平均成本原子过账）。
     * 权限：finance:stocktake:post
     */
    @RequiresPermissions("finance:stocktake:post")
    @Log(title = "库存盘点-过账", businessType = BusinessType.UPDATE)
    @PutMapping("/{stocktakeId}/post")
    public AjaxResult post(@PathVariable Long stocktakeId,
                           @org.springframework.web.bind.annotation.RequestParam Integer version) {
        return toAjax(finStocktakeService.postStocktake(stocktakeId, version));
    }

    /**
     * 取消盘点任务（Task 7：仅过账前可取消）。
     * 权限：finance:stocktake:add（创建者可取消未过账任务）
     */
    @RequiresPermissions("finance:stocktake:add")
    @Log(title = "库存盘点-取消", businessType = BusinessType.UPDATE)
    @PutMapping("/{stocktakeId}/cancel")
    public AjaxResult cancel(@PathVariable Long stocktakeId,
                              @org.springframework.web.bind.annotation.RequestParam Integer version) {
        return toAjax(finStocktakeService.cancelStocktake(stocktakeId, version));
    }

    /**
     * 整单冲销盘点任务（Task 7：仅 POSTED 可冲销）。
     * 权限：finance:stocktake:reverse
     */
    @RequiresPermissions("finance:stocktake:reverse")
    @Log(title = "库存盘点-整单冲销", businessType = BusinessType.UPDATE)
    @PutMapping("/{stocktakeId}/reverse")
    public AjaxResult reverse(@PathVariable Long stocktakeId,
                               @RequestBody StocktakeReverseRequest request) {
        return toAjax(finStocktakeService.reverseStocktake(stocktakeId, request));
    }
}
