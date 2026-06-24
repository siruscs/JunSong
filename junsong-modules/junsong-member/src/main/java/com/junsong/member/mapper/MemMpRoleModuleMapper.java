package com.junsong.member.mapper;

import java.util.List;
import java.util.Map;
import com.junsong.member.domain.MemMpRoleModule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemMpRoleModuleMapper {

    List<MemMpRoleModule> selectMpRoleModuleList(MemMpRoleModule query);

    List<MemMpRoleModule> selectByRoleIdAndDeptId(@Param("roleId") Long roleId, @Param("deptId") Long deptId);

    List<String> selectModuleKeysByRoleIds(@Param("roleIds") List<Long> roleIds, @Param("deptId") Long deptId);

    List<Map<String, Object>> selectAllRoles();

    List<Long> selectRoleIdsByRoleKeys(@Param("roleKeys") Iterable<String> roleKeys);

    int insertMpRoleModule(MemMpRoleModule module);

    int deleteByRoleIdAndDeptId(@Param("roleId") Long roleId, @Param("deptId") Long deptId);

    int deleteByRoleId(@Param("roleId") Long roleId);

    int deleteById(Long id);
}
