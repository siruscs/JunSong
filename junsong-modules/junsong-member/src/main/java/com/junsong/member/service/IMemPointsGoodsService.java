package com.junsong.member.service;

import java.util.List;
import com.junsong.member.domain.MemPointsGoods;

/**
 * 积分物品Service接口
 */
public interface IMemPointsGoodsService {

    /**
     * 查询积分物品
     *
     * @param id 积分物品ID
     * @return 积分物品
     */
    public MemPointsGoods selectMemPointsGoodsById(Long id);

    /**
     * 查询积分物品列表
     *
     * @param memPointsGoods 积分物品
     * @return 积分物品集合
     */
    public List<MemPointsGoods> selectMemPointsGoodsList(MemPointsGoods memPointsGoods);

    /**
     * 新增积分物品
     *
     * @param memPointsGoods 积分物品
     * @return 结果
     */
    public int insertMemPointsGoods(MemPointsGoods memPointsGoods);

    /**
     * 修改积分物品
     *
     * @param memPointsGoods 积分物品
     * @return 结果
     */
    public int updateMemPointsGoods(MemPointsGoods memPointsGoods);

    /**
     * 删除积分物品
     *
     * @param id 积分物品ID
     * @return 结果
     */
    public int deleteMemPointsGoodsById(Long id);

    /**
     * 批量删除积分物品
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMemPointsGoodsByIds(Long[] ids);

    /**
     * 校验积分物品编号是否唯一
     *
     * @param memPointsGoods 积分物品
     * @return 结果
     */
    public boolean checkMemPointsGoodsNoUnique(MemPointsGoods memPointsGoods);
}
