package com.junsong.member.service.impl;

import java.util.List;
import java.util.Map;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.junsong.member.service.ConfigSyncAdapter;

/** 政策同步适配器：按商品编码解析目标商品，按目标核算周期落地政策和套餐快照。 */
@Component
public class MemberCampaignPolicyConfigSyncAdapter implements ConfigSyncAdapter
{
    private static final String POLICY_INSERT = "insert into mem_campaign_policy (tenant_id, dept_id, period_id, product_id, policy_no, policy_name, version, customer_scope, effective_start, effective_end, status, del_flag, create_by, create_time, remark) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0', ?, sysdate(), ?)";
    private final JdbcTemplate jdbcTemplate;

    public MemberCampaignPolicyConfigSyncAdapter(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override public String type() { return "CAMPAIGN_POLICY"; }

    @Override public int create(Map<String, Object> source, Long targetDeptId, String operator)
    {
        Long targetPeriodId = requiredLong(source, "targetPeriodId", "target accounting period is required");
        Long productId = resolveProduct(source, targetDeptId);
        source.put("targetDeptId", targetDeptId);
        jdbcTemplate.update(POLICY_INSERT, source.get("tenantId"), targetDeptId, targetPeriodId, productId,
                source.get("businessKey"), source.get("displayName"), source.get("version"),
                source.get("customer_scope"), toSqlDate(source.get("effective_start")), toSqlDate(source.get("effective_end")),
                source.get("status"), operator, source.get("remark"));
        Long policyId = jdbcTemplate.queryForObject(
                "select policy_id from mem_campaign_policy where tenant_id=? and dept_id=? and policy_no=? and version=? and del_flag='0' limit 1",
                Long.class, source.get("tenantId"), targetDeptId, source.get("businessKey"), source.get("version"));
        if (policyId == null) throw new IllegalStateException("created policy id cannot be resolved");
        insertPackages(policyId, source, operator);
        return 1;
    }

    @Override public int overwrite(Map<String, Object> source, Map<String, Object> target,
                                   Long targetDeptId, String operator)
    {
        Long targetPeriodId = requiredLong(source, "targetPeriodId", "target accounting period is required");
        Long productId = resolveProduct(source, targetDeptId);
        source.put("targetDeptId", targetDeptId);
        Long policyId = ((Number) target.get("recordId")).longValue();
        int affected = jdbcTemplate.update("update mem_campaign_policy set period_id=?, product_id=?, policy_name=?, customer_scope=?, effective_start=?, effective_end=?, status=?, update_by=?, update_time=sysdate(), remark=? where policy_id=? and tenant_id=? and dept_id=? and del_flag='0'",
                targetPeriodId, productId, source.get("displayName"), source.get("customer_scope"),
                toSqlDate(source.get("effective_start")), toSqlDate(source.get("effective_end")), source.get("status"), operator,
                source.get("remark"), policyId, source.get("tenantId"), targetDeptId);
        if (affected == 1) {
            jdbcTemplate.update("delete from mem_campaign_policy_package where policy_id=? and tenant_id=? and dept_id=? and del_flag='0'",
                    policyId, source.get("tenantId"), targetDeptId);
            insertPackages(policyId, source, operator);
        }
        return affected;
    }

    private Long resolveProduct(Map<String, Object> source, Long targetDeptId)
    {
        Object code = source.get("productCode");
        if (code == null || String.valueOf(code).isBlank()) throw new IllegalArgumentException("target product code is required");
        Long productId = jdbcTemplate.queryForObject(
                "select product_id from fin_product where product_code=? and dept_id=? and del_flag='0' limit 1",
                Long.class, code, targetDeptId);
        if (productId == null) throw new IllegalArgumentException("target product does not exist");
        return productId;
    }

    @SuppressWarnings("unchecked")
    private void insertPackages(Long policyId, Map<String, Object> source, String operator)
    {
        Object value = source.get("packages");
        if (!(value instanceof List<?> packages) || packages.isEmpty())
            throw new IllegalArgumentException("销售政策至少需要一个套餐档位");
        for (Object item : packages) {
            if (!(item instanceof Map<?, ?> raw)) throw new IllegalArgumentException("invalid policy package snapshot");
            Map<String, Object> pkg = (Map<String, Object>) raw;
            jdbcTemplate.update("insert into mem_campaign_policy_package (policy_id, tenant_id, dept_id, package_name, purchase_quantity, gift_quantity, total_quantity, package_price, sort_no, del_flag, create_by, create_time) values (?, ?, ?, ?, ?, ?, ?, ?, ?, '0', ?, sysdate())",
                    policyId, source.get("tenantId"), source.get("targetDeptId"), pkg.get("packageName"),
                    pkg.get("purchaseQuantity"), pkg.get("giftQuantity"), pkg.get("totalQuantity"),
                    pkg.get("packagePrice"), pkg.get("sortNo"), operator);
        }
    }

    private Long requiredLong(Map<String, Object> source, String key, String message)
    {
        Object value = source.get(key);
        if (!(value instanceof Number)) throw new IllegalArgumentException(message);
        return ((Number) value).longValue();
    }

    private Timestamp toSqlDate(Object value)
    {
        if (value == null) return null;
        if (value instanceof Timestamp timestamp) return timestamp;
        if (value instanceof java.util.Date date) return new Timestamp(date.getTime());
        if (value instanceof List<?> values)
        {
            if (values.size() < 5) throw new IllegalArgumentException("政策有效期格式无效");
            return Timestamp.valueOf(LocalDateTime.of(((Number) values.get(0)).intValue(), ((Number) values.get(1)).intValue(), ((Number) values.get(2)).intValue(), ((Number) values.get(3)).intValue(), ((Number) values.get(4)).intValue(), values.size() > 5 ? ((Number) values.get(5)).intValue() : 0));
        }
        String text = String.valueOf(value);
        try { return Timestamp.valueOf(LocalDateTime.parse(text)); }
        catch (RuntimeException ignored) { return Timestamp.from(OffsetDateTime.parse(text).toInstant()); }
    }
}
