package com.junsong.member.service;

import com.junsong.member.domain.MemPurchasePayment;

public interface IMemberPurchasePaymentService
{
    int receive(MemPurchasePayment payment);
    int update(MemPurchasePayment payment);
}
