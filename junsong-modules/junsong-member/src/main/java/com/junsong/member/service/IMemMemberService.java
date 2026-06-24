package com.junsong.member.service;

import java.util.List;
import com.junsong.member.domain.MemMember;

/**
 * 会员信息Service接口
 */
public interface IMemMemberService {

    /**
     * 查询会员信息
     *
     * @param id 会员信息ID
     * @return 会员信息
     */
    public MemMember selectMemMemberById(Long id);

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
     * @param id 会员信息ID
     * @return 结果
     */
    public int deleteMemMemberById(Long id);

    /**
     * 批量删除会员信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMemMemberByIds(Long[] ids);

    /**
     * 校验会员编号是否唯一
     *
     * @param memMember 会员信息
     * @return 结果
     */
    public boolean checkMemMemberNoUnique(MemMember memMember);
    
    /**
     * 生成会员编号
     *
     * @param deptId 部门ID
     * @return 会员编号
     */
    public String generateMemberNo(Long deptId);

    /**
     * 根据会员编号查询会员（仅限当前部门）
     *
     * @param memberNo 会员编号
     * @param deptId 当前部门ID
     * @return 会员信息
     */
    public MemMember selectMemMemberByNo(String memberNo, Long deptId);

    /**
     * 导入会员信息
     *
     * @param memberList 会员信息列表
     * @param updateSupport 是否支持更新
     * @param operName 操作人
     * @param deptId 部门ID
     * @return 导入结果消息
     */
    public String importMember(List<MemMember> memberList, boolean updateSupport, String operName, Long deptId);
}
