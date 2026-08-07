package com.junsong.member.mapper;

import java.util.List;
import com.junsong.member.domain.MemPurchaseItem;
import com.junsong.member.domain.MemPurchaseOrder;
import com.junsong.member.domain.MemPurchasePayment;
import com.junsong.member.domain.MemPurchaseDelivery;
import java.util.Map;

public interface MemPurchaseMapper
{
    String selectProductNameById(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                                 @org.apache.ibatis.annotations.Param("deptId") Long deptId,
                                 @org.apache.ibatis.annotations.Param("productId") Long productId);
    java.math.BigDecimal selectProductSalePriceById(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                                                    @org.apache.ibatis.annotations.Param("deptId") Long deptId,
                                                    @org.apache.ibatis.annotations.Param("productId") Long productId);
    MemPurchaseOrder selectPurchaseById(MemPurchaseOrder query);
    List<MemPurchasePayment> selectPaymentsByPurchaseId(@org.apache.ibatis.annotations.Param("purchaseId") Long purchaseId,
                                                        @org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                                                        @org.apache.ibatis.annotations.Param("deptId") Long deptId);
    List<MemPurchaseDelivery> selectDeliveriesByPurchaseId(@org.apache.ibatis.annotations.Param("purchaseId") Long purchaseId,
                                                           @org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                                                           @org.apache.ibatis.annotations.Param("deptId") Long deptId);
    MemPurchaseOrder selectPurchaseForUpdate(MemPurchasePayment payment);
    MemPurchaseOrder selectPurchaseOrderForUpdate(MemPurchaseOrder query);
    List<MemPurchaseOrder> selectPurchaseList(MemPurchaseOrder order);
    Map<String, Object> selectPurchaseStatistics(MemPurchaseOrder query);
    int insertPurchase(MemPurchaseOrder order);
    int updatePurchaseBasic(MemPurchaseOrder order);
    int updatePurchaseItem(MemPurchaseItem item);
    int updateOrderAmount(@org.apache.ibatis.annotations.Param("purchaseId") Long purchaseId,
                          @org.apache.ibatis.annotations.Param("totalAmount") java.math.BigDecimal totalAmount);
    int insertPurchaseItem(MemPurchaseItem item);
    int insertPurchasePayment(MemPurchasePayment payment);
    MemPurchasePayment selectPurchasePaymentForUpdate(MemPurchasePayment payment);
    int updatePurchasePayment(MemPurchasePayment payment);
    int updatePaymentSnapshot(@org.apache.ibatis.annotations.Param("purchaseId") Long purchaseId,
                              @org.apache.ibatis.annotations.Param("paidAmount") java.math.BigDecimal paidAmount,
                              @org.apache.ibatis.annotations.Param("receivableAmount") java.math.BigDecimal receivableAmount,
                              @org.apache.ibatis.annotations.Param("paymentStatus") String paymentStatus);
    int cancelPurchase(MemPurchaseOrder query);
    int bindPurchaseMember(MemPurchaseOrder query);
    MemPurchaseItem selectPurchaseItemForUpdate(MemPurchaseDelivery delivery);
    int insertPurchaseDelivery(MemPurchaseDelivery delivery);
    MemPurchaseDelivery selectPurchaseDeliveryForUpdate(MemPurchaseDelivery delivery);
    int updatePurchaseDelivery(MemPurchaseDelivery delivery);
    int updateDeliverySnapshot(@org.apache.ibatis.annotations.Param("itemId") Long itemId,
                               @org.apache.ibatis.annotations.Param("deliveredQuantity") java.math.BigDecimal deliveredQuantity,
                               @org.apache.ibatis.annotations.Param("deliveredSaleQuantity") java.math.BigDecimal deliveredSaleQuantity,
                               @org.apache.ibatis.annotations.Param("deliveredGiftQuantity") java.math.BigDecimal deliveredGiftQuantity,
                               @org.apache.ibatis.annotations.Param("remainingQuantity") java.math.BigDecimal remainingQuantity);
    int updateDeliveryOrderStatus(Long purchaseId);
    int updatePurchaseAfterReturn(@org.apache.ibatis.annotations.Param("purchaseId") Long purchaseId,
                                  @org.apache.ibatis.annotations.Param("totalAmount") java.math.BigDecimal totalAmount,
                                  @org.apache.ibatis.annotations.Param("paidAmount") java.math.BigDecimal paidAmount,
                                  @org.apache.ibatis.annotations.Param("paymentStatus") String paymentStatus);
}
