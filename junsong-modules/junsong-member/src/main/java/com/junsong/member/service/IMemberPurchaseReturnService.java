package com.junsong.member.service;

import java.util.List;
import com.junsong.member.domain.MemPurchaseReturn;

public interface IMemberPurchaseReturnService
{
    List<MemPurchaseReturn> selectReturnList(MemPurchaseReturn query);
    MemPurchaseReturn selectReturnById(MemPurchaseReturn query);
    int createReturn(MemPurchaseReturn value);
    int completeReturn(MemPurchaseReturn value);
}
