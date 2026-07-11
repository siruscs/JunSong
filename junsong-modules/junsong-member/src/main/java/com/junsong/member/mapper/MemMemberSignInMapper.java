package com.junsong.member.mapper;

import java.util.Date;
import java.util.List;
import com.junsong.member.domain.MemMemberSignIn;
import org.apache.ibatis.annotations.Param;

/**
 * 会员签到记录Mapper接口
 *
 * @author junsong
 */
public interface MemMemberSignInMapper
{
    /**
     * 查询签到记录列表
     */
    public List<MemMemberSignIn> selectSignInList(MemMemberSignIn signIn);

    /**
     * 新增签到记录
     */
    public int insertSignIn(MemMemberSignIn signIn);

    /**
     * 根据签到ID查询签到记录
     */
    public MemMemberSignIn selectSignInById(Long signId);

    /**
     * 删除签到记录
     */
    public int deleteSignInById(Long signId);

    /**
     * 查询会员今日是否已签到
     */
    public MemMemberSignIn selectTodaySignIn(@Param("memberId") Long memberId, @Param("signDate") Date signDate);

    /**
     * 查询会员昨日签到记录（计算连续天数）
     */
    public MemMemberSignIn selectYesterdaySignIn(@Param("memberId") Long memberId, @Param("signDate") Date signDate);

    /**
     * 查询会员月签到日历
     */
    public List<MemMemberSignIn> selectMonthlyCalendar(@Param("memberId") Long memberId, @Param("beginDate") Date beginDate, @Param("endDate") Date endDate);

    /**
     * 查询某月已签到日期列表（用于补录时过滤已签到日期）
     *
     * @param memberId 会员ID
     * @param beginDate 开始日期
     * @param endDate 结束日期
     * @return 已签到日期列表（升序）
     */
    public List<Date> selectSignedDates(@Param("memberId") Long memberId, @Param("beginDate") Date beginDate, @Param("endDate") Date endDate);
}
