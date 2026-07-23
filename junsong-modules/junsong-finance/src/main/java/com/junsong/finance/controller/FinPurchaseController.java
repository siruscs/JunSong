package com.junsong.finance.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinPurchase;
import com.junsong.finance.service.IFinPurchaseService;

/**
 * 进货单Controller
 * 
 * @author junsong
 */
@RestController
@RequestMapping("/purchase")
public class FinPurchaseController extends BaseController
{
    @Autowired
    private IFinPurchaseService finPurchaseService;

    /**
     * 查询进货单列表
     */
    @RequiresPermissions("finance:purchase:list")
    @GetMapping("/list")
    public TableDataInfo list(FinPurchase finPurchase)
    {
        finPurchase.setDeptId(SecurityUtils.getDeptId());
        startPage();
        List<FinPurchase> list = finPurchaseService.selectFinPurchaseList(finPurchase);
        return getDataTable(list);
    }

    /**
     * 导出进货单列表
     */
    @RequiresPermissions("finance:purchase:export")
    @Log(title = "进货单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FinPurchase finPurchase)
    {
        finPurchase.setDeptId(SecurityUtils.getDeptId());
        List<FinPurchase> list = finPurchaseService.selectFinPurchaseList(finPurchase);
        ExcelUtil<FinPurchase> util = new ExcelUtil<FinPurchase>(FinPurchase.class);
        util.exportExcel(response, list, "进货单数据");
    }

    /**
     * 获取进货单详细信息
     */
    @RequiresPermissions("finance:purchase:query")
    @GetMapping(value = "/{purchaseId}")
    public AjaxResult getInfo(@PathVariable Long purchaseId)
    {
        FinPurchase purchase = finPurchaseService.selectFinPurchaseByPurchaseId(purchaseId);
        if (purchase == null || (!SecurityUtils.isAdmin() && !java.util.Objects.equals(purchase.getDeptId(), SecurityUtils.getDeptId())))
        {
            return error("进货单不存在或无权访问");
        }
        return success(purchase);
    }

    /**
     * 新增进货单
     */
    @RequiresPermissions("finance:purchase:add")
    @Log(title = "进货单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinPurchase finPurchase)
    {
        if (!finPurchaseService.checkPurchaseNoUnique(finPurchase))
        {
            return error("新增进货单失败，进货单号已存在");
        }
        finPurchase.setCreateBy(SecurityUtils.getUsername());
        finPurchase.setDeptId(SecurityUtils.getDeptId());
        if (finPurchase.getDetails() != null)
        {
            finPurchase.getDetails().forEach(detail -> detail.setCreateBy(SecurityUtils.getUsername()));
        }
        return toAjax(finPurchaseService.insertFinPurchase(finPurchase));
    }

    /**
     * 修改进货单
     */
    @RequiresPermissions("finance:purchase:edit")
    @Log(title = "进货单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinPurchase finPurchase)
    {
        FinPurchase existing = finPurchaseService.selectFinPurchaseByPurchaseId(finPurchase.getPurchaseId());
        if (existing == null || (!SecurityUtils.isAdmin() && !java.util.Objects.equals(existing.getDeptId(), SecurityUtils.getDeptId())))
        {
            return error("进货单不存在或无权编辑");
        }
        finPurchase.setDeptId(SecurityUtils.getDeptId());
        if (!finPurchaseService.checkPurchaseNoUnique(finPurchase))
        {
            return error("修改进货单失败，进货单号已存在");
        }
        finPurchase.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(finPurchaseService.updateFinPurchase(finPurchase));
    }

    /**
     * 删除进货单
     */
    @RequiresPermissions("finance:purchase:remove")
    @Log(title = "进货单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{purchaseIds}")
    public AjaxResult remove(@PathVariable Long[] purchaseIds)
    {
        if (!SecurityUtils.isAdmin())
        {
            Long deptId = SecurityUtils.getDeptId();
            for (Long purchaseId : purchaseIds)
            {
                FinPurchase purchase = finPurchaseService.selectFinPurchaseByPurchaseId(purchaseId);
                if (purchase == null || !java.util.Objects.equals(purchase.getDeptId(), deptId))
                {
                    return error("包含不存在或无权删除的进货单");
                }
            }
        }
        return toAjax(finPurchaseService.deleteFinPurchaseByPurchaseIds(purchaseIds));
    }
}
