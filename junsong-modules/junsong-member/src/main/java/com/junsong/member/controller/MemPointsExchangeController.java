package com.junsong.member.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.member.domain.MemPointsExchange;
import com.junsong.member.domain.MemPointsRecord;
import com.junsong.member.service.IMemPointsExchangeService;
import com.junsong.member.service.IMemPointsRecordService;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;

@RestController
@RequestMapping("/pointsExchange")
public class MemPointsExchangeController extends BaseController {

    @Autowired
    private IMemPointsExchangeService memPointsExchangeService;

    @Autowired
    private IMemPointsRecordService memPointsRecordService;

    @RequiresPermissions("member:pointsExchange:list")
    @GetMapping("/list")
    public TableDataInfo list(MemPointsExchange memPointsExchange) {
        startPage();
        List<MemPointsExchange> list = memPointsExchangeService.selectMemPointsExchangeList(memPointsExchange);
        return getDataTable(list);
    }

    @RequiresPermissions("member:pointsExchange:export")
    @Log(title = "积分兑换", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemPointsExchange memPointsExchange) {
        List<MemPointsExchange> list = memPointsExchangeService.selectMemPointsExchangeList(memPointsExchange);
        ExcelUtil<MemPointsExchange> util = new ExcelUtil<MemPointsExchange>(MemPointsExchange.class);
        util.exportExcel(response, list, "积分兑换数据");
    }

    @RequiresPermissions("member:pointsExchange:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(memPointsExchangeService.selectMemPointsExchangeById(id));
    }

    @RequiresPermissions("member:pointsExchange:add")
    @Log(title = "积分兑换", businessType = BusinessType.INSERT)
    @Idempotent(scene = "member:pointsExchange:create")
    @PostMapping
    public AjaxResult add(@RequestBody MemPointsExchange memPointsExchange) {
        try
        {
            if (memPointsExchange.getDeptId() == null)
            {
                memPointsExchange.setDeptId(SecurityUtils.getDeptId());
            }
            memPointsExchangeService.exchangePoints(memPointsExchange, SecurityUtils.getUsername());
            return success();
        }
        catch (IllegalArgumentException e)
        {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("member:pointsExchange:edit")
    @Log(title = "积分兑换", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "member:points-exchange:edit")
    @PutMapping
    public AjaxResult edit(@RequestBody MemPointsExchange memPointsExchange) {
        if (!memPointsExchangeService.checkMemPointsExchangeNoUnique(memPointsExchange)) {
            return error("修改积分兑换失败，编号已存在");
        }
        memPointsExchange.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(memPointsExchangeService.updateMemPointsExchange(memPointsExchange));
    }

    @RequiresPermissions("member:pointsExchange:remove")
    @Log(title = "积分兑换", businessType = BusinessType.DELETE)
    @Idempotent(scene = "member:pointsExchange:delete")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        for (Long exchangeId : ids) {
            try {
                MemPointsExchange exchange = memPointsExchangeService.selectMemPointsExchangeById(exchangeId);
                if (exchange != null && exchange.getExchangeNo() != null) {
                    List<MemPointsRecord> records = memPointsRecordService.selectMemPointsRecordByRemark(
                        exchange.getExchangeNo());
                    for (MemPointsRecord record : records) {
                        memPointsRecordService.deleteMemPointsRecordById(record.getRecordId());
                    }
                }
            } catch (Exception e) {
            }
        }
        return toAjax(memPointsExchangeService.deleteMemPointsExchangeByIds(ids));
    }
}
