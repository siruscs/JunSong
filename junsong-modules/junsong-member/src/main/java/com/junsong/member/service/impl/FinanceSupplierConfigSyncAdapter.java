package com.junsong.member.service.impl;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.junsong.member.service.ConfigSyncAdapter;

@Component
public class FinanceSupplierConfigSyncAdapter implements ConfigSyncAdapter
{
    private static final String INSERT_SQL = "insert into fin_supplier (supplier_code, supplier_name, contact_person, contact_phone, address, status, dept_id, tenant_id, create_by, create_time, remark) values (?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate(), ?)";
    private final JdbcTemplate jdbcTemplate;

    public FinanceSupplierConfigSyncAdapter(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    @Override public String type() { return "SUPPLIER"; }
    @Override public int create(Map<String, Object> source, Long targetDeptId, String operator)
    {
        return jdbcTemplate.update(INSERT_SQL, source.get("businessKey"), source.get("displayName"), source.get("contact_person"),
                source.get("contact_phone"), source.get("address"), source.get("status"), targetDeptId, source.get("tenantId"), operator, source.get("remark"));
    }
    @Override public int overwrite(Map<String, Object> source, Map<String, Object> target, Long targetDeptId, String operator)
    {
        return jdbcTemplate.update("update fin_supplier set supplier_name=?, contact_person=?, contact_phone=?, address=?, status=?, update_by=?, update_time=sysdate(), remark=? where supplier_id=? and dept_id=? and del_flag='0'",
                source.get("displayName"), source.get("contact_person"), source.get("contact_phone"), source.get("address"),
                source.get("status"), operator, source.get("remark"), target.get("recordId"), targetDeptId);
    }
}
