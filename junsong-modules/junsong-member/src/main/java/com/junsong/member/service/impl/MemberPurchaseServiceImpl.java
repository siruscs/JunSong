package com.junsong.member.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.domain.MemPurchaseItem;
import com.junsong.member.domain.MemPurchaseDelivery;
import com.junsong.member.domain.MemPurchaseOrder;
import com.junsong.member.mapper.MemPurchaseMapper;
import com.junsong.member.mapper.MemMemberMapper;
import com.junsong.member.domain.MemMember;
import com.junsong.member.service.IMemberPurchaseService;
import com.junsong.member.service.IMemberIdentityPolicyService;
import com.junsong.member.service.MemberIdentityResolutionValidator;
import com.junsong.member.service.IMemberCampaignPolicyService;
import com.junsong.member.service.IMemberGrowthService;
import com.junsong.member.domain.MemCampaignPolicy;
import com.junsong.member.domain.MemCampaignPolicyPackage;

@Service
public class MemberPurchaseServiceImpl implements IMemberPurchaseService
{
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private final MemPurchaseMapper purchaseMapper;
    private final MemMemberMapper memberMapper;
    private final IMemberIdentityPolicyService identityPolicyService;
    private final IMemberCampaignPolicyService campaignPolicyService;
    private final IMemberGrowthService growthService;

    public MemberPurchaseServiceImpl(MemPurchaseMapper purchaseMapper,
                                     MemMemberMapper memberMapper,
                                     IMemberIdentityPolicyService identityPolicyService,
                                     IMemberCampaignPolicyService campaignPolicyService,
                                     IMemberGrowthService growthService)
    {
        this.purchaseMapper = purchaseMapper;
        this.memberMapper = memberMapper;
        this.identityPolicyService = identityPolicyService;
        this.campaignPolicyService = campaignPolicyService;
        this.growthService = growthService;
    }

    @Override
    public MemPurchaseOrder selectPurchaseById(MemPurchaseOrder query)
    {
        return purchaseMapper.selectPurchaseById(query);
    }

    @Override
    public List<MemPurchaseOrder> selectPurchaseList(MemPurchaseOrder order)
    {
        return purchaseMapper.selectPurchaseList(order);
    }

    @Override
    public Map<String, Object> selectPurchaseStatistics(MemPurchaseOrder query)
    {
        return purchaseMapper.selectPurchaseStatistics(query);
    }

    @Override
    @Transactional
    public int createPurchase(MemPurchaseOrder order)
    {
        validate(order);
        if (order.getPurchaseNo() == null || order.getPurchaseNo().isBlank())
        {
            order.setPurchaseNo(generatePurchaseNo());
        }
        if (order.getIdentityConfirmed() == null)
        {
            order.setIdentityConfirmed(Boolean.FALSE);
        }
        BigDecimal amount = ZERO;
        for (MemPurchaseItem item : order.getItems())
        {
            String productName = purchaseMapper.selectProductNameById(order.getTenantId(), order.getDeptId(), item.getProductId());
            if (productName == null || productName.isBlank())
            {
                throw new IllegalArgumentException("product does not exist or is out of scope");
            }
            item.setProductNameSnapshot(productName);
            BigDecimal productSalePrice = purchaseMapper.selectProductSalePriceById(
                    order.getTenantId(), order.getDeptId(), item.getProductId());
            BigDecimal resolvedUnitPrice = item.getPolicyId() == null ? item.getUnitPrice() : productSalePrice;
            if (resolvedUnitPrice == null)
            {
                throw new IllegalArgumentException("purchase unit price must be greater than zero");
            }
            item.setUnitPrice(resolvedUnitPrice.setScale(2, RoundingMode.HALF_UP));
            if (item.getUnitPrice().signum() <= 0)
            {
                throw new IllegalArgumentException("purchase unit price must be greater than zero");
            }
            item.setItemAmount(null);
            applyPolicySnapshot(order, item);
            if (item.getGiftQuantity() == null) item.setGiftQuantity(BigDecimal.ZERO);
            item.setDeliveredQuantity(BigDecimal.ZERO);
            item.setDeliveredSaleQuantity(BigDecimal.ZERO);
            item.setDeliveredGiftQuantity(BigDecimal.ZERO);
            item.setTotalQuantity(item.getPurchaseQuantity().add(item.getGiftQuantity()));
            item.setRemainingQuantity(item.getTotalQuantity().subtract(item.getDeliveredQuantity()));
            if (item.getItemAmount() == null)
            {
                item.setItemAmount(item.getUnitPrice().multiply(item.getPurchaseQuantity())
                        .setScale(2, RoundingMode.HALF_UP));
            }
            amount = amount.add(item.getItemAmount());
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal paid = order.getPaidAmount() == null ? ZERO : order.getPaidAmount();
        if (paid.compareTo(ZERO) < 0 || paid.compareTo(amount) > 0)
        {
            throw new IllegalArgumentException("paid amount is outside the order amount");
        }
        order.setTotalAmount(amount);
        order.setPaidAmount(paid.setScale(2, RoundingMode.HALF_UP));
        order.setReceivableAmount(amount.subtract(paid).setScale(2, RoundingMode.HALF_UP));
        order.setPaymentStatus(paid.signum() == 0 ? "0" : paid.compareTo(amount) == 0 ? "2" : "1");
        order.setDeliveryStatus(order.getItems().stream().allMatch(i -> i.getRemainingQuantity().signum() == 0) ? "2" : "0");
        if (order.getOrderStatus() == null) order.setOrderStatus("1");
        int rows = purchaseMapper.insertPurchase(order);
        if (rows != 1) return rows;
        for (MemPurchaseItem item : order.getItems())
        {
            item.setPurchaseId(order.getPurchaseId());
            item.setTenantId(order.getTenantId());
            item.setDeptId(order.getDeptId());
            purchaseMapper.insertPurchaseItem(item);
        }
        if ("MEMBER".equals(order.getCustomerType()) && order.getMemberId() != null)
        {
            growthService.awardPurchaseReward(order.getMemberId(), null, order.getCustomerName(),
                    order.getDeptId(), order.getPurchaseId(), order.getTotalAmount(), order.getCreateBy());
        }
        return rows;
    }

    private String generatePurchaseNo()
    {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "PO" + timestamp + suffix;
    }

    @Override
    @Transactional
    public int cancelPurchase(MemPurchaseOrder query)
    {
        MemPurchaseOrder order = purchaseMapper.selectPurchaseOrderForUpdate(query);
        if (order == null) throw new IllegalArgumentException("purchase order does not exist or is out of scope");
        if ("4".equals(order.getOrderStatus())) return 0;
        if ("2".equals(order.getDeliveryStatus())) throw new IllegalArgumentException("已全部领取的购买单不能作废");
        int rows = purchaseMapper.cancelPurchase(query);
        if (rows != 1) return rows;
        if ("MEMBER".equals(order.getCustomerType()) && order.getMemberId() != null)
            growthService.reversePurchaseReward(order.getMemberId(), order.getPurchaseId(), query.getUpdateBy());
        return rows;
    }

    @Override
    @Transactional
    public int updatePurchaseBasic(MemPurchaseOrder query)
    {
        MemPurchaseOrder order = purchaseMapper.selectPurchaseOrderForUpdate(query);
        if (order == null) throw new IllegalArgumentException("购买单不存在或不属于当前机构");
        if ("4".equals(order.getOrderStatus())) throw new IllegalArgumentException("已作废的购买单不能编辑");
        BigDecimal oldTotalAmount = order.getTotalAmount() == null ? ZERO : order.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        int rows = purchaseMapper.updatePurchaseBasic(query);
        if (rows != 1) return rows;
        BigDecimal newTotalAmount = null;
        if (query.getItems() != null)
        {
            BigDecimal total = ZERO;
            for (MemPurchaseItem item : query.getItems())
            {
                MemPurchaseItem current = purchaseMapper.selectPurchaseItemForUpdate(deliveryScope(query, item));
                if (current == null) throw new IllegalArgumentException("购买明细不存在或不属于当前购买单");
                BigDecimal purchaseQuantity = item.getPurchaseQuantity() == null ? current.getPurchaseQuantity() : item.getPurchaseQuantity();
                BigDecimal giftQuantity = item.getGiftQuantity() == null ? current.getGiftQuantity() : item.getGiftQuantity();
                if (purchaseQuantity.compareTo(current.getDeliveredSaleQuantity()) < 0 || giftQuantity.compareTo(current.getDeliveredGiftQuantity()) < 0)
                    throw new IllegalArgumentException("购买数量不能少于已领取数量");
                BigDecimal unitPrice = item.getUnitPrice() == null ? current.getUnitPrice() : item.getUnitPrice();
                if (unitPrice.signum() <= 0) throw new IllegalArgumentException("商品单价必须大于0");
                item.setPurchaseId(query.getPurchaseId()); item.setTenantId(query.getTenantId()); item.setDeptId(query.getDeptId());
                item.setTotalQuantity(purchaseQuantity.add(giftQuantity));
                item.setRemainingQuantity(item.getTotalQuantity().subtract(current.getDeliveredQuantity()));
                item.setItemAmount(unitPrice.multiply(purchaseQuantity).setScale(2, RoundingMode.HALF_UP));
                item.setUnitPrice(unitPrice.setScale(2, RoundingMode.HALF_UP));
                item.setUpdateBy(query.getUpdateBy());
                if (purchaseMapper.updatePurchaseItem(item) != 1) throw new IllegalArgumentException("购买明细保存失败");
                total = total.add(item.getItemAmount());
            }
            total = total.setScale(2, RoundingMode.HALF_UP);
            if (query.getPaidAmount() != null && query.getPaidAmount().compareTo(total) > 0) throw new IllegalArgumentException("应收金额不能低于已收金额");
            purchaseMapper.updateOrderAmount(query.getPurchaseId(), total);
            newTotalAmount = total;
        }
        // 会员购买单金额变更时，核减原积分/成长值并重新计算
        if (newTotalAmount != null && "MEMBER".equals(order.getCustomerType()) && order.getMemberId() != null
                && newTotalAmount.compareTo(oldTotalAmount) != 0)
        {
            growthService.reversePurchaseReward(order.getMemberId(), query.getPurchaseId(), query.getUpdateBy());
            growthService.reawardPurchaseReward(order.getMemberId(), null, order.getCustomerName(),
                    order.getDeptId(), query.getPurchaseId(), newTotalAmount, query.getUpdateBy());
        }
        return rows;
    }

    private MemPurchaseDelivery deliveryScope(MemPurchaseOrder order, MemPurchaseItem item)
    {
        MemPurchaseDelivery scope = new MemPurchaseDelivery();
        scope.setItemId(item.getItemId()); scope.setPurchaseId(order.getPurchaseId()); scope.setTenantId(order.getTenantId()); scope.setDeptId(order.getDeptId());
        return scope;
    }

    @Override
    @Transactional
    public int bindPurchaseMember(Long purchaseId, Long tenantId, Long deptId, Long memberId, String operator)
    {
        // 1. 先按购买单ID定位订单，不依赖调用方传入的deptId（避免用户切换了当前部门导致找不到原订单）
        MemPurchaseOrder baseQuery = new MemPurchaseOrder();
        baseQuery.setPurchaseId(purchaseId);
        baseQuery.setTenantId(tenantId);
        MemPurchaseOrder baseOrder = purchaseMapper.selectPurchaseById(baseQuery);
        if (baseOrder == null) throw new IllegalArgumentException("购买单不存在或不在权限范围内");

        final Long orderDeptId = baseOrder.getDeptId();
        if (orderDeptId == null) throw new IllegalArgumentException("购买单部门信息异常");

        // 2. 用订单实际所属部门加行锁，避免并发修改
        MemPurchaseOrder scope = new MemPurchaseOrder();
        scope.setPurchaseId(purchaseId);
        scope.setTenantId(tenantId);
        scope.setDeptId(orderDeptId);
        MemPurchaseOrder order = purchaseMapper.selectPurchaseOrderForUpdate(scope);
        if (order == null) throw new IllegalArgumentException("购买单不存在或不在权限范围内");

        if ("4".equals(order.getOrderStatus())) throw new IllegalArgumentException("已作废的购买单无法绑定会员");
        if ("MEMBER".equals(order.getCustomerType()) && memberId.equals(order.getMemberId())) return 1;
        if (order.getMemberId() != null) throw new IllegalArgumentException("该购买单已绑定会员，不可重复绑定");

        // 3. 校验会员：必须与购买单属于同一部门，且状态为正常
        MemMember member = memberMapper.selectMemMemberByMemberId(memberId);
        if (member == null) {
            throw new IllegalArgumentException("会员不存在");
        }
        if (!"0".equals(member.getStatus())) {
            throw new IllegalArgumentException("会员状态无效，无法绑定");
        }
        if (!orderDeptId.equals(member.getDeptId())) {
            throw new IllegalArgumentException("会员与购买单不属于同一机构，无法绑定");
        }

        scope.setMemberId(memberId);
        scope.setCustomerType("MEMBER");
        scope.setCustomerName(member.getMemberName());
        scope.setCustomerPhone(member.getPhone());
        scope.setIdentityMode("MEMBER_NO");
        scope.setIdentityConfirmed(Boolean.TRUE);
        scope.setUpdateBy(operator);
        return purchaseMapper.bindPurchaseMember(scope);
    }

    private void applyPolicySnapshot(MemPurchaseOrder order, MemPurchaseItem item)
    {
        BigDecimal requestedGift = item.getGiftQuantity() == null ? BigDecimal.ZERO : item.getGiftQuantity();
        if (item.getPolicyId() == null)
        {
            if (requestedGift.signum() != 0)
                throw new IllegalArgumentException("gift quantity requires a campaign policy");
            item.setGiftQuantity(BigDecimal.ZERO);
            return;
        }
        // Reuse the policy service's scoped query contract through its domain object.
        MemCampaignPolicy policy = campaignPolicyService.selectPolicyById(scopedPolicy(order, item.getPolicyId()));
        if (policy == null || policy.getPackages() == null || policy.getPackages().isEmpty())
            throw new IllegalArgumentException("campaign policy does not exist or is not available");
        if (!item.getProductId().equals(policy.getProductId()))
            throw new IllegalArgumentException("purchase product does not match campaign policy");
        MemCampaignPolicyPackage matched = policy.getPackages().stream()
                .filter(p -> item.getPackageId() != null && item.getPackageId().equals(p.getPackageId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("campaign package is invalid"));
        if (item.getPurchaseQuantity().compareTo(matched.getPurchaseQuantity()) != 0)
            throw new IllegalArgumentException("purchase quantity does not match campaign package");
        item.setPolicyVersion(policy.getVersion());
        item.setPackageNameSnapshot(matched.getPackageName());
        item.setGiftQuantity(matched.getGiftQuantity() == null ? BigDecimal.ZERO : matched.getGiftQuantity());
        if (matched.getPackagePrice() != null)
        {
            // package_price is the total amount for the fixed package, not a unit price.
            item.setItemAmount(matched.getPackagePrice().setScale(2, RoundingMode.HALF_UP));
        }
    }

    private MemCampaignPolicy scopedPolicy(MemPurchaseOrder order, Long policyId)
    {
        MemCampaignPolicy query = new MemCampaignPolicy();
        query.setPolicyId(policyId);
        query.setTenantId(order.getTenantId());
        query.setDeptId(order.getDeptId());
        return query;
    }

    private void validate(MemPurchaseOrder order)
    {
        if (order == null || order.getTenantId() == null || order.getDeptId() == null
                || order.getItems() == null || order.getItems().isEmpty())
        {
            throw new IllegalArgumentException("tenant, department and purchase items are required");
        }
        if ("MEMBER".equals(order.getCustomerType()) && order.getMemberId() == null)
        {
            throw new IllegalArgumentException("member purchase requires member id");
        }
        String configuredMode = identityPolicyService.resolveMode(order.getTenantId(), order.getDeptId());
        String mode = order.getIdentityMode() == null ? configuredMode : order.getIdentityMode();
        if ("ANONYMOUS".equals(mode) && !identityPolicyService.allowsAnonymous(order.getTenantId(), order.getDeptId()))
            throw new IllegalArgumentException("anonymous customer is disabled for this store");
        MemberIdentityResolutionValidator.validate(order.getCustomerName(), order.getCustomerPhone(),
                order.getMemberId() == null ? null : order.getMemberId().toString(), mode);
        order.setIdentityMode(mode);
        if ("WALK_IN".equals(order.getCustomerType()) && order.getPaidAmount() != null
                && order.getPaidAmount().signum() < 0)
        {
            throw new IllegalArgumentException("walk-in paid amount must not be negative");
        }
        for (MemPurchaseItem item : order.getItems())
        {
            if (item.getProductId() == null || item.getPurchaseQuantity() == null
                    || item.getPurchaseQuantity().signum() <= 0
                    || (item.getUnitPrice() != null && item.getUnitPrice().signum() < 0))
            {
                throw new IllegalArgumentException("purchase item product, quantity and price are invalid");
            }
        }
    }
}
