package com.junsong.system.service;

import java.util.List;
import com.junsong.system.domain.SysRegion;
import com.junsong.system.domain.vo.RegionTreeSelect;

public interface ISysRegionService
{
    public List<RegionTreeSelect> selectRegionTree();

    public List<SysRegion> selectChildrenByParentCode(String parentCode);

    public SysRegion selectRegionByCode(String code);

    public int insertRegion(SysRegion region);

    public int updateRegion(SysRegion region);

    public int deleteRegionByCode(String code);
}
