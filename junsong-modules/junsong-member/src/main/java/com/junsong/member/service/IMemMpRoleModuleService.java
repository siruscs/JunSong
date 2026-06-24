package com.junsong.member.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.junsong.member.domain.MemMpRoleModule;

public interface IMemMpRoleModuleService {

    List<MemMpRoleModule> selectMpRoleModuleList(MemMpRoleModule query);

    List<String> getAccessibleModules(List<Long> roleIds, Long deptId);

    List<Map<String, Object>> selectAllRoles();

    List<Long> selectRoleIdsByRoleKeys(Set<String> roleKeys);

    void saveRoleModules(Long roleId, Long deptId, List<String> moduleKeys);

    int deleteById(Long id);

    int deleteByRoleId(Long roleId);

    int deleteByRoleIdAndDeptId(Long roleId, Long deptId);
}
