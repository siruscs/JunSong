package com.junsong.finance.mapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.junsong.finance.domain.FinSaleRecord;

/**
 * 销售记录Mapper接口
 * 
 * @author junsong
 */
public interface FinSaleRecordMapper
{
    /**
     * 查询销售记录
     * 
     * @param saleId 销售记录主键
     * @return 销售记录
     */
    public FinSaleRecord selectFinSaleRecordBySaleId(Long saleId);

    /**
     * 查询销售记录列表
     * 
     * @param finSaleRecord 销售记录
     * @return 销售记录集合
     */
    public List<FinSaleRecord> selectFinSaleRecordList(FinSaleRecord finSaleRecord);

    /**
     * 查询未缴清销售单（历史欠款）列表
     *
     * @param finSaleRecord 查询条件（门店/销售单号/商品名称/销售周期/状态）
     * @return 未缴清销售记录集合
     */
    public List<FinSaleRecord> selectReceivableList(FinSaleRecord finSaleRecord);

    /**
     * 统计指定周期内未缴清销售单笔数（结转检查用）
     */
    public int countReceivableByPeriodId(@Param("deptId") Long deptId, @Param("periodId") Long periodId);

    /**
     * 统计指定周期内未缴清销售单剩余应收总额（结转检查用）
     */
    public BigDecimal sumReceivableByPeriodId(@Param("deptId") Long deptId, @Param("periodId") Long periodId);

    /**
     * 新增销售记录
     * 
     * @param finSaleRecord 销售记录
     * @return 结果
     */
    public int insertFinSaleRecord(FinSaleRecord finSaleRecord);

    /**
     * 修改销售记录
     * 
     * @param finSaleRecord 销售记录
     * @return 结果
     */
    public int updateFinSaleRecord(FinSaleRecord finSaleRecord);

    /**
     * 仅更新销售单的已缴金额与缴款状态（跨周期补缴款场景）。
     * 不修改销售业务字段，允许在原销售周期已结转后调用。
     *
     * @param saleId 销售记录主键
     * @param paidAmount 累计已缴金额
     * @param status 缴款状态
     * @return 结果
     */
    public int updatePaidAmountAndStatus(@Param("saleId") Long saleId, @Param("paidAmount") BigDecimal paidAmount, @Param("status") String status);

    /**
     * 删除销售记录
     * 
     * @param saleId 销售记录主键
     * @return 结果
     */
    public int deleteFinSaleRecordBySaleId(Long saleId);

    /**
     * 批量删除销售记录
     * 
     * @param saleIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinSaleRecordBySaleIds(Long[] saleIds);

    public List<Map<String, Object>> selectSaleTrendStats(@Param("deptIds") List<Long> deptIds, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 统计销售笔数
     */
    public int countSaleRecords(@Param("deptIds") List<Long> deptIds, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 统计销售总数量（件数）
     */
    public BigDecimal sumSaleQuantity(@Param("deptIds") List<Long> deptIds, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 校验销售单号是否唯一
     * 
     * @param saleNo 销售单号
     * @return 结果
     */
    public FinSaleRecord checkSaleNoUnique(String saleNo);

    /**
     * 统计今日销售单数量
     *
     * @return 结果
     */
    public int countTodaySales();

    /**
     * 查询今日销售单号最大序号（含已删除记录，防止单号碰撞）
     *
     * @return 今日最大序号，无记录返回0
     */
    public int maxTodaySaleSeq();

    BigDecimal selectTodayTotalSales(@Param("deptIds") List<Long> deptIds);
    BigDecimal selectMonthTotalSales(@Param("deptIds") List<Long> deptIds);
    BigDecimal selectTodayTotalSalesForPrev(@Param("deptIds") List<Long> deptIds);
    BigDecimal selectMonthTotalSalesForPrev(@Param("deptIds") List<Long> deptIds);
    List<Map<String, Object>> selectSalesByDept(@Param("deptIds") List<Long> deptIds, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    List<Map<String, Object>> selectProductSalesRank(@Param("deptIds") List<Long> deptIds, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    BigDecimal selectMemberSales(@Param("deptIds") List<Long> deptIds, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    BigDecimal selectSeckillSales(@Param("deptIds") List<Long> deptIds, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /** 本期实收：fin_sale_payment 中 period_id = currentPeriodId 的缴款总额 */
    BigDecimal selectCurrentPeriodPaymentTotal(@Param("deptIds") List<Long> deptIds, @Param("periodId") Long periodId);

    /** 历史欠款回收：缴款 period_id = currentPeriodId 且对应销售 period_id <> currentPeriodId */
    BigDecimal selectHistoricalReceivableCollected(@Param("deptIds") List<Long> deptIds, @Param("periodId") Long periodId);

    /** 本期新增应收：sale.period_id = currentPeriodId 且 sale_amount > COALESCE(paid_amount,0) */
    BigDecimal selectCurrentPeriodNewReceivable(@Param("deptIds") List<Long> deptIds, @Param("periodId") Long periodId);

    /** 期末应收余额：所有未缴清销售单 sale_amount - COALESCE(paid_amount,0) 的总额 */
    BigDecimal selectEndingReceivableBalance(@Param("deptIds") List<Long> deptIds);

    /**
     * 行锁读取销售单（缴款并发保护）
     *
     * @param saleId 销售记录主键
     * @return 销售记录（带行锁）
     */
    public FinSaleRecord selectFinSaleRecordBySaleIdForUpdate(Long saleId);

    /** 逾期应收笔数：账龄超30天的未缴清销售单数量 */
    int countOverdueReceivable(@Param("deptIds") List<Long> deptIds);
}
