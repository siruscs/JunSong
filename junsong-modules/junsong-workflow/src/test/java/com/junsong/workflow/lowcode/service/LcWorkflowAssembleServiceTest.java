package com.junsong.workflow.lowcode.service;

import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.mapper.WfSysUserMapper;
import com.junsong.workflow.service.identity.DeptUserResolveService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LcWorkflowAssembleServiceTest
{
    @Test
    void assemblesStocktakeApproverUsernameFromFixedUser()
    {
        LcMetadataService metadataService = mock(LcMetadataService.class);
        DeptUserResolveService deptUserResolveService = mock(DeptUserResolveService.class);
        WfSysUserMapper sysUserMapper = mock(WfSysUserMapper.class);
        LcWorkflowAssembleService service = new LcWorkflowAssembleService(
                metadataService, deptUserResolveService, sysUserMapper);

        LcBizNodeAssignee approver = new LcBizNodeAssignee();
        approver.setBizCode("stocktake");
        approver.setTaskKey("Task_Approve");
        approver.setAssigneeSource("FIXED_USER");
        approver.setAssigneeValue("admin");
        approver.setProcessVarName("approverUsername");

        when(metadataService.selectFieldsByBizCode("stocktake")).thenReturn(List.of());
        when(metadataService.selectNodeAssigneesByBizCode("stocktake")).thenReturn(List.of(approver));
        when(metadataService.selectBranchRulesByBizCode("stocktake")).thenReturn(List.of());

        Map<String, Object> variables = service.assembleVariables("stocktake", Map.of(), "admin");

        assertEquals("admin", variables.get("approverUsername"));
    }

    @Test
    void convertsNumericFixedUserIdToUsernameBeforeFlowable()
    {
        LcMetadataService metadataService = mock(LcMetadataService.class);
        DeptUserResolveService deptUserResolveService = mock(DeptUserResolveService.class);
        WfSysUserMapper sysUserMapper = mock(WfSysUserMapper.class);
        LcWorkflowAssembleService service = new LcWorkflowAssembleService(
                metadataService, deptUserResolveService, sysUserMapper);

        LcBizNodeAssignee approver = new LcBizNodeAssignee();
        approver.setBizCode("stocktake");
        approver.setTaskKey("Task_Approve");
        approver.setAssigneeSource("FIXED_USER");
        approver.setAssigneeValue("1");
        approver.setProcessVarName("approverUsername");

        when(metadataService.selectFieldsByBizCode("stocktake")).thenReturn(List.of());
        when(metadataService.selectNodeAssigneesByBizCode("stocktake")).thenReturn(List.of(approver));
        when(metadataService.selectBranchRulesByBizCode("stocktake")).thenReturn(List.of());
        when(sysUserMapper.selectUserNameByUserId(1L)).thenReturn("admin");

        Map<String, Object> variables = service.assembleVariables("stocktake", Map.of(), "admin");

        assertEquals("admin", variables.get("approverUsername"));
    }

    @Test
    void formFieldUserAssigneeOverridesSameNamedRawProcessVariableWithUsername()
    {
        LcMetadataService metadataService = mock(LcMetadataService.class);
        DeptUserResolveService deptUserResolveService = mock(DeptUserResolveService.class);
        WfSysUserMapper sysUserMapper = mock(WfSysUserMapper.class);
        LcWorkflowAssembleService service = new LcWorkflowAssembleService(
                metadataService, deptUserResolveService, sysUserMapper);

        LcBizField counterField = new LcBizField();
        counterField.setBizCode("stocktake");
        counterField.setFieldKey("counter_user_id");
        counterField.setIsProcessVar("1");
        counterField.setProcessVarName("counterUsername");

        LcBizNodeAssignee counter = new LcBizNodeAssignee();
        counter.setBizCode("stocktake");
        counter.setTaskKey("Task_Count");
        counter.setAssigneeSource("FORM_FIELD_USER");
        counter.setAssigneeValue("counter_user_id");
        counter.setProcessVarName("counterUsername");

        when(metadataService.selectFieldsByBizCode("stocktake")).thenReturn(List.of(counterField));
        when(metadataService.selectNodeAssigneesByBizCode("stocktake")).thenReturn(List.of(counter));
        when(metadataService.selectBranchRulesByBizCode("stocktake")).thenReturn(List.of());
        when(sysUserMapper.selectUserNameByUserId(1L)).thenReturn("admin");

        Map<String, Object> variables = service.assembleVariables("stocktake", Map.of("counter_user_id", 1), "admin");

        assertEquals("admin", variables.get("counterUsername"));
    }

    @Test
    void stocktakeRecountBranchIsTrueWhenRecountUserIsProvided()
    {
        LcMetadataService metadataService = mock(LcMetadataService.class);
        DeptUserResolveService deptUserResolveService = mock(DeptUserResolveService.class);
        WfSysUserMapper sysUserMapper = mock(WfSysUserMapper.class);
        LcWorkflowAssembleService service = new LcWorkflowAssembleService(
                metadataService, deptUserResolveService, sysUserMapper);

        LcBizBranchRule needRecount = new LcBizBranchRule();
        needRecount.setBizCode("stocktake");
        needRecount.setGatewayKey("Gateway_NeedRecount");
        needRecount.setFieldKey("recount_user_id");
        needRecount.setOperator("NOT_EMPTY");
        needRecount.setTargetVarName("needRecount");

        when(metadataService.selectFieldsByBizCode("stocktake")).thenReturn(List.of());
        when(metadataService.selectNodeAssigneesByBizCode("stocktake")).thenReturn(List.of());
        when(metadataService.selectBranchRulesByBizCode("stocktake")).thenReturn(List.of(needRecount));

        Map<String, Object> variables = service.assembleVariables("stocktake", Map.of("recount_user_id", 103), "admin");

        assertEquals(Boolean.TRUE, variables.get("needRecount"));
    }
}
