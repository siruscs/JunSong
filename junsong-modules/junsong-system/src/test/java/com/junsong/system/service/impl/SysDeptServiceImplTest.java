package com.junsong.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.mapper.SysDeptMapper;

class SysDeptServiceImplTest
{
    @Test
    void insertsRootDepartmentWithZeroAncestorsWhenParentIdIsZero() throws Exception
    {
        SysDeptMapper deptMapper = Mockito.mock(SysDeptMapper.class);
        when(deptMapper.insertDept(any(SysDept.class))).thenReturn(1);
        SysDeptServiceImpl service = new SysDeptServiceImpl();
        Field mapperField = SysDeptServiceImpl.class.getDeclaredField("deptMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, deptMapper);
        SysDept dept = new SysDept();
        dept.setParentId(0L);
        dept.setDeptName("总部");

        int result = service.insertDept(dept);

        ArgumentCaptor<SysDept> captor = ArgumentCaptor.forClass(SysDept.class);
        verify(deptMapper).insertDept(captor.capture());
        assertEquals(1, result);
        assertEquals("0", captor.getValue().getAncestors());
    }
}
