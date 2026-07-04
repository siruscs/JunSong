package com.junsong.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import com.junsong.system.domain.vo.DataQualityDashboardVO;
import com.junsong.system.domain.vo.DataQualityIssueVO;
import com.junsong.system.mapper.SysDataQualityMapper;
import com.junsong.system.service.ISysDataQualityService;
import org.springframework.stereotype.Service;

/**
 * 数据质量服务实现。
 * R20: 汇总现有表问题，不写数据库，不做自动修复。
 * mapper 查询异常时记录 dbError 并将 status 设为 ERROR，不伪装为 HEALTHY。
 */
@Service
public class SysDataQualityServiceImpl implements ISysDataQualityService {

    private final SysDataQualityMapper mapper;

    /** 当前请求期间累积的数据库查询错误 */
    private final ThreadLocal<List<String>> currentErrors = ThreadLocal.withInitial(ArrayList::new);

    public SysDataQualityServiceImpl(SysDataQualityMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public DataQualityDashboardVO getDashboard() {
        currentErrors.get().clear();
        List<DataQualityIssueVO> issues = new ArrayList<>();

        addIssueIfPresent(issues, "FINANCE_SALE_WITHOUT_DEPT", "finance", "HIGH",
                countOrError("countFinanceSaleWithoutDept", mapper::countFinanceSaleWithoutDept),
                "fin_sale_record", "销售记录缺少门店/部门，影响门店授权与财务总览",
                "/finance/sale");

        addIssueIfPresent(issues, "FINANCE_PAYMENT_WITHOUT_SALE", "finance", "HIGH",
                countOrError("countFinancePaymentWithoutSale", mapper::countFinancePaymentWithoutSale),
                "fin_sale_payment", "缴款记录缺销售单，无法关联销售周期",
                "/finance/sale");

        addIssueIfPresent(issues, "FINANCE_RECEIVABLE_OVERDUE_WITHOUT_OWNER", "finance", "MEDIUM",
                countOrError("countFinanceReceivableOverdueWithoutOwner", mapper::countFinanceReceivableOverdueWithoutOwner),
                "finance_receivable_collection", "逾期应收缺负责人/跟进人",
                "/finance/receivableCollection");

        addIssueIfPresent(issues, "MEMBER_WITHOUT_PHONE_AND_OPENID", "member", "MEDIUM",
                countOrError("countMemberWithoutPhoneAndOpenid", mapper::countMemberWithoutPhoneAndOpenid),
                "mem_member", "会员缺可识别联系方式（手机号为空）",
                "/member");

        addIssueIfPresent(issues, "MEMBER_GROWTH_ACTION_WITHOUT_EFFECT", "member", "LOW",
                countOrError("countMemberGrowthActionWithoutEffect", mapper::countMemberGrowthActionWithoutEffect),
                "mem_growth_action_member", "已执行增长动作缺效果统计（复购/签到/成长值均无变化）",
                "/member/growthAction");

        addIssueIfPresent(issues, "STOCK_NEGATIVE_POSITION", "stock", "HIGH",
                countOrError("countNegativeStockPosition", mapper::countNegativeStockPosition),
                "fin_stock_position", "库存当前结存为负",
                "/finance/report/stock");

        addIssueIfPresent(issues, "SYSTEM_MENU_COMPONENT_EMPTY", "system", "MEDIUM",
                countOrError("countSystemMenuComponentEmpty", mapper::countSystemMenuComponentEmpty),
                "sys_menu", "菜单组件为空，可能导致页面白屏",
                "/system/menu");

        List<String> errors = currentErrors.get();

        DataQualityDashboardVO dashboard = new DataQualityDashboardVO();
        dashboard.setIssues(issues);
        dashboard.setTotalIssueCount(issues.size());
        dashboard.setDbErrorCount(errors.size());
        dashboard.setDbErrors(new ArrayList<>(errors));

        int high = 0, medium = 0, low = 0;
        for (DataQualityIssueVO issue : issues) {
            switch (issue.getSeverity()) {
                case "HIGH": high++; break;
                case "MEDIUM": medium++; break;
                case "LOW": low++; break;
                default: break;
            }
        }
        dashboard.setHighIssueCount(high);
        dashboard.setMediumIssueCount(medium);
        dashboard.setLowIssueCount(low);

        // ERROR 优先级最高：任何 mapper 查询失败都不能返回 HEALTHY
        if (!errors.isEmpty()) {
            dashboard.setStatus("ERROR");
        } else if (high > 0) {
            dashboard.setStatus("BLOCKED");
        } else if (medium > 0 || low > 0) {
            dashboard.setStatus("WARN");
        } else {
            dashboard.setStatus("HEALTHY");
        }

        currentErrors.get().clear();
        return dashboard;
    }

    private void addIssueIfPresent(List<DataQualityIssueVO> issues, String issueType, String module,
                                    String severity, long count, String sourceTables,
                                    String reason, String drilldownPath) {
        if (count > 0) {
            DataQualityIssueVO issue = new DataQualityIssueVO();
            issue.setIssueType(issueType);
            issue.setModule(module);
            issue.setSeverity(severity);
            issue.setIssueCount(count);
            issue.setSourceTables(sourceTables);
            issue.setReason(reason);
            issue.setDrilldownPath(drilldownPath);
            issues.add(issue);
        }
    }

    /**
     * 执行 mapper 查询。成功返回计数值（null 视为 0）；
     * 失败时记录错误到 currentErrors 并返回 -1（不会被 addIssueIfPresent 当作有效问题）。
     */
    private long countOrError(String queryName, java.util.function.Supplier<Long> supplier) {
        try {
            Long count = supplier.get();
            return count != null ? count : 0L;
        } catch (Exception e) {
            currentErrors.get().add(queryName + ": " + e.getMessage());
            return -1L;
        }
    }
}
