package com.junsong.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.mapper.MemPointsRuleMapper;
import com.junsong.member.domain.MemPointsRule;
import com.junsong.member.service.IMemPointsRuleService;

/**
 * 积分规则Service业务层处理
 */
@Service
public class MemPointsRuleServiceImpl implements IMemPointsRuleService {

    @Autowired
    private MemPointsRuleMapper memPointsRuleMapper;

    /**
     * 查询积分规则
     *
     * @param id 积分规则ID
     * @return 积分规则
     */
    @Override
    public MemPointsRule selectMemPointsRuleById(Long id) {
        return memPointsRuleMapper.selectMemPointsRuleById(id);
    }

    /**
     * 查询积分规则列表
     *
     * @param memPointsRule 积分规则
     * @return 积分规则
     */
    @Override
    public List<MemPointsRule> selectMemPointsRuleList(MemPointsRule memPointsRule) {
        return memPointsRuleMapper.selectMemPointsRuleList(memPointsRule);
    }

    /**
     * 新增积分规则
     *
     * @param memPointsRule 积分规则
     * @return 结果
     */
    @Override
    @Transactional
    public int insertMemPointsRule(MemPointsRule memPointsRule) {
        return memPointsRuleMapper.insertMemPointsRule(memPointsRule);
    }

    /**
     * 修改积分规则
     *
     * @param memPointsRule 积分规则
     * @return 结果
     */
    @Override
    @Transactional
    public int updateMemPointsRule(MemPointsRule memPointsRule) {
        return memPointsRuleMapper.updateMemPointsRule(memPointsRule);
    }

    /**
     * 删除积分规则
     *
     * @param id 积分规则ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemPointsRuleById(Long id) {
        return memPointsRuleMapper.deleteMemPointsRuleById(id);
    }

    /**
     * 批量删除积分规则
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemPointsRuleByIds(Long[] ids) {
        return memPointsRuleMapper.deleteMemPointsRuleByIds(ids);
    }

    /**
     * 校验积分规则编号是否唯一
     *
     * @param memPointsRule 积分规则
     * @return 结果
     */
    @Override
    public boolean checkMemPointsRuleNoUnique(MemPointsRule memPointsRule)
    {
        int count = memPointsRuleMapper.checkMemRuleCodeUnique(memPointsRule);
        return count == 0;
    }

    /**
     * 获取第一条生效的积分规则
     */
    @Override
    public MemPointsRule getEffectiveRule() {
        return memPointsRuleMapper.selectEffectiveRule();
    }
}
