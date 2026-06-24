package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinProduct;

/**
 * 商品Service接口
 * 
 * @author junsong
 */
public interface IFinProductService
{
    /**
     * 查询商品
     * 
     * @param productId 商品主键
     * @return 商品
     */
    public FinProduct selectFinProductByProductId(Long productId);

    /**
     * 查询商品列表
     * 
     * @param finProduct 商品
     * @return 商品集合
     */
    public List<FinProduct> selectFinProductList(FinProduct finProduct);

    /**
     * 新增商品
     * 
     * @param finProduct 商品
     * @return 结果
     */
    public int insertFinProduct(FinProduct finProduct);

    /**
     * 修改商品
     * 
     * @param finProduct 商品
     * @return 结果
     */
    public int updateFinProduct(FinProduct finProduct);

    /**
     * 批量删除商品
     * 
     * @param productIds 需要删除的商品主键集合
     * @return 结果
     */
    public int deleteFinProductByProductIds(Long[] productIds);

    /**
     * 删除商品信息
     * 
     * @param productId 商品主键
     * @return 结果
     */
    public int deleteFinProductByProductId(Long productId);

    /**
     * 校验商品编码是否唯一
     * 
     * @param finProduct 商品信息
     * @return 结果
     */
    public boolean checkProductCodeUnique(FinProduct finProduct);
}
