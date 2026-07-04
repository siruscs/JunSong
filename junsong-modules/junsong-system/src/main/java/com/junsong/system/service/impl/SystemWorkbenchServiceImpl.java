package com.junsong.system.service.impl;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.domain.vo.WorkbenchTaskVO;
import com.junsong.system.service.ISystemWorkbenchService;

/**
 * 统一工作台任务聚合服务实现。
 *
 * 第二阶段来源：
 * - SYSTEM：系统治理问题（未授权菜单、近期登录失败等）。
 * - STOCK：库存底座健康问题（负库存等）。
 * - FINANCE：财务复盘任务（PENDING / IN_PROGRESS）。
 * - MEMBER：会员经营异常（沉默占比过高、积分负债过高等）。
 *
 * 跨模块查询使用 JdbcTemplate 直读同库表（只读），后续可 Feign 化。
 *
 * R7 回修：所有涉及门店的查询（FINANCE/STOCK/MEMBER）均按当前用户授权门店过滤。
 * 超管不过滤；非超管按 sys_user_dept 授权门店过滤；无授权门店时返回空。
 *
 * @author junsong
 */
@Service
public class SystemWorkbenchServiceImpl implements ISystemWorkbenchService {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<WorkbenchTaskVO> aggregateTasks() {
        List<WorkbenchTaskVO> tasks = new ArrayList<>();
        // 解析当前用户授权门店（admin 返回 null 表示不过滤）
        List<Long> authorizedDeptIds = resolveAuthorizedDeptIds();

        tasks.addAll(collectSystemTasks());
        tasks.addAll(collectStockTasks(authorizedDeptIds));
        tasks.addAll(collectFinanceReviewTasks(authorizedDeptIds));
        tasks.addAll(collectMemberTasks(authorizedDeptIds));
        // 按 severity(HIGH>MEDIUM>LOW) 再按 occurTime(新->旧) 排序
        tasks.sort(Comparator
                .comparing((WorkbenchTaskVO t) -> severityRank(t.getSeverity()))
                .thenComparing(WorkbenchTaskVO::getOccurTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return tasks;
    }

    // ==================== 授权门店解析 ====================

    /**
     * 解析当前用户授权门店 ID 列表。
     * - 超管：返回 null（表示不过滤，查询全部）
     * - 非超管：查 sys_user_dept 表获取授权门店列表；无授权时返回 [-1L]（哨兵值，匹配不到任何门店）
     *
     * @return null=不过滤；空列表=异常情况按无授权处理；非空=授权门店列表
     */
    List<Long> resolveAuthorizedDeptIds() {
        try {
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                return Collections.singletonList(-1L);
            }
            // 超管不过滤
            if (SecurityUtils.isAdmin(userId)) {
                return null;
            }
            // 非超管：查 sys_user_dept
            if (jdbcTemplate == null) {
                return Collections.singletonList(-1L);
            }
            List<Long> deptIds = jdbcTemplate.queryForList(
                    "SELECT dept_id FROM sys_user_dept WHERE user_id = ? AND status = '0'",
                    Long.class, userId);
            if (deptIds == null || deptIds.isEmpty()) {
                return Collections.singletonList(-1L);
            }
            return deptIds;
        } catch (Exception e) {
            return Collections.singletonList(-1L);
        }
    }

    /**
     * 构建 SQL dept_id IN (...) 过滤子句。
     * @param authorizedDeptIds null=不过滤（超管），返回空串；非空=返回 " AND dept_id IN (...)"
     */
    private String buildDeptFilter(List<Long> authorizedDeptIds) {
        if (authorizedDeptIds == null) {
            return "";
        }
        if (authorizedDeptIds.isEmpty()) {
            // 无授权：返回匹配不到任何记录的条件
            return " AND dept_id = -1";
        }
        String ids = authorizedDeptIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return " AND dept_id IN (" + ids + ")";
    }

    /**
     * 构建 SQL 带表别名的 dept_id IN (...) 过滤子句。
     * @param alias 表别名（如 "m"、"e"），生成 " AND {alias}.dept_id IN (...)"
     */
    private String buildDeptFilter(List<Long> authorizedDeptIds, String alias) {
        if (authorizedDeptIds == null) {
            return "";
        }
        String col = (alias != null && !alias.isEmpty()) ? alias + ".dept_id" : "dept_id";
        if (authorizedDeptIds.isEmpty()) {
            return " AND " + col + " = -1";
        }
        String ids = authorizedDeptIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return " AND " + col + " IN (" + ids + ")";
    }

    // ==================== SYSTEM ====================

    private List<WorkbenchTaskVO> collectSystemTasks() {
        List<WorkbenchTaskVO> tasks = new ArrayList<>();
        if (jdbcTemplate == null) {
            return tasks;
        }

        long menuWithoutRole = queryCount(
                "SELECT COUNT(*) FROM sys_menu m LEFT JOIN sys_role_menu rm ON m.menu_id = rm.menu_id "
                + "WHERE m.menu_type IN ('C', 'F') AND rm.role_id IS NULL");
        if (menuWithoutRole > 0) {
            tasks.add(new WorkbenchTaskVO("SYSTEM", "MENU_WITHOUT_ROLE", "MEDIUM",
                    "存在未授权菜单/按钮",
                    "有 " + menuWithoutRole + " 个菜单或按钮未关联任何角色，可能存在权限遗漏。",
                    "请到角色管理检查并补齐授权。", "/system/role"));
        }

        long recentLoginFail = queryCount(
                "SELECT COUNT(*) FROM sys_logininfor WHERE status = '1' AND access_time > NOW() - INTERVAL 1 DAY");
        if (recentLoginFail > 0) {
            tasks.add(new WorkbenchTaskVO("SYSTEM", "LOGIN_FAIL", "HIGH",
                    "近24小时存在登录失败",
                    "近24小时有 " + recentLoginFail + " 次登录失败，请关注是否存在暴力破解。",
                    "请到登录日志排查异常来源。", "/monitor/logininfor"));
        }

        return tasks;
    }

    // ==================== STOCK ====================

    private List<WorkbenchTaskVO> collectStockTasks(List<Long> authorizedDeptIds) {
        List<WorkbenchTaskVO> tasks = new ArrayList<>();
        if (jdbcTemplate == null) {
            return tasks;
        }
        if (!tableExists("fin_stock_ledger")) {
            return tasks;
        }

        // R7 回修补强：按门店分组生成任务，每条任务携带 deptId，
        // 通知侧据此过滤接收人，避免 deptId==null 隐式放开给所有授权用户。
        String deptFilter = buildDeptFilter(authorizedDeptIds);
        String sql = "SELECT dept_id, COUNT(DISTINCT product_id) AS neg_cnt "
                + "FROM fin_stock_ledger "
                + "WHERE del_flag = '0' AND after_quantity < 0"
                + deptFilter
                + " GROUP BY dept_id";
        try {
            jdbcTemplate.query(sql, (ResultSet rs, int rowNum) -> {
                Long deptId = rs.getObject("dept_id", Long.class);
                long negCnt = rs.getLong("neg_cnt");
                WorkbenchTaskVO vo = new WorkbenchTaskVO("STOCK", "NEGATIVE_STOCK", "HIGH",
                        "存在负库存流水",
                        "门店 " + deptId + " 有 " + negCnt + " 个商品出现负结存，库存底座不可信。",
                        "请核查采购入库与销售出库记录。", "/finance/stock/health");
                vo.setDeptId(deptId);
                // bizId 带门店维度，避免不同门店通知互相去重覆盖
                vo.setBizId("STOCK:NEGATIVE_STOCK:" + deptId);
                tasks.add(vo);
                return vo;
            });
        } catch (Exception e) {
            // 表结构不匹配时降级为空，不返回假数据
        }

        return tasks;
    }

    // ==================== FINANCE ====================

    /**
     * 聚合财务复盘任务（PENDING / IN_PROGRESS），DONE / IGNORED 不进入工作台。
     * 按当前用户授权门店过滤。
     */
    private List<WorkbenchTaskVO> collectFinanceReviewTasks(List<Long> authorizedDeptIds) {
        List<WorkbenchTaskVO> tasks = new ArrayList<>();
        if (jdbcTemplate == null) {
            return tasks;
        }
        if (!tableExists("finance_review_task")) {
            return tasks;
        }

        String deptFilter = buildDeptFilter(authorizedDeptIds);
        String sql = "SELECT task_id, task_type, dept_id, dept_name, severity, title, reason, suggestion, "
                + "target_route, status, impact_amount, create_time "
                + "FROM finance_review_task "
                + "WHERE del_flag = '0' AND status IN ('PENDING', 'IN_PROGRESS')"
                + deptFilter
                + " ORDER BY create_time DESC LIMIT 50";

        try {
            jdbcTemplate.query(sql, (ResultSet rs, int rowNum) -> {
                WorkbenchTaskVO vo = new WorkbenchTaskVO();
                vo.setSourceModule("FINANCE");
                vo.setTaskType(rs.getString("task_type"));
                vo.setSeverity(safeStr(rs.getString("severity"), "MEDIUM"));
                vo.setDeptId(rs.getObject("dept_id", Long.class));
                vo.setDeptName(rs.getString("dept_name"));
                vo.setTitle(rs.getString("title"));
                vo.setReason(rs.getString("reason"));
                vo.setSuggestion(rs.getString("suggestion"));
                vo.setTargetRoute(rs.getString("target_route"));
                vo.setStatus(rs.getString("status"));
                vo.setBizId("FINANCE:" + rs.getLong("task_id"));
                BigDecimal impact = rs.getBigDecimal("impact_amount");
                vo.setImpactAmount(impact);
                Timestamp ts = rs.getTimestamp("create_time");
                vo.setOccurTime(ts != null ? new java.util.Date(ts.getTime()) : null);
                tasks.add(vo);
                return vo;
            });
        } catch (Exception e) {
            // 表结构不匹配时降级为空，不返回假数据
        }

        return tasks;
    }

    // ==================== MEMBER ====================

    /**
     * 聚合会员经营异常任务。
     * 规则来源：复用 R5 会员经营建议（MemberOperationSuggestionServiceImpl）阈值：
     * - SILENT_MEMBER_HIGH: 沉默会员占比 > 30% -> HIGH
     * - POINTS_LIABILITY_HIGH: 积分负债估算 > 1000 -> MEDIUM
     *
     * 使用 JdbcTemplate 直读同库 mem_member / mem_points_record 表（只读）。
     * 按当前用户授权门店过滤。
     */
    private List<WorkbenchTaskVO> collectMemberTasks(List<Long> authorizedDeptIds) {
        List<WorkbenchTaskVO> tasks = new ArrayList<>();
        if (jdbcTemplate == null) {
            return tasks;
        }
        if (!tableExists("mem_member") || !tableExists("mem_points_record")) {
            return tasks;
        }

        String mFilter = buildDeptFilter(authorizedDeptIds, "m");
        String rFilter = buildDeptFilter(authorizedDeptIds, "r");

        // 按门店聚合会员指标
        String sql = "SELECT m.dept_id, "
                + "COUNT(*) AS total_members, "
                + "COUNT(DISTINCT CASE WHEN p.member_id IS NOT NULL THEN m.member_id END) AS active_members "
                + "FROM mem_member m "
                + "LEFT JOIN mem_points_record p ON p.member_id = m.member_id AND p.create_time > NOW() - INTERVAL 30 DAY "
                + "WHERE m.del_flag = '0' AND m.status = '0'"
                + mFilter
                + " GROUP BY m.dept_id";

        try {
            jdbcTemplate.query(sql, (ResultSet rs) -> {
                long deptId = rs.getLong("dept_id");
                long total = rs.getLong("total_members");
                long active = rs.getLong("active_members");
                if (total == 0) return;
                long silent = total - active;
                double silentRatio = silent * 100.0 / total;

                // SILENT_MEMBER_HIGH
                if (silentRatio > 30) {
                    WorkbenchTaskVO vo = new WorkbenchTaskVO(
                            "MEMBER", "SILENT_MEMBER_HIGH", "HIGH",
                            "沉默会员占比过高",
                            "门店" + deptId + " 沉默会员 " + silent + " 人，占比 " + String.format("%.0f", silentRatio) + "%，超过30%阈值。",
                            "请到会员分层查看沉默会员并制定激活方案。",
                            "/member/segment?segmentType=SILENT");
                    vo.setDeptId(deptId);
                    vo.setStatus("VIEW_ONLY");
                    vo.setBizId("MEMBER:SILENT_MEMBER_HIGH:" + deptId);
                    tasks.add(vo);
                }
            });
        } catch (Exception e) {
            // 降级为空
        }

        // 积分负债
        try {
            String pointsSql = "SELECT r.dept_id, SUM(r.balance) AS total_balance "
                    + "FROM mem_points_record r "
                    + "INNER JOIN (SELECT member_id, MAX(record_id) AS max_id FROM mem_points_record GROUP BY member_id) latest "
                    + "ON r.record_id = latest.max_id "
                    + "WHERE 1=1"
                    + rFilter
                    + " GROUP BY r.dept_id";
            jdbcTemplate.query(pointsSql, (ResultSet rs) -> {
                long deptId = rs.getLong("dept_id");
                BigDecimal totalBalance = rs.getBigDecimal("total_balance");
                if (totalBalance == null) return;
                // 积分按 100:1 估算负债
                BigDecimal liability = totalBalance.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
                if (liability.compareTo(new BigDecimal("1000")) > 0) {
                    WorkbenchTaskVO vo = new WorkbenchTaskVO(
                            "MEMBER", "POINTS_LIABILITY_HIGH", "MEDIUM",
                            "积分负债过高",
                            "门店" + deptId + " 积分沉淀估算负债 " + liability + " 元，超过1000元阈值。",
                            "请到会员积分运营页核查积分过期策略。",
                            "/member/pointsGoods");
                    vo.setDeptId(deptId);
                    vo.setStatus("VIEW_ONLY");
                    vo.setImpactAmount(liability);
                    vo.setBizId("MEMBER:POINTS_LIABILITY_HIGH:" + deptId);
                    tasks.add(vo);
                }
            });
        } catch (Exception e) {
            // 降级为空
        }

        return tasks;
    }

    // ==================== Utils ====================

    private int severityRank(String severity) {
        if ("HIGH".equals(severity)) return 0;
        if ("MEDIUM".equals(severity)) return 1;
        return 2;
    }

    private String safeStr(String val, String defaultVal) {
        return (val == null || val.isEmpty()) ? defaultVal : val;
    }

    private boolean tableExists(String tableName) {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables "
                    + "WHERE table_schema = DATABASE() AND table_name = ?",
                    Long.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private long queryCount(String sql) {
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}
