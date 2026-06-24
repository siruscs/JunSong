package com.junsong.system.service;

import java.util.List;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.domain.vo.TreeSelect;

/**
 * 部门管理 服务层
 * 
 * @author junsong
 */
public interface ISysDeptService
{
    /**
     * 查询部门管理数据
     * 
     * @param dept 部门信息
     * @return 部门信息集合
     */
    public List<SysDept> selectDeptList(SysDept dept);

    /**
     * 查询部门树结构信息
     * 
     * @param dept 部门信息
     * @return 部门树信息集合
     */
    public List<TreeSelect> selectDeptTreeList(SysDept dept);

    /**
     * 构建前端所需要树结构
     * 
     * @param depts 部门列表
     * @return 树结构列表
     */
    public List<SysDept> buildDeptTree(List<SysDept> depts);

    /**
     * 构建前端所需要下拉树结构
     * 
     * @param depts 部门列表
     * @return 下拉树结构列表
     */
    public List<TreeSelect> buildDeptTreeSelect(List<SysDept> depts);

    /**
     * 根据角色ID查询部门树信息
     * 
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    public List<Long> selectDeptListByRoleId(Long roleId);

    /**
     * 根据部门ID查询信息
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    public SysDept selectDeptById(Long deptId);

    /**
     * 根据部门ID列表批量查询信息
     *
     * @param deptIds 部门ID列表
     * @return 部门信息集合
     */
    public List<SysDept> selectDeptByIds(List<Long> deptIds);

    /**
     * 根据ID查询所有子部门（正常状态）
     * 
     * @param deptId 部门ID
     * @return 子部门数
     */
    public int selectNormalChildrenDeptById(Long deptId);

    /**
     * 是否存在部门子节点
     * 
     * @param deptId 部门ID
     * @return 结果
     */
    public boolean hasChildByDeptId(Long deptId);

    /**
     * 查询部门是否存在用户
     * 
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    public boolean checkDeptExistUser(Long deptId);

    /**
     * 校验部门名称是否唯一
     * 
     * @param dept 部门信息
     * @return 结果
     */
    public boolean checkDeptNameUnique(SysDept dept);

    /**
     * 校验部门是否有数据权限
     * 
     * @param deptId 部门id
     */
    public void checkDeptDataScope(Long deptId);

    /**
     * 新增保存部门信息
     * 
     * @param dept 部门信息
     * @return 结果
     */
    public int insertDept(SysDept dept);

    /**
     * 修改保存部门信息
     * 
     * @param dept 部门信息
     * @return 结果
     */
    public int updateDept(SysDept dept);

    /**
     * 保存部门排序
     *
     * @param deptIds 部门ID数组
     * @param orderNums 排序数组
     */
    public void updateDeptSort(String[] deptIds, String[] orderNums);

    /**
     * 删除部门管理信息
     * 
     * @param deptId 部门ID
     * @return 结果
     */
    public int deleteDeptById(Long deptId);

    /**
     * 查询包围盒内的门店坐标列表（供工作流空间查重远程调用）
     *
     * @param minLng 最小经度
     * @param maxLng 最大经度
     * @param minLat 最小纬度
     * @param maxLat 最大纬度
     * @return 门店列表
     */
    public java.util.List<SysDept> selectStoreGeoInBbox(double minLng, double maxLng, double minLat, double maxLat);

    /**
     * 查询所有门店的地理坐标
     */
    public java.util.List<SysDept> selectStoreGeoAll();

    /**
     * 查询所有顶级部门的地理坐标
     */
    public java.util.List<SysDept> selectTopDeptGeoAll();

    /**
     * 按省市区街道过滤查询门店地理坐标
     */
    public java.util.List<SysDept> selectStoreGeoByRegion(SysDept dept);
}
