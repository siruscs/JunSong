package com.junsong.member.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.domain.MemPurchaseDelivery;
import com.junsong.member.domain.MemPurchaseItem;
import com.junsong.member.mapper.MemPurchaseMapper;
import com.junsong.member.service.IMemberPurchaseDeliveryService;
import com.junsong.member.service.MemberPurchaseDeliveryValidator;

@Service
public class MemberPurchaseDeliveryServiceImpl implements IMemberPurchaseDeliveryService
{
    private final MemPurchaseMapper purchaseMapper;

    public MemberPurchaseDeliveryServiceImpl(MemPurchaseMapper purchaseMapper)
    {
        this.purchaseMapper = purchaseMapper;
    }

    @Override
    @Transactional
    public int deliver(MemPurchaseDelivery delivery)
    {
        if (delivery == null || delivery.getItemId() == null)
        {
            throw new IllegalArgumentException("purchase item id is required");
        }
        MemPurchaseItem item = purchaseMapper.selectPurchaseItemForUpdate(delivery);
        if (item == null) throw new IllegalArgumentException("purchase item does not exist");
        BigDecimal sale = delivery.getSaleDeliveryQuantity() == null ? BigDecimal.ZERO : delivery.getSaleDeliveryQuantity();
        BigDecimal gift = delivery.getGiftDeliveryQuantity() == null ? BigDecimal.ZERO : delivery.getGiftDeliveryQuantity();
        BigDecimal deliveredSale = item.getDeliveredSaleQuantity() == null ? BigDecimal.ZERO : item.getDeliveredSaleQuantity();
        BigDecimal deliveredGift = item.getDeliveredGiftQuantity() == null ? BigDecimal.ZERO : item.getDeliveredGiftQuantity();
        BigDecimal remainingSale = item.getPurchaseQuantity().subtract(deliveredSale);
        BigDecimal remainingGift = item.getGiftQuantity().subtract(deliveredGift);
        MemberPurchaseDeliveryValidator.validate(remainingSale, remainingGift, sale, gift);
        delivery.setTotalDeliveryQuantity(sale.add(gift));
        delivery.setPurchaseId(item.getPurchaseId());
        if (delivery.getDeliveryNo() == null || delivery.getDeliveryNo().isBlank())
        {
            delivery.setDeliveryNo(generateDeliveryNo());
        }
        if (delivery.getDeliveryDate() == null)
        {
            delivery.setDeliveryDate(new Date());
        }
        int insertRows = purchaseMapper.insertPurchaseDelivery(delivery);
        if (insertRows != 1) return insertRows;
        BigDecimal delivered = (item.getDeliveredQuantity() == null ? BigDecimal.ZERO : item.getDeliveredQuantity())
                .add(delivery.getTotalDeliveryQuantity());
        BigDecimal remaining = item.getTotalQuantity().subtract(delivered);
        int rows = purchaseMapper.updateDeliverySnapshot(item.getItemId(), delivered,
                deliveredSale.add(sale), deliveredGift.add(gift), remaining);
        if (rows != 1) return rows;
        return purchaseMapper.updateDeliveryOrderStatus(item.getPurchaseId());
    }

    @Override
    @Transactional
    public int update(MemPurchaseDelivery delivery)
    {
        MemPurchaseItem item = purchaseMapper.selectPurchaseItemForUpdate(delivery);
        if (item == null) throw new IllegalArgumentException("购买明细不存在或不属于当前购买单");
        MemPurchaseDelivery current = purchaseMapper.selectPurchaseDeliveryForUpdate(delivery);
        if (current == null) throw new IllegalArgumentException("领取记录不存在或不属于当前购买单");
        BigDecimal sale = delivery.getSaleDeliveryQuantity() == null ? BigDecimal.ZERO : delivery.getSaleDeliveryQuantity();
        BigDecimal gift = delivery.getGiftDeliveryQuantity() == null ? BigDecimal.ZERO : delivery.getGiftDeliveryQuantity();
        BigDecimal otherSale = BigDecimal.ZERO, otherGift = BigDecimal.ZERO;
        List<MemPurchaseDelivery> records = purchaseMapper.selectDeliveriesByPurchaseId(delivery.getPurchaseId(), delivery.getTenantId(), delivery.getDeptId());
        for (MemPurchaseDelivery row : records) if (!row.getDeliveryId().equals(delivery.getDeliveryId())) { otherSale = otherSale.add(row.getSaleDeliveryQuantity()); otherGift = otherGift.add(row.getGiftDeliveryQuantity()); }
        MemberPurchaseDeliveryValidator.validate(item.getPurchaseQuantity().subtract(otherSale), item.getGiftQuantity().subtract(otherGift), sale, gift);
        delivery.setTotalDeliveryQuantity(sale.add(gift));
        if (delivery.getDeliveryDate() == null) delivery.setDeliveryDate(current.getDeliveryDate());
        int rows = purchaseMapper.updatePurchaseDelivery(delivery);
        if (rows != 1) return rows;
        BigDecimal deliveredSale = otherSale.add(sale), deliveredGift = otherGift.add(gift);
        BigDecimal delivered = deliveredSale.add(deliveredGift);
        rows = purchaseMapper.updateDeliverySnapshot(item.getItemId(), delivered, deliveredSale, deliveredGift, item.getTotalQuantity().subtract(delivered));
        if (rows != 1) return rows;
        return purchaseMapper.updateDeliveryOrderStatus(delivery.getPurchaseId());
    }

    private String generateDeliveryNo()
    {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "DEL" + timestamp + suffix;
    }
}
