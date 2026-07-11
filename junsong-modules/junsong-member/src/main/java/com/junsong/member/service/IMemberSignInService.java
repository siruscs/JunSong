package com.junsong.member.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.junsong.member.domain.MemMemberSignIn;

/**
 * 会员签到Service接口
 *
 * @author junsong
 */
public interface IMemberSignInService
{
    /**
     * 会员签到
     *
     * @param memberId 会员ID
     * @param signDate 签到日期（为空时默认当天；实时签到不允许传未来日期）
     * @param operator 操作人
     * @return 签到结果（含 signDate, continuousDays, rewardLevel, pointsEarned, growthEarned,
     *         growthValue, currentLevel, currentLevelName, levelChanged, beforeLevel, afterLevel）
     */
    public Map<String, Object> signIn(Long memberId, Date signDate, String operator);

    /**
     * 批量补录签到
     *
     * @param memberId 会员ID
     * @param targetMonth 目标月份 yyyy-MM，不能晚于当前月份
     * @param fillMode 补录模式 SELECT_DATES / COUNT_ONLY
     * @param signDates SELECT_DATES 模式下选择的日期列表（yyyy-MM-dd）
     * @param signCount COUNT_ONLY 模式下请求补录次数
     * @param remark 备注
     * @param operator 操作人
     * @return 补录结果（batchId, actualCount, filledDates, skippedDates, totalPoints, totalGrowth 等）
     */
    public Map<String, Object> backfillSignIn(Long memberId, String targetMonth, String fillMode,
                                               List<String> signDates, Integer signCount,
                                               String remark, String operator);

    /**
     * 查询今日签到状态
     */
    public MemMemberSignIn getTodaySignIn(Long memberId);

    /**
     * 查询签到记录列表
     */
    public List<MemMemberSignIn> selectSignInList(MemMemberSignIn signIn);

    /**
     * 查询月签到日历
     */
    public List<MemMemberSignIn> selectMonthlyCalendar(Long memberId, String month);

    /**
     * 删除签到/补签到记录，并冲回对应积分和成长值
     */
    public int deleteSignInByIds(Long[] signIds, String operator);

    /**
     * 签到预览：查询会员当前等级名称、单次签到积分、单次签到成长值
     * 用于补录弹窗提交前展示预估信息
     *
     * @param memberId 会员ID
     * @return {cardType, levelName, pointsPerSign, growthPerSign}
     */
    public Map<String, Object> previewSignIn(Long memberId);
}
