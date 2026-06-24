package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.vo.ExpenseVerifyVO;

/**
 * 费用记录Service接口
 * 
 * @author junsong
 */
public interface IFinExpenseService
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
     * 批量删除费用记录
     * 
     * @param expenseIds 需要删除的费用记录主键集合
     * @return 结果
     */
    public int deleteFinExpenseByExpenseIds(Long[] expenseIds);

    /**
     * 删除费用记录信息
     * 
     * @param expenseId 费用记录主键
     * @return 结果
     */
    public int deleteFinExpenseByExpenseId(Long expenseId);

    /**
     * 校验费用单号是否唯一
     * 
     * @param finExpense 费用记录信息
     * @return 结果
     */
    public boolean checkExpenseNoUnique(FinExpense finExpense);

    /**
     * 核销费用记录
     * 
     * @param expenseId 费用记录主键
     * @param verifyBy 核销人
     * @return 结果
     */
    public int verifyExpense(Long expenseId, String verifyBy);

    /**
     * 批量核销费用记录
     * 
     * @param verifyVO 核销信息
     * @param verifyBy 核销人
     * @return 结果
     */
    public int batchVerifyExpense(ExpenseVerifyVO verifyVO, String verifyBy);

    /**
     * 获取费用统计信息
     *
     * @return 统计结果
     */
    public com.junsong.finance.domain.vo.ExpenseSummary getExpenseSummary(Long deptId);

    /**
     * 导入费用记录
     *
     * @param expenseList 费用记录列表
     * @param updateSupport 是否支持更新
     * @param operName 操作人
     * @param deptId 部门ID
     * @return 导入结果消息
     */
    public String importExpense(List<FinExpense> expenseList, boolean updateSupport, String operName, Long deptId);
}
