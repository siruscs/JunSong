package com.junsong.member.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.member.domain.MemConfigSyncBatch;
import com.junsong.member.domain.MemConfigSyncDetail;

public interface MemConfigSyncMapper
{
    int insertBatch(MemConfigSyncBatch batch);
    int insertDetail(MemConfigSyncDetail detail);
    MemConfigSyncBatch selectBatch(@Param("tenantId") Long tenantId, @Param("batchId") Long batchId);
    MemConfigSyncBatch selectBatchByIdempotency(@Param("tenantId") Long tenantId,
                                                @Param("idempotencyKey") String idempotencyKey);
    List<MemConfigSyncDetail> selectDetails(@Param("tenantId") Long tenantId, @Param("batchId") Long batchId);
    int updateDetailDecision(@Param("tenantId") Long tenantId, @Param("batchId") Long batchId,
                             @Param("detailId") Long detailId, @Param("decision") String decision);
    int updateDetailResult(MemConfigSyncDetail detail);
    int updateBatchStatus(@Param("tenantId") Long tenantId, @Param("batchId") Long batchId,
                          @Param("status") String status, @Param("updateBy") String updateBy);
}
