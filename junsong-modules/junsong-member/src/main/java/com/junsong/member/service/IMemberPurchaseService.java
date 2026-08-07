package com.junsong.member.service;

import java.util.List;
import com.junsong.member.domain.MemPurchaseOrder;
import java.util.Map;

public interface IMemberPurchaseService
{
    MemPurchaseOrder selectPurchaseById(MemPurchaseOrder query);
    List<MemPurchaseOrder> selectPurchaseList(MemPurchaseOrder order);
    Map<String, Object> selectPurchaseStatistics(MemPurchaseOrder query);
    int createPurchase(MemPurchaseOrder order);
    int updatePurchaseBasic(MemPurchaseOrder order);
    int cancelPurchase(MemPurchaseOrder query);
    int bindPurchaseMember(Long purchaseId, Long tenantId, Long deptId, Long memberId, String operator);
}
