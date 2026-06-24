package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinSupplier;

/**
 * 供应商Service接口
 * 
 * @author junsong
 */
public interface IFinSupplierService
{
    /**
     * 查询供应商
     * 
     * @param supplierId 供应商主键
     * @return 供应商
     */
    public FinSupplier selectFinSupplierBySupplierId(Long supplierId);

    /**
     * 查询供应商列表
     * 
     * @param finSupplier 供应商
     * @return 供应商集合
     */
    public List<FinSupplier> selectFinSupplierList(FinSupplier finSupplier);

    /**
     * 新增供应商
     * 
     * @param finSupplier 供应商
     * @return 结果
     */
    public int insertFinSupplier(FinSupplier finSupplier);

    /**
     * 修改供应商
     * 
     * @param finSupplier 供应商
     * @return 结果
     */
    public int updateFinSupplier(FinSupplier finSupplier);

    /**
     * 批量删除供应商
     * 
     * @param supplierIds 需要删除的供应商主键集合
     * @return 结果
     */
    public int deleteFinSupplierBySupplierIds(Long[] supplierIds);

    /**
     * 删除供应商信息
     * 
     * @param supplierId 供应商主键
     * @return 结果
     */
    public int deleteFinSupplierBySupplierId(Long supplierId);

    /**
     * 校验供应商编码是否唯一
     * 
     * @param finSupplier 供应商信息
     * @return 结果
     */
    public boolean checkSupplierCodeUnique(FinSupplier finSupplier);
}
