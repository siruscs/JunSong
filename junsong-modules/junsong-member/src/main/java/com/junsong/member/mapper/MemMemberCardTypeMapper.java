package com.junsong.member.mapper;

import java.util.List;
import com.junsong.member.domain.MemMemberCardType;

/**
 * 会员卡类型Mapper接口（兼作等级配置）
 *
 * @author junsong
 */
public interface MemMemberCardTypeMapper
{
    /**
     * 查询等级配置列表
     */
    public List<MemMemberCardType> selectCardTypeList(MemMemberCardType cardType);

    /**
     * 查询等级配置详情
     */
    public MemMemberCardType selectCardTypeByTypeCode(@org.apache.ibatis.annotations.Param("typeCode") String typeCode,
                                                      @org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                                                      @org.apache.ibatis.annotations.Param("deptId") Long deptId);

    public MemMemberCardType selectCardTypeById(@org.apache.ibatis.annotations.Param("typeId") Long typeId,
                                                @org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                                                @org.apache.ibatis.annotations.Param("deptId") Long deptId);

    /**
     * 查询所有启用的等级配置（按 min_growth 升序）
     */
    public List<MemMemberCardType> selectEnabledCardTypes(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                                                          @org.apache.ibatis.annotations.Param("deptId") Long deptId);

    /**
     * 根据成长值查询目标等级（命中最高 min_growth 的等级）
     */
    public MemMemberCardType selectLevelByGrowth(@org.apache.ibatis.annotations.Param("growthValue") Long growthValue,
                                                 @org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                                                 @org.apache.ibatis.annotations.Param("deptId") Long deptId);

    /**
     * 修改等级配置
     */
    public int updateCardType(MemMemberCardType cardType);

    /**
     * 新增等级配置
     */
    public int insertCardType(MemMemberCardType cardType);

    /**
     * 校验等级编码是否唯一
     */
    public MemMemberCardType checkTypeCodeUnique(@org.apache.ibatis.annotations.Param("typeCode") String typeCode,
                                                 @org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                                                 @org.apache.ibatis.annotations.Param("deptId") Long deptId);
}
