package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinAdvance;

/**
 * 借支记录Service接口
 * 
 * @author junsong
 */
public interface IFinAdvanceService
{
    public FinAdvance selectFinAdvanceByAdvanceId(Long advanceId);
    public List<FinAdvance> selectFinAdvanceList(FinAdvance finAdvance);
    public int insertFinAdvance(FinAdvance finAdvance);
    public int updateFinAdvance(FinAdvance finAdvance);
    public int deleteFinAdvanceByAdvanceIds(Long[] advanceIds);
    public int deleteFinAdvanceByAdvanceId(Long advanceId);
    public boolean checkAdvanceNoUnique(FinAdvance finAdvance);
    public int verifyAdvance(Long advanceId, String verifyBy);
}
