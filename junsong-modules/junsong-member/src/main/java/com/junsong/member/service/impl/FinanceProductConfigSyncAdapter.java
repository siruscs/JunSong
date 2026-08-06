package com.junsong.member.service.impl;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.junsong.member.service.ConfigSyncAdapter;

@Component
public class FinanceProductConfigSyncAdapter implements ConfigSyncAdapter
{
    private static final String INSERT_SQL = "insert into fin_product (product_code, product_name, category_id, unit, purchase_price, sale_price, min_stock, status, dept_id, tenant_id, create_by, create_time, remark) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate(), ?)";
    private final JdbcTemplate jdbcTemplate;

    public FinanceProductConfigSyncAdapter(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    @Override public String type() { return "PRODUCT"; }
    @Override public int create(Map<String, Object> source, Long targetDeptId, String operator)
    {
        return jdbcTemplate.update(INSERT_SQL, source.get("businessKey"), source.get("displayName"), source.get("category_id"),
                source.get("unit"), source.get("purchase_price"), source.get("sale_price"), source.get("min_stock"),
                source.get("status"), targetDeptId, source.get("tenantId"), operator, source.get("remark"));
    }
    @Override public int overwrite(Map<String, Object> source, Map<String, Object> target, Long targetDeptId, String operator)
    {
        return jdbcTemplate.update("update fin_product set product_name=?, category_id=?, unit=?, purchase_price=?, sale_price=?, min_stock=?, status=?, update_by=?, update_time=sysdate(), remark=? where product_id=? and dept_id=? and del_flag='0'",
                source.get("displayName"), source.get("category_id"), source.get("unit"), source.get("purchase_price"),
                source.get("sale_price"), source.get("min_stock"), source.get("status"), operator, source.get("remark"),
                target.get("recordId"), targetDeptId);
    }
}
