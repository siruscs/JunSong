package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinCompositeAccountingPool;
import com.junsong.finance.domain.FinCompositePeriodItem;
import com.junsong.finance.domain.vo.CompositeCandidatePeriodVO;
import com.junsong.finance.domain.vo.CompositePoolOverviewVO;
import com.junsong.finance.domain.vo.CompositeTrialResultVO;

/**
 * 复合核算服务接口
 *
 * @author junsong
 */
public interface IFinCompositeAccountingService
{
    /**
     * 查询复合核算池列表
     */
    public List<FinCompositeAccountingPool> selectCompositePoolList(FinCompositeAccountingPool pool);

    /**
     * 查询复合核算池详情
     */
    public FinCompositeAccountingPool selectCompositePoolByPoolId(Long poolId);
    boolean canAccessPool(Long poolId, Long deptId);

    /**
     * 创建复合核算池(同时保存参与店面和共享投资人)
     */
    public int createPool(FinCompositeAccountingPool pool);

    /**
     * 修改复合核算池基础信息
     */
    public int updatePool(FinCompositeAccountingPool pool);

    /**
     * 删除复合核算池(软删)
     */
    public int deleteCompositePoolByPoolIds(Long[] poolIds);

    /**
     * 维护参与店面(全量覆盖)
     */
    public int bindDepts(Long poolId, List<Long> deptIds);

    /**
     * 维护共享投资人和出资款(全量覆盖)
     */
    public int bindInvestors(Long poolId, List<InvestorInput> investors);

    /**
     * 复合核算池概览(含参与店面、共享投资人、周期明细、回本进度)
     */
    public CompositePoolOverviewVO getOverview(Long poolId);

    /**
     * 查询已纳入周期明细
     */
    public List<FinCompositePeriodItem> listPeriods(Long poolId);

    /**
     * 查询可手动纳入的候选周期(回本后使用)
     */
    public List<CompositeCandidatePeriodVO> listCandidatePeriods(Long poolId, Long deptId);

    /**
     * 试算手动纳入结果(不落库)
     */
    public CompositeTrialResultVO trialIncludePeriods(Long poolId, List<Long> periodIds);

    /**
     * 确认纳入周期(落库并刷新回本金额)
     */
    public int confirmIncludePeriods(Long poolId, List<Long> periodIds);

    /**
     * 重新计算累计回本、缺口、超额收益
     */
    public int recalculatePool(Long poolId);

    /**
     * 财务确认整体回本
     */
    public int confirmBreakEven(Long poolId);

    /**
     * 关闭复合核算池
     */
    public int closePool(Long poolId);

    /**
     * 单店结转后自动纳入复合核算池
     * 不阻断单店结转流程,失败仅记录日志
     */
    public void autoIncludeAfterPeriodCarryForward(Long periodId);

    /**
     * 校验某周期是否已纳入复合核算池(用于单店结转回退保护)
     */
    public boolean isPeriodIncludedInComposite(Long periodId);

    /**
     * 投资人输入参数
     */
    class InvestorInput {
        private Long investorId;
        private String investorName;
        private java.math.BigDecimal investAmount;

        public Long getInvestorId() { return investorId; }
        public void setInvestorId(Long investorId) { this.investorId = investorId; }
        public String getInvestorName() { return investorName; }
        public void setInvestorName(String investorName) { this.investorName = investorName; }
        public java.math.BigDecimal getInvestAmount() { return investAmount; }
        public void setInvestAmount(java.math.BigDecimal investAmount) { this.investAmount = investAmount; }
    }
}
