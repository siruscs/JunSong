package com.junsong.finance.mapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.junsong.finance.domain.FinExpense;

/**
 * 费用记录Mapper接口
 * 
 * @author junsong
 */
public interface FinExpenseMapper
{
    /**
     * 查询费用记录
     * 
     * @param expenseId 费用记录主键
     * @return 费用记录
     */
    public FinExpense selectFinExpenseByExpenseId(Long expenseId);

    /**
     * 查询费用记录列表
     * 
     * @param finExpense 费用记录
     * @return 费用记录集合
     */
    public List<FinExpense> selectFinExpenseList(FinExpense finExpense);

    /**
     * 新增费用记录
     * 
     * @param finExpense 费用记录
     * @return 结果
     */
    public int insertFinExpense(FinExpense finExpense);

    /**
     * 修改费用记录
     * 
     * @param finExpense 费用记录
     * @return 结果
     */
    public int updateFinExpense(FinExpense finExpense);

    /**
     * 删除费用记录
     * 
     * @param expenseId 费用记录主键
     * @return 结果
     */
    public int deleteFinExpenseByExpenseId(Long expenseId);

    /**
     * 批量删除费用记录
     * 
     * @param expenseIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinExpenseByExpenseIds(Long[] expenseIds);

    /**
     * 校验费用单号是否唯一
     *
     * @param expenseNo 费用单号
     * @return 结果
     */
    public FinExpense checkExpenseNoUnique(String expenseNo);

    /**
     * 统计今日费用单数量
     *
     * @return 结果
     */
    public int countTodayExpenses();

    /**
     * 获取今日费用单号的最大序号（4位后缀对应的数字）
     * 用于生成不冲突的费用单号，避免 count 与实际序号不一致导致的碰撞
     *
     * @return 今日最大序号，无记录时返回0
     */
    public int maxTodayExpenseSeq();

    /**
     * 统计未核销费用总金额
     * 
     * @return 结果
     */
    public java.math.BigDecimal sumUnverifiedExpenses();

    public java.math.BigDecimal sumUnverifiedExpensesByDeptId(@Param("deptId") Long deptId);

    public java.math.BigDecimal sumAllExpenses();

    public java.math.BigDecimal sumAllExpensesByDeptId(@Param("deptId") Long deptId);

    public java.math.BigDecimal sumAllExpensesByPeriodId(@Param("periodId") Long periodId);

    /**
     * 根据费用记录ID数组查询费用记录
     * 
     * @param expenseIds 费用记录ID数组
     * @return 费用记录集合
     */
    public List<FinExpense> selectFinExpenseByExpenseIds(Long[] expenseIds);

    public List<FinExpense> selectFinExpenseByExpenseIdsScoped(@Param("expenseIds") List<Long> expenseIds,
        @Param("tenantId") Long tenantId, @Param("deptId") Long deptId);

    public int markExpenseVerified(@Param("expenseId") Long expenseId, @Param("advanceId") Long advanceId,
        @Param("verifyBy") String verifyBy, @Param("verifyTime") Date verifyTime,
        @Param("tenantId") Long tenantId, @Param("deptId") Long deptId);

    public int restoreExpenseUnverified(@Param("expenseId") Long expenseId);

    /**
     * 费用分类统计
     */
    public List<java.util.Map<String, Object>> selectExpenseCategoryStats(java.util.Map<String, Object> params);

    /**
     * 费用趋势统计
     */
    public List<java.util.Map<String, Object>> selectExpenseTrendStats(java.util.Map<String, Object> params);

    /**
     * 费用门店统计
     */
    public List<java.util.Map<String, Object>> selectExpenseDeptStats(java.util.Map<String, Object> params);

    /**
     * 费用总金额统计
     */
    public java.math.BigDecimal selectExpenseTotal(java.util.Map<String, Object> params);

    BigDecimal selectTodayTotalExpense(@Param("deptIds") List<Long> deptIds);
    BigDecimal selectMonthTotalExpense(@Param("deptIds") List<Long> deptIds);
    BigDecimal selectMonthTotalExpenseForPrev(@Param("deptIds") List<Long> deptIds);
    int countUnverifiedExpenses(@Param("deptIds") List<Long> deptIds);
    BigDecimal sumUnverifiedExpenseAmount(@Param("deptIds") List<Long> deptIds);
    int countUnverifiedExpensesByPeriodId(@Param("deptIds") List<Long> deptIds, @Param("periodId") Long periodId);
    BigDecimal sumUnverifiedExpenseAmountByPeriodId(@Param("deptIds") List<Long> deptIds, @Param("periodId") Long periodId);
    List<Map<String, Object>> selectExpenseCategoryStatsWithPrev(@Param("deptIds") List<Long> deptIds, @Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("prevStartTime") Date prevStartTime, @Param("prevEndTime") Date prevEndTime);
    List<Map<String, Object>> selectUnverifiedExpenseList(@Param("deptIds") List<Long> deptIds);
    List<Map<String, Object>> selectOcrAnomalies(@Param("deptIds") List<Long> deptIds);
}
