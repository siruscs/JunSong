package com.junsong.member.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.domain.MemPurchaseItem;
import com.junsong.member.domain.MemPurchaseDelivery;
import com.junsong.member.domain.MemPurchaseOrder;
import com.junsong.member.domain.MemPurchaseReturn;
import com.junsong.member.domain.MemPurchaseReturnItem;
import com.junsong.member.mapper.MemPurchaseMapper;
import com.junsong.member.mapper.MemPurchaseReturnMapper;
import com.junsong.member.service.IMemberPurchaseReturnService;
import com.junsong.member.service.IMemberGrowthService;
import com.junsong.member.service.MemberPurchaseReturnCalculator;

@Service
public class MemberPurchaseReturnServiceImpl implements IMemberPurchaseReturnService
{
    private final MemPurchaseReturnMapper returnMapper;
    private final MemPurchaseMapper purchaseMapper;
    private final IMemberGrowthService growthService;
    private final MemberPurchaseReturnCalculator calculator = new MemberPurchaseReturnCalculator();

    public MemberPurchaseReturnServiceImpl(MemPurchaseReturnMapper returnMapper, MemPurchaseMapper purchaseMapper,
                                           IMemberGrowthService growthService)
    {
        this.returnMapper = returnMapper;
        this.purchaseMapper = purchaseMapper;
        this.growthService = growthService;
    }

    @Override
    public List<MemPurchaseReturn> selectReturnList(MemPurchaseReturn query)
    {
        return returnMapper.selectReturnList(query);
    }

    @Override
    public MemPurchaseReturn selectReturnById(MemPurchaseReturn query)
    {
        MemPurchaseReturn r = returnMapper.selectReturnById(query);
        if (r != null)
        {
            MemPurchaseReturn itemQuery = new MemPurchaseReturn();
            itemQuery.setReturnId(r.getReturnId());
            itemQuery.setTenantId(r.getTenantId());
            itemQuery.setDeptId(r.getDeptId());
            List<MemPurchaseReturnItem> items = returnMapper.selectReturnItems(itemQuery);
            r.setItems(items);
        }
        return r;
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
        // 1. 锁定退货单
        MemPurchaseReturn current = returnMapper.selectReturnForUpdate(value);
        if (current == null) throw new IllegalArgumentException("退货单不存在或不在当前机构范围内");
        if (!"DRAFT".equals(current.getStatus())) throw new IllegalArgumentException("只有草稿状态的退货单可以完成");
        if (current.getItems() == null || current.getItems().isEmpty())
            throw new IllegalArgumentException("退货单没有明细，无法完成");
        // 2. 锁定原购买单
        MemPurchaseOrder purchaseScope = new MemPurchaseOrder();
        purchaseScope.setPurchaseId(current.getPurchaseId());
        purchaseScope.setTenantId(value.getTenantId());
        purchaseScope.setDeptId(value.getDeptId());
        MemPurchaseOrder purchase = purchaseMapper.selectPurchaseOrderForUpdate(purchaseScope);
        if (purchase == null) throw new IllegalArgumentException("原购买单不存在或不在当前机构范围内");
        if ("4".equals(purchase.getOrderStatus())) throw new IllegalArgumentException("原购买单已作废，不能完成退货");
        BigDecimal refundAmount = current.getRefundAmount() == null ? BigDecimal.ZERO : current.getRefundAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal originalTotalAmount = purchase.getTotalAmount() == null ? BigDecimal.ZERO : purchase.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        // 3. 逐条更新原购买明细数量
        for (MemPurchaseReturnItem retItem : current.getItems())
        {
            MemPurchaseDelivery itemScope = new MemPurchaseDelivery();
            itemScope.setItemId(retItem.getItemId()); itemScope.setPurchaseId(current.getPurchaseId());
            itemScope.setTenantId(value.getTenantId()); itemScope.setDeptId(value.getDeptId());
            MemPurchaseItem origin = purchaseMapper.selectPurchaseItemForUpdate(itemScope);
            if (origin == null) throw new IllegalArgumentException("原购买明细不存在或不属于当前购买单");
            BigDecimal returnSale = normalizeQuantity(retItem.getReturnSaleQuantity());
            BigDecimal returnGift = normalizeQuantity(retItem.getReturnGiftQuantity());
            BigDecimal newPurchaseQty = normalizeQuantity(origin.getPurchaseQuantity()).subtract(returnSale);
            BigDecimal newGiftQty = normalizeQuantity(origin.getGiftQuantity()).subtract(returnGift);
            BigDecimal newTotalQty = newPurchaseQty.add(newGiftQty);
            // 已领取数量核减：退货正品不超过已领取正品，退货赠品不超过已领取赠品
            BigDecimal newDeliveredSale = normalizeQuantity(origin.getDeliveredSaleQuantity()).subtract(
                    returnSale.min(normalizeQuantity(origin.getDeliveredSaleQuantity())));
            BigDecimal newDeliveredGift = normalizeQuantity(origin.getDeliveredGiftQuantity()).subtract(
                    returnGift.min(normalizeQuantity(origin.getDeliveredGiftQuantity())));
            BigDecimal newDelivered = newDeliveredSale.add(newDeliveredGift);
            BigDecimal newRemaining = newTotalQty.subtract(newDelivered);
            BigDecimal newItemAmount = origin.getItemAmount() == null ? BigDecimal.ZERO
                    : origin.getItemAmount().setScale(2, RoundingMode.HALF_UP).subtract(
                            retItem.getRefundAmount() == null ? BigDecimal.ZERO : retItem.getRefundAmount().setScale(2, RoundingMode.HALF_UP));
            origin.setPurchaseQuantity(newPurchaseQty);
            origin.setGiftQuantity(newGiftQty);
            origin.setTotalQuantity(newTotalQty);
            origin.setDeliveredSaleQuantity(newDeliveredSale);
            origin.setDeliveredGiftQuantity(newDeliveredGift);
            origin.setDeliveredQuantity(newDelivered);
            origin.setRemainingQuantity(newRemaining);
            origin.setItemAmount(newItemAmount);
            origin.setUpdateBy(value.getUpdateBy());
            if (purchaseMapper.updatePurchaseItem(origin) != 1)
                throw new IllegalArgumentException("购买明细更新失败");
        }
        // 4. 更新原购买单金额
        BigDecimal newTotalAmount = originalTotalAmount.subtract(refundAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newPaidAmount = (purchase.getPaidAmount() == null ? BigDecimal.ZERO : purchase.getPaidAmount())
                .subtract(refundAmount).setScale(2, RoundingMode.HALF_UP);
        if (newPaidAmount.signum() < 0) newPaidAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String paymentStatus;
        if (newPaidAmount.signum() == 0) paymentStatus = "0";
        else if (newPaidAmount.compareTo(newTotalAmount) == 0) paymentStatus = "2";
        else paymentStatus = "1";
        purchaseMapper.updatePurchaseAfterReturn(current.getPurchaseId(), newTotalAmount, newPaidAmount, paymentStatus);
        // 5. 会员积分/成长值核减
        if ("MEMBER".equals(current.getCustomerType()) && current.getMemberId() != null)
        {
            growthService.reversePurchaseRewardByReturn(current.getMemberId(), current.getPurchaseId(),
                    current.getReturnId(), refundAmount, originalTotalAmount, value.getUpdateBy());
        }
        // 6. 更新退货单状态（乐观锁），同步写入已退金额
        value.setVersion(current.getVersion());
        value.setRefundAmount(refundAmount);
        return returnMapper.completeReturn(value);
    }

    private BigDecimal normalizeQuantity(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value.setScale(3, RoundingMode.HALF_UP);
    }
}
