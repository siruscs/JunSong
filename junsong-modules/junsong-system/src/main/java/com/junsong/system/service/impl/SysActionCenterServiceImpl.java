package com.junsong.system.service.impl;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.finance.api.RemoteFinanceActionCenterService;
import com.junsong.member.api.RemoteMemberActionCenterService;
import com.junsong.system.api.domain.ActionCenterSourceItem;
import com.junsong.system.domain.SysActionCenterTouchLog;
import com.junsong.system.domain.vo.ActionCenterCalendarVO;
import com.junsong.system.domain.vo.ActionCenterItemVO;
import com.junsong.system.domain.vo.ActionCenterQueryParams;
import com.junsong.system.domain.vo.OperationScheduleDashboardVO;
import com.junsong.system.domain.vo.OperationScheduleLogVO;
import com.junsong.system.mapper.SysActionCenterTouchLogMapper;
import com.junsong.system.service.ISysActionCenterService;
import com.junsong.system.service.ISysOperationScheduleLogService;
import org.springframework.stereotype.Service;

/**
 * R22 动作中心聚合服务。
 *
 * 动作来源（sourceType）：
 *   - FINANCE_RECEIVABLE：财务复盘/应收催收动作（finance 模块 via Feign）
 *   - MEMBER_GROWTH：会员增长动作（member 模块 via Feign）
 *   - STOCK_HEALTH：库存健康动作（预留来源，暂无独立数据源，由 SYSTEM_GOVERNANCE 兜底）
 *   - SYSTEM_GOVERNANCE：系统治理动作（R21 调度 FAILED/PARTIAL、数据源不可达风险）
 *
 * 动作状态（status）：PENDING / IN_PROGRESS / DONE / IGNORED / EFFECT_PENDING
 */
@Service
public class SysActionCenterServiceImpl implements ISysActionCenterService {

    private final RemoteFinanceActionCenterService financeActionCenterService;
    private final RemoteMemberActionCenterService memberActionCenterService;
    private final ISysOperationScheduleLogService scheduleLogService;
    private final SysActionCenterTouchLogMapper touchLogMapper;

    public SysActionCenterServiceImpl(RemoteFinanceActionCenterService financeActionCenterService,
                                      RemoteMemberActionCenterService memberActionCenterService,
                                      ISysOperationScheduleLogService scheduleLogService,
                                      SysActionCenterTouchLogMapper touchLogMapper) {
        this.financeActionCenterService = financeActionCenterService;
        this.memberActionCenterService = memberActionCenterService;
        this.scheduleLogService = scheduleLogService;
        this.touchLogMapper = touchLogMapper;
    }

    @Override
    public List<ActionCenterItemVO> listActions(ActionCenterQueryParams params) {
        List<ActionCenterItemVO> actions = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        aggregateFinance(actions, today);
        aggregateMember(actions, today);
        aggregateSystemGovernance(actions, today);

        enrichTouchInfo(actions);
        List<ActionCenterItemVO> filtered = filter(actions, params, today);
        sort(filtered, today);
        return filtered;
    }

    @Override
    public ActionCenterItemVO getAction(String actionId) {
        if (actionId == null || actionId.isEmpty()) {
            return null;
        }
        for (ActionCenterItemVO item : listActions(null)) {
            if (actionId.equals(item.getActionId())) {
                return item;
            }
        }
        return null;
    }

    @Override
    public List<ActionCenterCalendarVO> getCalendar(ActionCenterQueryParams params) {
        List<ActionCenterItemVO> actions = listActions(params);
        Map<String, ActionCenterCalendarVO> bucket = new TreeMap<>();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        for (ActionCenterItemVO item : actions) {
            String date = item.getDueDate() != null ? item.getDueDate() : today;
            ActionCenterCalendarVO vo = bucket.computeIfAbsent(date, d -> {
                ActionCenterCalendarVO c = new ActionCenterCalendarVO();
                c.setDate(d);
                return c;
            });
            String status = item.getStatus();
            if (status == null) {
                status = "PENDING";
            }
            switch (status) {
                case "PENDING":
                    vo.setPendingCount(vo.getPendingCount() + 1);
                    if (isOverdue(item.getDueDate(), today)) {
                        vo.setOverdueCount(vo.getOverdueCount() + 1);
                    }
                    break;
                case "IN_PROGRESS":
                    vo.setPendingCount(vo.getPendingCount() + 1);
                    if (isOverdue(item.getDueDate(), today)) {
                        vo.setOverdueCount(vo.getOverdueCount() + 1);
                    }
                    break;
                case "EFFECT_PENDING":
                    vo.setEffectPendingCount(vo.getEffectPendingCount() + 1);
                    break;
                case "DONE":
                case "IGNORED":
                    vo.setDoneCount(vo.getDoneCount() + 1);
                    break;
                default:
                    vo.setPendingCount(vo.getPendingCount() + 1);
                    break;
            }
        }
        return new ArrayList<>(bucket.values());
    }

    private void aggregateFinance(List<ActionCenterItemVO> actions, String today) {
        try {
            R<List<ActionCenterSourceItem>> ret = financeActionCenterService.listFinanceActions(SecurityConstants.INNER);
            if (ret != null && R.isSuccess(ret) && ret.getData() != null) {
                for (ActionCenterSourceItem src : ret.getData()) {
                    actions.add(convert(src, today));
                }
            }
        } catch (Exception e) {
            actions.add(riskAction("SYSTEM_GOVERNANCE:SOURCE_FINANCE_UNREACHABLE",
                    "SYSTEM_GOVERNANCE", "财务动作中心数据源不可达",
                    "FINANCE_RECEIVABLE 数据源调用失败：" + safeMessage(e), "HIGH", today));
        }
    }

    private void aggregateMember(List<ActionCenterItemVO> actions, String today) {
        try {
            R<List<ActionCenterSourceItem>> ret = memberActionCenterService.listMemberActions(SecurityConstants.INNER);
            if (ret != null && R.isSuccess(ret) && ret.getData() != null) {
                for (ActionCenterSourceItem src : ret.getData()) {
                    actions.add(convert(src, today));
                }
            }
        } catch (Exception e) {
            actions.add(riskAction("SYSTEM_GOVERNANCE:SOURCE_MEMBER_UNREACHABLE",
                    "SYSTEM_GOVERNANCE", "会员动作中心数据源不可达",
                    "MEMBER_GROWTH 数据源调用失败：" + safeMessage(e), "HIGH", today));
        }
    }

    private void aggregateSystemGovernance(List<ActionCenterItemVO> actions, String today) {
        try {
            OperationScheduleDashboardVO dashboard = scheduleLogService.getDashboard();
            if (dashboard != null && dashboard.getRecentFailures() != null) {
                for (OperationScheduleLogVO log : dashboard.getRecentFailures()) {
                    String severity = "PARTIAL".equalsIgnoreCase(log.getStatus()) ? "MEDIUM" : "HIGH";
                    actions.add(riskAction("SYSTEM_GOVERNANCE:SCHED_" + log.getLogId(),
                            "SYSTEM_GOVERNANCE",
                            (log.getJobName() != null ? log.getJobName() : "调度任务") + " 执行异常",
                            log.getErrorMessage() != null ? log.getErrorMessage() : log.getResultSummary(),
                            severity, today));
                }
            }
        } catch (Exception e) {
            actions.add(riskAction("SYSTEM_GOVERNANCE:SCHED_UNREACHABLE",
                    "SYSTEM_GOVERNANCE", "运维调度看板不可达",
                    "R21 调度看板聚合失败：" + safeMessage(e), "HIGH", today));
        }
    }

    private ActionCenterItemVO convert(ActionCenterSourceItem src, String today) {
        ActionCenterItemVO vo = new ActionCenterItemVO();
        vo.setActionId(src.getActionId());
        vo.setSourceType(src.getSourceType());
        vo.setSourceId(src.getSourceId());
        vo.setTitle(src.getTitle());
        vo.setDescription(src.getDescription());
        vo.setPriority(src.getPriority());
        vo.setStatus(src.getStatus());
        vo.setOwnerName(src.getOwnerName());
        vo.setOwnerId(src.getOwnerId());
        vo.setDeptId(src.getDeptId());
        vo.setDeptName(src.getDeptName());
        vo.setDueDate(src.getDueDate() != null ? src.getDueDate() : today);
        vo.setEffectStatus(src.getEffectStatus());
        vo.setDrilldownPath(src.getDrilldownPath());
        return vo;
    }

    private ActionCenterItemVO riskAction(String actionId, String sourceType, String title,
                                          String description, String priority, String today) {
        ActionCenterItemVO vo = new ActionCenterItemVO();
        vo.setActionId(actionId);
        vo.setSourceType(sourceType);
        vo.setSourceId(actionId);
        vo.setTitle(title);
        vo.setDescription(description);
        vo.setPriority(priority);
        vo.setStatus("PENDING");
        vo.setDueDate(today);
        vo.setDrilldownPath("/system/operation-scheduler");
        return vo;
    }

    private void enrichTouchInfo(List<ActionCenterItemVO> actions) {
        Date since24h = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24));
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (ActionCenterItemVO vo : actions) {
            try {
                SysActionCenterTouchLog latest = touchLogMapper.selectLatestByActionId(vo.getActionId());
                if (latest != null) {
                    vo.setLatestTouchStatus(latest.getTouchStatus());
                    vo.setLatestTouchTime(latest.getCreateTime() != null ? fmt.format(latest.getCreateTime()) : null);
                    if ("SUCCESS".equals(latest.getTouchStatus()) || "DRY_RUN".equals(latest.getTouchStatus())) {
                        if ("PENDING".equals(vo.getStatus()) || "IN_PROGRESS".equals(vo.getStatus())) {
                            vo.setStatus("EFFECT_PENDING");
                            vo.setEffectStatus("EFFECT_PENDING");
                        }
                    }
                }
                List<SysActionCenterTouchLog> recent = touchLogMapper.selectRecentByActionId(vo.getActionId(), 200);
                int count24h = 0;
                if (recent != null) {
                    for (SysActionCenterTouchLog log : recent) {
                        if (log.getCreateTime() != null && !log.getCreateTime().before(since24h)) {
                            count24h++;
                        }
                    }
                }
                vo.setTouchCount24h(count24h);
            } catch (Exception e) {
                vo.setTouchCount24h(0);
            }
            applyTouchable(vo);
        }
    }

    private void applyTouchable(ActionCenterItemVO vo) {
        String status = vo.getStatus();
        boolean touchable = "PENDING".equals(status) || "IN_PROGRESS".equals(status);
        vo.setTouchable(touchable);
        if (!touchable) {
            String reason;
            if ("EFFECT_PENDING".equals(status)) {
                reason = "动作已执行，待效果评估";
            } else if ("DONE".equals(status)) {
                reason = "动作已完成";
            } else if ("IGNORED".equals(status)) {
                reason = "动作已忽略";
            } else {
                reason = "当前状态不允许触达";
            }
            vo.setTouchDisabledReason(reason);
        }
    }

    private List<ActionCenterItemVO> filter(List<ActionCenterItemVO> actions, ActionCenterQueryParams params, String today) {
        if (params == null) {
            return new ArrayList<>(actions);
        }
        List<ActionCenterItemVO> result = new ArrayList<>();
        for (ActionCenterItemVO vo : actions) {
            if (params.getSourceType() != null && !params.getSourceType().isEmpty()
                    && !params.getSourceType().equals(vo.getSourceType())) {
                continue;
            }
            if (params.getStatus() != null && !params.getStatus().isEmpty()
                    && !params.getStatus().equals(vo.getStatus())) {
                continue;
            }
            if (params.getPriority() != null && !params.getPriority().isEmpty()
                    && !params.getPriority().equals(vo.getPriority())) {
                continue;
            }
            if (Boolean.TRUE.equals(params.getOnlyToday()) && !today.equals(vo.getDueDate())) {
                continue;
            }
            if (Boolean.TRUE.equals(params.getOnlyOverdue()) && !isOverdue(vo.getDueDate(), today)) {
                continue;
            }
            result.add(vo);
        }
        return result;
    }

    private void sort(List<ActionCenterItemVO> actions, String today) {
        actions.sort(Comparator
                .comparingInt((ActionCenterItemVO v) -> priorityWeight(v.getPriority()))
                .thenComparing(v -> !isOverdue(v.getDueDate(), today)));
    }

    private int priorityWeight(String priority) {
        if (priority == null) return 3;
        switch (priority.toUpperCase()) {
            case "HIGH": return 0;
            case "MEDIUM": return 1;
            case "LOW": return 2;
            default: return 3;
        }
    }

    private boolean isOverdue(String dueDate, String today) {
        return dueDate != null && dueDate.compareTo(today) < 0;
    }

    private String safeMessage(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
