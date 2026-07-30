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
import org.springframework.web.bind.annotation.RequestHeader;
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
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.service.IFinSaleRecordService;
import com.junsong.finance.mapper.FinSalePaymentMapper;
import com.junsong.finance.domain.FinSalePayment;

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

    @Autowired
    private FinSalePaymentMapper finSalePaymentMapper;

    private boolean canOperatePayment(Long paymentId)
    {
        if (SecurityUtils.isAdmin())
        {
            return true;
        }
        FinSalePayment payment = finSalePaymentMapper.selectFinSalePaymentByPaymentId(paymentId);
        if (payment == null)
        {
            return false;
        }
        FinSaleRecord sale = finSaleRecordService.selectFinSaleRecordBySaleId(payment.getSaleId());
        return sale != null && java.util.Objects.equals(sale.getDeptId(), SecurityUtils.getDeptId());
    }

    /**
     * 查询销售记录列表
     */
    @RequiresPermissions("finance:sale:list")
    @GetMapping("/list")
    public TableDataInfo list(FinSaleRecord finSaleRecord)
    {
        finSaleRecord.setDeptId(SecurityUtils.getDeptId());
        startPage();
        List<FinSaleRecord> list = finSaleRecordService.selectFinSaleRecordList(finSaleRecord);
        return getDataTable(list);
    }

    /**
     * 查询未缴清销售单（历史欠款）列表
     */
    @RequiresPermissions("finance:sale:list")
    @GetMapping("/receivable/list")
    public TableDataInfo receivableList(FinSaleRecord finSaleRecord)
    {
        finSaleRecord.setDeptId(SecurityUtils.getDeptId());
        startPage();
        List<FinSaleRecord> list = finSaleRecordService.selectReceivableList(finSaleRecord);
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
        finSaleRecord.setDeptId(SecurityUtils.getDeptId());
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
        FinSaleRecord sale = finSaleRecordService.selectFinSaleRecordBySaleId(saleId);
        if (sale == null || (!SecurityUtils.isAdmin() && !java.util.Objects.equals(sale.getDeptId(), SecurityUtils.getDeptId())))
        {
            return error("销售记录不存在或无权访问");
        }
        return success(sale);
    }

    /**
     * 新增销售记录
     */
    @RequiresPermissions("finance:sale:add")
    @Log(title = "销售记录", businessType = BusinessType.INSERT)
    @Idempotent(scene = "sale:create", highRisk = true, ttlSeconds = 2592000)
    @PostMapping
    public AjaxResult add(@RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                          @Validated @RequestBody FinSaleRecord finSaleRecord)
    {
        finSaleRecord.setDeptId(SecurityUtils.getDeptId());
        finSaleRecord.setCreateBy(SecurityUtils.getUsername());
        finSaleRecord.setIdempotencyKey(idempotencyKey);
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
    @Idempotent(scene = "sale:update")
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinSaleRecord finSaleRecord)
    {
        FinSaleRecord existing = finSaleRecordService.selectFinSaleRecordBySaleId(finSaleRecord.getSaleId());
        if (existing == null || (!SecurityUtils.isAdmin() && !java.util.Objects.equals(existing.getDeptId(), SecurityUtils.getDeptId())))
        {
            return error("销售记录不存在或无权编辑");
        }
        finSaleRecord.setDeptId(SecurityUtils.getDeptId());
        finSaleRecord.setUpdateBy(SecurityUtils.getUsername());
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
        if (!SecurityUtils.isAdmin())
        {
            Long deptId = SecurityUtils.getDeptId();
            for (Long saleId : saleIds)
            {
                FinSaleRecord sale = finSaleRecordService.selectFinSaleRecordBySaleId(saleId);
                if (sale == null || !java.util.Objects.equals(sale.getDeptId(), deptId))
                {
                    return error("包含不存在或无权删除的销售记录");
                }
            }
        }
        return toAjax(finSaleRecordService.deleteFinSaleRecordBySaleIds(saleIds));
    }

    /**
     * 添加缴款记录
     */
    @RequiresPermissions("finance:sale:edit")
    @Log(title = "添加缴款", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "sale:payment", highRisk = true, ttlSeconds = 2592000)
    @PostMapping("/payment/{saleId}")
    public AjaxResult addPayment(@RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                 @PathVariable Long saleId, @RequestBody java.util.Map<String, Object> params)
    {
        FinSaleRecord existing = finSaleRecordService.selectFinSaleRecordBySaleId(saleId);
        if (existing == null || (!SecurityUtils.isAdmin() && !java.util.Objects.equals(existing.getDeptId(), SecurityUtils.getDeptId())))
        {
            return error("销售记录不存在或无权操作");
        }
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
        return toAjax(finSaleRecordService.addPayment(saleId, paymentAmount, paymentMethod, remark, paymentDate, idempotencyKey));
    }

    /**
     * 修改缴款记录
     */
    @RequiresPermissions("finance:sale:payment")
    @Log(title = "修改缴款", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "sale:payment:update", highRisk = true, ttlSeconds = 2592000)
    @PutMapping("/payment/{paymentId}")
    public AjaxResult updatePayment(@PathVariable Long paymentId, @RequestBody java.util.Map<String, Object> params)
    {
        if (!canOperatePayment(paymentId))
        {
            return error("缴款记录不存在或无权操作");
        }
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
        if (!canOperatePayment(paymentId))
        {
            return error("缴款记录不存在或无权操作");
        }
        return toAjax(finSaleRecordService.deletePayment(paymentId));
    }
}
