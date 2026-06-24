package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinPurchase;
import com.junsong.finance.domain.FinPurchaseDetail;
import com.junsong.finance.mapper.FinPurchaseMapper;
import com.junsong.finance.mapper.FinPurchaseDetailMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinPurchaseService;
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
        // 自动生成进货单号
        if (StringUtils.isEmpty(finPurchase.getPurchaseNo()))
        {
            int todayCount = finPurchaseMapper.countTodayPurchases();
            finPurchase.setPurchaseNo(CodeGenerator.generatePurchaseNo(todayCount));
        }
        finPurchase.setDeptId(SecurityUtils.getDeptId());
        fillCurrentPeriod(finPurchase);
        finAccountingPeriodService.assertPeriodEditable(finPurchase.getPeriodId());
        
        // 计算总金额和总数量（处理赠品）
        calculatePurchaseAmountAndQuantity(finPurchase);
        
        int rows = finPurchaseMapper.insertFinPurchase(finPurchase);
        insertFinPurchaseDetail(finPurchase);
        refreshPeriodStatsIfNeeded(finPurchase);
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
     * 批量删除进货单
     * 
     * @param purchaseIds 需要删除的进货单主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteFinPurchaseByPurchaseIds(Long[] purchaseIds)
    {
        if (purchaseIds != null) {
            for (Long purchaseId : purchaseIds) {
                assertPurchaseEditable(purchaseId);
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
        if (oldPurchase != null) {
            finAccountingPeriodService.assertPeriodEditable(oldPurchase.getPeriodId());
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
