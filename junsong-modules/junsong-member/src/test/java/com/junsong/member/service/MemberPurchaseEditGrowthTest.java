package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.junsong.member.domain.MemPurchaseItem;
import com.junsong.member.domain.MemPurchaseOrder;
import com.junsong.member.mapper.MemMemberMapper;
import com.junsong.member.mapper.MemPurchaseMapper;
import com.junsong.member.service.impl.MemberPurchaseServiceImpl;

/**
 * 编辑购买单积分/成长值重算测试。
 */
class MemberPurchaseEditGrowthTest
{
    private MemPurchaseMapper purchaseMapper = Mockito.mock(MemPurchaseMapper.class);
    private MemMemberMapper memberMapper = Mockito.mock(MemMemberMapper.class);
    private IMemberIdentityPolicyService identityPolicyService = Mockito.mock(IMemberIdentityPolicyService.class);
    private IMemberCampaignPolicyService policyService = Mockito.mock(IMemberCampaignPolicyService.class);
    private IMemberGrowthService growthService = Mockito.mock(IMemberGrowthService.class);
    private MemberPurchaseServiceImpl service = new MemberPurchaseServiceImpl(
            purchaseMapper, memberMapper, identityPolicyService, policyService, growthService);

    @BeforeEach
    void setUp()
    {
        Mockito.reset(purchaseMapper, memberMapper, identityPolicyService, policyService, growthService);
    }

    private MemPurchaseOrder buildMemberPurchase(BigDecimal totalAmount)
    {
        MemPurchaseOrder order = new MemPurchaseOrder();
        order.setPurchaseId(100L);
        order.setTenantId(1L);
        order.setDeptId(10L);
        order.setCustomerType("MEMBER");
        order.setMemberId(88L);
        order.setCustomerName("张三");
        order.setTotalAmount(totalAmount);
        order.setPaidAmount(totalAmount);
        order.setOrderStatus("1");
        return order;
    }

    private MemPurchaseItem buildItem(BigDecimal unitPrice, BigDecimal qty, BigDecimal delivered)
    {
        MemPurchaseItem item = new MemPurchaseItem();
        item.setItemId(200L);
        item.setPurchaseId(100L);
        item.setTenantId(1L);
        item.setDeptId(10L);
        item.setProductId(300L);
        item.setUnitPrice(unitPrice);
        item.setPurchaseQuantity(qty);
        item.setGiftQuantity(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
        item.setDeliveredQuantity(delivered);
        item.setDeliveredSaleQuantity(delivered);
        item.setDeliveredGiftQuantity(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
        return item;
    }

    @Test
    void editMemberPurchaseWithAmountChangeReversesAndReawardsGrowth()
    {
        MemPurchaseOrder order = buildMemberPurchase(new BigDecimal("1000.00"));
        when(purchaseMapper.selectPurchaseOrderForUpdate(any())).thenReturn(order);
        when(purchaseMapper.updatePurchaseBasic(any())).thenReturn(1);
        MemPurchaseItem current = buildItem(new BigDecimal("100.00"), new BigDecimal("10.000"), BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
        when(purchaseMapper.selectPurchaseItemForUpdate(any())).thenReturn(current);
        when(purchaseMapper.updatePurchaseItem(any())).thenReturn(1);

        MemPurchaseOrder query = new MemPurchaseOrder();
        query.setPurchaseId(100L); query.setTenantId(1L); query.setDeptId(10L); query.setUpdateBy("admin");
        MemPurchaseItem editItem = new MemPurchaseItem();
        editItem.setItemId(200L);
        editItem.setUnitPrice(new BigDecimal("120.00"));
        editItem.setPurchaseQuantity(new BigDecimal("10.000"));
        query.setItems(Collections.singletonList(editItem));

        service.updatePurchaseBasic(query);

        // 验证原积分被核减并重新计算（新总额1200 != 原总额1000）
        verify(growthService).reversePurchaseReward(eq(88L), eq(100L), eq("admin"));
        verify(growthService).reawardPurchaseReward(eq(88L), any(), eq("张三"), eq(10L),
                eq(100L), eq(new BigDecimal("1200.00")), eq("admin"));
    }

    @Test
    void editMemberPurchaseWithoutAmountChangeDoesNotTouchGrowth()
    {
        MemPurchaseOrder order = buildMemberPurchase(new BigDecimal("1000.00"));
        when(purchaseMapper.selectPurchaseOrderForUpdate(any())).thenReturn(order);
        when(purchaseMapper.updatePurchaseBasic(any())).thenReturn(1);
        MemPurchaseItem current = buildItem(new BigDecimal("100.00"), new BigDecimal("10.000"), BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
        when(purchaseMapper.selectPurchaseItemForUpdate(any())).thenReturn(current);
        when(purchaseMapper.updatePurchaseItem(any())).thenReturn(1);

        MemPurchaseOrder query = new MemPurchaseOrder();
        query.setPurchaseId(100L); query.setTenantId(1L); query.setDeptId(10L); query.setUpdateBy("admin");
        MemPurchaseItem editItem = new MemPurchaseItem();
        editItem.setItemId(200L);
        editItem.setUnitPrice(new BigDecimal("100.00")); // 同价
        editItem.setPurchaseQuantity(new BigDecimal("10.000")); // 同量
        query.setItems(Collections.singletonList(editItem));

        service.updatePurchaseBasic(query);

        verify(growthService, never()).reversePurchaseReward(anyLong(), anyLong(), anyString());
        verify(growthService, never()).reawardPurchaseReward(anyLong(), any(), any(), any(), anyLong(), any(), anyString());
    }

    @Test
    void editWalkInPurchaseDoesNotTouchGrowth()
    {
        MemPurchaseOrder order = buildMemberPurchase(new BigDecimal("1000.00"));
        order.setCustomerType("WALK_IN");
        order.setMemberId(null);
        when(purchaseMapper.selectPurchaseOrderForUpdate(any())).thenReturn(order);
        when(purchaseMapper.updatePurchaseBasic(any())).thenReturn(1);
        MemPurchaseItem current = buildItem(new BigDecimal("100.00"), new BigDecimal("10.000"), BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
        when(purchaseMapper.selectPurchaseItemForUpdate(any())).thenReturn(current);
        when(purchaseMapper.updatePurchaseItem(any())).thenReturn(1);

        MemPurchaseOrder query = new MemPurchaseOrder();
        query.setPurchaseId(100L); query.setTenantId(1L); query.setDeptId(10L); query.setUpdateBy("admin");
        MemPurchaseItem editItem = new MemPurchaseItem();
        editItem.setItemId(200L);
        editItem.setUnitPrice(new BigDecimal("150.00")); // 价格变了
        editItem.setPurchaseQuantity(new BigDecimal("10.000"));
        query.setItems(Collections.singletonList(editItem));

        service.updatePurchaseBasic(query);

        verify(growthService, never()).reversePurchaseReward(anyLong(), anyLong(), anyString());
        verify(growthService, never()).reawardPurchaseReward(anyLong(), any(), any(), any(), anyLong(), any(), anyString());
    }

    @Test
    void editCancelledPurchaseIsRejected()
    {
        MemPurchaseOrder order = buildMemberPurchase(new BigDecimal("1000.00"));
        order.setOrderStatus("4"); // 已作废
        when(purchaseMapper.selectPurchaseOrderForUpdate(any())).thenReturn(order);

        MemPurchaseOrder query = new MemPurchaseOrder();
        query.setPurchaseId(100L); query.setTenantId(1L); query.setDeptId(10L); query.setUpdateBy("admin");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.updatePurchaseBasic(query));
    }
}
