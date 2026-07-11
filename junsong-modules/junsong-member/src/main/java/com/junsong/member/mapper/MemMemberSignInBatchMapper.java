package com.junsong.member.mapper;

import com.junsong.member.domain.MemMemberSignInBatch;

/**
 * 会员签到补录批次Mapper接口
 *
 * @author junsong
 */
public interface MemMemberSignInBatchMapper
{
    /**
     * 新增补录批次（回写 batchId）
     */
    public int insertBatch(MemMemberSignInBatch batch);

    /**
     * 根据批次ID查询批次详情
     */
    public MemMemberSignInBatch selectBatchById(Long batchId);

    /**
     * 更新补录批次汇总
     */
    public int updateBatch(MemMemberSignInBatch batch);
}
