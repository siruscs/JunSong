package com.junsong.member.mapper;

import java.util.List;
import com.junsong.member.domain.MemSeckill;

/**
 * 秒杀活动Mapper接口
 *
 * @author junsong
 */
public interface MemSeckillMapper
{
    /**
     * 查询秒杀活动
     *
     * @param seckillId 秒杀活动ID
     * @return 秒杀活动
     */
    public MemSeckill selectMemSeckillById(Long seckillId);

    /**
     * 查询秒杀活动列表
     *
     * @param memSeckill 秒杀活动
     * @return 秒杀活动集合
     */
    public List<MemSeckill> selectMemSeckillList(MemSeckill memSeckill);

    /**
     * 新增秒杀活动
     *
     * @param memSeckill 秒杀活动
     * @return 结果
     */
    public int insertMemSeckill(MemSeckill memSeckill);

    /**
     * 修改秒杀活动
     *
     * @param memSeckill 秒杀活动
     * @return 结果
     */
    public int updateMemSeckill(MemSeckill memSeckill);

    /**
     * 删除秒杀活动
     *
     * @param seckillId 秒杀活动ID
     * @return 结果
     */
    public int deleteMemSeckillById(Long seckillId);

    /**
     * 批量删除秒杀活动
     *
     * @param seckillIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMemSeckillByIds(Long[] seckillIds);

    /**
     * 校验秒杀编号是否唯一
     *
     * @param memSeckill 秒杀活动
     * @return 结果
     */
    public int checkMemSeckillNoUnique(MemSeckill memSeckill);
}
