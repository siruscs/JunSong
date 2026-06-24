package com.junsong.system.domain.vo;

import java.util.ArrayList;
import java.util.List;
import com.junsong.system.domain.SysRegion;

public class RegionTreeSelect
{
    private String value;

    private String label;

    private List<RegionTreeSelect> children = new ArrayList<>();

    public RegionTreeSelect()
    {
    }

    public RegionTreeSelect(SysRegion region)
    {
        this.value = region.getCode();
        this.label = region.getName();
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public List<RegionTreeSelect> getChildren()
    {
        return children;
    }

    public void setChildren(List<RegionTreeSelect> children)
    {
        this.children = children;
    }
}
