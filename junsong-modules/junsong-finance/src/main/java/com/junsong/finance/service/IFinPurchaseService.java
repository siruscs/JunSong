package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinPurchase;

/**
 * 进货单Service接口
 * 
 * @author junsong
 */
public interface IFinPurchaseService
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
     * 批量删除进货单
     * 
     * @param purchaseIds 需要删除的进货单主键集合
     * @return 结果
     */
    public int deleteFinPurchaseByPurchaseIds(Long[] purchaseIds);

    /**
     * 删除进货单信息
     * 
     * @param purchaseId 进货单主键
     * @return 结果
     */
    public int deleteFinPurchaseByPurchaseId(Long purchaseId);

    /**
     * 校验进货单号是否唯一
     * 
     * @param finPurchase 进货单信息
     * @return 结果
     */
    public boolean checkPurchaseNoUnique(FinPurchase finPurchase);
}
