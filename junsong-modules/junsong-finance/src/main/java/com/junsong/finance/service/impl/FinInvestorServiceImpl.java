package com.junsong.finance.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinInvestRecord;
import com.junsong.finance.domain.FinInvestor;
import com.junsong.finance.domain.FinInvestorPayment;
import com.junsong.finance.mapper.FinInvestRecordMapper;
import com.junsong.finance.mapper.FinInvestorMapper;
import com.junsong.finance.mapper.FinInvestorPaymentMapper;
import com.junsong.finance.service.IFinInvestorService;

@Service
public class FinInvestorServiceImpl implements IFinInvestorService
{
    @Autowired
    private FinInvestorMapper finInvestorMapper;
    @Autowired
    private FinInvestRecordMapper finInvestRecordMapper;
    @Autowired
    private FinInvestorPaymentMapper finInvestorPaymentMapper;

    public FinInvestor selectFinInvestorByInvestorId(Long investorId) { return finInvestorMapper.selectFinInvestorByInvestorId(investorId); }
    public List<FinInvestor> selectFinInvestorList(FinInvestor finInvestor) { return finInvestorMapper.selectFinInvestorList(finInvestor); }
    public int insertFinInvestor(FinInvestor finInvestor) { return finInvestorMapper.insertFinInvestor(finInvestor); }
    public int updateFinInvestor(FinInvestor finInvestor) {
        FinInvestor oldInvestor = finInvestorMapper.selectFinInvestorByInvestorId(finInvestor.getInvestorId());
        if (oldInvestor != null && finInvestor.getDeptId() != null && !finInvestor.getDeptId().equals(oldInvestor.getDeptId()) && hasFinancialRecords(finInvestor.getInvestorId())) {
            throw new ServiceException("投资人已有财务记录，不能修改所属机构");
        }
        return finInvestorMapper.updateFinInvestor(finInvestor);
    }
    public int deleteFinInvestorByInvestorIds(Long[] investorIds) {
        if (investorIds != null) {
            for (Long investorId : investorIds) {
                if (hasFinancialRecords(investorId)) {
                    throw new ServiceException("投资人已有财务记录，不能删除；如需停用请修改状态");
                }
            }
        }
        return finInvestorMapper.deleteFinInvestorByInvestorIds(investorIds);
    }

    private boolean hasFinancialRecords(Long investorId) {
        if (investorId == null) {
            return false;
        }
        FinInvestRecord investRecordQuery = new FinInvestRecord();
        investRecordQuery.setInvestorId(investorId);
        if (!finInvestRecordMapper.selectFinInvestRecordList(investRecordQuery).isEmpty()) {
            return true;
        }
        FinInvestorPayment paymentQuery = new FinInvestorPayment();
        paymentQuery.setInvestorId(investorId);
        return !finInvestorPaymentMapper.selectFinInvestorPaymentList(paymentQuery).isEmpty();
    }
}
