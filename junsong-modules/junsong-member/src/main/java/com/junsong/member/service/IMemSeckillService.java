package com.junsong.member.service;

import java.util.List;
import com.junsong.member.domain.MemSeckill;

/**
 * 秒杀活动Service接口
 */
public interface IMemSeckillService {

    /**
     * 查询秒杀活动
     *
     * @param id 秒杀活动ID
     * @return 秒杀活动
     */
    public MemSeckill selectMemSeckillById(Long id);

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
     * @param id 秒杀活动ID
     * @return 结果
     */
    public int deleteMemSeckillById(Long id);

    /**
     * 批量删除秒杀活动
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMemSeckillByIds(Long[] ids);

    /**
     * 校验秒杀活动编号是否唯一
     *
     * @param memSeckill 秒杀活动
     * @return 结果
     */
    public boolean checkMemSeckillNoUnique(MemSeckill memSeckill);
}
