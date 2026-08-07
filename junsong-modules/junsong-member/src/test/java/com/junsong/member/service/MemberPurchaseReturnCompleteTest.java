package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.junsong.member.domain.MemPurchaseItem;
import com.junsong.member.domain.MemPurchaseOrder;
import com.junsong.member.domain.MemPurchaseReturn;
import com.junsong.member.domain.MemPurchaseReturnItem;
import com.junsong.member.mapper.MemPurchaseMapper;
import com.junsong.member.mapper.MemPurchaseReturnMapper;
import com.junsong.member.service.impl.MemberPurchaseReturnServiceImpl;

/**
 * 退货完成流程测试：验证原购买单金额/数量更新、已领取商品退货、跨周期、积分成长值核减。
 */
class MemberPurchaseReturnCompleteTest
{
    private MemPurchaseReturnMapper returnMapper = Mockito.mock(MemPurchaseReturnMapper.class);
    private MemPurchaseMapper purchaseMapper = Mockito.mock(MemPurchaseMapper.class);
    private IMemberGrowthService growthService = Mockito.mock(IMemberGrowthService.class);
    private MemberPurchaseReturnServiceImpl service = new MemberPurchaseReturnServiceImpl(returnMapper, purchaseMapper, growthService);

    @BeforeEach
    void setUp()
    {
        Mockito.reset(returnMapper, purchaseMapper, growthService);
        when(purchaseMapper.updatePurchaseItem(any())).thenReturn(1);
        when(purchaseMapper.updatePurchaseAfterReturn(anyLong(), any(), any(), anyString())).thenReturn(1);
    }

    /** 构造一个含10正品+3赠品、已付款4899、已领取5正品的购买单。 */
    private MemPurchaseOrder buildPurchase()
    {
        MemPurchaseOrder order = new MemPurchaseOrder();
        order.setPurchaseId(100L);
        order.setTenantId(1L);
        order.setDeptId(10L);
        order.setPeriodId(50L);
        order.setCustomerType("MEMBER");
        order.setMemberId(88L);
        order.setCustomerName("张三");
        order.setTotalAmount(new BigDecimal("4899.00"));
        order.setPaidAmount(new BigDecimal("4899.00"));
        order.setReceivableAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        order.setPaymentStatus("2");
        order.setDeliveryStatus("1");
        order.setOrderStatus("1");

        MemPurchaseItem item = new MemPurchaseItem();
        item.setItemId(200L);
        item.setPurchaseId(100L);
        item.setTenantId(1L);
        item.setDeptId(10L);
        item.setProductId(300L);
        item.setProductNameSnapshot("测试商品");
        item.setPurchaseQuantity(new BigDecimal("10.000"));
        item.setGiftQuantity(new BigDecimal("3.000"));
        item.setTotalQuantity(new BigDecimal("13.000"));
        item.setDeliveredQuantity(new BigDecimal("5.000"));
        item.setDeliveredSaleQuantity(new BigDecimal("5.000"));
        item.setDeliveredGiftQuantity(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
        item.setRemainingQuantity(new BigDecimal("8.000"));
        item.setUnitPrice(new BigDecimal("489.90"));
        item.setItemAmount(new BigDecimal("4899.00"));
        order.setItems(Collections.singletonList(item));
        return order;
    }

    /** 构造一个退5正品+1赠品的退货单(草稿状态)。 */
    private MemPurchaseReturn buildReturn()
    {
        MemPurchaseReturn ret = new MemPurchaseReturn();
        ret.setReturnId(500L);
        ret.setTenantId(1L);
        ret.setDeptId(10L);
        ret.setPurchaseId(100L);
        ret.setOriginalPeriodId(50L);
        ret.setReturnPeriodId(55L);
        ret.setCustomerType("MEMBER");
        ret.setMemberId(88L);
        ret.setCustomerName("张三");
        ret.setStatus("DRAFT");
        ret.setRefundAmount(new BigDecimal("2261.08"));
        ret.setVersion(0L);
        ret.setUpdateBy("admin");

        MemPurchaseReturnItem retItem = new MemPurchaseReturnItem();
        retItem.setReturnId(500L);
        retItem.setPurchaseId(100L);
        retItem.setItemId(200L);
        retItem.setProductId(300L);
        retItem.setProductNameSnapshot("测试商品");
        retItem.setReturnSaleQuantity(new BigDecimal("5.000"));
        retItem.setReturnGiftQuantity(new BigDecimal("1.000"));
        retItem.setReturnTotalQuantity(new BigDecimal("6.000"));
        retItem.setRefundUnitPrice(new BigDecimal("376.85"));
        retItem.setRefundAmount(new BigDecimal("2261.08"));
        ret.setItems(Collections.singletonList(retItem));
        return ret;
    }

    @Test
    void completeReturnUpdatesOriginalPurchaseAmounts()
    {
        MemPurchaseReturn ret = buildReturn();
        when(returnMapper.selectReturnForUpdate(any())).thenReturn(ret);
        when(purchaseMapper.selectPurchaseOrderForUpdate(any())).thenReturn(buildPurchase());
        when(purchaseMapper.selectPurchaseItemForUpdate(any())).thenReturn(buildPurchase().getItems().get(0));
        when(returnMapper.completeReturn(any())).thenReturn(1);

        service.completeReturn(ret);

        // 验证原购买单金额被更新：total和paid各减去refundAmount
        verify(purchaseMapper).updatePurchaseAfterReturn(eq(100L),
                eq(new BigDecimal("2637.92")),  // 4899.00 - 2261.08
                eq(new BigDecimal("2637.92")),  // 4899.00 - 2261.08
                anyString());
    }

    @Test
    void completeReturnUpdatesOriginalPurchaseItemQuantities()
    {
        MemPurchaseReturn ret = buildReturn();
        when(returnMapper.selectReturnForUpdate(any())).thenReturn(ret);
        when(purchaseMapper.selectPurchaseOrderForUpdate(any())).thenReturn(buildPurchase());
        when(purchaseMapper.selectPurchaseItemForUpdate(any())).thenReturn(buildPurchase().getItems().get(0));
        when(returnMapper.completeReturn(any())).thenReturn(1);

        service.completeReturn(ret);

        // 验证原购买明细数量被更新
        verify(purchaseMapper).updatePurchaseItem(Mockito.argThat(item ->
                item.getItemId().equals(200L)
                        && item.getPurchaseQuantity().compareTo(new BigDecimal("5.000")) == 0  // 10-5
                        && item.getGiftQuantity().compareTo(new BigDecimal("2.000")) == 0      // 3-1
                        && item.getTotalQuantity().compareTo(new BigDecimal("7.000")) == 0));   // 5+2
    }

    @Test
    void completeReturnReversesGrowthForMemberPurchase()
    {
        MemPurchaseReturn ret = buildReturn();
        when(returnMapper.selectReturnForUpdate(any())).thenReturn(ret);
        when(purchaseMapper.selectPurchaseOrderForUpdate(any())).thenReturn(buildPurchase());
        when(purchaseMapper.selectPurchaseItemForUpdate(any())).thenReturn(buildPurchase().getItems().get(0));
        when(returnMapper.completeReturn(any())).thenReturn(1);

        service.completeReturn(ret);

        // 验证会员积分/成长值被按比例核减
        verify(growthService).reversePurchaseRewardByReturn(
                eq(88L), eq(100L), eq(500L),
                eq(new BigDecimal("2261.08")),
                eq(new BigDecimal("4899.00")),
                anyString());
    }

    @Test
    void completeReturnDoesNotReverseGrowthForWalkInPurchase()
    {
        MemPurchaseReturn ret = buildReturn();
        ret.setCustomerType("WALK_IN");
        ret.setMemberId(null);
        MemPurchaseOrder purchase = buildPurchase();
        purchase.setCustomerType("WALK_IN");
        purchase.setMemberId(null);

        when(returnMapper.selectReturnForUpdate(any())).thenReturn(ret);
        when(purchaseMapper.selectPurchaseOrderForUpdate(any())).thenReturn(purchase);
        when(purchaseMapper.selectPurchaseItemForUpdate(any())).thenReturn(purchase.getItems().get(0));
        when(returnMapper.completeReturn(any())).thenReturn(1);

        service.completeReturn(ret);

        verify(growthService, never()).reversePurchaseRewardByReturn(
                anyLong(), anyLong(), anyLong(), any(), any(), anyString());
    }

    @Test
    void completeReturnRejectsNonDraftStatus()
    {
        MemPurchaseReturn ret = buildReturn();
        ret.setStatus("COMPLETED");
        when(returnMapper.selectReturnForUpdate(any())).thenReturn(ret);

        assertThrows(IllegalArgumentException.class, () -> service.completeReturn(ret));
        verify(purchaseMapper, never()).updatePurchaseAfterReturn(anyLong(), any(), any(), anyString());
    }

    @Test
    void completeReturnHandlesAlreadyDeliveredItems()
    {
        // 退货5正品，其中5正品已全部领取，退的是已领取的商品
        MemPurchaseReturn ret = buildReturn();
        ret.getItems().get(0).setReturnSaleQuantity(new BigDecimal("5.000"));
        ret.getItems().get(0).setReturnGiftQuantity(new BigDecimal("0.000"));
        ret.getItems().get(0).setReturnTotalQuantity(new BigDecimal("5.000"));

        MemPurchaseOrder purchase = buildPurchase();
        // 全部正品已领取
        purchase.getItems().get(0).setDeliveredQuantity(new BigDecimal("10.000"));
        purchase.getItems().get(0).setDeliveredSaleQuantity(new BigDecimal("10.000"));
        purchase.getItems().get(0).setDeliveredGiftQuantity(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
        purchase.getItems().get(0).setRemainingQuantity(new BigDecimal("3.000")); // 13-10

        when(returnMapper.selectReturnForUpdate(any())).thenReturn(ret);
        when(purchaseMapper.selectPurchaseOrderForUpdate(any())).thenReturn(purchase);
        when(purchaseMapper.selectPurchaseItemForUpdate(any())).thenReturn(purchase.getItems().get(0));
        when(returnMapper.completeReturn(any())).thenReturn(1);

        service.completeReturn(ret);

        // 验证已领取数量也被核减：delivered_sale 10-5=5
        verify(purchaseMapper).updatePurchaseItem(Mockito.argThat(item ->
                item.getDeliveredSaleQuantity().compareTo(new BigDecimal("5.000")) == 0
                        && item.getPurchaseQuantity().compareTo(new BigDecimal("5.000")) == 0));
    }

    @Test
    void completeReturnSupportsCrossPeriodReturn()
    {
        MemPurchaseReturn ret = buildReturn();
        ret.setOriginalPeriodId(50L);
        ret.setReturnPeriodId(55L); // 不同周期

        when(returnMapper.selectReturnForUpdate(any())).thenReturn(ret);
        when(purchaseMapper.selectPurchaseOrderForUpdate(any())).thenReturn(buildPurchase());
        when(purchaseMapper.selectPurchaseItemForUpdate(any())).thenReturn(buildPurchase().getItems().get(0));
        when(returnMapper.completeReturn(any())).thenReturn(1);

        int result = service.completeReturn(ret);
        assertEquals(1, result);
        // 跨周期退货应正常完成，不报错
        verify(returnMapper).completeReturn(any());
    }
}
