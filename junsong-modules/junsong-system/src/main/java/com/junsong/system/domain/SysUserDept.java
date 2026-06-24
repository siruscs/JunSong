package com.junsong.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 用户和部门关联对象 sys_user_dept
 * 
 * @author junsong
 */
public class SysUserDept extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long userDeptId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 部门/店面ID */
    @Excel(name = "部门ID")
    private Long deptId;

    /** 状态（0在职 1离职） */
    @Excel(name = "状态", readConverterExp = "0=在职,1=离职")
    private String status;

    /** 入职日期 */
    @Excel(name = "入职日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date hireDate;

    /** 离职日期 */
    @Excel(name = "离职日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date leaveDate;

    /** 用户昵称（非持久化） */
    private String nickName;

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public Long getUserDeptId()
    {
        return userDeptId;
    }

    public void setUserDeptId(Long userDeptId)
    {
        this.userDeptId = userDeptId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getHireDate()
    {
        return hireDate;
    }

    public void setHireDate(Date hireDate)
    {
        this.hireDate = hireDate;
    }

    public Date getLeaveDate()
    {
        return leaveDate;
    }

    public void setLeaveDate(Date leaveDate)
    {
        this.leaveDate = leaveDate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userDeptId", getUserDeptId())
            .append("userId", getUserId())
            .append("deptId", getDeptId())
            .append("status", getStatus())
            .append("hireDate", getHireDate())
            .append("leaveDate", getLeaveDate())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
