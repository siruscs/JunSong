package com.junsong.finance.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinSalePayment;
import com.junsong.finance.mapper.FinSalePaymentMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinSalePaymentService;

/**
 * 缴款记录Service业务层处理
 * 
 * @author junsong
 */
@Service
public class FinSalePaymentServiceImpl implements IFinSalePaymentService
{
    @Autowired
    private FinSalePaymentMapper finSalePaymentMapper;

    @Autowired
    private IFinAccountingPeriodService finAccountingPeriodService;

    /**
     * 查询缴款记录
     * 
     * @param paymentId 缴款记录主键
     * @return 缴款记录
     */
    @Override
    public FinSalePayment selectFinSalePaymentByPaymentId(Long paymentId)
    {
        return finSalePaymentMapper.selectFinSalePaymentByPaymentId(paymentId);
    }

    /**
     * 根据销售ID查询缴款记录列表
     * 
     * @param saleId 销售记录主键
     * @return 缴款记录集合
     */
    @Override
    public List<FinSalePayment> selectFinSalePaymentBySaleId(Long saleId)
    {
        return finSalePaymentMapper.selectFinSalePaymentBySaleId(saleId);
    }

    /**
     * 查询缴款记录列表
     * 
     * @param finSalePayment 缴款记录
     * @return 缴款记录
     */
    @Override
    public List<FinSalePayment> selectFinSalePaymentList(FinSalePayment finSalePayment)
    {
        return finSalePaymentMapper.selectFinSalePaymentList(finSalePayment);
    }

    /**
     * 新增缴款记录
     * 
     * @param finSalePayment 缴款记录
     * @return 结果
     */
    @Override
    public int insertFinSalePayment(FinSalePayment finSalePayment)
    {
        finSalePayment.setDeptId(SecurityUtils.getDeptId());
        fillCurrentPeriod(finSalePayment);
        finAccountingPeriodService.assertPeriodEditable(finSalePayment.getPeriodId());
        int rows = finSalePaymentMapper.insertFinSalePayment(finSalePayment);
        finAccountingPeriodService.selectCurrentPeriodByDeptId(finSalePayment.getDeptId());
        return rows;
    }

    private void fillCurrentPeriod(FinSalePayment finSalePayment)
    {
        if (finSalePayment.getPeriodId() == null && finSalePayment.getDeptId() != null)
        {
            FinAccountingPeriod period = finAccountingPeriodService.initCurrentPeriod(finSalePayment.getDeptId());
            finSalePayment.setPeriodId(period.getPeriodId());
        }
    }

    /**
     * 修改缴款记录
     * 
     * @param finSalePayment 缴款记录
     * @return 结果
     */
    @Override
    public int updateFinSalePayment(FinSalePayment finSalePayment)
    {
        assertPaymentEditable(finSalePayment.getPaymentId());
        finAccountingPeriodService.assertPeriodEditable(finSalePayment.getPeriodId());
        int rows = finSalePaymentMapper.updateFinSalePayment(finSalePayment);
        if (finSalePayment.getDeptId() != null)
        {
            finAccountingPeriodService.selectCurrentPeriodByDeptId(finSalePayment.getDeptId());
        }
        return rows;
    }

    /**
     * 批量删除缴款记录
     * 
     * @param paymentIds 需要删除的缴款记录主键集合
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteFinSalePaymentByPaymentIds(Long[] paymentIds)
    {
        if (paymentIds != null) {
            for (Long paymentId : paymentIds) {
                assertPaymentEditable(paymentId);
            }
        }
        return finSalePaymentMapper.deleteFinSalePaymentByPaymentIds(paymentIds);
    }

    /**
     * 删除缴款记录信息
     * 
     * @param paymentId 缴款记录主键
     * @return 结果
     */
    @Override
    public int deleteFinSalePaymentByPaymentId(Long paymentId)
    {
        assertPaymentEditable(paymentId);
        return finSalePaymentMapper.deleteFinSalePaymentByPaymentId(paymentId);
    }

    private void assertPaymentEditable(Long paymentId)
    {
        if (paymentId == null) {
            return;
        }
        FinSalePayment oldPayment = finSalePaymentMapper.selectFinSalePaymentByPaymentId(paymentId);
        if (oldPayment != null) {
            finAccountingPeriodService.assertPeriodEditable(oldPayment.getPeriodId());
        }
    }

    /**
     * 校验缴款单号是否唯一
     * 
     * @param finSalePayment 缴款记录信息
     * @return 结果
     */
    @Override
    public boolean checkPaymentNoUnique(FinSalePayment finSalePayment)
    {
        Long paymentId = finSalePayment.getPaymentId() == null ? -1L : finSalePayment.getPaymentId();
        FinSalePayment info = finSalePaymentMapper.checkPaymentNoUnique(finSalePayment.getPaymentNo());
        if (StringUtils.isNotNull(info) && info.getPaymentId().longValue() != paymentId.longValue())
        {
            return false;
        }
        return true;
    }
}
