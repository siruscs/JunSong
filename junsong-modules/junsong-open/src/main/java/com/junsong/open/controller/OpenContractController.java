package com.junsong.open.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.open.domain.OpenContract;
import com.junsong.open.service.IOpenContractService;

/**
 * 开放平台合约管理 Controller
 *
 * @author junsong
 */
@RestController
@RequestMapping("/contract")
public class OpenContractController extends BaseController
{
    @Autowired
    private IOpenContractService openContractService;

    @GetMapping("/list")
    @RequiresPermissions("open:contract:list")
    public TableDataInfo list(OpenContract openContract)
    {
        startPage();
        List<OpenContract> list = openContractService.selectOpenContractList(openContract);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("open:contract:query")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(openContractService.selectOpenContractById(id));
    }

    @PostMapping
    @RequiresPermissions("open:contract:add")
    @Idempotent(scene = "open:contract:add")
    public AjaxResult add(@RequestBody OpenContract openContract)
    {
        return toAjax(openContractService.insertOpenContract(openContract));
    }

    @PutMapping
    @RequiresPermissions("open:contract:edit")
    @Idempotent(scene = "open:contract:edit")
    public AjaxResult edit(@RequestBody OpenContract openContract)
    {
        return toAjax(openContractService.updateOpenContract(openContract));
    }

    @PutMapping("/activate/{id}")
    @RequiresPermissions("open:contract:edit")
    @Idempotent(scene = "open:contract:activate", highRisk = true)
    public AjaxResult activate(@PathVariable("id") Long id)
    {
        return toAjax(openContractService.activateContract(id));
    }

    @PutMapping("/terminate/{id}")
    @RequiresPermissions("open:contract:edit")
    @Idempotent(scene = "open:contract:terminate", highRisk = true, ttlSeconds = 2592000)
    public AjaxResult terminate(@PathVariable("id") Long id)
    {
        return toAjax(openContractService.terminateContract(id));
    }

    @DeleteMapping("/{ids}")
    @RequiresPermissions("open:contract:remove")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(openContractService.deleteOpenContractByIds(ids));
    }
}
