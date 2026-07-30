package com.junsong.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.system.domain.vo.WorkbenchTaskVO;
import com.junsong.system.service.ISystemWorkbenchService;
import com.junsong.system.service.ISystemWorkbenchNotifierService;

/**
 * 统一工作台Controller。
 * 聚合财务、会员、系统、库存健康待办任务的统一入口。
 *
 * @author junsong
 */
@RestController
@RequestMapping("/workbench")
public class SystemWorkbenchController {

    @Autowired
    private ISystemWorkbenchService systemWorkbenchService;

    @Autowired
    private ISystemWorkbenchNotifierService notifierService;

    /**
     * 查询工作台聚合任务。
     */
    @RequiresPermissions("system:workbench:tasks")
    @GetMapping("/tasks")
    public AjaxResult tasks() {
        List<WorkbenchTaskVO> tasks = systemWorkbenchService.aggregateTasks();
        return AjaxResult.success(tasks);
    }

    /**
     * 触发高优先级任务通知发送（R7-C）。
     * 扫描工作台 HIGH severity 任务，按用户授权去重写入通知中心。
     */
    @RequiresPermissions("system:workbench:notify")
    @Idempotent(scene = "system:workbench:notify-high")
    @PostMapping("/notify-high")
    public AjaxResult notifyHigh() {
        int count = notifierService.notifyHighPriorityTasks();
        return AjaxResult.success("已发送 " + count + " 条通知", count);
    }
}