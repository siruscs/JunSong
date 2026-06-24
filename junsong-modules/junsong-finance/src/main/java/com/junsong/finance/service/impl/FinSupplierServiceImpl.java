package com.junsong.finance.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.finance.domain.FinSupplier;
import com.junsong.finance.mapper.FinSupplierMapper;
import com.junsong.finance.service.IFinSupplierService;
import com.junsong.finance.util.CodeGenerator;

/**
 * 供应商Service业务层处理
 * 
 * @author junsong
 */
@Service
public class FinSupplierServiceImpl implements IFinSupplierService
{
    @Autowired
    private FinSupplierMapper finSupplierMapper;

    /**
     * 查询供应商
     * 
     * @param supplierId 供应商主键
     * @return 供应商
     */
    @Override
    public FinSupplier selectFinSupplierBySupplierId(Long supplierId)
    {
        return finSupplierMapper.selectFinSupplierBySupplierId(supplierId);
    }

    /**
     * 查询供应商列表
     * 
     * @param finSupplier 供应商
     * @return 供应商
     */
    @Override
    public List<FinSupplier> selectFinSupplierList(FinSupplier finSupplier)
    {
        return finSupplierMapper.selectFinSupplierList(finSupplier);
    }

    /**
     * 新增供应商
     * 
     * @param finSupplier 供应商
     * @return 结果
     */
    @Override
    public int insertFinSupplier(FinSupplier finSupplier)
    {
        // 自动生成6位数字供应商编码
        if (StringUtils.isEmpty(finSupplier.getSupplierCode()))
        {
            finSupplier.setSupplierCode(CodeGenerator.generateSupplierCode());
        }
        return finSupplierMapper.insertFinSupplier(finSupplier);
    }

    /**
     * 修改供应商
     * 
     * @param finSupplier 供应商
     * @return 结果
     */
    @Override
    public int updateFinSupplier(FinSupplier finSupplier)
    {
        return finSupplierMapper.updateFinSupplier(finSupplier);
    }

    /**
     * 批量删除供应商
     * 
     * @param supplierIds 需要删除的供应商主键
     * @return 结果
     */
    @Override
    public int deleteFinSupplierBySupplierIds(Long[] supplierIds)
    {
        return finSupplierMapper.deleteFinSupplierBySupplierIds(supplierIds);
    }

    /**
     * 删除供应商信息
     * 
     * @param supplierId 供应商主键
     * @return 结果
     */
    @Override
    public int deleteFinSupplierBySupplierId(Long supplierId)
    {
        return finSupplierMapper.deleteFinSupplierBySupplierId(supplierId);
    }

    /**
     * 校验供应商编码是否唯一
     * 
     * @param finSupplier 供应商信息
     * @return 结果
     */
    @Override
    public boolean checkSupplierCodeUnique(FinSupplier finSupplier)
    {
        Long supplierId = StringUtils.isNull(finSupplier.getSupplierId()) ? -1L : finSupplier.getSupplierId();
        FinSupplier info = finSupplierMapper.checkSupplierCodeUnique(finSupplier.getSupplierCode());
        if (StringUtils.isNotNull(info) && info.getSupplierId().longValue() != supplierId.longValue())
        {
            return false;
        }
        return true;
    }
}
