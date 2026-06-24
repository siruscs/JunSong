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
}
