package com.junsong.finance.mapper;

import java.util.List;
import com.junsong.finance.domain.FinProfitShareDetail;

public interface FinProfitShareDetailMapper
{
    public List<FinProfitShareDetail> selectFinProfitShareDetailByShareId(Long shareId);
    public int insertFinProfitShareDetail(FinProfitShareDetail finProfitShareDetail);
    public int updateFinProfitShareDetail(FinProfitShareDetail finProfitShareDetail);
}
