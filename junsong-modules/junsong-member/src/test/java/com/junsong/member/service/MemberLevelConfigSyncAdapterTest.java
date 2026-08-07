package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.junsong.member.service.impl.MemberLevelConfigSyncAdapter;

class MemberLevelConfigSyncAdapterTest
{
    @Test
    void createUsesLevelCodeAndTargetDeptWithoutSourceId()
    {
        JdbcTemplate jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(1);
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("businessKey", "GOLD");
        source.put("displayName", "黄金会员");
        source.put("card_fee", 99);
        source.put("discount_rate", 0.9);
        source.put("points_rate", 1.5);
        source.put("min_growth", 1000);
        source.put("sign_in_points", 10);
        source.put("status", "0");
        source.put("recordId", 88L);

        int rows = new MemberLevelConfigSyncAdapter(jdbc).create(source, 20L, "admin");

        assertEquals(1, rows);
        verify(jdbc).update(eq("insert into mem_member_card_type (tenant_id, dept_id, type_name, type_code, card_fee, discount_rate, points_rate, min_growth, sign_in_points, status, create_by, create_time) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate())"), any(Object[].class));
    }

    @Test
    void overwriteUpdatesOnlyTargetDepartmentRow()
    {
        JdbcTemplate jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(1);
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("displayName", "黄金会员");
        source.put("card_fee", 99);
        source.put("discount_rate", 0.9);
        source.put("points_rate", 1.5);
        source.put("min_growth", 1000);
        source.put("sign_in_points", 10);
        source.put("status", "0");
        Map<String, Object> target = Map.of("recordId", 20L);

        int rows = new MemberLevelConfigSyncAdapter(jdbc).overwrite(source, target, 30L, "admin");

        assertEquals(1, rows);
        verify(jdbc).update(eq("update mem_member_card_type set type_name=?, card_fee=?, discount_rate=?, points_rate=?, min_growth=?, sign_in_points=?, status=?, update_by=?, update_time=sysdate() where type_id=? and dept_id=? and del_flag='0'"), any(Object[].class));
    }
}
