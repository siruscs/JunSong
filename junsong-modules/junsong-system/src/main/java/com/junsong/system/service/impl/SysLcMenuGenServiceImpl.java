package com.junsong.system.service.impl;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.system.api.domain.LcMenuGenerateRequest;
import com.junsong.system.domain.SysMenu;
import com.junsong.system.mapper.SysMenuMapper;
import com.junsong.system.service.ISysLcMenuGenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 低代码菜单自动生成 服务实现
 */
@Service
public class SysLcMenuGenServiceImpl implements ISysLcMenuGenService
{
    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generate(LcMenuGenerateRequest request)
    {
        if (request == null || request.getBizCode() == null || request.getBizCode().isBlank())
        {
            throw new ServiceException("bizCode 不能为空");
        }
        String bizCode = request.getBizCode();
        String bizName = (request.getBizName() == null || request.getBizName().isBlank())
                ? bizCode : request.getBizName();
        String parentMenuName = (request.getParentMenuName() == null || request.getParentMenuName().isBlank())
                ? "低代码应用" : request.getParentMenuName();
        String icon = (request.getIcon() == null || request.getIcon().isBlank())
                ? "documentation" : request.getIcon();

        // 1. 查/建父目录
        Long parentId = ensureParentDir(parentMenuName);

        // 2. 幂等清理：删除该 bizCode 已有的菜单 C 及其按钮 F
        cleanupExistingMenu(bizCode);

        // 3. 插入菜单 C
        SysMenu menuC = new SysMenu();
        menuC.setMenuName(bizName);
        menuC.setParentId(parentId);
        menuC.setOrderNum(1);
        menuC.setPath(bizCode);
        menuC.setComponent("lowcode/SchemaList");
        menuC.setRouteName(toRouteName(bizCode));
        menuC.setIsFrame("1");
        menuC.setIsCache("0");
        menuC.setMenuType("C");
        menuC.setVisible("0");
        menuC.setStatus("0");
        menuC.setPerms("lowcode:biz:list");
        menuC.setIcon(icon);
        sysMenuMapper.insertMenu(menuC);

        // 4. 插入 3 个按钮 F
        insertButton(menuC.getMenuId(), 1, "单据查询", "lowcode:biz:list");
        insertButton(menuC.getMenuId(), 2, "单据新增编辑", "lowcode:biz:edit");
        insertButton(menuC.getMenuId(), 3, "单据提交履约", "lowcode:biz:submit");

        return menuC.getMenuId();
    }

    private Long ensureParentDir(String parentMenuName)
    {
        SysMenu existing = sysMenuMapper.checkMenuNameUnique(parentMenuName, 0L);
        if (existing != null)
        {
            return existing.getMenuId();
        }
        SysMenu dir = new SysMenu();
        dir.setMenuName(parentMenuName);
        dir.setParentId(0L);
        dir.setOrderNum(8);
        dir.setPath("lowcode");
        dir.setIsFrame("1");
        dir.setIsCache("0");
        dir.setMenuType("M");
        dir.setVisible("0");
        dir.setStatus("0");
        dir.setIcon("form");
        sysMenuMapper.insertMenu(dir);
        return dir.getMenuId();
    }

    private void cleanupExistingMenu(String bizCode)
    {
        List<SysMenu> existing = sysMenuMapper.selectMenusByPath(bizCode);
        if (existing == null || existing.isEmpty())
        {
            return;
        }
        for (SysMenu menu : existing)
        {
            // 先删子按钮
            List<SysMenu> children = sysMenuMapper.selectMenusByParentId(menu.getMenuId());
            if (children != null)
            {
                for (SysMenu child : children)
                {
                    sysMenuMapper.deleteMenuById(child.getMenuId());
                }
            }
            // 再删菜单本身
            sysMenuMapper.deleteMenuById(menu.getMenuId());
        }
    }

    private void insertButton(Long parentId, int orderNum, String menuName, String perms)
    {
        SysMenu btn = new SysMenu();
        btn.setMenuName(menuName);
        btn.setParentId(parentId);
        btn.setOrderNum(orderNum);
        btn.setIsFrame("1");
        btn.setIsCache("0");
        btn.setMenuType("F");
        btn.setVisible("0");
        btn.setStatus("0");
        btn.setPerms(perms);
        btn.setIcon("#");
        sysMenuMapper.insertMenu(btn);
    }

    /** bizCode → routeName: biz_code → LowcodeBizCode */
    private String toRouteName(String bizCode)
    {
        StringBuilder sb = new StringBuilder("Lowcode");
        for (String part : bizCode.split("_"))
        {
            if (!part.isEmpty())
            {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1)
                {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }
}
