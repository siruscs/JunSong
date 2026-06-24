package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinInvestRecord;
import com.junsong.finance.domain.FinInvestor;
import com.junsong.finance.mapper.FinInvestRecordMapper;
import com.junsong.finance.mapper.FinInvestorMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinInvestRecordService;

@Service
public class FinInvestRecordServiceImpl implements IFinInvestRecordService
{
    @Autowired
    private FinInvestRecordMapper finInvestRecordMapper;
    @Autowired
    private FinInvestorMapper finInvestorMapper;
    @Autowired
    private IFinAccountingPeriodService finAccountingPeriodService;

    public FinInvestRecord selectFinInvestRecordByInvestId(Long investId) { return finInvestRecordMapper.selectFinInvestRecordByInvestId(investId); }
    public List<FinInvestRecord> selectFinInvestRecordList(FinInvestRecord finInvestRecord) { return finInvestRecordMapper.selectFinInvestRecordList(finInvestRecord); }
    public int insertFinInvestRecord(FinInvestRecord finInvestRecord) {
        normalizeInvestRecord(finInvestRecord);
        fillCurrentPeriod(finInvestRecord);
        finAccountingPeriodService.assertPeriodEditable(finInvestRecord.getPeriodId());
        return finInvestRecordMapper.insertFinInvestRecord(finInvestRecord);
    }
    public int updateFinInvestRecord(FinInvestRecord finInvestRecord) {
        assertInvestRecordEditable(finInvestRecord.getInvestId());
        finAccountingPeriodService.assertPeriodEditable(finInvestRecord.getPeriodId());
        normalizeInvestRecord(finInvestRecord);
        return finInvestRecordMapper.updateFinInvestRecord(finInvestRecord);
    }
    public int deleteFinInvestRecordByInvestIds(Long[] investIds) {
        if (investIds != null) {
            for (Long investId : investIds) {
                assertInvestRecordEditable(investId);
            }
        }
        return finInvestRecordMapper.deleteFinInvestRecordByInvestIds(investIds);
    }

    private void normalizeInvestRecord(FinInvestRecord finInvestRecord) {
        if (finInvestRecord.getInvestAmount() == null || finInvestRecord.getInvestAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("投资来源金额必须大于0");
        }
        FinInvestor investor = finInvestorMapper.selectFinInvestorByInvestorId(finInvestRecord.getInvestorId());
        if (investor == null || !"0".equals(investor.getDelFlag())) {
            throw new ServiceException("投资人不存在");
        }
        if (!"0".equals(investor.getStatus())) {
            throw new ServiceException("投资人已停用，不能登记投资来源");
        }
        if (finInvestRecord.getDeptId() != null && !finInvestRecord.getDeptId().equals(investor.getDeptId())) {
            throw new ServiceException("投资来源机构与投资人所属机构不一致");
        }
        finInvestRecord.setDeptId(investor.getDeptId());
        finInvestRecord.setInvestorName(investor.getInvestorName());
    }

    private void fillCurrentPeriod(FinInvestRecord finInvestRecord) {
        if (finInvestRecord.getPeriodId() == null && finInvestRecord.getDeptId() != null) {
            FinAccountingPeriod period = finAccountingPeriodService.initCurrentPeriod(finInvestRecord.getDeptId());
            finInvestRecord.setPeriodId(period.getPeriodId());
        }
    }

    private void assertInvestRecordEditable(Long investId) {
        if (investId == null) {
            return;
        }
        FinInvestRecord oldRecord = finInvestRecordMapper.selectFinInvestRecordByInvestId(investId);
        if (oldRecord != null) {
            finAccountingPeriodService.assertPeriodEditable(oldRecord.getPeriodId());
        }
    }
}
