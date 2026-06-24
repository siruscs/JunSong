package com.junsong.finance.mapper;

import java.util.List;
import com.junsong.finance.domain.FinPurchaseDetail;

/**
 * 进货单明细Mapper接口
 * 
 * @author junsong
 */
public interface FinPurchaseDetailMapper
{
    /**
     * 查询进货单明细
     * 
     * @param detailId 进货单明细主键
     * @return 进货单明细
     */
    public FinPurchaseDetail selectFinPurchaseDetailByDetailId(Long detailId);

    /**
     * 查询进货单明细列表
     * 
     * @param finPurchaseDetail 进货单明细
     * @return 进货单明细集合
     */
    public List<FinPurchaseDetail> selectFinPurchaseDetailList(FinPurchaseDetail finPurchaseDetail);

    /**
     * 根据进货单ID查询明细列表
     * 
     * @param purchaseId 进货单ID
     * @return 进货单明细集合
     */
    public List<FinPurchaseDetail> selectFinPurchaseDetailByPurchaseId(Long purchaseId);

    /**
     * 新增进货单明细
     * 
     * @param finPurchaseDetail 进货单明细
     * @return 结果
     */
    public int insertFinPurchaseDetail(FinPurchaseDetail finPurchaseDetail);

    /**
     * 修改进货单明细
     * 
     * @param finPurchaseDetail 进货单明细
     * @return 结果
     */
    public int updateFinPurchaseDetail(FinPurchaseDetail finPurchaseDetail);

    /**
     * 删除进货单明细
     * 
     * @param detailId 进货单明细主键
     * @return 结果
     */
    public int deleteFinPurchaseDetailByDetailId(Long detailId);

    /**
     * 批量删除进货单明细
     * 
     * @param detailIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinPurchaseDetailByDetailIds(Long[] detailIds);
}
