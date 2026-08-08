package com.junsong.member.controller;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.MemMpRoleModule;
import com.junsong.member.service.IMemMpRoleModuleService;
import com.junsong.member.util.MpModuleCatalog;

@RestController
@RequestMapping({"/mpPerm", "/member/mpPerm"})
public class MemMpPermController extends BaseController {

    @Autowired
    private IMemMpRoleModuleService mpRoleModuleService;

    // 模块权威字典由 MpModuleCatalog 统一维护（与 MemMpController / mpPerm/index.vue 共用），
    // 避免三处各写一份导致名称/分组/漏项不一致。

    @RequiresPermissions("member:mpPerm:list")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) Long roleId) {
        MemMpRoleModule query = new MemMpRoleModule();
        query.setRoleId(roleId);
        if (!SecurityUtils.isAdmin()) {
            query.setDeptId(SecurityUtils.getDeptId());
        }
        return AjaxResult.success(mpRoleModuleService.selectMpRoleModuleList(query));
    }

    @RequiresPermissions("member:mpPerm:list")
    @GetMapping("/roles")
    public AjaxResult roles() {
        return AjaxResult.success(mpRoleModuleService.selectAllRoles());
    }

    @RequiresPermissions("member:mpPerm:list")
    @GetMapping("/modules")
    public AjaxResult modules() {
        return AjaxResult.success(MpModuleCatalog.definitions());
    }

    @RequiresPermissions("member:mpPerm:add")
    @Idempotent(scene = "member:mp-perm:save")
    @PostMapping
    public AjaxResult save(@RequestBody Map<String, Object> params) {
        Long roleId = Long.valueOf(params.get("roleId").toString());
        Object deptIdObj = params.get("deptId");
        Long deptId = (deptIdObj != null && !deptIdObj.toString().isEmpty() && !"null".equals(deptIdObj.toString()))
                ? Long.valueOf(deptIdObj.toString())
                : 0L;

        @SuppressWarnings("unchecked")
        List<String> moduleKeys = (List<String>) params.get("moduleKeys");

        mpRoleModuleService.saveRoleModules(roleId, deptId, moduleKeys);
        return AjaxResult.success();
    }

    @RequiresPermissions("member:mpPerm:remove")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(mpRoleModuleService.deleteById(id));
    }

    @RequiresPermissions("member:mpPerm:remove")
    @DeleteMapping("/role/{roleId}/{deptId}")
    public AjaxResult removeByRole(@PathVariable Long roleId, @PathVariable Long deptId) {
        if (deptId == null || deptId == 0L) {
            return toAjax(mpRoleModuleService.deleteByRoleId(roleId));
        }
        return toAjax(mpRoleModuleService.deleteByRoleIdAndDeptId(roleId, deptId));
    }
}
