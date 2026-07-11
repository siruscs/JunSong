package com.junsong.finance.mapper;

import java.math.BigDecimal;
import java.util.List;
import com.junsong.finance.domain.FinCompositeAccountingPool;
import com.junsong.finance.domain.FinCompositePeriodItem;
import com.junsong.finance.domain.FinCompositePoolDept;
import com.junsong.finance.domain.FinCompositePoolInvestor;
import com.junsong.finance.domain.vo.CompositeAccountingSummaryVO;
import org.apache.ibatis.annotations.Param;

/**
 * 复合核算 Mapper
 * 统一管理 fin_composite_accounting_pool / fin_composite_pool_dept /
 * fin_composite_pool_investor / fin_composite_period_item 四张表
 *
 * @author junsong
 */
public interface FinCompositeAccountingMapper
{
    // ============== 复合核算池主表 ==============
    public FinCompositeAccountingPool selectCompositePoolByPoolId(Long poolId);

    public List<FinCompositeAccountingPool> selectCompositePoolList(FinCompositeAccountingPool pool);

    public int insertCompositePool(FinCompositeAccountingPool pool);

    public int updateCompositePool(FinCompositeAccountingPool pool);

    public int deleteCompositePoolByPoolIds(Long[] poolIds);

    /**
     * 查询某店面已加入的进行中复合核算池
     */
    public FinCompositeAccountingPool selectActivePoolByDeptId(@Param("deptId") Long deptId);

    /**
     * 累加累计回本金额
     */
    public int addReturnAmount(@Param("poolId") Long poolId, @Param("amount") BigDecimal amount);

    // ============== 复合池店面关系表 ==============
    public List<FinCompositePoolDept> selectPoolDeptsByPoolId(Long poolId);

    /**
     * 查询复合池参与店面ID集合(仅有效记录)
     */
    public java.util.List<Long> selectPoolDeptIdsByPoolId(Long poolId);

    /**
     * 查询用户关联的店面ID集合(在职状态)
     */
    public java.util.List<Long> selectUserDeptIdsByUserId(Long userId);

    /**
     * 根据店面ID查询店面名称
     */
    public String selectDeptNameById(Long deptId);

    public int insertPoolDept(FinCompositePoolDept dept);

    public int deletePoolDeptByPoolId(Long poolId);

    public int deletePoolDeptByPoolIdAndDeptId(@Param("poolId") Long poolId, @Param("deptId") Long deptId);

    // ============== 复合池共享投资人表 ==============
    public List<FinCompositePoolInvestor> selectPoolInvestorsByPoolId(Long poolId);

    public int insertPoolInvestor(FinCompositePoolInvestor investor);

    public int deletePoolInvestorByPoolId(Long poolId);

    /**
     * 更新投资人已分摊回本金额
     */
    public int updateInvestorReturnedAmount(@Param("id") Long id, @Param("returnedAmount") BigDecimal returnedAmount);

    // ============== 复合核算周期纳入明细表 ==============
    public List<FinCompositePeriodItem> selectPeriodItemsByPoolId(Long poolId);

    public CompositeAccountingSummaryVO selectSummaryByPoolId(Long poolId);

    public FinCompositePeriodItem selectPeriodItemByPoolIdAndPeriodId(@Param("poolId") Long poolId, @Param("periodId") Long periodId);

    /**
     * 全局查询某周期是否已被纳入复合核算池
     */
    public FinCompositePeriodItem selectPeriodItemByPeriodId(@Param("periodId") Long periodId);

    public int insertPeriodItem(FinCompositePeriodItem item);

    public int deletePeriodItemByPoolId(Long poolId);

    /**
     * 撤销某周期的纳入记录
     */
    public int revokePeriodItem(@Param("poolId") Long poolId, @Param("periodId") Long periodId);
}
