package com.junsong.finance.controller;

import java.util.*;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.finance.domain.FinanceReviewTask;
import com.junsong.finance.domain.vo.StockHealthIssueVO;
import com.junsong.finance.domain.vo.StockHealthVO;
import com.junsong.finance.service.IFinanceReviewTaskService;
import com.junsong.finance.service.IStockHealthService;
import com.junsong.system.api.domain.ActionCenterSourceItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/finance/inner/action-center")
public class FinanceActionCenterInnerController {
    @Autowired
    private IFinanceReviewTaskService reviewTaskService;

    @Autowired
    private IStockHealthService stockHealthService;

    @InnerAuth
    @GetMapping("/items")
    public R<List<ActionCenterSourceItem>> listItems(@RequestHeader(SecurityConstants.FROM_SOURCE) String source) {
        List<ActionCenterSourceItem> items = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("includeArchived", false);
        List<FinanceReviewTask> tasks = reviewTaskService.listTasks(params);
        if (tasks != null) {
            for (FinanceReviewTask task : tasks) {
                ActionCenterSourceItem item = new ActionCenterSourceItem();
                item.setActionId("FINANCE_REVIEW:" + task.getTaskId());
                item.setSourceType("FINANCE_RECEIVABLE");
                item.setSourceId(String.valueOf(task.getTaskId()));
                item.setTitle(task.getTitle() != null ? task.getTitle() : "财务复盘任务");
                item.setDescription(task.getReason());
                item.setPriority(task.getSeverity() != null ? task.getSeverity() : "MEDIUM");
                item.setStatus(mapReviewStatus(task.getStatus()));
                item.setOwnerId(task.getHandlerId());
                item.setOwnerName(task.getHandlerName());
                item.setDeptId(task.getDeptId());
                item.setDeptName(task.getDeptName());
                item.setDrilldownPath(task.getTargetRoute() != null ? task.getTargetRoute() : "/finance/reviewTask");
                items.add(item);
            }
        }

        StockHealthVO stockHealth = stockHealthService.checkHealth();
        if (stockHealth != null && stockHealth.getIssues() != null) {
            for (StockHealthIssueVO issue : stockHealth.getIssues()) {
                ActionCenterSourceItem item = new ActionCenterSourceItem();
                item.setActionId("STOCK_HEALTH:" + issue.getType());
                item.setSourceType("STOCK_HEALTH");
                item.setSourceId(issue.getType());
                item.setTitle(issue.getTitle() != null ? issue.getTitle() : "库存健康异常");
                item.setDescription(issue.getDetail());
                item.setPriority(issue.getSeverity() != null ? issue.getSeverity() : "MEDIUM");
                item.setStatus("PENDING");
                item.setDrilldownPath("/finance/stockHealth");
                items.add(item);
            }
        }

        return R.ok(items);
    }

    private String mapReviewStatus(String status) {
        if (status == null) return "PENDING";
        if ("DONE".equalsIgnoreCase(status)) return "EFFECT_PENDING";
        if ("IGNORED".equalsIgnoreCase(status)) return "IGNORED";
        if ("IN_PROGRESS".equalsIgnoreCase(status)) return "IN_PROGRESS";
        return "PENDING";
    }
}
