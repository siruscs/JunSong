package com.junsong.member.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.domain.MemPurchaseItem;
import com.junsong.member.domain.MemPurchaseOrder;
import com.junsong.member.domain.MemPurchaseReturn;
import com.junsong.member.domain.MemPurchaseReturnItem;
import com.junsong.member.mapper.MemPurchaseMapper;
import com.junsong.member.mapper.MemPurchaseReturnMapper;
import com.junsong.member.service.IMemberPurchaseReturnService;
import com.junsong.member.service.MemberPurchaseReturnCalculator;

@Service
public class MemberPurchaseReturnServiceImpl implements IMemberPurchaseReturnService
{
    private final MemPurchaseReturnMapper returnMapper;
    private final MemPurchaseMapper purchaseMapper;
    private final MemberPurchaseReturnCalculator calculator = new MemberPurchaseReturnCalculator();

    public MemberPurchaseReturnServiceImpl(MemPurchaseReturnMapper returnMapper, MemPurchaseMapper purchaseMapper)
    {
        this.returnMapper = returnMapper;
        this.purchaseMapper = purchaseMapper;
    }

    @Override
    public List<MemPurchaseReturn> selectReturnList(MemPurchaseReturn query)
    {
        return returnMapper.selectReturnList(query);
    }

    @Override
    public MemPurchaseReturn selectReturnById(MemPurchaseReturn query)
    {
        return returnMapper.selectReturnById(query);
    }

    @Override
    @Transactional
    public int createReturn(MemPurchaseReturn value)
    {
        if (value.getPurchaseId() == null || value.getReturnPeriodId() == null || value.getItems() == null || value.getItems().isEmpty())
        {
            throw new IllegalArgumentException("请选择原购买单、退货办理周期和退货明细");
        }
        MemPurchaseOrder scope = new MemPurchaseOrder();
        scope.setPurchaseId(value.getPurchaseId()); scope.setTenantId(value.getTenantId()); scope.setDeptId(value.getDeptId());
        MemPurchaseOrder purchase = purchaseMapper.selectPurchaseById(scope);
        if (purchase == null) throw new IllegalArgumentException("原购买单不存在或不在当前机构范围内");
        if (purchase.getItems() == null || purchase.getItems().isEmpty()) throw new IllegalArgumentException("原购买单没有可退货明细");
        Map<Long, MemPurchaseReturnItem> returned = new HashMap<>();
        MemPurchaseReturn returnedQuery = new MemPurchaseReturn();
        returnedQuery.setPurchaseId(value.getPurchaseId()); returnedQuery.setTenantId(value.getTenantId()); returnedQuery.setDeptId(value.getDeptId());
        for (MemPurchaseReturnItem item : returnMapper.selectReturnedQuantities(returnedQuery)) returned.put(item.getItemId(), item);
        value.setOriginalPeriodId(purchase.getPeriodId());
        value.setCustomerType(purchase.getCustomerType()); value.setMemberId(purchase.getMemberId());
        value.setCustomerName(purchase.getCustomerName()); value.setCustomerPhone(purchase.getCustomerPhone());
        value.setReturnNo("RT" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        value.setStatus("DRAFT"); value.setRefundedAmount(BigDecimal.ZERO); value.setDelFlag("0");
        value.setRefundAmount(BigDecimal.ZERO);
        for (MemPurchaseReturnItem requested : value.getItems())
        {
            MemPurchaseItem origin = purchase.getItems().stream().filter(item -> item.getItemId().equals(requested.getItemId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("退货明细不属于原购买单"));
            BigDecimal returnSale = normalizeQuantity(requested.getReturnSaleQuantity());
            BigDecimal returnGift = normalizeQuantity(requested.getReturnGiftQuantity());
            MemPurchaseReturnItem alreadyReturned = returned.get(origin.getItemId());
            BigDecimal priorSale = alreadyReturned == null ? BigDecimal.ZERO : normalizeQuantity(alreadyReturned.getReturnSaleQuantity());
            BigDecimal priorGift = alreadyReturned == null ? BigDecimal.ZERO : normalizeQuantity(alreadyReturned.getReturnGiftQuantity());
            // 退货允许覆盖已领取数量；可退上限只扣除本购买单已经办理的历史退货数量。
            BigDecimal availableSale = normalizeQuantity(origin.getPurchaseQuantity()).subtract(priorSale);
            BigDecimal availableGift = normalizeQuantity(origin.getGiftQuantity()).subtract(priorGift);
            if (returnSale.signum() < 0 || returnGift.signum() < 0 || returnSale.compareTo(availableSale) > 0 || returnGift.compareTo(availableGift) > 0)
                throw new IllegalArgumentException("退货数量不能超过原购买数量扣除历史退货后的可退数量");
            BigDecimal itemAmount = origin.getItemAmount();
            if (itemAmount == null) itemAmount = purchase.getItems().size() == 1 ? purchase.getTotalAmount() : BigDecimal.ZERO;
            String amountText = itemAmount == null ? "0" : itemAmount.toPlainString();
            BigDecimal refund = calculator.refundAmount(amountText, origin.getPurchaseQuantity().toPlainString(), origin.getGiftQuantity().toPlainString(),
                    returnSale.toPlainString(), returnGift.toPlainString());
            requested.setReturnId(value.getReturnId()); requested.setPurchaseId(purchase.getPurchaseId()); requested.setTenantId(value.getTenantId()); requested.setDeptId(value.getDeptId());
            requested.setProductId(origin.getProductId()); requested.setProductNameSnapshot(origin.getProductNameSnapshot());
            requested.setReturnSaleQuantity(returnSale); requested.setReturnGiftQuantity(returnGift);
            requested.setReturnTotalQuantity(returnSale.add(returnGift));
            requested.setRefundUnitPrice(calculator.weightedUnitPrice(amountText, origin.getPurchaseQuantity().toPlainString(), origin.getGiftQuantity().toPlainString()));
            requested.setRefundAmount(refund);
            value.setRefundAmount(value.getRefundAmount().add(refund));
        }
        BigDecimal paidAmount = purchase.getPaidAmount() == null ? BigDecimal.ZERO : purchase.getPaidAmount();
        BigDecimal existingRefundAmount = returnMapper.selectExistingRefundAmount(returnedQuery);
        if (existingRefundAmount == null) existingRefundAmount = BigDecimal.ZERO;
        if (value.getRefundAmount().add(existingRefundAmount).compareTo(paidAmount) > 0)
            throw new IllegalArgumentException("退货退款金额超过该购买单已收且尚未退款的金额");
        int rows = returnMapper.insertReturn(value);
        for (MemPurchaseReturnItem item : value.getItems()) { item.setReturnId(value.getReturnId()); returnMapper.insertReturnItem(item); }
        return rows;
    }

    @Override
    @Transactional
    public int completeReturn(MemPurchaseReturn value)
    {
        if (value.getReturnId() == null) throw new IllegalArgumentException("退货单不存在");
        MemPurchaseReturn current = returnMapper.selectReturnById(value);
        if (current == null) throw new IllegalArgumentException("退货单不存在或不在当前机构范围内");
        if (!"DRAFT".equals(current.getStatus())) throw new IllegalArgumentException("只有草稿状态的退货单可以完成");
        value.setVersion(current.getVersion());
        return returnMapper.completeReturn(value);
    }

    private BigDecimal normalizeQuantity(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value.setScale(3, java.math.RoundingMode.HALF_UP);
    }
}
