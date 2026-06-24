package com.junsong.member.mapper;

import com.junsong.member.domain.MemPointsExchange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 积分兑换Mapper接口
 */
@Mapper
public interface MemPointsExchangeMapper {

    /**
     * 查询积分兑换列表
     *
     * @param exchange 积分兑换信息
     * @return 积分兑换列表
     */
    List<MemPointsExchange> selectMemPointsExchangeList(MemPointsExchange exchange);

    /**
     * 根据ID查询积分兑换
     *
     * @param exchangeId 积分兑换ID
     * @return 积分兑换信息
     */
    MemPointsExchange selectMemPointsExchangeById(Long exchangeId);

    /**
     * 插入积分兑换
     *
     * @param exchange 积分兑换信息
     * @return 影响行数
     */
    int insertMemPointsExchange(MemPointsExchange exchange);

    /**
     * 更新积分兑换
     *
     * @param exchange 积分兑换信息
     * @return 影响行数
     */
    int updateMemPointsExchange(MemPointsExchange exchange);

    /**
     * 删除积分兑换
     *
     * @param exchangeId 积分兑换ID
     * @return 影响行数
     */
    int deleteMemPointsExchangeById(Long exchangeId);

    /**
     * 批量删除积分兑换
     *
     * @param exchangeIds 积分兑换ID数组
     * @return 影响行数
     */
    int deleteMemPointsExchangeByIds(Long[] exchangeIds);

    /**
     * 校验兑换编号唯一性
     *
     * @param exchangeNo 兑换编号
     * @return 兑换数量
     */
    int checkMemExchangeNoUnique(@Param("exchangeNo") String exchangeNo);
}
