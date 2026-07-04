package com.junsong.system.service;

/**
 * 工作台高优先级任务通知调度服务（R7-C）。
 *
 * 将工作台中 HIGH severity 的经营异常按用户授权去重写入通知中心。
 *
 * @author junsong
 */
public interface ISystemWorkbenchNotifierService
{
    /**
     * 扫描工作台 HIGH 优先级任务，按授权用户去重发送通知。
     *
     * @return 实际发送的通知条数（幂等跳过的不计入）
     */
    int notifyHighPriorityTasks();
}
