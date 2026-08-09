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
import com.junsong.member.domain.SysMpModuleSort;
import com.junsong.member.service.IMemMpRoleModuleService;
import com.junsong.member.service.ISysMpModuleSortService;
import com.junsong.member.util.MpModuleCatalogSupplier;

@RestController
@RequestMapping({"/mpPerm", "/member/mpPerm"})
public class MemMpPermController extends BaseController {

    @Autowired
    private IMemMpRoleModuleService mpRoleModuleService;

    @Autowired
    private MpModuleCatalogSupplier moduleCatalogSupplier;

    @Autowired
    private ISysMpModuleSortService moduleSortService;

    // 模块权威字典统一维护在 MpModuleCatalog + sys_mp_module_sort：
    // - MpModuleCatalog：hardcode 基准字典（名称、分组、权限码）
    // - sys_mp_module_sort：PC 端「功能模块调整」保存的显示顺序
    // MpModuleCatalogSupplier 把两者结合后对外提供排序后的模块列表。

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

    /** PC 端 mpPerm 页勾选框列表：返回按当前显示顺序排列的模块定义。 */
    @RequiresPermissions("member:mpPerm:list")
    @GetMapping("/modules")
    public AjaxResult modules() {
        return AjaxResult.success(moduleCatalogSupplier.definitions());
    }

    /**
     * PC 端「功能模块调整」弹窗初始化：
     * 返回「模块完整定义 + 分组排序顺序」的包装结构，便于弹窗同时支持组内模块拖拽与大组间拖拽。
     */
    @RequiresPermissions("member:mpPerm:list")
    @GetMapping("/moduleSort")
    public AjaxResult listModuleSort() {
        return AjaxResult.success(moduleCatalogSupplier.definitionsWithGroupOrder());
    }

    /**
     * PC 端「功能模块调整」弹窗保存：
     * 按前端拖拽后的 moduleKey 顺序整体重写 sys_mp_module_sort。
     * 入参：[{moduleKey, groupName, sortOrder?, remark?}] 数组。
     * 为了简单和幂等，后端会：删全部 → 按入参顺序以 10 步进重新写入 sort_order。
     */
    @RequiresPermissions("member:mpPerm:add")
    @Idempotent(scene = "member:mp-module-sort:save")
    @PostMapping("/moduleSort")
    public AjaxResult saveModuleSort(@RequestBody List<Map<String, Object>> sortList) {
        if (sortList == null) {
            return AjaxResult.error("参数不能为空");
        }
        List<SysMpModuleSort> records = new ArrayList<>(sortList.size());
        for (Map<String, Object> row : sortList) {
            if (row == null) continue;
            Object key = row.get("moduleKey");
            if (key == null || String.valueOf(key).trim().isEmpty()) continue;
            SysMpModuleSort rec = new SysMpModuleSort();
            rec.setModuleKey(String.valueOf(key));
            Object gn = row.get("groupName");
            rec.setGroupName(gn == null ? null : String.valueOf(gn));
            Object rk = row.get("remark");
            rec.setRemark(rk == null ? null : String.valueOf(rk));
            records.add(rec);
        }
        moduleSortService.saveBatch(records);
        return AjaxResult.success();
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
