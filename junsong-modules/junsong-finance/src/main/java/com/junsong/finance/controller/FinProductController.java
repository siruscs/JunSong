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
        startPage();
        List<FinProduct> list = finProductService.selectFinProductList(finProduct);
        return getDataTable(list);
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
    @GetMapping(value = "/{productId}")
    public AjaxResult getInfo(@PathVariable Long productId)
    {
        return success(finProductService.selectFinProductByProductId(productId));
    }

    /**
     * 新增商品
     */
    @RequiresPermissions("finance:product:add")
    @Log(title = "商品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinProduct finProduct)
    {
        if (!finProductService.checkProductCodeUnique(finProduct))
        {
            return error("新增商品'" + finProduct.getProductName() + "'失败，商品编码已存在");
        }
        finProduct.setCreateBy(SecurityUtils.getUsername());
        finProduct.setDeptId(SecurityUtils.getDeptId());
        return toAjax(finProductService.insertFinProduct(finProduct));
    }

    /**
     * 修改商品
     */
    @RequiresPermissions("finance:product:edit")
    @Log(title = "商品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinProduct finProduct)
    {
        if (!finProductService.checkProductCodeUnique(finProduct))
        {
            return error("修改商品'" + finProduct.getProductName() + "'失败，商品编码已存在");
        }
        finProduct.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(finProductService.updateFinProduct(finProduct));
    }

    /**
     * 删除商品
     */
    @RequiresPermissions("finance:product:remove")
    @Log(title = "商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productIds}")
    public AjaxResult remove(@PathVariable Long[] productIds)
    {
        return toAjax(finProductService.deleteFinProductByProductIds(productIds));
    }
}
