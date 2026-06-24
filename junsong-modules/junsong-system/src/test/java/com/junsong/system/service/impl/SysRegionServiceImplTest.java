package com.junsong.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.system.domain.SysRegion;
import com.junsong.system.domain.vo.RegionTreeSelect;
import com.junsong.system.mapper.SysRegionMapper;

class SysRegionServiceImplTest
{
    @Test
    void buildsRegionTreeToStreetLevel() throws Exception
    {
        SysRegionMapper regionMapper = Mockito.mock(SysRegionMapper.class);
        when(regionMapper.selectRegionList()).thenReturn(Arrays.asList(
                region("440000", "广东省", "0", 1),
                region("440300", "深圳市", "440000", 2),
                region("440305", "南山区", "440300", 3),
                region("440305007", "粤海街道", "440305", 4)));
        SysRegionServiceImpl service = service(regionMapper);

        List<RegionTreeSelect> tree = service.selectRegionTree();

        assertEquals(1, tree.size());
        assertEquals("440000", tree.get(0).getValue());
        assertEquals("广东省", tree.get(0).getLabel());
        assertEquals("深圳市", tree.get(0).getChildren().get(0).getLabel());
        assertEquals("南山区", tree.get(0).getChildren().get(0).getChildren().get(0).getLabel());
        assertEquals("粤海街道", tree.get(0).getChildren().get(0).getChildren().get(0).getChildren().get(0).getLabel());
    }

    @Test
    void selectsChildrenByParentCode() throws Exception
    {
        SysRegionMapper regionMapper = Mockito.mock(SysRegionMapper.class);
        when(regionMapper.selectChildrenByParentCode("440300")).thenReturn(Arrays.asList(region("440305", "南山区", "440300", 3)));
        SysRegionServiceImpl service = service(regionMapper);

        List<SysRegion> children = service.selectChildrenByParentCode("440300");

        assertEquals(1, children.size());
        assertEquals("南山区", children.get(0).getName());
    }

    @Test
    void insertsRegion() throws Exception
    {
        SysRegionMapper regionMapper = Mockito.mock(SysRegionMapper.class);
        SysRegionServiceImpl service = service(regionMapper);
        SysRegion region = region("440305099", "测试街道", "440305", 4);

        service.insertRegion(region);

        verify(regionMapper).insertRegion(region);
    }

    @Test
    void updatesRegion() throws Exception
    {
        SysRegionMapper regionMapper = Mockito.mock(SysRegionMapper.class);
        SysRegionServiceImpl service = service(regionMapper);
        SysRegion region = region("440305007", "粤海街道", "440305", 4);

        service.updateRegion(region);

        verify(regionMapper).updateRegion(region);
    }

    @Test
    void rejectsDeletingRegionWithChildren() throws Exception
    {
        SysRegionMapper regionMapper = Mockito.mock(SysRegionMapper.class);
        when(regionMapper.countChildrenByParentCode("440300")).thenReturn(1);
        SysRegionServiceImpl service = service(regionMapper);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.deleteRegionByCode("440300"));

        assertEquals("存在下级地址,不能删除", exception.getMessage());
    }

    @Test
    void deletesLeafRegion() throws Exception
    {
        SysRegionMapper regionMapper = Mockito.mock(SysRegionMapper.class);
        when(regionMapper.countChildrenByParentCode("440305007")).thenReturn(0);
        SysRegionServiceImpl service = service(regionMapper);

        service.deleteRegionByCode("440305007");

        verify(regionMapper).deleteRegionByCode("440305007");
    }

    private SysRegionServiceImpl service(SysRegionMapper regionMapper) throws Exception
    {
        SysRegionServiceImpl service = new SysRegionServiceImpl();
        Field mapperField = SysRegionServiceImpl.class.getDeclaredField("regionMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, regionMapper);
        return service;
    }

    private SysRegion region(String code, String name, String parentCode, Integer level)
    {
        SysRegion region = new SysRegion();
        region.setCode(code);
        region.setName(name);
        region.setParentCode(parentCode);
        region.setLevel(level);
        region.setSort(1);
        region.setStatus("0");
        return region;
    }
}
