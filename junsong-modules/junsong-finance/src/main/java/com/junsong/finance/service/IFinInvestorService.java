package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinInvestor;

public interface IFinInvestorService
{
    public FinInvestor selectFinInvestorByInvestorId(Long investorId);
    public List<FinInvestor> selectFinInvestorList(FinInvestor finInvestor);
    public int insertFinInvestor(FinInvestor finInvestor);
    public int updateFinInvestor(FinInvestor finInvestor);
    public int deleteFinInvestorByInvestorIds(Long[] investorIds);
}
