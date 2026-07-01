package com.junsong.workflow.service.timeout;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.domain.WfNodeTimeout;
import com.junsong.workflow.domain.WfTimeoutTriggerLog;
import com.junsong.workflow.mapper.WfNodeTimeoutMapper;
import com.junsong.workflow.mapper.WfNotificationMapper;
import com.junsong.workflow.mapper.WfSysUserMapper;
import com.junsong.workflow.mapper.WfTimeoutTriggerLogMapper;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowTimeoutService
{
    @Autowired
    private WfNodeTimeoutMapper timeoutMapper;
    @Autowired
    private WfTimeoutTriggerLogMapper triggerLogMapper;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private WfNotificationMapper notificationMapper;
    @Autowired
    private WfSysUserMapper sysUserMapper;

    public R<List<WfNodeTimeout>> list(WfNodeTimeout timeout)
    {
        return R.ok(timeoutMapper.selectList(timeout));
    }

    public R<WfNodeTimeout> getById(Long id)
    {
        return R.ok(timeoutMapper.selectById(id));
    }

    public R<Void> add(WfNodeTimeout timeout)
    {
        timeoutMapper.insert(timeout);
        return R.ok();
    }

    public R<Void> update(WfNodeTimeout timeout)
    {
        timeoutMapper.update(timeout);
        return R.ok();
    }

    public R<Void> delete(Long id)
    {
        timeoutMapper.deleteById(id);
        return R.ok();
    }

    /**
     * 扫描超时任务并触发升级
     * 防止重复触发：每次触发后记录 lastTriggerTime，下次扫描时至少间隔 timeoutMinutes/2 才再次触发
     */
    public void scanAndEscalate()
    {
        List<WfNodeTimeout> configs = timeoutMapper.selectAll();
        for (WfNodeTimeout config : configs)
        {
            List<Task> tasks = taskService.createTaskQuery()
                    .taskDefinitionKey(config.getActivityId())
                    .processDefinitionKey(config.getProcessDefinitionKey())
                    .list();
            for (Task task : tasks)
            {
                if (task.getCreateTime() == null || config.getTimeoutMinutes() == null)
                {
                    continue;
                }

                long elapsedMinutes = computeElapsedMinutes(task.getCreateTime(), "1".equals(config.getIsWorkday()));
                if (elapsedMinutes < config.getTimeoutMinutes())
                {
                    continue;
                }

                // 防止重复触发：距离上次触发至少间隔 timeoutMinutes/2
                if (config.getLastTriggerTime() != null)
                {
                    long minutesSinceLastTrigger = Duration.between(config.getLastTriggerTime().toInstant(), Instant.now()).toMinutes();
                    long cooldown = Math.max(config.getTimeoutMinutes() / 2, 30); // 最小冷却30分钟
                    if (minutesSinceLastTrigger < cooldown)
                    {
                        continue;
                    }
                }

                // 触发升级
                boolean success = handleEscalation(task, config);

                // 记录触发日志
                WfTimeoutTriggerLog log = new WfTimeoutTriggerLog();
                log.setTimeoutConfigId(config.getId());
                log.setTaskId(task.getId());
                log.setProcessInstanceId(task.getProcessInstanceId());
                log.setEscalationType(config.getEscalationType());
                log.setStatus(success ? "success" : "failed");
                triggerLogMapper.insert(log);

                // 更新配置的最后触发时间
                config.setLastTriggerTime(new Date());
                timeoutMapper.update(config);
            }
        }
    }

    /**
     * 计算从任务创建到现在经过的分钟数
     * @param startTime 任务创建时间
     * @param workdayOnly 是否只计算工作日
     */
    private long computeElapsedMinutes(Date startTime, boolean workdayOnly)
    {
        if (!workdayOnly)
        {
            return Duration.between(startTime.toInstant(), Instant.now()).toMinutes();
        }

        // 按工作日计算：跳过周末
        Calendar cal = Calendar.getInstance();
        cal.setTime(startTime);
        long minutes = 0;
        Date now = new Date();

        while (cal.getTime().before(now))
        {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            // 跳过周六(7)和周日(1)
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY)
            {
                minutes += 24 * 60;
            }
            cal.add(Calendar.DATE, 1);
        }

        // 减去今天多算的部分
        long overMinutes = Duration.between(now.toInstant(), cal.getTime().toInstant()).toMinutes();
        minutes -= overMinutes;

        return Math.max(0, minutes);
    }

    private boolean handleEscalation(Task task, WfNodeTimeout config)
    {
        try
        {
            String escalationType = config.getEscalationType();
            if (escalationType == null)
            {
                escalationType = "urge";
            }
            switch (escalationType)
            {
                case "urge":
                    if (task.getAssignee() != null)
                    {
                        Long assigneeId = sysUserMapper.selectUserIdByUserName(task.getAssignee());
                        if (assigneeId != null)
                        {
                            notificationMapper.insertNotification(
                                    assigneeId,
                                    "流程任务超时催办",
                                    "您的任务【" + task.getName() + "】已超时，请尽快处理",
                                    "wf_timeout_urge",
                                    "/workflow/task",
                                    task.getId());
                        }
                    }
                    break;
                case "auto_approve":
                    taskService.complete(task.getId());
                    break;
                case "auto_reject":
                    runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "超时自动驳回");
                    break;
                case "transfer":
                    if (config.getEscalationTarget() != null && !config.getEscalationTarget().isBlank())
                    {
                        taskService.setAssignee(task.getId(), config.getEscalationTarget());
                        Long targetId = sysUserMapper.selectUserIdByUserName(config.getEscalationTarget());
                        if (targetId != null)
                        {
                            notificationMapper.insertNotification(
                                    targetId,
                                    "超时任务转办",
                                    "任务【" + task.getName() + "】因超时转办给您",
                                    "wf_timeout_transfer",
                                    "/workflow/task",
                                    task.getId());
                        }
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
