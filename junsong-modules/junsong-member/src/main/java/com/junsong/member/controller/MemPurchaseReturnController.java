package com.junsong.member.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.MemPurchaseReturn;
import com.junsong.member.service.IMemberPurchaseReturnService;

@RestController
@RequestMapping("/purchase-return")
public class MemPurchaseReturnController extends BaseController
{
    private final IMemberPurchaseReturnService returnService;
    public MemPurchaseReturnController(IMemberPurchaseReturnService returnService){this.returnService=returnService;}

    @RequiresPermissions("member:purchaseReturn:list")
    @GetMapping("/list")
    public TableDataInfo list(MemPurchaseReturn query){query.setTenantId(TenantContext.getTenantId());query.setDeptId(SecurityUtils.getDeptId());startPage();return getDataTable(returnService.selectReturnList(query));}

    @RequiresPermissions("member:purchaseReturn:query")
    @GetMapping("/{returnId}")
    public AjaxResult detail(@PathVariable Long returnId){MemPurchaseReturn q=new MemPurchaseReturn();q.setReturnId(returnId);q.setTenantId(TenantContext.getTenantId());q.setDeptId(SecurityUtils.getDeptId());return AjaxResult.success(returnService.selectReturnById(q));}

    @RequiresPermissions("member:purchaseReturn:add")
    @Log(title="会员购买退货",businessType=BusinessType.INSERT)
    @Idempotent(scene="member:purchase-return:create")
    @PostMapping
    public AjaxResult create(@RequestBody MemPurchaseReturn value){value.setTenantId(TenantContext.getTenantId());value.setDeptId(SecurityUtils.getDeptId());value.setCreateBy(SecurityUtils.getUsername());return returnService.createReturn(value)==1?AjaxResult.success(value):AjaxResult.error("退货单创建失败");}

    @RequiresPermissions("member:purchaseReturn:edit")
    @Log(title="编辑会员购买退货",businessType=BusinessType.UPDATE)
    @PutMapping("/{returnId}")
    public AjaxResult update(@PathVariable Long returnId, @RequestBody MemPurchaseReturn value){value.setReturnId(returnId);value.setTenantId(TenantContext.getTenantId());value.setDeptId(SecurityUtils.getDeptId());value.setUpdateBy(SecurityUtils.getUsername());return returnService.updateReturn(value)==1?AjaxResult.success():AjaxResult.error("退货单更新失败，请刷新后重试");}

    @RequiresPermissions("member:purchaseReturn:complete")
    @Log(title="完成会员购买退货",businessType=BusinessType.UPDATE)
    @Idempotent(scene="member:purchase-return:complete")
    @PutMapping("/{returnId}/complete")
    public AjaxResult complete(@PathVariable Long returnId){MemPurchaseReturn value=new MemPurchaseReturn();value.setReturnId(returnId);value.setTenantId(TenantContext.getTenantId());value.setDeptId(SecurityUtils.getDeptId());value.setUpdateBy(SecurityUtils.getUsername());return returnService.completeReturn(value)==1?AjaxResult.success():AjaxResult.error("退货单完成失败，请刷新后重试");}
}
