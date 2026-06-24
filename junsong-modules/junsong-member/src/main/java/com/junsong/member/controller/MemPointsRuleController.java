package com.junsong.member.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.member.domain.MemPointsRule;
import com.junsong.member.service.IMemPointsRuleService;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;

/**
 * 积分规则Controller
 */
@RestController
@RequestMapping("/pointsRule")
public class MemPointsRuleController extends BaseController {

    @Autowired
    private IMemPointsRuleService memPointsRuleService;

    /**
     * 查询积分规则列表
     */
    @RequiresPermissions("member:pointsRule:list")
    @GetMapping("/list")
    public TableDataInfo list(MemPointsRule memPointsRule) {
        startPage();
        List<MemPointsRule> list = memPointsRuleService.selectMemPointsRuleList(memPointsRule);
        return getDataTable(list);
    }

    /**
     * 导出积分规则列表
     */
    @RequiresPermissions("member:pointsRule:export")
    @Log(title = "积分规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemPointsRule memPointsRule) {
        List<MemPointsRule> list = memPointsRuleService.selectMemPointsRuleList(memPointsRule);
        ExcelUtil<MemPointsRule> util = new ExcelUtil<MemPointsRule>(MemPointsRule.class);
        util.exportExcel(response, list, "积分规则数据");
    }

    /**
     * 获取积分规则详细信息
     */
    @RequiresPermissions("member:pointsRule:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(memPointsRuleService.selectMemPointsRuleById(id));
    }

    /**
     * 新增积分规则
     */
    @RequiresPermissions("member:pointsRule:add")
    @Log(title = "积分规则", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MemPointsRule memPointsRule) {
        if (!memPointsRuleService.checkMemPointsRuleNoUnique(memPointsRule)) {
            return error("新增积分规则失败，编号已存在");
        }
        memPointsRule.setCreateBy(SecurityUtils.getUsername());
        return toAjax(memPointsRuleService.insertMemPointsRule(memPointsRule));
    }

    /**
     * 修改积分规则
     */
    @RequiresPermissions("member:pointsRule:edit")
    @Log(title = "积分规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MemPointsRule memPointsRule) {
        if (!memPointsRuleService.checkMemPointsRuleNoUnique(memPointsRule)) {
            return error("修改积分规则失败，编号已存在");
        }
        memPointsRule.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(memPointsRuleService.updateMemPointsRule(memPointsRule));
    }

    /**
     * 删除积分规则
     */
    @RequiresPermissions("member:pointsRule:remove")
    @Log(title = "积分规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(memPointsRuleService.deleteMemPointsRuleByIds(ids));
    }

    /**
     * 获取当前生效的积分规则（第一条启用的规则）
     */
    @GetMapping("/effective")
    public AjaxResult getEffectiveRule() {
        MemPointsRule rule = memPointsRuleService.getEffectiveRule();
        if (rule == null) {
            return error("未找到生效的积分规则，请先配置积分规则");
        }
        return success(rule);
    }
}
