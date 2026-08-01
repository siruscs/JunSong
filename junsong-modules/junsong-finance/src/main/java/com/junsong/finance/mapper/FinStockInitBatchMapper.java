package com.junsong.finance.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.finance.domain.FinStockInitBatch;
import com.junsong.finance.domain.FinStockInitItem;

/**
 * 期初库存 Mapper 接口。
 *
 * 安全契约：
 * 1. 所有读写显式带 tenant_id
 * 2. 列表查询包含授权部门集合过滤
 * 3. 状态更新使用乐观锁 version 谓词
 * 4. 锁查询按 dept_id, product_id 确定排序
 * 5. 插入使用 useGeneratedKeys 返回自增主键
 * 6. 禁止物理 DELETE（期初数据不可物理删除）
 *
 * @author junsong
 */
public interface FinStockInitBatchMapper {

    // ===== 头表 =====

    int insertBatch(FinStockInitBatch batch);

    FinStockInitBatch selectBatchById(@Param("tenantId") Long tenantId,
                                       @Param("batchId") Long batchId);

    FinStockInitBatch selectBatchForUpdate(@Param("tenantId") Long tenantId,
                                             @Param("batchId") Long batchId);

    FinStockInitBatch selectBatchByPostIdempotencyKey(@Param("tenantId") Long tenantId,
                                                       @Param("postIdempotencyKey") String postIdempotencyKey);

    List<FinStockInitBatch> listBatches(@Param("tenantId") Long tenantId,
                                          @Param("deptIds") List<Long> deptIds,
                                          @Param("status") String status,
                                          @Param("batchNo") String batchNo);

    int countByBatchNo(@Param("tenantId") Long tenantId, @Param("batchNo") String batchNo);

    int countByPostIdempotencyKey(@Param("tenantId") Long tenantId,
                                   @Param("postIdempotencyKey") String postIdempotencyKey);

    int updateBatchStatus(@Param("tenantId") Long tenantId,
                           @Param("batchId") Long batchId,
                           @Param("fromStatus") String fromStatus,
                           @Param("toStatus") String toStatus,
                           @Param("version") Integer version,
                           @Param("updateBy") String updateBy,
                           @Param("submittedBy") String submittedBy,
                           @Param("approvedBy") String approvedBy,
                           @Param("postedBy") String postedBy,
                           @Param("postIdempotencyKey") String postIdempotencyKey);

    int markBatchDeleted(@Param("tenantId") Long tenantId, @Param("batchId") Long batchId,
                         @Param("version") Integer version, @Param("updateBy") String updateBy);

    // ===== 行表 =====

    int insertBatchItem(FinStockInitItem item);

    List<FinStockInitItem> listBatchItems(@Param("tenantId") Long tenantId,
                                            @Param("batchId") Long batchId);

    List<FinStockInitItem> selectBatchItemsForUpdate(@Param("tenantId") Long tenantId,
                                                       @Param("batchId") Long batchId);

    int updateBatchItemPostingRefs(@Param("tenantId") Long tenantId,
                                    @Param("itemId") Long itemId,
                                    @Param("stockLedgerId") Long stockLedgerId,
                                    @Param("costLedgerId") Long costLedgerId,
                                    @Param("version") Integer version);
}
