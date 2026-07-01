package com.junsong.member.task;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.junsong.member.mapper.MemMemberMapper;
import com.junsong.member.mapper.MemSeckillMapper;

/**
 * 会员生命周期定时任务
 * 每日凌晨执行：过期会员卡、关闭已结束秒杀活动
 *
 * @author junsong
 */
@Component
public class MemberLifecycleTask
{
    private static final Logger log = LoggerFactory.getLogger(MemberLifecycleTask.class);

    @Autowired
    private MemMemberMapper memberMapper;

    @Autowired
    private MemSeckillMapper seckillMapper;

    /**
     * 每日凌晨 1:00 执行会员生命周期清理
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void execute()
    {
        log.info("[MemberLifecycle] 开始执行会员生命周期定时任务");
        Date today = new Date();

        try
        {
            int expiredCards = expireMemberCards(today);
            log.info("[MemberLifecycle] 过期会员卡数: {}", expiredCards);
        }
        catch (Exception e)
        {
            log.error("[MemberLifecycle] 会员卡过期处理异常", e);
        }

        try
        {
            int closedSeckills = closeExpiredSeckills(today);
            log.info("[MemberLifecycle] 关闭已结束秒杀活动数: {}", closedSeckills);
        }
        catch (Exception e)
        {
            log.error("[MemberLifecycle] 秒杀活动关闭处理异常", e);
        }

        log.info("[MemberLifecycle] 会员生命周期定时任务执行完成");
    }

    /**
     * 过期会员卡：将 expire_date < today 且 status='0' 的会员置为 status='1'（失效）
     */
    public int expireMemberCards(Date today)
    {
        return memberMapper.expireMemberCards(today);
    }

    /**
     * 关闭已结束的秒杀活动：将 end_date < today 且 status='0' 的活动置为 status='1'（已结束）
     */
    public int closeExpiredSeckills(Date today)
    {
        return seckillMapper.closeExpiredSeckills(today);
    }
}
