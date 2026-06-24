package com.junsong.member.domain;

import com.junsong.common.core.web.domain.BaseEntity;

public class MemMpRoleModule extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long roleId;
    private String moduleKey;
    private Long deptId;

    private String roleName;
    private String roleKey;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleKey() { return roleKey; }
    public void setRoleKey(String roleKey) { this.roleKey = roleKey; }
}
