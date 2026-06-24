package com.junsong.workflow.lowcode.mapper;

import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LcBizBranchRuleMapper
{
    List<LcBizBranchRule> selectLcBizBranchRuleList(LcBizBranchRule query);

    LcBizBranchRule selectLcBizBranchRuleById(Long id);

    List<LcBizBranchRule> selectByBizCode(String bizCode);

    int insertLcBizBranchRule(LcBizBranchRule lcBizBranchRule);

    int updateLcBizBranchRule(LcBizBranchRule lcBizBranchRule);

    int deleteLcBizBranchRuleByIds(Long[] ids);

    int physicalDeleteByBizCode(String bizCode);
}
