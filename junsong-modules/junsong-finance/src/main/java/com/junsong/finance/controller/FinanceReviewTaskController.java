package com.junsong.finance.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinanceReviewTask;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.service.IFinanceReviewTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务复盘任务Controller
 *
 * @author junsong
 */
@RestController
@RequestMapping("/review-task")
public class FinanceReviewTaskController extends BaseController {

    @Autowired
    private IFinanceReviewTaskService reviewTaskService;

    /**
     * 查询复盘任务列表（分页）
     */
    @RequiresPermissions("finance:reviewTask:list")
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) List<Long> deptIds,
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {
        startPage();
        Map<String, Object> params = new HashMap<>();
        if (status != null && !status.isEmpty()) {
            params.put("status", status);
        }
        if (title != null && !title.isEmpty()) {
            params.put("title", title);
        }
        if (severity != null && !severity.isEmpty()) {
            params.put("severity", severity);
        }
        if (taskType != null && !taskType.isEmpty()) {
            params.put("taskType", taskType);
        }
        if (deptIds != null && !deptIds.isEmpty()) {
            params.put("deptIds", deptIds);
        }
        if (periodId != null) {
            params.put("periodId", periodId);
        }
        if (beginTime != null && !beginTime.isEmpty()) {
            params.put("beginTime", beginTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            params.put("endTime", endTime);
        }
        List<FinanceReviewTask> list = reviewTaskService.listTasks(params);
        return getDataTable(list);
    }

    /**
     * 从诊断结果生成复盘任务
     */
    @RequiresPermissions("finance:reviewTask:add")
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody ReportQueryParams params) {
        List<Long> deptIds = params.getDeptIds();
        if (deptIds == null || deptIds.isEmpty()) {
            return AjaxResult.error("门店ID列表不能为空");
        }
        int count = reviewTaskService.generateFromDiagnosis(deptIds, params);
        return AjaxResult.success("成功生成 " + count + " 条复盘任务", count);
    }

    /**
     * 标记任务为处理中
     */
    @RequiresPermissions("finance:reviewTask:edit")
    @PostMapping("/{taskId}/in-progress")
    public AjaxResult markInProgress(@PathVariable Long taskId) {
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        reviewTaskService.markInProgress(taskId, userId, username);
        return AjaxResult.success("任务已标记为处理中");
    }

    /**
     * 标记任务为已完成
     */
    @RequiresPermissions("finance:reviewTask:edit")
    @PostMapping("/{taskId}/done")
    public AjaxResult markDone(@PathVariable Long taskId, @RequestBody Map<String, String> body) {
        String handlerNote = body.get("handlerNote");
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        reviewTaskService.markDone(taskId, userId, username, handlerNote);
        return AjaxResult.success("任务已标记为完成");
    }

    /**
     * 标记任务为已忽略
     */
    @RequiresPermissions("finance:reviewTask:edit")
    @PostMapping("/{taskId}/ignored")
    public AjaxResult markIgnored(@PathVariable Long taskId, @RequestBody Map<String, String> body) {
        String ignoreReason = body.get("ignoreReason");
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        reviewTaskService.markIgnored(taskId, userId, username, ignoreReason);
        return AjaxResult.success("任务已标记为忽略");
    }
}
