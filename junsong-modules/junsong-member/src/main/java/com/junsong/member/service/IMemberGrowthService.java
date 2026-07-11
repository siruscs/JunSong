package com.junsong.member.service;

import java.util.Date;
import java.util.List;
import java.math.BigDecimal;
import com.junsong.member.domain.MemGrowthRecord;

/**
 * 会员成长值核心Service接口
 * 负责消费奖励、签到奖励、手工调整、等级重算
 *
 * @author junsong
 */
public interface IMemberGrowthService
{
    /**
     * 消费奖励入账（财务模块调用）
     * 幂等键: SALE:{sourceId}
     *
     * @param memberId 会员ID
     * @param memberNo 会员编号
     * @param memberName 会员姓名
     * @param deptId 部门ID
     * @param saleId 销售单ID（幂等来源）
     * @param saleAmount 消费金额
     * @param operator 操作人
     * @return 是否入账成功（已处理返回false但不报错）
     */
    public boolean awardSaleGrowth(Long memberId, String memberNo, String memberName, Long deptId,
                                   Long saleId, BigDecimal saleAmount, String operator);

    /**
     * 签到奖励入账
     * 幂等键: SIGN_IN:{memberId}:{signDate}
     *
     * @param memberId 会员ID
     * @param signDate 签到日期（为空时默认当天）
     * @param operator 操作人
     * @return 成长值变动结果（含等级变化信息）
     */
    public GrowthAwardResult awardSignInGrowth(Long memberId, Date signDate, String operator);

    /**
     * 批量补录签到汇总发奖
     * 一次批量补录只写一条积分流水和一条成长值流水
     * 幂等键: SIGN_IN_BACKFILL:{batchId}
     *
     * @param memberId 会员ID
     * @param batchId 补录批次ID
     * @param totalPoints 总积分
     * @param totalGrowth 总成长值
     * @param rewardLevelCode 奖励等级快照
     * @param operator 操作人
     * @return 成长值变动结果
     */
    public GrowthAwardResult awardSignInBackfillGrowth(Long memberId, Long batchId, BigDecimal totalPoints,
                                                        Long totalGrowth, String rewardLevelCode, String operator);

    /**
     * 手工调整积分和成长值
     * 幂等键: MANUAL:{uuid}
     *
     * @param memberId 会员ID
     * @param pointsChange 积分变动（可正可负）
     * @param growthChange 成长值变动（可正可负）
     * @param remark 备注
     * @param operator 操作人
     * @return 成长值变动结果
     */
    public GrowthAwardResult manualAdjust(Long memberId, BigDecimal pointsChange, Long growthChange,
                                          String remark, String operator);

    /**
     * 衰减扣减成长值（定时任务调用）
     * 幂等键: DECAY:{memberId}:{yyyy-MM-dd}
     *
     * @param memberId 会员ID
     * @param decayAmount 衰减量
     * @param operator 操作人
     * @return 是否扣减成功
     */
    public boolean decayGrowth(Long memberId, Long decayAmount, String operator);

    /**
     * 查询成长值记录列表
     */
    public List<MemGrowthRecord> selectGrowthRecordList(MemGrowthRecord record);

    /**
     * 查询会员成长值汇总
     */
    public GrowthSummary getGrowthSummary(Long memberId);

    /**
     * 成长值变动结果
     */
    public static class GrowthAwardResult
    {
        public Long growthChange;
        public Long balance;
        public String beforeLevel;
        public String afterLevel;
        public boolean levelChanged;

        public GrowthAwardResult(Long growthChange, Long balance, String beforeLevel, String afterLevel)
        {
            this.growthChange = growthChange;
            this.balance = balance;
            this.beforeLevel = beforeLevel;
            this.afterLevel = afterLevel;
            this.levelChanged = !afterLevel.equals(beforeLevel);
        }
    }

    /**
     * 成长值汇总
     */
    public static class GrowthSummary
    {
        public Long memberId;
        public String memberNo;
        public String memberName;
        public Long growthValue;
        public String currentLevel;
        public String currentLevelName;
        public String nextLevel;
        public String nextLevelName;
        public Long nextLevelGrowth;
        public Long growthToNextLevel;
        public BigDecimal progressPercent;
    }
}
