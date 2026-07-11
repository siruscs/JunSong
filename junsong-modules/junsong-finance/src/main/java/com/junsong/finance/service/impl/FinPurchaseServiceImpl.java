package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinPurchase;
import com.junsong.finance.domain.FinPurchaseDetail;
import com.junsong.finance.mapper.FinPurchaseMapper;
import com.junsong.finance.mapper.FinPurchaseDetailMapper;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinPurchaseService;
import com.junsong.finance.service.IFinStockLedgerService;
import com.junsong.finance.util.CodeGenerator;

/**
 * 进货单Service业务层处理
 * 
 * @author junsong
 */
@Service
public class FinPurchaseServiceImpl implements IFinPurchaseService
{
    @Autowired
    private FinPurchaseMapper finPurchaseMapper;

    @Autowired
    private FinPurchaseDetailMapper finPurchaseDetailMapper;

    @Autowired
    private IFinAccountingPeriodService finAccountingPeriodService;

    @Autowired
    private IFinStockLedgerService finStockLedgerService;

    @Autowired
    private FinStockLedgerMapper finStockLedgerMapper;

    /**
     * 查询进货单
     * 
     * @param purchaseId 进货单主键
     * @return 进货单
     */
    @Override
    public FinPurchase selectFinPurchaseByPurchaseId(Long purchaseId)
    {
        FinPurchase purchase = finPurchaseMapper.selectFinPurchaseByPurchaseId(purchaseId);
        if (purchase != null)
        {
            FinPurchaseDetail detail = new FinPurchaseDetail();
            detail.setPurchaseId(purchaseId);
            List<FinPurchaseDetail> details = finPurchaseDetailMapper.selectFinPurchaseDetailList(detail);
            purchase.setDetails(details);
        }
        return purchase;
    }

    /**
     * 查询进货单列表
     * 
     * @param finPurchase 进货单
     * @return 进货单
     */
    @Override
    public List<FinPurchase> selectFinPurchaseList(FinPurchase finPurchase)
    {
        return finPurchaseMapper.selectFinPurchaseList(finPurchase);
    }

    /**
     * 新增进货单
     * 
     * @param finPurchase 进货单
     * @return 结果
     */
    @Transactional
    @Override
    public int insertFinPurchase(FinPurchase finPurchase)
    {
        // 自动生成进货单号（带重试机制防止并发重复）
        if (StringUtils.isEmpty(finPurchase.getPurchaseNo()))
        {
            int retryCount = 0;
            int maxRetries = 3;
            while (retryCount < maxRetries)
            {
                try
                {
                    int todayCount = finPurchaseMapper.countTodayPurchases();
                    finPurchase.setPurchaseNo(CodeGenerator.generatePurchaseNo(todayCount + retryCount));
                    return doInsertFinPurchase(finPurchase);
                }
                catch (DuplicateKeyException e)
                {
                    retryCount++;
                    if (retryCount >= maxRetries)
                    {
                        throw new ServiceException("进货单号生成失败，请稍后重试");
                    }
                }
            }
        }
        return doInsertFinPurchase(finPurchase);
    }

    private int doInsertFinPurchase(FinPurchase finPurchase)
    {
        finPurchase.setDeptId(SecurityUtils.getDeptId());
        fillCurrentPeriod(finPurchase);
        finAccountingPeriodService.assertPeriodEditable(finPurchase.getPeriodId());

        // 计算总金额和总数量（处理赠品）
        calculatePurchaseAmountAndQuantity(finPurchase);

        int rows = finPurchaseMapper.insertFinPurchase(finPurchase);
        insertFinPurchaseDetail(finPurchase);
        refreshPeriodStatsIfNeeded(finPurchase);
        applyPurchaseStockIn(finPurchase);
        return rows;
    }

    private void fillCurrentPeriod(FinPurchase finPurchase)
    {
        if (finPurchase.getPeriodId() == null && finPurchase.getDeptId() != null)
        {
            FinAccountingPeriod period = finAccountingPeriodService.initCurrentPeriod(finPurchase.getDeptId());
            finPurchase.setPeriodId(period.getPeriodId());
        }
    }
    
    /**
     * 计算进货单总金额和总数量（赠品不计入金额但计入数量）
     */
    private void calculatePurchaseAmountAndQuantity(FinPurchase finPurchase)
    {
        List<FinPurchaseDetail> details = finPurchase.getDetails();
        if (StringUtils.isNotNull(details))
        {
            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalQuantity = 0;
            
            for (FinPurchaseDetail detail : details)
            {
                // 如果是赠品，金额设为0
                if ("1".equals(detail.getIsGift()))
                {
                    detail.setPrice(BigDecimal.ZERO);
                    detail.setAmount(BigDecimal.ZERO);
                }
                else if (detail.getQuantity() != null && detail.getPrice() != null)
                {
                    // 正常商品计算金额
                    detail.setAmount(detail.getPrice().multiply(new BigDecimal(detail.getQuantity())));
                    totalAmount = totalAmount.add(detail.getAmount());
                }
                
                // 不管是不是赠品，都计入总数量
                if (detail.getQuantity() != null)
                {
                    totalQuantity += detail.getQuantity();
                }
            }
            
            finPurchase.setTotalAmount(totalAmount);
            finPurchase.setTotalQuantity(totalQuantity);
        }
    }

    /**
     * 修改进货单
     * 
     * @param finPurchase 进货单
     * @return 结果
     */
    @Transactional
    @Override
    public int updateFinPurchase(FinPurchase finPurchase)
    {
        assertPurchaseEditable(finPurchase.getPurchaseId());
        finAccountingPeriodService.assertPeriodEditable(finPurchase.getPeriodId());
        // 重新计算总金额和总数量
        calculatePurchaseAmountAndQuantity(finPurchase);
        
        finPurchaseMapper.deleteFinPurchaseDetailByPurchaseId(finPurchase.getPurchaseId());
        insertFinPurchaseDetail(finPurchase);
        int rows = finPurchaseMapper.updateFinPurchase(finPurchase);
        refreshPeriodStatsIfNeeded(finPurchase);
        applyPurchaseStockIn(finPurchase);
        return rows;
    }

    private void refreshPeriodStatsIfNeeded(FinPurchase finPurchase)
    {
        if (finPurchase.getDeptId() != null && ("1".equals(finPurchase.getStatus()) || "2".equals(finPurchase.getStatus())))
        {
            finAccountingPeriodService.selectCurrentPeriodByDeptId(finPurchase.getDeptId());
        }
    }

    /**
     * 采购入库时对账生成库存流水（支持新增/修改数量/删除明细）。
     * 已确认(1)或已完成(2)的采购单按明细目标数量对账；草稿(0)视为目标 0 反向冲销已入库量。
     * 对账天然幂等：内部按 reference_id + product_id 计算与已记录净额的差额。
     */
    void applyPurchaseStockIn(FinPurchase finPurchase)
    {
        if (finPurchase.getPurchaseId() == null)
        {
            return;
        }

        boolean active = "1".equals(finPurchase.getStatus()) || "2".equals(finPurchase.getStatus());

        // 汇总本次明细中每个商品的目标入库量（同一商品多明细合并）
        java.util.Map<Long, Integer> targetByProduct = new java.util.HashMap<>();
        java.util.Map<Long, String> nameByProduct = new java.util.HashMap<>();
        java.util.Map<Long, BigDecimal> costByProduct = new java.util.HashMap<>();
        if (active && finPurchase.getDetails() != null)
        {
            for (FinPurchaseDetail detail : finPurchase.getDetails())
            {
                if (detail.getProductId() == null || detail.getQuantity() == null || detail.getQuantity() <= 0)
                {
                    continue;
                }
                Long pid = detail.getProductId();
                targetByProduct.merge(pid, detail.getQuantity(), Integer::sum);
                nameByProduct.put(pid, detail.getProductName());
                costByProduct.put(pid, detail.getPrice() != null ? detail.getPrice() : BigDecimal.ZERO);
            }
        }

        // 并集：本次目标商品 + 该单历史已记录商品（历史有但本次没有 => 目标 0 反向冲销）
        java.util.Set<Long> productIds = new java.util.HashSet<>(targetByProduct.keySet());
        List<Long> recordedProductIds = finStockLedgerMapper.selectRecordedProductIds("PURCHASE", finPurchase.getPurchaseId());
        if (recordedProductIds != null)
        {
            productIds.addAll(recordedProductIds);
        }

        for (Long productId : productIds)
        {
            int target = targetByProduct.getOrDefault(productId, 0);
            String productName = nameByProduct.get(productId);
            BigDecimal cost = costByProduct.getOrDefault(productId, BigDecimal.ZERO);
            finStockLedgerService.reconcilePurchaseStock(
                    finPurchase.getDeptId(),
                    productId,
                    productName,
                    finPurchase.getPurchaseId(),
                    finPurchase.getPurchaseNo(),
                    target,
                    cost,
                    finPurchase.getCreateBy()
            );
        }
    }

    /**
     * 删除采购单前反向冲销其库存流水（对齐目标 0）。
     */
    void reversePurchaseStock(Long purchaseId, String purchaseNo, Long deptId, String operator)
    {
        if (purchaseId == null)
        {
            return;
        }
        List<Long> recordedProductIds = finStockLedgerMapper.selectRecordedProductIds("PURCHASE", purchaseId);
        if (recordedProductIds == null)
        {
            return;
        }
        for (Long productId : recordedProductIds)
        {
            finStockLedgerService.reconcilePurchaseStock(deptId, productId, null, purchaseId, purchaseNo,
                    0, BigDecimal.ZERO, operator);
        }
    }

    /**
     * 批量删除进货单
     * 
     * @param purchaseIds 需要删除的进货单主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteFinPurchaseByPurchaseIds(Long[] purchaseIds)
    {
        if (purchaseIds != null && purchaseIds.length > 0) {
            List<FinPurchase> list = finPurchaseMapper.selectFinPurchaseByPurchaseIds(purchaseIds);
            for (FinPurchase p : list) {
                assertPurchaseEditable(p);
                reversePurchaseStock(p.getPurchaseId(), p.getPurchaseNo(), p.getDeptId(), p.getUpdateBy());
            }
        }
        finPurchaseMapper.deleteFinPurchaseDetailByPurchaseIds(purchaseIds);
        return finPurchaseMapper.deleteFinPurchaseByPurchaseIds(purchaseIds);
    }

    /**
     * 删除进货单信息
     * 
     * @param purchaseId 进货单主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteFinPurchaseByPurchaseId(Long purchaseId)
    {
        assertPurchaseEditable(purchaseId);
        FinPurchase old = finPurchaseMapper.selectFinPurchaseByPurchaseId(purchaseId);
        if (old != null) {
            reversePurchaseStock(old.getPurchaseId(), old.getPurchaseNo(), old.getDeptId(), old.getUpdateBy());
        }
        finPurchaseMapper.deleteFinPurchaseDetailByPurchaseId(purchaseId);
        return finPurchaseMapper.deleteFinPurchaseByPurchaseId(purchaseId);
    }

    /**
     * 新增进货单明细信息
     * 
     * @param finPurchase 进货单对象
     */
    public void insertFinPurchaseDetail(FinPurchase finPurchase)
    {
        List<FinPurchaseDetail> finPurchaseDetailList = finPurchase.getDetails();
        Long purchaseId = finPurchase.getPurchaseId();
        if (StringUtils.isNotNull(finPurchaseDetailList))
        {
            for (FinPurchaseDetail finPurchaseDetail : finPurchaseDetailList)
            {
                finPurchaseDetail.setPurchaseId(purchaseId);
                // 标准化 isGift 值，把 true/false/1/0 统一转换为 "1" 或 "0"
                normalizeIsGift(finPurchaseDetail);
            }
            if (finPurchaseDetailList.size() > 0)
            {
                finPurchaseMapper.batchFinPurchaseDetail(finPurchaseDetailList);
            }
        }
    }

    private void assertPurchaseEditable(Long purchaseId)
    {
        if (purchaseId == null) {
            return;
        }
        FinPurchase oldPurchase = finPurchaseMapper.selectFinPurchaseByPurchaseId(purchaseId);
        assertPurchaseEditable(oldPurchase);
    }

    private void assertPurchaseEditable(FinPurchase purchase)
    {
        if (purchase != null) {
            finAccountingPeriodService.assertPeriodEditable(purchase.getPeriodId());
        }
    }
    
    /**
     * 标准化 isGift 值
     */
    private void normalizeIsGift(FinPurchaseDetail detail)
    {
        String isGift = detail.getIsGift();
        if (StringUtils.isEmpty(isGift))
        {
            detail.setIsGift("0");
            return;
        }
        else if ("true".equalsIgnoreCase(isGift)
            || "1".equals(isGift)
            || "yes".equalsIgnoreCase(isGift))
        {
            detail.setIsGift("1");
        }
        else
        {
            detail.setIsGift("0");
        }
    }

    /**
     * 校验进货单号是否唯一
     * 
     * @param finPurchase 进货单信息
     * @return 结果
     */
    @Override
    public boolean checkPurchaseNoUnique(FinPurchase finPurchase)
    {
        Long purchaseId = StringUtils.isNull(finPurchase.getPurchaseId()) ? -1L : finPurchase.getPurchaseId();
        FinPurchase info = finPurchaseMapper.checkPurchaseNoUnique(finPurchase.getPurchaseNo());
        if (StringUtils.isNotNull(info) && info.getPurchaseId().longValue() != purchaseId.longValue())
        {
            return false;
        }
        return true;
    }
}
