package com.junsong.open.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.open.domain.OpenIsv;
import com.junsong.open.service.IOpenIsvService;

/**
 * ISV注册管理 Controller
 *
 * @author junsong
 */
@RestController
@RequestMapping("/isv")
public class OpenIsvController extends BaseController
{
    @Autowired
    private IOpenIsvService openIsvService;

    @GetMapping("/list")
    @RequiresPermissions("open:isv:list")
    public TableDataInfo list(OpenIsv openIsv)
    {
        startPage();
        List<OpenIsv> list = openIsvService.selectOpenIsvList(openIsv);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("open:isv:query")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(openIsvService.selectOpenIsvById(id));
    }

    /**
     * ISV 自助注册（公开接口，无需登录）
     */
    @PostMapping("/register")
    @Idempotent(scene = "open:isv:register", highRisk = true)
    public AjaxResult register(@RequestBody OpenIsv openIsv)
    {
        return toAjax(openIsvService.insertOpenIsv(openIsv));
    }

    @PostMapping
    @RequiresPermissions("open:isv:add")
    @Idempotent(scene = "open:isv:create")
    public AjaxResult add(@RequestBody OpenIsv openIsv)
    {
        return toAjax(openIsvService.insertOpenIsv(openIsv));
    }

    @PutMapping
    @RequiresPermissions("open:isv:edit")
    @Idempotent(scene = "open:isv:edit")
    public AjaxResult edit(@RequestBody OpenIsv openIsv)
    {
        return toAjax(openIsvService.updateOpenIsv(openIsv));
    }

    @PutMapping("/approve/{id}")
    @RequiresPermissions("open:isv:approve")
    @Idempotent(scene = "open:isv:approve", highRisk = true)
    public AjaxResult approve(@PathVariable("id") Long id)
    {
        return toAjax(openIsvService.approveIsv(id, SecurityUtils.getUsername()));
    }

    @PutMapping("/reject/{id}")
    @RequiresPermissions("open:isv:approve")
    @Idempotent(scene = "open:isv:reject", highRisk = true)
    public AjaxResult reject(@PathVariable("id") Long id, @RequestParam String rejectReason)
    {
        return toAjax(openIsvService.rejectIsv(id, rejectReason, SecurityUtils.getUsername()));
    }

    @DeleteMapping("/{ids}")
    @RequiresPermissions("open:isv:remove")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(openIsvService.deleteOpenIsvByIds(ids));
    }
}
