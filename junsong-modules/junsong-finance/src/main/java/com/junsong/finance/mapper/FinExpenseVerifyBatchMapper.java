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
    FinExpenseVerifyBatch selectByRequestId(@Param("tenantId") Long tenantId, @Param("deptId") Long deptId,
        @Param("requestId") String requestId);
    FinExpenseVerifyBatch selectByExpenseId(@Param("expenseId") Long expenseId, @Param("tenantId") Long tenantId,
        @Param("deptId") Long deptId);
    FinExpenseVerifyBatch selectBatchForUpdate(@Param("batchId") Long batchId, @Param("tenantId") Long tenantId,
        @Param("deptId") Long deptId);
    List<com.junsong.finance.domain.FinExpense> selectCurrentExpensesForUpdate(@Param("batchId") Long batchId,
        @Param("tenantId") Long tenantId, @Param("deptId") Long deptId);
    List<com.junsong.finance.domain.FinAdvance> selectCurrentAdvancesForUpdate(@Param("batchId") Long batchId,
        @Param("tenantId") Long tenantId, @Param("deptId") Long deptId);
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
    int restoreExpenseSnapshot(@Param("expenseId") Long expenseId, @Param("originalStatus") String originalStatus,
        @Param("originalAdvanceId") Long originalAdvanceId, @Param("tenantId") Long tenantId, @Param("deptId") Long deptId,
        @Param("expectedAdvanceId") Long expectedAdvanceId, @Param("verifyBy") String verifyBy, @Param("verifyTime") Date verifyTime);
    int restoreAdvanceSnapshot(@Param("advanceId") Long advanceId, @Param("originalStatus") String originalStatus,
        @Param("tenantId") Long tenantId, @Param("deptId") Long deptId,
        @Param("verifyBy") String verifyBy, @Param("verifyTime") Date verifyTime);
    int invalidateGeneratedAdvance(@Param("advanceId") Long advanceId, @Param("tenantId") Long tenantId,
        @Param("deptId") Long deptId, @Param("expectedStatus") String expectedStatus,
        @Param("verifyBy") String verifyBy, @Param("verifyTime") Date verifyTime);
    int countGeneratedAdvanceDownstreamReferences(@Param("advanceId") Long advanceId,
        @Param("batchId") Long batchId, @Param("tenantId") Long tenantId, @Param("deptId") Long deptId);
}
