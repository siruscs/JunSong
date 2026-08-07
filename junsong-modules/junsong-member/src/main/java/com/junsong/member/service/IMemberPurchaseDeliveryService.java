package com.junsong.member.service;

import com.junsong.member.domain.MemPurchaseDelivery;

public interface IMemberPurchaseDeliveryService
{
    int deliver(MemPurchaseDelivery delivery);
    int update(MemPurchaseDelivery delivery);
}
