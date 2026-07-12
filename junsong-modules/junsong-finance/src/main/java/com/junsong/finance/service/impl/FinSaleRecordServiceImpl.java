package com.junsong.finance.service.impl;

import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.domain.FinSalePayment;
import com.junsong.finance.mapper.FinSaleRecordMapper;
import com.junsong.finance.mapper.FinSalePaymentMapper;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinSaleRecordService;
import com.junsong.finance.service.IFinStockLedgerService;
import com.junsong.finance.constant.PaymentStatus;
import com.junsong.finance.util.CodeGenerator;
import com.junsong.member.api.RemoteMemberGrowthService;
import com.junsong.member.api.domain.SaleGrowthAwardReq;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;

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

    @Autowired
    private FinAuditTrailRecorder auditTrailRecorder;

    @Autowired
    private IFinStockLedgerService finStockLedgerService;

    @Autowired
    private FinStockLedgerMapper finStockLedgerMapper;

    @Autowired
    private RemoteMemberGrowthService remoteMemberGrowthService;

    @Autowired(required = false)
    private RemoteUserService remoteUserService;

    private List<Long> authorizedDeptIdsOverride;

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
     * 查询未缴清销售单（历史欠款）列表
     *
     * @param finSaleRecord 查询条件
     * @return 未缴清销售记录集合
     */
    @Override
    public List<FinSaleRecord> selectReceivableList(FinSaleRecord finSaleRecord)
    {
        return finSaleRecordMapper.selectReceivableList(finSaleRecord);
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
        // 自动设置部门ID
        finSaleRecord.setDeptId(SecurityUtils.getDeptId());
        fillCurrentPeriod(finSaleRecord);
        finAccountingPeriodService.assertPeriodEditable(finSaleRecord.getPeriodId());

        // 计算总数量和单价
        calculateSaleQuantityAndUnitPrice(finSaleRecord);

        // 初始状态为待缴款
        finSaleRecord.setStatus(PaymentStatus.PENDING);
        finSaleRecord.setPaidAmount(BigDecimal.ZERO);

        // 自动生成销售单号（带重试机制防止并发重复）
        if (StringUtils.isEmpty(finSaleRecord.getSaleNo()))
        {
            int retryCount = 0;
            int maxRetries = 3;
            while (retryCount < maxRetries) {
                try {
                    int maxSeq = finSaleRecordMapper.maxTodaySaleSeq();
                    finSaleRecord.setSaleNo(CodeGenerator.generateSaleNo(maxSeq + 1 + retryCount));
                    int rows = finSaleRecordMapper.insertFinSaleRecord(finSaleRecord);
                    applySaleStockOut(finSaleRecord);
                    awardSaleGrowthIfNeeded(finSaleRecord);
                    return rows;
                } catch (DuplicateKeyException e) {
                    retryCount++;
                    if (retryCount >= maxRetries) {
                        throw new ServiceException("销售单号生成失败，请稍后重试");
                    }
                }
            }
        }

        int rows = finSaleRecordMapper.insertFinSaleRecord(finSaleRecord);
        applySaleStockOut(finSaleRecord);
        awardSaleGrowthIfNeeded(finSaleRecord);
        return rows;
    }

    /**
     * 销售单创建成功后调用会员成长入账（仅 memberId != null 时调用）。
     * 通过 SALE:{saleId} 幂等键防止重复发放。
     * 调用失败抛出异常以触发事务回滚（设计文档 5.1 节推荐策略）。
     */
    private void awardSaleGrowthIfNeeded(FinSaleRecord finSaleRecord)
    {
        if (finSaleRecord.getMemberId() == null)
        {
            return;
        }
        SaleGrowthAwardReq req = new SaleGrowthAwardReq();
        req.setMemberId(finSaleRecord.getMemberId());
        req.setMemberNo(finSaleRecord.getMemberNo());
        req.setMemberName(finSaleRecord.getMemberName());
        req.setDeptId(finSaleRecord.getDeptId());
        req.setSaleId(finSaleRecord.getSaleId());
        req.setSaleAmount(finSaleRecord.getSaleAmount());
        req.setOperator(finSaleRecord.getCreateBy() != null ? finSaleRecord.getCreateBy() : "finance");

        R<Boolean> result = remoteMemberGrowthService.awardSaleGrowth(req, SecurityConstants.FROM_SOURCE);
        if (result == null || !R.isSuccess(result))
        {
            String msg = result != null ? result.getMsg() : "远程调用返回空";
            throw new ServiceException("会员成长值入账失败，销售单已回滚: " + msg);
        }
    }

    /**
     * 销售出库对账生成库存流水（支持新增/修改数量/删除）。
     * 出库总量 = 销售数量 + 赠品数量（赠品同样消耗实物库存）。
     * 关键约束：缺少商品或有效销售数量时必须阻断并抛出异常，不允许造假扣减。
     * 对账天然幂等：内部按 reference_id + product_id 计算与已记录净额的差额。
     */
    void applySaleStockOut(FinSaleRecord finSaleRecord)
    {
        if (finSaleRecord.getSaleId() == null)
        {
            return;
        }
        assertAuthorizedStockDept(finSaleRecord.getDeptId());
        if (finSaleRecord.getProductId() == null)
        {
            throw new ServiceException("销售出库失败：销售记录缺少商品，无法扣减库存");
        }
        if (finSaleRecord.getSaleQuantity() == null || finSaleRecord.getSaleQuantity() <= 0)
        {
            throw new ServiceException("销售出库失败：销售记录缺少有效数量，无法扣减库存");
        }
        int giftQuantity = finSaleRecord.getGiftQuantity() != null ? finSaleRecord.getGiftQuantity() : 0;
        if (giftQuantity < 0)
        {
            throw new ServiceException("销售出库失败：赠品数量不能为负数");
        }
        int outQuantity;
        try
        {
            outQuantity = Math.addExact(finSaleRecord.getSaleQuantity(), giftQuantity);
        }
        catch (ArithmeticException ex)
        {
            throw new ServiceException("销售出库失败：销售数量与赠品数量合计超出允许范围");
        }

        // 并集：本次目标商品 + 该单历史已记录商品（换商品时旧商品历史 SALE_OUT 需目标 0 反向回补）
        Long currentProductId = finSaleRecord.getProductId();
        java.util.Set<Long> productIds = new java.util.HashSet<>();
        productIds.add(currentProductId);
        List<Long> recordedProductIds = finStockLedgerMapper.selectRecordedProductIds(TenantContext.getTenantId(), "SALE", finSaleRecord.getSaleId());
        if (recordedProductIds != null)
        {
            productIds.addAll(recordedProductIds);
        }

        for (Long productId : productIds)
        {
            boolean isCurrent = productId.equals(currentProductId);
            int target = isCurrent ? outQuantity : 0;
            String productName = isCurrent ? finSaleRecord.getProductName() : null;
            finStockLedgerService.reconcileSaleStock(
                    TenantContext.getTenantId(),
                    finSaleRecord.getDeptId(),
                    productId,
                    productName,
                    finSaleRecord.getSaleId(),
                    finSaleRecord.getSaleNo(),
                    target,
                    finSaleRecord.getCreateBy()
            );
        }
    }

    /**
     * 删除销售记录前反向回补库存（对齐目标 0）。
     */
    void reverseSaleStock(FinSaleRecord old)
    {
        if (old == null || old.getSaleId() == null || old.getProductId() == null)
        {
            return;
        }
        assertAuthorizedStockDept(old.getDeptId());
        finStockLedgerService.reconcileSaleStock(
                TenantContext.getTenantId(),
                old.getDeptId(),
                old.getProductId(),
                old.getProductName(),
                old.getSaleId(),
                old.getSaleNo(),
                0,
                old.getUpdateBy()
        );
    }

    private void assertAuthorizedStockDept(Long deptId)
    {
        if (deptId == null)
        {
            throw new ServiceException("销售库存缺少门店上下文");
        }
        if (!SecurityUtils.isAdmin() && !loadAuthorizedStockDeptIds().contains(deptId))
        {
            throw new ServiceException("无权操作该门店的销售库存");
        }
    }

    private List<Long> loadAuthorizedStockDeptIds()
    {
        if (authorizedDeptIdsOverride != null)
        {
            return authorizedDeptIdsOverride;
        }
        try
        {
            R<List<SysDept>> result = remoteUserService == null ? null
                    : remoteUserService.getUserDeptList(SecurityUtils.getUsername(), SecurityConstants.INNER);
            if (result != null && result.getData() != null)
            {
                return result.getData().stream().map(SysDept::getDeptId)
                        .filter(java.util.Objects::nonNull).toList();
            }
        }
        catch (Exception ignored)
        {
            // Fail closed to the currently selected department when the authorization service is unavailable.
        }
        Long currentDeptId = SecurityUtils.getDeptId();
        return currentDeptId == null ? java.util.Collections.emptyList()
                : java.util.Collections.singletonList(currentDeptId);
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
        int rows = finSaleRecordMapper.updateFinSaleRecord(finSaleRecord);
        // 补齐对账所需上下文后按新数量对账（差额或反向）
        FinSaleRecord current = finSaleRecordMapper.selectFinSaleRecordBySaleId(finSaleRecord.getSaleId());
        if (current != null)
        {
            applySaleStockOut(current);
        }
        return rows;
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
                FinSaleRecord old = finSaleRecordMapper.selectFinSaleRecordBySaleId(saleId);
                if (old != null) {
                    reverseSaleStock(old);
                    auditTrailRecorder.record("delete_sale", "fin_sale_record", String.valueOf(saleId),
                            "{\"saleNo\":\"" + old.getSaleNo() + "\",\"amount\":" + old.getSaleAmount() + "}", null);
                }
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
        FinSaleRecord old = finSaleRecordMapper.selectFinSaleRecordBySaleId(saleId);
        if (old != null) {
            reverseSaleStock(old);
            auditTrailRecorder.record("delete_sale", "fin_sale_record", String.valueOf(saleId),
                    "{\"saleNo\":\"" + old.getSaleNo() + "\",\"amount\":" + old.getSaleAmount() + "}", null);
        }
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
        // 查询销售记录（行锁保护，防止并发缴款超额）
        FinSaleRecord sale = finSaleRecordMapper.selectFinSaleRecordBySaleIdForUpdate(saleId);
        if (sale == null)
        {
            throw new ServiceException("销售记录不存在");
        }

        // 跨周期补缴款改造：缴款归属"实际缴款周期"，不再校验销售单原周期是否可编辑。
        // 允许对历史（原周期已结转）未缴清销售单继续新增缴款。
        // 缴款周期 = 缴款发生时门店当前进行中周期；若无进行中周期则自动初始化。
        Long currentPeriodId = getCurrentPeriodId(sale.getDeptId());
        // 校验的是"当前缴款周期"可编辑（正常进行中周期恒可编辑，此处防御性拦截异常状态周期）
        finAccountingPeriodService.assertPeriodEditable(currentPeriodId);

        // 超额缴款保护：缴款后累计已缴不能超出销售金额范围
        BigDecimal totalPaid = finSalePaymentMapper.sumPaymentAmountBySaleId(saleId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        BigDecimal saleAmount = sale.getSaleAmount();
        BigDecimal afterPaid = totalPaid.add(paymentAmount);
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new ServiceException("缴款金额不能为0");
        }
        if (saleAmount.compareTo(BigDecimal.ZERO) > 0) {
            // 正向销售：0 <= afterPaid <= saleAmount
            if (afterPaid.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("退货缴款金额不能超过已缴金额");
            }
            if (afterPaid.compareTo(saleAmount) > 0) {
                throw new ServiceException("缴款金额不能大于剩余应收金额");
            }
        } else if (saleAmount.compareTo(BigDecimal.ZERO) < 0) {
            // 退货销售：saleAmount <= afterPaid <= 0
            if (afterPaid.compareTo(BigDecimal.ZERO) > 0) {
                throw new ServiceException("退款后累计已缴不能为正数");
            }
            if (afterPaid.compareTo(saleAmount) < 0) {
                throw new ServiceException("退货缴款金额不能超过应退金额");
            }
        }

        // 创建缴款记录
        FinSalePayment payment = new FinSalePayment();
        payment.setSaleId(saleId);
        payment.setDeptId(sale.getDeptId());
        payment.setPeriodId(currentPeriodId);
        payment.setPaymentAmount(paymentAmount);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentDate(paymentDate != null ? paymentDate : new Date());
        payment.setRemark(remark);

        // 生成缴款单号（带重试机制防止并发重复）
        int retryCount = 0;
        int maxRetries = 3;
        while (retryCount < maxRetries)
        {
            try
            {
                int todayCount = finSalePaymentMapper.countTodayPayments();
                payment.setPaymentNo(CodeGenerator.generatePaymentNo(todayCount + retryCount));
                int rows = finSalePaymentMapper.insertFinSalePayment(payment);
                finAccountingPeriodService.selectCurrentPeriodByDeptId(sale.getDeptId());

                // 更新销售记录的已缴金额和状态
                updateSalePaidAmountAndStatus(saleId);

                return rows;
            }
            catch (DuplicateKeyException e)
            {
                retryCount++;
                if (retryCount >= maxRetries)
                {
                    throw new ServiceException("缴款单号生成失败，请稍后重试");
                }
            }
        }
        return 0;
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

        // 期间锁：已结转期间不允许修改缴款
        if (payment.getPeriodId() != null) {
            finAccountingPeriodService.assertPeriodEditable(payment.getPeriodId());
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

        // 期间锁：已结转期间不允许删除缴款
        if (payment.getPeriodId() != null) {
            finAccountingPeriodService.assertPeriodEditable(payment.getPeriodId());
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
     *
     * 跨周期补缴款改造：仅更新 paid_amount / status 两个累计字段，
     * 不修改任何销售业务字段，也不校验销售单原周期是否可编辑，
     * 允许在原销售周期已结转后随新周期缴款而更新累计缴款状态。
     */
    private void updateSalePaidAmountAndStatus(Long saleId)
    {
        BigDecimal totalPaid = finSalePaymentMapper.sumPaymentAmountBySaleId(saleId);
        if (totalPaid == null)
        {
            totalPaid = BigDecimal.ZERO;
        }
        FinSaleRecord sale = finSaleRecordMapper.selectFinSaleRecordBySaleId(saleId);
        if (sale == null)
        {
            return;
        }

        // 状态判断逻辑：已缴金额绝对值达到销售金额绝对值即为已缴清
        String status;
        BigDecimal saleAmount = sale.getSaleAmount();
        if (saleAmount == null || saleAmount.compareTo(BigDecimal.ZERO) == 0)
        {
            status = PaymentStatus.PAID; // 已缴清（金额为0的情况）
        }
        else if (totalPaid.compareTo(BigDecimal.ZERO) == 0)
        {
            status = PaymentStatus.PENDING; // 待缴款
        }
        else if (totalPaid.abs().compareTo(saleAmount.abs()) >= 0)
        {
            status = PaymentStatus.PAID; // 已缴清
        }
        else
        {
            status = PaymentStatus.PARTIAL; // 部分缴款
        }

        // 仅更新累计缴款字段，绕开全字段更新与期间锁校验
        finSaleRecordMapper.updatePaidAmountAndStatus(saleId, totalPaid, status);
    }
}
