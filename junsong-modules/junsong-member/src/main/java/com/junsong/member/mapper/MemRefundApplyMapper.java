package com.junsong.member.mapper;

import com.junsong.member.domain.MemRefundApply;
import java.util.List;

public interface MemRefundApplyMapper
{
    List<MemRefundApply> selectMemRefundApplyList(MemRefundApply query);

    MemRefundApply selectMemRefundApplyById(Long id);

    MemRefundApply selectMemRefundApplyByRefundNo(String refundNo);

    int insertMemRefundApply(MemRefundApply refundApply);

    int updateMemRefundApply(MemRefundApply refundApply);

    int deleteMemRefundApplyByIds(Long[] ids);

    int updateWorkflowSnapshot(MemRefundApply refundApply);
}
