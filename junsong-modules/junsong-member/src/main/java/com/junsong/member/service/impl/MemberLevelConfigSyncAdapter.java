package com.junsong.member.service.impl;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.junsong.member.service.ConfigSyncAdapter;

/** 会员等级配置同步适配器。只复制等级规则，不复制会员关联和来源主键。 */
@Component
public class MemberLevelConfigSyncAdapter implements ConfigSyncAdapter
{
    private static final String INSERT_SQL = "insert into mem_member_card_type (tenant_id, dept_id, type_name, type_code, card_fee, discount_rate, points_rate, min_growth, sign_in_points, status, create_by, create_time) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate())";
    private final JdbcTemplate jdbcTemplate;

    public MemberLevelConfigSyncAdapter(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override public String type() { return "LEVEL"; }

    @Override public int create(Map<String, Object> source, Long targetDeptId, String operator)
    {
        return jdbcTemplate.update(INSERT_SQL, source.get("tenantId"), targetDeptId, source.get("displayName"),
                source.get("businessKey"), source.get("card_fee"), source.get("discount_rate"),
                source.get("points_rate"), source.get("min_growth"), source.get("sign_in_points"),
                source.get("status"), operator);
    }

    @Override public int overwrite(Map<String, Object> source, Map<String, Object> target,
                                   Long targetDeptId, String operator)
    {
        return jdbcTemplate.update("update mem_member_card_type set type_name=?, card_fee=?, discount_rate=?, points_rate=?, min_growth=?, sign_in_points=?, status=?, update_by=?, update_time=sysdate() where type_id=? and dept_id=? and del_flag='0'",
                source.get("displayName"), source.get("card_fee"), source.get("discount_rate"),
                source.get("points_rate"), source.get("min_growth"), source.get("sign_in_points"),
                source.get("status"), operator, target.get("recordId"), targetDeptId);
    }
}
