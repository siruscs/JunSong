package com.junsong.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.mapper.MemPointsExchangeMapper;
import com.junsong.member.domain.MemPointsExchange;
import com.junsong.member.service.IMemPointsExchangeService;
import com.junsong.common.datascope.annotation.DataScope;

/**
 * 积分兑换Service业务层处理
 */
@Service
public class MemPointsExchangeServiceImpl implements IMemPointsExchangeService {

    @Autowired
    private MemPointsExchangeMapper memPointsExchangeMapper;

    /**
     * 查询积分兑换
     *
     * @param id 积分兑换ID
     * @return 积分兑换
     */
    @Override
    @DataScope(deptAlias = "mem_points_exchange")
    public MemPointsExchange selectMemPointsExchangeById(Long id) {
        return memPointsExchangeMapper.selectMemPointsExchangeById(id);
    }

    /**
     * 查询积分兑换列表
     *
     * @param memPointsExchange 积分兑换
     * @return 积分兑换
     */
    @Override
    @DataScope(deptAlias = "mem_points_exchange")
    public List<MemPointsExchange> selectMemPointsExchangeList(MemPointsExchange memPointsExchange) {
        return memPointsExchangeMapper.selectMemPointsExchangeList(memPointsExchange);
    }

    /**
     * 新增积分兑换
     *
     * @param memPointsExchange 积分兑换
     * @return 结果
     */
    @Override
    @Transactional
    public int insertMemPointsExchange(MemPointsExchange memPointsExchange) {
        return memPointsExchangeMapper.insertMemPointsExchange(memPointsExchange);
    }

    /**
     * 修改积分兑换
     *
     * @param memPointsExchange 积分兑换
     * @return 结果
     */
    @Override
    @Transactional
    public int updateMemPointsExchange(MemPointsExchange memPointsExchange) {
        return memPointsExchangeMapper.updateMemPointsExchange(memPointsExchange);
    }

    /**
     * 删除积分兑换
     *
     * @param id 积分兑换ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemPointsExchangeById(Long id) {
        return memPointsExchangeMapper.deleteMemPointsExchangeById(id);
    }

    /**
     * 批量删除积分兑换
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemPointsExchangeByIds(Long[] ids) {
        return memPointsExchangeMapper.deleteMemPointsExchangeByIds(ids);
    }

    /**
     * 校验积分兑换编号是否唯一
     *
     * @param memPointsExchange 积分兑换
     * @return 结果
     */
    @Override
    public boolean checkMemPointsExchangeNoUnique(MemPointsExchange memPointsExchange) {
        int count = memPointsExchangeMapper.checkMemExchangeNoUnique(memPointsExchange.getExchangeNo());
        return count == 0;
    }
}
