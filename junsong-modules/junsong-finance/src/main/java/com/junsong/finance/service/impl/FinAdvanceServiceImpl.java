package com.junsong.finance.service.impl;

import com.junsong.common.core.exception.ServiceException;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinAdvance;
import com.junsong.finance.mapper.FinAdvanceMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinAdvanceService;
import com.junsong.finance.constant.VerifyStatus;
import com.junsong.finance.util.CodeGenerator;

@Service
public class FinAdvanceServiceImpl implements IFinAdvanceService
{
    @Autowired
    private FinAdvanceMapper finAdvanceMapper;

    @Autowired
    private IFinAccountingPeriodService finAccountingPeriodService;

    @Override
    public FinAdvance selectFinAdvanceByAdvanceId(Long advanceId)
    {
        return finAdvanceMapper.selectFinAdvanceByAdvanceId(advanceId);
    }

    @Override
    public List<FinAdvance> selectFinAdvanceList(FinAdvance finAdvance)
    {
        return finAdvanceMapper.selectFinAdvanceList(finAdvance);
    }

    @Transactional
    @Override
    public int insertFinAdvance(FinAdvance finAdvance)
    {
        if (StringUtils.isEmpty(finAdvance.getAdvanceNo()))
        {
            int todayCount = finAdvanceMapper.countTodayAdvances();
            finAdvance.setAdvanceNo(CodeGenerator.generateAdvanceNo(todayCount));
        }
        
        finAdvance.setDeptId(SecurityUtils.getDeptId());
        fillCurrentPeriod(finAdvance);
        finAccountingPeriodService.assertPeriodEditable(finAdvance.getPeriodId());
        
        finAdvance.setStatus(VerifyStatus.UNVERIFIED);
        
        return finAdvanceMapper.insertFinAdvance(finAdvance);
    }

    private void fillCurrentPeriod(FinAdvance finAdvance)
    {
        if (finAdvance.getPeriodId() == null && finAdvance.getDeptId() != null)
        {
            FinAccountingPeriod period = finAccountingPeriodService.initCurrentPeriod(finAdvance.getDeptId());
            finAdvance.setPeriodId(period.getPeriodId());
        }
    }

    @Transactional
    @Override
    public int updateFinAdvance(FinAdvance finAdvance)
    {
        assertAdvanceEditable(finAdvance.getAdvanceId());
        finAccountingPeriodService.assertPeriodEditable(finAdvance.getPeriodId());
        return finAdvanceMapper.updateFinAdvance(finAdvance);
    }

    @Transactional
    @Override
    public int deleteFinAdvanceByAdvanceIds(Long[] advanceIds)
    {
        if (advanceIds != null) {
            for (Long advanceId : advanceIds) {
                assertAdvanceEditable(advanceId);
            }
        }
        return finAdvanceMapper.deleteFinAdvanceByAdvanceIds(advanceIds);
    }

    @Transactional
    @Override
    public int deleteFinAdvanceByAdvanceId(Long advanceId)
    {
        assertAdvanceEditable(advanceId);
        return finAdvanceMapper.deleteFinAdvanceByAdvanceId(advanceId);
    }

    @Override
    public boolean checkAdvanceNoUnique(FinAdvance finAdvance)
    {
        Long advanceId = finAdvance.getAdvanceId() == null ? -1L : finAdvance.getAdvanceId();
        FinAdvance info = finAdvanceMapper.checkAdvanceNoUnique(finAdvance.getAdvanceNo());
        if (StringUtils.isNotNull(info) && info.getAdvanceId().longValue() != advanceId.longValue())
        {
            return false;
        }
        return true;
    }

    @Transactional
    @Override
    public int verifyAdvance(Long advanceId, String verifyBy)
    {
        FinAdvance advance = finAdvanceMapper.selectFinAdvanceByAdvanceId(advanceId);
        if (advance == null)
        {
            throw new ServiceException("借支记录不存在");
        }
        finAccountingPeriodService.assertPeriodEditable(advance.getPeriodId());
        if (VerifyStatus.VERIFIED.equals(advance.getStatus()))
        {
            throw new ServiceException("借支记录已核销，不可重复核销");
        }
        
        advance.setStatus(VerifyStatus.VERIFIED);
        advance.setVerifyBy(verifyBy);
        advance.setVerifyTime(new Date());
        
        return finAdvanceMapper.updateFinAdvance(advance);
    }

    private void assertAdvanceEditable(Long advanceId)
    {
        if (advanceId == null) {
            return;
        }
        FinAdvance oldAdvance = finAdvanceMapper.selectFinAdvanceByAdvanceId(advanceId);
        if (oldAdvance != null) {
            finAccountingPeriodService.assertPeriodEditable(oldAdvance.getPeriodId());
        }
    }
}
