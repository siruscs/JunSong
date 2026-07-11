package com.junsong.member.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.mapper.MemMpDashboardMapper;
import com.junsong.member.service.IMemMpDashboardService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 小程序移动端首页聚合看板服务实现。
 *
 * 多租户与部门边界参考 MemDashboardController.loadAllowedDeptIds/resolveDeptIds/inFilter
 * 三件套：admin 不限制；非 admin 取 RemoteUserService.getUserDeptList 授权门店列表，
 * 与请求 deptId 求交集；无可见部门时返回哨兵 [-1L]（确保不越权）。
 */
@Service
public class MemMpDashboardServiceImpl implements IMemMpDashboardService {

    private static final Logger log = LoggerFactory.getLogger(MemMpDashboardServiceImpl.class);

    /** 哨兵部门 ID：表示无可见部门，用于在 SQL IN (?,?,?) 中匹配零行。 */
    private static final List<Long> SENTINEL_DEPT_IDS = Collections.singletonList(-1L);

    @Autowired
    private MemMpDashboardMapper dashboardMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    @Override
    public Map<String, Object> getOverview(List<String> accessibleModules) {
        Long tenantId = TenantContext.getTenantId();
        Long currentDeptId = SecurityUtils.getDeptId();
        List<Long> deptIds = resolveDeptIds(null); // 不接受外部 deptId，统一走授权范围

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("deptId", currentDeptId);
        result.put("deptName", "");

        Set<String> modules = accessibleModules == null ? Collections.emptySet()
                : new HashSet<>(accessibleModules);

        // 会员模块（member）
        if (modules.contains("member")) {
            try {
                Map<String, Object> member = dashboardMapper.queryMemberOverview(tenantId, deptIds);
                result.put("member", normalizeMember(member));
            } catch (Exception e) {
                log.warn("queryMemberOverview failed", e);
                result.put("member", emptyMember());
            }
        }

        // 成长体系（依赖 member 模块权限）
        if (modules.contains("member")) {
            try {
                Map<String, Object> growth = dashboardMapper.queryGrowthOverview(tenantId, deptIds);
                result.put("growth", normalizeGrowth(growth));
            } catch (Exception e) {
                log.warn("queryGrowthOverview failed", e);
                result.put("growth", emptyGrowth());
            }
        }

        // 积分运营（pointsRecord / pointsExchange）
        if (modules.contains("pointsRecord") || modules.contains("pointsExchange")) {
            try {
                Map<String, Object> points = dashboardMapper.queryPointsOverview(tenantId, deptIds);
                result.put("points", normalizePoints(points));
            } catch (Exception e) {
                log.warn("queryPointsOverview failed", e);
                result.put("points", emptyPoints());
            }
        }

        // 等级分布（依赖 member 模块权限）
        if (modules.contains("member")) {
            try {
                List<Map<String, Object>> levelDist = dashboardMapper.queryLevelDistribution(tenantId, deptIds);
                Map<String, Object> level = new HashMap<>();
                level.put("distribution", levelDist != null ? levelDist : Collections.emptyList());
                result.put("level", level);
            } catch (Exception e) {
                log.warn("queryLevelDistribution failed", e);
                Map<String, Object> level = new HashMap<>();
                level.put("distribution", Collections.emptyList());
                result.put("level", level);
            }
        }

        // 分层洞察（依赖 member 模块权限）
        if (modules.contains("member")) {
            try {
                List<Map<String, Object>> segDist = dashboardMapper.querySegmentDistribution(tenantId, deptIds);
                Map<String, Object> segment = new HashMap<>();
                segment.put("distribution", segDist != null ? segDist : Collections.emptyList());
                result.put("segment", segment);
            } catch (Exception e) {
                log.warn("querySegmentDistribution failed", e);
                Map<String, Object> segment = new HashMap<>();
                segment.put("distribution", Collections.emptyList());
                result.put("segment", segment);
            }
        }

        // 活动表现（seckill / seckillRecord）
        if (modules.contains("seckill") || modules.contains("seckillRecord")) {
            try {
                Map<String, Object> activity = dashboardMapper.queryActivityOverview(tenantId, deptIds);
                result.put("activity", normalizeActivity(activity));
            } catch (Exception e) {
                log.warn("queryActivityOverview failed", e);
                result.put("activity", emptyActivity());
            }
        }

        // 财务今日经营（expense / advance / sale）
        if (modules.contains("expense") || modules.contains("advance") || modules.contains("sale")) {
            try {
                Map<String, Object> finance = dashboardMapper.queryFinanceOverview(tenantId, deptIds);
                result.put("finance", normalizeFinance(finance));
            } catch (Exception e) {
                log.warn("queryFinanceOverview failed", e);
                result.put("finance", emptyFinance());
            }
        }

        return result;
    }

    // ==================== 多租户 + 部门授权边界 ====================

    /**
     * 加载当前用户授权门店列表。
     * - admin：返回空列表（不限）
     * - 非 admin：取 RemoteUserService.getUserDeptList；失败回退当前 deptId；
     *   都没有则返回哨兵 [-1L]
     */
    List<Long> loadAllowedDeptIds() {
        if (SecurityUtils.isAdmin()) {
            return Collections.emptyList();
        }
        try {
            R<List<SysDept>> r = remoteUserService.getUserDeptList(
                    SecurityUtils.getUsername(), SecurityConstants.INNER);
            if (R.isSuccess(r) && r.getData() != null && !r.getData().isEmpty()) {
                List<Long> ids = new ArrayList<>();
                for (SysDept dept : r.getData()) {
                    if (dept.getDeptId() != null) {
                        ids.add(dept.getDeptId());
                    }
                }
                if (!ids.isEmpty()) {
                    return ids;
                }
            }
        } catch (Exception e) {
            log.warn("loadAllowedDeptIds remote call failed", e);
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId != null) {
            return Collections.singletonList(deptId);
        }
        return SENTINEL_DEPT_IDS;
    }

    /**
     * 解析最终部门范围。
     * - admin：返回空列表（SQL 不加 dept_id IN 过滤，仅按 tenant_id 过滤）
     * - 非 admin：与请求 deptIds 求交集；交集为空返回授权范围；授权范围为空返回哨兵
     */
    List<Long> resolveDeptIds(List<Long> requestedDeptIds) {
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            // admin：无请求参数时返回空（不限）；有请求参数时按请求
            if (requestedDeptIds == null || requestedDeptIds.isEmpty()) {
                return Collections.emptyList();
            }
            return requestedDeptIds;
        }
        if (requestedDeptIds == null || requestedDeptIds.isEmpty()) {
            return allowed;
        }
        Set<Long> allowedSet = new HashSet<>(allowed);
        List<Long> intersection = new ArrayList<>();
        for (Long id : requestedDeptIds) {
            if (allowedSet.contains(id)) {
                intersection.add(id);
            }
        }
        if (intersection.isEmpty()) {
            return SENTINEL_DEPT_IDS;
        }
        return intersection;
    }

    // ==================== 字段归一化（防 null / 类型不一致） ====================

    private Map<String, Object> normalizeMember(Map<String, Object> raw) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (raw == null) raw = Collections.emptyMap();
        long total = toLong(raw.get("totalMembers"));
        long today = toLong(raw.get("todayMembers"));
        long active = toLong(raw.get("activeMembers"));
        m.put("totalMembers", total);
        m.put("todayMembers", today);
        m.put("activeMembers", active);
        m.put("silentMembers", Math.max(total - active, 0));
        return m;
    }

    private Map<String, Object> emptyMember() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalMembers", 0L);
        m.put("todayMembers", 0L);
        m.put("activeMembers", 0L);
        m.put("silentMembers", 0L);
        return m;
    }

    private Map<String, Object> normalizeGrowth(Map<String, Object> raw) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (raw == null) raw = Collections.emptyMap();
        long todaySignIn = toLong(raw.get("todaySignInCount"));
        long avgGrowth = toLong(raw.get("avgGrowthValue"));
        long pending = toLong(raw.get("pendingGrowthActions"));
        long completed = toLong(raw.get("completedGrowthActions"));
        long total = toLong(raw.get("totalGrowthActions"));
        m.put("todaySignInCount", todaySignIn);
        m.put("avgGrowthValue", avgGrowth);
        m.put("pendingGrowthActions", pending);
        m.put("completedGrowthActions", completed);
        m.put("growthActionEffectRate", total > 0 ? Math.round(completed * 100.0 / total) : 0);
        return m;
    }

    private Map<String, Object> emptyGrowth() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todaySignInCount", 0L);
        m.put("avgGrowthValue", 0L);
        m.put("pendingGrowthActions", 0L);
        m.put("completedGrowthActions", 0L);
        m.put("growthActionEffectRate", 0);
        return m;
    }

    private Map<String, Object> normalizePoints(Map<String, Object> raw) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (raw == null) raw = Collections.emptyMap();
        m.put("totalAvailablePoints", toLong(raw.get("totalAvailablePoints")));
        m.put("todayPointsIssued", toLong(raw.get("todayPointsIssued")));
        m.put("todayPointsConsumed", toLong(raw.get("todayPointsConsumed")));
        m.put("pendingExchangeCount", toLong(raw.get("pendingExchangeCount")));
        return m;
    }

    private Map<String, Object> emptyPoints() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalAvailablePoints", 0L);
        m.put("todayPointsIssued", 0L);
        m.put("todayPointsConsumed", 0L);
        m.put("pendingExchangeCount", 0L);
        return m;
    }

    private Map<String, Object> normalizeActivity(Map<String, Object> raw) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (raw == null) raw = Collections.emptyMap();
        m.put("activeSeckillCount", toLong(raw.get("activeSeckillCount")));
        m.put("todayActivityMembers", toLong(raw.get("todayActivityMembers")));
        m.put("todayActivityAmount", toBigDecimal(raw.get("todayActivityAmount")));
        return m;
    }

    private Map<String, Object> emptyActivity() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activeSeckillCount", 0L);
        m.put("todayActivityMembers", 0L);
        m.put("todayActivityAmount", BigDecimal.ZERO);
        return m;
    }

    private Map<String, Object> normalizeFinance(Map<String, Object> raw) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (raw == null) raw = Collections.emptyMap();
        m.put("todaySale", toBigDecimal(raw.get("todaySale")));
        m.put("todayExpense", toBigDecimal(raw.get("todayExpense")));
        m.put("unverifiedExpense", toBigDecimal(raw.get("unverifiedExpense")));
        m.put("unverifiedAdvance", toBigDecimal(raw.get("unverifiedAdvance")));
        return m;
    }

    private Map<String, Object> emptyFinance() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todaySale", BigDecimal.ZERO);
        m.put("todayExpense", BigDecimal.ZERO);
        m.put("unverifiedExpense", BigDecimal.ZERO);
        m.put("unverifiedAdvance", BigDecimal.ZERO);
        return m;
    }

    private long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(((Number) v).toString());
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
