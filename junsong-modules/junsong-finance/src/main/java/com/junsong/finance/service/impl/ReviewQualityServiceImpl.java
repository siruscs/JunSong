package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.vo.ReviewQualityDashboardVO;
import com.junsong.finance.domain.vo.ReviewQualityQueryParams;
import com.junsong.finance.mapper.ReviewQualityMapper;
import com.junsong.finance.service.IReviewQualityService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;

/**
 * 复盘质量服务实现。读取 sys_health_rule_config 配置阈值进行评分。
 */
@Service
public class ReviewQualityServiceImpl implements IReviewQualityService {

    private static final Logger log = LoggerFactory.getLogger(ReviewQualityServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<Long> SENTINEL = Collections.singletonList(-1L);

    @Autowired
    private ReviewQualityMapper mapper;

    @Autowired
    private HealthRuleConfigReader ruleReader;

    @Autowired
    private RemoteUserService remoteUserService;

    @Override
    public ReviewQualityDashboardVO getDashboard(ReviewQualityQueryParams params) {
        ReviewQualityDashboardVO vo = new ReviewQualityDashboardVO();

        List<Long> deptIds = resolveDeptIds(params);

        Date startDate = parseDate(params.getStartDate(), true);
        Date endDate = parseDate(params.getEndDate(), false);

        // 读取配置阈值
        BigDecimal overdueThreshold = ruleReader.getThreshold("FIN_REVIEW_OVERDUE_RATIO", BigDecimal.valueOf(20));
        BigDecimal firstResponseThreshold = ruleReader.getThreshold("FIN_REVIEW_FIRST_RESPONSE_HOURS", BigDecimal.valueOf(24));
        BigDecimal closeThreshold = ruleReader.getThreshold("FIN_REVIEW_CLOSE_HOURS", BigDecimal.valueOf(72));
        boolean overdueEnabled = ruleReader.isEnabled("FIN_REVIEW_OVERDUE_RATIO", true);
        boolean firstResponseEnabled = ruleReader.isEnabled("FIN_REVIEW_FIRST_RESPONSE_HOURS", true);
        boolean closeEnabled = ruleReader.isEnabled("FIN_REVIEW_CLOSE_HOURS", true);

        try {
            int total = safeCount(() -> mapper.countTasks(deptIds, startDate, endDate));
            int done = safeCount(() -> mapper.countDoneTasks(deptIds, startDate, endDate));
            int overdue = safeCount(() -> mapper.countOverdueTasks(deptIds, startDate, endDate));
            int noNote = safeCount(() -> mapper.countNoNoteDoneTasks(deptIds, startDate, endDate));
            double avgFirst = safeAvg(() -> mapper.avgFirstResponseHours(deptIds, startDate, endDate));
            double avgClose = safeAvg(() -> mapper.avgCloseHours(deptIds, startDate, endDate));

            vo.setTotalTaskCount(total);
            vo.setDoneTaskCount(done);
            vo.setOverdueTaskCount(overdue);
            vo.setNoNoteDoneCount(noNote);
            vo.setAvgFirstResponseHours(BigDecimal.valueOf(avgFirst).setScale(1, RoundingMode.HALF_UP));
            vo.setAvgCloseHours(BigDecimal.valueOf(avgClose).setScale(1, RoundingMode.HALF_UP));

            BigDecimal overdueRatio = total > 0
                ? BigDecimal.valueOf(overdue).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            vo.setOverdueRatio(overdueRatio);

            // 评分
            BigDecimal score = BigDecimal.valueOf(100);
            List<String> suggestions = new ArrayList<>();

            if (overdueEnabled && overdueRatio.compareTo(overdueThreshold) > 0) {
                score = score.subtract(BigDecimal.valueOf(25));
                suggestions.add(ruleReader.getSuggestion("FIN_REVIEW_OVERDUE_RATIO",
                    "逾期复盘比例过高，请减少未关闭任务堆积。"));
            }
            if (firstResponseEnabled && BigDecimal.valueOf(avgFirst).compareTo(firstResponseThreshold) > 0) {
                score = score.subtract(BigDecimal.valueOf(20));
                suggestions.add(ruleReader.getSuggestion("FIN_REVIEW_FIRST_RESPONSE_HOURS",
                    "复盘首次响应超时，请优先分派处理人。"));
            }
            if (closeEnabled && BigDecimal.valueOf(avgClose).compareTo(closeThreshold) > 0) {
                score = score.subtract(BigDecimal.valueOf(20));
                suggestions.add(ruleReader.getSuggestion("FIN_REVIEW_CLOSE_HOURS",
                    "复盘任务关闭超时，请确认阻塞原因。"));
            }
            if (noNote > 0) {
                score = score.subtract(BigDecimal.valueOf(10));
                suggestions.add("存在 " + noNote + " 个已完成复盘未填写处理备注。");
            }

            if (score.compareTo(BigDecimal.ZERO) < 0) {
                score = BigDecimal.ZERO;
            }
            vo.setQualityScore(score);
            vo.setSuggestions(suggestions);
        } catch (Exception e) {
            log.warn("复盘质量看板查询失败", e);
            vo.setSuggestions(Collections.singletonList("复盘质量数据暂不可用"));
        }

        return vo;
    }

    protected List<Long> resolveDeptIds(ReviewQualityQueryParams params) {
        if (SecurityUtils.isAdmin()) {
            if (params.getDeptId() != null) {
                return Collections.singletonList(params.getDeptId());
            }
            List<Long> requested = params.getDeptIds();
            return (requested != null && !requested.isEmpty()) ? requested : null;
        }

        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            return SENTINEL;
        }

        if (params.getDeptId() != null) {
            return allowed.contains(params.getDeptId())
                ? Collections.singletonList(params.getDeptId())
                : SENTINEL;
        }

        List<Long> requested = params.getDeptIds();
        if (requested == null || requested.isEmpty()) {
            return new ArrayList<>(allowed);
        }

        List<Long> intersection = requested.stream()
            .filter(allowed::contains)
            .collect(Collectors.toList());
        return intersection.isEmpty() ? SENTINEL : intersection;
    }

    private List<Long> loadAllowedDeptIds() {
        String username = SecurityUtils.getUsername();
        if (username == null || username.isEmpty()) return Collections.emptyList();
        try {
            R<List<SysDept>> response = remoteUserService.getUserDeptList(username, SecurityConstants.INNER);
            if (response == null || response.getData() == null) return Collections.emptyList();
            return response.getData().stream().map(SysDept::getDeptId).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("加载授权门店失败", e);
            return Collections.emptyList();
        }
    }

    private Date parseDate(String dateStr, boolean startOfDay) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        LocalDate ld = LocalDate.parse(dateStr, DATE_FMT);
        if (startOfDay) {
            return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        return Date.from(ld.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
    }

    private int safeCount(java.util.function.Supplier<Integer> supplier) {
        try {
            Integer val = supplier.get();
            return val != null ? val : 0;
        } catch (Exception e) {
            log.warn("count query failed", e);
            return 0;
        }
    }

    private double safeAvg(java.util.function.Supplier<Double> supplier) {
        try {
            Double val = supplier.get();
            return val != null ? val : 0.0;
        } catch (Exception e) {
            log.warn("avg query failed", e);
            return 0.0;
        }
    }
}
