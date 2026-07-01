package com.junsong.member.service;

import java.util.List;
import com.junsong.member.domain.MemPointsExchange;

/**
 * 积分兑换Service接口
 */
public interface IMemPointsExchangeService {

    /**
     * 查询积分兑换
     *
     * @param id 积分兑换ID
     * @return 积分兑换
     */
    public MemPointsExchange selectMemPointsExchangeById(Long id);

    /**
     * 查询积分兑换列表
     *
     * @param memPointsExchange 积分兑换
     * @return 积分兑换集合
     */
    public List<MemPointsExchange> selectMemPointsExchangeList(MemPointsExchange memPointsExchange);

    /**
     * 新增积分兑换
     *
     * @param memPointsExchange 积分兑换
     * @return 结果
     */
    public int insertMemPointsExchange(MemPointsExchange memPointsExchange);

    /**
     * 积分兑换：在同一事务内完成兑换单插入和积分扣减记录创建。
     * 任何一步失败则整体回滚。
     *
     * @param exchange 兑换参数（memberId、pointsDeducted等必须已设置）
     * @param operator 操作人用户名
     * @throws IllegalArgumentException 余额不足、编号重复等
     */
    public void exchangePoints(MemPointsExchange exchange, String operator);

    /**
     * 修改积分兑换
     *
     * @param memPointsExchange 积分兑换
     * @return 结果
     */
    public int updateMemPointsExchange(MemPointsExchange memPointsExchange);

    /**
     * 删除积分兑换
     *
     * @param id 积分兑换ID
     * @return 结果
     */
    public int deleteMemPointsExchangeById(Long id);

    /**
     * 批量删除积分兑换
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMemPointsExchangeByIds(Long[] ids);

    /**
     * 校验积分兑换编号是否唯一
     *
     * @param memPointsExchange 积分兑换
     * @return 结果
     */
    public boolean checkMemPointsExchangeNoUnique(MemPointsExchange memPointsExchange);
}
