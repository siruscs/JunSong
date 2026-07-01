package com.junsong.system.task;

import java.util.Calendar;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.junsong.system.mapper.SysLogininforMapper;
import com.junsong.system.mapper.SysOperLogMapper;

/**
 * 日志定时清理任务
 * 每天凌晨 2 点自动清理超过 90 天的登录日志和操作日志
 *
 * @author junsong
 */
@Component
public class LogCleanupTask
{
    private static final Logger log = LoggerFactory.getLogger(LogCleanupTask.class);

    private static final int RETENTION_DAYS = 90;

    @Autowired
    private SysLogininforMapper logininforMapper;

    @Autowired
    private SysOperLogMapper operLogMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredLogs()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -RETENTION_DAYS);
        Date expireDate = calendar.getTime();

        try
        {
            int loginCount = logininforMapper.deleteLogininforBeforeExpire(expireDate);
            log.info("定时清理登录日志完成，删除 {} 条过期记录", loginCount);
        }
        catch (Exception e)
        {
            log.error("定时清理登录日志异常", e);
        }

        try
        {
            int operCount = operLogMapper.deleteOperLogBeforeExpire(expireDate);
            log.info("定时清理操作日志完成，删除 {} 条过期记录", operCount);
        }
        catch (Exception e)
        {
            log.error("定时清理操作日志异常", e);
        }
    }
}
