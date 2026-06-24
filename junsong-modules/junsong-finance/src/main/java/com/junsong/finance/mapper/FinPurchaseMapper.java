package com.junsong.finance.mapper;

import java.util.List;
import com.junsong.finance.domain.FinPurchase;
import com.junsong.finance.domain.FinPurchaseDetail;

/**
 * 进货单Mapper接口
 * 
 * @author junsong
 */
public interface FinPurchaseMapper
{
    /**
     * 查询进货单
     * 
     * @param purchaseId 进货单主键
     * @return 进货单
     */
    public FinPurchase selectFinPurchaseByPurchaseId(Long purchaseId);

    /**
     * 查询进货单列表
     * 
     * @param finPurchase 进货单
     * @return 进货单集合
     */
    public List<FinPurchase> selectFinPurchaseList(FinPurchase finPurchase);

    /**
     * 新增进货单
     * 
     * @param finPurchase 进货单
     * @return 结果
     */
    public int insertFinPurchase(FinPurchase finPurchase);

    /**
     * 修改进货单
     * 
     * @param finPurchase 进货单
     * @return 结果
     */
    public int updateFinPurchase(FinPurchase finPurchase);

    /**
     * 删除进货单
     * 
     * @param purchaseId 进货单主键
     * @return 结果
     */
    public int deleteFinPurchaseByPurchaseId(Long purchaseId);

    /**
     * 批量删除进货单
     * 
     * @param purchaseIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinPurchaseByPurchaseIds(Long[] purchaseIds);

    /**
     * 批量删除进货单明细
     * 
     * @param purchaseIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinPurchaseDetailByPurchaseIds(Long[] purchaseIds);
    
    /**
     * 批量新增进货单明细
     * 
     * @param finPurchaseDetailList 进货单明细列表
     * @return 结果
     */
    public int batchFinPurchaseDetail(List<FinPurchaseDetail> finPurchaseDetailList);
    

    /**
     * 通过进货单主键删除进货单明细信息
     * 
     * @param purchaseId 进货单ID
     * @return 结果
     */
    public int deleteFinPurchaseDetailByPurchaseId(Long purchaseId);

    /**
     * 校验进货单号是否唯一
     * 
     * @param purchaseNo 进货单号
     * @return 结果
     */
    public FinPurchase checkPurchaseNoUnique(String purchaseNo);
    
    /**
     * 统计今天的进货单数量
     * 
     * @return 数量
     */
    public int countTodayPurchases();
}
