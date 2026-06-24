package com.junsong.member.controller;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.core.workflow.WorkflowSubmitSnapshot;
import com.junsong.common.core.workflow.WorkflowSyncSnapshot;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.MemRefundApply;
import com.junsong.member.service.IMemRefundApplyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refund")
public class MemRefundApplyController extends BaseController
{
    @Autowired
    private IMemRefundApplyService refundApplyService;

    @RequiresPermissions("member:refund:list")
    @GetMapping("/list")
    public TableDataInfo list(MemRefundApply query)
    {
        startPage();
        List<MemRefundApply> list = refundApplyService.selectMemRefundApplyList(query);
        return getDataTable(list);
    }

    @RequiresPermissions("member:refund:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(refundApplyService.selectMemRefundApplyById(id));
    }

    @GetMapping("/no/{refundNo}")
    public AjaxResult getByRefundNo(@PathVariable("refundNo") String refundNo)
    {
        return success(refundApplyService.selectMemRefundApplyByRefundNo(refundNo));
    }

    @RequiresPermissions("member:refund:add")
    @Log(title = "会员退款申请", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody MemRefundApply refundApply)
    {
        refundApply.setCreateBy(SecurityUtils.getUsername());
        refundApplyService.insertMemRefundApply(refundApply);
        return success(refundApplyService.selectMemRefundApplyById(refundApply.getId()));
    }

    @RequiresPermissions("member:refund:edit")
    @Log(title = "会员退款申请", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody MemRefundApply refundApply)
    {
        refundApply.setUpdateBy(SecurityUtils.getUsername());
        refundApplyService.updateMemRefundApply(refundApply);
        return success(refundApplyService.selectMemRefundApplyById(refundApply.getId()));
    }

    @RequiresPermissions("member:refund:remove")
    @Log(title = "会员退款申请", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(refundApplyService.deleteMemRefundApplyByIds(ids));
    }

    @RequiresPermissions("member:refund:submit")
    @Log(title = "会员退款申请", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/submit")
    public AjaxResult submit(@PathVariable("id") Long id, @RequestBody SubmitPayload payload)
    {
        WorkflowSubmitSnapshot snapshot = new WorkflowSubmitSnapshot();
        snapshot.setId(id);
        snapshot.setProcessInstanceId(payload.processInstanceId);
        snapshot.setProcessDefinitionKey(payload.processDefinitionKey);
        snapshot.setCurrentTaskName(payload.currentTaskName);
        snapshot.setOperator(SecurityUtils.getUsername());
        return toAjax(refundApplyService.markSubmitted(snapshot));
    }

    @RequiresPermissions("member:refund:withdraw")
    @Log(title = "会员退款申请", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/withdraw")
    public AjaxResult withdraw(@PathVariable("id") Long id)
    {
        return toAjax(refundApplyService.withdraw(id, SecurityUtils.getUsername()));
    }

    @InnerAuth
    @PostMapping("/workflow/sync")
    public R<Boolean> syncWorkflowStatus(@RequestBody WorkflowSyncPayload payload)
    {
        WorkflowSyncSnapshot snapshot = new WorkflowSyncSnapshot();
        snapshot.setProcessInstanceId(payload.processInstanceId);
        snapshot.setWorkflowStatus(payload.workflowStatus);
        snapshot.setCurrentTaskName(payload.currentTaskName);
        snapshot.setOperator(payload.operator == null ? "workflow" : payload.operator);
        return R.ok(refundApplyService.syncWorkflowStatus(snapshot) > 0);
    }

    public static class SubmitPayload
    {
        public String processInstanceId;
        public String processDefinitionKey;
        public String currentTaskName;
    }

    public static class WorkflowSyncPayload
    {
        public String processInstanceId;
        public String workflowStatus;
        public String currentTaskName;
        public String operator;
    }
}
