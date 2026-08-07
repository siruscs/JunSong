package com.junsong.member.mapper;

import java.util.List;
import java.math.BigDecimal;
import com.junsong.member.domain.MemPurchaseReturn;
import com.junsong.member.domain.MemPurchaseReturnItem;

public interface MemPurchaseReturnMapper
{
    List<MemPurchaseReturn> selectReturnList(MemPurchaseReturn query);
    MemPurchaseReturn selectReturnById(MemPurchaseReturn query);
    List<MemPurchaseReturnItem> selectReturnItems(MemPurchaseReturn query);
    List<MemPurchaseReturnItem> selectReturnedQuantities(MemPurchaseReturn query);
    BigDecimal selectExistingRefundAmount(MemPurchaseReturn query);
    int insertReturn(MemPurchaseReturn value);
    int insertReturnItem(MemPurchaseReturnItem value);
    int completeReturn(MemPurchaseReturn value);
    int updateReturn(MemPurchaseReturn value);
    int deleteReturnItems(MemPurchaseReturn query);
    int updateReturnTotalAmount(MemPurchaseReturn value);
    MemPurchaseReturn selectReturnForUpdate(MemPurchaseReturn query);
}
