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
import com.junsong.finance.domain.FinProduct;
import com.junsong.finance.service.IFinProductService;

/**
 * 商品Controller
 * 
 * @author junsong
 */
@RestController
@RequestMapping("/product")
public class FinProductController extends BaseController
{
    @Autowired
    private IFinProductService finProductService;

    /**
     * 查询商品列表
     */
    @RequiresPermissions("finance:product:list")
    @GetMapping("/list")
    public TableDataInfo list(FinProduct finProduct)
    {
        finProduct.setDeptId(SecurityUtils.getDeptId());
        startPage();
        List<FinProduct> list = finProductService.selectFinProductList(finProduct);
        return getDataTable(list);
    }

    /** 供进销业务选择商品，仅返回当前部门的启用商品。 */
    @RequiresPermissions("finance:product:list")
    @GetMapping("/selector")
    public AjaxResult selector()
    {
        FinProduct query = new FinProduct();
        query.setDeptId(SecurityUtils.getDeptId());
        query.setStatus("0");
        return success(finProductService.selectFinProductList(query));
    }

    /**
     * 导出商品列表
     */
    @RequiresPermissions("finance:product:export")
    @Log(title = "商品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FinProduct finProduct)
    {
        List<FinProduct> list = finProductService.selectFinProductList(finProduct);
        ExcelUtil<FinProduct> util = new ExcelUtil<FinProduct>(FinProduct.class);
        util.exportExcel(response, list, "商品数据");
    }

    /**
     * 获取商品详细信息
     */
    @RequiresPermissions("finance:product:query")
    @GetMapping(value = "/{productId:\\d+}")
    public AjaxResult getInfo(@PathVariable Long productId)
    {
        if (productId == null)
        {
            return error("商品不存在");
        }
        Long deptId = SecurityUtils.getDeptId();
        FinProduct product = SecurityUtils.isAdmin()
            ? finProductService.selectFinProductByProductId(productId)
            : finProductService.selectFinProductByProductIdAndDeptId(productId, deptId);
        return product == null ? error("商品不存在或无权访问") : success(product);
    }

    /**
     * 新增商品
     */
    @RequiresPermissions("finance:product:add")
    @Log(title = "商品", businessType = BusinessType.INSERT)
    @Idempotent(scene = "finProduct:create")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinProduct finProduct)
    {
        finProduct.setDeptId(SecurityUtils.getDeptId());
        if (!finProductService.checkProductCodeUnique(finProduct))
        {
            return error("新增商品'" + finProduct.getProductName() + "'失败，商品编码已存在");
        }
        finProduct.setCreateBy(SecurityUtils.getUsername());
        return toAjax(finProductService.insertFinProduct(finProduct));
    }

    /**
     * 修改商品
     */
    @RequiresPermissions("finance:product:edit")
    @Log(title = "商品", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "finProduct:update")
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinProduct finProduct)
    {
        if (finProduct.getProductId() == null)
        {
            return error("修改商品失败，商品ID不能为空，请重新打开编辑页面");
        }
        finProduct.setDeptId(SecurityUtils.getDeptId());
        if (!finProductService.checkProductCodeUnique(finProduct))
        {
            return error("修改商品'" + finProduct.getProductName() + "'失败，商品编码已存在");
        }
        finProduct.setUpdateBy(SecurityUtils.getUsername());
        Long deptId = SecurityUtils.getDeptId();
        int rows = SecurityUtils.isAdmin()
            ? finProductService.updateFinProduct(finProduct)
            : finProductService.updateFinProductByDeptId(finProduct, deptId);
        return rows == 1 ? success("商品修改成功") : error("商品不存在或不属于当前机构，商品未保存");
    }

    /**
     * 删除商品
     */
    @RequiresPermissions("finance:product:remove")
    @Log(title = "商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productIds:\\d+(?:,\\d+)*}")
    public AjaxResult remove(@PathVariable Long[] productIds)
    {
        Long deptId = SecurityUtils.getDeptId();
        return toAjax(SecurityUtils.isAdmin()
            ? finProductService.deleteFinProductByProductIds(productIds)
            : finProductService.deleteFinProductByProductIdsAndDeptId(productIds, deptId));
    }
}
