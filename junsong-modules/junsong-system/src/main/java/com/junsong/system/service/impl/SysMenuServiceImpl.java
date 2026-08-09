package com.junsong.system.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.constant.CacheConstants;
import com.junsong.common.core.constant.Constants;
import com.junsong.common.core.constant.UserConstants;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.text.Convert;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.redis.service.RedisService;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.api.domain.SysRole;
import com.junsong.system.domain.SysMenu;
import com.junsong.system.domain.vo.MetaVo;
import com.junsong.system.domain.vo.RouterVo;
import com.junsong.system.domain.vo.TreeSelect;
import com.junsong.system.mapper.SysMenuMapper;
import com.junsong.system.mapper.SysRoleMapper;
import com.junsong.system.mapper.SysRoleMenuMapper;
import com.junsong.system.service.ISysMenuService;

/**
 * 菜单 业务层处理
 * 
 * @author junsong
 */
@Service
public class SysMenuServiceImpl implements ISysMenuService
{
    private static final Logger log = LoggerFactory.getLogger(SysMenuServiceImpl.class);

    public static final String PREMISSION_STRING = "perms[\"{0}\"]";

    public static final Long MENU_ROOT_ID = 0L;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private RedisService redisService;

    private static final long MENU_CACHE_TTL_MINUTES = 30;

    private String getMenuTreeCacheKey(Long userId)
    {
        return CacheConstants.SYS_MENU_KEY + "tree:" + userId;
    }

    private void clearMenuCache()
    {
        Collection<String> keys = redisService.keys(CacheConstants.SYS_MENU_KEY + "*");
        if (keys != null && !keys.isEmpty())
        {
            redisService.deleteObject(keys);
        }
    }

    /**
     * 清除指定用户的菜单路由缓存
     *
     * 用于用户角色关联发生变更（分配/取消角色）后，
     * 使该用户下次 getRouters 时重新按最新角色查库生成菜单，
     * 避免旧的（可能为空的）菜单缓存在 TTL 内继续生效。
     *
     * @param userId 用户ID
     */
    @Override
    public void clearMenuCacheByUserId(Long userId)
    {
        if (userId == null)
        {
            return;
        }
        redisService.deleteObject(getMenuTreeCacheKey(userId));
    }

    /**
     * 根据用户查询系统菜单列表
     * 
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuList(Long userId)
    {
        return selectMenuList(new SysMenu(), userId);
    }

    /**
     * 查询系统菜单列表
     * 
     * @param menu 菜单信息
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuList(SysMenu menu, Long userId)
    {
        List<SysMenu> menuList = null;
        // 管理员显示所有菜单信息
        if (SecurityUtils.isAdmin(userId))
        {
            menuList = menuMapper.selectMenuList(menu);
        }
        else
        {
            menu.getParams().put("userId", userId);
            menuList = menuMapper.selectMenuListByUserId(menu);
        }
        return menuList;
    }

    /**
     * 根据用户ID查询权限
     * 
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByUserId(Long userId)
    {
        List<String> perms = menuMapper.selectMenuPermsByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms)
        {
            if (StringUtils.isNotEmpty(perm))
            {
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 根据角色ID查询权限
     * 
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByRoleId(Long roleId)
    {
        List<String> perms = menuMapper.selectMenuPermsByRoleId(roleId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms)
        {
            if (StringUtils.isNotEmpty(perm))
            {
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 根据用户ID查询菜单
     * 
     * @param userId 用户名称
     * @return 菜单列表
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<SysMenu> selectMenuTreeByUserId(Long userId)
    {
        String cacheKey = getMenuTreeCacheKey(userId);
        List<SysMenu> cached = redisService.getCacheObject(cacheKey);
        if (cached != null)
        {
            return cached;
        }
        List<SysMenu> menus = null;
        if (SecurityUtils.isAdmin(userId))
        {
            menus = menuMapper.selectMenuTreeAll();
        }
        else
        {
            menus = menuMapper.selectMenuTreeByUserId(userId);
        }
        List<SysMenu> result = getChildPerms(menus, MENU_ROOT_ID);
        redisService.setCacheObject(cacheKey, result, MENU_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return result;
    }

    /**
     * 根据角色ID查询菜单树信息
     * 
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    @Override
    public List<Long> selectMenuListByRoleId(Long roleId)
    {
        SysRole role = roleMapper.selectRoleById(roleId);
        return menuMapper.selectMenuListByRoleId(roleId, role.isMenuCheckStrictly());
    }

    /**
     * 构建前端路由所需要的菜单
     * 
     * @param menus 菜单列表
     * @return 路由列表
     */
    @Override
    public List<RouterVo> buildMenus(List<SysMenu> menus)
    {
        List<RouterVo> routers = new LinkedList<RouterVo>();
        if (menus == null)
        {
            return routers;
        }
        for (SysMenu menu : menus)
        {
            if (menu == null)
            {
                continue;
            }
            // 菜单路由防御性检查并规范化：空 path / 空 component 的叶子节点需要兜底，
            // 避免 Vue Router addRoute 抛 invalid route 导致用户登录失败。
            normalizeMenuPathsForRoute(menu);
            RouterVo router = new RouterVo();
            router.setHidden("1".equals(menu.getVisible()));
            router.setName(getRouteName(menu));
            final String rawPath = getRouterPath(menu);
            router.setPath(sanitizeRouterPath(rawPath, menu));
            final String rawComponent = getComponent(menu);
            router.setComponent(sanitizeComponent(rawComponent, menu));
            router.setQuery(menu.getQuery());
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
            List<SysMenu> cMenus = menu.getChildren();
            if (StringUtils.isNotEmpty(cMenus) && UserConstants.TYPE_DIR.equals(menu.getMenuType()))
            {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildMenus(cMenus));
            }
            else if (isMenuFrame(menu))
            {
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<RouterVo>();
                RouterVo children = new RouterVo();
                children.setPath(sanitizeRouterPath(menu.getPath(), menu));
                children.setComponent(StringUtils.isNotEmpty(menu.getComponent()) ? menu.getComponent() : UserConstants.LAYOUT);
                children.setName(getRouteName(menu.getRouteName(), menu.getPath()));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
                children.setQuery(menu.getQuery());
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            else if (MENU_ROOT_ID.equals(menu.getParentId()) && isInnerLink(menu))
            {
                router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
                router.setPath("/");
                List<RouterVo> childrenList = new ArrayList<RouterVo>();
                RouterVo children = new RouterVo();
                String routerPath = innerLinkReplaceEach(menu.getPath());
                children.setPath(sanitizeRouterPath(routerPath, menu));
                children.setComponent(UserConstants.INNER_LINK);
                children.setName(getRouteName(menu.getRouteName(), routerPath));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath()));
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            // 没有 children 且没有 component 也没有 redirect 的叶子节点 → 跳过不输出，前端 fail-soft 不阻塞登录
            if (router.getChildren() == null || router.getChildren().isEmpty())
            {
                boolean hasComponent = StringUtils.isNotEmpty(router.getComponent());
                boolean hasRedirect = StringUtils.isNotEmpty(router.getRedirect());
                boolean isExternal = isExternalRouterPath(router.getPath());
                if (!hasComponent && !hasRedirect && !isExternal)
                {
                    log.warn("[buildMenus] 跳过异常菜单 menuId={} menuName={}：component 为空且无 children/redirect/external，避免前端 addRoute 异常",
                            menu.getMenuId(), menu.getMenuName());
                    continue;
                }
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * 规范化菜单路径：去除首尾空白，合并连续斜杠；必要时按 menuId 生成占位路径。
     */
    private void normalizeMenuPathsForRoute(SysMenu menu)
    {
        if (StringUtils.isNotEmpty(menu.getPath()))
        {
            String normalized = menu.getPath().trim().replaceAll("/{2,}", "/");
            menu.setPath(normalized);
        }
    }

    private String sanitizeRouterPath(String rawPath, SysMenu menu)
    {
        String path = StringUtils.isEmpty(rawPath) ? "" : rawPath.trim().replaceAll("/{2,}", "/");
        if (StringUtils.isEmpty(path))
        {
            String fallback = "/placeholder-menu-" + (menu.getMenuId() != null ? String.valueOf(menu.getMenuId()) : "orphan");
            log.warn("[buildMenus] 菜单 menuId={} menuName={} path 为空，已兜底为 {}", menu.getMenuId(), menu.getMenuName(), fallback);
            return fallback;
        }
        return path;
    }

    private String sanitizeComponent(String rawComponent, SysMenu menu)
    {
        // 对一级菜单（Layout 父级）或目录型节点，空 component 可以兜底为 Layout/ParentView
        boolean isRoot = MENU_ROOT_ID.equals(menu.getParentId());
        boolean hasChildren = StringUtils.isNotEmpty(menu.getChildren());
        boolean dirOrFrame = UserConstants.TYPE_DIR.equals(menu.getMenuType()) || isMenuFrame(menu);
        if (StringUtils.isEmpty(rawComponent))
        {
            if (isRoot || hasChildren || dirOrFrame)
            {
                String fallback = isRoot && !dirOrFrame ? UserConstants.LAYOUT
                        : UserConstants.PARENT_VIEW;
                log.warn("[buildMenus] 菜单 menuId={} menuName={} component 为空，已兜底为 {}", menu.getMenuId(), menu.getMenuName(), fallback);
                return fallback;
            }
        }
        return rawComponent;
    }

    private boolean isExternalRouterPath(String path)
    {
        if (StringUtils.isEmpty(path))
        {
            return false;
        }
        String p = path.trim();
        return p.startsWith("http://") || p.startsWith("https://") || p.startsWith("mailto:") || p.startsWith("tel:");
    }

    /**
     * 构建前端所需要树结构
     * 
     * @param menus 菜单列表
     * @return 树结构列表
     */
    @Override
    public List<SysMenu> buildMenuTree(List<SysMenu> menus)
    {
        List<SysMenu> returnList = new ArrayList<SysMenu>();
        List<Long> tempList = menus.stream().map(SysMenu::getMenuId).collect(Collectors.toList());
        for (Iterator<SysMenu> iterator = menus.iterator(); iterator.hasNext();)
        {
            SysMenu menu = (SysMenu) iterator.next();
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (menu.getParentId() == null || !tempList.contains(menu.getParentId()))
            {
                recursionFn(menus, menu);
                returnList.add(menu);
            }
        }
        if (returnList.isEmpty())
        {
            returnList = menus;
        }
        return returnList;
    }

    /**
     * 构建前端所需要下拉树结构
     * 
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelect> buildMenuTreeSelect(List<SysMenu> menus)
    {
        List<SysMenu> menuTrees = buildMenuTree(menus);
        return menuTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 根据菜单ID查询信息
     * 
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    @Override
    public SysMenu selectMenuById(Long menuId)
    {
        return menuMapper.selectMenuById(menuId);
    }

    /**
     * 是否存在菜单子节点
     * 
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean hasChildByMenuId(Long menuId)
    {
        int result = menuMapper.hasChildByMenuId(menuId);
        return result > 0;
    }

    /**
     * 查询菜单使用数量
     * 
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean checkMenuExistRole(Long menuId)
    {
        int result = roleMenuMapper.checkMenuExistRole(menuId);
        return result > 0;
    }

    /**
     * 新增保存菜单信息
     * 
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public int insertMenu(SysMenu menu)
    {
        int result = menuMapper.insertMenu(menu);
        clearMenuCache();
        return result;
    }

    /**
     * 修改保存菜单信息
     * 
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public int updateMenu(SysMenu menu)
    {
        int result = menuMapper.updateMenu(menu);
        clearMenuCache();
        return result;
    }

    /**
     * 保存菜单排序
     * 
     * @param menuIds 菜单ID
     * @param orderNums 排序ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenuSort(String[] menuIds, String[] orderNums)
    {
        try
        {
            for (int i = 0; i < menuIds.length; i++)
            {
                SysMenu menu = new SysMenu();
                menu.setMenuId(Convert.toLong(menuIds[i]));
                menu.setOrderNum(Convert.toInt(orderNums[i]));
                menuMapper.updateMenuSort(menu);
            }
        }
        catch (Exception e)
        {
            throw new ServiceException("保存排序异常，请联系管理员");
        }
    }

    /**
     * 删除菜单管理信息
     * 
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public int deleteMenuById(Long menuId)
    {
        int result = menuMapper.deleteMenuById(menuId);
        clearMenuCache();
        return result;
    }

    /**
     * 校验菜单名称是否唯一
     * 
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkMenuNameUnique(SysMenu menu)
    {
        Long menuId = StringUtils.isNull(menu.getMenuId()) ? -1L : menu.getMenuId();
        SysMenu info = menuMapper.checkMenuNameUnique(menu.getMenuName(), menu.getParentId());
        if (StringUtils.isNotNull(info) && info.getMenuId().longValue() != menuId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验路由名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkRouteConfigUnique(SysMenu menu)
    {
        Long menuId = StringUtils.isNull(menu.getMenuId()) ? -1L : menu.getMenuId();
        Long parentId = menu.getParentId();
        String path = menu.getPath();
        String routeName = StringUtils.isEmpty(menu.getRouteName()) ? path : menu.getRouteName();
        List<SysMenu> sysMenuList = menuMapper.selectMenusByPathOrRouteName(path, routeName);
        for (SysMenu sysMenu : sysMenuList)
        {
            if (sysMenu.getMenuId().longValue() != menuId.longValue())
            {
                Long dbParentId = sysMenu.getParentId();
                String dbPath = sysMenu.getPath();
                String dbRouteName = StringUtils.isEmpty(sysMenu.getRouteName()) ? dbPath : sysMenu.getRouteName();
                if (StringUtils.equalsAnyIgnoreCase(path, dbPath) && (parentId != null && dbParentId != null && parentId.longValue() == dbParentId.longValue()))
                {
                    log.warn("[同级路由冲突] 同级下已存在相同路由路径 '{}'，冲突菜单：{}", dbPath, sysMenu.getMenuName());
                    return UserConstants.NOT_UNIQUE;
                }
                else if (StringUtils.equalsAnyIgnoreCase(path, dbPath) && MENU_ROOT_ID.equals(parentId))
                {
                    log.warn("[根目录路由冲突] 根目录下路由 '{}' 必须唯一，已被菜单 '{}' 占用", path, sysMenu.getMenuName());
                    return UserConstants.NOT_UNIQUE;
                }
                else if (StringUtils.equalsAnyIgnoreCase(routeName, dbRouteName))
                {
                    log.warn("[路由名称冲突] 路由名称 '{}' 需全局唯一，已被菜单 '{}' 使用", routeName, sysMenu.getMenuName());
                    return UserConstants.NOT_UNIQUE;
                }
            }
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 获取路由名称
     * 
     * @param menu 菜单信息
     * @return 路由名称
     */
    public String getRouteName(SysMenu menu)
    {
        // 非外链并且是一级目录（类型为目录）
        if (isMenuFrame(menu))
        {
            return StringUtils.EMPTY;
        }
        return getRouteName(menu.getRouteName(), menu.getPath());
    }

    /**
     * 获取路由名称，如没有配置路由名称则取路由地址
     * 
     * @param name 路由名称
     * @param path 路由地址
     * @return 路由名称（驼峰格式）
     */
    public String getRouteName(String name, String path)
    {
        String routerName = StringUtils.isNotEmpty(name) ? name : path;
        return StringUtils.capitalize(routerName);
    }

    /**
     * 获取路由地址
     * 
     * @param menu 菜单信息
     * @return 路由地址
     */
    public String getRouterPath(SysMenu menu)
    {
        String routerPath = menu.getPath();
        // 内链打开外网方式
        if (!MENU_ROOT_ID.equals(menu.getParentId()) && isInnerLink(menu))
        {
            routerPath = innerLinkReplaceEach(routerPath);
        }
        // 非外链并且是一级目录（类型为目录）
        if (MENU_ROOT_ID.equals(menu.getParentId()) && UserConstants.TYPE_DIR.equals(menu.getMenuType())
                && UserConstants.NO_FRAME.equals(menu.getIsFrame()))
        {
            routerPath = "/" + menu.getPath();
        }
        // 非外链并且是一级目录（类型为菜单）
        else if (isMenuFrame(menu))
        {
            routerPath = "/";
        }
        return routerPath;
    }

    /**
     * 获取组件信息
     * 
     * @param menu 菜单信息
     * @return 组件信息
     */
    public String getComponent(SysMenu menu)
    {
        String component = UserConstants.LAYOUT;
        if (StringUtils.isNotEmpty(menu.getComponent()) && !isMenuFrame(menu))
        {
            component = menu.getComponent();
        }
        else if (StringUtils.isEmpty(menu.getComponent()) && !MENU_ROOT_ID.equals(menu.getParentId()) && isInnerLink(menu))
        {
            component = UserConstants.INNER_LINK;
        }
        else if (StringUtils.isEmpty(menu.getComponent()) && isParentView(menu))
        {
            component = UserConstants.PARENT_VIEW;
        }
        return component;
    }

    /**
     * 是否为菜单内部跳转
     * 
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isMenuFrame(SysMenu menu)
    {
        return MENU_ROOT_ID.equals(menu.getParentId()) && UserConstants.TYPE_MENU.equals(menu.getMenuType())
                && menu.getIsFrame().equals(UserConstants.NO_FRAME);
    }

    /**
     * 是否为parent_view组件
     * 
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isParentView(SysMenu menu)
    {
        return !MENU_ROOT_ID.equals(menu.getParentId()) && UserConstants.TYPE_DIR.equals(menu.getMenuType());
    }

    /**
     * 是否为内链组件
     * 
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isInnerLink(SysMenu menu)
    {
        return menu.getIsFrame().equals(UserConstants.NO_FRAME) && StringUtils.ishttp(menu.getPath());
    }

    /**
     * 根据父节点的ID获取所有子节点
     * 
     * @param list 分类表
     * @param parentId 传入的父节点ID
     * @return String
     */
    public List<SysMenu> getChildPerms(List<SysMenu> list, long parentId)
    {
        List<SysMenu> returnList = new ArrayList<SysMenu>();
        for (Iterator<SysMenu> iterator = list.iterator(); iterator.hasNext();)
        {
            SysMenu t = (SysMenu) iterator.next();
            // 一、根据传入的某个父节点ID,遍历该父节点的所有子节点
            if (t.getParentId() == parentId)
            {
                recursionFn(list, t);
                returnList.add(t);
            }
        }
        return returnList;
    }

    /**
     * 递归列表
     * 
     * @param list 分类表
     * @param t 子节点
     */
    private void recursionFn(List<SysMenu> list, SysMenu t)
    {
        // 得到子节点列表
        List<SysMenu> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysMenu tChild : childList)
        {
            if (hasChild(list, tChild))
            {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<SysMenu> getChildList(List<SysMenu> list, SysMenu t)
    {
        List<SysMenu> tlist = new ArrayList<SysMenu>();
        Iterator<SysMenu> it = list.iterator();
        while (it.hasNext())
        {
            SysMenu n = (SysMenu) it.next();
            if (t.getMenuId().equals(n.getParentId()))
            {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<SysMenu> list, SysMenu t)
    {
        return getChildList(list, t).size() > 0;
    }

    /**
     * 内链域名特殊字符替换
     * 
     * @return 替换后的内链域名
     */
    public String innerLinkReplaceEach(String path)
    {
        return StringUtils.replaceEach(path, new String[] { Constants.HTTP, Constants.HTTPS, Constants.WWW, ".", ":" },
                new String[] { "", "", "", "/", "/" });
    }
}
