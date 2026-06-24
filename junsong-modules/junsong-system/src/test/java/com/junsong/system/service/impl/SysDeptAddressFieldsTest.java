package com.junsong.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import com.junsong.system.api.domain.SysDept;

class SysDeptAddressFieldsTest
{
    @Test
    void storesOptionalDepartmentAddressFields()
    {
        SysDept dept = new SysDept();

        dept.setProvinceCode("440000");
        dept.setProvinceName("广东省");
        dept.setCityCode("440300");
        dept.setCityName("深圳市");
        dept.setDistrictCode("440305");
        dept.setDistrictName("南山区");
        dept.setStreetCode("440305007");
        dept.setStreetName("粤海街道");
        dept.setDetailAddress("科技园科苑路 15 号");

        assertEquals("440000", dept.getProvinceCode());
        assertEquals("广东省", dept.getProvinceName());
        assertEquals("440300", dept.getCityCode());
        assertEquals("深圳市", dept.getCityName());
        assertEquals("440305", dept.getDistrictCode());
        assertEquals("南山区", dept.getDistrictName());
        assertEquals("440305007", dept.getStreetCode());
        assertEquals("粤海街道", dept.getStreetName());
        assertEquals("科技园科苑路 15 号", dept.getDetailAddress());
    }
}
