package com.junsong.finance.service.impl;

import com.junsong.common.core.exception.ServiceException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.FinAdvance;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.vo.ExpenseVerifyVO;
import com.junsong.finance.mapper.FinExpenseMapper;
import com.junsong.finance.mapper.FinAdvanceMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinExpenseService;
import com.junsong.finance.constant.VerifyStatus;
import com.junsong.finance.util.CodeGenerator;

/**
 * 费用记录Service业务层处理
 * 
 * @author junsong
 */
@Service
public class FinExpenseServiceImpl implements IFinExpenseService
{
    @Autowired
    private FinExpenseMapper finExpenseMapper;

    @Autowired
    private FinAdvanceMapper finAdvanceMapper;

    @Autowired
    private IFinAccountingPeriodService finAccountingPeriodService;

    /**
     * 查询费用记录
     * 
     * @param expenseId 费用记录主键
     * @return 费用记录
     */
    @Override
    public FinExpense selectFinExpenseByExpenseId(Long expenseId)
    {
        return finExpenseMapper.selectFinExpenseByExpenseId(expenseId);
    }

    /**
     * 查询费用记录列表
     * 
     * @param finExpense 费用记录
     * @return 费用记录
     */
    @Override
    public List<FinExpense> selectFinExpenseList(FinExpense finExpense)
    {
        return finExpenseMapper.selectFinExpenseList(finExpense);
    }

    /**
     * 新增费用记录
     * 
     * @param finExpense 费用记录
     * @return 结果
     */
    @Transactional
    @Override
    public int insertFinExpense(FinExpense finExpense)
    {
        // 自动生成费用单号（带重试机制防止并发重复）
        if (StringUtils.isEmpty(finExpense.getExpenseNo()))
        {
            int retryCount = 0;
            int maxRetries = 10;
            while (retryCount < maxRetries) {
                try {
                    int todayCount = finExpenseMapper.countTodayExpenses();
                    finExpense.setExpenseNo(CodeGenerator.generateExpenseNo(todayCount + retryCount));

                    // 尝试插入
                    return doInsertFinExpense(finExpense);
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    // 单号重复，重试
                    retryCount++;
                    if (retryCount >= maxRetries) {
                        throw new ServiceException("费用单号生成失败，请稍后重试");
                    }
                }
            }
        }

        // 如果用户已提供单号，直接插入
        return doInsertFinExpense(finExpense);
    }

    /**
     * 执行费用记录插入（内部方法）
     */
    private int doInsertFinExpense(FinExpense finExpense)
    {
        // 自动设置部门ID
        finExpense.setDeptId(SecurityUtils.getDeptId());
        fillCurrentPeriod(finExpense);
        finAccountingPeriodService.assertPeriodEditable(finExpense.getPeriodId());

        // 初始状态为未核销
        finExpense.setStatus(VerifyStatus.UNVERIFIED);

        // 如果是收入类型，自动将金额转为负数
        if ("收入".equals(finExpense.getExpenseType()) && finExpense.getExpenseAmount() != null)
        {
            BigDecimal amount = finExpense.getExpenseAmount();
            if (amount.compareTo(BigDecimal.ZERO) > 0)
            {
                finExpense.setExpenseAmount(amount.negate());
            }
        }

        return finExpenseMapper.insertFinExpense(finExpense);
    }

    /**
     * 修改费用记录
     * 
     * @param finExpense 费用记录
     * @return 结果
     */
    @Transactional
    @Override
    public int updateFinExpense(FinExpense finExpense)
    {
        assertExpenseEditable(finExpense.getExpenseId());
        finAccountingPeriodService.assertPeriodEditable(finExpense.getPeriodId());
        FinExpense existing = finExpenseMapper.selectFinExpenseByExpenseId(finExpense.getExpenseId());
        if (existing != null && VerifyStatus.VERIFIED.equals(existing.getStatus()))
        {
            throw new ServiceException("已核销的费用记录不可修改");
        }
        // 如果是收入类型，自动将金额转为负数
        if ("收入".equals(finExpense.getExpenseType()) && finExpense.getExpenseAmount() != null)
        {
            BigDecimal amount = finExpense.getExpenseAmount();
            if (amount.compareTo(BigDecimal.ZERO) > 0)
            {
                finExpense.setExpenseAmount(amount.negate());
            }
        }
        
        finExpense.setStatus(null);
        finExpense.setVerifyBy(null);
        finExpense.setVerifyTime(null);
        finExpense.setAdvanceId(null);
        return finExpenseMapper.updateFinExpense(finExpense);
    }

    /**
     * 批量删除费用记录
     * 
     * @param expenseIds 需要删除的费用记录主键集合
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteFinExpenseByExpenseIds(Long[] expenseIds)
    {
        if (expenseIds != null) {
            for (Long expenseId : expenseIds) {
                assertExpenseDeletable(expenseId);
            }
        }
        return finExpenseMapper.deleteFinExpenseByExpenseIds(expenseIds);
    }

    /**
     * 删除费用记录信息
     * 
     * @param expenseId 费用记录主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteFinExpenseByExpenseId(Long expenseId)
    {
        assertExpenseDeletable(expenseId);
        return finExpenseMapper.deleteFinExpenseByExpenseId(expenseId);
    }

    /**
     * 校验费用单号是否唯一
     * 
     * @param finExpense 费用记录信息
     * @return 结果
     */
    @Override
    public boolean checkExpenseNoUnique(FinExpense finExpense)
    {
        Long expenseId = finExpense.getExpenseId() == null ? -1L : finExpense.getExpenseId();
        FinExpense info = finExpenseMapper.checkExpenseNoUnique(finExpense.getExpenseNo());
        if (StringUtils.isNotNull(info) && info.getExpenseId().longValue() != expenseId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 核销费用记录
     * 
     * @param expenseId 费用记录主键
     * @param verifyBy 核销人
     * @return 结果
     */
    @Transactional
    @Override
    public int verifyExpense(Long expenseId, String verifyBy)
    {
        FinExpense expense = finExpenseMapper.selectFinExpenseByExpenseId(expenseId);
        if (expense == null)
        {
            throw new ServiceException("费用记录不存在");
        }
        finAccountingPeriodService.assertPeriodEditable(expense.getPeriodId());
        if (VerifyStatus.VERIFIED.equals(expense.getStatus()))
        {
            throw new ServiceException("费用记录已核销，不可重复核销");
        }
        
        expense.setStatus(VerifyStatus.VERIFIED);
        expense.setVerifyBy(verifyBy);
        expense.setVerifyTime(new Date());
        
        return finExpenseMapper.updateFinExpense(expense);
    }

    /**
     * 批量核销费用记录
     * 
     * @param verifyVO 核销信息
     * @param verifyBy 核销人
     * @return 结果
     */
    @Transactional
    @Override
    public int batchVerifyExpense(ExpenseVerifyVO verifyVO, String verifyBy)
    {
        Date now = new Date();
        
        // 1. 查询选中的费用记录
        List<FinExpense> expenses = finExpenseMapper.selectFinExpenseByExpenseIds(verifyVO.getExpenseIds().toArray(new Long[0]));
        if (expenses == null || expenses.isEmpty())
        {
            throw new ServiceException("请选择要核销的费用记录");
        }

        // 2. 查询借支记录列表（可选）
        List<Long> advanceIdList = verifyVO.getAdvanceIds();
        List<FinAdvance> advances = Collections.emptyList();
        if (advanceIdList != null && !advanceIdList.isEmpty())
        {
            advances = finAdvanceMapper.selectFinAdvanceByAdvanceIds(advanceIdList.toArray(new Long[0]));
        }

        // 批量校验所有涉及的周期是否可编辑
        Set<Long> periodIds = new HashSet<>();
        for (FinExpense expense : expenses)
        {
            periodIds.add(expense.getPeriodId());
        }
        for (FinAdvance advance : advances)
        {
            if (advance != null)
            {
                periodIds.add(advance.getPeriodId());
            }
        }
        for (Long periodId : periodIds)
        {
            finAccountingPeriodService.assertPeriodEditable(periodId);
        }

        // 计算总核销费用
        BigDecimal totalExpenseAmount = BigDecimal.ZERO;
        for (FinExpense expense : expenses)
        {
            if (VerifyStatus.VERIFIED.equals(expense.getStatus()))
            {
                throw new ServiceException("费用记录[" + expense.getExpenseNo() + "]已核销");
            }
            totalExpenseAmount = totalExpenseAmount.add(expense.getExpenseAmount());
        }

        for (FinAdvance advance : advances)
        {
            if (VerifyStatus.VERIFIED.equals(advance.getStatus()))
            {
                throw new ServiceException("借支记录[" + advance.getAdvanceNo() + "]已核销");
            }
        }
        
        // 3. 核销所有选中的费用记录
        int count = 0;
        Long firstAdvanceId = (advances.isEmpty()) ? null : advances.get(0).getAdvanceId();
        for (FinExpense expense : expenses)
        {
            expense.setStatus(VerifyStatus.VERIFIED);
            expense.setVerifyBy(verifyBy);
            expense.setVerifyTime(now);
            expense.setAdvanceId(firstAdvanceId);
            finExpenseMapper.updateFinExpense(expense);
            count++;
        }

        // 没有借支记录时，仅核销费用记录即可
        if (advances.isEmpty())
        {
            if (!periodIds.isEmpty())
            {
                Long deptId = expenses.get(0).getDeptId();
                finAccountingPeriodService.selectCurrentPeriodByDeptId(deptId);
            }
            return count;
        }
        
        // 4. 核销所有选中的借支记录
        for (FinAdvance advance : advances)
        {
            advance.setStatus(VerifyStatus.VERIFIED);
            advance.setVerifyBy(verifyBy);
            advance.setVerifyTime(now);
            finAdvanceMapper.updateFinAdvance(advance);
        }
        
        // 5. 处理差额
        BigDecimal totalAdvanceAmount = BigDecimal.ZERO;
        for (FinAdvance advance : advances)
        {
            totalAdvanceAmount = totalAdvanceAmount.add(advance.getAdvanceAmount());
        }

        if (totalExpenseAmount.compareTo(totalAdvanceAmount) > 0)
        {
            // 核销总费用大于借支总金额，创建新的已核销借支记录（补款）
            BigDecimal newAmount = totalExpenseAmount.subtract(totalAdvanceAmount);
            FinAdvance newAdvance = new FinAdvance();
            newAdvance.setAdvanceNo(CodeGenerator.generateAdvanceNo(finAdvanceMapper.countTodayAdvances()));
            newAdvance.setDeptId(advances.get(0).getDeptId());
            newAdvance.setPeriodId(advances.get(0).getPeriodId());
            newAdvance.setAdvanceDate(now);
            newAdvance.setAdvanceAmount(newAmount);
            newAdvance.setBorrower(advances.get(0).getBorrower());
            newAdvance.setPurpose("核销补款");
            newAdvance.setStatus(VerifyStatus.VERIFIED);
            newAdvance.setVerifyBy(verifyBy);
            newAdvance.setVerifyTime(now);
            newAdvance.setCreateBy(verifyBy);
            newAdvance.setCreateTime(now);
            finAdvanceMapper.insertFinAdvance(newAdvance);
        }
        else if (totalExpenseAmount.compareTo(totalAdvanceAmount) < 0)
        {
            // 核销总费用小于借支总金额，创建新的未核销借支记录（节余）
            BigDecimal newAmount = totalAdvanceAmount.subtract(totalExpenseAmount);
            FinAdvance newAdvance = new FinAdvance();
            newAdvance.setAdvanceNo(CodeGenerator.generateAdvanceNo(finAdvanceMapper.countTodayAdvances()));
            newAdvance.setDeptId(advances.get(0).getDeptId());
            newAdvance.setPeriodId(advances.get(0).getPeriodId());
            newAdvance.setAdvanceDate(now);
            newAdvance.setAdvanceAmount(newAmount);
            newAdvance.setBorrower(advances.get(0).getBorrower());
            newAdvance.setPurpose("核销节余");
            newAdvance.setStatus(VerifyStatus.UNVERIFIED);
            newAdvance.setCreateBy(verifyBy);
            newAdvance.setCreateTime(now);
            finAdvanceMapper.insertFinAdvance(newAdvance);
        }
        // 如果相等，不需要额外处理
        finAccountingPeriodService.selectCurrentPeriodByDeptId(advances.get(0).getDeptId());
        
        return count;
    }

    private void fillCurrentPeriod(FinExpense finExpense)
    {
        if (finExpense.getPeriodId() == null && finExpense.getDeptId() != null)
        {
            FinAccountingPeriod period = finAccountingPeriodService.initCurrentPeriod(finExpense.getDeptId());
            finExpense.setPeriodId(period.getPeriodId());
        }
    }

    private void assertExpenseEditable(Long expenseId)
    {
        if (expenseId == null) {
            return;
        }
        FinExpense oldExpense = finExpenseMapper.selectFinExpenseByExpenseId(expenseId);
        if (oldExpense != null) {
            finAccountingPeriodService.assertPeriodEditable(oldExpense.getPeriodId());
        }
    }

    private void assertExpenseDeletable(Long expenseId)
    {
        if (expenseId == null) {
            return;
        }
        FinExpense oldExpense = finExpenseMapper.selectFinExpenseByExpenseId(expenseId);
        if (oldExpense != null) {
            finAccountingPeriodService.assertPeriodEditable(oldExpense.getPeriodId());
            if (VerifyStatus.VERIFIED.equals(oldExpense.getStatus()))
            {
                throw new ServiceException("已核销费用不能删除");
            }
        }
    }

    @Override
    public com.junsong.finance.domain.vo.ExpenseSummary getExpenseSummary(Long deptId)
    {
        com.junsong.finance.domain.vo.ExpenseSummary summary = new com.junsong.finance.domain.vo.ExpenseSummary();

        java.math.BigDecimal unverifiedAdvance = nvl(finAdvanceMapper.sumUnverifiedAdvancesByDeptId(deptId));
        java.math.BigDecimal unverifiedExpense = nvl(finExpenseMapper.sumUnverifiedExpensesByDeptId(deptId));
        FinAccountingPeriod currentPeriod = deptId == null ? null : finAccountingPeriodService.selectCurrentPeriodByDeptId(deptId);
        java.math.BigDecimal totalExpense = currentPeriod == null ? java.math.BigDecimal.ZERO : nvl(finExpenseMapper.sumAllExpensesByPeriodId(currentPeriod.getPeriodId()));

        summary.setUnverifiedAdvanceAmount(unverifiedAdvance);
        summary.setUnverifiedExpenseAmount(unverifiedExpense);
        summary.setAdvanceBalance(unverifiedAdvance.compareTo(java.math.BigDecimal.ZERO) == 0 ? java.math.BigDecimal.ZERO : unverifiedAdvance.subtract(unverifiedExpense));
        summary.setTotalExpenseAmount(totalExpense);

        return summary;
    }

    /**
     * 导入费用记录
     */
    @Override
    @Transactional
    public String importExpense(List<FinExpense> expenseList, boolean updateSupport, String operName, Long deptId) {
        if (expenseList == null || expenseList.isEmpty()) {
            throw new ServiceException("导入费用数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();

        // 有效的费用类别
        java.util.Set<String> validExpenseTypes = java.util.Set.of("预支", "开支", "收入");
        // 有效的付款方式
        java.util.Set<String> validPaymentMethods = java.util.Set.of("现金", "微信", "支付宝", "银行卡");

        for (FinExpense expense : expenseList) {
            try {
                // 验证费用类别
                if (expense.getExpenseType() == null || expense.getExpenseType().isEmpty()) {
                    throw new ServiceException("费用类别不能为空，可选值：预支、开支、收入");
                }
                String expenseType = expense.getExpenseType().trim();
                if (!validExpenseTypes.contains(expenseType)) {
                    throw new ServiceException("费用类别无效，可选值：预支、开支、收入");
                }
                expense.setExpenseType(expenseType);

                // 验证付款方式
                if (expense.getPaymentMethod() == null || expense.getPaymentMethod().isEmpty()) {
                    throw new ServiceException("付款方式不能为空，可选值：现金、微信、支付宝、银行卡");
                }
                String paymentMethod = expense.getPaymentMethod().trim();
                if (!validPaymentMethods.contains(paymentMethod)) {
                    throw new ServiceException("付款方式无效，可选值：现金、微信、支付宝、银行卡");
                }
                expense.setPaymentMethod(paymentMethod);

                // 验证费用日期
                if (expense.getExpenseDate() == null) {
                    throw new ServiceException("费用日期不能为空");
                }

                // 验证费用金额
                if (expense.getExpenseAmount() == null) {
                    throw new ServiceException("费用金额不能为空");
                }

                // 如果是收入类型，自动将金额转为负数
                if ("收入".equals(expense.getExpenseType()) && expense.getExpenseAmount().compareTo(BigDecimal.ZERO) > 0) {
                    expense.setExpenseAmount(expense.getExpenseAmount().negate());
                }

                normalizeImportStatus(expense, operName);

                // 判断用户是否填写了费用单号
                boolean userProvidedNo = !StringUtils.isEmpty(expense.getExpenseNo());
                FinExpense existExpense = null;
                if (userProvidedNo) {
                    existExpense = finExpenseMapper.checkExpenseNoUnique(expense.getExpenseNo().trim());
                    expense.setExpenseNo(expense.getExpenseNo().trim());
                }

                if (existExpense == null) {
                    // 新增：若用户未填写费用单号，则自动生成（带重试）
                    if (!userProvidedNo) {
                        int retryCount = 0;
                        int maxRetries = 10;
                        while (retryCount < maxRetries) {
                            try {
                                int todayCount = finExpenseMapper.countTodayExpenses();
                                expense.setExpenseNo(CodeGenerator.generateExpenseNo(todayCount + successNum + retryCount));
                                expense.setDeptId(deptId);
                                expense.setCreateBy(operName);
                                // 自动设置核算周期
                                fillCurrentPeriod(expense);
                                finExpenseMapper.insertFinExpense(expense);
                                break; // 插入成功，跳出重试循环
                            } catch (org.springframework.dao.DuplicateKeyException e) {
                                retryCount++;
                                if (retryCount >= maxRetries) {
                                    throw new ServiceException("费用单号生成失败，请稍后重试");
                                }
                            }
                        }
                    } else {
                        expense.setDeptId(deptId);
                        expense.setCreateBy(operName);
                        // 自动设置核算周期
                        fillCurrentPeriod(expense);
                        finExpenseMapper.insertFinExpense(expense);
                    }
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、费用单号 " + expense.getExpenseNo() + " 导入成功");
                } else if (updateSupport) {
                    // 更新
                    if (VerifyStatus.VERIFIED.equals(existExpense.getStatus())) {
                        throw new ServiceException("费用单号 " + expense.getExpenseNo() + " 已核销，不可更新");
                    }
                    expense.setExpenseId(existExpense.getExpenseId());
                    expense.setDeptId(deptId);
                    expense.setUpdateBy(operName);

                    finExpenseMapper.updateFinExpense(expense);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、费用单号 " + expense.getExpenseNo() + " 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、费用单号 " + expense.getExpenseNo() + " 已存在");
                }
            } catch (ServiceException se) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、费用单号 " + (StringUtils.isEmpty(expense.getExpenseNo()) ? "(自动生成)" : expense.getExpenseNo()) + " 导入失败：" + se.getMessage();
                failureMsg.append(msg);
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、费用单号 " + (StringUtils.isEmpty(expense.getExpenseNo()) ? "(自动生成)" : expense.getExpenseNo()) + " 导入失败：" + e.getMessage();
                failureMsg.append(msg);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

    private void normalizeImportStatus(FinExpense expense, String operName)
    {
        String status = StringUtils.isEmpty(expense.getStatus()) ? VerifyStatus.UNVERIFIED : expense.getStatus().trim();
        if ("未核销".equals(status))
        {
            status = VerifyStatus.UNVERIFIED;
        }
        else if ("已核销".equals(status))
        {
            status = VerifyStatus.VERIFIED;
        }
        if (!VerifyStatus.UNVERIFIED.equals(status) && !VerifyStatus.VERIFIED.equals(status))
        {
            throw new ServiceException("状态无效，可选值：未核销、已核销");
        }
        expense.setStatus(status);
        if (VerifyStatus.VERIFIED.equals(status))
        {
            if (StringUtils.isEmpty(expense.getVerifyBy()))
            {
                expense.setVerifyBy(operName);
            }
            if (expense.getVerifyTime() == null)
            {
                expense.setVerifyTime(new Date());
            }
        }
        else
        {
            expense.setVerifyBy(null);
            expense.setVerifyTime(null);
        }
    }

    private BigDecimal nvl(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }
}
