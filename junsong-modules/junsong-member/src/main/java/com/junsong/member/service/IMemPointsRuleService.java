package com.junsong.member.service;

import java.util.List;
import com.junsong.member.domain.MemPointsRule;

/**
 * 积分规则Service接口
 */
public interface IMemPointsRuleService {

    /**
     * 查询积分规则
     *
     * @param id 积分规则ID
     * @return 积分规则
     */
    public MemPointsRule selectMemPointsRuleById(Long id);

    /**
     * 查询积分规则列表
     *
     * @param memPointsRule 积分规则
     * @return 积分规则集合
     */
    public List<MemPointsRule> selectMemPointsRuleList(MemPointsRule memPointsRule);

    /**
     * 新增积分规则
     *
     * @param memPointsRule 积分规则
     * @return 结果
     */
    public int insertMemPointsRule(MemPointsRule memPointsRule);

    /**
     * 修改积分规则
     *
     * @param memPointsRule 积分规则
     * @return 结果
     */
    public int updateMemPointsRule(MemPointsRule memPointsRule);

    /**
     * 删除积分规则
     *
     * @param id 积分规则ID
     * @return 结果
     */
    public int deleteMemPointsRuleById(Long id);

    /**
     * 批量删除积分规则
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMemPointsRuleByIds(Long[] ids);

    /**
     * 校验积分规则编号是否唯一
     *
     * @param memPointsRule 积分规则
     * @return 结果
     */
    public boolean checkMemPointsRuleNoUnique(MemPointsRule memPointsRule);

    /**
     * 获取第一条生效的积分规则
     *
     * @return 生效的积分规则
     */
    public MemPointsRule getEffectiveRule();
}
