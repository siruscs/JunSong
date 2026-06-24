package com.junsong.finance.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.finance.domain.FinCostAccounting;

/**
 * 成本核算Mapper接口
 *
 * @author junsong
 */
public interface FinCostAccountingMapper
{
    /**
     * 查询成本核算
     *
     * @param accountingId 成本核算主键
     * @return 成本核算
     */
    public FinCostAccounting selectFinCostAccountingByAccountingId(Long accountingId);

    /**
     * 查询成本核算列表
     *
     * @param finCostAccounting 成本核算
     * @return 成本核算集合
     */
    public List<FinCostAccounting> selectFinCostAccountingList(FinCostAccounting finCostAccounting);

    /**
     * 新增成本核算
     *
     * @param finCostAccounting 成本核算
     * @return 结果
     */
    public int insertFinCostAccounting(FinCostAccounting finCostAccounting);

    /**
     * 修改成本核算
     *
     * @param finCostAccounting 成本核算
     * @return 结果
     */
    public int updateFinCostAccounting(FinCostAccounting finCostAccounting);

    /**
     * 删除成本核算
     *
     * @param accountingId 成本核算主键
     * @return 结果
     */
    public int deleteFinCostAccountingByAccountingId(Long accountingId);

    /**
     * 批量删除成本核算
     *
     * @param accountingIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinCostAccountingByAccountingIds(Long[] accountingIds);

    /**
     * 校验核算单号是否唯一
     *
     * @param accountingNo 核算单号
     * @return 结果
     */
    public FinCostAccounting checkAccountingNoUnique(String accountingNo);

    /**
     * 统计今日核算单数量
     *
     * @return 结果
     */
    public int countTodayAccountings();

    /**
     * 统计总花销费用
     *
     * @return 结果
     */
    public BigDecimal sumTotalExpenseAmount();

    /**
     * 统计总进货金额
     *
     * @return 结果
     */
    public BigDecimal sumTotalPurchaseAmount();

    /**
     * 统计总销售金额
     *
     * @return 结果
     */
    public BigDecimal sumTotalSaleAmount();

    /**
     * 统计总缴款金额
     *
     * @return 结果
     */
    public BigDecimal sumTotalPaymentAmount();

    /**
     * 统计投资来源金额
     *
     * @return 结果
     */
    public BigDecimal sumTotalInvestAmount();

    /**
     * 统计未核销借支总额（当前借支）
     *
     * @return 结果
     */
    public BigDecimal sumCurrentAdvance();

    /**
     * 根据日期范围统计花销费用
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 结果
     */
    public BigDecimal sumExpenseAmountByDateRange(String startDate, String endDate);

    /**
     * 根据日期范围统计进货金额
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 结果
     */
    public BigDecimal sumPurchaseAmountByDateRange(String startDate, String endDate);

    /**
     * 根据日期范围统计销售金额
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 结果
     */
    public BigDecimal sumSaleAmountByDateRange(String startDate, String endDate);

    /**
     * 根据日期范围统计缴款金额
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 结果
     */
    public BigDecimal sumPaymentAmountByDateRange(String startDate, String endDate);

    /**
     * 根据日期范围统计投资金额
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 结果
     */
    public BigDecimal sumInvestAmountByDateRange(String startDate, String endDate);

    /**
     * 检查是否已经有非亏损核算记录
     *
     * @return 结果
     */
    public boolean checkAlreadyProfit();

    /**
     * 统计未核销费用数量
     *
     * @return 结果
     */
    public int countUnverifiedExpense(@Param("deptId") Long deptId);
}
