package com.junsong.finance.mapper;

import java.util.List;
import com.junsong.finance.domain.FinInvestor;

public interface FinInvestorMapper
{
    public FinInvestor selectFinInvestorByInvestorId(Long investorId);
    public List<FinInvestor> selectFinInvestorList(FinInvestor finInvestor);
    public int insertFinInvestor(FinInvestor finInvestor);
    public int updateFinInvestor(FinInvestor finInvestor);
    public int deleteFinInvestorByInvestorId(Long investorId);
    public int deleteFinInvestorByInvestorIds(Long[] investorIds);
}
