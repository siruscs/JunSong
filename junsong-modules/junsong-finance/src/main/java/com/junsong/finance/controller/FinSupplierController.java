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
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinSupplier;
import com.junsong.finance.service.IFinSupplierService;

/**
 * 供应商Controller
 * 
 * @author junsong
 */
@RestController
@RequestMapping("/supplier")
public class FinSupplierController extends BaseController
{
    @Autowired
    private IFinSupplierService finSupplierService;

    /**
     * 查询供应商列表
     */
    @RequiresPermissions("finance:supplier:list")
    @GetMapping("/list")
    public TableDataInfo list(FinSupplier finSupplier)
    {
        finSupplier.setDeptId(SecurityUtils.getDeptId());
        startPage();
        List<FinSupplier> list = finSupplierService.selectFinSupplierList(finSupplier);
        return getDataTable(list);
    }

    /** 供进货业务选择供应商，仅返回当前部门的启用供应商。 */
    @RequiresPermissions("finance:supplier:list")
    @GetMapping("/selector")
    public AjaxResult selector()
    {
        FinSupplier query = new FinSupplier();
        query.setDeptId(SecurityUtils.getDeptId());
        query.setStatus("0");
        return success(finSupplierService.selectFinSupplierList(query));
    }

    /**
     * 导出供应商列表
     */
    @RequiresPermissions("finance:supplier:export")
    @Log(title = "供应商", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FinSupplier finSupplier)
    {
        List<FinSupplier> list = finSupplierService.selectFinSupplierList(finSupplier);
        ExcelUtil<FinSupplier> util = new ExcelUtil<FinSupplier>(FinSupplier.class);
        util.exportExcel(response, list, "供应商数据");
    }

    /**
     * 获取供应商详细信息
     */
    @RequiresPermissions("finance:supplier:query")
    @GetMapping(value = "/{supplierId}")
    public AjaxResult getInfo(@PathVariable Long supplierId)
    {
        if (supplierId == null)
        {
            return error("供应商不存在");
        }
        Long deptId = SecurityUtils.getDeptId();
        FinSupplier supplier = SecurityUtils.isAdmin()
            ? finSupplierService.selectFinSupplierBySupplierId(supplierId)
            : finSupplierService.selectFinSupplierBySupplierIdAndDeptId(supplierId, deptId);
        return supplier == null ? error("供应商不存在或无权访问") : success(supplier);
    }

    /**
     * 新增供应商
     */
    @RequiresPermissions("finance:supplier:add")
    @Log(title = "供应商", businessType = BusinessType.INSERT)
    @Idempotent(scene = "supplier:create")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinSupplier finSupplier)
    {
        if (!finSupplierService.checkSupplierCodeUnique(finSupplier))
        {
            return error("新增供应商'" + finSupplier.getSupplierName() + "'失败，供应商编码已存在");
        }
        finSupplier.setCreateBy(SecurityUtils.getUsername());
        finSupplier.setDeptId(SecurityUtils.getDeptId());
        return toAjax(finSupplierService.insertFinSupplier(finSupplier));
    }

    /**
     * 修改供应商
     */
    @RequiresPermissions("finance:supplier:edit")
    @Log(title = "供应商", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "supplier:update")
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinSupplier finSupplier)
    {
        if (!finSupplierService.checkSupplierCodeUnique(finSupplier))
        {
            return error("修改供应商'" + finSupplier.getSupplierName() + "'失败，供应商编码已存在");
        }
        finSupplier.setUpdateBy(SecurityUtils.getUsername());
        Long deptId = SecurityUtils.getDeptId();
        return toAjax(SecurityUtils.isAdmin()
            ? finSupplierService.updateFinSupplier(finSupplier)
            : finSupplierService.updateFinSupplierByDeptId(finSupplier, deptId));
    }

    /**
     * 删除供应商
     */
    @RequiresPermissions("finance:supplier:remove")
    @Log(title = "供应商", businessType = BusinessType.DELETE)
    @Idempotent(scene = "supplier:delete")
    @DeleteMapping("/{supplierIds}")
    public AjaxResult remove(@PathVariable Long[] supplierIds)
    {
        Long deptId = SecurityUtils.getDeptId();
        return toAjax(SecurityUtils.isAdmin()
            ? finSupplierService.deleteFinSupplierBySupplierIds(supplierIds)
            : finSupplierService.deleteFinSupplierBySupplierIdsAndDeptId(supplierIds, deptId));
    }
}
