package com.junsong.finance.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.FinAdvance;
import com.junsong.finance.domain.vo.ExpenseVerifyVO;
import com.junsong.finance.domain.vo.ExpenseUnverifyVO;
import com.junsong.finance.constant.VerifyStatus;
import com.junsong.finance.service.IFinExpenseService;
import com.junsong.finance.service.IFinAdvanceService;
import com.junsong.finance.service.IFinExpenseVerificationService;

@RestController
@RequestMapping("/expense")
public class FinExpenseController extends BaseController
{
    @Autowired
    private IFinExpenseService finExpenseService;

    @Autowired
    private IFinAdvanceService finAdvanceService;

    @Autowired
    private IFinExpenseVerificationService finExpenseVerificationService;

    @RequiresPermissions("finance:expense:list")
    @GetMapping("/list")
    public TableDataInfo list(FinExpense finExpense)
    {
        if (finExpense.getDeptId() == null)
        {
            finExpense.setDeptId(SecurityUtils.getDeptId());
        }
        startPage();
        List<FinExpense> list = finExpenseService.selectFinExpenseList(finExpense);
        return getDataTable(list);
    }

    @RequiresPermissions("finance:expense:query")
    @GetMapping("/summary")
    public AjaxResult getSummary(@RequestParam(required = false) Long deptId)
    {
        return success(finExpenseService.getExpenseSummary(deptId));
    }

    @RequiresPermissions("finance:advance:list")
    @GetMapping("/unverifiedAdvances")
    public TableDataInfo getUnverifiedAdvances()
    {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null)
        {
            throw new ServiceException("无法确定当前门店，禁止查询借支单");
        }
        FinAdvance query = new FinAdvance();
        query.setStatus(VerifyStatus.UNVERIFIED);
        query.setDeptId(deptId);
        List<FinAdvance> list = finAdvanceService.selectFinAdvanceList(query);
        return getDataTable(list);
    }

    @RequiresPermissions("finance:expense:verify")
    @Log(title = "批量核销费用", businessType = BusinessType.UPDATE)
    @PutMapping("/batchVerify")
    public AjaxResult batchVerify(@Validated @RequestBody ExpenseVerifyVO verifyVO)
    {
        return success(finExpenseVerificationService.verify(verifyVO, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("finance:expense:unverify")
    @Log(title = "反核销费用", businessType = BusinessType.UPDATE)
    @PutMapping("/unverify/{batchId}")
    public AjaxResult unverify(@PathVariable Long batchId, @Validated @RequestBody ExpenseUnverifyVO request)
    {
        return success(finExpenseVerificationService.unverify(batchId, request, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("finance:expense:unverify")
    @GetMapping("/{expenseId}/capability")
    public AjaxResult capability(@PathVariable Long expenseId)
    {
        return success(finExpenseVerificationService.getCapability(expenseId));
    }

    @RequiresPermissions("finance:expense:verify")
    @GetMapping("/{expenseId}/verificationCandidate")
    public AjaxResult verificationCandidate(@PathVariable Long expenseId)
    {
        return success(finExpenseVerificationService.getVerificationCandidate(expenseId));
    }

    @RequiresPermissions("finance:expense:export")
    @Log(title = "费用记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FinExpense finExpense)
    {
        if (!SecurityUtils.isAdmin())
        {
            finExpense.setDeptId(SecurityUtils.getDeptId());
        }
        List<FinExpense> list = finExpenseService.selectFinExpenseList(finExpense);
        ExcelUtil<FinExpense> util = new ExcelUtil<FinExpense>(FinExpense.class);
        util.exportExcel(response, list, "费用记录数据");
    }

    @RequiresPermissions("finance:expense:query")
    @GetMapping(value = "/{expenseId}")
    public AjaxResult getInfo(@PathVariable Long expenseId)
    {
        return success(finExpenseService.selectFinExpenseByExpenseId(expenseId));
    }

    @RequiresPermissions("finance:expense:add")
    @Log(title = "费用记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinExpense finExpense)
    {
        finExpense.setDeptId(SecurityUtils.getDeptId());
        if (!finExpenseService.checkExpenseNoUnique(finExpense))
        {
            return error("新增费用记录'" + finExpense.getExpenseNo() + "'失败，费用单号已存在");
        }
        return toAjax(finExpenseService.insertFinExpense(finExpense));
    }

    @RequiresPermissions("finance:expense:edit")
    @Log(title = "费用记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinExpense finExpense)
    {
        finExpense.setDeptId(SecurityUtils.getDeptId());
        if (!finExpenseService.checkExpenseNoUnique(finExpense))
        {
            return error("修改费用记录'" + finExpense.getExpenseNo() + "'失败，费用单号已存在");
        }
        return toAjax(finExpenseService.updateFinExpense(finExpense));
    }

    @RequiresPermissions("finance:expense:remove")
    @Log(title = "费用记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{expenseIds}")
    public AjaxResult remove(@PathVariable Long[] expenseIds)
    {
        return toAjax(finExpenseService.deleteFinExpenseByExpenseIds(expenseIds));
    }

    @Deprecated
    @RequiresPermissions("finance:expense:verify")
    @Log(title = "核销费用", businessType = BusinessType.UPDATE)
    @PutMapping("/verify/{expenseId}")
    public AjaxResult verify(@PathVariable Long expenseId)
    {
        ExpenseVerifyVO request = new ExpenseVerifyVO();
        request.setExpenseIds(List.of(expenseId));
        request.setAdvanceIds(List.of());
        request.setRequestId("legacy-" + UUID.randomUUID());
        return success(finExpenseVerificationService.verify(request, SecurityUtils.getUsername()));
    }

    /**
     * 导入费用记录列表
     */
    @RequiresPermissions("finance:expense:import")
    @Log(title = "费用记录", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<FinExpense> util = new ExcelUtil<FinExpense>(FinExpense.class);
        List<FinExpense> expenseList = util.importExcel(file.getInputStream());
        String operName = SecurityUtils.getUsername();
        Long deptId = SecurityUtils.getDeptId();
        String message = finExpenseService.importExpense(expenseList, updateSupport, operName, deptId);
        return success(message);
    }

    /**
     * 下载费用记录导入模板
     */
    @RequiresPermissions("finance:expense:import")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws IOException
    {
        ExcelUtil<FinExpense> util = new ExcelUtil<FinExpense>(FinExpense.class);
        util.importTemplateExcel(response, "费用记录数据");
    }
}
