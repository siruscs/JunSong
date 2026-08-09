package com.junsong.member.util;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.junsong.member.domain.SysMpModuleSort;
import com.junsong.member.service.ISysMpModuleSortService;

/**
 * 运行时「小程序模块目录」提供者。
 *
 * <p>以 MpModuleCatalog 静态 hardcode 列表作为基准字典，
 * 读取 sys_mp_module_sort 表中用户在 PC 端「功能模块调整」里保存的排序值进行覆盖重排。
 *
 * <p>sys_mp_module_sort 表同时存储两种记录（通过 module_key 的前缀区分）：
 * <ul>
 *   <li>普通模块排序：module_key = {@code member}, {@code expense} 等真实模块 key</li>
 *   <li>分组排序（哨兵行）：module_key = {@code @GROUP@会员服务} — 控制分组整体显示顺序</li>
 * </ul>
 * 复用同一张表，避免新增表结构。
 */
@Component
public class MpModuleCatalogSupplier {

    /** 分组排序哨兵行的 module_key 前缀（同一张表里和普通模块 key 区分）。 */
    public static final String GROUP_SENTINEL_PREFIX = "@GROUP@";

    /** 默认分组显示顺序（用于 DB 还没有配置过分组排序时兜底）。 */
    private static final List<String> DEFAULT_GROUP_ORDER = Collections.unmodifiableList(Arrays.asList(
            MpModuleCatalog.GROUP_MEMBER,
            MpModuleCatalog.GROUP_OPERATION,
            MpModuleCatalog.GROUP_FINANCE,
            MpModuleCatalog.GROUP_SYSTEM,
            MpModuleCatalog.GROUP_OFFICE
    ));

    @Autowired
    private ISysMpModuleSortService sortService;

    /** 返回按 PC 端配置（分组哨兵行 + 模块行）排序后的分组名称列表。用于小程序端首页分组展示顺序。 */
    public List<String> sortedGroupNames() {
        List<SysMpModuleSort> dbSort;
        try {
            dbSort = sortService.selectAll();
        } catch (Exception ignored) {
            return DEFAULT_GROUP_ORDER;
        }
        if (dbSort == null || dbSort.isEmpty()) return DEFAULT_GROUP_ORDER;

        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        for (SysMpModuleSort s : dbSort) {
            if (s == null || s.getModuleKey() == null || s.getSortOrder() == null) continue;
            if (!s.getModuleKey().startsWith(GROUP_SENTINEL_PREFIX)) continue;
            String groupName = s.getModuleKey().substring(GROUP_SENTINEL_PREFIX.length());
            if (groupName.isEmpty()) continue;
            list.add(new AbstractMap.SimpleImmutableEntry<>(groupName, s.getSortOrder()));
        }
        if (list.isEmpty()) return DEFAULT_GROUP_ORDER;
        list.sort(Comparator.comparingInt(Map.Entry::getValue));
        List<String> ordered = new ArrayList<>(list.size());
        for (Map.Entry<String, Integer> e : list) ordered.add(e.getKey());

        // 补全可能遗漏的分组（比如代码里新分组但 DB 没配置的），追加到末尾。
        for (String g : DEFAULT_GROUP_ORDER) {
            if (!ordered.contains(g)) ordered.add(g);
        }
        return Collections.unmodifiableList(ordered);
    }

    /**
     * 返回「按 DB 模块排序覆盖后」的完整模块列表。
     *
     * <p>注意：本方法会忽略 {@link #GROUP_SENTINEL_PREFIX} 开头的分组哨兵行，
     * 因为这些不是真实模块。
     */
    public List<MpModuleCatalog.Module> allSorted() {
        List<MpModuleCatalog.Module> base = MpModuleCatalog.all();
        List<SysMpModuleSort> dbSort;
        try {
            dbSort = sortService.selectAll();
        } catch (Exception ignored) {
            return Collections.unmodifiableList(base);
        }
        Map<String, Integer> sortMap = new HashMap<>(Math.max(dbSort != null ? dbSort.size() : 0, base.size()));
        if (dbSort != null) {
            for (SysMpModuleSort s : dbSort) {
                if (s == null || s.getModuleKey() == null) continue;
                if (s.getSortOrder() == null) continue;
                // 忽略分组哨兵行，这些不参与真实模块排序。
                if (s.getModuleKey().startsWith(GROUP_SENTINEL_PREFIX)) continue;
                sortMap.put(s.getModuleKey(), s.getSortOrder());
            }
        }
        List<MpModuleCatalog.Module> copy = new ArrayList<>(base);
        copy.sort((a, b) -> {
            Integer oa = sortMap.get(a.key);
            Integer ob = sortMap.get(b.key);
            int ia = (oa != null) ? oa : Integer.MAX_VALUE;
            int ib = (ob != null) ? ob : Integer.MAX_VALUE;
            if (ia != ib) return Integer.compare(ia, ib);
            return Integer.compare(indexOf(base, a.key), indexOf(base, b.key));
        });
        return Collections.unmodifiableList(copy);
    }

    private static int indexOf(List<MpModuleCatalog.Module> list, String key) {
        for (int i = 0; i < list.size(); i++) {
            if (key.equals(list.get(i).key)) return i;
        }
        return Integer.MAX_VALUE;
    }

    /** 返回带排序后的 key/name/group 定义列表（平铺）。用于 PC mpPerm 页 checkbox 区。
     *  仅返回 hasFrontendPage=true 的模块，避免三级页面（如 stockLedger）和未实现功能（如 stocktake）出现在权限配置页。 */
    public List<Map<String, String>> definitions() {
        List<MpModuleCatalog.Module> sorted = allSorted();
        List<Map<String, String>> out = new ArrayList<>(sorted.size());
        for (MpModuleCatalog.Module m : sorted) {
            if (!m.hasFrontendPage) continue;
            out.add(m.toDefinitionMap());
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * 返回「模块定义 + 当前分组排序顺序」的包装体，用于 PC「功能模块调整」弹窗 GET /mpPerm/moduleSort 接口。
     * 小程序端不需要模块定义（直接用 modules.js），只需要 sortedGroupNames() 返回的分组顺序即可。
     */
    public Map<String, Object> definitionsWithGroupOrder() {
        Map<String, Object> wrapper = new LinkedHashMap<>(4);
        wrapper.put("definitions", definitions());
        wrapper.put("groupOrder", sortedGroupNames());
        return Collections.unmodifiableMap(wrapper);
    }

    /** 返回排序后的前端页面模块 keys（用于 MemMpController 对 ALL_MODULES 的兜底顺序）。 */
    public List<String> frontendModuleKeys() {
        List<MpModuleCatalog.Module> sorted = allSorted();
        List<String> out = new ArrayList<>(sorted.size());
        for (MpModuleCatalog.Module m : sorted) {
            if (m.hasFrontendPage) out.add(m.key);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * 按排序要求重新排列输入的 moduleKeys。
     *
     * <p>用于：后端根据权限从 DB 取出用户的 module keys，按当前 PC 端配置的显示顺序
     * 重新排序后再返回给小程序端 /mp/userinfo & /mp/modules。
     *
     * @param moduleKeys 原始 keys（通常来自 role 配置 DB），允许重复；顺序任意
     * @return 去重后按当前排序要求返回的 key 列表
     */
    public List<String> sortModuleKeys(List<String> moduleKeys) {
        if (moduleKeys == null || moduleKeys.isEmpty()) return Collections.emptyList();
        List<MpModuleCatalog.Module> orderRef = allSorted();
        Map<String, Integer> rank = new HashMap<>(orderRef.size());
        for (int i = 0; i < orderRef.size(); i++) rank.put(orderRef.get(i).key, i);
        Set<String> seen = new LinkedHashSet<>(moduleKeys);
        List<String> result = new ArrayList<>(seen);
        result.sort((a, b) -> {
            Integer ra = rank.get(a);
            Integer rb = rank.get(b);
            int ia = (ra == null) ? Integer.MAX_VALUE : ra;
            int ib = (rb == null) ? Integer.MAX_VALUE : rb;
            if (ia != ib) return Integer.compare(ia, ib);
            return 0;
        });
        return result;
    }
}
