package com.junsong.member.service;

import java.util.List;
import com.junsong.member.domain.MemMemberCardType;

/**
 * 会员等级配置Service接口
 *
 * @author junsong
 */
public interface IMemberLevelService
{
    /**
     * 查询等级配置列表
     */
    public List<MemMemberCardType> selectLevelList(MemMemberCardType cardType);

    /**
     * 查询等级配置详情
     */
    public MemMemberCardType selectLevelByTypeCode(String typeCode);

    /**
     * 查询所有启用的等级配置
     */
    public List<MemMemberCardType> selectEnabledLevels();

    /**
     * 修改等级配置
     */
    public int updateLevel(MemMemberCardType cardType);

    /**
     * 新增等级配置
     */
    public int insertLevel(MemMemberCardType cardType);

    /**
     * 校验等级编码是否唯一
     *
     * @param cardType 等级配置
     * @return 结果
     */
    public boolean checkTypeCodeUnique(MemMemberCardType cardType);

    /**
     * 根据成长值计算目标等级 type_code
     *
     * @param growthValue 成长值
     * @return 目标等级 type_code，未命中返回 experience
     */
    public String calculateLevel(Long growthValue);
}
