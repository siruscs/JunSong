package com.junsong.member.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.junsong.common.security.auth.AuthUtil;
import com.junsong.member.domain.MemConfigSyncBatch;
import com.junsong.member.domain.MemConfigSyncDetail;
import com.junsong.member.domain.vo.ConfigSyncPreviewRequest;
import com.junsong.member.domain.vo.ConfigSyncDecision;
import com.junsong.member.domain.vo.ConfigSyncExecuteRequest;
import com.junsong.member.mapper.MemConfigSyncMapper;
import com.junsong.member.service.ConfigSyncDiff;
import com.junsong.member.service.ConfigSyncExecutionDecisions;
import com.junsong.member.service.ConfigSyncAdapter;
import com.junsong.member.service.IMemberConfigSyncService;

@Service
public class MemberConfigSyncServiceImpl implements IMemberConfigSyncService
{
    private static final List<String> SUPPORTED_PREVIEW_TYPES = List.of("PRODUCT", "SUPPLIER", "LEVEL", "CAMPAIGN_POLICY");
    private final JdbcTemplate jdbcTemplate;
    /** JdbcTemplate 可能返回 LocalDateTime，快照序列化必须注册 Java 时间模块。 */
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final MemConfigSyncMapper syncMapper;
    private final FinanceProductConfigSyncAdapter productAdapter;
    private final FinanceSupplierConfigSyncAdapter supplierAdapter;
    private final MemberLevelConfigSyncAdapter levelAdapter;
    private final MemberCampaignPolicyConfigSyncAdapter campaignPolicyAdapter;

    public MemberConfigSyncServiceImpl(JdbcTemplate jdbcTemplate, MemConfigSyncMapper syncMapper,
                                       FinanceProductConfigSyncAdapter productAdapter,
                                       FinanceSupplierConfigSyncAdapter supplierAdapter,
                                       MemberLevelConfigSyncAdapter levelAdapter,
                                       MemberCampaignPolicyConfigSyncAdapter campaignPolicyAdapter)
    {
        this.jdbcTemplate = jdbcTemplate;
        this.syncMapper = syncMapper;
        this.productAdapter = productAdapter;
        this.supplierAdapter = supplierAdapter;
        this.levelAdapter = levelAdapter;
        this.campaignPolicyAdapter = campaignPolicyAdapter;
    }

    @Override
    @Transactional
    public Map<String, Object> preview(ConfigSyncPreviewRequest request, Long tenantId, Long sourceDeptId,
                                       Long userId, String operator)
    {
        validateRequest(request, tenantId, sourceDeptId, userId);
        String type = request.getSyncType().toUpperCase();
        Map<String, Object> source = loadSource(type, request.getSourceRecordId(), sourceDeptId, tenantId);
        if (source == null) throw new IllegalArgumentException("source configuration does not exist or is out of scope");
        List<Map<String, Object>> sources = "LEVEL".equals(type)
                ? loadLevelSources(tenantId, sourceDeptId) : List.of(source);
        String idempotencyKey = request.getIdempotencyKey();
        MemConfigSyncBatch existing = syncMapper.selectBatchByIdempotency(tenantId, idempotencyKey);
        if (existing != null) return result(existing, syncMapper.selectDetails(tenantId, existing.getBatchId()));

        MemConfigSyncBatch batch = new MemConfigSyncBatch();
        batch.setTenantId(tenantId);
        batch.setSourceDeptId(sourceDeptId);
        batch.setSyncType(type);
        batch.setPreviewVersion(1L);
        batch.setStatus("PREVIEWED");
        batch.setIdempotencyKey(idempotencyKey);
        batch.setCreateBy(operator);
        syncMapper.insertBatch(batch);
        List<MemConfigSyncDetail> details = new ArrayList<>();
        for (Map<String, Object> sourceItem : sources)
        {
            String businessKey = String.valueOf(sourceItem.get("businessKey"));
            for (Long targetDeptId : request.getTargetDeptIds())
            {
                ensureTargetAuthorized(userId, targetDeptId, sourceDeptId);
                Map<String, Object> target = loadTarget(type, businessKey, targetDeptId, tenantId);
                Map<String, Object> sourceForTarget = new LinkedHashMap<>(sourceItem);
                if ("CAMPAIGN_POLICY".equals(type)) {
                    Long targetPeriodId = request.getTargetPeriodIds() == null ? null : request.getTargetPeriodIds().get(targetDeptId);
                    sourceForTarget.put("targetPeriodId", targetPeriodId);
                    sourceForTarget.put("period_id", targetPeriodId);
                }
                boolean scopeConflict = target != null && Boolean.TRUE.equals(target.get("scopeConflict"));
                boolean levelInUse = "LEVEL".equals(type) && target != null
                        && ((Number) target.getOrDefault("memberUsageCount", 0)).longValue() > 0;
                Map<String, Object> targetComparable = target == null ? null : new LinkedHashMap<>(target);
                if (targetComparable != null) targetComparable.remove("memberUsageCount");
                ConfigSyncDiff diff = ConfigSyncDiff.compare(type, businessKey, sourceForTarget, targetComparable);
                String operation = scopeConflict ? "CONFLICT"
                        : levelInUse && !diff.fields().isEmpty() ? "IMPACT_BLOCKED" : diff.operation();
                MemConfigSyncDetail detail = new MemConfigSyncDetail();
                detail.setBatchId(batch.getBatchId());
                detail.setTenantId(tenantId);
                detail.setTargetDeptId(targetDeptId);
                if ("CAMPAIGN_POLICY".equals(type)) {
                    Long targetPeriodId = request.getTargetPeriodIds() == null ? null : request.getTargetPeriodIds().get(targetDeptId);
                    if (targetPeriodId == null) throw new IllegalArgumentException("target accounting period is required for policy sync");
                    detail.setTargetPeriodId(targetPeriodId);
                }
                detail.setBusinessKey(businessKey);
                detail.setSourceRecordId(((Number) sourceItem.get("recordId")).longValue());
                if (target != null) detail.setTargetRecordId(((Number) target.get("recordId")).longValue());
                detail.setOperation(operation);
                detail.setDecision("CREATE".equals(operation) ? "CREATE"
                        : "CONFLICT".equals(operation) || "IMPACT_BLOCKED".equals(operation) || "NOOP".equals(operation) ? "SKIP" : null);
                Map<String, Object> detailSource = sourceForTarget;
                if (detail.getTargetPeriodId() != null) detailSource.put("targetPeriodId", detail.getTargetPeriodId());
                detail.setSourceSnapshot(json(detailSource));
                detail.setTargetSnapshot(target == null ? null : json(target));
                if (scopeConflict) {
                    detail.setDiffSnapshot(json(Map.of("reason", "业务编码已存在于其他机构", "conflictDeptId", target.get("conflictDeptId"))));
                } else if (levelInUse) {
                    Map<String, Object> impact = new LinkedHashMap<>();
                    impact.put("reason", "目标机构已有会员正在使用该等级，禁止自动覆盖");
                    impact.put("memberUsageCount", target.get("memberUsageCount"));
                    impact.put("changes", diff.fields());
                    detail.setDiffSnapshot(json(impact));
                } else {
                    detail.setDiffSnapshot(json(diff.fields()));
                }
                detail.setResultStatus("PENDING");
                syncMapper.insertDetail(detail);
                details.add(detail);
            }
        }
        return result(batch, details);
    }

    @Override
    public Map<String, Object> getBatch(Long tenantId, Long batchId)
    {
        if (tenantId == null || batchId == null) throw new IllegalArgumentException("tenant and batch are required");
        MemConfigSyncBatch batch = syncMapper.selectBatch(tenantId, batchId);
        if (batch == null) throw new IllegalArgumentException("sync batch does not exist or is out of scope");
        return result(batch, syncMapper.selectDetails(tenantId, batchId));
    }

    @Override
    @Transactional
    public Map<String, Object> execute(ConfigSyncExecuteRequest request, Long tenantId, Long userId, String operator)
    {
        if (request == null || tenantId == null || userId == null || request.getBatchId() == null)
            throw new IllegalArgumentException("sync batch, tenant and user are required");
        MemConfigSyncBatch batch = syncMapper.selectBatch(tenantId, request.getBatchId());
        if (batch == null) throw new IllegalArgumentException("sync batch does not exist or is out of scope");
        if (!Objects.equals(batch.getPreviewVersion(), request.getPreviewVersion()))
            throw new IllegalArgumentException("sync preview has expired");
        if (!"PREVIEWED".equals(batch.getStatus()))
            throw new IllegalArgumentException("sync batch is not executable");
        String permission = permissionFor(batch.getSyncType());
        AuthUtil.checkPermi(permission);
        List<MemConfigSyncDetail> details = syncMapper.selectDetails(tenantId, batch.getBatchId());
        Map<Long, String> decisions = new LinkedHashMap<>();
        if (request.getDecisions() != null)
            for (ConfigSyncDecision item : request.getDecisions())
                if (item != null && item.getDetailId() != null)
                    decisions.put(item.getDetailId(), item.getDecision());
        Map<Long, String> operations = new LinkedHashMap<>();
        for (MemConfigSyncDetail detail : details) operations.put(detail.getDetailId(), detail.getOperation());
        ConfigSyncExecutionDecisions.validateAll(decisions, operations);
        syncMapper.updateBatchStatus(tenantId, batch.getBatchId(), "EXECUTING", operator);
        for (MemConfigSyncDetail detail : details)
        {
            String decision = ConfigSyncExecutionDecisions.resolve(detail.getOperation(), decisions.get(detail.getDetailId()));
            syncMapper.updateDetailDecision(tenantId, batch.getBatchId(), detail.getDetailId(), decision);
            if ("SKIP".equals(decision))
            {
                detail.setResultStatus("CONFLICT".equals(detail.getOperation()) || "IMPACT_BLOCKED".equals(detail.getOperation()) ? "CONFLICT" : "SKIPPED");
                if ("CONFLICT".equals(detail.getOperation()))
                {
                    detail.setErrorCode("BUSINESS_KEY_CONFLICT");
                    detail.setErrorMessage("业务编码已存在于其他机构，无法在目标机构新增");
                }
                if ("IMPACT_BLOCKED".equals(detail.getOperation()))
                {
                    detail.setErrorCode("MEMBER_LEVEL_IN_USE");
                    detail.setErrorMessage("目标机构已有会员使用该等级，配置存在差异，禁止自动覆盖");
                }
                syncMapper.updateDetailResult(detail);
                continue;
            }
            executeDetail(batch, detail, decision, operator);
        }
        syncMapper.updateBatchStatus(tenantId, batch.getBatchId(), "COMPLETED", operator);
        return getBatch(tenantId, batch.getBatchId());
    }

    private void executeDetail(MemConfigSyncBatch batch, MemConfigSyncDetail detail, String decision, String operator)
    {
        Map<String, Object> source = readSnapshot(detail.getSourceSnapshot());
        Map<String, Object> expectedTarget = detail.getTargetSnapshot() == null ? null : readSnapshot(detail.getTargetSnapshot());
        Map<String, Object> currentTarget = loadTarget(batch.getSyncType(), detail.getBusinessKey(), detail.getTargetDeptId(), batch.getTenantId());
        if ("CREATE".equals(decision) && currentTarget != null)
        {
            markConflict(detail, Boolean.TRUE.equals(currentTarget.get("scopeConflict"))
                    ? "业务编码已存在于其他机构，无法在目标机构新增"
                    : "目标机构在预览后已创建该配置",
                    Boolean.TRUE.equals(currentTarget.get("scopeConflict"))
                            ? "BUSINESS_KEY_CONFLICT" : "PREVIEW_STALE");
            return;
        }
        if ("OVERWRITE".equals(decision)
                && !ConfigSyncDiff.equivalent(expectedTarget, currentTarget))
        {
            markConflict(detail, "target configuration changed after preview");
            return;
        }
        ConfigSyncAdapter adapter = adapterFor(batch.getSyncType());
        int affected = "CREATE".equals(decision)
                ? adapter.create(source, detail.getTargetDeptId(), operator)
                : adapter.overwrite(source, currentTarget, detail.getTargetDeptId(), operator);
        if (affected != 1)
        {
            markConflict(detail, "target configuration was changed by another operation");
            return;
        }
        detail.setResultStatus("SUCCESS");
        detail.setTargetRecordId(currentTarget == null ? null : ((Number) currentTarget.get("recordId")).longValue());
        syncMapper.updateDetailResult(detail);
    }

    private Map<String, Object> readSnapshot(String value)
    {
        try { return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() { }); }
        catch (JsonProcessingException e) { throw new IllegalStateException("invalid sync snapshot"); }
    }

    private void markConflict(MemConfigSyncDetail detail, String message)
    {
        markConflict(detail, message, "PREVIEW_STALE");
    }

    private void markConflict(MemConfigSyncDetail detail, String message, String errorCode)
    {
        detail.setResultStatus("CONFLICT");
        detail.setErrorCode(errorCode);
        detail.setErrorMessage(message);
        syncMapper.updateDetailResult(detail);
    }

    private Map<String, Object> loadSource(String type, Long recordId, Long deptId, Long tenantId)
    {
        String predicate = "LEVEL".equals(type)
                ? "record_id = ? and (dept_id = ? or dept_id = 0)"
                : "record_id = ? and dept_id = ?";
        return load(type, "tenant_id = ? and " + predicate, tenantId, recordId, deptId);
    }

    private List<Map<String, Object>> loadLevelSources(Long tenantId, Long deptId)
    {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select type_id, tenant_id, dept_id, type_code, type_name, card_fee, discount_rate, points_rate, min_growth, sign_in_points, status "
                        + "from mem_member_card_type where tenant_id = ? and del_flag = '0' "
                        + "and (dept_id = ? or (dept_id = 0 and not exists (select 1 from mem_member_card_type scoped "
                        + "where scoped.tenant_id = mem_member_card_type.tenant_id and scoped.type_code = mem_member_card_type.type_code "
                        + "and scoped.dept_id = ? and scoped.del_flag = '0'))) order by min_growth, type_id", tenantId, deptId, deptId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> raw : rows)
        {
            Map<String, Object> row = new LinkedHashMap<>(raw);
            row.put("recordId", row.remove("type_id"));
            row.put("businessKey", row.remove("type_code"));
            row.put("displayName", row.remove("type_name"));
            row.put("tenantId", row.remove("tenant_id"));
            result.add(row);
        }
        if (result.isEmpty()) throw new IllegalArgumentException("source member levels do not exist or are out of scope");
        return result;
    }

    private Map<String, Object> loadTarget(String type, String businessKey, Long deptId, Long tenantId)
    {
        return load(type, "tenant_id = ? and business_key = ? and dept_id = ?", tenantId, businessKey, deptId);
    }

    private Map<String, Object> load(String type, String predicate, Object... args)
    {
        String table;
        String keyColumn;
        String idColumn;
        String nameColumn;
        String fields;
        if ("PRODUCT".equals(type)) {
            table = "fin_product"; keyColumn = "product_code"; idColumn = "product_id"; nameColumn = "product_name";
            fields = "product_id, tenant_id, dept_id, product_code, product_name, category_id, unit, purchase_price, sale_price, min_stock, status, remark";
        } else if ("SUPPLIER".equals(type)) {
            table = "fin_supplier"; keyColumn = "supplier_code"; idColumn = "supplier_id"; nameColumn = "supplier_name";
            fields = "supplier_id, tenant_id, dept_id, supplier_code, supplier_name, contact_person, contact_phone, address, status, remark";
        } else if ("LEVEL".equals(type)) {
            table = "mem_member_card_type"; keyColumn = "type_code"; idColumn = "type_id"; nameColumn = "type_name";
            fields = "type_id, tenant_id, dept_id, type_code, type_name, card_fee, discount_rate, points_rate, min_growth, sign_in_points, status";
        } else if ("CAMPAIGN_POLICY".equals(type)) {
            boolean byBusinessKey = predicate.contains("business_key");
            boolean tenantScoped = predicate.contains("tenant_id = ?");
            String lookupSql = "select p.policy_id, p.tenant_id, p.dept_id, p.period_id, p.product_id, p.policy_no, p.policy_name, p.version, p.customer_scope, p.effective_start, p.effective_end, p.status, p.remark, f.product_code from mem_campaign_policy p join fin_product f on f.product_id=p.product_id and f.dept_id=p.dept_id where "
                    + (tenantScoped ? "p.tenant_id=? and " : "")
                    + "p." + (byBusinessKey ? "policy_no" : "policy_id") + "=? and p.dept_id=? and p.del_flag='0' order by p.version desc limit 1";
            Object[] lookupArgs = tenantScoped
                    ? new Object[] { args[0], args[1], args[2] }
                    : new Object[] { args[0], args[1] };
            List<Map<String, Object>> policies = jdbcTemplate.queryForList(lookupSql, lookupArgs);
            if (policies.isEmpty()) return null;
            Map<String, Object> row = new LinkedHashMap<>(policies.get(0));
            row.put("recordId", row.remove("policy_id"));
            row.put("businessKey", row.remove("policy_no"));
            row.put("displayName", row.remove("policy_name"));
            row.put("productCode", row.remove("product_code"));
            row.put("targetPeriodId", row.get("period_id"));
            row.put("packages", jdbcTemplate.queryForList("select package_name as packageName, purchase_quantity as purchaseQuantity, gift_quantity as giftQuantity, total_quantity as totalQuantity, package_price as packagePrice, sort_no as sortNo from mem_campaign_policy_package where policy_id=? and tenant_id=? and dept_id=? and del_flag='0' order by purchase_quantity desc, sort_no", row.get("recordId"), row.get("tenant_id"), args[2]));
            row.put("tenantId", row.remove("tenant_id"));
            return row;
        } else {
            throw new IllegalArgumentException("sync type is not ready for preview");
        }
        String sqlPredicate = predicate.replace("record_id", idColumn).replace("business_key", keyColumn);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select " + fields + " from " + table + " where " + sqlPredicate + " and del_flag = '0' limit 1", args);
        if (rows.isEmpty()) return null;
        Map<String, Object> row = new LinkedHashMap<>(rows.get(0));
        row.put("recordId", row.remove(idColumn));
        row.put("businessKey", row.remove(keyColumn));
        row.put("displayName", row.remove(nameColumn));
        if ("LEVEL".equals(type)) {
            Integer memberUsageCount = jdbcTemplate.queryForObject(
                    "select count(1) from mem_member where tenant_id = ? and dept_id = ? and card_type = ? and del_flag = '0'",
                    Integer.class, args[0], args[2], row.get("businessKey"));
            row.put("memberUsageCount", memberUsageCount == null ? 0 : memberUsageCount);
        }
        if ("PRODUCT".equals(type) || "SUPPLIER".equals(type) || "LEVEL".equals(type)) row.put("tenantId", row.remove("tenant_id"));
        return row;
    }

    private void validateRequest(ConfigSyncPreviewRequest request, Long tenantId, Long sourceDeptId, Long userId)
    {
        if (tenantId == null || sourceDeptId == null || userId == null || request == null
                || request.getSourceRecordId() == null || request.getTargetDeptIds() == null
                || request.getTargetDeptIds().isEmpty() || request.getIdempotencyKey() == null
                || request.getIdempotencyKey().isBlank())
            throw new IllegalArgumentException("sync source, targets, user and idempotency key are required");
        if (!SUPPORTED_PREVIEW_TYPES.contains(request.getSyncType() == null ? "" : request.getSyncType().toUpperCase()))
            throw new IllegalArgumentException("sync type is not ready for preview");
        String type = request.getSyncType().toUpperCase();
        AuthUtil.checkPermi(permissionFor(type));
    }

    private ConfigSyncAdapter adapterFor(String type)
    {
        return switch (type) {
            case "PRODUCT" -> productAdapter;
            case "SUPPLIER" -> supplierAdapter;
            case "LEVEL" -> levelAdapter;
            case "CAMPAIGN_POLICY" -> campaignPolicyAdapter;
            default -> throw new IllegalArgumentException("sync type is not ready for execution");
        };
    }

    private String permissionFor(String type)
    {
        return switch (type) {
            case "PRODUCT" -> "finance:product:sync";
            case "SUPPLIER" -> "finance:supplier:sync";
            case "LEVEL" -> "member:level:sync";
            case "CAMPAIGN_POLICY" -> "member:campaignPolicy:sync";
            default -> throw new IllegalArgumentException("sync type is not ready");
        };
    }

    private void ensureTargetAuthorized(Long userId, Long targetDeptId, Long sourceDeptId)
    {
        if (targetDeptId == null || Objects.equals(targetDeptId, sourceDeptId))
            throw new IllegalArgumentException("target institution must differ from source institution");
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from sys_user_dept where user_id = ? and dept_id = ? and status = '0'",
                Integer.class, userId, targetDeptId);
        if (count == null || count != 1) throw new IllegalArgumentException("target institution is out of scope");
    }

    private String json(Object value)
    {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("failed to create sync snapshot", e); }
    }

    private Map<String, Object> result(MemConfigSyncBatch batch, List<MemConfigSyncDetail> details)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("details", details);
        return result;
    }
}
