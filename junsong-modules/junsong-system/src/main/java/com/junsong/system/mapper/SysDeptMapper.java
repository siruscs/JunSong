package com.junsong.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.system.api.domain.SysDept;

/**
 * 部门管理 数据层
 * 
 * @author junsong
 */
public interface SysDeptMapper
{
    /**
     * 查询部门管理数据
     * 
     * @param dept 部门信息
     * @return 部门信息集合
     */
    public List<SysDept> selectDeptList(SysDept dept);

    /**
     * 根据角色ID查询部门树信息
     * 
     * @param roleId 角色ID
     * @param deptCheckStrictly 部门树选择项是否关联显示
     * @return 选中部门列表
     */
    public List<Long> selectDeptListByRoleId(@Param("roleId") Long roleId, @Param("deptCheckStrictly") boolean deptCheckStrictly);

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
    public List<SysDept> selectDeptByIds(@Param("deptIds") List<Long> deptIds);

    /**
     * 根据ID查询所有子部门
     * 
     * @param deptId 部门ID
     * @return 部门列表
     */
    public List<SysDept> selectChildrenDeptById(Long deptId);

    /**
     * 根据ID查询所有子部门（正常状态）
     * 
     * @param deptId 部门ID
     * @return 子部门数
     */
    public int selectNormalChildrenDeptById(Long deptId);

    /**
     * 是否存在子节点
     * 
     * @param deptId 部门ID
     * @return 结果
     */
    public int hasChildByDeptId(Long deptId);

    /**
     * 查询部门是否存在用户
     * 
     * @param deptId 部门ID
     * @return 结果
     */
    public int checkDeptExistUser(Long deptId);

    /**
     * 校验部门名称是否唯一
     * 
     * @param deptName 部门名称
     * @param parentId 父部门ID
     * @return 结果
     */
    public SysDept checkDeptNameUnique(@Param("deptName") String deptName, @Param("parentId") Long parentId);

    /**
     * 新增部门信息
     * 
     * @param dept 部门信息
     * @return 结果
     */
    public int insertDept(SysDept dept);

    /**
     * 修改部门信息
     * 
     * @param dept 部门信息
     * @return 结果
     */
    public int updateDept(SysDept dept);

    /**
     * 修改所在部门正常状态
     * 
     * @param deptIds 部门ID组
     */
    public void updateDeptStatusNormal(Long[] deptIds);

    /**
     * 修改子元素关系
     * 
     * @param depts 子元素
     * @return 结果
     */
    public int updateDeptChildren(@Param("depts") List<SysDept> depts);

    /**
     * 保存部门排序
     *
     * @param dept 部门信息
     */
    public void updateDeptSort(SysDept dept);

    /**
     * 删除部门管理信息
     * 
     * @param deptId 部门ID
     * @return 结果
     */
    public int deleteDeptById(Long deptId);

    /**
     * 查询包围盒内的门店（dept_type=1）坐标列表
     *
     * @param minLng 最小经度
     * @param maxLng 最大经度
     * @param minLat 最小纬度
     * @param maxLat 最大纬度
     * @return 门店列表（含 dept_id, dept_name, longitude, latitude）
     */
    public List<SysDept> selectStoreGeoInBbox(@Param("minLng") double minLng, @Param("maxLng") double maxLng, @Param("minLat") double minLat, @Param("maxLat") double maxLat);

    /**
     * 查询所有门店（dept_type=1）的地理坐标
     */
    public List<SysDept> selectStoreGeoAll();

    /**
     * 查询所有顶级部门（parentId=0）的地理坐标
     */
    public List<SysDept> selectTopDeptGeoAll();

    /**
     * 按省市区街道过滤查询门店地理坐标
     */
    public List<SysDept> selectStoreGeoByRegion(SysDept dept);
}
