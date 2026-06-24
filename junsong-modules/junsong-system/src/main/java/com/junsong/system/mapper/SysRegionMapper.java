package com.junsong.system.mapper;

import java.util.List;
import com.junsong.system.domain.SysRegion;

public interface SysRegionMapper
{
    public List<SysRegion> selectRegionList();

    public List<SysRegion> selectChildrenByParentCode(String parentCode);

    public SysRegion selectRegionByCode(String code);

    public int insertRegion(SysRegion region);

    public int updateRegion(SysRegion region);

    public int countChildrenByParentCode(String parentCode);

    public int deleteRegionByCode(String code);
}
