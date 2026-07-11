package com.junsong.finance.service.impl;

import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinanceCashflowForecastSnapshot;
import com.junsong.finance.domain.vo.CashflowForecastDashboardVO;
import com.junsong.finance.domain.vo.CashflowForecastDeviationVO;
import com.junsong.finance.domain.vo.CashflowForecastQueryParams;
import com.junsong.finance.domain.vo.CashflowForecastRhythmVO;
import com.junsong.finance.domain.vo.CashflowForecastWindowVO;
import com.junsong.finance.domain.vo.CashflowPressureVO;
import com.junsong.finance.mapper.CashflowForecastMapper;
import com.junsong.finance.service.ICashflowForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class CashflowForecastServiceImpl implements ICashflowForecastService {

    private static final String LOW = "LOW";
    private static final String MEDIUM = "MEDIUM";
    private static final String HIGH = "HIGH";
    private static final String CRITICAL = "CRITICAL";
    private static final String FORECAST_BASIS = "现金流预测来自R15承诺回款和近7天现金流";
    private static final List<Integer> WINDOWS = Arrays.asList(7, 14, 30);

    @Autowired
    private CashflowForecastMapper cashflowForecastMapper;

    @Override
    public CashflowForecastDashboardVO getDashboard(CashflowForecastQueryParams params) {
        CashflowForecastDashboardVO dashboard = new CashflowForecastDashboardVO();
        if (params == null) {
            params = new CashflowForecastQueryParams();
        }

        List<CashflowForecastWindowVO> windows = new ArrayList<>();
        for (Integer windowDays : WINDOWS) {
            CashflowForecastWindowVO window = new CashflowForecastWindowVO();
            window.setWindowDays(windowDays);
            BigDecimal promisedAmount = nullToZero(cashflowForecastMapper.sumPromisedAmount(params, windowDays));
            BigDecimal actualReceivableAmount = nullToZero(cashflowForecastMapper.sumActualPaymentAmount(params, windowDays));
            window.setPromisedAmount(promisedAmount);
            window.setForecastReceivableAmount(promisedAmount);
            window.setActualReceivableAmount(actualReceivableAmount);
            BigDecimal deviationAmount = actualReceivableAmount.subtract(promisedAmount);
            window.setDeviationAmount(deviationAmount);
            window.setDeviationRate(calcDeviationRate(deviationAmount, promisedAmount));
            window.setWindowLabel("未来" + windowDays + "天");
            windows.add(window);
        }
        dashboard.setWindows(windows);

        CashflowPressureVO pressure = buildPressure(params);
        dashboard.setPressure(pressure);

        List<CashflowForecastDeviationVO> forecastDeviation = cashflowForecastMapper.selectRecentDeviation(params);
        dashboard.setForecastDeviation(forecastDeviation == null ? new ArrayList<>() : forecastDeviation);

        dashboard.setWeeklyRhythm(buildWeeklyRhythm(params, pressure));
        return dashboard;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createSnapshot(CashflowForecastQueryParams params) {
        if (params == null) {
            params = new CashflowForecastQueryParams();
        }
        CashflowPressureVO pressure = buildPressure(params);
        Date today = truncateToDate(new Date());
        String username = currentUsername();
        int count = 0;
        for (Integer windowDays : WINDOWS) {
            BigDecimal promisedAmount = nullToZero(cashflowForecastMapper.sumPromisedAmount(params, windowDays));
            BigDecimal actualReceivableAmount = nullToZero(cashflowForecastMapper.sumActualPaymentAmount(params, windowDays));
            BigDecimal deviationAmount = actualReceivableAmount.subtract(promisedAmount);

            FinanceCashflowForecastSnapshot snapshot = new FinanceCashflowForecastSnapshot();
            snapshot.setDeptId(params.getDeptId());
            snapshot.setForecastDate(today);
            snapshot.setWindowDays(windowDays);
            snapshot.setForecastReceivableAmount(promisedAmount);
            snapshot.setActualReceivableAmount(actualReceivableAmount);
            snapshot.setDeviationAmount(deviationAmount);
            snapshot.setDeviationRate(calcDeviationRate(deviationAmount, promisedAmount));
            snapshot.setPressureScore(pressure.getPressureScore());
            snapshot.setPressureLevel(pressure.getPressureLevel());
            snapshot.setForecastBasis(FORECAST_BASIS);
            snapshot.setCreateBy(username);
            snapshot.setCreateTime(new Date());
            count += cashflowForecastMapper.insertSnapshot(snapshot);
        }
        return count;
    }

    private CashflowPressureVO buildPressure(CashflowForecastQueryParams params) {
        CashflowPressureVO pressure = new CashflowPressureVO();
        BigDecimal totalUnpaidAmount = nullToZero(cashflowForecastMapper.sumTotalUnpaidAmount(params));
        BigDecimal overduePromiseAmount = nullToZero(cashflowForecastMapper.sumOverduePromiseAmount(params));
        BigDecimal age30PlusAmount = nullToZero(cashflowForecastMapper.sumAge30PlusAmount(params));
        BigDecimal recentCashInAmount = nullToZero(cashflowForecastMapper.sumRecentCashInAmount(params));
        BigDecimal recentExpenseAmount = nullToZero(cashflowForecastMapper.sumRecentExpenseAmount(params));
        BigDecimal promised7Day = nullToZero(cashflowForecastMapper.sumPromisedAmount(params, 7));

        pressure.setTotalUnpaidAmount(totalUnpaidAmount);
        pressure.setOverduePromiseAmount(overduePromiseAmount);
        pressure.setAge30PlusAmount(age30PlusAmount);
        pressure.setRecentCashInAmount(recentCashInAmount);
        pressure.setRecentExpenseAmount(recentExpenseAmount);

        List<String> reasons = new ArrayList<>();

        int overdueRatioScore = calcOverdueRatioScore(overduePromiseAmount, totalUnpaidAmount);
        if (overdueRatioScore >= 30) {
            reasons.add("逾期承诺占比偏高");
        }

        int age30PlusScore = calcAge30PlusScore(age30PlusAmount, totalUnpaidAmount);
        if (age30PlusScore >= 25) {
            reasons.add("30天以上应收占比偏高");
        }

        int collectionPromiseRiskScore = calcCollectionPromiseRiskScore(promised7Day, totalUnpaidAmount);

        int netCashflowRiskScore = 0;
        if (recentCashInAmount.compareTo(recentExpenseAmount) < 0) {
            netCashflowRiskScore = 25;
            reasons.add("近7天净现金流为负");
        }

        int pressureScore = Math.min(100, overdueRatioScore + age30PlusScore + collectionPromiseRiskScore + netCashflowRiskScore);
        pressure.setPressureScore(pressureScore);
        pressure.setPressureLevel(resolvePressureLevel(pressureScore));
        pressure.setReasons(reasons);
        return pressure;
    }

    private int calcOverdueRatioScore(BigDecimal overduePromiseAmount, BigDecimal totalUnpaidAmount) {
        if (totalUnpaidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal ratio = overduePromiseAmount.divide(totalUnpaidAmount, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.20")) >= 0) {
            return 30;
        }
        return ratio.divide(new BigDecimal("0.20"), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("30"))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int calcAge30PlusScore(BigDecimal age30PlusAmount, BigDecimal totalUnpaidAmount) {
        if (totalUnpaidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal ratio = age30PlusAmount.divide(totalUnpaidAmount, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.30")) >= 0) {
            return 25;
        }
        return ratio.divide(new BigDecimal("0.30"), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("25"))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int calcCollectionPromiseRiskScore(BigDecimal promised7Day, BigDecimal totalUnpaidAmount) {
        if (totalUnpaidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 5;
        }
        BigDecimal threshold = totalUnpaidAmount.multiply(new BigDecimal("0.10"));
        if (promised7Day.compareTo(threshold) < 0) {
            return 20;
        }
        return 5;
    }

    private String resolvePressureLevel(int pressureScore) {
        if (pressureScore >= 80) {
            return CRITICAL;
        }
        if (pressureScore >= 60) {
            return HIGH;
        }
        if (pressureScore >= 30) {
            return MEDIUM;
        }
        return LOW;
    }

    private CashflowForecastRhythmVO buildWeeklyRhythm(CashflowForecastQueryParams params, CashflowPressureVO pressure) {
        CashflowForecastRhythmVO rhythm = new CashflowForecastRhythmVO();
        BigDecimal weeklyForecastAmount = nullToZero(cashflowForecastMapper.sumPromisedAmount(params, 7));
        BigDecimal weeklyOverduePromiseAmount = nullToZero(cashflowForecastMapper.sumOverduePromiseAmount(params));
        rhythm.setWeeklyForecastAmount(weeklyForecastAmount);
        rhythm.setWeeklyOverduePromiseAmount(weeklyOverduePromiseAmount);
        rhythm.setWeeklyPressureLevel(pressure.getPressureLevel());
        rhythm.setRecommendedAction(resolveRecommendedAction(pressure.getPressureLevel(), weeklyOverduePromiseAmount));
        return rhythm;
    }

    private String resolveRecommendedAction(String pressureLevel, BigDecimal weeklyOverduePromiseAmount) {
        if (CRITICAL.equals(pressureLevel)) {
            return "现金压力严重，优先催收逾期承诺并控制费用支出";
        }
        if (HIGH.equals(pressureLevel)) {
            return "现金压力偏高，加快本周承诺回款兑现";
        }
        if (weeklyOverduePromiseAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "关注逾期承诺回款，保持本周现金流节奏";
        }
        return "现金流稳健，按计划推进本周经营动作";
    }

    private BigDecimal calcDeviationRate(BigDecimal deviationAmount, BigDecimal forecastReceivableAmount) {
        if (forecastReceivableAmount == null || forecastReceivableAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return deviationAmount.divide(forecastReceivableAmount, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Date truncateToDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private String currentUsername() {
        try {
            return SecurityUtils.getUsername();
        } catch (Exception ignored) {
            return "";
        }
    }
}
