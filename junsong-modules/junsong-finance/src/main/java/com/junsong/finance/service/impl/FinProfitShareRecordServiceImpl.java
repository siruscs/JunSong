package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinDeptProfitConfig;
import com.junsong.finance.domain.FinInvestRecord;
import com.junsong.finance.domain.FinInvestorPayment;
import com.junsong.finance.domain.FinProfitShareDetail;
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.mapper.FinDeptProfitConfigMapper;
import com.junsong.finance.mapper.FinInvestRecordMapper;
import com.junsong.finance.mapper.FinInvestorPaymentMapper;
import com.junsong.finance.mapper.FinProfitShareDetailMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.finance.service.IFinProfitShareRecordService;
import com.junsong.finance.constant.ShareStatus;
import com.junsong.finance.constant.PeriodStatus;

@Service
public class FinProfitShareRecordServiceImpl implements IFinProfitShareRecordService
{
    @Autowired
    private FinProfitShareRecordMapper finProfitShareRecordMapper;
    @Autowired
    private FinProfitShareDetailMapper finProfitShareDetailMapper;
    @Autowired
    private FinAccountingPeriodMapper finAccountingPeriodMapper;
    @Autowired
    private FinDeptProfitConfigMapper finDeptProfitConfigMapper;
    @Autowired
    private FinInvestRecordMapper finInvestRecordMapper;
    @Autowired
    private FinInvestorPaymentMapper finInvestorPaymentMapper;

    public FinProfitShareRecord selectFinProfitShareRecordByShareId(Long shareId) { return finProfitShareRecordMapper.selectFinProfitShareRecordByShareId(shareId); }

    @Transactional(rollbackFor = Exception.class)
    public FinProfitShareRecord carryForwardPeriod(Long periodId) {
        if (periodId == null) {
            throw new ServiceException("核算周期不能为空");
        }
        FinProfitShareRecord existingShare = finProfitShareRecordMapper.selectFinProfitShareRecordByPeriodId(periodId);
        if (existingShare != null) {
            return existingShare;
        }

        FinAccountingPeriod period = finAccountingPeriodMapper.selectFinAccountingPeriodByPeriodId(periodId);
        if (period == null || !"0".equals(period.getDelFlag())) {
            throw new ServiceException("核算周期不存在");
        }
        if (!"2".equals(period.getStatus())) {
            throw new ServiceException("只有已结转的周期才能执行分润");
        }
        period = refreshClosedPeriodStats(period);
        BigDecimal netProfit = nvl(period.getNetProfit());
        if (netProfit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("净利必须大于0才能结转分润");
        }

        FinDeptProfitConfig config = finDeptProfitConfigMapper.selectFinDeptProfitConfigByDeptId(period.getDeptId());
        if (config == null || !"0".equals(config.getStatus())) {
            throw new ServiceException("请先配置并启用店面默认店长分润比例");
        }
        BigDecimal managerRate = normalizeRate(config.getManagerProfitRate());
        BigDecimal managerAmount = netProfit.multiply(managerRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal investorAmount = netProfit.subtract(managerAmount).setScale(2, RoundingMode.HALF_UP);

        FinInvestRecord query = new FinInvestRecord();
        query.setDeptId(period.getDeptId());
        List<FinInvestRecord> investRecords = mergeInvestRecordsByInvestor(finInvestRecordMapper.selectFinInvestRecordList(query));
        BigDecimal totalInvest = investRecords.stream().map(record -> nvl(record.getInvestAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (investorAmount.compareTo(BigDecimal.ZERO) > 0 && totalInvest.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("没有可用于分润的投资记录");
        }

        Date now = new Date();
        FinProfitShareRecord share = new FinProfitShareRecord();
        share.setDeptId(period.getDeptId());
        share.setPeriodId(period.getPeriodId());
        share.setShareNo(buildShareNo(period.getDeptId(), now));
        share.setNetProfit(netProfit);
        share.setManagerProfitRate(managerRate);
        share.setManagerProfitAmount(managerAmount);
        share.setInvestorProfitAmount(investorAmount);
        share.setStatus(ShareStatus.CARRIED);
        share.setShareTime(now);
        share.setCreateBy(SecurityUtils.getUsername());
        share.setRemark("回本周期结转自动生成");
        finProfitShareRecordMapper.insertFinProfitShareRecord(share);

        insertManagerDetail(share, managerAmount);
        insertInvestorDetailsAndPayments(share, investRecords, totalInvest, investorAmount, now, "1".equals(config.getAutoCreateInvestorPayment()));

        // 周期状态已由结转操作设置为CARRIED，此处只更新分润相关字段
        period.setManagerProfitRate(managerRate);
        period.setManagerProfitAmount(managerAmount);
        period.setInvestorProfitAmount(investorAmount);
        period.setUpdateBy(SecurityUtils.getUsername());
        finAccountingPeriodMapper.updateFinAccountingPeriod(period);

        return finProfitShareRecordMapper.selectFinProfitShareRecordByShareId(share.getShareId());
    }

    public List<FinProfitShareRecord> selectFinProfitShareRecordList(FinProfitShareRecord finProfitShareRecord) { return finProfitShareRecordMapper.selectFinProfitShareRecordList(finProfitShareRecord); }
    public int insertFinProfitShareRecord(FinProfitShareRecord finProfitShareRecord) {
        int rows = finProfitShareRecordMapper.insertFinProfitShareRecord(finProfitShareRecord);
        if (finProfitShareRecord.getDetails() != null) {
            for (FinProfitShareDetail detail : finProfitShareRecord.getDetails()) {
                detail.setShareId(finProfitShareRecord.getShareId());
                detail.setDeptId(finProfitShareRecord.getDeptId());
                detail.setPeriodId(finProfitShareRecord.getPeriodId());
                finProfitShareDetailMapper.insertFinProfitShareDetail(detail);
            }
        }
        return rows;
    }
    public int updateFinProfitShareRecord(FinProfitShareRecord finProfitShareRecord) { return finProfitShareRecordMapper.updateFinProfitShareRecord(finProfitShareRecord); }
    @Transactional(rollbackFor = Exception.class)
    public int deleteFinProfitShareRecordByShareIds(Long[] shareIds) {
        if (shareIds == null || shareIds.length == 0) {
            return 0;
        }
        int rows = 0;
        String username = SecurityUtils.getUsername();
        for (Long shareId : shareIds) {
            FinProfitShareRecord share = finProfitShareRecordMapper.selectFinProfitShareRecordByShareId(shareId);
            if (share == null || !"0".equals(share.getDelFlag())) {
                continue;
            }
            if ("2".equals(share.getStatus())) {
                continue;
            }
            finInvestorPaymentMapper.deleteAutoInvestorPaymentByShareId(shareId);
            share.setStatus(ShareStatus.CANCELLED);
            share.setUpdateBy(username);
            share.setRemark(appendRemark(share.getRemark(), "分润记录已作废"));
            rows += finProfitShareRecordMapper.updateFinProfitShareRecord(share);
            finAccountingPeriodMapper.resetCarryForwardByPeriodId(share.getPeriodId(), username);
        }
        return rows;
    }

    private void insertManagerDetail(FinProfitShareRecord share, BigDecimal managerAmount) {
        if (managerAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        FinProfitShareDetail detail = new FinProfitShareDetail();
        detail.setShareId(share.getShareId());
        detail.setDeptId(share.getDeptId());
        detail.setPeriodId(share.getPeriodId());
        detail.setReceiverType("1");
        detail.setReceiverName("店长分润");
        detail.setInvestAmount(BigDecimal.ZERO);
        detail.setInvestRatio(share.getManagerProfitRate());
        detail.setShareAmount(managerAmount);
        finProfitShareDetailMapper.insertFinProfitShareDetail(detail);
    }

    private void insertInvestorDetailsAndPayments(FinProfitShareRecord share, List<FinInvestRecord> investRecords, BigDecimal totalInvest, BigDecimal investorAmount, Date paymentDate, boolean autoCreatePayment) {
        BigDecimal allocatedAmount = BigDecimal.ZERO;
        int effectiveCount = 0;
        for (FinInvestRecord record : investRecords) {
            if (nvl(record.getInvestAmount()).compareTo(BigDecimal.ZERO) > 0) {
                effectiveCount++;
            }
        }
        int index = 0;
        for (FinInvestRecord record : investRecords) {
            BigDecimal investAmount = nvl(record.getInvestAmount());
            if (investAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            index++;
            BigDecimal investRatio = investAmount.divide(totalInvest, 6, RoundingMode.HALF_UP);
            BigDecimal shareAmount = index == effectiveCount ? investorAmount.subtract(allocatedAmount).setScale(2, RoundingMode.HALF_UP)
                    : investorAmount.multiply(investRatio).setScale(2, RoundingMode.HALF_UP);
            allocatedAmount = allocatedAmount.add(shareAmount);

            FinProfitShareDetail detail = new FinProfitShareDetail();
            detail.setShareId(share.getShareId());
            detail.setDeptId(share.getDeptId());
            detail.setPeriodId(share.getPeriodId());
            detail.setReceiverType("2");
            detail.setReceiverId(record.getInvestorId());
            detail.setReceiverName(record.getInvestorName());
            detail.setInvestAmount(investAmount);
            detail.setInvestRatio(investRatio);
            detail.setShareAmount(shareAmount);
            finProfitShareDetailMapper.insertFinProfitShareDetail(detail);

            if (!autoCreatePayment) {
                continue;
            }
            FinInvestorPayment payment = new FinInvestorPayment();
            payment.setDeptId(share.getDeptId());
            payment.setPeriodId(share.getPeriodId());
            payment.setShareId(share.getShareId());
            payment.setShareDetailId(detail.getDetailId());
            payment.setInvestorId(record.getInvestorId());
            payment.setPaymentNo(buildPaymentNo(share.getDeptId(), paymentDate, index));
            payment.setPaymentDate(paymentDate);
            payment.setPaymentType("return");
            payment.setInvestorName(record.getInvestorName());
            payment.setAmount(shareAmount);
            payment.setSourceType("1");
            payment.setPaymentStatus("1");
            payment.setInvestRatio(investRatio);
            payment.setCreateBy(SecurityUtils.getUsername());
            payment.setRemark("结转分润自动返款");
            finInvestorPaymentMapper.insertFinInvestorPayment(payment);

            detail.setPaymentId(payment.getPaymentId());
            finProfitShareDetailMapper.updateFinProfitShareDetail(detail);
        }
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        BigDecimal value = nvl(rate);
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return value.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        }
        return value;
    }

    private String appendRemark(String oldRemark, String newRemark) {
        if (oldRemark == null || oldRemark.isEmpty()) {
            return newRemark;
        }
        return oldRemark + "；" + newRemark;
    }

    private FinAccountingPeriod refreshClosedPeriodStats(FinAccountingPeriod period) {
        Date endTime = period.getEndTime();
        if (endTime == null) {
            endTime = period.getBreakEvenTime() == null ? new Date() : period.getBreakEvenTime();
            period.setEndTime(endTime);
        }
        period.setTotalVerifiedExpense(nvl(finAccountingPeriodMapper.selectTotalVerifiedExpense(period.getPeriodId(), period.getDeptId(), period.getStartTime(), endTime)));
        period.setTotalPurchase(nvl(finAccountingPeriodMapper.selectTotalPurchase(period.getPeriodId(), period.getDeptId(), period.getStartTime(), endTime)));
        period.setTotalSalePayment(nvl(finAccountingPeriodMapper.selectTotalSalePayment(period.getPeriodId(), period.getDeptId(), period.getStartTime(), endTime)));
        period.setTotalUnverifiedAdvance(nvl(finAccountingPeriodMapper.selectTotalUnverifiedAdvance(period.getPeriodId(), period.getDeptId(), period.getStartTime(), endTime)));
        period.setNetProfit(nvl(period.getTotalSalePayment())
                .subtract(nvl(period.getTotalVerifiedExpense()))
                .subtract(nvl(period.getTotalPurchase()))
                .subtract(nvl(period.getTotalUnverifiedAdvance())));
        period.setUpdateBy(SecurityUtils.getUsername());
        finAccountingPeriodMapper.updateFinAccountingPeriod(period);
        return finAccountingPeriodMapper.selectFinAccountingPeriodByPeriodId(period.getPeriodId());
    }

    private List<FinInvestRecord> mergeInvestRecordsByInvestor(List<FinInvestRecord> records) {
        Map<Long, FinInvestRecord> merged = new LinkedHashMap<>();
        for (FinInvestRecord record : records) {
            if (record.getInvestorId() == null) {
                continue;
            }
            FinInvestRecord target = merged.get(record.getInvestorId());
            if (target == null) {
                target = new FinInvestRecord();
                target.setDeptId(record.getDeptId());
                target.setInvestorId(record.getInvestorId());
                target.setInvestorName(record.getInvestorName());
                target.setInvestAmount(BigDecimal.ZERO);
                merged.put(record.getInvestorId(), target);
            }
            target.setInvestAmount(nvl(target.getInvestAmount()).add(nvl(record.getInvestAmount())));
        }
        return List.copyOf(merged.values());
    }

    private String buildShareNo(Long deptId, Date date) {
        return "PS" + buildNoBody(deptId, date);
    }

    private String buildPaymentNo(Long deptId, Date date, int index) {
        return "IP" + buildNoBody(deptId, date) + String.format("%02d", index);
    }

    private String buildNoBody(Long deptId, Date date) {
        return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(date)
                + deptId
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
