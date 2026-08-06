package com.junsong.member.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.MemPurchaseDelivery;
import com.junsong.member.service.IMemberPurchaseDeliveryService;

@RestController
@RequestMapping("/purchase")
public class MemPurchaseDeliveryController
{
    private final IMemberPurchaseDeliveryService deliveryService;

    public MemPurchaseDeliveryController(IMemberPurchaseDeliveryService deliveryService)
    {
        this.deliveryService = deliveryService;
    }

    @RequiresPermissions("member:purchase:delivery")
    @Log(title = "会员购买领取", businessType = BusinessType.INSERT)
    @Idempotent(scene = "member:purchase:delivery")
    @PostMapping("/{purchaseId}/delivery")
    public AjaxResult deliver(@PathVariable Long purchaseId, @RequestBody MemPurchaseDelivery delivery)
    {
        delivery.setPurchaseId(purchaseId);
        delivery.setTenantId(TenantContext.getTenantId());
        delivery.setDeptId(SecurityUtils.getDeptId());
        delivery.setOperatorName(SecurityUtils.getUsername());
        return deliveryService.deliver(delivery) == 1
                ? AjaxResult.success("领取成功") : AjaxResult.error("领取失败");
    }

    @RequiresPermissions("member:purchase:edit")
    @Log(title = "会员购买领取编辑", businessType = BusinessType.UPDATE)
    @PutMapping("/{purchaseId}/delivery/{deliveryId}")
    public AjaxResult update(@PathVariable Long purchaseId, @PathVariable Long deliveryId, @RequestBody MemPurchaseDelivery delivery)
    {
        delivery.setPurchaseId(purchaseId); delivery.setDeliveryId(deliveryId);
        delivery.setTenantId(TenantContext.getTenantId()); delivery.setDeptId(SecurityUtils.getDeptId());
        return deliveryService.update(delivery) == 1 ? AjaxResult.success("领取记录已保存") : AjaxResult.error("领取记录未保存");
    }
}
