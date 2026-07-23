package com.junsong.system.service.signal;

/**
 * 经营任务信号生成器接口。
 * 每个实现负责一种来源的信号扫描与经营任务投递。
 * 生成器必须：
 * - 幂等：同一来源重复执行不创建重复任务
 * - 安全：失败记录 WARN 日志，不抛异常阻断其他生成器
 * - 隔离：按当前租户和授权部门范围扫描
 *
 * @author junsong
 */
public interface OperatingTaskSignalGenerator
{
    /**
     * 生成器标识（如 "OVERDUE_RECEIVABLE"）
     */
    String generatorCode();

    /**
     * 执行信号扫描与任务投递
     *
     * @return 本轮新创建的任务数（已存在的幂等命中不算）
     */
    int generate();
}
