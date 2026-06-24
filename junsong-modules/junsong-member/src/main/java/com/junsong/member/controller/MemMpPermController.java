package com.junsong.member.controller;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.MemMpRoleModule;
import com.junsong.member.service.IMemMpRoleModuleService;

@RestController
@RequestMapping({"/mpPerm", "/member/mpPerm"})
public class MemMpPermController extends BaseController {

    @Autowired
    private IMemMpRoleModuleService mpRoleModuleService;

    private static final List<Map<String, String>> MODULE_DEFINITIONS;
    static {
        String[][] defs = {
                {"member", "会员管理", "会员服务"},
                {"pointsGoods", "积分商品", "会员服务"},
                {"pointsRecord", "积分记录", "会员服务"},
                {"pointsExchange", "积分兑换", "会员服务"},
                {"seckill", "秒杀活动", "会员服务"},
                {"seckillRecord", "秒杀记录", "会员服务"},
                {"expense", "费用管理", "财务管理"},
                {"advance", "借支管理", "财务管理"},
                {"product", "商品管理", "财务管理"},
                {"supplier", "供应商管理", "财务管理"},
                {"purchase", "进货管理", "财务管理"},
                {"sale", "销售管理", "财务管理"},
                {"investorPayment", "投资人返款", "财务管理"},
                {"investor", "投资人管理", "财务管理"},
                {"investRecord", "投资款记录", "财务管理"},
                {"deptProfitConfig", "店面分润配置", "财务管理"},
                {"accountingPeriod", "核算周期", "财务管理"},
                {"profitShare", "分润结转", "财务管理"},
                {"costAccounting", "成本核算", "财务管理"}
        };
        List<Map<String, String>> list = new ArrayList<>();
        for (String[] arr : defs) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("key", arr[0]);
            m.put("name", arr[1]);
            m.put("group", arr[2]);
            list.add(m);
        }
        MODULE_DEFINITIONS = list;
    }

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
        return AjaxResult.success(MODULE_DEFINITIONS);
    }

    @RequiresPermissions("member:mpPerm:add")
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
