package com.junsong.finance.mapper;

import java.util.List;
import com.junsong.finance.domain.FinSalePayment;

/**
 * 缴款记录Mapper接口
 * 
 * @author junsong
 */
public interface FinSalePaymentMapper
{
    /**
     * 查询缴款记录
     * 
     * @param paymentId 缴款记录主键
     * @return 缴款记录
     */
    public FinSalePayment selectFinSalePaymentByPaymentId(Long paymentId);

    /**
     * 根据销售ID查询缴款记录列表
     * 
     * @param saleId 销售记录主键
     * @return 缴款记录集合
     */
    public List<FinSalePayment> selectFinSalePaymentBySaleId(Long saleId);

    /**
     * 查询缴款记录列表
     * 
     * @param finSalePayment 缴款记录
     * @return 缴款记录集合
     */
    public List<FinSalePayment> selectFinSalePaymentList(FinSalePayment finSalePayment);

    /**
     * 新增缴款记录
     * 
     * @param finSalePayment 缴款记录
     * @return 结果
     */
    public int insertFinSalePayment(FinSalePayment finSalePayment);

    /**
     * 批量新增缴款记录
     * 
     * @param list 缴款记录列表
     * @return 结果
     */
    public int batchFinSalePayment(List<FinSalePayment> list);

    /**
     * 修改缴款记录
     * 
     * @param finSalePayment 缴款记录
     * @return 结果
     */
    public int updateFinSalePayment(FinSalePayment finSalePayment);

    /**
     * 删除缴款记录
     * 
     * @param paymentId 缴款记录主键
     * @return 结果
     */
    public int deleteFinSalePaymentByPaymentId(Long paymentId);

    /**
     * 批量删除缴款记录
     * 
     * @param paymentIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinSalePaymentByPaymentIds(Long[] paymentIds);

    /**
     * 删除指定销售记录的所有缴款记录
     * 
     * @param saleId 销售记录主键
     * @return 结果
     */
    public int deleteFinSalePaymentBySaleId(Long saleId);

    /**
     * 批量删除指定销售记录的所有缴款记录
     * 
     * @param saleIds 需要删除的销售记录主键集合
     * @return 结果
     */
    public int deleteFinSalePaymentBySaleIds(Long[] saleIds);

    /**
     * 校验缴款单号是否唯一
     * 
     * @param paymentNo 缴款单号
     * @return 结果
     */
    public FinSalePayment checkPaymentNoUnique(String paymentNo);

    /**
     * 统计今天的缴款记录数量
     * 
     * @return 数量
     */
    public int countTodayPayments();

    /**
     * 根据销售ID统计缴款金额
     *
     * @param saleId 销售记录主键
     * @return 缴款金额合计
     */
    public BigDecimal sumPaymentAmountBySaleId(Long saleId);
}
