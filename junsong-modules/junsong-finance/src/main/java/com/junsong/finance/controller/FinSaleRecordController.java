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
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.service.IFinSaleRecordService;

/**
 * 销售记录Controller
 * 
 * @author junsong
 */
@RestController
@RequestMapping("/sale")
public class FinSaleRecordController extends BaseController
{
    @Autowired
    private IFinSaleRecordService finSaleRecordService;

    /**
     * 查询销售记录列表
     */
    @RequiresPermissions("finance:sale:list")
    @GetMapping("/list")
    public TableDataInfo list(FinSaleRecord finSaleRecord)
    {
        startPage();
        List<FinSaleRecord> list = finSaleRecordService.selectFinSaleRecordList(finSaleRecord);
        return getDataTable(list);
    }

    /**
     * 导出销售记录列表
     */
    @RequiresPermissions("finance:sale:export")
    @Log(title = "销售记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FinSaleRecord finSaleRecord)
    {
        List<FinSaleRecord> list = finSaleRecordService.selectFinSaleRecordList(finSaleRecord);
        ExcelUtil<FinSaleRecord> util = new ExcelUtil<FinSaleRecord>(FinSaleRecord.class);
        util.exportExcel(response, list, "销售记录数据");
    }

    /**
     * 获取销售记录详细信息
     */
    @RequiresPermissions("finance:sale:query")
    @GetMapping(value = "/{saleId}")
    public AjaxResult getInfo(@PathVariable Long saleId)
    {
        return success(finSaleRecordService.selectFinSaleRecordBySaleId(saleId));
    }

    /**
     * 新增销售记录
     */
    @RequiresPermissions("finance:sale:add")
    @Log(title = "销售记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinSaleRecord finSaleRecord)
    {
        finSaleRecord.setDeptId(SecurityUtils.getDeptId());
        if (!finSaleRecordService.checkSaleNoUnique(finSaleRecord))
        {
            return error("新增销售记录'" + finSaleRecord.getSaleNo() + "'失败，销售单号已存在");
        }
        return toAjax(finSaleRecordService.insertFinSaleRecord(finSaleRecord));
    }

    /**
     * 修改销售记录
     */
    @RequiresPermissions("finance:sale:edit")
    @Log(title = "销售记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinSaleRecord finSaleRecord)
    {
        finSaleRecord.setDeptId(SecurityUtils.getDeptId());
        if (!finSaleRecordService.checkSaleNoUnique(finSaleRecord))
        {
            return error("修改销售记录'" + finSaleRecord.getSaleNo() + "'失败，销售单号已存在");
        }
        return toAjax(finSaleRecordService.updateFinSaleRecord(finSaleRecord));
    }

    /**
     * 删除销售记录
     */
    @RequiresPermissions("finance:sale:remove")
    @Log(title = "销售记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{saleIds}")
    public AjaxResult remove(@PathVariable Long[] saleIds)
    {
        return toAjax(finSaleRecordService.deleteFinSaleRecordBySaleIds(saleIds));
    }

    /**
     * 添加缴款记录
     */
    @RequiresPermissions("finance:sale:edit")
    @Log(title = "添加缴款", businessType = BusinessType.UPDATE)
    @PostMapping("/payment/{saleId}")
    public AjaxResult addPayment(@PathVariable Long saleId, @RequestBody java.util.Map<String, Object> params)
    {
        java.math.BigDecimal paymentAmount = new java.math.BigDecimal(params.get("paymentAmount").toString());
        String paymentMethod = (String) params.get("paymentMethod");
        String remark = (String) params.get("remark");
        String paymentDateStr = (String) params.get("paymentDate");
        java.util.Date paymentDate = null;
        if (paymentDateStr != null && !paymentDateStr.isEmpty())
        {
            try
            {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                paymentDate = sdf.parse(paymentDateStr);
            }
            catch (Exception e)
            {
                paymentDate = new java.util.Date();
            }
        }
        else
        {
            paymentDate = new java.util.Date();
        }
        return toAjax(finSaleRecordService.addPayment(saleId, paymentAmount, paymentMethod, remark, paymentDate));
    }

    /**
     * 修改缴款记录
     */
    @RequiresPermissions("finance:sale:payment")
    @Log(title = "修改缴款", businessType = BusinessType.UPDATE)
    @PutMapping("/payment/{paymentId}")
    public AjaxResult updatePayment(@PathVariable Long paymentId, @RequestBody java.util.Map<String, Object> params)
    {
        java.math.BigDecimal paymentAmount = new java.math.BigDecimal(params.get("paymentAmount").toString());
        String paymentMethod = (String) params.get("paymentMethod");
        String remark = (String) params.get("remark");
        String paymentDateStr = (String) params.get("paymentDate");
        java.util.Date paymentDate = null;
        if (paymentDateStr != null && !paymentDateStr.isEmpty())
        {
            try
            {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                paymentDate = sdf.parse(paymentDateStr);
            }
            catch (Exception e)
            {
                paymentDate = new java.util.Date();
            }
        }
        else
        {
            paymentDate = new java.util.Date();
        }
        return toAjax(finSaleRecordService.updatePayment(paymentId, paymentAmount, paymentMethod, remark, paymentDate));
    }

    /**
     * 删除缴款记录
     */
    @RequiresPermissions("finance:sale:payment")
    @Log(title = "删除缴款", businessType = BusinessType.DELETE)
    @DeleteMapping("/payment/{paymentId}")
    public AjaxResult deletePayment(@PathVariable Long paymentId)
    {
        return toAjax(finSaleRecordService.deletePayment(paymentId));
    }
}
