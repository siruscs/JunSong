package com.junsong.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.mapper.MemSeckillMapper;
import com.junsong.member.domain.MemSeckill;
import com.junsong.member.service.IMemSeckillService;
import com.junsong.common.datascope.annotation.DataScope;

/**
 * 秒杀活动Service业务层处理
 */
@Service
public class MemSeckillServiceImpl implements IMemSeckillService {

    @Autowired
    private MemSeckillMapper memSeckillMapper;

    /**
     * 查询秒杀活动
     *
     * @param id 秒杀活动ID
     * @return 秒杀活动
     */
    @Override
    @DataScope(deptAlias = "mem_seckill")
    public MemSeckill selectMemSeckillById(Long id) {
        return memSeckillMapper.selectMemSeckillById(id);
    }

    /**
     * 查询秒杀活动列表
     *
     * @param memSeckill 秒杀活动
     * @return 秒杀活动
     */
    @Override
    @DataScope(deptAlias = "mem_seckill")
    public List<MemSeckill> selectMemSeckillList(MemSeckill memSeckill) {
        return memSeckillMapper.selectMemSeckillList(memSeckill);
    }

    /**
     * 新增秒杀活动
     *
     * @param memSeckill 秒杀活动
     * @return 结果
     */
    @Override
    @Transactional
    public int insertMemSeckill(MemSeckill memSeckill) {
        return memSeckillMapper.insertMemSeckill(memSeckill);
    }

    /**
     * 修改秒杀活动
     *
     * @param memSeckill 秒杀活动
     * @return 结果
     */
    @Override
    @Transactional
    public int updateMemSeckill(MemSeckill memSeckill) {
        return memSeckillMapper.updateMemSeckill(memSeckill);
    }

    /**
     * 删除秒杀活动
     *
     * @param id 秒杀活动ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemSeckillById(Long id) {
        return memSeckillMapper.deleteMemSeckillById(id);
    }

    /**
     * 批量删除秒杀活动
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemSeckillByIds(Long[] ids) {
        return memSeckillMapper.deleteMemSeckillByIds(ids);
    }

    /**
     * 校验秒杀活动编号是否唯一
     *
     * @param memSeckill 秒杀活动
     * @return 结果
     */
    @Override
    public boolean checkMemSeckillNoUnique(MemSeckill memSeckill) {
        int count = memSeckillMapper.checkMemSeckillNoUnique(memSeckill);
        return count == 0;
    }
}
