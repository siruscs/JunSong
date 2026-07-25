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
import com.junsong.finance.domain.vo.StocktakeAssignRequest;
import com.junsong.finance.domain.vo.StocktakeCreateRequest;
import com.junsong.finance.domain.vo.StocktakeDetailVO;
import com.junsong.finance.domain.vo.StocktakeQuery;
import com.junsong.finance.service.IFinStocktakeService;

/**
 * 库存盘点 Controller（Task 3：创建、分配、列表、详情）。
 *
 * 端点：/stocktakes
 *
 * 权限分离（CRUD 与业务操作独立）：
 * - finance:stocktake:add     创建
 * - finance:stocktake:list    列表
 * - finance:stocktake:query   详情
 * - finance:stocktake:assign  分配
 *
 * 后续 Task 扩展：start / count / submit / recount / approve / post / cancel / reverse / export
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
}
