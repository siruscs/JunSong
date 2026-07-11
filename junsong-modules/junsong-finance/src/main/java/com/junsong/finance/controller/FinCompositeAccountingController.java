package com.junsong.finance.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinCompositeAccountingPool;
import com.junsong.finance.domain.FinCompositePeriodItem;
import com.junsong.finance.domain.vo.CompositeCandidatePeriodVO;
import com.junsong.finance.domain.vo.CompositePoolOverviewVO;
import com.junsong.finance.domain.vo.CompositeTrialResultVO;
import com.junsong.finance.service.IFinCompositeAccountingService;

/**
 * 复合核算 Controller
 *
 * @author junsong
 */
@RestController
@RequestMapping("/compositeAccounting")
public class FinCompositeAccountingController extends BaseController
{
    @Autowired
    private IFinCompositeAccountingService compositeAccountingService;

    /**
     * 查询复合核算池列表
     */
    @RequiresPermissions("finance:compositeAccounting:list")
    @GetMapping("/list")
    public TableDataInfo list(FinCompositeAccountingPool pool) {
        startPage();
        List<FinCompositeAccountingPool> list = compositeAccountingService.selectCompositePoolList(pool);
        return getDataTable(list);
    }

    /**
     * 获取复合核算池详情
     */
    @RequiresPermissions("finance:compositeAccounting:query")
    @GetMapping("/{poolId}")
    public AjaxResult getInfo(@PathVariable Long poolId) {
        return success(compositeAccountingService.selectCompositePoolByPoolId(poolId));
    }

    /**
     * 获取复合核算池概览(含参与店面、共享投资人、周期明细、回本进度)
     */
    @RequiresPermissions("finance:compositeAccounting:query")
    @GetMapping("/{poolId}/overview")
    public AjaxResult getOverview(@PathVariable Long poolId) {
        CompositePoolOverviewVO vo = compositeAccountingService.getOverview(poolId);
        return success(vo);
    }

    /**
     * 查询已纳入周期明细
     */
    @RequiresPermissions("finance:compositeAccounting:query")
    @GetMapping("/{poolId}/periods")
    public AjaxResult listPeriods(@PathVariable Long poolId) {
        List<FinCompositePeriodItem> list = compositeAccountingService.listPeriods(poolId);
        return success(list);
    }

    /**
     * 查询可手动纳入的候选周期(回本后使用)
     */
    @RequiresPermissions("finance:compositeAccounting:query")
    @GetMapping("/{poolId}/candidatePeriods")
    public AjaxResult listCandidatePeriods(@PathVariable Long poolId,
                                           @RequestParam(value = "deptId", required = false) Long deptId) {
        List<CompositeCandidatePeriodVO> list = compositeAccountingService.listCandidatePeriods(poolId, deptId);
        return success(list);
    }

    /**
     * 新增复合核算池
     */
    @RequiresPermissions("finance:compositeAccounting:add")
    @Log(title = "复合核算池", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FinCompositeAccountingPool pool) {
        pool.setCreateBy(SecurityUtils.getUsername());
        return toAjax(compositeAccountingService.createPool(pool));
    }

    /**
     * 修改复合核算池基础信息
     */
    @RequiresPermissions("finance:compositeAccounting:edit")
    @Log(title = "复合核算池", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FinCompositeAccountingPool pool) {
        pool.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(compositeAccountingService.updatePool(pool));
    }

    /**
     * 删除复合核算池
     */
    @RequiresPermissions("finance:compositeAccounting:remove")
    @Log(title = "复合核算池", businessType = BusinessType.DELETE)
    @DeleteMapping("/{poolIds}")
    public AjaxResult remove(@PathVariable Long[] poolIds) {
        return toAjax(compositeAccountingService.deleteCompositePoolByPoolIds(poolIds));
    }

    /**
     * 维护参与店面(全量覆盖)
     */
    @RequiresPermissions("finance:compositeAccounting:edit")
    @Log(title = "复合核算池-绑定店面", businessType = BusinessType.UPDATE)
    @PostMapping("/{poolId}/bindDepts")
    public AjaxResult bindDepts(@PathVariable Long poolId, @RequestBody List<Long> deptIds) {
        return toAjax(compositeAccountingService.bindDepts(poolId, deptIds));
    }

    /**
     * 维护共享投资人和出资款(全量覆盖)
     */
    @RequiresPermissions("finance:compositeAccounting:edit")
    @Log(title = "复合核算池-绑定投资人", businessType = BusinessType.UPDATE)
    @PostMapping("/{poolId}/bindInvestors")
    public AjaxResult bindInvestors(@PathVariable Long poolId,
                                    @RequestBody List<IFinCompositeAccountingService.InvestorInput> investors) {
        return toAjax(compositeAccountingService.bindInvestors(poolId, investors));
    }

    /**
     * 试算手动纳入结果(不落库)
     */
    @RequiresPermissions("finance:compositeAccounting:include")
    @PostMapping("/{poolId}/trialInclude")
    public AjaxResult trialInclude(@PathVariable Long poolId, @RequestBody List<Long> periodIds) {
        CompositeTrialResultVO result = compositeAccountingService.trialIncludePeriods(poolId, periodIds);
        return success(result);
    }

    /**
     * 确认纳入周期(落库并刷新回本金额)
     */
    @RequiresPermissions("finance:compositeAccounting:include")
    @Log(title = "复合核算池-确认纳入周期", businessType = BusinessType.UPDATE)
    @PostMapping("/{poolId}/confirmInclude")
    public AjaxResult confirmInclude(@PathVariable Long poolId, @RequestBody List<Long> periodIds) {
        return toAjax(compositeAccountingService.confirmIncludePeriods(poolId, periodIds));
    }

    /**
     * 重新计算累计回本、缺口、超额收益
     */
    @RequiresPermissions("finance:compositeAccounting:edit")
    @Log(title = "复合核算池-重新计算", businessType = BusinessType.UPDATE)
    @PostMapping("/{poolId}/recalculate")
    public AjaxResult recalculate(@PathVariable Long poolId) {
        return toAjax(compositeAccountingService.recalculatePool(poolId));
    }

    /**
     * 财务确认整体回本
     */
    @RequiresPermissions("finance:compositeAccounting:confirm")
    @Log(title = "复合核算池-确认回本", businessType = BusinessType.UPDATE)
    @PostMapping("/{poolId}/confirmBreakEven")
    public AjaxResult confirmBreakEven(@PathVariable Long poolId) {
        return toAjax(compositeAccountingService.confirmBreakEven(poolId));
    }

    /**
     * 关闭复合核算池
     */
    @RequiresPermissions("finance:compositeAccounting:close")
    @Log(title = "复合核算池-关闭", businessType = BusinessType.UPDATE)
    @PostMapping("/{poolId}/close")
    public AjaxResult close(@PathVariable Long poolId) {
        return toAjax(compositeAccountingService.closePool(poolId));
    }
}
