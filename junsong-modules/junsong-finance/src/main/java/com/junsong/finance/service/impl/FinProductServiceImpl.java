package com.junsong.finance.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.finance.domain.FinProduct;
import com.junsong.finance.mapper.FinProductMapper;
import com.junsong.finance.service.IFinProductService;
import com.junsong.finance.util.CodeGenerator;

/**
 * 商品Service业务层处理
 * 
 * @author junsong
 */
@Service
public class FinProductServiceImpl implements IFinProductService
{
    @Autowired
    private FinProductMapper finProductMapper;

    /**
     * 查询商品
     * 
     * @param productId 商品主键
     * @return 商品
     */
    @Override
    public FinProduct selectFinProductByProductId(Long productId)
    {
        return finProductMapper.selectFinProductByProductId(productId);
    }

    /**
     * 查询商品列表
     * 
     * @param finProduct 商品
     * @return 商品
     */
    @Override
    public List<FinProduct> selectFinProductList(FinProduct finProduct)
    {
        return finProductMapper.selectFinProductList(finProduct);
    }

    /**
     * 新增商品
     * 
     * @param finProduct 商品
     * @return 结果
     */
    @Override
    public int insertFinProduct(FinProduct finProduct)
    {
        // 自动生成8位数字商品编码
        if (StringUtils.isEmpty(finProduct.getProductCode()))
        {
            finProduct.setProductCode(CodeGenerator.generateProductCode());
        }
        return finProductMapper.insertFinProduct(finProduct);
    }

    /**
     * 修改商品
     * 
     * @param finProduct 商品
     * @return 结果
     */
    @Override
    public int updateFinProduct(FinProduct finProduct)
    {
        return finProductMapper.updateFinProduct(finProduct);
    }

    /**
     * 批量删除商品
     * 
     * @param productIds 需要删除的商品主键
     * @return 结果
     */
    @Override
    public int deleteFinProductByProductIds(Long[] productIds)
    {
        return finProductMapper.deleteFinProductByProductIds(productIds);
    }

    /**
     * 删除商品信息
     * 
     * @param productId 商品主键
     * @return 结果
     */
    @Override
    public int deleteFinProductByProductId(Long productId)
    {
        return finProductMapper.deleteFinProductByProductId(productId);
    }

    /**
     * 校验商品编码是否唯一
     * 
     * @param finProduct 商品信息
     * @return 结果
     */
    @Override
    public boolean checkProductCodeUnique(FinProduct finProduct)
    {
        Long productId = StringUtils.isNull(finProduct.getProductId()) ? -1L : finProduct.getProductId();
        FinProduct info = finProductMapper.checkProductCodeUnique(finProduct.getProductCode());
        if (StringUtils.isNotNull(info) && info.getProductId().longValue() != productId.longValue())
        {
            return false;
        }
        return true;
    }
}
