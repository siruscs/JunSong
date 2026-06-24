package com.junsong.workflow.lowcode.domain;

import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 低代码节点定时器配置。
 *
 * <p>用于配置流程节点的边界定时器（Timer Boundary Event），覆盖：
 * <ul>
 *   <li>审核超时：审批节点 N 天未处理 → 自动取消</li>
 *   <li>补材料限期：履约节点 N 天未提交 → 超时处理</li>
 * </ul>
 *
 * <p>BPMN 装配时，为配置了定时器的 userTask 动态挂边界定时器：
 * <pre>
 * &lt;boundaryEvent attachedToRef="{taskKey}" cancelActivity="true"&gt;
 *   &lt;timerEventDefinition&gt;
 *     &lt;timeDuration&gt;{duration}&lt;/timeDuration&gt;
 *   &lt;/timerEventDefinition&gt;
 * &lt;/boundaryEvent&gt;
 * </pre>
 *
 * @author junsong
 */
public class LcBizNodeTimer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bizCode;
    private String taskKey;
    private String taskName;
    /** ISO-8601 时长：P3D(3天)/P45D(45天) */
    private String duration;
    /** 超时动作：AUTO_CANCEL / AUTO_PASS / NOTIFY */
    private String timeoutAction;
    /** 超时后业务状态：CANCELLED / EXPIRED 等 */
    private String targetStatus;
    private String delFlag;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getBizCode()
    {
        return bizCode;
    }

    public void setBizCode(String bizCode)
    {
        this.bizCode = bizCode;
    }

    public String getTaskKey()
    {
        return taskKey;
    }

    public void setTaskKey(String taskKey)
    {
        this.taskKey = taskKey;
    }

    public String getTaskName()
    {
        return taskName;
    }

    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
    }

    public String getDuration()
    {
        return duration;
    }

    public void setDuration(String duration)
    {
        this.duration = duration;
    }

    public String getTimeoutAction()
    {
        return timeoutAction;
    }

    public void setTimeoutAction(String timeoutAction)
    {
        this.timeoutAction = timeoutAction;
    }

    public String getTargetStatus()
    {
        return targetStatus;
    }

    public void setTargetStatus(String targetStatus)
    {
        this.targetStatus = targetStatus;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("bizCode", getBizCode())
                .append("taskKey", getTaskKey())
                .append("taskName", getTaskName())
                .append("duration", getDuration())
                .append("timeoutAction", getTimeoutAction())
                .append("targetStatus", getTargetStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
