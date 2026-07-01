package com.junsong.system.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.constant.CacheConstants;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.redis.service.RedisService;
import com.junsong.system.domain.SysRegion;
import com.junsong.system.domain.vo.RegionTreeSelect;
import com.junsong.system.mapper.SysRegionMapper;
import com.junsong.system.service.ISysRegionService;

@Service
public class SysRegionServiceImpl implements ISysRegionService
{
    @Autowired
    private SysRegionMapper regionMapper;

    @Autowired
    private RedisService redisService;

    private static final String REGION_TREE_CACHE_KEY = CacheConstants.SYS_REGION_KEY + "tree";
    private static final long REGION_CACHE_TTL_MINUTES = 60;

    @Override
    @SuppressWarnings("unchecked")
    public List<RegionTreeSelect> selectRegionTree()
    {
        List<RegionTreeSelect> cached = redisService.getCacheObject(REGION_TREE_CACHE_KEY);
        if (cached != null)
        {
            return cached;
        }
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
        redisService.setCacheObject(REGION_TREE_CACHE_KEY, roots, REGION_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
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
        int result = regionMapper.insertRegion(region);
        redisService.deleteObject(REGION_TREE_CACHE_KEY);
        return result;
    }

    @Override
    public int updateRegion(SysRegion region)
    {
        int result = regionMapper.updateRegion(region);
        redisService.deleteObject(REGION_TREE_CACHE_KEY);
        return result;
    }

    @Override
    public int deleteRegionByCode(String code)
    {
        if (regionMapper.countChildrenByParentCode(code) > 0)
        {
            throw new ServiceException("存在下级地址,不能删除");
        }
        int result = regionMapper.deleteRegionByCode(code);
        redisService.deleteObject(REGION_TREE_CACHE_KEY);
        return result;
    }
}
