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
import com.junsong.member.domain.MemPurchasePayment;
import com.junsong.member.service.IMemberPurchasePaymentService;

@RestController
@RequestMapping("/purchase")
public class MemPurchasePaymentController
{
    private final IMemberPurchasePaymentService paymentService;

    public MemPurchasePaymentController(IMemberPurchasePaymentService paymentService)
    {
        this.paymentService = paymentService;
    }

    @RequiresPermissions("member:purchase:payment")
    @Log(title = "会员购买收款", businessType = BusinessType.INSERT)
    @Idempotent(scene = "member:purchase:payment")
    @PostMapping("/{purchaseId}/payment")
    public AjaxResult receive(@PathVariable Long purchaseId, @RequestBody MemPurchasePayment payment)
    {
        payment.setPurchaseId(purchaseId);
        payment.setTenantId(TenantContext.getTenantId());
        payment.setDeptId(SecurityUtils.getDeptId());
        payment.setOperatorName(SecurityUtils.getUsername());
        return paymentService.receive(payment) == 1
                ? AjaxResult.success("收款成功") : AjaxResult.error("收款失败");
    }

    @RequiresPermissions("member:purchase:edit")
    @Log(title = "会员购买收款编辑", businessType = BusinessType.UPDATE)
    @PutMapping("/{purchaseId}/payment/{paymentId}")
    public AjaxResult update(@PathVariable Long purchaseId, @PathVariable Long paymentId, @RequestBody MemPurchasePayment payment)
    {
        payment.setPurchaseId(purchaseId); payment.setPaymentId(paymentId);
        payment.setTenantId(TenantContext.getTenantId()); payment.setDeptId(SecurityUtils.getDeptId());
        return paymentService.update(payment) == 1 ? AjaxResult.success("付款记录已保存") : AjaxResult.error("付款记录未保存");
    }
}
