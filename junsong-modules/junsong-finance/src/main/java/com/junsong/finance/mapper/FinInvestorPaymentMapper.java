package com.junsong.finance.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.finance.domain.FinInvestorPayment;

/**
 * 投资人返款Mapper接口
 * 
 * @author junsong
 */
public interface FinInvestorPaymentMapper
{
    /**
     * 查询投资人返款
     * 
     * @param paymentId 投资人返款主键
     * @return 投资人返款
     */
    public FinInvestorPayment selectFinInvestorPaymentByPaymentId(Long paymentId);

    /**
     * 查询投资人返款列表
     * 
     * @param finInvestorPayment 投资人返款
     * @return 投资人返款集合
     */
    public List<FinInvestorPayment> selectFinInvestorPaymentList(FinInvestorPayment finInvestorPayment);

    /**
     * 新增投资人返款
     * 
     * @param finInvestorPayment 投资人返款
     * @return 结果
     */
    public int insertFinInvestorPayment(FinInvestorPayment finInvestorPayment);

    /**
     * 修改投资人返款
     * 
     * @param finInvestorPayment 投资人返款
     * @return 结果
     */
    public int updateFinInvestorPayment(FinInvestorPayment finInvestorPayment);

    /**
     * 删除投资人返款
     * 
     * @param paymentId 投资人返款主键
     * @return 结果
     */
    public int deleteFinInvestorPaymentByPaymentId(Long paymentId);

    /**
     * 批量删除投资人返款
     * 
     * @param paymentIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinInvestorPaymentByPaymentIds(Long[] paymentIds);
    public int deleteAutoInvestorPaymentByShareId(@Param("shareId") Long shareId);

    /**
     * 校验返款单号是否唯一
     * 
     * @param paymentNo 返款单号
     * @return 结果
     */
    public FinInvestorPayment checkPaymentNoUnique(String paymentNo);

    /**
     * 统计今日返款单数量
     * 
     * @return 结果
     */
    public int countTodayInvestorPayments();

    /**
     * 统计备用金总额
     * 
     * @return 结果
     */
    public java.math.BigDecimal sumReserveFund();

    /**
     * 统计已核销费用总金额
     * 
     * @return 结果
     */
    public java.math.BigDecimal sumVerifiedExpenses();
    public java.math.BigDecimal sumVerifiedExpensesByDeptId(@Param("deptId") Long deptId);

    /**
     * 统计投资来源金额
     * 
     * @return 结果
     */
    public java.math.BigDecimal sumTotalInvestAmount();

    /**
     * 统计总返款金额
     * 
     * @return 结果
     */
    public java.math.BigDecimal sumTotalReturnAmount();
    public java.math.BigDecimal sumTotalReturnAmountByDeptId(@Param("deptId") Long deptId);
}
