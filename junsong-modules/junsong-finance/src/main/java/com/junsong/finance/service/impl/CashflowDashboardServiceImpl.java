package com.junsong.finance.service.impl;

import com.junsong.finance.domain.vo.CashflowDashboardVO;
import com.junsong.finance.mapper.CashflowDashboardMapper;
import com.junsong.finance.service.ICashflowDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 轻量现金流看板 Service 实现.
 * R7-D: 小店面可用，不做预算，只做现金流入/流出/净额/待结算。
 *
 * @author junsong
 */
@Service
public class CashflowDashboardServiceImpl implements ICashflowDashboardService {

    private static final Logger log = LoggerFactory.getLogger(CashflowDashboardServiceImpl.class);

    @Autowired
    private CashflowDashboardMapper cashflowDashboardMapper;

    @Override
    public CashflowDashboardVO getCashflowDashboard(List<Long> deptIds, Date startTime, Date endTime) {
        // 默认时间范围：当月（月初到当前）
        if (startTime == null) {
            startTime = getMonthStart();
        }
        if (endTime == null) {
            endTime = new Date();
        }

        CashflowDashboardVO vo = new CashflowDashboardVO();

        // 先查表是否存在，不存在返回零值 VO（不报错）
        if (!tablesExist()) {
            log.warn("现金流看板所需表不存在，返回零值 VO");
            vo.setTrendRows(new ArrayList<>());
            vo.setPendingItems(new ArrayList<>());
            return vo;
        }

        // 查询汇总
        try {
            CashflowDashboardVO summary = cashflowDashboardMapper.selectCashflowSummary(deptIds, startTime, endTime);
            if (summary != null) {
                vo.setCashInAmount(nullToZero(summary.getCashInAmount()));
                vo.setCashOutAmount(nullToZero(summary.getCashOutAmount()));
                vo.setPendingExpenseAmount(nullToZero(summary.getPendingExpenseAmount()));
                vo.setPendingAdvanceAmount(nullToZero(summary.getPendingAdvanceAmount()));
                vo.setPendingProfitShareAmount(nullToZero(summary.getPendingProfitShareAmount()));
                vo.setPendingExpenseCount(summary.getPendingExpenseCount() != null ? summary.getPendingExpenseCount() : 0);
                vo.setPendingAdvanceCount(summary.getPendingAdvanceCount() != null ? summary.getPendingAdvanceCount() : 0);
                vo.setPendingProfitShareCount(summary.getPendingProfitShareCount() != null ? summary.getPendingProfitShareCount() : 0);
            }
        } catch (Exception e) {
            log.warn("查询现金流汇总失败，使用零值: {}", e.getMessage());
        }

        // netCashflowAmount = cashInAmount - cashOutAmount
        vo.setNetCashflowAmount(vo.getCashInAmount().subtract(vo.getCashOutAmount()));

        // 查询日趋势
        try {
            List<CashflowDashboardVO.CashflowTrendRowVO> trendRows =
                    cashflowDashboardMapper.selectCashflowTrendRows(deptIds, startTime, endTime);
            vo.setTrendRows(trendRows != null ? trendRows : new ArrayList<>());
        } catch (Exception e) {
            log.warn("查询现金流趋势失败: {}", e.getMessage());
            vo.setTrendRows(new ArrayList<>());
        }

        // 查询待结算明细
        try {
            List<CashflowDashboardVO.CashflowPendingItemVO> pendingItems =
                    cashflowDashboardMapper.selectPendingItems(deptIds);
            vo.setPendingItems(pendingItems != null ? pendingItems : new ArrayList<>());
        } catch (Exception e) {
            log.warn("查询待结算明细失败: {}", e.getMessage());
            vo.setPendingItems(new ArrayList<>());
        }

        return vo;
    }

    // ── 内部工具方法 ──

    /**
     * 检查现金流看板所需的核心表是否存在
     */
    private boolean tablesExist() {
        String[] requiredTables = {"fin_sale_payment", "fin_expense", "fin_advance",
                "fin_investor_payment", "fin_profit_share_record"};
        for (String table : requiredTables) {
            try {
                if (cashflowDashboardMapper.checkTableExists(table) == 0) {
                    return false;
                }
            } catch (Exception e) {
                log.warn("检查表 {} 是否存在时出错: {}", table, e.getMessage());
                return false;
            }
        }
        return true;
    }

    /**
     * 获取当月月初时间
     */
    private Date getMonthStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * null 转 BigDecimal.ZERO
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
