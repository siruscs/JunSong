package com.junsong.member.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.domain.MemPurchaseOrder;
import com.junsong.member.domain.MemPurchasePayment;
import com.junsong.member.mapper.MemPurchaseMapper;
import com.junsong.member.service.IMemberPurchasePaymentService;
import com.junsong.member.service.MemberPurchasePaymentValidator;

@Service
public class MemberPurchasePaymentServiceImpl implements IMemberPurchasePaymentService
{
    private final MemPurchaseMapper purchaseMapper;

    public MemberPurchasePaymentServiceImpl(MemPurchaseMapper purchaseMapper)
    {
        this.purchaseMapper = purchaseMapper;
    }

    @Override
    @Transactional
    public int receive(MemPurchasePayment payment)
    {
        if (payment == null || payment.getPurchaseId() == null)
        {
            throw new IllegalArgumentException("purchase id is required");
        }
        MemPurchaseOrder order = purchaseMapper.selectPurchaseForUpdate(payment);
        if (order == null) throw new IllegalArgumentException("purchase order does not exist");
        if (payment.getPaymentNo() == null || payment.getPaymentNo().isBlank())
        {
            payment.setPaymentNo(generatePaymentNo());
        }
        if (payment.getPaymentDate() == null)
        {
            payment.setPaymentDate(new Date());
        }
        BigDecimal receivable = order.getReceivableAmount() == null ? BigDecimal.ZERO : order.getReceivableAmount();
        MemberPurchasePaymentValidator.validate(receivable, payment.getPaymentAmount());
        BigDecimal paid = (order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount())
                .add(payment.getPaymentAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = order.getTotalAmount().subtract(paid).setScale(2, RoundingMode.HALF_UP);
        String status = remaining.signum() == 0 ? "2" : "1";
        int rows = purchaseMapper.insertPurchasePayment(payment);
        if (rows != 1) return rows;
        return purchaseMapper.updatePaymentSnapshot(payment.getPurchaseId(), paid, remaining, status);
    }

    @Override
    @Transactional
    public int update(MemPurchasePayment payment)
    {
        MemPurchaseOrder order = purchaseMapper.selectPurchaseForUpdate(payment);
        if (order == null || "4".equals(order.getOrderStatus())) throw new IllegalArgumentException("购买单不存在或已作废");
        MemPurchasePayment current = purchaseMapper.selectPurchasePaymentForUpdate(payment);
        if (current == null) throw new IllegalArgumentException("付款记录不存在或不属于当前购买单");
        MemberPurchasePaymentValidator.validate(order.getTotalAmount(), payment.getPaymentAmount());
        BigDecimal paid = BigDecimal.ZERO;
        List<MemPurchasePayment> payments = purchaseMapper.selectPaymentsByPurchaseId(payment.getPurchaseId(), payment.getTenantId(), payment.getDeptId());
        for (MemPurchasePayment row : payments)
        {
            paid = paid.add(row.getPaymentId().equals(payment.getPaymentId()) ? payment.getPaymentAmount() : row.getPaymentAmount());
        }
        paid = paid.setScale(2, RoundingMode.HALF_UP);
        if (paid.compareTo(order.getTotalAmount()) > 0) throw new IllegalArgumentException("付款合计不能超过购买单应收金额");
        if (payment.getPaymentDate() == null) payment.setPaymentDate(current.getPaymentDate());
        int rows = purchaseMapper.updatePurchasePayment(payment);
        if (rows != 1) return rows;
        BigDecimal remaining = order.getTotalAmount().subtract(paid).setScale(2, RoundingMode.HALF_UP);
        return purchaseMapper.updatePaymentSnapshot(payment.getPurchaseId(), paid, remaining, paid.signum() == 0 ? "0" : paid.compareTo(order.getTotalAmount()) == 0 ? "2" : "1");
    }

    private String generatePaymentNo()
    {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "PAY" + timestamp + suffix;
    }
}
