package com.junsong.system.service;

import java.util.List;
import com.junsong.system.domain.SysUserDept;

/**
 * 用户与部门关联 业务层
 * 
 * @author junsong
 */
public interface ISysUserDeptService
{
    /**
     * 查询用户与部门关联列表
     * 
     * @param sysUserDept 用户与部门关联
     * @return 用户与部门关联集合
     */
    public List<SysUserDept> selectSysUserDeptList(SysUserDept sysUserDept);

    /**
     * 通过用户ID查询用户关联的部门列表
     * 
     * @param userId 用户ID
     * @return 部门列表
     */
    public List<SysUserDept> selectUserDeptByUserId(Long userId);

    /**
     * 通过用户与部门关联主键查询
     * 
     * @param userDeptId 用户与部门关联主键
     * @return 用户与部门关联
     */
    public SysUserDept selectSysUserDeptByUserDeptId(Long userDeptId);

    /**
     * 通过用户ID和部门ID查询用户与部门关联
     * 
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 用户与部门关联
     */
    public SysUserDept selectUserDeptByUserIdAndDeptId(Long userId, Long deptId);

    /**
     * 新增用户与部门关联
     * 
     * @param sysUserDept 用户与部门关联
     * @return 结果
     */
    public int insertSysUserDept(SysUserDept sysUserDept);

    /**
     * 修改用户与部门关联
     * 
     * @param sysUserDept 用户与部门关联
     * @return 结果
     */
    public int updateSysUserDept(SysUserDept sysUserDept);

    /**
     * 删除用户与部门关联
     * 
     * @param userDeptId 用户与部门关联主键
     * @return 结果
     */
    public int deleteSysUserDeptByUserDeptId(Long userDeptId);

    /**
     * 批量删除用户与部门关联
     * 
     * @param userDeptIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysUserDeptByUserDeptIds(Long[] userDeptIds);

    /**
     * 用户入职部门
     * 
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 结果
     */
    public int hireUserToDept(Long userId, Long deptId);

    /**
     * 用户离职部门
     * 
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 结果
     */
    public int leaveUserFromDept(Long userId, Long deptId);

    /**
     * 批量新增用户部门关联
     * 
     * @param userDeptList 用户部门列表
     * @return 结果
     */
    public int batchUserDept(List<SysUserDept> userDeptList);

    public List<SysUserDept> selectStaffByDeptId(Long deptId);
}
