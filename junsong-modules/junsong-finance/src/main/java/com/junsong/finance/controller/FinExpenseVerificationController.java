package com.junsong.finance.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.FinExpenseVerifyBatch;
import com.junsong.finance.service.IFinExpenseVerificationService;

/**
 * 费用核销批次记录 Controller
 * <p>
 * 从 FinExpenseController 独立出来，避免 /verificationBatches 路径
 * 与 FinExpenseController 的 /{expenseId} 通配路由产生 Spring MVC 路径冲突。
 */
@RestController
@RequestMapping("/verification-batch")
public class FinExpenseVerificationController extends BaseController
{
    @Autowired
    private IFinExpenseVerificationService finExpenseVerificationService;

    /**
     * 查询核销批次记录列表
     */
    @RequiresPermissions("finance:expense:verificationRecord:list")
    @GetMapping("/list")
    public TableDataInfo list(FinExpenseVerifyBatch query)
    {
        startPage();
        List<FinExpenseVerifyBatch> list = finExpenseVerificationService.selectBatchList(query);
        return getDataTable(list);
    }

    /**
     * 查询核销批次详情
     */
    @RequiresPermissions("finance:expense:verificationRecord:list")
    @GetMapping("/{batchId}")
    public AjaxResult detail(@PathVariable Long batchId)
    {
        return success(finExpenseVerificationService.getBatchDetail(batchId));
    }
}
