package com.junsong.member.service.impl;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.SysMpModuleSort;
import com.junsong.member.mapper.SysMpModuleSortMapper;
import com.junsong.member.service.ISysMpModuleSortService;

@Service
public class SysMpModuleSortServiceImpl implements ISysMpModuleSortService {

    @Autowired
    private SysMpModuleSortMapper sortMapper;

    @Override
    public List<SysMpModuleSort> selectAll() {
        List<SysMpModuleSort> list = sortMapper.selectAll();
        return list != null ? list : Collections.emptyList();
    }

    @Override
    @Transactional
    public void saveBatch(List<SysMpModuleSort> sortList) {
        sortMapper.deleteAll();
        if (sortList == null || sortList.isEmpty()) {
            return;
        }
        Date now = new Date();
        String userName;
        try { userName = SecurityUtils.getUsername(); } catch (Exception ignored) { userName = ""; }

        int order = 10;
        // 使用 LinkedHashSet 去重（按 moduleKey），保留入参顺序。
        Set<String> seen = new LinkedHashSet<>();
        for (SysMpModuleSort r : sortList) {
            if (r == null || r.getModuleKey() == null || r.getModuleKey().trim().isEmpty()) continue;
            if (!seen.add(r.getModuleKey())) continue; // 重复跳过
            SysMpModuleSort entity = new SysMpModuleSort();
            entity.setModuleKey(r.getModuleKey());
            entity.setGroupName(r.getGroupName());
            entity.setSortOrder(order);
            entity.setRemark(r.getRemark());
            entity.setCreateBy(userName);
            entity.setCreateTime(now);
            entity.setUpdateBy(userName);
            entity.setUpdateTime(now);
            sortMapper.insert(entity);
            order += 10;
        }
    }

    @Override
    @Transactional
    public int saveOne(SysMpModuleSort record) {
        if (record == null || record.getModuleKey() == null || record.getModuleKey().trim().isEmpty()) {
            return 0;
        }
        SysMpModuleSort existing = sortMapper.selectByModuleKey(record.getModuleKey());
        Date now = new Date();
        String userName;
        try { userName = SecurityUtils.getUsername(); } catch (Exception ignored) { userName = ""; }
        if (existing == null) {
            record.setCreateBy(userName);
            record.setCreateTime(now);
            record.setUpdateBy(userName);
            record.setUpdateTime(now);
            return sortMapper.insert(record);
        }
        record.setId(existing.getId());
        record.setUpdateBy(userName);
        record.setUpdateTime(now);
        return sortMapper.updateById(record);
    }
}
