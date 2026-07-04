package com.junsong.system.controller;

import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.system.domain.vo.*;
import com.junsong.system.service.ISysActionCenterService;
import com.junsong.system.service.ISysActionTouchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/action-center")
public class SysActionCenterController {
    @Autowired
    private ISysActionCenterService actionCenterService;
    @Autowired
    private ISysActionTouchService actionTouchService;

    @RequiresPermissions("system:action-center:view")
    @GetMapping("/list")
    public AjaxResult list(ActionCenterQueryParams params) {
        return AjaxResult.success(actionCenterService.listActions(params));
    }

    @RequiresPermissions("system:action-center:view")
    @GetMapping("/calendar")
    public AjaxResult calendar(ActionCenterQueryParams params) {
        return AjaxResult.success(actionCenterService.getCalendar(params));
    }

    @RequiresPermissions("system:action-center:touch")
    @PostMapping("/{actionId}/touch")
    public AjaxResult touch(@PathVariable String actionId, @RequestBody ActionTouchRequestVO request) {
        ActionTouchResultVO result = actionTouchService.touch(actionId, request);
        if ("FAILED".equals(result.getTouchStatus())) {
            return AjaxResult.error(result.getMessage(), result);
        }
        return AjaxResult.success(result);
    }
}
