package com.junsong.finance.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.finance.domain.FinStocktake;
import com.junsong.finance.domain.FinStocktakeItem;
import com.junsong.finance.domain.FinStocktakeHistory;

/**
 * 库存盘点 Mapper 接口。
 *
 * 安全契约：
 * 1. 所有读写显式带 tenant_id
 * 2. 列表查询包含授权部门集合过滤
 * 3. 状态更新使用乐观锁 version 谓词
 * 4. 锁查询按 dept_id, product_id 确定排序
 * 5. 插入使用 useGeneratedKeys 返回自增主键
 * 6. 禁止物理 DELETE（盘点数据不可物理删除）
 *
 * @author junsong
 */
public interface FinStocktakeMapper {

    // ===== 头表 =====

    int insertStocktake(FinStocktake stocktake);

    FinStocktake selectStocktakeById(@Param("tenantId") Long tenantId,
                                      @Param("stocktakeId") Long stocktakeId);

    FinStocktake selectStocktakeForUpdate(@Param("tenantId") Long tenantId,
                                           @Param("stocktakeId") Long stocktakeId);

    List<FinStocktake> listStocktakes(@Param("tenantId") Long tenantId,
                                       @Param("deptIds") List<Long> deptIds,
                                       @Param("status") String status,
                                       @Param("counterUserId") Long counterUserId);

    int countByTakeNo(@Param("tenantId") Long tenantId, @Param("takeNo") String takeNo);

    int updateStocktakeStatus(@Param("tenantId") Long tenantId,
                               @Param("stocktakeId") Long stocktakeId,
                               @Param("fromStatus") String fromStatus,
                               @Param("toStatus") String toStatus,
                               @Param("version") Integer version,
                               @Param("updateBy") String updateBy,
                               @Param("submittedBy") String submittedBy,
                               @Param("approvedBy") String approvedBy,
                               @Param("postedBy") String postedBy,
                               @Param("reversedBy") String reversedBy,
                               @Param("reversalReason") String reversalReason);

    /**
     * 分配盘点人和复盘人（仅 DRAFT 状态允许，带乐观锁谓词）。
     *
     * @param tenantId 租户ID
     * @param stocktakeId 盘点任务ID
     * @param counterUserId 盘点人用户ID
     * @param recountUserId 复盘人用户ID（可空）
     * @param version 当前版本号
     * @param updateBy 操作人
     * @return 影响行数（0 表示状态/版本不匹配）
     */
    int assignCounter(@Param("tenantId") Long tenantId,
                       @Param("stocktakeId") Long stocktakeId,
                       @Param("counterUserId") Long counterUserId,
                       @Param("recountUserId") Long recountUserId,
                       @Param("version") Integer version,
                       @Param("updateBy") String updateBy);

    // ===== 行表 =====

    int insertStocktakeItem(FinStocktakeItem item);

    FinStocktakeItem selectStocktakeItemById(@Param("tenantId") Long tenantId,
                                              @Param("itemId") Long itemId);

    List<FinStocktakeItem> listStocktakeItems(@Param("tenantId") Long tenantId,
                                               @Param("stocktakeId") Long stocktakeId);

    List<FinStocktakeItem> selectStocktakeItemsForUpdate(@Param("tenantId") Long tenantId,
                                                          @Param("stocktakeId") Long stocktakeId);

    int updateStocktakeItemCount(@Param("tenantId") Long tenantId,
                                  @Param("itemId") Long itemId,
                                  @Param("actualQuantity") Integer actualQuantity,
                                  @Param("reasonCode") String reasonCode,
                                  @Param("reason") String reason,
                                  @Param("attachments") String attachments,
                                  @Param("countIdempotencyKey") String countIdempotencyKey,
                                  @Param("countedBy") String countedBy,
                                  @Param("version") Integer version);

    int updateStocktakeItemRecount(@Param("tenantId") Long tenantId,
                                    @Param("itemId") Long itemId,
                                    @Param("recountQuantity") Integer recountQuantity,
                                    @Param("reasonCode") String reasonCode,
                                    @Param("reason") String reason,
                                    @Param("recountIdempotencyKey") String recountIdempotencyKey,
                                    @Param("recountedBy") String recountedBy,
                                    @Param("version") Integer version);

    int updateStocktakeItemFinal(@Param("tenantId") Long tenantId,
                                  @Param("itemId") Long itemId,
                                  @Param("finalQuantity") Integer finalQuantity,
                                  @Param("varianceQuantity") Integer varianceQuantity,
                                  @Param("unitCost") java.math.BigDecimal unitCost,
                                  @Param("varianceAmount") java.math.BigDecimal varianceAmount,
                                  @Param("reasonCode") String reasonCode,
                                  @Param("reason") String reason,
                                  @Param("movementQuantityAfterFreeze") Integer movementQuantityAfterFreeze,
                                  @Param("adjustedExpectedQuantity") Integer adjustedExpectedQuantity,
                                  @Param("version") Integer version);

    int updateStocktakeItemPostingRefs(@Param("tenantId") Long tenantId,
                                        @Param("itemId") Long itemId,
                                        @Param("stockLedgerId") Long stockLedgerId,
                                        @Param("costLedgerId") Long costLedgerId,
                                        @Param("version") Integer version);

    int updateStocktakeItemReverseRefs(@Param("tenantId") Long tenantId,
                                        @Param("itemId") Long itemId,
                                        @Param("reverseStockLedgerId") Long reverseStockLedgerId,
                                        @Param("reverseCostLedgerId") Long reverseCostLedgerId,
                                        @Param("version") Integer version);

    int countByCountIdempotencyKey(@Param("tenantId") Long tenantId,
                                    @Param("countIdempotencyKey") String countIdempotencyKey);

    // ===== 历史表 =====

    int insertStocktakeHistory(FinStocktakeHistory history);

    List<FinStocktakeHistory> listStocktakeHistory(@Param("tenantId") Long tenantId,
                                                     @Param("stocktakeId") Long stocktakeId);

    // ===== 冻结后 movement 汇总（Task 6 使用） =====

    Integer sumMovementAfterFreeze(@Param("tenantId") Long tenantId,
                                    @Param("deptId") Long deptId,
                                    @Param("productId") Long productId,
                                    @Param("freezeTime") java.util.Date freezeTime);
}
