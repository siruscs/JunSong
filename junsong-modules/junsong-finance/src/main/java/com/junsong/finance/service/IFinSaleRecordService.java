package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinSaleRecord;

/**
 * 销售记录Service接口
 * 
 * @author junsong
 */
public interface IFinSaleRecordService
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
     * 批量删除销售记录
     * 
     * @param saleIds 需要删除的销售记录主键集合
     * @return 结果
     */
    public int deleteFinSaleRecordBySaleIds(Long[] saleIds);

    /**
     * 删除销售记录信息
     * 
     * @param saleId 销售记录主键
     * @return 结果
     */
    public int deleteFinSaleRecordBySaleId(Long saleId);

    /**
     * 校验销售单号是否唯一
     * 
     * @param finSaleRecord 销售记录信息
     * @return 结果
     */
    public boolean checkSaleNoUnique(FinSaleRecord finSaleRecord);

    /**
     * 添加缴款记录
     * 
     * @param saleId 销售记录主键
     * @param paymentAmount 缴款金额
     * @param paymentMethod 付款方式
     * @param remark 备注
     * @param paymentDate 缴款日期
     * @return 结果
     */
    public int addPayment(Long saleId, java.math.BigDecimal paymentAmount, String paymentMethod, String remark, java.util.Date paymentDate);

    /**
     * 修改缴款记录
     * 
     * @param paymentId 缴款记录主键
     * @param paymentAmount 缴款金额
     * @param paymentMethod 付款方式
     * @param remark 备注
     * @param paymentDate 缴款日期
     * @return 结果
     */
    public int updatePayment(Long paymentId, java.math.BigDecimal paymentAmount, String paymentMethod, String remark, java.util.Date paymentDate);

    /**
     * 删除缴款记录
     * 
     * @param paymentId 缴款记录主键
     * @return 结果
     */
    public int deletePayment(Long paymentId);
}
