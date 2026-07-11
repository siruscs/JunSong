package com.junsong.finance.mapper;

import com.junsong.finance.domain.FinAdvanceVerifyDetail;
import com.junsong.finance.domain.FinExpenseVerifyBatch;
import com.junsong.finance.domain.FinExpenseVerifyDetail;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 费用核销批次持久化接口。 */
public interface FinExpenseVerifyBatchMapper
{
    FinExpenseVerifyBatch selectByRequestId(@Param("tenantId") Long tenantId, @Param("requestId") String requestId);
    FinExpenseVerifyBatch selectBatchForUpdate(@Param("batchId") Long batchId, @Param("tenantId") Long tenantId);
    int insertBatch(FinExpenseVerifyBatch batch);
    /** @param items non-null, non-empty expense detail list */
    int insertExpenseDetails(@Param("items") List<FinExpenseVerifyDetail> items);
    /** @param items non-null, non-empty advance detail list */
    int insertAdvanceDetails(@Param("items") List<FinAdvanceVerifyDetail> items);
    List<FinExpenseVerifyDetail> selectExpenseDetails(@Param("batchId") Long batchId,
        @Param("tenantId") Long tenantId, @Param("deptId") Long deptId);
    List<FinAdvanceVerifyDetail> selectAdvanceDetails(@Param("batchId") Long batchId,
        @Param("tenantId") Long tenantId, @Param("deptId") Long deptId);
    int markBatchReversed(@Param("batchId") Long batchId, @Param("tenantId") Long tenantId,
        @Param("deptId") Long deptId, @Param("version") Integer version,
        @Param("reverseBy") String reverseBy, @Param("reverseTime") Date reverseTime,
        @Param("reason") String reason, @Param("requestId") String requestId);
}
