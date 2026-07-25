package com.junsong.finance.mapper;

import org.apache.ibatis.annotations.Param;
import com.junsong.finance.domain.FinStockCostLayer;
import com.junsong.finance.domain.FinStockCostLedger;

/**
 * 库存成本层 Mapper。
 * 所有读写按 tenant + dept + product 隔离，与 fin_stock_position 使用相同租户键和锁顺序。
 *
 * @author junsong
 */
public interface FinStockCostLayerMapper {

    /**
     * 幂等创建成本层行（INSERT IGNORE），保证后续 FOR UPDATE 有行可锁。
     */
    int insertCostLayerIfAbsent(@Param("tenantId") Long tenantId,
                                 @Param("deptId") Long deptId,
                                 @Param("productId") Long productId);

    /**
     * 加行锁查询成本层（SELECT ... FOR UPDATE）。
     * 返回 null 表示行不存在（调用方应先 insertCostLayerIfAbsent）。
     */
    FinStockCostLayer selectCostLayerForUpdate(@Param("tenantId") Long tenantId,
                                                @Param("deptId") Long deptId,
                                                @Param("productId") Long productId);

    /**
     * 乐观锁更新成本层：更新平均成本、数量、金额，version + 1。
     * 影响行数必须为 1，否则视为并发冲突。
     */
    int updateCostLayer(@Param("tenantId") Long tenantId,
                        @Param("deptId") Long deptId,
                        @Param("productId") Long productId,
                        @Param("avgUnitCost") java.math.BigDecimal avgUnitCost,
                        @Param("stockQuantity") Integer stockQuantity,
                        @Param("stockAmount") java.math.BigDecimal stockAmount,
                        @Param("version") Integer version,
                        @Param("updateBy") String updateBy);

    /**
     * 写入一笔成本流水。
     */
    int insertCostLedger(FinStockCostLedger costLedger);

    /**
     * 按主键查询成本流水（冲销时用于判断原 costChangeType）。
     * 不加行锁（成本流水为不可变审计记录，仅读）。
     *
     * @param costLedgerId 成本流水ID
     * @return 成本流水，不存在返回 null
     */
    FinStockCostLedger selectCostLedgerById(@Param("costLedgerId") Long costLedgerId);
}
