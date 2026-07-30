package com.junsong.workflow.lowcode.controller;

import com.junsong.common.core.domain.R;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.workflow.lowcode.domain.LcBizInstance;
import com.junsong.workflow.lowcode.service.LcBizService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 低代码运行态业务 Controller。
 * 端点前缀 /lowcode/biz/{bizCode}，挂靠 workflow 模块现有路由。
 */
@Validated
@RestController
@RequestMapping("/lowcode/biz")
public class LcBizController extends BaseController
{
    private final LcBizService bizService;

    public LcBizController(LcBizService bizService)
    {
        this.bizService = bizService;
    }

    @PreAuthorize("@ss.hasPermi('lowcode:biz:list')")
    @GetMapping("/{bizCode}/list")
    public R<List<LcBizInstance>> list(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode, LcBizInstance query)
    {
        startPage();
        List<LcBizInstance> list = bizService.list(bizCode, query);
        return R.ok(list);
    }

    @PreAuthorize("@ss.hasPermi('lowcode:biz:list')")
    @GetMapping("/{bizCode}/{id}")
    public R<LcBizInstance> getById(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode, @PathVariable("id") @Positive(message = "ID必须为正整数") Long id)
    {
        LcBizInstance instance = bizService.getById(bizCode, id);
        if (instance == null)
        {
            return R.fail("单据不存在");
        }
        return R.ok(instance);
    }

    @PreAuthorize("@ss.hasPermi('lowcode:biz:add') or @ss.hasPermi('workflow:instance:start')")
    @Idempotent(scene = "lowcode:biz:save")
    @PostMapping("/{bizCode}")
    public R<Long> save(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode, @RequestBody Map<String, Object> formData)
    {
        return bizService.save(bizCode, formData);
    }

    @PreAuthorize("@ss.hasPermi('lowcode:biz:edit')")
    @Idempotent(scene = "lowcode:biz:update")
    @PutMapping("/{bizCode}/{id}")
    public R<Void> update(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode, @PathVariable("id") @Positive(message = "ID必须为正整数") Long id,
                          @RequestBody Map<String, Object> formData)
    {
        return bizService.update(bizCode, id, formData);
    }

    @PreAuthorize("@ss.hasPermi('lowcode:biz:remove')")
    @Idempotent(scene = "lowcode:biz:delete")
    @DeleteMapping("/{bizCode}/{ids}")
    public R<Void> delete(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode, @PathVariable("ids") Long[] ids)
    {
        return bizService.delete(bizCode, ids);
    }

    @PreAuthorize("@ss.hasPermi('lowcode:biz:submit') or @ss.hasPermi('workflow:instance:start')")
    @Idempotent(scene = "lowcode:biz:submit", highRisk = true, ttlSeconds = 2592000)
    @PostMapping("/{bizCode}/{id}/submit")
    public R<Map<String, Object>> submit(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode, @PathVariable("id") @Positive(message = "ID必须为正整数") Long id)
    {
        return bizService.submit(bizCode, id);
    }

    @PreAuthorize("@ss.hasPermi('lowcode:biz:submit')")
    @Idempotent(scene = "lowcode:biz:withdraw", highRisk = true, ttlSeconds = 2592000)
    @PostMapping("/{bizCode}/{id}/withdraw")
    public R<Void> withdraw(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode, @PathVariable("id") @Positive(message = "ID必须为正整数") Long id)
    {
        return bizService.withdraw(bizCode, id);
    }

    @PreAuthorize("@ss.hasPermi('lowcode:biz:fulfill')")
    @Idempotent(scene = "lowcode:biz:fulfill", highRisk = true, ttlSeconds = 2592000)
    @PostMapping("/{bizCode}/{id}/fulfill")
    public R<Void> fulfill(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode, @PathVariable("id") @Positive(message = "ID必须为正整数") Long id,
                           @RequestBody(required = false) Map<String, Object> formData)
    {
        return bizService.fulfill(bizCode, id, formData);
    }
}
