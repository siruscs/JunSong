package com.junsong.member.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.vo.MemberPointsOperationSummaryVO;
import com.junsong.member.domain.vo.MemberPointsRiskRowVO;
import com.junsong.member.service.IMemberPointsOperationService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 积分经营摘要服务实现。
 *
 * 口径：
 * - 可用积分 = 每个会员最新一条 mem_points_record.balance 之和
 * - 积分负债估算 = 可用积分 / 100（100 积分 = 1 元）
 * - 兑换成本 = SUM(mem_points_goods.goods_value * mem_points_exchange.quantity)
 * - 高积分会员 = 可用积分 > 1000 的会员，按可用积分倒序
 */
@Service
public class MemberPointsOperationServiceImpl implements IMemberPointsOperationService {

    static final long HIGH_POINTS_THRESHOLD = 1000L;
    static final BigDecimal POINTS_TO_YUAN_RATE = new BigDecimal("100");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RemoteUserService remoteUserService;

    @Override
    public MemberPointsOperationSummaryVO getPointsOperationSummary(List<Long> requestedDeptIds) {
        List<Long> deptIds = resolveDeptIds(requestedDeptIds);

        MemberPointsOperationSummaryVO vo = new MemberPointsOperationSummaryVO();

        long totalAvailablePoints = queryTotalAvailablePoints(deptIds);
        vo.setTotalAvailablePoints(totalAvailablePoints);
        vo.setEstimatedPointsLiabilityAmount(computeLiability(totalAvailablePoints));

        vo.setRedeemedCostAmount(queryRedeemedCost(deptIds));

        List<MemberPointsRiskRowVO> highPointsMembers = queryHighPointsMembers(deptIds);
        vo.setHighPointsMemberCount((long) highPointsMembers.size());
        vo.setHighPointsMembers(highPointsMembers);

        return vo;
    }

    // ==================== pure-logic methods (testable) ====================

    /**
     * 积分负债估算：可用积分 / 100，保留 2 位小数。
     */
    BigDecimal computeLiability(long totalAvailablePoints) {
        if (totalAvailablePoints <= 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(totalAvailablePoints)
                .divide(POINTS_TO_YUAN_RATE, 2, RoundingMode.HALF_UP);
    }

    /**
     * 手机号脱敏：11 位保留前 3 后 4，中间 **** 替换；其余返回空串。
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

    // ==================== dept permission (same口径 as MemDashboardController) ====================

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

    // ==================== package-private query methods (overridable in tests) ====================

    long queryTotalAvailablePoints(List<Long> deptIds) {
        String deptFilter = inClause("m.dept_id", deptIds);
        String sql = "SELECT COALESCE(SUM(r.balance), 0) FROM mem_points_record r "
                + "INNER JOIN mem_member m ON r.member_id = m.member_id "
                + "INNER JOIN (SELECT member_id, MAX(record_id) AS max_id "
                + "FROM mem_points_record WHERE del_flag='0' GROUP BY member_id) latest "
                + "ON r.record_id = latest.max_id "
                + "WHERE r.del_flag='0' AND m.del_flag='0' AND " + deptFilter;
        try {
            Long val = jdbcTemplate.queryForObject(sql, Long.class);
            return val != null ? val : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    BigDecimal queryRedeemedCost(List<Long> deptIds) {
        String deptFilter = inClause("pe.dept_id", deptIds);
        String sql = "SELECT COALESCE(SUM(COALESCE(pg.goods_value,0) * COALESCE(pe.quantity,1)), 0) "
                + "FROM mem_points_exchange pe "
                + "LEFT JOIN mem_points_goods pg ON pg.goods_id = pe.goods_id AND pg.del_flag='0' "
                + "WHERE pe.del_flag='0' AND " + deptFilter;
        try {
            return jdbcTemplate.queryForObject(sql, BigDecimal.class);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    List<MemberPointsRiskRowVO> queryHighPointsMembers(List<Long> deptIds) {
        String deptFilter = inClause("m.dept_id", deptIds);
        String sql = "SELECT m.member_id, m.member_no, m.member_name, m.phone, m.dept_id, d.dept_name, "
                + "pr.balance AS available_points, agg.last_order_time "
                + "FROM mem_member m "
                + "LEFT JOIN sys_dept d ON m.dept_id = d.dept_id "
                + "INNER JOIN (SELECT r1.member_id, r1.balance FROM mem_points_record r1 "
                + "WHERE r1.del_flag='0' AND r1.record_id = (SELECT MAX(record_id) FROM mem_points_record r2 "
                + "WHERE r2.member_id=r1.member_id AND r2.del_flag='0')) pr ON m.member_id = pr.member_id "
                + "LEFT JOIN (SELECT member_id, MAX(create_time) AS last_order_time "
                + "FROM mem_points_record WHERE del_flag='0' GROUP BY member_id) agg ON m.member_id = agg.member_id "
                + "WHERE m.del_flag='0' AND " + deptFilter + " AND pr.balance > " + HIGH_POINTS_THRESHOLD + " "
                + "ORDER BY pr.balance DESC LIMIT 100";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            List<MemberPointsRiskRowVO> list = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                MemberPointsRiskRowVO vo = new MemberPointsRiskRowVO();
                vo.setMemberId(toLong(row.get("member_id")));
                vo.setMemberNo((String) row.get("member_no"));
                vo.setMemberName((String) row.get("member_name"));
                vo.setMaskedPhone(maskPhone((String) row.get("phone")));
                vo.setDeptId(toLong(row.get("dept_id")));
                vo.setDeptName((String) row.get("dept_name"));
                vo.setAvailablePoints(toLong(row.get("available_points")));
                vo.setEstimatedLiability(computeLiability(vo.getAvailablePoints()));
                vo.setLastOrderTime((Date) row.get("last_order_time"));
                list.add(vo);
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ==================== helpers ====================

    private String inClause(String column, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "1=1";
        }
        StringBuilder sb = new StringBuilder(column).append(" IN (");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        sb.append(')');
        return sb.toString();
    }

    private Long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.valueOf(v.toString()); } catch (Exception e) { return 0L; }
    }
}
