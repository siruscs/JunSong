package com.junsong.member.mapper;

import com.junsong.member.domain.MemPointsGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 积分物品Mapper接口
 */
@Mapper
public interface MemPointsGoodsMapper {

    /**
     * 查询积分物品列表
     *
     * @param goods 积分物品信息
     * @return 积分物品列表
     */
    List<MemPointsGoods> selectMemPointsGoodsList(MemPointsGoods goods);

    /**
     * 根据ID查询积分物品
     *
     * @param goodsId 积分物品ID
     * @return 积分物品信息
     */
    MemPointsGoods selectMemPointsGoodsById(Long goodsId);

    /**
     * 插入积分物品
     *
     * @param goods 积分物品信息
     * @return 影响行数
     */
    int insertMemPointsGoods(MemPointsGoods goods);

    /**
     * 更新积分物品
     *
     * @param goods 积分物品信息
     * @return 影响行数
     */
    int updateMemPointsGoods(MemPointsGoods goods);

    /**
     * 删除积分物品
     *
     * @param goodsId 积分物品ID
     * @return 影响行数
     */
    int deleteMemPointsGoodsById(Long goodsId);

    /**
     * 批量删除积分物品
     *
     * @param goodsIds 积分物品ID数组
     * @return 影响行数
     */
    int deleteMemPointsGoodsByIds(Long[] goodsIds);

    /**
     * 校验物品编号唯一性
     *
     * @param goodsNo 物品编号
     * @return 物品数量
     */
    int checkMemGoodsCodeUnique(@Param("goodsNo") String goodsNo);
}
