package com.junsong.finance.mapper;

import java.util.List;
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
}
