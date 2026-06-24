package com.junsong.member.service.impl;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.domain.MemMpRoleModule;
import com.junsong.member.mapper.MemMpRoleModuleMapper;
import com.junsong.member.service.IMemMpRoleModuleService;

@Service
public class MemMpRoleModuleServiceImpl implements IMemMpRoleModuleService {

    @Autowired
    private MemMpRoleModuleMapper mpRoleModuleMapper;

    @Override
    public List<MemMpRoleModule> selectMpRoleModuleList(MemMpRoleModule query) {
        return mpRoleModuleMapper.selectMpRoleModuleList(query);
    }

    @Override
    public List<Map<String, Object>> selectAllRoles() {
        return mpRoleModuleMapper.selectAllRoles();
    }

    @Override
    public List<Long> selectRoleIdsByRoleKeys(Set<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = mpRoleModuleMapper.selectRoleIdsByRoleKeys(roleKeys);
        return roleIds != null ? roleIds : Collections.emptyList();
    }

    @Override
    public List<String> getAccessibleModules(List<Long> roleIds, Long deptId) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> modules = mpRoleModuleMapper.selectModuleKeysByRoleIds(roleIds, deptId);
        return modules != null ? modules : Collections.emptyList();
    }

    @Override
    @Transactional
    public void saveRoleModules(Long roleId, Long deptId, List<String> moduleKeys) {
        mpRoleModuleMapper.deleteByRoleId(roleId);
        if (moduleKeys != null && !moduleKeys.isEmpty()) {
            Set<String> uniqueKeys = new LinkedHashSet<>(moduleKeys);
            for (String key : uniqueKeys) {
                MemMpRoleModule entity = new MemMpRoleModule();
                entity.setRoleId(roleId);
                entity.setDeptId(deptId);
                entity.setModuleKey(key);
                mpRoleModuleMapper.insertMpRoleModule(entity);
            }
        }
    }

    @Override
    @Transactional
    public int deleteById(Long id) {
        return mpRoleModuleMapper.deleteById(id);
    }

    @Override
    @Transactional
    public int deleteByRoleId(Long roleId) {
        return mpRoleModuleMapper.deleteByRoleId(roleId);
    }

    @Override
    @Transactional
    public int deleteByRoleIdAndDeptId(Long roleId, Long deptId) {
        return mpRoleModuleMapper.deleteByRoleIdAndDeptId(roleId, deptId);
    }
}
