package com.junsong.system.service.impl;

import com.junsong.system.domain.SysStoreOpening;
import com.junsong.system.mapper.SysStoreOpeningMapper;
import com.junsong.system.service.ISysStoreOpeningService;
import com.junsong.common.core.workflow.WorkflowSubmitSnapshot;
import com.junsong.common.core.workflow.WorkflowSyncSnapshot;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysStoreOpeningServiceImpl implements ISysStoreOpeningService
{
    @Autowired
    private SysStoreOpeningMapper storeOpeningMapper;

    @Override
    public List<SysStoreOpening> selectStoreOpeningList(SysStoreOpening query)
    {
        return storeOpeningMapper.selectStoreOpeningList(query);
    }

    @Override
    public SysStoreOpening selectStoreOpeningById(Long id)
    {
        return storeOpeningMapper.selectStoreOpeningById(id);
    }

    @Override
    public SysStoreOpening selectStoreOpeningByOrderNo(String orderNo)
    {
        return storeOpeningMapper.selectStoreOpeningByOrderNo(orderNo);
    }

    @Override
    public int insertStoreOpening(SysStoreOpening storeOpening)
    {
        if (storeOpening.getOrderNo() == null || storeOpening.getOrderNo().isBlank()) {
            storeOpening.setOrderNo(generateOrderNo());
        }
        if (storeOpening.getWorkflowStatus() == null || storeOpening.getWorkflowStatus().isBlank()) {
            storeOpening.setWorkflowStatus(SysStoreOpening.STATUS_DRAFT);
        }
        if (storeOpening.getDelFlag() == null || storeOpening.getDelFlag().isBlank()) {
            storeOpening.setDelFlag("0");
        }
        return storeOpeningMapper.insertStoreOpening(storeOpening);
    }

    @Override
    public int updateStoreOpening(SysStoreOpening storeOpening)
    {
        return storeOpeningMapper.updateStoreOpening(storeOpening);
    }

    @Override
    public int deleteStoreOpeningById(Long id)
    {
        return storeOpeningMapper.deleteStoreOpeningById(id);
    }

    @Override
    public int markSubmitted(Long id, String processInstanceId, String processDefinitionKey, String currentTaskName, String username)
    {
        WorkflowSubmitSnapshot snapshot = new WorkflowSubmitSnapshot();
        snapshot.setId(id);
        snapshot.setProcessInstanceId(processInstanceId);
        snapshot.setProcessDefinitionKey(processDefinitionKey);
        snapshot.setCurrentTaskName(currentTaskName);
        snapshot.setOperator(username);
        return markSubmitted(snapshot);
    }

    @Override
    public int markSubmitted(WorkflowSubmitSnapshot snapshot)
    {
        SysStoreOpening next = new SysStoreOpening();
        next.setId(snapshot.getId());
        next.setProcessInstanceId(snapshot.getProcessInstanceId());
        next.setProcessDefinitionKey(snapshot.getProcessDefinitionKey());
        next.setWorkflowStatus(SysStoreOpening.STATUS_PENDING_REGION_APPROVAL);
        next.setCurrentTaskName(snapshot.getCurrentTaskName());
        next.setSubmitter(snapshot.getOperator());
        next.setSubmitTime(new Date());
        next.setUpdateBy(snapshot.getOperator());
        return storeOpeningMapper.updateWorkflowSnapshot(next);
    }

    @Override
    public int withdraw(Long id, String username)
    {
        SysStoreOpening next = new SysStoreOpening();
        next.setId(id);
        next.setWorkflowStatus(SysStoreOpening.STATUS_WITHDRAWN);
        next.setCurrentTaskName("已撤回");
        next.setUpdateBy(username);
        return storeOpeningMapper.updateWorkflowSnapshot(next);
    }

    @Override
    public int syncWorkflowStatus(String processInstanceId, String workflowStatus, String currentTaskName, String username)
    {
        WorkflowSyncSnapshot snapshot = new WorkflowSyncSnapshot();
        snapshot.setProcessInstanceId(processInstanceId);
        snapshot.setWorkflowStatus(workflowStatus);
        snapshot.setCurrentTaskName(currentTaskName);
        snapshot.setOperator(username);
        return syncWorkflowStatus(snapshot);
    }

    @Override
    public int syncWorkflowStatus(WorkflowSyncSnapshot snapshot)
    {
        SysStoreOpening next = new SysStoreOpening();
        next.setProcessInstanceId(snapshot.getProcessInstanceId());
        next.setWorkflowStatus(snapshot.getWorkflowStatus());
        next.setCurrentTaskName(snapshot.getCurrentTaskName());
        next.setUpdateBy(snapshot.getOperator());
        return storeOpeningMapper.updateWorkflowSnapshot(next);
    }

    private String generateOrderNo()
    {
        return "SOA-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis() % 100000;
    }
}
