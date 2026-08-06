package com.junsong.member.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.domain.MemGrowthRecord;
import com.junsong.member.domain.MemGrowthRule;
import com.junsong.member.domain.MemMember;
import com.junsong.member.domain.MemPointsRecord;
import com.junsong.member.mapper.MemGrowthRecordMapper;
import com.junsong.member.mapper.MemMemberMapper;
import com.junsong.member.service.IMemberGrowthRuleService;
import com.junsong.member.service.IMemberGrowthService;
import com.junsong.member.service.IMemberLevelService;
import com.junsong.member.service.IMemPointsRecordService;
import com.junsong.member.domain.MemMemberCardType;

/**
 * 会员成长值核心Service实现
 * 负责消费奖励、签到奖励、手工调整、衰减扣减、等级重算
 *
 * @author junsong
 */
@Service
public class MemberGrowthServiceImpl implements IMemberGrowthService
{
    private static final Logger log = LoggerFactory.getLogger(MemberGrowthServiceImpl.class);

    @Autowired
    private MemMemberMapper memberMapper;

    @Autowired
    private MemGrowthRecordMapper growthRecordMapper;

    @Autowired
    private IMemberGrowthRuleService growthRuleService;

    @Autowired
    private IMemberLevelService levelService;

    @Autowired
    private IMemPointsRecordService pointsRecordService;

    /**
     * 消费奖励入账
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean awardSaleGrowth(Long memberId, String memberNo, String memberName, Long deptId,
                                   Long saleId, BigDecimal saleAmount, String operator)
    {
        String dedupKey = "SALE:" + saleId;

        // 1. 幂等检查
        MemGrowthRecord existing = growthRecordMapper.selectByDedupKey(dedupKey);
        if (existing != null)
        {
            log.info("消费奖励已处理，跳过: memberId={}, saleId={}", memberId, saleId);
            return false;
        }

        // 2. 查询会员
        MemMember member = memberMapper.selectMemMemberByMemberId(memberId);
        if (member == null || !"0".equals(member.getStatus()))
        {
            log.warn("会员不存在或状态异常: memberId={}", memberId);
            return false;
        }

        // 3. 读取成长规则
        MemGrowthRule rule = growthRuleService.getGrowthRule();

        // 4. 计算成长值
        long growthEarned = 0;
        if (saleAmount != null && saleAmount.compareTo(BigDecimal.ZERO) > 0)
        {
            BigDecimal growth = saleAmount.multiply(rule.getSaleGrowthRatio());
            growthEarned = growth.setScale(0, java.math.RoundingMode.FLOOR).longValue();
        }

        if (growthEarned <= 0)
        {
            log.info("消费金额为0或成长值为0，跳过: memberId={}, saleAmount={}", memberId, saleAmount);
            return false;
        }

        // 5. 原子更新会员成长值和活跃时间（积分由现有积分规则处理，这里只加成长值）
        memberMapper.addGrowthOnly(memberId, growthEarned, operator);

        // 6. 重新查询最新成长值
        MemMember updated = memberMapper.selectMemMemberByMemberId(memberId);
        Long newGrowth = updated.getGrowthValue();
        String beforeLevel = updated.getCardType();

        // 7. 检查并更新等级
        String targetLevel = levelService.calculateLevel(newGrowth);
        if (!targetLevel.equals(beforeLevel))
        {
            memberMapper.updateMemberLevel(memberId, targetLevel, operator);
        }

        // 8. 写成长值流水
        MemGrowthRecord record = new MemGrowthRecord();
        record.setTenantId(1L);
        record.setDeptId(deptId != null ? deptId : member.getDeptId());
        record.setMemberId(memberId);
        record.setMemberNo(memberNo != null ? memberNo : member.getMemberNo());
        record.setMemberName(memberName != null ? memberName : member.getMemberName());
        record.setSourceType("SALE");
        record.setSourceId(saleId);
        record.setDedupKey(dedupKey);
        record.setGrowthChange(growthEarned);
        record.setBalance(newGrowth);
        record.setBeforeLevel(beforeLevel);
        record.setAfterLevel(targetLevel);
        record.setRemark("消费奖励-销售单" + saleId);
        record.setCreateBy(operator);
        growthRecordMapper.insertGrowthRecord(record);

        log.info("消费奖励入账成功: memberId={}, saleId={}, growth={}", memberId, saleId, growthEarned);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean awardPurchaseReward(Long memberId, String memberNo, String memberName, Long deptId,
                                       Long purchaseId, BigDecimal purchaseAmount, String operator)
    {
        String dedupKey = "PURCHASE_REWARD:" + purchaseId;
        if (growthRecordMapper.selectByDedupKey(dedupKey) != null) return false;
        MemMember member = memberMapper.selectMemMemberByMemberId(memberId);
        if (member == null || !"0".equals(member.getStatus())) return false;
        MemGrowthRule rule = growthRuleService.getGrowthRule();
        MemMemberCardType level = levelService.selectLevelByTypeCode(member.getCardType());
        BigDecimal pointsRate = level != null && level.getPointsRate() != null
                ? level.getPointsRate() : BigDecimal.ONE;
        BigDecimal points = purchaseAmount == null ? BigDecimal.ZERO
                : purchaseAmount.multiply(pointsRate).setScale(2, java.math.RoundingMode.DOWN);
        long growth = purchaseAmount == null || rule == null || rule.getSaleGrowthRatio() == null ? 0L
                : purchaseAmount.multiply(rule.getSaleGrowthRatio()).setScale(0, java.math.RoundingMode.FLOOR).longValue();
        if (points.signum() == 0 && growth == 0) return false;
        memberMapper.addPointsAndGrowth(memberId, points, growth, operator);
        MemPointsRecord latest = pointsRecordService.selectLatestBalanceByMemberId(memberId);
        MemPointsRecord pointsRecord = new MemPointsRecord();
        pointsRecord.setDeptId(deptId != null ? deptId : member.getDeptId());
        pointsRecord.setMemberId(memberId);
        pointsRecord.setMemberNo(memberNo != null ? memberNo : member.getMemberNo());
        pointsRecord.setMemberName(memberName != null ? memberName : member.getMemberName());
        pointsRecord.setRecordType("1");
        pointsRecord.setConsumeAmount(purchaseAmount);
        pointsRecord.setPoints(points);
        pointsRecord.setBalance((latest == null || latest.getBalance() == null ? BigDecimal.ZERO : latest.getBalance()).add(points));
        pointsRecord.setRuleCode("PURCHASE_LEVEL_RATE");
        pointsRecord.setRemark("会员购买奖励:" + purchaseId);
        pointsRecord.setCreateBy(operator);
        pointsRecordService.insertMemPointsRecord(pointsRecord);

        MemMember updated = memberMapper.selectMemMemberByMemberId(memberId);
        String beforeLevel = member.getCardType();
        String afterLevel = levelService.calculateLevel(updated.getGrowthValue());
        if (!afterLevel.equals(beforeLevel)) memberMapper.updateMemberLevel(memberId, afterLevel, operator);
        MemGrowthRecord growthRecord = new MemGrowthRecord();
        growthRecord.setTenantId(1L);
        growthRecord.setDeptId(deptId != null ? deptId : member.getDeptId());
        growthRecord.setMemberId(memberId);
        growthRecord.setMemberNo(memberNo != null ? memberNo : member.getMemberNo());
        growthRecord.setMemberName(memberName != null ? memberName : member.getMemberName());
        growthRecord.setSourceType("PURCHASE");
        growthRecord.setSourceId(purchaseId);
        growthRecord.setDedupKey(dedupKey);
        growthRecord.setGrowthChange(growth);
        growthRecord.setBalance(updated.getGrowthValue());
        growthRecord.setBeforeLevel(beforeLevel);
        growthRecord.setAfterLevel(afterLevel);
        growthRecord.setRemark("会员购买奖励:" + purchaseId);
        growthRecord.setCreateBy(operator);
        growthRecordMapper.insertGrowthRecord(growthRecord);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reversePurchaseReward(Long memberId, Long purchaseId, String operator)
    {
        String originalKey = "PURCHASE_REWARD:" + purchaseId;
        MemGrowthRecord original = growthRecordMapper.selectByDedupKey(originalKey);
        if (original == null) return false;
        String reversalKey = "PURCHASE_REWARD_REVERSAL:" + purchaseId;
        if (growthRecordMapper.selectByDedupKey(reversalKey) != null) return false;
        MemMember member = memberMapper.selectMemMemberByMemberId(memberId);
        if (member == null) throw new IllegalArgumentException("member does not exist");
        MemPointsRecord pointOriginal = pointsRecordService.selectMemPointsRecordByRemark("会员购买奖励:" + purchaseId)
                .stream().findFirst().orElse(null);
        BigDecimal points = pointOriginal == null || pointOriginal.getPoints() == null ? BigDecimal.ZERO : pointOriginal.getPoints().negate();
        long growth = original.getGrowthChange() == null ? 0L : -original.getGrowthChange();
        memberMapper.addPointsAndGrowth(memberId, points, growth, operator);
        MemPointsRecord latest = pointsRecordService.selectLatestBalanceByMemberId(memberId);
        MemPointsRecord reversal = new MemPointsRecord();
        reversal.setDeptId(member.getDeptId()); reversal.setMemberId(memberId);
        reversal.setMemberNo(member.getMemberNo()); reversal.setMemberName(member.getMemberName());
        reversal.setRecordType("4"); reversal.setPoints(points);
        reversal.setBalance((latest == null || latest.getBalance() == null ? BigDecimal.ZERO : latest.getBalance()).add(points));
        reversal.setRemark("会员购买奖励冲正:" + purchaseId); reversal.setCreateBy(operator);
        pointsRecordService.insertMemPointsRecord(reversal);
        MemGrowthRecord record = new MemGrowthRecord();
        record.setTenantId(original.getTenantId()); record.setDeptId(member.getDeptId()); record.setMemberId(memberId);
        record.setMemberNo(member.getMemberNo()); record.setMemberName(member.getMemberName()); record.setSourceType("PURCHASE_REVERSAL");
        record.setSourceId(purchaseId); record.setDedupKey(reversalKey); record.setGrowthChange(growth);
        MemMember updated = memberMapper.selectMemMemberByMemberId(memberId); record.setBalance(updated.getGrowthValue());
        String afterLevel = levelService.calculateLevel(updated.getGrowthValue());
        record.setBeforeLevel(member.getCardType()); record.setAfterLevel(afterLevel);
        if (!afterLevel.equals(member.getCardType())) memberMapper.updateMemberLevel(memberId, afterLevel, operator);
        record.setRemark("会员购买奖励冲正:" + purchaseId); record.setCreateBy(operator);
        growthRecordMapper.insertGrowthRecord(record);
        return true;
    }

    /**
     * 签到奖励入账
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthAwardResult awardSignInGrowth(Long memberId, java.util.Date signDate, String operator)
    {
        java.util.Date signDateUsed = signDate != null ? signDate : new java.util.Date();
        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(signDateUsed);
        String dedupKey = "SIGN_IN:" + memberId + ":" + dateStr;

        // 1. 幂等检查
        MemGrowthRecord existing = growthRecordMapper.selectByDedupKey(dedupKey);
        if (existing != null)
        {
            throw new RuntimeException("今日已签到");
        }

        // 2. 查询会员
        MemMember member = memberMapper.selectMemMemberByMemberId(memberId);
        if (member == null || !"0".equals(member.getStatus()))
        {
            throw new RuntimeException("会员不存在或状态异常");
        }

        // 3. 读取成长规则
        MemGrowthRule rule = growthRuleService.getGrowthRule();

        // 4. 签到积分优先用等级 sign_in_points，若等级未配置则用规则 sign_in_points 兜底
        BigDecimal pointsEarned = rule.getSignInPoints();
        MemMemberCardType currentLevel = levelService.selectLevelByTypeCode(member.getCardType());
        if (currentLevel != null && currentLevel.getSignInPoints() != null)
        {
            pointsEarned = currentLevel.getSignInPoints();
        }
        long growthEarned = rule.getSignInGrowth();

        // 5. 原子更新会员积分和成长值
        memberMapper.addPointsAndGrowth(memberId, pointsEarned, growthEarned, operator);

        // 6. 写积分流水
        MemPointsRecord pointsRecord = new MemPointsRecord();
        pointsRecord.setDeptId(member.getDeptId());
        pointsRecord.setMemberId(memberId);
        pointsRecord.setMemberNo(member.getMemberNo());
        pointsRecord.setMemberName(member.getMemberName());
        pointsRecord.setRecordType("5");
        pointsRecord.setPoints(pointsEarned);
        // 查最新积分余额
        MemPointsRecord latest = pointsRecordService.selectLatestBalanceByMemberId(memberId);
        pointsRecord.setBalance(latest != null && latest.getBalance() != null
                ? latest.getBalance().add(pointsEarned) : pointsEarned);
        pointsRecord.setRemark("签到得积分");
        pointsRecord.setCreateBy(operator);
        pointsRecordService.insertMemPointsRecord(pointsRecord);

        // 7. 重新查询最新成长值
        MemMember updated = memberMapper.selectMemMemberByMemberId(memberId);
        Long newGrowth = updated.getGrowthValue();
        String beforeLevel = member.getCardType();

        // 8. 检查并更新等级
        String targetLevel = levelService.calculateLevel(newGrowth);
        if (!targetLevel.equals(beforeLevel))
        {
            memberMapper.updateMemberLevel(memberId, targetLevel, operator);
        }

        // 9. 写成长值流水
        MemGrowthRecord record = new MemGrowthRecord();
        record.setTenantId(1L);
        record.setDeptId(member.getDeptId());
        record.setMemberId(memberId);
        record.setMemberNo(member.getMemberNo());
        record.setMemberName(member.getMemberName());
        record.setSourceType("SIGN_IN");
        record.setSourceId(null);
        record.setDedupKey(dedupKey);
        record.setGrowthChange(growthEarned);
        record.setBalance(newGrowth);
        record.setBeforeLevel(beforeLevel);
        record.setAfterLevel(targetLevel);
        record.setRemark("签到奖励");
        record.setCreateBy(operator);
        growthRecordMapper.insertGrowthRecord(record);

        log.info("签到奖励入账成功: memberId={}, points={}, growth={}", memberId, pointsEarned, growthEarned);
        return new GrowthAwardResult(growthEarned, newGrowth, beforeLevel, targetLevel);
    }

    /**
     * 批量补录签到汇总发奖
     * 一次批量补录只写一条积分流水和一条成长值流水
     * 幂等键: SIGN_IN_BACKFILL:{batchId}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthAwardResult awardSignInBackfillGrowth(Long memberId, Long batchId, BigDecimal totalPoints,
                                                        Long totalGrowth, String rewardLevelCode, String operator)
    {
        if (batchId == null)
        {
            throw new RuntimeException("补录批次ID不能为空");
        }
        String dedupKey = "SIGN_IN_BACKFILL:" + batchId;

        // 1. 幂等检查
        MemGrowthRecord existing = growthRecordMapper.selectByDedupKey(dedupKey);
        if (existing != null)
        {
            throw new RuntimeException("该补录批次已发放奖励");
        }

        // 2. 查询会员
        MemMember member = memberMapper.selectMemMemberByMemberId(memberId);
        if (member == null || !"0".equals(member.getStatus()))
        {
            throw new RuntimeException("会员不存在或状态异常");
        }

        // 3. 计算实际入账积分和成长值（若入参为空按0处理）
        BigDecimal pointsEarned = totalPoints != null ? totalPoints : BigDecimal.ZERO;
        long growthEarned = totalGrowth != null ? totalGrowth : 0L;

        // 4. 原子更新会员积分和成长值
        memberMapper.addPointsAndGrowth(memberId, pointsEarned, growthEarned, operator);

        // 5. 写积分流水（record_type=5 签到）
        MemPointsRecord pointsRecord = new MemPointsRecord();
        pointsRecord.setDeptId(member.getDeptId());
        pointsRecord.setMemberId(memberId);
        pointsRecord.setMemberNo(member.getMemberNo());
        pointsRecord.setMemberName(member.getMemberName());
        pointsRecord.setRecordType("5");
        pointsRecord.setPoints(pointsEarned);
        MemPointsRecord latest = pointsRecordService.selectLatestBalanceByMemberId(memberId);
        pointsRecord.setBalance(latest != null && latest.getBalance() != null
                ? latest.getBalance().add(pointsEarned) : pointsEarned);
        pointsRecord.setRemark("签到补录-批次" + batchId);
        pointsRecord.setCreateBy(operator);
        pointsRecordService.insertMemPointsRecord(pointsRecord);

        // 6. 重新查询最新成长值
        MemMember updated = memberMapper.selectMemMemberByMemberId(memberId);
        Long newGrowth = updated.getGrowthValue();
        String beforeLevel = member.getCardType();

        // 7. 检查并更新等级
        String targetLevel = levelService.calculateLevel(newGrowth);
        if (!targetLevel.equals(beforeLevel))
        {
            memberMapper.updateMemberLevel(memberId, targetLevel, operator);
        }

        // 8. 写成长值流水（source_type=SIGN_IN_BACKFILL）
        MemGrowthRecord record = new MemGrowthRecord();
        record.setTenantId(1L);
        record.setDeptId(member.getDeptId());
        record.setMemberId(memberId);
        record.setMemberNo(member.getMemberNo());
        record.setMemberName(member.getMemberName());
        record.setSourceType("SIGN_IN_BACKFILL");
        record.setSourceId(batchId);
        record.setDedupKey(dedupKey);
        record.setGrowthChange(growthEarned);
        record.setBalance(newGrowth);
        record.setBeforeLevel(beforeLevel);
        record.setAfterLevel(targetLevel);
        record.setRemark("签到补录奖励-批次" + batchId);
        record.setCreateBy(operator);
        growthRecordMapper.insertGrowthRecord(record);

        log.info("签到补录奖励入账成功: memberId={}, batchId={}, points={}, growth={}",
                memberId, batchId, pointsEarned, growthEarned);
        return new GrowthAwardResult(growthEarned, newGrowth, beforeLevel, targetLevel);
    }

    /**
     * 手工调整积分和成长值
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthAwardResult manualAdjust(Long memberId, BigDecimal pointsChange, Long growthChange,
                                          String remark, String operator)
    {
        if ((pointsChange == null || pointsChange.compareTo(BigDecimal.ZERO) == 0)
            && (growthChange == null || growthChange == 0L))
        {
            throw new RuntimeException("积分调整值和成长值调整值至少一个非0");
        }

        String dedupKey = "MANUAL:" + UUID.randomUUID().toString();

        // 1. 查询会员
        MemMember member = memberMapper.selectMemMemberByMemberId(memberId);
        if (member == null)
        {
            throw new RuntimeException("会员不存在");
        }

        String beforeLevel = member.getCardType();
        Long beforeGrowth = member.getGrowthValue() != null ? member.getGrowthValue() : 0L;

        // 2. 调整积分
        if (pointsChange != null && pointsChange.compareTo(BigDecimal.ZERO) != 0)
        {
            memberMapper.addPointsAndGrowth(memberId, pointsChange, 0L, operator);

            // 写积分流水（余额 = 变动前最新余额 + 本次变动值，与会员表 addPointsAndGrowth 保持一致）
            MemPointsRecord pointsRecord = new MemPointsRecord();
            pointsRecord.setDeptId(member.getDeptId());
            pointsRecord.setMemberId(memberId);
            pointsRecord.setMemberNo(member.getMemberNo());
            pointsRecord.setMemberName(member.getMemberName());
            pointsRecord.setRecordType("4");
            pointsRecord.setPoints(pointsChange);
            MemPointsRecord latest = pointsRecordService.selectLatestBalanceByMemberId(memberId);
            BigDecimal latestBalance = latest != null && latest.getBalance() != null
                    ? latest.getBalance() : BigDecimal.ZERO;
            pointsRecord.setBalance(latestBalance.add(pointsChange));
            pointsRecord.setRemark(remark != null ? remark : "手动调整");
            pointsRecord.setCreateBy(operator);
            pointsRecordService.insertMemPointsRecord(pointsRecord);
        }

        // 3. 调整成长值
        Long actualGrowthChange = 0L;
        if (growthChange != null && growthChange != 0L)
        {
            memberMapper.addGrowthOnly(memberId, growthChange, operator);
            actualGrowthChange = growthChange;
        }
        else if (pointsChange != null && pointsChange.compareTo(BigDecimal.ZERO) != 0)
        {
            // 只调积分时也要更新活跃时间
            memberMapper.updateLastActiveTime(memberId);
        }

        // 4. 重新查询最新成长值和等级
        MemMember updated = memberMapper.selectMemMemberByMemberId(memberId);
        Long newGrowth = updated.getGrowthValue();

        // 5. 检查并更新等级
        String targetLevel = levelService.calculateLevel(newGrowth);
        if (!targetLevel.equals(beforeLevel))
        {
            memberMapper.updateMemberLevel(memberId, targetLevel, operator);
        }

        // 6. 写成长值流水（只有成长值变动时才写）
        if (actualGrowthChange != 0L)
        {
            MemGrowthRecord record = new MemGrowthRecord();
            record.setTenantId(1L);
            record.setDeptId(member.getDeptId());
            record.setMemberId(memberId);
            record.setMemberNo(member.getMemberNo());
            record.setMemberName(member.getMemberName());
            record.setSourceType("MANUAL");
            record.setSourceId(null);
            record.setDedupKey(dedupKey);
            record.setGrowthChange(actualGrowthChange);
            record.setBalance(newGrowth);
            record.setBeforeLevel(beforeLevel);
            record.setAfterLevel(targetLevel);
            record.setRemark(remark != null ? remark : "手动调整");
            record.setCreateBy(operator);
            growthRecordMapper.insertGrowthRecord(record);
        }

        log.info("手工调整完成: memberId={}, pointsChange={}, growthChange={}", memberId, pointsChange, growthChange);
        return new GrowthAwardResult(actualGrowthChange, newGrowth, beforeLevel, targetLevel);
    }

    /**
     * 衰减扣减成长值
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decayGrowth(Long memberId, Long decayAmount, String operator)
    {
        if (decayAmount == null || decayAmount <= 0)
        {
            return false;
        }

        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        String dedupKey = "DECAY:" + memberId + ":" + dateStr;

        // 1. 幂等检查
        MemGrowthRecord existing = growthRecordMapper.selectByDedupKey(dedupKey);
        if (existing != null)
        {
            return false;
        }

        // 2. 查询会员
        MemMember member = memberMapper.selectMemMemberByMemberId(memberId);
        if (member == null || member.getGrowthValue() == null || member.getGrowthValue() <= 0)
        {
            return false;
        }

        String beforeLevel = member.getCardType();

        // 3. 扣减成长值（负数），不更新活跃时间（避免被衰减会员变成"活跃"导致衰减规则失真）
        memberMapper.addGrowthOnlyWithoutActiveTime(memberId, -decayAmount, operator);

        // 4. 重新查询并重算等级
        MemMember updated = memberMapper.selectMemMemberByMemberId(memberId);
        Long newGrowth = updated.getGrowthValue();
        String targetLevel = levelService.calculateLevel(newGrowth);
        if (!targetLevel.equals(beforeLevel))
        {
            memberMapper.updateMemberLevel(memberId, targetLevel, operator);
        }

        // 5. 写成长值流水
        MemGrowthRecord record = new MemGrowthRecord();
        record.setTenantId(1L);
        record.setDeptId(member.getDeptId());
        record.setMemberId(memberId);
        record.setMemberNo(member.getMemberNo());
        record.setMemberName(member.getMemberName());
        record.setSourceType("DECAY");
        record.setSourceId(null);
        record.setDedupKey(dedupKey);
        record.setGrowthChange(-decayAmount);
        record.setBalance(newGrowth);
        record.setBeforeLevel(beforeLevel);
        record.setAfterLevel(targetLevel);
        record.setRemark("不活跃衰减");
        record.setCreateBy(operator);
        growthRecordMapper.insertGrowthRecord(record);

        log.info("衰减扣减完成: memberId={}, decayAmount={}, newGrowth={}", memberId, decayAmount, newGrowth);
        return true;
    }

    /**
     * 查询成长值记录列表
     */
    @Override
    public List<MemGrowthRecord> selectGrowthRecordList(MemGrowthRecord record)
    {
        return growthRecordMapper.selectGrowthRecordList(record);
    }

    /**
     * 查询会员成长值汇总
     */
    @Override
    public GrowthSummary getGrowthSummary(Long memberId)
    {
        MemMember member = memberMapper.selectMemMemberByMemberId(memberId);
        if (member == null)
        {
            return null;
        }

        GrowthSummary summary = new GrowthSummary();
        summary.memberId = memberId;
        summary.memberNo = member.getMemberNo();
        summary.memberName = member.getMemberName();
        summary.growthValue = member.getGrowthValue() != null ? member.getGrowthValue() : 0L;
        summary.currentLevel = member.getCardType();

        // 查询当前等级名称
        MemMemberCardType currentLevel = levelService.selectLevelByTypeCode(member.getCardType());
        summary.currentLevelName = currentLevel != null ? currentLevel.getTypeName() : member.getCardType();

        // 查询下一等级
        List<MemMemberCardType> levels = levelService.selectEnabledLevels();
        MemMemberCardType nextLevel = null;
        for (MemMemberCardType level : levels)
        {
            if (level.getMinGrowth() != null && level.getMinGrowth() > summary.growthValue)
            {
                nextLevel = level;
                break;
            }
        }

        if (nextLevel != null)
        {
            summary.nextLevel = nextLevel.getTypeCode();
            summary.nextLevelName = nextLevel.getTypeName();
            summary.nextLevelGrowth = nextLevel.getMinGrowth();
            summary.growthToNextLevel = nextLevel.getMinGrowth() - summary.growthValue;
            if (nextLevel.getMinGrowth() > 0)
            {
                summary.progressPercent = new BigDecimal(summary.growthValue)
                        .multiply(new BigDecimal("100"))
                        .divide(new BigDecimal(nextLevel.getMinGrowth()), 2, java.math.RoundingMode.HALF_UP);
            }
            else
            {
                summary.progressPercent = new BigDecimal("100.00");
            }
        }
        else
        {
            // 已是最高等级
            summary.nextLevel = null;
            summary.nextLevelName = "已最高等级";
            summary.nextLevelGrowth = summary.growthValue;
            summary.growthToNextLevel = 0L;
            summary.progressPercent = new BigDecimal("100.00");
        }

        return summary;
    }
}
