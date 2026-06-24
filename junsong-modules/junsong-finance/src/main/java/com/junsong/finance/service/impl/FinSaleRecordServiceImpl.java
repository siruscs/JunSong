package com.junsong.finance.service.impl;

import com.junsong.common.core.exception.ServiceException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.domain.FinSalePayment;
import com.junsong.finance.mapper.FinSaleRecordMapper;
import com.junsong.finance.mapper.FinSalePaymentMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinSaleRecordService;
import com.junsong.finance.constant.PaymentStatus;
import com.junsong.finance.util.CodeGenerator;

/**
 * 销售记录Service业务层处理
 * 
 * @author junsong
 */
@Service
public class FinSaleRecordServiceImpl implements IFinSaleRecordService
{
    @Autowired
    private FinSaleRecordMapper finSaleRecordMapper;

    @Autowired
    private FinSalePaymentMapper finSalePaymentMapper;

    @Autowired
    private IFinAccountingPeriodService finAccountingPeriodService;

    /**
     * 查询销售记录
     * 
     * @param saleId 销售记录主键
     * @return 销售记录
     */
    @Override
    public FinSaleRecord selectFinSaleRecordBySaleId(Long saleId)
    {
        FinSaleRecord sale = finSaleRecordMapper.selectFinSaleRecordBySaleId(saleId);
        if (sale != null)
        {
            List<FinSalePayment> payments = finSalePaymentMapper.selectFinSalePaymentBySaleId(saleId);
            sale.setPayments(payments);
        }
        return sale;
    }

    /**
     * 查询销售记录列表
     * 
     * @param finSaleRecord 销售记录
     * @return 销售记录
     */
    @Override
    public List<FinSaleRecord> selectFinSaleRecordList(FinSaleRecord finSaleRecord)
    {
        return finSaleRecordMapper.selectFinSaleRecordList(finSaleRecord);
    }

    /**
     * 新增销售记录
     * 
     * @param finSaleRecord 销售记录
     * @return 结果
     */
    @Transactional
    @Override
    public int insertFinSaleRecord(FinSaleRecord finSaleRecord)
    {
        // 自动生成销售单号
        if (StringUtils.isEmpty(finSaleRecord.getSaleNo()))
        {
            int todayCount = finSaleRecordMapper.countTodaySales();
            finSaleRecord.setSaleNo(CodeGenerator.generateSaleNo(todayCount));
        }
        
        // 自动设置部门ID
        finSaleRecord.setDeptId(SecurityUtils.getDeptId());
        fillCurrentPeriod(finSaleRecord);
        finAccountingPeriodService.assertPeriodEditable(finSaleRecord.getPeriodId());
        
        // 计算总数量和单价
        calculateSaleQuantityAndUnitPrice(finSaleRecord);
        
        // 初始状态为待缴款
        finSaleRecord.setStatus(PaymentStatus.PENDING);
        finSaleRecord.setPaidAmount(BigDecimal.ZERO);
        
        int rows = finSaleRecordMapper.insertFinSaleRecord(finSaleRecord);
        return rows;
    }

    private void fillCurrentPeriod(FinSaleRecord finSaleRecord)
    {
        if (finSaleRecord.getPeriodId() == null && finSaleRecord.getDeptId() != null)
        {
            FinAccountingPeriod period = finAccountingPeriodService.initCurrentPeriod(finSaleRecord.getDeptId());
            finSaleRecord.setPeriodId(period.getPeriodId());
        }
    }
    
    /**
     * 计算销售记录的总数量和单价
     */
    private void calculateSaleQuantityAndUnitPrice(FinSaleRecord finSaleRecord)
    {
        Integer saleQuantity = finSaleRecord.getSaleQuantity();
        Integer giftQuantity = finSaleRecord.getGiftQuantity();
        
        // 计算总数量
        int totalQuantity = (saleQuantity != null ? saleQuantity : 0) + (giftQuantity != null ? giftQuantity : 0);
        finSaleRecord.setTotalQuantity(totalQuantity);
        
        // 计算单价：单价 = 销售金额 / 销售数量
        BigDecimal saleAmount = finSaleRecord.getSaleAmount();
        if (saleAmount != null && saleQuantity != null && saleQuantity > 0)
        {
            BigDecimal unitPrice = saleAmount.divide(new BigDecimal(saleQuantity), 2, BigDecimal.ROUND_HALF_UP);
            finSaleRecord.setUnitPrice(unitPrice);
        }
        else
        {
            finSaleRecord.setUnitPrice(BigDecimal.ZERO);
        }
    }

    /**
     * 修改销售记录
     * 
     * @param finSaleRecord 销售记录
     * @return 结果
     */
    @Transactional
    @Override
    public int updateFinSaleRecord(FinSaleRecord finSaleRecord)
    {
        assertSaleEditable(finSaleRecord.getSaleId());
        finAccountingPeriodService.assertPeriodEditable(finSaleRecord.getPeriodId());
        // 重新计算总数量和单价
        calculateSaleQuantityAndUnitPrice(finSaleRecord);
        return finSaleRecordMapper.updateFinSaleRecord(finSaleRecord);
    }

    /**
     * 批量删除销售记录
     * 
     * @param saleIds 需要删除的销售记录主键集合
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteFinSaleRecordBySaleIds(Long[] saleIds)
    {
        if (saleIds != null) {
            for (Long saleId : saleIds) {
                assertSaleEditable(saleId);
            }
        }
        // 先删除对应的缴款记录
        finSalePaymentMapper.deleteFinSalePaymentBySaleIds(saleIds);
        return finSaleRecordMapper.deleteFinSaleRecordBySaleIds(saleIds);
    }

    /**
     * 删除销售记录信息
     * 
     * @param saleId 销售记录主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteFinSaleRecordBySaleId(Long saleId)
    {
        assertSaleEditable(saleId);
        // 先删除对应的缴款记录
        finSalePaymentMapper.deleteFinSalePaymentBySaleId(saleId);
        return finSaleRecordMapper.deleteFinSaleRecordBySaleId(saleId);
    }

    private void assertSaleEditable(Long saleId)
    {
        if (saleId == null) {
            return;
        }
        FinSaleRecord oldSale = finSaleRecordMapper.selectFinSaleRecordBySaleId(saleId);
        if (oldSale != null) {
            finAccountingPeriodService.assertPeriodEditable(oldSale.getPeriodId());
        }
        List<FinSalePayment> payments = finSalePaymentMapper.selectFinSalePaymentBySaleId(saleId);
        if (payments != null) {
            for (FinSalePayment payment : payments) {
                finAccountingPeriodService.assertPeriodEditable(payment.getPeriodId());
            }
        }
    }

    /**
     * 校验销售单号是否唯一
     * 
     * @param finSaleRecord 销售记录信息
     * @return 结果
     */
    @Override
    public boolean checkSaleNoUnique(FinSaleRecord finSaleRecord)
    {
        Long saleId = finSaleRecord.getSaleId() == null ? -1L : finSaleRecord.getSaleId();
        FinSaleRecord info = finSaleRecordMapper.checkSaleNoUnique(finSaleRecord.getSaleNo());
        if (StringUtils.isNotNull(info) && info.getSaleId().longValue() != saleId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 添加缴款记录
     * 
     * @param saleId 销售记录主键
     * @param paymentAmount 缴款金额
     * @param paymentMethod 付款方式
     * @param remark 备注
     * @param paymentDate 缴款日期
     * @return 结果
     */
    @Transactional
    @Override
    public int addPayment(Long saleId, BigDecimal paymentAmount, String paymentMethod, String remark, Date paymentDate)
    {
        // 查询销售记录
        FinSaleRecord sale = finSaleRecordMapper.selectFinSaleRecordBySaleId(saleId);
        if (sale == null)
        {
            throw new ServiceException("销售记录不存在");
        }
        
        // 创建缴款记录
        FinSalePayment payment = new FinSalePayment();
        int todayCount = finSalePaymentMapper.countTodayPayments();
        payment.setPaymentNo(CodeGenerator.generatePaymentNo(todayCount));
        payment.setSaleId(saleId);
        payment.setDeptId(sale.getDeptId());
        payment.setPeriodId(getCurrentPeriodId(sale.getDeptId()));
        payment.setPaymentAmount(paymentAmount);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentDate(paymentDate != null ? paymentDate : new Date());
        payment.setRemark(remark);
        
        // 插入缴款记录
        int rows = finSalePaymentMapper.insertFinSalePayment(payment);
        finAccountingPeriodService.selectCurrentPeriodByDeptId(sale.getDeptId());
        
        // 更新销售记录的已缴金额和状态
        updateSalePaidAmountAndStatus(saleId);
        
        return rows;
    }

    private Long getCurrentPeriodId(Long deptId)
    {
        FinAccountingPeriod period = finAccountingPeriodService.initCurrentPeriod(deptId);
        return period.getPeriodId();
    }

    /**
     * 修改缴款记录
     * 
     * @param paymentId 缴款记录主键
     * @param paymentAmount 缴款金额
     * @param paymentMethod 付款方式
     * @param remark 备注
     * @param paymentDate 缴款日期
     * @return 结果
     */
    @Transactional
    @Override
    public int updatePayment(Long paymentId, BigDecimal paymentAmount, String paymentMethod, String remark, Date paymentDate)
    {
        // 查询缴款记录
        FinSalePayment payment = finSalePaymentMapper.selectFinSalePaymentByPaymentId(paymentId);
        if (payment == null)
        {
            throw new ServiceException("缴款记录不存在");
        }
        
        // 更新缴款记录
        payment.setPaymentAmount(paymentAmount);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentDate(paymentDate != null ? paymentDate : new Date());
        payment.setRemark(remark);
        
        int rows = finSalePaymentMapper.updateFinSalePayment(payment);
        
        // 更新销售记录的已缴金额和状态
        updateSalePaidAmountAndStatus(payment.getSaleId());
        
        return rows;
    }

    /**
     * 删除缴款记录
     * 
     * @param paymentId 缴款记录主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deletePayment(Long paymentId)
    {
        // 查询缴款记录
        FinSalePayment payment = finSalePaymentMapper.selectFinSalePaymentByPaymentId(paymentId);
        if (payment == null)
        {
            throw new ServiceException("缴款记录不存在");
        }
        
        Long saleId = payment.getSaleId();
        
        // 删除缴款记录
        int rows = finSalePaymentMapper.deleteFinSalePaymentByPaymentId(paymentId);
        
        // 更新销售记录的已缴金额和状态
        updateSalePaidAmountAndStatus(saleId);
        
        return rows;
    }

    /**
     * 更新销售记录的已缴金额和状态
     */
    private void updateSalePaidAmountAndStatus(Long saleId)
    {
        FinSaleRecord sale = finSaleRecordMapper.selectFinSaleRecordBySaleId(saleId);
        if (sale == null)
        {
            return;
        }
        
        List<FinSalePayment> payments = finSalePaymentMapper.selectFinSalePaymentBySaleId(saleId);
        BigDecimal totalPaid = BigDecimal.ZERO;
        for (FinSalePayment payment : payments)
        {
            if (payment.getPaymentAmount() != null)
            {
                totalPaid = totalPaid.add(payment.getPaymentAmount());
            }
        }
        
        sale.setPaidAmount(totalPaid);
        
        // 计算状态
        BigDecimal saleAmount = sale.getSaleAmount();
        if (saleAmount == null || saleAmount.compareTo(BigDecimal.ZERO) == 0)
        {
            sale.setStatus(PaymentStatus.PAID); // 已缴清（金额为0的情况）
        }
        else if (totalPaid.compareTo(BigDecimal.ZERO) == 0)
        {
            sale.setStatus(PaymentStatus.PENDING); // 待缴款
        }
        else if (totalPaid.compareTo(saleAmount) >= 0)
        {
            sale.setStatus(PaymentStatus.PAID); // 已缴清
        }
        else
        {
            sale.setStatus(PaymentStatus.PARTIAL); // 部分缴款
        }
        
        finSaleRecordMapper.updateFinSaleRecord(sale);
    }
}
