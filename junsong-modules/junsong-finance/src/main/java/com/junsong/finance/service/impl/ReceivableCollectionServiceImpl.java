package com.junsong.finance.service.impl;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinanceReceivableCollection;
import com.junsong.finance.domain.FinanceReceivableCollectionLog;
import com.junsong.finance.domain.vo.ReceivableCollectionDashboardVO;
import com.junsong.finance.domain.vo.ReceivableCollectionRowVO;
import com.junsong.finance.domain.vo.ReceivableCollectionSummaryVO;
import com.junsong.finance.domain.vo.ReceivableCollectionSyncParams;
import com.junsong.finance.domain.vo.ReceivableCollectionUpdateParams;
import com.junsong.finance.mapper.FinanceReceivableCollectionMapper;
import com.junsong.finance.service.IReceivableCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ReceivableCollectionServiceImpl implements IReceivableCollectionService {

    private static final String PENDING = "PENDING";
    private static final String PROMISED = "PROMISED";
    private static final String PAID = "PAID";
    private static final String AGE_0_7 = "AGE_0_7";
    private static final String AGE_8_14 = "AGE_8_14";
    private static final String AGE_15_30 = "AGE_15_30";
    private static final String AGE_30_PLUS = "AGE_30_PLUS";

    @Autowired
    private FinanceReceivableCollectionMapper receivableCollectionMapper;

    @Override
    public ReceivableCollectionDashboardVO getDashboard(ReceivableCollectionSyncParams params) {
        List<ReceivableCollectionRowVO> rows = receivableCollectionMapper.selectDashboardRows(params);
        ReceivableCollectionDashboardVO dashboard = new ReceivableCollectionDashboardVO();
        ReceivableCollectionSummaryVO summary = new ReceivableCollectionSummaryVO();
        Date now = new Date();
        Date dayStart = dayStart(now);
        Date nextDayStart = nextDayStart(now);
        List<ReceivableCollectionRowVO> todayFollowUps = new ArrayList<>();
        List<ReceivableCollectionRowVO> overduePromises = new ArrayList<>();
        List<ReceivableCollectionRowVO> highRiskReceivables = new ArrayList<>();

        for (ReceivableCollectionRowVO row : rows) {
            summary.setTotalUnpaidAmount(summary.getTotalUnpaidAmount().add(nullToZero(row.getUnpaidAmount())));
            if (PENDING.equals(row.getCollectionStatus())) {
                summary.setPendingCount(summary.getPendingCount() + 1);
            }
            if (PROMISED.equals(row.getCollectionStatus())) {
                summary.setPromisedCount(summary.getPromisedCount() + 1);
                summary.setPromisedAmount(summary.getPromisedAmount().add(nullToZero(row.getPromisedAmount())));
            }
            if (PAID.equals(row.getCollectionStatus())) {
                summary.setPaidCount(summary.getPaidCount() + 1);
                summary.setRecoveredAmount(summary.getRecoveredAmount().add(nullToZero(row.getPaidAmount())));
            }
            if (AGE_0_7.equals(row.getAgeBucket())) {
                summary.setAge0To7Count(summary.getAge0To7Count() + 1);
            } else if (AGE_8_14.equals(row.getAgeBucket())) {
                summary.setAge8To14Count(summary.getAge8To14Count() + 1);
            } else if (AGE_15_30.equals(row.getAgeBucket())) {
                summary.setAge15To30Count(summary.getAge15To30Count() + 1);
            } else if (AGE_30_PLUS.equals(row.getAgeBucket())) {
                summary.setAge30PlusCount(summary.getAge30PlusCount() + 1);
                highRiskReceivables.add(row);
            }
            if (row.getNextFollowTime() != null
                    && !row.getNextFollowTime().before(dayStart)
                    && row.getNextFollowTime().before(nextDayStart)) {
                todayFollowUps.add(row);
            }
            if (PROMISED.equals(row.getCollectionStatus())
                    && row.getPromisedPayDate() != null
                    && row.getPromisedPayDate().before(dayStart)) {
                summary.setOverduePromiseCount(summary.getOverduePromiseCount() + 1);
                overduePromises.add(row);
            }
        }

        dashboard.setSummary(summary);
        dashboard.setTodayFollowUps(limit(todayFollowUps, 8));
        dashboard.setOverduePromises(limit(overduePromises, 8));
        dashboard.setHighRiskReceivables(limit(highRiskReceivables, 8));
        return dashboard;
    }

    @Override
    public List<ReceivableCollectionRowVO> list(ReceivableCollectionSyncParams params) {
        return receivableCollectionMapper.selectList(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncFromReceivables(ReceivableCollectionSyncParams params) {
        List<ReceivableCollectionRowVO> rows = receivableCollectionMapper.selectUnpaidSalesForSync(params);
        int count = 0;
        for (ReceivableCollectionRowVO row : rows) {
            if (row.getSaleId() == null || receivableCollectionMapper.selectBySaleId(row.getSaleId()) != null) {
                continue;
            }
            FinanceReceivableCollection collection = new FinanceReceivableCollection();
            collection.setSaleId(row.getSaleId());
            collection.setSaleNo(row.getSaleNo());
            collection.setDeptId(row.getDeptId());
            collection.setMemberId(row.getMemberId());
            collection.setCustomerName(row.getCustomerName());
            collection.setSaleAmount(row.getSaleAmount());
            collection.setPaidAmount(row.getPaidAmount());
            collection.setUnpaidAmount(row.getUnpaidAmount());
            collection.setAgeDays(row.getAgeDays());
            collection.setAgeBucket(resolveAgeBucket(row.getAgeDays()));
            collection.setPriorityLevel(resolvePriority(row.getAgeDays(), row.getUnpaidAmount()));
            collection.setCollectionStatus(PENDING);
            collection.setFollowCount(0);
            collection.setDelFlag("0");
            collection.setCreateBy(currentUsername());
            collection.setCreateTime(new Date());
            count += receivableCollectionMapper.insertCollection(collection);
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateFollow(Long collectionId, ReceivableCollectionUpdateParams params) {
        FinanceReceivableCollection existing = receivableCollectionMapper.selectById(collectionId);
        if (existing == null) {
            throw new ServiceException("催收记录不存在");
        }
        validateFollow(existing, params);

        FinanceReceivableCollection update = new FinanceReceivableCollection();
        update.setCollectionId(collectionId);
        update.setCollectionStatus(params.getCollectionStatus());
        update.setPromisedPayDate(params.getPromisedPayDate());
        update.setPromisedAmount(params.getPromisedAmount());
        update.setNextFollowTime(params.getNextFollowTime());
        update.setLastFollowTime(new Date());
        update.setFollowCount((existing.getFollowCount() == null ? 0 : existing.getFollowCount()) + 1);
        update.setUpdateBy(currentUsername());
        update.setUpdateTime(new Date());
        int rows = receivableCollectionMapper.updateCollection(update);

        FinanceReceivableCollectionLog log = new FinanceReceivableCollectionLog();
        log.setCollectionId(collectionId);
        log.setOldStatus(existing.getCollectionStatus());
        log.setNewStatus(params.getCollectionStatus());
        log.setFollowNote(params.getFollowNote());
        log.setPromisedPayDate(params.getPromisedPayDate());
        log.setPromisedAmount(params.getPromisedAmount());
        log.setNextFollowTime(params.getNextFollowTime());
        log.setOperatorId(currentUserId());
        log.setOperatorName(currentUsername());
        log.setCreateTime(new Date());
        receivableCollectionMapper.insertLog(log);
        receivableCollectionMapper.refreshCollectionAmounts(collectionId);
        return rows;
    }

    @Override
    public boolean canAccess(Long collectionId, Long deptId) {
        FinanceReceivableCollection collection = receivableCollectionMapper.selectById(collectionId);
        return collection != null && (SecurityUtils.isAdmin()
                || (deptId != null && deptId.equals(collection.getDeptId())));
    }

    private void validateFollow(FinanceReceivableCollection existing, ReceivableCollectionUpdateParams params) {
        if (params == null || isBlank(params.getCollectionStatus())) {
            throw new ServiceException("催收状态不能为空");
        }
        if (!params.getCollectionStatus().equals(existing.getCollectionStatus()) && isBlank(params.getFollowNote())) {
            throw new ServiceException("状态变更必须填写跟进备注");
        }
        if (PROMISED.equals(params.getCollectionStatus())) {
            if (params.getPromisedPayDate() == null) {
                throw new ServiceException("承诺回款日期不能为空");
            }
            if (params.getPromisedAmount() == null || params.getPromisedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("承诺回款金额必须大于0");
            }
            if (params.getPromisedAmount().compareTo(nullToZero(existing.getUnpaidAmount())) > 0) {
                throw new ServiceException("承诺回款金额不能大于未缴金额");
            }
            if (params.getNextFollowTime() == null) {
                throw new ServiceException("承诺付款必须设置下次跟进时间");
            }
        }
        if (PAID.equals(params.getCollectionStatus()) && nullToZero(existing.getUnpaidAmount()).compareTo(BigDecimal.ZERO) > 0) {
            throw new ServiceException("未缴清记录不能手动标记为已回款");
        }
    }

    private String resolveAgeBucket(Integer ageDays) {
        int days = ageDays == null ? 0 : ageDays;
        if (days <= 7) {
            return AGE_0_7;
        }
        if (days <= 14) {
            return AGE_8_14;
        }
        if (days <= 30) {
            return AGE_15_30;
        }
        return AGE_30_PLUS;
    }

    private String resolvePriority(Integer ageDays, BigDecimal unpaidAmount) {
        int days = ageDays == null ? 0 : ageDays;
        BigDecimal unpaid = nullToZero(unpaidAmount);
        if (days > 30 || unpaid.compareTo(new BigDecimal("5000")) >= 0) {
            return "CRITICAL";
        }
        if (days > 14 || unpaid.compareTo(new BigDecimal("2000")) >= 0) {
            return "HIGH";
        }
        if (days > 7 || unpaid.compareTo(new BigDecimal("500")) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Date dayStart(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date nextDayStart(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dayStart(date));
        calendar.add(Calendar.DATE, 1);
        return calendar.getTime();
    }

    private List<ReceivableCollectionRowVO> limit(List<ReceivableCollectionRowVO> rows, int max) {
        return rows.size() <= max ? rows : rows.subList(0, max);
    }

    private Long currentUserId() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String currentUsername() {
        try {
            return SecurityUtils.getUsername();
        } catch (Exception ignored) {
            return "";
        }
    }
}
