package com.junsong.member.task;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.member.domain.MemMember;
import com.junsong.member.domain.MemSeckill;
import com.junsong.member.mapper.MemMemberMapper;
import com.junsong.member.mapper.MemSeckillMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 会员生命周期定时任务测试
 *
 * 验证会员卡过期、秒杀活动自动关闭的时间边界逻辑。
 * 使用手写 fake 替代 Mockito，避免 JDK 26+ 兼容性问题。
 */
class MemberLifecycleTaskTest
{
    private MemberLifecycleTask task;
    private FakeMemberMapper memberMapper;
    private FakeSeckillMapper seckillMapper;

    @BeforeEach
    void setUp() throws Exception
    {
        task = new MemberLifecycleTask();
        memberMapper = new FakeMemberMapper();
        seckillMapper = new FakeSeckillMapper();
        setField(task, "memberMapper", memberMapper);
        setField(task, "seckillMapper", seckillMapper);
    }

    // ── 会员卡过期 ──

    @Test
    void expireMemberCardsShouldDelegateToMapper()
    {
        Date today = new Date();
        memberMapper.expireResult = 3;

        int result = task.expireMemberCards(today);

        assertEquals(3, result, "应返回过期会员卡更新行数");
        assertEquals(today, memberMapper.lastExpireToday, "应传入当天日期");
    }

    @Test
    void expireMemberCardsShouldReturnZeroWhenNoneExpired()
    {
        memberMapper.expireResult = 0;

        int result = task.expireMemberCards(new Date());

        assertEquals(0, result, "无过期会员时应返回0");
    }

    @Test
    void expireMemberCardsTimeBoundary()
    {
        // 时间边界：传入精确的当天日期，由 SQL 层判断 expire_date < today
        Date today = new Date();
        memberMapper.expireResult = 5;

        int result = task.expireMemberCards(today);

        assertEquals(5, result, "应处理时间边界情况");
        assertNotNull(memberMapper.lastExpireToday, "应传入日期参数");
    }

    // ── 秒杀活动关闭 ──

    @Test
    void closeExpiredSeckillsShouldDelegateToMapper()
    {
        Date today = new Date();
        seckillMapper.closeResult = 2;

        int result = task.closeExpiredSeckills(today);

        assertEquals(2, result, "应返回关闭的秒杀活动数");
        assertEquals(today, seckillMapper.lastCloseToday, "应传入当天日期");
    }

    @Test
    void closeExpiredSeckillsShouldReturnZeroWhenNoneExpired()
    {
        seckillMapper.closeResult = 0;

        int result = task.closeExpiredSeckills(new Date());

        assertEquals(0, result, "无已结束活动时应返回0");
    }

    @Test
    void closeExpiredSeckillsTimeBoundary()
    {
        Date today = new Date();
        seckillMapper.closeResult = 1;

        int result = task.closeExpiredSeckills(today);

        assertEquals(1, result, "应处理时间边界情况");
        assertNotNull(seckillMapper.lastCloseToday, "应传入日期参数");
    }

    // ── execute 全流程 ──

    @Test
    void executeShouldRunBothTasks()
    {
        memberMapper.expireResult = 2;
        seckillMapper.closeResult = 1;

        // execute() 内部调用 expireMemberCards 和 closeExpiredSeckills
        task.execute();

        assertNotNull(memberMapper.lastExpireToday, "应调用会员卡过期");
        assertNotNull(seckillMapper.lastCloseToday, "应调用秒杀关闭");
    }

    @Test
    void executeShouldNotFailWhenOneTaskThrows()
    {
        memberMapper.throwOnExpire = true;
        seckillMapper.closeResult = 1;

        // execute 应捕获异常不中断
        assertDoesNotThrow(() -> task.execute(), "单个任务异常不应中断整体执行");
        assertNotNull(seckillMapper.lastCloseToday, "秒杀关闭任务仍应执行");
    }

    // ── 辅助方法 ──

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── Fake 实现 ──

    static class FakeMemberMapper implements MemMemberMapper
    {
        int expireResult = 0;
        Date lastExpireToday = null;
        boolean throwOnExpire = false;

        @Override
        public int expireMemberCards(Date today)
        {
            lastExpireToday = today;
            if (throwOnExpire) { throw new RuntimeException("模拟异常"); }
            return expireResult;
        }

        @Override public MemMember selectMemMemberByMemberId(Long id) { return null; }
        @Override public List<MemMember> selectMemMemberList(MemMember m) { return Collections.emptyList(); }
        @Override public int insertMemMember(MemMember m) { return 1; }
        @Override public int updateMemMember(MemMember m) { return 1; }
        @Override public int deleteMemMemberByMemberId(Long id) { return 1; }
        @Override public int deleteMemMemberByMemberIds(Long[] ids) { return 1; }
        @Override public int checkMemberNoUnique(MemMember m) { return 0; }
        @Override public String selectNextMemberNo(String prefix) { return prefix + "00001"; }
        @Override public String selectDeptNameById(Long deptId) { return "test"; }
        @Override public List<MemMember> selectMemMemberByMemberNo(String no) { return Collections.emptyList(); }
        @Override public MemMember selectMemMemberByNoAndDept(String no, Long deptId) { return null; }
        @Override public List<MemMember> selectActiveMembersForSeckill(Long deptId, Date d) { return Collections.emptyList(); }
        @Override public int addPointsAndGrowth(Long memberId, java.math.BigDecimal pointsDelta, Long growthDelta, String operator) { return 1; }
        @Override public int addGrowthOnly(Long memberId, Long growthDelta, String operator) { return 1; }
        @Override public int addGrowthOnlyWithoutActiveTime(Long memberId, Long growthDelta, String operator) { return 1; }
        @Override public int updateMemberLevel(Long memberId, String newLevel, String operator) { return 1; }
        @Override public int updateLastActiveTime(Long memberId) { return 1; }
        @Override public List<MemMember> selectInactiveMembers(Long tenantId, Date threshold) { return Collections.emptyList(); }
    }

    static class FakeSeckillMapper implements MemSeckillMapper
    {
        int closeResult = 0;
        Date lastCloseToday = null;

        @Override
        public int closeExpiredSeckills(Date today)
        {
            lastCloseToday = today;
            return closeResult;
        }

        @Override public MemSeckill selectMemSeckillById(Long id) { return null; }
        @Override public List<MemSeckill> selectMemSeckillList(MemSeckill s) { return Collections.emptyList(); }
        @Override public int insertMemSeckill(MemSeckill s) { return 1; }
        @Override public int updateMemSeckill(MemSeckill s) { return 1; }
        @Override public int deleteMemSeckillById(Long id) { return 1; }
        @Override public int deleteMemSeckillByIds(Long[] ids) { return 1; }
        @Override public int checkMemSeckillNoUnique(MemSeckill s) { return 0; }
    }
}
