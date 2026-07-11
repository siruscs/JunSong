package com.junsong.member.task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.junsong.member.domain.MemGrowthRule;
import com.junsong.member.domain.MemMember;
import com.junsong.member.mapper.MemGrowthRuleMapper;
import com.junsong.member.mapper.MemMemberMapper;
import com.junsong.member.service.IMemberGrowthService;

/**
 * 会员成长值衰减定时任务
 * 每天 02:00 执行：扫描不活跃会员，按租户规则衰减成长值并重算等级
 *
 * 流程：
 * 1. 遍历 mem_growth_rule 中 decay_enabled='1' 的租户规则
 * 2. 查询 last_active_time 超过 inactive_days 阈值且 growth_value > 0 的会员
 * 3. 计算 decayAmount = floor(growth_value * decay_ratio)
 * 4. 调用 growthService.decayGrowth 扣减并写流水（dedup_key=DECAY:{memberId}:{date} 幂等）
 * 5. 单会员异常不影响其他会员
 *
 * 注意：本任务按 tenantId 显式过滤不活跃会员，支持多租户安全。
 *
 * @author junsong
 */
@Component
public class MemberGrowthDecayTask
{
    private static final Logger log = LoggerFactory.getLogger(MemberGrowthDecayTask.class);

    @Autowired
    private MemGrowthRuleMapper growthRuleMapper;

    @Autowired
    private MemMemberMapper memberMapper;

    @Autowired
    private IMemberGrowthService growthService;

    /**
     * 每天 02:00 执行成长值衰减
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void execute()
    {
        log.info("[MemberGrowthDecay] 开始执行成长值衰减定时任务");
        Date now = new Date();

        List<MemGrowthRule> rules = growthRuleMapper.selectDecayEnabledRules();
        if (rules == null || rules.isEmpty())
        {
            log.info("[MemberGrowthDecay] 无启用衰减的租户规则，任务结束");
            return;
        }

        int totalScanned = 0;
        int totalSuccess = 0;
        int totalSkipped = 0;
        int totalFailed = 0;

        for (MemGrowthRule rule : rules)
        {
            Long tenantId = rule.getTenantId();
            int inactiveDays = rule.getInactiveDays() != null ? rule.getInactiveDays() : 180;
            BigDecimal decayRatio = rule.getDecayRatio() != null ? rule.getDecayRatio() : new BigDecimal("0.50");

            Date threshold = computeThreshold(now, inactiveDays);
            log.info("[MemberGrowthDecay] 处理租户 tenantId={}, inactiveDays={}, decayRatio={}, threshold={}",
                    tenantId, inactiveDays, decayRatio, threshold);

            List<MemMember> inactiveMembers = memberMapper.selectInactiveMembers(tenantId, threshold);
            if (inactiveMembers == null || inactiveMembers.isEmpty())
            {
                log.info("[MemberGrowthDecay] 租户 {} 无不活跃会员", tenantId);
                continue;
            }

            for (MemMember member : inactiveMembers)
            {
                totalScanned++;
                try
                {
                    Long growthValue = member.getGrowthValue() != null ? member.getGrowthValue() : 0L;
                    if (growthValue <= 0)
                    {
                        totalSkipped++;
                        continue;
                    }

                    // decayAmount = floor(growth_value * decay_ratio)
                    long decayAmount = new BigDecimal(growthValue)
                            .multiply(decayRatio)
                            .setScale(0, RoundingMode.FLOOR)
                            .longValue();

                    if (decayAmount <= 0)
                    {
                        totalSkipped++;
                        continue;
                    }

                    boolean success = growthService.decayGrowth(member.getMemberId(), decayAmount, "system");
                    if (success)
                    {
                        totalSuccess++;
                    }
                    else
                    {
                        totalSkipped++;
                    }
                }
                catch (Exception e)
                {
                    totalFailed++;
                    log.error("[MemberGrowthDecay] 会员 {} 衰减失败: {}", member.getMemberId(), e.getMessage(), e);
                }
            }
        }

        log.info("[MemberGrowthDecay] 任务完成: 扫描={}, 成功={}, 跳过={}, 失败={}",
                totalScanned, totalSuccess, totalSkipped, totalFailed);
    }

    /**
     * 计算不活跃截止时间 = now - inactiveDays 天
     */
    private Date computeThreshold(Date now, int inactiveDays)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DAY_OF_MONTH, -inactiveDays);
        return cal.getTime();
    }
}
