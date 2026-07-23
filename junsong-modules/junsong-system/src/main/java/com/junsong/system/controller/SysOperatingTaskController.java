package com.junsong.system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.system.domain.SysOperatingTask;
import com.junsong.system.domain.SysOperatingTaskLog;
import com.junsong.system.service.ISysOperatingTaskService;

/**
 * 经营任务 Controller
 *
 * 端点：list / detail / create / claim / complete / reject / reopen / logs / pendingCount
 * 权限：system:operatingTask:list/claim/complete/reject/reopen
 *
 * @author junsong
 */
@RestController
@RequestMapping("/operatingTask")
public class SysOperatingTaskController extends BaseController
{
    @Autowired
    private ISysOperatingTaskService operatingTaskService;

    /**
     * 查询经营任务列表（分页）
     */
    @RequiresPermissions("system:operatingTask:list")
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String sourceModule,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String priority)
    {
        startPage();
        Map<String, Object> params = new HashMap<>();
        if (status != null && !status.isEmpty())
        {
            params.put("status", status);
        }
        if (assigneeId != null)
        {
            params.put("assigneeId", assigneeId);
        }
        if (sourceModule != null && !sourceModule.isEmpty())
        {
            params.put("sourceModule", sourceModule);
        }
        if (sourceType != null && !sourceType.isEmpty())
        {
            params.put("sourceType", sourceType);
        }
        if (priority != null && !priority.isEmpty())
        {
            params.put("priority", priority);
        }
        List<SysOperatingTask> list = operatingTaskService.selectOperatingTaskList(params);
        return getDataTable(list);
    }

    /**
     * 查询经营任务详情
     */
    @RequiresPermissions("system:operatingTask:list")
    @GetMapping("/{taskId}")
    public AjaxResult getInfo(@PathVariable Long taskId)
    {
        SysOperatingTask task = operatingTaskService.selectOperatingTaskById(taskId);
        if (task == null)
        {
            return AjaxResult.error("任务不存在或无权访问");
        }
        return AjaxResult.success(task);
    }

    /**
     * 幂等创建经营任务（内部调用，复用 list 权限）
     */
    @RequiresPermissions("system:operatingTask:list")
    @PostMapping("/create")
    public AjaxResult create(@RequestBody SysOperatingTask task)
    {
        SysOperatingTask result = operatingTaskService.createOrUpdateTask(task);
        return AjaxResult.success(result);
    }

    /**
     * 认领任务
     */
    @RequiresPermissions("system:operatingTask:claim")
    @PutMapping("/claim/{taskId}")
    public AjaxResult claim(@PathVariable Long taskId)
    {
        operatingTaskService.claimTask(taskId);
        return AjaxResult.success("任务认领成功");
    }

    /**
     * 完成任务
     */
    @RequiresPermissions("system:operatingTask:complete")
    @PutMapping("/complete/{taskId}")
    public AjaxResult complete(@PathVariable Long taskId, @RequestBody Map<String, String> body)
    {
        String handlerNote = body.get("handlerNote");
        operatingTaskService.completeTask(taskId, handlerNote);
        return AjaxResult.success("任务已完成");
    }

    /**
     * 驳回任务
     */
    @RequiresPermissions("system:operatingTask:reject")
    @PutMapping("/reject/{taskId}")
    public AjaxResult reject(@PathVariable Long taskId, @RequestBody Map<String, String> body)
    {
        String rejectReason = body.get("rejectReason");
        operatingTaskService.rejectTask(taskId, rejectReason);
        return AjaxResult.success("任务已驳回");
    }

    /**
     * 重开任务
     */
    @RequiresPermissions("system:operatingTask:reopen")
    @PutMapping("/reopen/{taskId}")
    public AjaxResult reopen(@PathVariable Long taskId, @RequestBody Map<String, String> body)
    {
        String reason = body.get("reason");
        operatingTaskService.reopenTask(taskId, reason);
        return AjaxResult.success("任务已重开");
    }

    /**
     * 查询任务操作日志
     */
    @RequiresPermissions("system:operatingTask:list")
    @GetMapping("/logs/{taskId}")
    public AjaxResult logs(@PathVariable Long taskId)
    {
        List<SysOperatingTaskLog> logs = operatingTaskService.selectTaskLogs(taskId);
        return AjaxResult.success(logs);
    }

    /**
     * 当前用户待办计数
     */
    @RequiresPermissions("system:operatingTask:list")
    @GetMapping("/pendingCount")
    public AjaxResult pendingCount()
    {
        int count = operatingTaskService.countPendingTasks();
        return AjaxResult.success(count);
    }
}
