package com.junsong.system.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.system.domain.SysRegion;
import com.junsong.system.domain.vo.RegionTreeSelect;
import com.junsong.system.mapper.SysRegionMapper;
import com.junsong.system.service.ISysRegionService;

@Service
public class SysRegionServiceImpl implements ISysRegionService
{
    @Autowired
    private SysRegionMapper regionMapper;

    @Override
    public List<RegionTreeSelect> selectRegionTree()
    {
        List<SysRegion> regions = regionMapper.selectRegionList();
        Map<String, RegionTreeSelect> nodeMap = new LinkedHashMap<>();
        List<RegionTreeSelect> roots = new ArrayList<>();
        for (SysRegion region : regions)
        {
            nodeMap.put(region.getCode(), new RegionTreeSelect(region));
        }
        for (SysRegion region : regions)
        {
            RegionTreeSelect node = nodeMap.get(region.getCode());
            RegionTreeSelect parent = nodeMap.get(region.getParentCode());
            if (parent == null)
            {
                roots.add(node);
            }
            else
            {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    @Override
    public List<SysRegion> selectChildrenByParentCode(String parentCode)
    {
        return regionMapper.selectChildrenByParentCode(parentCode);
    }

    @Override
    public SysRegion selectRegionByCode(String code)
    {
        return regionMapper.selectRegionByCode(code);
    }

    @Override
    public int insertRegion(SysRegion region)
    {
        return regionMapper.insertRegion(region);
    }

    @Override
    public int updateRegion(SysRegion region)
    {
        return regionMapper.updateRegion(region);
    }

    @Override
    public int deleteRegionByCode(String code)
    {
        if (regionMapper.countChildrenByParentCode(code) > 0)
        {
            throw new ServiceException("存在下级地址,不能删除");
        }
        return regionMapper.deleteRegionByCode(code);
    }
}
