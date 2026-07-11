package com.junsong.member.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.domain.MemGrowthRecord;
import com.junsong.member.domain.MemGrowthRule;
import com.junsong.member.domain.MemMember;
import com.junsong.member.domain.MemMemberCardType;
import com.junsong.member.domain.MemPointsRecord;
import com.junsong.member.domain.MemMemberSignIn;
import com.junsong.member.domain.MemMemberSignInBatch;
import com.junsong.member.mapper.MemGrowthRecordMapper;
import com.junsong.member.mapper.MemMemberMapper;
import com.junsong.member.mapper.MemMemberSignInBatchMapper;
import com.junsong.member.mapper.MemMemberSignInMapper;
import com.junsong.member.service.IMemberGrowthRuleService;
import com.junsong.member.service.IMemberGrowthService;
import com.junsong.member.service.IMemberGrowthService.GrowthAwardResult;
import com.junsong.member.service.IMemberLevelService;
import com.junsong.member.service.IMemPointsRecordService;
import com.junsong.member.service.IMemberSignInService;

/**
 * 会员签到Service实现
 *
 * @author junsong
 */
@Service
public class MemberSignInServiceImpl implements IMemberSignInService
{
    @Autowired
    private MemMemberSignInMapper signInMapper;

    @Autowired
    private MemMemberMapper memberMapper;

    @Autowired
    private IMemberGrowthService growthService;

    @Autowired
    private IMemberGrowthRuleService growthRuleService;

    @Autowired
    private IMemberLevelService levelService;

    @Autowired
    private MemMemberSignInBatchMapper batchMapper;

    @Autowired
    private IMemPointsRecordService pointsRecordService;

    @Autowired
    private MemGrowthRecordMapper growthRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> signIn(Long memberId, Date signDate, String operator)
    {
        // 1. 查询会员
        MemMember member = memberMapper.selectMemMemberByMemberId(memberId);
        if (member == null || !"0".equals(member.getStatus()))
        {
            throw new RuntimeException("会员不存在或状态异常");
        }

        // 2. 签到日期为空时默认当天，统一截断到天（避免带时分秒与 DATE 字段等值匹配失败）
        Date actualSignDate = truncateToDate(signDate != null ? signDate : new Date());

        // 3. 实时签到不允许传未来日期
        Date today = truncateToDate(new Date());
        if (actualSignDate.after(today))
        {
            throw new RuntimeException("实时签到不允许传未来日期");
        }

        // 4. 校验该日期是否已签到
        MemMemberSignIn existSignIn = signInMapper.selectTodaySignIn(memberId, actualSignDate);
        if (existSignIn != null)
        {
            throw new RuntimeException("该日期已签到");
        }

        // 5. 计算连续天数（基于签到日期的前一天）
        Calendar cal = Calendar.getInstance();
        cal.setTime(actualSignDate);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = truncateToDate(cal.getTime());
        MemMemberSignIn yesterdaySignIn = signInMapper.selectYesterdaySignIn(memberId, yesterday);
        int continuousDays = yesterdaySignIn != null ? yesterdaySignIn.getContinuousDays() + 1 : 1;

        // 6. 调用成长服务入账（含积分、成长值、等级检查、幂等）
        GrowthAwardResult result = growthService.awardSignInGrowth(memberId, actualSignDate, operator);

        // 7. 计算签到积分（优先等级 sign_in_points，兜底规则 sign_in_points）和成长值（规则 sign_in_growth）
        MemGrowthRule rule = growthRuleService.getGrowthRule();
        BigDecimal pointsEarned = rule != null && rule.getSignInPoints() != null
                ? rule.getSignInPoints() : new BigDecimal("10.00");
        MemMemberCardType currentLevel = levelService.selectLevelByTypeCode(member.getCardType());
        if (currentLevel != null && currentLevel.getSignInPoints() != null)
        {
            pointsEarned = currentLevel.getSignInPoints();
        }
        Long growthEarned = rule != null && rule.getSignInGrowth() != null ? rule.getSignInGrowth() : 5L;

        // 8. 写签到记录
        MemMemberSignIn signIn = new MemMemberSignIn();
        signIn.setTenantId(1L);
        signIn.setDeptId(member.getDeptId());
        signIn.setMemberId(memberId);
        signIn.setMemberNo(member.getMemberNo());
        signIn.setMemberName(member.getMemberName());
        signIn.setSignDate(actualSignDate);
        signIn.setContinuousDays(continuousDays);
        signIn.setPointsEarned(pointsEarned);
        signIn.setGrowthEarned(growthEarned);
        signIn.setSignType("REALTIME");
        signIn.setRewardLevelCode(member.getCardType());
        signIn.setCreateBy(operator);
        signInMapper.insertSignIn(signIn);

        // 9. 构建签到响应（包含设计文档要求的全部字段）
        String rewardLevelCode = member.getCardType();
        String currentLevelName = result.afterLevel;
        MemMemberCardType afterLevelObj = levelService.selectLevelByTypeCode(result.afterLevel);
        if (afterLevelObj != null && afterLevelObj.getTypeName() != null)
        {
            currentLevelName = afterLevelObj.getTypeName();
        }
        SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Object> data = new HashMap<>();
        data.put("signDate", dateSdf.format(actualSignDate));
        data.put("continuousDays", continuousDays);
        data.put("rewardLevel", rewardLevelCode);
        data.put("pointsEarned", pointsEarned);
        data.put("growthEarned", result.growthChange);
        data.put("growthValue", result.balance);
        data.put("currentLevel", result.afterLevel);
        data.put("currentLevelName", currentLevelName);
        data.put("levelChanged", result.levelChanged);
        data.put("beforeLevel", result.beforeLevel);
        data.put("afterLevel", result.afterLevel);
        return data;
    }

    /**
     * 批量补录签到
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> backfillSignIn(Long memberId, String targetMonth, String fillMode,
                                               List<String> signDates, Integer signCount,
                                               String remark, String operator)
    {
        // 1. 校验会员存在
        MemMember member = memberMapper.selectMemMemberByMemberId(memberId);
        if (member == null || !"0".equals(member.getStatus()))
        {
            throw new RuntimeException("会员不存在或状态异常");
        }

        // 2. 校验 targetMonth 格式 yyyy-MM，不能晚于当前月份
        if (targetMonth == null || targetMonth.isEmpty())
        {
            throw new RuntimeException("目标月份不能为空");
        }
        SimpleDateFormat monthSdf = new SimpleDateFormat("yyyy-MM");
        Date monthDate;
        try
        {
            monthDate = monthSdf.parse(targetMonth);
        }
        catch (Exception e)
        {
            throw new RuntimeException("月份格式错误，应为 yyyy-MM");
        }
        Calendar nowCal = Calendar.getInstance();
        Calendar monthCal = Calendar.getInstance();
        monthCal.setTime(monthDate);
        // 重置日为1，比较年月
        if (monthCal.get(Calendar.YEAR) > nowCal.get(Calendar.YEAR)
            || (monthCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
                && monthCal.get(Calendar.MONTH) > nowCal.get(Calendar.MONTH)))
        {
            throw new RuntimeException("目标月份不能晚于当前月份");
        }

        // 3. 读取会员当前等级，确定 rewardLevelCode、pointsPerSign、growthPerSign
        String rewardLevelCode = member.getCardType();
        MemGrowthRule rule = growthRuleService.getGrowthRule();
        BigDecimal pointsPerSign = rule != null && rule.getSignInPoints() != null
                ? rule.getSignInPoints() : new BigDecimal("10.00");
        Long growthPerSign = rule != null && rule.getSignInGrowth() != null ? rule.getSignInGrowth() : 5L;
        MemMemberCardType currentLevel = levelService.selectLevelByTypeCode(rewardLevelCode);
        if (currentLevel != null && currentLevel.getSignInPoints() != null)
        {
            pointsPerSign = currentLevel.getSignInPoints();
        }

        // 计算 targetMonth 的起止日期
        Calendar beginCal = Calendar.getInstance();
        beginCal.setTime(monthDate);
        beginCal.set(Calendar.DAY_OF_MONTH, 1);
        Date beginDate = beginCal.getTime();
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(monthDate);
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        // 不能晚于今天
        Date today = truncateToDate(new Date());
        if (endCal.getTime().after(today))
        {
            endCal.setTime(today);
        }
        Date endDate = endCal.getTime();

        // 查询已签到日期
        List<Date> signedDateList = signInMapper.selectSignedDates(memberId, beginDate, endDate);
        Set<String> signedSet = new HashSet<>();
        SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd");
        for (Date d : signedDateList)
        {
            signedSet.add(dateSdf.format(d));
        }

        // 4. 根据模式确定要补录的日期
        List<String> filledDates = new ArrayList<>();
        List<String> skippedDates = new ArrayList<>();

        if ("SELECT_DATES".equals(fillMode))
        {
            if (signDates == null || signDates.isEmpty())
            {
                throw new RuntimeException("SELECT_DATES 模式下 signDates 不能为空");
            }
            // 去重升序
            Set<String> uniqueDates = new LinkedHashSet<>(signDates);
            List<String> sortedDates = new ArrayList<>(uniqueDates);
            Collections.sort(sortedDates);
            for (String d : sortedDates)
            {
                // 校验日期属于 targetMonth
                String monthPart = d.length() >= 7 ? d.substring(0, 7) : "";
                if (!targetMonth.equals(monthPart))
                {
                    throw new RuntimeException("日期 " + d + " 不属于目标月份 " + targetMonth);
                }
                // 过滤已签到
                if (signedSet.contains(d))
                {
                    skippedDates.add(d);
                }
                else
                {
                    filledDates.add(d);
                }
            }
        }
        else if ("COUNT_ONLY".equals(fillMode))
        {
            if (signCount == null || signCount <= 0)
            {
                throw new RuntimeException("COUNT_ONLY 模式下 signCount 必须为正整数");
            }
            // 按日期升序自动选未签到日期，actualCount 不超过可用天数
            List<String> availableDates = new ArrayList<>();
            Calendar cursor = Calendar.getInstance();
            cursor.setTime(beginDate);
            while (!cursor.getTime().after(endDate))
            {
                String d = dateSdf.format(cursor.getTime());
                if (!signedSet.contains(d))
                {
                    availableDates.add(d);
                }
                cursor.add(Calendar.DAY_OF_MONTH, 1);
            }
            int actual = Math.min(signCount, availableDates.size());
            for (int i = 0; i < actual; i++)
            {
                filledDates.add(availableDates.get(i));
            }
        }
        else
        {
            throw new RuntimeException("fillMode 只支持 SELECT_DATES 或 COUNT_ONLY");
        }

        // 5. 计算 requestedCount / totalPoints / totalGrowth（插入批次前必须赋值，字段 NOT NULL）
        int requestedCount;
        if ("SELECT_DATES".equals(fillMode))
        {
            requestedCount = signDates != null ? signDates.size() : 0;
        }
        else
        {
            requestedCount = signCount != null ? signCount : 0;
        }
        BigDecimal totalPoints = pointsPerSign.multiply(new BigDecimal(filledDates.size()));
        Long totalGrowth = growthPerSign * filledDates.size();

        // 6. 创建批次记录
        MemMemberSignInBatch batch = new MemMemberSignInBatch();
        batch.setTenantId(1L);
        batch.setDeptId(member.getDeptId());
        batch.setMemberId(memberId);
        batch.setMemberNo(member.getMemberNo());
        batch.setMemberName(member.getMemberName());
        batch.setTargetMonth(targetMonth);
        batch.setFillMode(fillMode);
        batch.setRequestedCount(requestedCount);
        batch.setActualCount(filledDates.size());
        batch.setSelectedDates(String.join(",", filledDates));
        batch.setRewardLevelCode(rewardLevelCode);
        batch.setPointsPerSign(pointsPerSign);
        batch.setGrowthPerSign(growthPerSign);
        batch.setTotalPoints(totalPoints);
        batch.setTotalGrowth(totalGrowth);
        batch.setRemark(remark);
        batch.setCreateBy(operator);
        batchMapper.insertBatch(batch);
        Long batchId = batch.getBatchId();

        // 6. 逐条插入签到明细（sign_type=BACKFILL, batch_id=批次ID）
        for (String d : filledDates)
        {
            MemMemberSignIn signIn = new MemMemberSignIn();
            signIn.setTenantId(1L);
            signIn.setDeptId(member.getDeptId());
            signIn.setMemberId(memberId);
            signIn.setMemberNo(member.getMemberNo());
            signIn.setMemberName(member.getMemberName());
            try
            {
                signIn.setSignDate(dateSdf.parse(d));
            }
            catch (Exception e)
            {
                throw new RuntimeException("日期格式错误: " + d);
            }
            signIn.setContinuousDays(1);
            signIn.setPointsEarned(pointsPerSign);
            signIn.setGrowthEarned(growthPerSign);
            signIn.setBatchId(batchId);
            signIn.setSignType("BACKFILL");
            signIn.setRewardLevelCode(rewardLevelCode);
            signIn.setCreateBy(operator);
            signInMapper.insertSignIn(signIn);
        }

        // 7. 汇总发奖（totalPoints/totalGrowth 已在步骤 5 算好）
        GrowthAwardResult growthResult = null;
        if (filledDates.size() > 0)
        {
            growthResult = growthService.awardSignInBackfillGrowth(memberId, batchId, totalPoints,
                    totalGrowth, rewardLevelCode, operator);
        }
        else
        {
            totalPoints = BigDecimal.ZERO;
            totalGrowth = 0L;
        }

        // 8. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("targetMonth", targetMonth);
        result.put("fillMode", fillMode);
        result.put("rewardLevelCode", rewardLevelCode);
        result.put("pointsPerSign", pointsPerSign);
        result.put("growthPerSign", growthPerSign);
        result.put("actualCount", filledDates.size());
        result.put("filledDates", filledDates);
        result.put("skippedDates", skippedDates);
        result.put("totalPoints", totalPoints);
        result.put("totalGrowth", totalGrowth);
        if (growthResult != null)
        {
            result.put("growthValue", growthResult.balance);
            result.put("beforeLevel", growthResult.beforeLevel);
            result.put("afterLevel", growthResult.afterLevel);
            result.put("levelChanged", growthResult.levelChanged);
        }
        return result;
    }

    @Override
    public MemMemberSignIn getTodaySignIn(Long memberId)
    {
        return signInMapper.selectTodaySignIn(memberId, truncateToDate(new Date()));
    }

    @Override
    public List<MemMemberSignIn> selectSignInList(MemMemberSignIn signIn)
    {
        return signInMapper.selectSignInList(signIn);
    }

    @Override
    public List<MemMemberSignIn> selectMonthlyCalendar(Long memberId, String month)
    {
        try
        {
            // month 格式: yyyy-MM
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
            Date monthDate = sdf.parse(month);
            Calendar cal = Calendar.getInstance();
            cal.setTime(monthDate);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date beginDate = cal.getTime();
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            Date endDate = cal.getTime();
            return signInMapper.selectMonthlyCalendar(memberId, beginDate, endDate);
        }
        catch (Exception e)
        {
            throw new RuntimeException("月份格式错误，应为 yyyy-MM");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSignInByIds(Long[] signIds, String operator)
    {
        if (signIds == null || signIds.length == 0)
        {
            return 0;
        }

        int rows = 0;
        for (Long signId : signIds)
        {
            MemMemberSignIn signIn = signInMapper.selectSignInById(signId);
            if (signIn == null)
            {
                continue;
            }

            MemMember member = memberMapper.selectMemMemberByMemberId(signIn.getMemberId());
            if (member == null)
            {
                throw new RuntimeException("会员不存在，无法删除签到记录: " + signId);
            }

            BigDecimal pointsRollback = signIn.getPointsEarned() != null
                    ? signIn.getPointsEarned().negate() : BigDecimal.ZERO;
            Long growthRollback = signIn.getGrowthEarned() != null ? -signIn.getGrowthEarned() : 0L;
            String beforeLevel = member.getCardType();

            memberMapper.addPointsAndGrowth(signIn.getMemberId(), pointsRollback, growthRollback, operator);

            MemPointsRecord latest = pointsRecordService.selectLatestBalanceByMemberId(signIn.getMemberId());
            BigDecimal latestBalance = latest != null && latest.getBalance() != null
                    ? latest.getBalance() : BigDecimal.ZERO;
            MemPointsRecord pointsRecord = new MemPointsRecord();
            pointsRecord.setDeptId(signIn.getDeptId());
            pointsRecord.setMemberId(signIn.getMemberId());
            pointsRecord.setMemberNo(signIn.getMemberNo());
            pointsRecord.setMemberName(signIn.getMemberName());
            pointsRecord.setRecordType("4");
            pointsRecord.setPoints(pointsRollback);
            pointsRecord.setBalance(latestBalance.add(pointsRollback));
            pointsRecord.setRemark("删除签到记录冲回-签到ID" + signId);
            pointsRecord.setCreateBy(operator);
            pointsRecordService.insertMemPointsRecord(pointsRecord);

            MemMember updated = memberMapper.selectMemMemberByMemberId(signIn.getMemberId());
            Long newGrowth = updated.getGrowthValue();
            String targetLevel = levelService.calculateLevel(newGrowth);
            if (!targetLevel.equals(beforeLevel))
            {
                memberMapper.updateMemberLevel(signIn.getMemberId(), targetLevel, operator);
            }

            MemGrowthRecord growthRecord = new MemGrowthRecord();
            growthRecord.setTenantId(1L);
            growthRecord.setDeptId(signIn.getDeptId());
            growthRecord.setMemberId(signIn.getMemberId());
            growthRecord.setMemberNo(signIn.getMemberNo());
            growthRecord.setMemberName(signIn.getMemberName());
            growthRecord.setSourceType("SIGN_IN_DELETE");
            growthRecord.setSourceId(signId);
            growthRecord.setDedupKey("SIGN_IN_DELETE:" + signId);
            growthRecord.setGrowthChange(growthRollback);
            growthRecord.setBalance(newGrowth);
            growthRecord.setBeforeLevel(beforeLevel);
            growthRecord.setAfterLevel(targetLevel);
            growthRecord.setRemark("删除签到记录冲回");
            growthRecord.setCreateBy(operator);
            growthRecordMapper.insertGrowthRecord(growthRecord);

            if (signIn.getBatchId() != null)
            {
                updateBackfillBatchAfterDelete(signIn);
            }

            rows += signInMapper.deleteSignInById(signId);
        }
        return rows;
    }

    private void updateBackfillBatchAfterDelete(MemMemberSignIn signIn)
    {
        MemMemberSignInBatch batch = batchMapper.selectBatchById(signIn.getBatchId());
        if (batch == null)
        {
            return;
        }

        BigDecimal points = signIn.getPointsEarned() != null ? signIn.getPointsEarned() : BigDecimal.ZERO;
        Long growth = signIn.getGrowthEarned() != null ? signIn.getGrowthEarned() : 0L;
        Integer actualCount = batch.getActualCount() != null ? batch.getActualCount() : 0;
        BigDecimal totalPoints = batch.getTotalPoints() != null ? batch.getTotalPoints() : BigDecimal.ZERO;
        Long totalGrowth = batch.getTotalGrowth() != null ? batch.getTotalGrowth() : 0L;

        batch.setActualCount(Math.max(0, actualCount - 1));
        batch.setTotalPoints(totalPoints.subtract(points).max(BigDecimal.ZERO));
        batch.setTotalGrowth(Math.max(0L, totalGrowth - growth));

        if (batch.getSelectedDates() != null && signIn.getSignDate() != null)
        {
            String deletedDate = new SimpleDateFormat("yyyy-MM-dd").format(signIn.getSignDate());
            List<String> dates = new ArrayList<>();
            for (String date : batch.getSelectedDates().split(","))
            {
                if (!deletedDate.equals(date))
                {
                    dates.add(date);
                }
            }
            batch.setSelectedDates(String.join(",", dates));
        }

        batchMapper.updateBatch(batch);
    }

    /**
     * 将日期截断到天（去除时分秒）
     */
    private Date truncateToDate(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 签到预览：查询会员当前等级名称、单次签到积分、单次签到成长值
     */
    @Override
    public Map<String, Object> previewSignIn(Long memberId)
    {
        MemMember member = memberMapper.selectMemMemberByMemberId(memberId);
        if (member == null || !"0".equals(member.getStatus()))
        {
            throw new RuntimeException("会员不存在或状态异常");
        }

        MemGrowthRule rule = growthRuleService.getGrowthRule();
        BigDecimal pointsPerSign = rule != null && rule.getSignInPoints() != null
                ? rule.getSignInPoints() : new BigDecimal("1.00");
        Long growthPerSign = rule != null && rule.getSignInGrowth() != null ? rule.getSignInGrowth() : 5L;

        String levelName = member.getCardType();
        MemMemberCardType currentLevel = levelService.selectLevelByTypeCode(member.getCardType());
        if (currentLevel != null)
        {
            if (currentLevel.getTypeName() != null)
            {
                levelName = currentLevel.getTypeName();
            }
            if (currentLevel.getSignInPoints() != null)
            {
                pointsPerSign = currentLevel.getSignInPoints();
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("cardType", member.getCardType());
        data.put("levelName", levelName);
        data.put("pointsPerSign", pointsPerSign);
        data.put("growthPerSign", growthPerSign);
        return data;
    }
}
