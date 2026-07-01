package com.junsong.member.controller;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.bo.MemberSegmentQueryBO;
import com.junsong.member.domain.vo.MemberSegmentMemberVO;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;

/**
 * 会员分层钻取接口：按 NEW/ACTIVE/SILENT/HIGH_VALUE/LOW_POINTS/HIGH_POINTS 查询会员清单。
 * 手机号强制脱敏，只能查授权门店。
 */
@RestController
@RequestMapping("/segment")
public class MemberSegmentController extends BaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RemoteUserService remoteUserService;

    // ==================== endpoint ====================

    @RequiresPermissions("member:segment:list")
    @PostMapping("/list")
    public AjaxResult list(@RequestBody MemberSegmentQueryBO query) {
        if (query == null) {
            return AjaxResult.error("查询参数不能为空");
        }
        List<Long> requested = query.getDeptId() == null
                ? Collections.emptyList()
                : Collections.singletonList(query.getDeptId());
        List<Long> resolved = resolveDeptIds(requested);
        // 非法或越权请求直接返回空清单
        if (resolved.size() == 1 && resolved.get(0) == -1L) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("rows", Collections.emptyList());
            empty.put("total", 0L);
            return AjaxResult.success(empty);
        }

        String deptFilter = inFilter("m.dept_id", resolved);
        String segmentFilter = buildSegmentFilter(normalizeSegmentType(query.getSegmentType()));
        String where = "m.del_flag = '0' AND " + deptFilter + " AND " + segmentFilter;

        List<Object> timeArgs = new ArrayList<>();
        if (query.getBeginTime() != null && !query.getBeginTime().trim().isEmpty()) {
            where += " AND m.create_time >= ?";
            timeArgs.add(query.getBeginTime());
        }
        if (query.getEndTime() != null && !query.getEndTime().trim().isEmpty()) {
            where += " AND m.create_time < DATE_ADD(?, INTERVAL 1 DAY)";
            timeArgs.add(query.getEndTime());
        }

        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        long total = queryCount("SELECT COUNT(*) FROM mem_member m LEFT JOIN ("
                + "SELECT member_id, SUM(consume_amount) AS total_sales, COUNT(*) AS order_count, MAX(create_time) AS last_order_time "
                + "FROM mem_points_record WHERE del_flag='0' GROUP BY member_id) agg ON m.member_id = agg.member_id "
                + "LEFT JOIN (SELECT r1.member_id, r1.balance FROM mem_points_record r1 "
                + "WHERE r1.del_flag='0' AND r1.record_id = (SELECT MAX(record_id) FROM mem_points_record r2 "
                + "WHERE r2.member_id=r1.member_id AND r2.del_flag='0')) pr ON m.member_id = pr.member_id "
                + "WHERE " + where, concatArgs(resolved, timeArgs));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT m.member_id, m.member_no, m.member_name, m.phone, m.dept_id, d.dept_name, "
                        + "COALESCE(agg.total_sales, 0) AS total_sales_amount, "
                        + "COALESCE(agg.order_count, 0) AS order_count, "
                        + "agg.last_order_time, COALESCE(pr.balance, 0) AS available_points "
                        + "FROM mem_member m "
                        + "LEFT JOIN sys_dept d ON m.dept_id = d.dept_id "
                        + "LEFT JOIN (SELECT member_id, SUM(consume_amount) AS total_sales, COUNT(*) AS order_count, MAX(create_time) AS last_order_time "
                        + "FROM mem_points_record WHERE del_flag='0' GROUP BY member_id) agg ON m.member_id = agg.member_id "
                        + "LEFT JOIN (SELECT r1.member_id, r1.balance FROM mem_points_record r1 "
                        + "WHERE r1.del_flag='0' AND r1.record_id = (SELECT MAX(record_id) FROM mem_points_record r2 "
                        + "WHERE r2.member_id=r1.member_id AND r2.del_flag='0')) pr ON m.member_id = pr.member_id "
                        + "WHERE " + where + " ORDER BY m.member_no DESC LIMIT ? OFFSET ?",
                concatArgs(concatArgs(resolved, timeArgs), Arrays.asList((long) pageSize, (long) offset)));

        String segmentType = normalizeSegmentType(query.getSegmentType());
        String action = suggestedAction(segmentType);
        List<MemberSegmentMemberVO> voList = rows.stream().map(row -> {
            MemberSegmentMemberVO vo = new MemberSegmentMemberVO();
            vo.setMemberId(toLong(row.get("member_id")));
            vo.setMemberNo((String) row.get("member_no"));
            vo.setMemberName((String) row.get("member_name"));
            vo.setMaskedPhone(maskPhone((String) row.get("phone")));
            vo.setDeptId(toLong(row.get("dept_id")));
            vo.setDeptName((String) row.get("dept_name"));
            vo.setTotalSalesAmount(toBigDecimal(row.get("total_sales_amount")));
            vo.setOrderCount(toInt(row.get("order_count")));
            vo.setLastOrderTime((java.util.Date) row.get("last_order_time"));
            vo.setAvailablePoints(toLong(row.get("available_points")));
            vo.setSuggestedAction(action);
            return vo;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("rows", voList);
        data.put("total", total);
        return AjaxResult.success(data);
    }

    // ==================== testable pure methods ====================

    String normalizeSegmentType(String segmentType) {
        if (segmentType == null) {
            return "";
        }
        String upper = segmentType.trim().toUpperCase();
        switch (upper) {
            case "NEW":
            case "ACTIVE":
            case "SILENT":
            case "HIGH_VALUE":
            case "LOW_POINTS":
            case "HIGH_POINTS":
                return upper;
            default:
                return "";
        }
    }

    String buildSegmentFilter(String segmentType) {
        if (segmentType == null || segmentType.isEmpty()) {
            return "1=1";
        }
        switch (segmentType) {
            case "NEW":
                return "m.create_time > NOW() - INTERVAL 30 DAY";
            case "ACTIVE":
                return "agg.order_count > 0 AND agg.last_order_time > NOW() - INTERVAL 30 DAY";
            case "SILENT":
                return "(agg.last_order_time IS NULL OR agg.last_order_time < NOW() - INTERVAL 30 DAY)";
            case "HIGH_VALUE":
                return "agg.total_sales >= 1000";
            case "LOW_POINTS":
                return "COALESCE(pr.balance,0) < 100";
            case "HIGH_POINTS":
                return "COALESCE(pr.balance,0) > 1000";
            default:
                return "1=1";
        }
    }

    String suggestedAction(String segmentType) {
        if (segmentType == null) return "";
        switch (segmentType) {
            case "NEW": return "引导首购，发放新人权益";
            case "ACTIVE": return "维护复购，推送活动";
            case "SILENT": return "回访唤醒，定向权益";
            case "HIGH_VALUE": return "专属维护，提升体验";
            case "LOW_POINTS": return "引导积分获取";
            case "HIGH_POINTS": return "推动积分兑换";
            default: return "";
        }
    }

    /**
     * 手机号脱敏：11 位手机号保留前 3 后 4，中间用 **** 替换；其余情况返回空串。
     */
    String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }
        String p = phone.trim();
        if (p.length() == 11) {
            return p.substring(0, 3) + "****" + p.substring(7);
        }
        if (p.length() > 7) {
            return p.substring(0, 3) + "****" + p.substring(p.length() - 4);
        }
        return "";
    }

    // ==================== dept permission helpers (same口径 as MemDashboardController) ====================

    List<Long> loadAllowedDeptIds() {
        if (SecurityUtils.isAdmin()) {
            return Collections.emptyList();
        }
        try {
            R<List<SysDept>> result = remoteUserService.getUserDeptList(
                    SecurityUtils.getUsername(), SecurityConstants.INNER);
            if (R.isSuccess(result) && result.getData() != null && !result.getData().isEmpty()) {
                return result.getData().stream().map(SysDept::getDeptId).collect(Collectors.toList());
            }
        } catch (Exception ignored) {
            // remote call failed, fall through
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId != null) {
            return Collections.singletonList(deptId);
        }
        return Collections.singletonList(-1L);
    }

    List<Long> resolveDeptIds(List<Long> requestedDeptIds) {
        List<Long> allowedDeptIds = loadAllowedDeptIds();
        if (allowedDeptIds.isEmpty()) {
            if (requestedDeptIds != null && !requestedDeptIds.isEmpty()) {
                return requestedDeptIds;
            }
            return Collections.emptyList();
        }
        if (requestedDeptIds == null || requestedDeptIds.isEmpty()) {
            return allowedDeptIds;
        }
        Set<Long> allowed = new HashSet<>(allowedDeptIds);
        List<Long> intersection = requestedDeptIds.stream()
                .filter(allowed::contains).collect(Collectors.toList());
        if (intersection.isEmpty()) {
            return Collections.singletonList(-1L);
        }
        return intersection;
    }

    String inFilter(String column, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "1=1";
        }
        return column + " IN (" + inPlaceholders(ids.size()) + ")";
    }

    private String inPlaceholders(int count) {
        if (count <= 0) return "NULL";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    // ==================== query helpers ====================

    private long queryCount(String sql, List<Object> args) {
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class, args.toArray());
            return count != null ? count : 0L;
        } catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }

    private List<Object> concatArgs(List<?> deptIds, List<Object> extra) {
        List<Object> args = new ArrayList<>(deptIds);
        args.addAll(extra);
        return args;
    }

    private Long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.valueOf(v.toString()); } catch (Exception e) { return 0L; }
    }

    private Integer toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.valueOf(v.toString()); } catch (Exception e) { return 0; }
    }

    private java.math.BigDecimal toBigDecimal(Object v) {
        if (v == null) return java.math.BigDecimal.ZERO;
        if (v instanceof java.math.BigDecimal) return (java.math.BigDecimal) v;
        try { return new java.math.BigDecimal(v.toString()); } catch (Exception e) { return java.math.BigDecimal.ZERO; }
    }
}
