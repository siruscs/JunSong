package com.junsong.finance.mapper;

import java.util.List;
import com.junsong.finance.domain.FinSupplier;

/**
 * 供应商Mapper接口
 * 
 * @author junsong
 */
public interface FinSupplierMapper
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
     * 删除供应商
     * 
     * @param supplierId 供应商主键
     * @return 结果
     */
    public int deleteFinSupplierBySupplierId(Long supplierId);

    /**
     * 批量删除供应商
     * 
     * @param supplierIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinSupplierBySupplierIds(Long[] supplierIds);

    /**
     * 校验供应商编码是否唯一
     * 
     * @param supplierCode 供应商编码
     * @return 结果
     */
    public FinSupplier checkSupplierCodeUnique(String supplierCode);
}
