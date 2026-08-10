package com.junsong.member.util;

import java.util.*;

/**
 * 小程序模块目录（权威字典）。
 *
 * <h3>为什么需要这个类</h3>
 * 小程序模块的 key / 展示名称 / 分组 / view 权限码，同时出现在 4 个地方：
 * 1) 小程序端 junsong-miniprogram/src/config/modules.js（首页与工作台九宫格渲染）
 * 2) junsong-ui-v3/src/views/member/mpPerm/index.vue（PC「小程序权限」配置页的 checkbox 列表）
 * 3) MemMpPermController（/mpPerm/modules 接口，把 2）中静态字典搬到服务端一份）
 * 4) MemMpController（/mp/userinfo /mp/modules 接口，决定用户登录后能看到哪些模块，按标准权限二次过滤）
 * 三处各写一份非常容易漏项和命名不一致，这就是“小程序端功能清单 vs PC 端小程序权限清单对不上”的根因。
 *
 * <h3>修复策略（fail closed）</h3>
 * <ul>
 *   <li>后端（3+4）全部统一使用 MpModuleCatalog 作为唯一真源。</li>
 *   <li>前端 1) / 2) 侧使用时按 catalog.key 顺序拼装，不要在前端再私自加 key。</li>
 *   <li>MemMpController.ALL_MODULES 仅包含 hasFrontendPage=true 的模块（避免 mem_mp_role_module 里出现 PC 端小程序权限能勾、小程序端实际无页面可跳转的伪模块）。</li>
 *   <li>hasModuleViewPermission 同时支持“模块授权兜底”（storage.modules 中存在该 key）与 “标准权限兜底”（至少拥有 view 列表里任意一个权限码）。
 * </ul>
 *
 * <h3>命名规则</h3>
 * <ul>
 *   <li>displayName 必须与 PC 端小程序权限里的 name 完全一致。</li>
 *   <li>displayName 同时必须与小程序端 modules[].title 完全一致。</li>
 *   <li>groupName 必须四组之一：会员服务 / 财务管理 / 系统管理 / 移动办公。</li>
 *   <li>新增任何模块时：先改这里 → 再把 modules.js 的 title/group 同步过去 → 最后在 mpPerm/index.vue 里用后端 GET /mpPerm/modules 返回数据直接渲染（禁止前端硬编码）。</li>
 * </ul>
 */
public final class MpModuleCatalog {

    public static final String GROUP_MEMBER = "会员服务";
    public static final String GROUP_OPERATION = "会员运营";
    public static final String GROUP_FINANCE = "财务管理";
    public static final String GROUP_SYSTEM = "系统管理";
    public static final String GROUP_OFFICE = "移动办公";

    public static final class Module {
        public final String key;
        public final String displayName;
        public final String groupName;
        /** 是否有对应的小程序页面；没有的模块（如 dashboard / growth 这种看板/二级页）不应出现在九宫格里。 */
        public final boolean hasFrontendPage;
        /** 查看该模块需要的权限码（任一命中即视为可见）；空数组表示不按权限过滤。 */
        public final String[] viewPermissions;

        Module(String key, String displayName, String groupName, boolean hasFrontendPage, String[] viewPermissions) {
            this.key = key;
            this.displayName = displayName;
            this.groupName = groupName;
            this.hasFrontendPage = hasFrontendPage;
            this.viewPermissions = viewPermissions == null ? new String[0] : viewPermissions.clone();
        }

        public Map<String, String> toDefinitionMap() {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("key", key);
            m.put("name", displayName);
            m.put("group", groupName);
            return m;
        }
    }

    // ───────────────────────────────────────────────
    // 权威模块列表（顺序即 PC MP 权限配置页勾选顺序 & 小程序九宫格分组内顺序）
    // ───────────────────────────────────────────────
    private static final List<Module> ALL = Arrays.asList(
        // ===== 会员服务 =====
        m("member", "会员管理", GROUP_MEMBER, true,
            v("member:member:list", "member:member:query")),
        m("memberPurchase", "购买记录", GROUP_MEMBER, true,
            v("member:purchase:list", "member:purchase:query")),
        m("memberPurchaseReturn", "退货/退款", GROUP_MEMBER, true,
            v("member:purchaseReturn:list", "member:purchaseReturn:query")),
        m("memberLevel", "等级配置", GROUP_MEMBER, true,
            v("member:level:list", "member:level:query")),
        m("campaignPolicy", "销售政策", GROUP_MEMBER, true,
            v("member:campaignPolicy:list", "member:campaignPolicy:query")),
        m("pointsGoods", "积分商品", GROUP_MEMBER, true,
            v("member:pointsGoods:list", "member:pointsGoods:query")),
        m("pointsRule", "积分规则", GROUP_MEMBER, true,
            v("member:pointsRule:list", "member:pointsRule:query")),
        m("pointsRecord", "积分记录", GROUP_MEMBER, true,
            v("member:pointsRecord:list", "member:pointsRecord:query")),
        m("pointsExchange", "积分兑换", GROUP_MEMBER, true,
            v("member:pointsExchange:list", "member:pointsExchange:query")),
        m("seckill", "秒杀活动", GROUP_MEMBER, true,
            v("member:seckill:list", "member:seckill:query")),
        m("seckillRecord", "秒杀记录", GROUP_MEMBER, true,
            v("member:seckillRecord:list", "member:seckillRecord:query")),

        // ===== 会员运营（小程序端 pages/member/{dashboard,growth,actions,points}.vue）=====
        // 这4个功能不是独立 CRUD 模块（不在九宫格以 module schema 渲染），
        // 但有独立页面，且需要出现在 PC「小程序权限」配置页以便按角色单独授权。
        m("dashboard", "会员运营看板", GROUP_OPERATION, true,
            v("member:member:list", "member:member:query")),
        m("growth", "成长体系", GROUP_OPERATION, true,
            v("member:member:list", "member:member:query")),
        m("actions", "增长动作", GROUP_OPERATION, true,
            v("member:member:list", "member:member:query")),
        m("points", "积分运营", GROUP_OPERATION, true,
            v("member:pointsRecord:list", "member:pointsRecord:query",
              "member:pointsExchange:list", "member:pointsExchange:query")),

        // ===== 财务管理 =====
        m("expense", "费用管理", GROUP_FINANCE, true,
            v("finance:expense:list", "finance:expense:query")),
        m("advance", "借支管理", GROUP_FINANCE, true,
            v("finance:advance:list", "finance:advance:query")),
        m("product", "商品管理", GROUP_FINANCE, true,
            v("finance:product:list", "finance:product:query")),
        m("supplier", "供应商管理", GROUP_FINANCE, true,
            v("finance:supplier:list", "finance:supplier:query")),
        m("purchase", "进货管理", GROUP_FINANCE, true,
            v("finance:purchase:list", "finance:purchase:query")),
        m("sale", "销售管理", GROUP_FINANCE, true,
            v("finance:sale:list", "finance:sale:query")),
        m("investorPayment", "投资人返款", GROUP_FINANCE, true,
            v("finance:investorPayment:list", "finance:investorPayment:query")),
        m("investor", "投资人管理", GROUP_FINANCE, true,
            v("finance:investor:list", "finance:investor:query")),
        m("investRecord", "投资款记录", GROUP_FINANCE, true,
            v("finance:investRecord:list", "finance:investRecord:query")),
        m("deptProfitConfig", "店面分润配置", GROUP_FINANCE, true,
            v("finance:deptProfitConfig:list", "finance:deptProfitConfig:query")),
        m("accountingPeriod", "核算周期", GROUP_FINANCE, true,
            v("finance:accountingPeriod:list", "finance:accountingPeriod:query")),
        m("profitShare", "分润结转", GROUP_FINANCE, true,
            v("finance:profitShare:list", "finance:profitShare:query")),
        m("costAccounting", "成本核算", GROUP_FINANCE, true,
            v("finance:costAccounting:list", "finance:costAccounting:query")),
        m("stockCost", "库存与成本", GROUP_FINANCE, true,
            v("finance:report:stock", "finance:stock:list")),
        // stockLedger 是 stockCost 的三级页面（库存查询→库存流水明细），不应单独授权；
        // hasFrontendPage=false 使其不出现在 PC 权限配置页和九宫格，前端通过 authKey:'stockCost' 继承访问权限。
        m("stockLedger", "库存流水", GROUP_FINANCE, false,
            v("finance:stockLedger:list", "finance:stock:ledger:query", "finance:stock:ledger:list")),
        m("stockAdjustment", "库存调整", GROUP_FINANCE, true,
            v("finance:stockInit:list")),
        // stocktake 小程序端暂无对应功能页面，hasFrontendPage=false 避免出现在权限配置页
        m("stocktake", "库存盘点", GROUP_FINANCE, false,
            v("finance:stocktake:list", "finance:stocktake:query")),
        m("verificationRecord", "核销记录", GROUP_FINANCE, true,
            v("finance:expense:verificationRecord:list")),

        // ===== 系统管理 =====
        m("userManage", "用户管理", GROUP_SYSTEM, true,
            v("system:user:list", "system:user:query")),
        m("deptManage", "部门管理", GROUP_SYSTEM, true,
            v("system:dept:list", "system:dept:query")),
        m("configSync", "配置同步", GROUP_SYSTEM, true,
            v("member:configSync:query")),

        // ===== 移动办公 =====
        // wfTodo/wfDone/wfNotify 是小程序端的审批流聚合入口，真正的权限边界由后端 workflow 服务按用户角色判定，
        // 这里只按「PC 小程序权限页显式勾选了哪个模块」控制可见性，不再用 sys_role_menu 的 perms 做二次过滤。
        // 否则会出现「PC 小程序权限页明明勾了，sys_role_menu 里没配 workflow:mobile:* 权限码 → 小程序端入口消失」的假性BUG。
        m("wfTodo", "待办任务", GROUP_OFFICE, true, v()),
        m("wfDone", "已办任务", GROUP_OFFICE, true, v()),
        m("wfNotify", "消息通知", GROUP_OFFICE, true, v())
    );

    private static Module m(String key, String displayName, String groupName, boolean hasFrontendPage, String[] viewPermissions) {
        return new Module(key, displayName, groupName, hasFrontendPage, viewPermissions);
    }

    private static String[] v(String... perms) { return perms; }

    private MpModuleCatalog() {}

    // ───────────────────────────────────────────────
    // 对外查询 API
    // ───────────────────────────────────────────────

    /** 返回所有模块定义（用于 PC 端小程序权限配置页与小程序端 config/modules.js 的基准字典）。 */
    public static List<Module> all() { return Collections.unmodifiableList(ALL); }

    /** 返回“有小程序端页面”的模块 keys（MemMpController.ALL_MODULES 用这个）。 */
    public static List<String> frontendModuleKeys() {
        List<String> out = new ArrayList<>(ALL.size());
        for (Module m : ALL) if (m.hasFrontendPage) out.add(m.key);
        return Collections.unmodifiableList(out);
    }

    /** PC 端小程序权限配置页 checkbox 列表：返回 key/name/group Map。 */
    public static List<Map<String, String>> definitions() {
        List<Map<String, String>> out = new ArrayList<>(ALL.size());
        for (Module m : ALL) out.add(m.toDefinitionMap());
        return Collections.unmodifiableList(out);
    }

    /** 查看该模块所需权限码（任一命中即可视为可见）。 */
    public static String[] viewPermissions(String key) {
        if (key == null) return new String[0];
        for (Module m : ALL) if (key.equals(m.key)) return m.viewPermissions.clone();
        return new String[0];
    }

    /** 该模块是否在小程序端有一级入口页面（stockLedger/stocktake=false，用于过滤显式授权残留）。 */
    public static boolean hasFrontendPage(String key) {
        if (key == null) return false;
        for (Module m : ALL) if (key.equals(m.key)) return m.hasFrontendPage;
        return false;
    }
}
