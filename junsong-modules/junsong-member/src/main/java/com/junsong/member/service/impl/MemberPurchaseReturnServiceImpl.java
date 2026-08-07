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
    public int updateReturn(MemPurchaseReturn value)
    {
        if (value.getReturnId() == null) throw new IllegalArgumentException("退货单不存在");
        MemPurchaseReturn current = returnMapper.selectReturnForUpdate(value);
        if (current == null) throw new IllegalArgumentException("退货单不存在或不在当前机构范围内");
        if (!"DRAFT".equals(current.getStatus())) throw new IllegalArgumentException("只有草稿状态的退货单可以编辑");
        value.setVersion(current.getVersion());

        // 查询旧明细，用于补齐必填字段和计算可退数量
        MemPurchaseReturn itemQuery = new MemPurchaseReturn();
        itemQuery.setReturnId(value.getReturnId());
        itemQuery.setTenantId(value.getTenantId());
        itemQuery.setDeptId(value.getDeptId());
        List<MemPurchaseReturnItem> existingItems = returnMapper.selectReturnItems(itemQuery);
        Map<Long, MemPurchaseReturnItem> existingMap = new HashMap<>();
        for (MemPurchaseReturnItem ei : existingItems) existingMap.put(ei.getItemId(), ei);

        if (value.getItems() != null && !value.getItems().isEmpty())
        {
            // 加载原购买单及明细，用于校验可退数量
            MemPurchaseOrder scope = new MemPurchaseOrder();
            scope.setPurchaseId(current.getPurchaseId());
            scope.setTenantId(value.getTenantId());
            scope.setDeptId(value.getDeptId());
            MemPurchaseOrder purchase = purchaseMapper.selectPurchaseById(scope);
            if (purchase == null) throw new IllegalArgumentException("原购买单不存在或不在当前机构范围内");
            if (purchase.getItems() == null || purchase.getItems().isEmpty())
                throw new IllegalArgumentException("原购买单没有可退货明细");

            // 查询该购买单所有非作废退货的累计数量（包含当前退货单）
            MemPurchaseReturn returnedQuery = new MemPurchaseReturn();
            returnedQuery.setPurchaseId(current.getPurchaseId());
            returnedQuery.setTenantId(value.getTenantId());
            returnedQuery.setDeptId(value.getDeptId());
            Map<Long, MemPurchaseReturnItem> allReturned = new HashMap<>();
            for (MemPurchaseReturnItem item : returnMapper.selectReturnedQuantities(returnedQuery))
                allReturned.put(item.getItemId(), item);

            BigDecimal totalAmount = BigDecimal.ZERO;
            for (MemPurchaseReturnItem item : value.getItems())
            {
                MemPurchaseItem origin = purchase.getItems().stream()
                        .filter(i -> i.getItemId().equals(item.getItemId())).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("退货明细不属于原购买单"));
                BigDecimal returnSale = normalizeQuantity(item.getReturnSaleQuantity());
                BigDecimal returnGift = normalizeQuantity(item.getReturnGiftQuantity());
                if (returnSale.signum() < 0 || returnGift.signum() < 0)
                    throw new IllegalArgumentException("退货数量不能为负数");

                // 累计退货（含当前单）减去当前单旧明细 = 其他退货单已退数量
                MemPurchaseReturnItem allRet = allReturned.get(origin.getItemId());
                MemPurchaseReturnItem oldItem = existingMap.get(origin.getItemId());
                BigDecimal otherSale = (allRet == null ? BigDecimal.ZERO : normalizeQuantity(allRet.getReturnSaleQuantity()))
                        .subtract(oldItem == null ? BigDecimal.ZERO : normalizeQuantity(oldItem.getReturnSaleQuantity()));
                BigDecimal otherGift = (allRet == null ? BigDecimal.ZERO : normalizeQuantity(allRet.getReturnGiftQuantity()))
                        .subtract(oldItem == null ? BigDecimal.ZERO : normalizeQuantity(oldItem.getReturnGiftQuantity()));
                BigDecimal availableSale = normalizeQuantity(origin.getPurchaseQuantity()).subtract(otherSale);
                BigDecimal availableGift = normalizeQuantity(origin.getGiftQuantity()).subtract(otherGift);
                if (returnSale.compareTo(availableSale) > 0 || returnGift.compareTo(availableGift) > 0)
                {
                    String name = origin.getProductNameSnapshot() == null ? String.valueOf(origin.getItemId()) : origin.getProductNameSnapshot();
                    throw new IllegalArgumentException("商品「" + name + "」退货数量超过可退数量"
                            + "（正品可退 " + availableSale + "，赠品可退 " + availableGift + "）");
                }

                item.setReturnId(value.getReturnId());
                item.setPurchaseId(current.getPurchaseId());
                item.setTenantId(value.getTenantId());
                item.setDeptId(value.getDeptId());
                if (item.getProductId() == null) item.setProductId(origin.getProductId());
                if (item.getProductNameSnapshot() == null) item.setProductNameSnapshot(origin.getProductNameSnapshot());
                item.setReturnSaleQuantity(returnSale);
                item.setReturnGiftQuantity(returnGift);
                item.setReturnTotalQuantity(returnSale.add(returnGift));
                BigDecimal unitPrice = item.getRefundUnitPrice() == null ? BigDecimal.ZERO : item.getRefundUnitPrice().setScale(2, RoundingMode.HALF_UP);
                item.setRefundUnitPrice(unitPrice);
                BigDecimal amount = item.getRefundAmount() == null ? BigDecimal.ZERO : item.getRefundAmount();
                item.setRefundAmount(amount.setScale(2, RoundingMode.HALF_UP));
                totalAmount = totalAmount.add(item.getRefundAmount());
            }

            // 校验退款金额不超过已收金额扣除其他退货单的退款
            BigDecimal paidAmount = purchase.getPaidAmount() == null ? BigDecimal.ZERO : purchase.getPaidAmount();
            BigDecimal existingRefundAmount = returnMapper.selectExistingRefundAmount(returnedQuery);
            if (existingRefundAmount == null) existingRefundAmount = BigDecimal.ZERO;
            BigDecimal oldRefundAmount = current.getRefundAmount() == null ? BigDecimal.ZERO : current.getRefundAmount();
            BigDecimal otherRefundAmount = existingRefundAmount.subtract(oldRefundAmount);
            if (totalAmount.add(otherRefundAmount).compareTo(paidAmount) > 0)
                throw new IllegalArgumentException("退货退款金额超过该购买单已收且尚未退款的金额");

            int rows = returnMapper.updateReturn(value);
            if (rows != 1) return rows;

            MemPurchaseReturn delQuery = new MemPurchaseReturn();
            delQuery.setReturnId(value.getReturnId());
            delQuery.setTenantId(value.getTenantId());
            delQuery.setDeptId(value.getDeptId());
            returnMapper.deleteReturnItems(delQuery);
            for (MemPurchaseReturnItem item : value.getItems())
            {
                returnMapper.insertReturnItem(item);
            }
            totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
            MemPurchaseReturn amtUpdate = new MemPurchaseReturn();
            amtUpdate.setReturnId(value.getReturnId());
            amtUpdate.setTenantId(value.getTenantId());
            amtUpdate.setDeptId(value.getDeptId());
            amtUpdate.setRefundAmount(totalAmount);
            amtUpdate.setUpdateBy(value.getUpdateBy());
            amtUpdate.setVersion(value.getVersion() + 1);
            returnMapper.updateReturnTotalAmount(amtUpdate);
            return rows;
        }
        return returnMapper.updateReturn(value);
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
        // 2. 锁定原购买单并重新校验数量/金额（防止两个草稿先后完成造成超额）
        MemPurchaseOrder purchaseScope = new MemPurchaseOrder();
        purchaseScope.setPurchaseId(current.getPurchaseId());
        purchaseScope.setTenantId(value.getTenantId());
        purchaseScope.setDeptId(value.getDeptId());
        MemPurchaseOrder purchase = purchaseMapper.selectPurchaseOrderForUpdate(purchaseScope);
        if (purchase == null) throw new IllegalArgumentException("原购买单不存在或不在当前机构范围内");
        if ("4".equals(purchase.getOrderStatus())) throw new IllegalArgumentException("原购买单已作废，不能完成退货");
        // 2a. 计算历史累计（非作废）退货数量/金额，排除当前单本身
        MemPurchaseReturn returnedQuery = new MemPurchaseReturn();
        returnedQuery.setPurchaseId(current.getPurchaseId()); returnedQuery.setTenantId(value.getTenantId()); returnedQuery.setDeptId(value.getDeptId());
        Map<Long, MemPurchaseReturnItem> priorReturns = new HashMap<>();
        for (MemPurchaseReturnItem item : returnMapper.selectReturnedQuantities(returnedQuery)) priorReturns.put(item.getItemId(), item);
        BigDecimal priorRefund = returnMapper.selectExistingRefundAmount(returnedQuery);
        if (priorRefund == null) priorRefund = BigDecimal.ZERO;
        // 累计金额：历史退款（含当前单之前的草稿/完成单）- 当前单的草稿应退 + 当前单的应退
        // 简化：priorRefund（已含所有非作废的 refund_amount）已经包括 current，所以无需额外修正
        BigDecimal paidAmount = purchase.getPaidAmount() == null ? BigDecimal.ZERO : purchase.getPaidAmount();
        if (priorRefund.compareTo(paidAmount) > 0)
            throw new IllegalArgumentException("累计退款金额超过该购买单已收金额，请作废多余草稿后再操作");
        // 2b. 逐条数量校验（历史累计 + 当前单 = 所有非作废合计 不能超过原购买数量）
        for (MemPurchaseReturnItem retItem : current.getItems())
        {
            MemPurchaseItem origin = purchase.getItems() == null ? null : purchase.getItems().stream()
                    .filter(i -> i.getItemId().equals(retItem.getItemId())).findFirst().orElse(null);
            if (origin == null)
            {
                MemPurchaseDelivery itemScope = new MemPurchaseDelivery();
                itemScope.setItemId(retItem.getItemId()); itemScope.setPurchaseId(current.getPurchaseId());
                itemScope.setTenantId(value.getTenantId()); itemScope.setDeptId(value.getDeptId());
                origin = purchaseMapper.selectPurchaseItemForUpdate(itemScope);
            }
            if (origin == null) throw new IllegalArgumentException("原购买明细不存在或不属于当前购买单");
            MemPurchaseReturnItem prior = priorReturns.get(origin.getItemId());
            // prior 已经包含当前单（因为当前单是非作废的 DRAFT），所以直接与原上限比较即可
            BigDecimal priorSale = prior == null ? BigDecimal.ZERO : normalizeQuantity(prior.getReturnSaleQuantity());
            BigDecimal priorGift = prior == null ? BigDecimal.ZERO : normalizeQuantity(prior.getReturnGiftQuantity());
            BigDecimal originSale = normalizeQuantity(origin.getPurchaseQuantity());
            BigDecimal originGift = normalizeQuantity(origin.getGiftQuantity());
            if (priorSale.compareTo(originSale) > 0 || priorGift.compareTo(originGift) > 0)
                throw new IllegalArgumentException("商品「" + (origin.getProductNameSnapshot() == null ? origin.getItemId() : origin.getProductNameSnapshot())
                        + "」累计退货数量超过原购买数量，请作废多余草稿后再操作");
        }
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
            if (newPurchaseQty.signum() < 0 || newGiftQty.signum() < 0)
                throw new IllegalArgumentException("商品「" + (origin.getProductNameSnapshot() == null ? origin.getItemId() : origin.getProductNameSnapshot())
                        + "」退货数量超过购买明细剩余可退数量，请作废其他草稿后重试");
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
        value.setRefundedAmount(refundAmount);
        return returnMapper.completeReturn(value);
    }

    private BigDecimal normalizeQuantity(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value.setScale(3, RoundingMode.HALF_UP);
    }
}
