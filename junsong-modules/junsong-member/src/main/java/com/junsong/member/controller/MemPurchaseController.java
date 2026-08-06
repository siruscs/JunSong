package com.junsong.member.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.MemPurchaseOrder;
import com.junsong.member.service.IMemberPurchaseService;

@RestController
@RequestMapping("/purchase")
public class MemPurchaseController extends BaseController
{
    private final IMemberPurchaseService purchaseService;

    public MemPurchaseController(IMemberPurchaseService purchaseService)
    {
        this.purchaseService = purchaseService;
    }

    @RequiresPermissions("member:purchase:list")
    @GetMapping("/list")
    public TableDataInfo list(MemPurchaseOrder order)
    {
        order.setTenantId(TenantContext.getTenantId());
        order.setDeptId(SecurityUtils.getDeptId());
        startPage();
        List<MemPurchaseOrder> rows = purchaseService.selectPurchaseList(order);
        return getDataTable(rows);
    }

    @RequiresPermissions("member:purchase:export")
    @Log(title = "会员购买记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemPurchaseOrder order)
    {
        order.setTenantId(TenantContext.getTenantId()); order.setDeptId(SecurityUtils.getDeptId());
        ExcelUtil<MemPurchaseOrder> util = new ExcelUtil<>(MemPurchaseOrder.class);
        util.exportExcel(response, purchaseService.selectPurchaseList(order), "会员购买记录");
    }

    @RequiresPermissions("member:purchase:list")
    @GetMapping("/statistics")
    public AjaxResult statistics(MemPurchaseOrder query)
    {
        query.setTenantId(TenantContext.getTenantId());
        query.setDeptId(SecurityUtils.getDeptId());
        return AjaxResult.success(purchaseService.selectPurchaseStatistics(query));
    }

    @RequiresPermissions("member:purchase:query")
    @GetMapping("/{purchaseId}")
    public AjaxResult detail(@PathVariable Long purchaseId)
    {
        MemPurchaseOrder query = new MemPurchaseOrder();
        query.setPurchaseId(purchaseId);
        query.setTenantId(TenantContext.getTenantId());
        query.setDeptId(SecurityUtils.getDeptId());
        return AjaxResult.success(purchaseService.selectPurchaseById(query));
    }

    @RequiresPermissions("member:purchase:add")
    @Log(title = "会员购买单", businessType = BusinessType.INSERT)
    @Idempotent(scene = "member:purchase:create")
    @PostMapping
    public AjaxResult create(@RequestBody MemPurchaseOrder order)
    {
        order.setTenantId(TenantContext.getTenantId());
        order.setDeptId(SecurityUtils.getDeptId());
        order.setCreateBy(SecurityUtils.getUsername());
        return purchaseService.createPurchase(order) == 1
                ? AjaxResult.success(order)
                : AjaxResult.error("会员购买单创建失败");
    }

    @RequiresPermissions("member:purchase:edit")
    @Log(title = "会员购买单编辑", businessType = BusinessType.UPDATE)
    @PutMapping("/{purchaseId}")
    public AjaxResult edit(@PathVariable Long purchaseId, @RequestBody MemPurchaseOrder order)
    {
        order.setPurchaseId(purchaseId);
        order.setTenantId(TenantContext.getTenantId());
        order.setDeptId(SecurityUtils.getDeptId());
        order.setUpdateBy(SecurityUtils.getUsername());
        return purchaseService.updatePurchaseBasic(order) == 1
                ? AjaxResult.success("购买单已保存") : AjaxResult.error("购买单未保存");
    }

    @RequiresPermissions("member:purchase:cancel")
    @Log(title = "会员购买单作废", businessType = BusinessType.UPDATE)
    @PutMapping("/{purchaseId}/cancel")
    public AjaxResult cancel(@PathVariable Long purchaseId)
    {
        MemPurchaseOrder query = new MemPurchaseOrder();
        query.setPurchaseId(purchaseId);
        query.setTenantId(TenantContext.getTenantId());
        query.setDeptId(SecurityUtils.getDeptId());
        query.setUpdateBy(SecurityUtils.getUsername());
        return purchaseService.cancelPurchase(query) == 1 ? AjaxResult.success("购买单已作废") : AjaxResult.error("购买单未作废");
    }

    @RequiresPermissions("member:purchase:bind")
    @Log(title = "散客购买单绑定会员", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "member:purchase:bind")
    @PutMapping("/{purchaseId}/bind-member/{memberId}")
    public AjaxResult bindMember(@PathVariable Long purchaseId, @PathVariable Long memberId)
    {
        return purchaseService.bindPurchaseMember(purchaseId, TenantContext.getTenantId(),
                SecurityUtils.getDeptId(), memberId, SecurityUtils.getUsername()) == 1
                ? AjaxResult.success("购买单已绑定会员") : AjaxResult.error("购买单绑定会员失败");
    }
}
