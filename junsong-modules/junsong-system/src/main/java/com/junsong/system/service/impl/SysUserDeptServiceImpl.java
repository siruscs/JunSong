package com.junsong.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.domain.SysUserDept;
import com.junsong.system.mapper.SysUserDeptMapper;
import com.junsong.system.service.ISysUserDeptService;

/**
 * 用户与部门关联 服务实现
 * 
 * @author junsong
 */
@Service
public class SysUserDeptServiceImpl implements ISysUserDeptService
{
    @Autowired
    private SysUserDeptMapper userDeptMapper;

    /**
     * 查询用户与部门关联列表
     * 
     * @param sysUserDept 用户与部门关联
     * @return 用户与部门关联
     */
    @Override
    public List<SysUserDept> selectSysUserDeptList(SysUserDept sysUserDept)
    {
        return userDeptMapper.selectSysUserDeptList(sysUserDept);
    }

    /**
     * 通过用户ID查询用户关联的部门列表
     * 
     * @param userId 用户ID
     * @return 部门列表
     */
    @Override
    public List<SysUserDept> selectUserDeptByUserId(Long userId)
    {
        return userDeptMapper.selectUserDeptByUserId(userId);
    }

    /**
     * 通过用户与部门关联主键查询
     * 
     * @param userDeptId 用户与部门关联主键
     * @return 用户与部门关联
     */
    @Override
    public SysUserDept selectSysUserDeptByUserDeptId(Long userDeptId)
    {
        SysUserDept query = new SysUserDept();
        query.setUserDeptId(userDeptId);
        List<SysUserDept> list = userDeptMapper.selectSysUserDeptList(query);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 通过用户ID和部门ID查询用户与部门关联
     * 
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 用户与部门关联
     */
    @Override
    public SysUserDept selectUserDeptByUserIdAndDeptId(Long userId, Long deptId)
    {
        return userDeptMapper.selectUserDeptByUserIdAndDeptId(userId, deptId);
    }

    /**
     * 新增用户与部门关联
     * 
     * @param sysUserDept 用户与部门关联
     * @return 结果
     */
    @Override
    public int insertSysUserDept(SysUserDept sysUserDept)
    {
        sysUserDept.setCreateBy(SecurityUtils.getUsername());
        return userDeptMapper.insertSysUserDept(sysUserDept);
    }

    /**
     * 修改用户与部门关联
     * 
     * @param sysUserDept 用户与部门关联
     * @return 结果
     */
    @Override
    public int updateSysUserDept(SysUserDept sysUserDept)
    {
        sysUserDept.setUpdateBy(SecurityUtils.getUsername());
        return userDeptMapper.updateSysUserDept(sysUserDept);
    }

    /**
     * 删除用户与部门关联
     * 
     * @param userDeptId 用户与部门关联主键
     * @return 结果
     */
    @Override
    public int deleteSysUserDeptByUserDeptId(Long userDeptId)
    {
        return userDeptMapper.deleteSysUserDeptByUserDeptId(userDeptId);
    }

    /**
     * 批量删除用户与部门关联
     * 
     * @param userDeptIds 需要删除的数据主键集合
     * @return 结果
     */
    @Override
    public int deleteSysUserDeptByUserDeptIds(Long[] userDeptIds)
    {
        return userDeptMapper.deleteSysUserDeptByUserDeptIds(userDeptIds);
    }

    /**
     * 用户入职部门
     * 
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int hireUserToDept(Long userId, Long deptId)
    {
        // 检查是否已经存在关联
        SysUserDept existUserDept = userDeptMapper.selectUserDeptByUserIdAndDeptId(userId, deptId);
        
        if (existUserDept != null)
        {
            // 如果已存在且是离职状态，则更新为在职
            if ("1".equals(existUserDept.getStatus()))
            {
                existUserDept.setStatus("0");
                existUserDept.setLeaveDate(null);
                existUserDept.setUpdateBy(SecurityUtils.getUsername());
                return userDeptMapper.updateSysUserDept(existUserDept);
            }
            // 如果已经是在职状态，直接返回成功
            return 1;
        }
        
        // 创建新的关联
        SysUserDept userDept = new SysUserDept();
        userDept.setUserId(userId);
        userDept.setDeptId(deptId);
        userDept.setStatus("0");
        userDept.setHireDate(new Date());
        userDept.setCreateBy(SecurityUtils.getUsername());
        return userDeptMapper.insertSysUserDept(userDept);
    }

    /**
     * 用户离职部门
     * 
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int leaveUserFromDept(Long userId, Long deptId)
    {
        SysUserDept userDept = userDeptMapper.selectUserDeptByUserIdAndDeptId(userId, deptId);
        if (userDept == null)
        {
            return 0;
        }
        
        userDept.setStatus("1");
        userDept.setLeaveDate(new Date());
        userDept.setUpdateBy(SecurityUtils.getUsername());
        return userDeptMapper.updateSysUserDept(userDept);
    }

    /**
     * 批量新增用户部门关联
     * 
     * @param userDeptList 用户部门列表
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUserDept(List<SysUserDept> userDeptList)
    {
        String username = SecurityUtils.getUsername();
        for (SysUserDept userDept : userDeptList)
        {
            userDept.setCreateBy(username);
            if (userDept.getStatus() == null)
            {
                userDept.setStatus("0");
            }
            if (userDept.getHireDate() == null)
            {
                userDept.setHireDate(new Date());
            }
        }
        return userDeptMapper.batchUserDept(userDeptList);
    }

    @Override
    public List<SysUserDept> selectStaffByDeptId(Long deptId)
    {
        return userDeptMapper.selectStaffByDeptId(deptId);
    }
}
