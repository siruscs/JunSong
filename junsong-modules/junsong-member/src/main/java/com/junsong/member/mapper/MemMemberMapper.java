package com.junsong.member.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.member.domain.MemMember;

/**
 * 会员信息Mapper接口
 *
 * @author junsong
 */
public interface MemMemberMapper
{
    /**
     * 查询会员信息
     *
     * @param memberId 会员ID
     * @return 会员信息
     */
    public MemMember selectMemMemberByMemberId(Long memberId);

    /**
     * 查询会员信息列表
     *
     * @param memMember 会员信息
     * @return 会员信息集合
     */
    public List<MemMember> selectMemMemberList(MemMember memMember);

    /**
     * 新增会员信息
     *
     * @param memMember 会员信息
     * @return 结果
     */
    public int insertMemMember(MemMember memMember);

    /**
     * 修改会员信息
     *
     * @param memMember 会员信息
     * @return 结果
     */
    public int updateMemMember(MemMember memMember);

    /**
     * 删除会员信息
     *
     * @param memberId 会员ID
     * @return 结果
     */
    public int deleteMemMemberByMemberId(Long memberId);

    /**
     * 批量删除会员信息
     *
     * @param memberIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMemMemberByMemberIds(Long[] memberIds);

    /**
     * 校验会员编号是否唯一
     *
     * @param memMember 会员信息
     * @return 结果
     */
    public int checkMemberNoUnique(MemMember memMember);
    
    /**
     * 查询下一个会员编号序列（根据部门前缀）
     *
     * @param prefix 前缀（店名前两个字母首拼）
     * @return 结果
     */
    public String selectNextMemberNo(String prefix);
    
    /**
     * 根据部门ID查询部门名称
     *
     * @param deptId 部门ID
     * @return 部门名称
     */
    public String selectDeptNameById(Long deptId);

    /**
     * 查询会员列表（根据会员编号）
     *
     * @param memberNo 会员编号
     * @return 会员信息列表
     */
    public List<MemMember> selectMemMemberByMemberNo(String memberNo);

    /**
     * 根据会员编号和部门ID查询会员（精确匹配，用于按编号检索时过滤部门）
     *
     * @param memberNo 会员编号
     * @param deptId 部门ID
     * @return 会员信息
     */
    public MemMember selectMemMemberByNoAndDept(@Param("memberNo") String memberNo, @Param("deptId") Long deptId);

    /**
     * 按部门查询全员秒杀使用的有效会员（status=0，未被删除，未过期）
     *
     * @param deptId 部门ID
     * @param seckillDate 秒杀日期
     * @return 有效会员列表
     */
    public List<MemMember> selectActiveMembersForSeckill(@Param("deptId") Long deptId, @Param("seckillDate") Date seckillDate);

    /**
     * 批量过期会员卡：将 expire_date < today 且 status='0' 的会员置为 status='1'（失效）
     *
     * @param today 当天日期
     * @return 更新行数
     */
    public int expireMemberCards(@Param("today") Date today);

    /**
     * 原子累加积分和成长值，同时更新最后活跃时间
     *
     * @param memberId 会员ID
     * @param pointsDelta 积分变动（可正可负）
     * @param growthDelta 成长值变动（可正可负）
     * @param operator 操作人
     * @return 更新行数
     */
    public int addPointsAndGrowth(@Param("memberId") Long memberId,
                                  @Param("pointsDelta") java.math.BigDecimal pointsDelta,
                                  @Param("growthDelta") Long growthDelta,
                                  @Param("operator") String operator);

    /**
     * 原子累加成长值，同时更新最后活跃时间（不改变积分）
     *
     * @param memberId 会员ID
     * @param growthDelta 成长值变动
     * @param operator 操作人
     * @return 更新行数
     */
    public int addGrowthOnly(@Param("memberId") Long memberId,
                             @Param("growthDelta") Long growthDelta,
                             @Param("operator") String operator);

    /**
     * 原子累加成长值，不更新最后活跃时间（用于衰减扣减，避免被衰减会员变成"活跃"）
     *
     * @param memberId 会员ID
     * @param growthDelta 成长值变动（衰减时传负数）
     * @param operator 操作人
     * @return 更新行数
     */
    public int addGrowthOnlyWithoutActiveTime(@Param("memberId") Long memberId,
                                              @Param("growthDelta") Long growthDelta,
                                              @Param("operator") String operator);

    /**
     * 更新会员等级
     *
     * @param memberId 会员ID
     * @param newLevel 新等级 type_code
     * @param operator 操作人
     * @return 更新行数
     */
    public int updateMemberLevel(@Param("memberId") Long memberId,
                                 @Param("newLevel") String newLevel,
                                 @Param("operator") String operator);

    /**
     * 更新最后活跃时间
     *
     * @param memberId 会员ID
     * @return 更新行数
     */
    public int updateLastActiveTime(@Param("memberId") Long memberId);

    /**
     * 查询不活跃会员（用于衰减定时任务，按租户过滤）
     *
     * @param tenantId 租户ID
     * @param threshold 不活跃截止时间
     * @return 会员列表
     */
    public List<MemMember> selectInactiveMembers(@Param("tenantId") Long tenantId,
                                                  @Param("threshold") Date threshold);
}
