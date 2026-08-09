package com.junsong.member.domain;

import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 小程序模块显示顺序配置 sys_mp_module_sort
 *
 * 用于 PC 端「功能模块调整」拖拽保存模块显示顺序，
 * 后端 MpModuleCatalog / MemMpController 据此向小程序端按排序返回模块清单。
 */
public class SysMpModuleSort extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    /** 模块 key，唯一，对应 MpModuleCatalog.Module.key */
    private String moduleKey;
    /** 所属分组：会员服务 / 会员运营 / 财务管理 / 系统管理 / 移动办公 */
    private String groupName;
    /** 排序值（越小越靠前） */
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
