package com.junsong.member.service.impl;

import com.junsong.common.core.workflow.WorkflowSubmitSnapshot;
import com.junsong.common.core.workflow.WorkflowSyncSnapshot;
import com.junsong.member.domain.MemRefundApply;
import com.junsong.member.mapper.MemRefundApplyMapper;
import com.junsong.member.service.IMemRefundApplyService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemRefundApplyServiceImpl implements IMemRefundApplyService
{
    @Autowired
    private MemRefundApplyMapper refundApplyMapper;

    @Override
    public List<MemRefundApply> selectMemRefundApplyList(MemRefundApply query)
    {
        return refundApplyMapper.selectMemRefundApplyList(query);
    }

    @Override
    public MemRefundApply selectMemRefundApplyById(Long id)
    {
        return refundApplyMapper.selectMemRefundApplyById(id);
    }

    @Override
    public MemRefundApply selectMemRefundApplyByRefundNo(String refundNo)
    {
        return refundApplyMapper.selectMemRefundApplyByRefundNo(refundNo);
    }

    @Override
    public int insertMemRefundApply(MemRefundApply refundApply)
    {
        if (refundApply.getRefundNo() == null || refundApply.getRefundNo().isBlank())
        {
            refundApply.setRefundNo(generateRefundNo());
        }
        if (refundApply.getWorkflowStatus() == null || refundApply.getWorkflowStatus().isBlank())
        {
            refundApply.setWorkflowStatus(MemRefundApply.STATUS_DRAFT);
        }
        if (refundApply.getDelFlag() == null || refundApply.getDelFlag().isBlank())
        {
            refundApply.setDelFlag("0");
        }
        return refundApplyMapper.insertMemRefundApply(refundApply);
    }

    @Override
    public int updateMemRefundApply(MemRefundApply refundApply)
    {
        return refundApplyMapper.updateMemRefundApply(refundApply);
    }

    @Override
    public int deleteMemRefundApplyByIds(Long[] ids)
    {
        return refundApplyMapper.deleteMemRefundApplyByIds(ids);
    }

    @Override
    public int markSubmitted(WorkflowSubmitSnapshot snapshot)
    {
        MemRefundApply next = new MemRefundApply();
        next.setId(snapshot.getId());
        next.setProcessInstanceId(snapshot.getProcessInstanceId());
        next.setProcessDefinitionKey(snapshot.getProcessDefinitionKey());
        next.setWorkflowStatus(MemRefundApply.STATUS_PENDING_STORE_APPROVAL);
        next.setCurrentTaskName(snapshot.getCurrentTaskName());
        next.setSubmitter(snapshot.getOperator());
        next.setSubmitTime(new Date());
        next.setUpdateBy(snapshot.getOperator());
        return refundApplyMapper.updateWorkflowSnapshot(next);
    }

    @Override
    public int withdraw(Long id, String username)
    {
        MemRefundApply next = new MemRefundApply();
        next.setId(id);
        next.setWorkflowStatus(MemRefundApply.STATUS_WITHDRAWN);
        next.setCurrentTaskName("已撤回");
        next.setUpdateBy(username);
        return refundApplyMapper.updateWorkflowSnapshot(next);
    }

    @Override
    public int syncWorkflowStatus(WorkflowSyncSnapshot snapshot)
    {
        MemRefundApply next = new MemRefundApply();
        next.setProcessInstanceId(snapshot.getProcessInstanceId());
        next.setWorkflowStatus(snapshot.getWorkflowStatus());
        next.setCurrentTaskName(snapshot.getCurrentTaskName());
        next.setUpdateBy(snapshot.getOperator());
        return refundApplyMapper.updateWorkflowSnapshot(next);
    }

    private String generateRefundNo()
    {
        return "MRA-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis() % 100000;
    }
}
