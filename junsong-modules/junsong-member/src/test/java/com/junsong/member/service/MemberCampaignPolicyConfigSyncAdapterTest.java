package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.LinkedHashMap;
import java.util.Map;
import java.sql.Timestamp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.junsong.member.service.impl.MemberCampaignPolicyConfigSyncAdapter;

class MemberCampaignPolicyConfigSyncAdapterTest
{
    @Test
    void policyAdapterRejectsSnapshotWithoutPackageTiers()
    {
        JdbcTemplate jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("select product_id from fin_product where product_code=? and dept_id=? and del_flag='0' limit 1"), eq(Long.class), any(Object[].class))).thenReturn(902L);
        when(jdbc.queryForObject(eq("select policy_id from mem_campaign_policy where tenant_id=? and dept_id=? and policy_no=? and version=? and del_flag='0' limit 1"), eq(Long.class), any(Object[].class))).thenReturn(1001L);
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(1);
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("tenantId", 1L); source.put("displayName", "政策"); source.put("businessKey", "POL-MISSING-PACKAGE");
        source.put("productCode", "GEL-01"); source.put("targetPeriodId", 700L); source.put("version", 1);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new MemberCampaignPolicyConfigSyncAdapter(jdbc).create(source, 20L, "admin"));
    }

    @Test
    void policyAdapterCreatesPolicyForMappedPeriodAndCopiesPackages()
    {
        JdbcTemplate jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("select product_id from fin_product where product_code=? and dept_id=? and del_flag='0' limit 1"), eq(Long.class), any(Object[].class))).thenReturn(902L);
        when(jdbc.queryForObject(eq("select policy_id from mem_campaign_policy where tenant_id=? and dept_id=? and policy_no=? and version=? and del_flag='0' limit 1"), eq(Long.class), any(Object[].class))).thenReturn(1001L);
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(1);
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("tenantId", 1L); source.put("displayName", "凝胶政策"); source.put("businessKey", "POL-01");
        source.put("productCode", "GEL-01"); source.put("targetPeriodId", 700L);
        source.put("effective_start", "2026-06-05T15:19:29");
        source.put("effective_end", "2026-08-05T15:19:29");
        source.put("version", 2); source.put("customer_scope", "ALL"); source.put("status", "1");
        source.put("packages", java.util.List.of(Map.of("packageName", "5送1", "purchaseQuantity", 5,
                "giftQuantity", 1, "totalQuantity", 6, "sortNo", 1)));

        int rows = new MemberCampaignPolicyConfigSyncAdapter(jdbc).create(source, 20L, "admin");

        assertEquals(1, rows);
        verify(jdbc).update(eq("insert into mem_campaign_policy (tenant_id, dept_id, period_id, product_id, policy_no, policy_name, version, customer_scope, effective_start, effective_end, status, del_flag, create_by, create_time, remark) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0', ?, sysdate(), ?)"), any(Object[].class));
    }

    @Test
    void policyAdapterConvertsSnapshotDateStringsToSqlTimestamps()
    {
        JdbcTemplate jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("select product_id from fin_product where product_code=? and dept_id=? and del_flag='0' limit 1"), eq(Long.class), any(Object[].class))).thenReturn(902L);
        when(jdbc.queryForObject(eq("select policy_id from mem_campaign_policy where tenant_id=? and dept_id=? and policy_no=? and version=? and del_flag='0' limit 1"), eq(Long.class), any(Object[].class))).thenReturn(1001L);
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(1);
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("tenantId", 1L); source.put("displayName", "政策"); source.put("businessKey", "POL-02");
        source.put("productCode", "GEL-01"); source.put("targetPeriodId", 700L); source.put("version", 1);
        source.put("effective_start", "2026-06-05T15:19:29"); source.put("effective_end", "2026-08-05T15:19:29");
        source.put("packages", java.util.List.of(Map.of("packageName", "5送1", "purchaseQuantity", 5, "giftQuantity", 1, "totalQuantity", 6, "sortNo", 1)));

        new MemberCampaignPolicyConfigSyncAdapter(jdbc).create(source, 20L, "admin");

        org.mockito.ArgumentCaptor<Object[]> captor = org.mockito.ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, org.mockito.Mockito.times(1)).update(eq("insert into mem_campaign_policy (tenant_id, dept_id, period_id, product_id, policy_no, policy_name, version, customer_scope, effective_start, effective_end, status, del_flag, create_by, create_time, remark) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0', ?, sysdate(), ?)"), captor.capture());
        assertEquals(Timestamp.class, captor.getValue()[8].getClass());
        assertEquals(Timestamp.class, captor.getValue()[9].getClass());
    }
}
