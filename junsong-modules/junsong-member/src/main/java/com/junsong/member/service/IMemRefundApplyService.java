package com.junsong.member.service;

import com.junsong.common.core.workflow.WorkflowSubmitSnapshot;
import com.junsong.common.core.workflow.WorkflowSyncSnapshot;
import com.junsong.member.domain.MemRefundApply;
import java.util.List;

public interface IMemRefundApplyService
{
    List<MemRefundApply> selectMemRefundApplyList(MemRefundApply query);

    MemRefundApply selectMemRefundApplyById(Long id);

    MemRefundApply selectMemRefundApplyByRefundNo(String refundNo);

    int insertMemRefundApply(MemRefundApply refundApply);

    int updateMemRefundApply(MemRefundApply refundApply);

    int deleteMemRefundApplyByIds(Long[] ids);

    int markSubmitted(WorkflowSubmitSnapshot snapshot);

    int withdraw(Long id, String username);

    int syncWorkflowStatus(WorkflowSyncSnapshot snapshot);
}
