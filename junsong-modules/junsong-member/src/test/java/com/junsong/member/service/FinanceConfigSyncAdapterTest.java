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
import com.junsong.member.service.impl.FinanceProductConfigSyncAdapter;
import com.junsong.member.service.impl.FinanceSupplierConfigSyncAdapter;

class FinanceConfigSyncAdapterTest
{
    @Test
    void productAdapterCopiesBusinessFieldsButNotStock()
    {
        JdbcTemplate jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(1);
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("businessKey", "GEL-01"); source.put("displayName", "凝胶"); source.put("sale_price", 88);
        source.put("stock_num", 999);
        int rows = new FinanceProductConfigSyncAdapter(jdbc).create(source, 20L, "admin");
        assertEquals(1, rows);
        verify(jdbc).update(eq("insert into fin_product (product_code, product_name, category_id, unit, purchase_price, sale_price, min_stock, status, dept_id, tenant_id, create_by, create_time, remark) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate(), ?)"), any(Object[].class));
    }

    @Test
    void supplierAdapterHasSeparateBusinessType()
    {
        assertEquals("SUPPLIER", new FinanceSupplierConfigSyncAdapter(org.mockito.Mockito.mock(JdbcTemplate.class)).type());
    }
}
