package com.junsong.finance.controller;

import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.FinanceReviewKnowledge;
import com.junsong.finance.service.IFinanceReviewKnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 复盘知识库Controller
 *
 * @author junsong
 */
@RestController
@RequestMapping("/review-knowledge")
public class FinanceReviewKnowledgeController extends BaseController {

    @Autowired
    private IFinanceReviewKnowledgeService knowledgeService;

    /**
     * 查询知识库列表（分页）
     */
    @RequiresPermissions("finance:reviewKnowledge:list")
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam(required = false) String problemType,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String reusable,
            @RequestParam(required = false) String title) {
        startPage();
        Map<String, Object> params = new HashMap<>();
        if (problemType != null && !problemType.isEmpty()) {
            params.put("problemType", problemType);
        }
        if (deptId != null) {
            params.put("deptId", deptId);
        }
        if (reusable != null && !reusable.isEmpty()) {
            params.put("reusable", reusable);
        }
        if (title != null && !title.isEmpty()) {
            params.put("title", title);
        }
        List<FinanceReviewKnowledge> list = knowledgeService.listKnowledge(params);
        return getDataTable(list);
    }

    /**
     * 新增知识
     */
    @RequiresPermissions("finance:reviewKnowledge:add")
    @PostMapping
    public AjaxResult add(@RequestBody FinanceReviewKnowledge knowledge) {
        knowledgeService.addKnowledge(knowledge);
        return AjaxResult.success("知识新增成功");
    }

    /**
     * 更新知识
     */
    @RequiresPermissions("finance:reviewKnowledge:edit")
    @PutMapping
    public AjaxResult edit(@RequestBody FinanceReviewKnowledge knowledge) {
        knowledgeService.updateKnowledge(knowledge);
        return AjaxResult.success("知识更新成功");
    }

    /**
     * 从复盘任务沉淀知识
     */
    @RequiresPermissions("finance:reviewKnowledge:add")
    @PostMapping("/from-task/{taskId}")
    public AjaxResult createFromTask(@PathVariable Long taskId, @RequestBody Map<String, String> body) {
        FinanceReviewKnowledge knowledge = knowledgeService.createFromTask(taskId, body);
        return AjaxResult.success("知识沉淀成功", knowledge);
    }

    /**
     * 为复盘任务推荐历史知识
     */
    @RequiresPermissions("finance:reviewKnowledge:list")
    @GetMapping("/recommendations/task/{taskId}")
    public R<List<FinanceReviewKnowledge>> recommendForTask(@PathVariable Long taskId) {
        return R.ok(knowledgeService.recommendForTask(taskId));
    }
}
