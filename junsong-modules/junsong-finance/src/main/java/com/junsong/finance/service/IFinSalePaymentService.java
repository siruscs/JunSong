package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinSalePayment;

/**
 * 缴款记录Service接口
 * 
 * @author junsong
 */
public interface IFinSalePaymentService
{
    /**
     * 查询缴款记录
     * 
     * @param paymentId 缴款记录主键
     * @return 缴款记录
     */
    public FinSalePayment selectFinSalePaymentByPaymentId(Long paymentId);

    /**
     * 查询缴款记录列表
     * 
     * @param finSalePayment 缴款记录
     * @return 缴款记录集合
     */
    public List<FinSalePayment> selectFinSalePaymentList(FinSalePayment finSalePayment);

    /**
     * 根据销售ID查询缴款记录列表
     * 
     * @param saleId 销售记录主键
     * @return 缴款记录集合
     */
    public List<FinSalePayment> selectFinSalePaymentBySaleId(Long saleId);

    /**
     * 新增缴款记录
     * 
     * @param finSalePayment 缴款记录
     * @return 结果
     */
    public int insertFinSalePayment(FinSalePayment finSalePayment);

    /**
     * 修改缴款记录
     * 
     * @param finSalePayment 缴款记录
     * @return 结果
     */
    public int updateFinSalePayment(FinSalePayment finSalePayment);

    /**
     * 批量删除缴款记录
     * 
     * @param paymentIds 需要删除的缴款记录主键集合
     * @return 结果
     */
    public int deleteFinSalePaymentByPaymentIds(Long[] paymentIds);

    /**
     * 删除缴款记录信息
     * 
     * @param paymentId 缴款记录主键
     * @return 结果
     */
    public int deleteFinSalePaymentByPaymentId(Long paymentId);

    /**
     * 校验缴款单号是否唯一
     * 
     * @param finSalePayment 缴款记录信息
     * @return 结果
     */
    public boolean checkPaymentNoUnique(FinSalePayment finSalePayment);
}
