package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.constant.PeriodStatus;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinCompositeAccountingPool;
import com.junsong.finance.domain.FinCompositePeriodItem;
import com.junsong.finance.domain.FinCompositePoolDept;
import com.junsong.finance.domain.FinCompositePoolInvestor;
import com.junsong.finance.domain.vo.CompositeCandidatePeriodVO;
import com.junsong.finance.domain.vo.CompositeAccountingSummaryVO;
import com.junsong.finance.domain.vo.CompositePoolOverviewVO;
import com.junsong.finance.domain.vo.CompositeTrialResultVO;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.mapper.FinCompositeAccountingMapper;
import com.junsong.finance.service.IFinCompositeAccountingService;

/**
 * 复合核算服务实现
 *
 * @author junsong
 */
@Service
public class FinCompositeAccountingServiceImpl implements IFinCompositeAccountingService
{
    private static final Logger log = LoggerFactory.getLogger(FinCompositeAccountingServiceImpl.class);

    /** 复合核算池状态:0=进行中 1=已达回本 2=已确认回本 3=已关闭 4=草稿 */
    private static final String POOL_STATUS_ACTIVE = "0";
    private static final String POOL_STATUS_BREAK_EVEN = "1";
    private static final String POOL_STATUS_CONFIRMED = "2";
    private static final String POOL_STATUS_CLOSED = "3";
    private static final String POOL_STATUS_DRAFT = "4";

    /** 纳入方式:0=自动 1=手动 */
    private static final String INCLUDE_MODE_AUTO = "0";
    private static final String INCLUDE_MODE_MANUAL = "1";

    @Autowired
    private FinCompositeAccountingMapper compositeMapper;

    @Autowired
    private FinAccountingPeriodMapper finAccountingPeriodMapper;

    @Autowired
    private FinAuditTrailRecorder auditTrailRecorder;

    /**
     * 用于自动纳入的独立事务模板。
     * 自动纳入在单店结转事务中调用,必须用独立事务隔离:
     * 自动纳入失败时仅回滚自身,不影响单店结转主事务。
     */
    @Autowired
    private TransactionTemplate transactionTemplate;

    // ==================== 查询 ====================

    @Override
    public List<FinCompositeAccountingPool> selectCompositePoolList(FinCompositeAccountingPool pool) {
        // tenant_id 由 TenantSqlInterceptor 自动注入(主表受保护)
        // 非管理员可查看自己创建的草稿/复合池,以及关联到自己店面的复合核算池
        if (!SecurityUtils.isAdmin()) {
            Long userId = SecurityUtils.getUserId();
            List<Long> userDeptIds = compositeMapper.selectUserDeptIdsByUserId(userId);
            pool.getParams().put("currentCreateBy", SecurityUtils.getUsername());
            pool.getParams().put("userDeptIds", userDeptIds == null ? new ArrayList<>() : userDeptIds);
        }
        return compositeMapper.selectCompositePoolList(pool);
    }

    @Override
    public FinCompositeAccountingPool selectCompositePoolByPoolId(Long poolId) {
        if (poolId == null) {
            return null;
        }
        // tenant_id 由 TenantSqlInterceptor 自动注入
        return compositeMapper.selectCompositePoolByPoolId(poolId);
    }

    @Override
    public CompositePoolOverviewVO getOverview(Long poolId) {
        // requirePool 已通过主表租户隔离校验归属
        FinCompositeAccountingPool pool = requirePool(poolId);
        CompositePoolOverviewVO vo = new CompositePoolOverviewVO();
        vo.setPool(pool);
        vo.setDepts(compositeMapper.selectPoolDeptsByPoolId(poolId));
        vo.setInvestors(compositeMapper.selectPoolInvestorsByPoolId(poolId));
        vo.setPeriodItems(compositeMapper.selectPeriodItemsByPoolId(poolId));
        CompositeAccountingSummaryVO summary = compositeMapper.selectSummaryByPoolId(poolId);
        if (summary == null) {
            summary = new CompositeAccountingSummaryVO();
        }
        summary.setBreakEvenGap(nvl(pool.getBreakEvenGap()));
        summary.setTheoreticalBreakEvenGap(nvl(summary.getTotalVerifiedExpense())
                .add(nvl(summary.getTotalPurchase()))
                .add(nvl(summary.getTotalUnverifiedAdvance()))
                .subtract(nvl(summary.getTotalSalePayment())));
        vo.setSummary(summary);

        BigDecimal totalInvest = nvl(pool.getTotalInvestAmount());
        BigDecimal totalReturn = nvl(pool.getTotalReturnAmount());
        if (totalInvest.compareTo(BigDecimal.ZERO) > 0) {
            vo.setBreakEvenProgress(totalReturn.divide(totalInvest, 4, RoundingMode.HALF_UP));
        }
        return vo;
    }

    @Override
    public List<FinCompositePeriodItem> listPeriods(Long poolId) {
        requirePool(poolId);
        return compositeMapper.selectPeriodItemsByPoolId(poolId);
    }

    // ==================== 创建 / 修改 / 删除 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createPool(FinCompositeAccountingPool pool) {
        if (pool == null) {
            throw new ServiceException("复合核算池不能为空");
        }
        if (StringUtils.isEmpty(pool.getPoolName())) {
            throw new ServiceException("复合核算池名称不能为空");
        }
        pool.setPoolNo(buildPoolNo(new Date()));
        // 创建时为草稿状态,需绑定店面和投资人后才转为进行中
        pool.setStatus(POOL_STATUS_DRAFT);
        pool.setTotalInvestAmount(nvl(pool.getTotalInvestAmount()));
        pool.setTotalReturnAmount(BigDecimal.ZERO);
        pool.setBreakEvenGap(pool.getTotalInvestAmount());
        pool.setOverReturnAmount(BigDecimal.ZERO);
        // 显式写入当前租户(兜底,TenantInterceptor 也会自动填充)
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            pool.setTenantId(tenantId);
        }
        pool.setCreateBy(SecurityUtils.getUsername());
        compositeMapper.insertCompositePool(pool);

        auditTrailRecorder.record("composite_pool_create", "composite_pool", String.valueOf(pool.getPoolId()),
                "{}", "{\"poolId\":" + pool.getPoolId() + ",\"poolName\":\"" + escapeJson(pool.getPoolName()) + "\"}");
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePool(FinCompositeAccountingPool pool) {
        if (pool == null || pool.getPoolId() == null) {
            throw new ServiceException("复合核算池ID不能为空");
        }
        FinCompositeAccountingPool existing = requirePool(pool.getPoolId());
        if (POOL_STATUS_CLOSED.equals(existing.getStatus())) {
            throw new ServiceException("已关闭的复合核算池不允许修改");
        }
        pool.setUpdateBy(SecurityUtils.getUsername());
        int rows = compositeMapper.updateCompositePool(pool);
        if (rows > 0) {
            recalculatePool(pool.getPoolId());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCompositePoolByPoolIds(Long[] poolIds) {
        if (poolIds == null || poolIds.length == 0) {
            return 0;
        }
        for (Long poolId : poolIds) {
            FinCompositeAccountingPool pool = compositeMapper.selectCompositePoolByPoolId(poolId);
            if (pool != null && POOL_STATUS_ACTIVE.equals(pool.getStatus())) {
                throw new ServiceException("进行中的复合核算池[" + pool.getPoolName() + "]不允许删除,请先关闭");
            }
        }
        for (Long poolId : poolIds) {
            compositeMapper.deletePoolDeptByPoolId(poolId);
            compositeMapper.deletePoolInvestorByPoolId(poolId);
            compositeMapper.deletePeriodItemByPoolId(poolId);
        }
        return compositeMapper.deleteCompositePoolByPoolIds(poolIds);
    }

    // ==================== 维护参与店面 / 投资人 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindDepts(Long poolId, List<Long> deptIds) {
        FinCompositeAccountingPool pool = requirePool(poolId);
        if (POOL_STATUS_CLOSED.equals(pool.getStatus())) {
            throw new ServiceException("已关闭的复合核算池不允许修改参与店面");
        }
        if (deptIds == null || deptIds.size() < 2) {
            throw new ServiceException("复合核算池至少选择 2 个店面");
        }
        Set<Long> deptIdSet = new HashSet<>(deptIds);
        if (deptIdSet.size() != deptIds.size()) {
            throw new ServiceException("同一个店面不能重复添加");
        }

        // 校验店面是否已加入其他进行中的池
        for (Long deptId : deptIdSet) {
            FinCompositeAccountingPool other = compositeMapper.selectActivePoolByDeptId(deptId);
            if (other != null && !other.getPoolId().equals(poolId)) {
                throw new ServiceException("店面[" + deptId + "]已加入其他进行中的复合核算池[" + other.getPoolName() + "]");
            }
        }

        compositeMapper.deletePoolDeptByPoolId(poolId);
        Date now = new Date();
        String username = SecurityUtils.getUsername();
        for (Long deptId : deptIdSet) {
            String deptName = compositeMapper.selectDeptNameById(deptId);
            FinCompositePoolDept pd = new FinCompositePoolDept();
            pd.setPoolId(poolId);
            pd.setDeptId(deptId);
            pd.setDeptName(deptName != null ? deptName : String.valueOf(deptId));
            pd.setJoinTime(now);
            pd.setStatus("0");
            pd.setCreateBy(username);
            compositeMapper.insertPoolDept(pd);
        }
        // 绑定店面后尝试激活草稿
        tryActivateDraft(poolId);
        autoIncludeAllPeriodsForActivePool(poolId);
        return deptIdSet.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindInvestors(Long poolId, List<InvestorInput> investors) {
        FinCompositeAccountingPool pool = requirePool(poolId);
        if (POOL_STATUS_CLOSED.equals(pool.getStatus())) {
            throw new ServiceException("已关闭的复合核算池不允许修改共享投资人");
        }
        if (investors == null || investors.isEmpty()) {
            throw new ServiceException("至少选择 1 个共享投资人");
        }

        Set<Long> investorIdSet = new HashSet<>();
        BigDecimal totalInvest = BigDecimal.ZERO;
        for (InvestorInput input : investors) {
            if (input.getInvestorId() == null) {
                throw new ServiceException("投资人ID不能为空");
            }
            if (!investorIdSet.add(input.getInvestorId())) {
                throw new ServiceException("同一个投资人不能重复添加");
            }
            BigDecimal amt = nvl(input.getInvestAmount());
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("投资人[" + input.getInvestorName() + "]出资款必须大于 0");
            }
            totalInvest = totalInvest.add(amt);
        }

        compositeMapper.deletePoolInvestorByPoolId(poolId);
        String username = SecurityUtils.getUsername();
        for (InvestorInput input : investors) {
            BigDecimal ratio = totalInvest.compareTo(BigDecimal.ZERO) > 0
                    ? nvl(input.getInvestAmount()).divide(totalInvest, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            FinCompositePoolInvestor pi = new FinCompositePoolInvestor();
            pi.setPoolId(poolId);
            pi.setInvestorId(input.getInvestorId());
            pi.setInvestorName(input.getInvestorName());
            pi.setInvestAmount(nvl(input.getInvestAmount()));
            pi.setInvestRatio(ratio);
            pi.setReturnedAmount(BigDecimal.ZERO);
            pi.setStatus("0");
            pi.setCreateBy(username);
            compositeMapper.insertPoolInvestor(pi);
        }

        pool.setTotalInvestAmount(totalInvest);
        pool.setUpdateBy(username);
        compositeMapper.updateCompositePool(pool);
        recalculatePool(poolId);
        // 绑定投资人后尝试激活草稿
        tryActivateDraft(poolId);
        autoIncludeAllPeriodsForActivePool(poolId);
        return investors.size();
    }

    /**
     * 草稿状态激活:当店面(>=2)和投资人(>=1)都齐全时,草稿转为进行中。
     */
    private void tryActivateDraft(Long poolId) {
        FinCompositeAccountingPool pool = compositeMapper.selectCompositePoolByPoolId(poolId);
        if (pool == null || !POOL_STATUS_DRAFT.equals(pool.getStatus())) {
            return;
        }
        List<Long> deptIds = compositeMapper.selectPoolDeptIdsByPoolId(poolId);
        List<FinCompositePoolInvestor> investors = compositeMapper.selectPoolInvestorsByPoolId(poolId);
        if (deptIds != null && deptIds.size() >= 2 && investors != null && !investors.isEmpty()) {
            pool.setStatus(POOL_STATUS_ACTIVE);
            pool.setUpdateBy(SecurityUtils.getUsername());
            compositeMapper.updateCompositePool(pool);
            auditTrailRecorder.record("composite_pool_activate", "composite_pool", String.valueOf(poolId),
                    "{\"status\":\"4\"}", "{\"status\":\"0\"}");
        }
    }

    // ==================== 候选周期 / 试算 / 确认纳入 ====================

    @Override
    public List<CompositeCandidatePeriodVO> listCandidatePeriods(Long poolId, Long deptId) {
        requirePool(poolId);
        if (deptId == null) {
            throw new ServiceException("店面ID不能为空");
        }
        // [P1#4] 校验 deptId 属于当前复合池参与店面,防止跨池查询
        assertDeptBelongsToPool(poolId, deptId);

        FinAccountingPeriod query = new FinAccountingPeriod();
        query.setDeptId(deptId);
        query.setStatus(PeriodStatus.CARRIED);
        // fin_accounting_period 受 TenantSqlInterceptor 保护,自动带租户边界
        List<FinAccountingPeriod> periods = finAccountingPeriodMapper.selectFinAccountingPeriodList(query);

        List<CompositeCandidatePeriodVO> result = new ArrayList<>();
        for (FinAccountingPeriod p : periods) {
            CompositeCandidatePeriodVO vo = new CompositeCandidatePeriodVO();
            vo.setPeriodId(p.getPeriodId());
            vo.setDeptId(p.getDeptId());
            vo.setDeptName(String.valueOf(p.getDeptId()));
            vo.setPeriodNo(p.getPeriodNo());
            vo.setNetProfit(nvl(p.getNetProfit()));
            vo.setManagerProfitAmount(nvl(p.getManagerProfitAmount()));
            vo.setInvestorProfitAmount(calcIncludeAmount(p));
            vo.setCarryForwardTime(formatTime(p.getCarryForwardTime()));
            FinCompositePeriodItem item = compositeMapper.selectPeriodItemByPeriodId(p.getPeriodId());
            vo.setIncludedByOther(item != null);
            result.add(vo);
        }
        return result;
    }

    @Override
    public CompositeTrialResultVO trialIncludePeriods(Long poolId, List<Long> periodIds) {
        FinCompositeAccountingPool pool = requirePool(poolId);
        if (POOL_STATUS_CLOSED.equals(pool.getStatus())) {
            throw new ServiceException("已关闭的复合核算池不允许纳入周期");
        }
        CompositeTrialResultVO result = new CompositeTrialResultVO();
        if (periodIds == null || periodIds.isEmpty()) {
            result.setTotalReturnAmount(nvl(pool.getTotalReturnAmount()));
            result.setTotalInvestAmount(nvl(pool.getTotalInvestAmount()));
            result.setBreakEvenGap(nvl(pool.getBreakEvenGap()));
            result.setOverReturnAmount(nvl(pool.getOverReturnAmount()));
            result.setBreakEvenReached(nvl(pool.getTotalReturnAmount()).compareTo(nvl(pool.getTotalInvestAmount())) >= 0);
            result.setInvestorAllocations(buildInvestorAllocations(poolId, BigDecimal.ZERO));
            return result;
        }

        // [P1#3] 校验所有周期属于该复合池参与店面,防止跨店纳入
        Set<Long> poolDeptIds = new HashSet<>(compositeMapper.selectPoolDeptIdsByPoolId(poolId));
        BigDecimal currentInclude = BigDecimal.ZERO;
        for (Long periodId : periodIds) {
            FinCompositePeriodItem existing = compositeMapper.selectPeriodItemByPoolIdAndPeriodId(poolId, periodId);
            if (existing != null) {
                throw new ServiceException("周期[" + periodId + "]已纳入当前池,不能重复纳入");
            }
            FinCompositePeriodItem other = compositeMapper.selectPeriodItemByPeriodId(periodId);
            if (other != null) {
                throw new ServiceException("周期[" + periodId + "]已被其他复合核算池纳入");
            }
            FinAccountingPeriod period = finAccountingPeriodMapper.selectFinAccountingPeriodByPeriodId(periodId);
            if (period == null) {
                throw new ServiceException("周期[" + periodId + "]不存在");
            }
            if (!PeriodStatus.CARRIED.equals(period.getStatus())) {
                throw new ServiceException("周期[" + period.getPeriodNo() + "]未结转,不能纳入");
            }
            // 边界校验:周期所属店面必须属于该复合池
            if (!poolDeptIds.contains(period.getDeptId())) {
                throw new ServiceException("周期[" + period.getPeriodNo() + "]所属店面不属于该复合核算池,不能纳入");
            }
            currentInclude = currentInclude.add(calcIncludeAmount(period));
        }

        BigDecimal totalReturn = nvl(pool.getTotalReturnAmount()).add(currentInclude);
        BigDecimal totalInvest = nvl(pool.getTotalInvestAmount());
        BigDecimal gap = totalInvest.subtract(totalReturn).max(BigDecimal.ZERO);
        BigDecimal over = totalReturn.subtract(totalInvest).max(BigDecimal.ZERO);

        result.setCurrentIncludeAmount(currentInclude);
        result.setTotalReturnAmount(totalReturn);
        result.setTotalInvestAmount(totalInvest);
        result.setBreakEvenGap(gap);
        result.setOverReturnAmount(over);
        result.setBreakEvenReached(totalReturn.compareTo(totalInvest) >= 0);
        if (totalInvest.compareTo(BigDecimal.ZERO) > 0) {
            result.setBreakEvenProgress(totalReturn.divide(totalInvest, 4, RoundingMode.HALF_UP));
        }
        result.setInvestorAllocations(buildInvestorAllocations(poolId, currentInclude));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmIncludePeriods(Long poolId, List<Long> periodIds) {
        FinCompositeAccountingPool pool = requirePool(poolId);
        if (POOL_STATUS_CLOSED.equals(pool.getStatus())) {
            throw new ServiceException("已关闭的复合核算池不允许纳入周期");
        }
        if (POOL_STATUS_ACTIVE.equals(pool.getStatus())) {
            throw new ServiceException("进行中的复合核算池仅支持自动纳入,手动纳入请在已达回本后操作");
        }
        if (POOL_STATUS_DRAFT.equals(pool.getStatus())) {
            throw new ServiceException("草稿状态的复合核算池不允许纳入周期,请先完成店面和投资人维护");
        }
        if (periodIds == null || periodIds.isEmpty()) {
            return 0;
        }

        // 试算校验(含跨店边界校验)
        trialIncludePeriods(poolId, periodIds);

        String username = SecurityUtils.getUsername();
        Date now = new Date();
        int count = 0;
        for (Long periodId : periodIds) {
            FinAccountingPeriod period = finAccountingPeriodMapper.selectFinAccountingPeriodByPeriodId(periodId);
            if (period == null) {
                continue;
            }
            FinCompositePeriodItem item = new FinCompositePeriodItem();
            item.setPoolId(poolId);
            item.setDeptId(period.getDeptId());
            item.setPeriodId(period.getPeriodId());
            item.setPeriodNo(period.getPeriodNo());
            item.setNetProfit(nvl(period.getNetProfit()));
            item.setManagerProfitAmount(nvl(period.getManagerProfitAmount()));
            item.setInvestorProfitAmount(calcIncludeAmount(period));
            item.setIncludedMode(INCLUDE_MODE_MANUAL);
            item.setIncludedTime(now);
            item.setIncludedBy(username);
            item.setCreateBy(username);
            // [P1#6] 数据库层 uk_active_period 全局唯一约束兜底并发
            compositeMapper.insertPeriodItem(item);
            count++;
        }
        recalculatePool(poolId);
        auditTrailRecorder.record("composite_pool_manual_include", "composite_pool", String.valueOf(poolId),
                "{\"periodIds\":" + periodIds + "}", "{\"count\":" + count + "}");
        return count;
    }

    // ==================== 自动纳入 ====================

    @Override
    public void autoIncludeAfterPeriodCarryForward(Long periodId) {
        if (periodId == null) {
            return;
        }
        // [P0#2] 自动纳入使用独立事务,内部失败仅回滚自身,不阻断单店结转
        try {
            transactionTemplate.execute(status -> {
                doAutoInclude(periodId);
                return null;
            });
        } catch (Exception e) {
            // 独立事务已回滚,单店结转主事务不受影响
            log.error("复合核算池自动纳入周期[{}]失败,已回滚自动纳入操作:{}", periodId, e.getMessage(), e);
        }
    }

    /**
     * 自动纳入核心逻辑(在独立事务中执行,异常会触发回滚)。
     */
    private void doAutoInclude(Long periodId) {
        FinAccountingPeriod period = finAccountingPeriodMapper.selectFinAccountingPeriodByPeriodId(periodId);
        if (period == null) {
            return;
        }
        if (!PeriodStatus.CARRIED.equals(period.getStatus())) {
            return;
        }

        FinCompositeAccountingPool pool = compositeMapper.selectActivePoolByDeptId(period.getDeptId());
        if (pool == null) {
            return;
        }
        // 仅进行中状态才自动纳入(草稿/已达回本/已确认/已关闭都不纳入)
        if (!POOL_STATUS_ACTIVE.equals(pool.getStatus())) {
            return;
        }

        // 防重复(数据库 uk_active_period 全局唯一约束兜底并发)
        FinCompositePeriodItem existing = compositeMapper.selectPeriodItemByPeriodId(periodId);
        if (existing != null) {
            return;
        }

        String username = SecurityUtils.getUsername();
        Date now = new Date();
        FinCompositePeriodItem item = buildPeriodItem(pool.getPoolId(), period, INCLUDE_MODE_AUTO, now, username);
        compositeMapper.insertPeriodItem(item);

        // 重算在同一独立事务内,失败则整体回滚,不会出现"已纳入但未刷新累计"的不一致
        recalculatePool(pool.getPoolId());
        log.info("复合核算池[{}]自动纳入周期[{}]成功,纳入金额={}", pool.getPoolId(), periodId, item.getInvestorProfitAmount());
    }

    @Override
    public boolean isPeriodIncludedInComposite(Long periodId) {
        if (periodId == null) {
            return false;
        }
        return compositeMapper.selectPeriodItemByPeriodId(periodId) != null;
    }

    // ==================== 重新计算 / 确认回本 / 关闭 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recalculatePool(Long poolId) {
        FinCompositeAccountingPool pool = requirePool(poolId);
        List<FinCompositePeriodItem> items = compositeMapper.selectPeriodItemsByPoolId(poolId);

        BigDecimal totalReturn = BigDecimal.ZERO;
        for (FinCompositePeriodItem item : items) {
            totalReturn = totalReturn.add(nvl(item.getInvestorProfitAmount()));
        }
        BigDecimal totalInvest = nvl(pool.getTotalInvestAmount());
        BigDecimal gap = totalInvest.subtract(totalReturn).max(BigDecimal.ZERO);
        BigDecimal over = totalReturn.subtract(totalInvest).max(BigDecimal.ZERO);

        pool.setTotalReturnAmount(totalReturn);
        pool.setBreakEvenGap(gap);
        pool.setOverReturnAmount(over);

        boolean reached = totalReturn.compareTo(totalInvest) >= 0 && totalInvest.compareTo(BigDecimal.ZERO) > 0;
        if (POOL_STATUS_ACTIVE.equals(pool.getStatus()) && reached) {
            pool.setStatus(POOL_STATUS_BREAK_EVEN);
            pool.setBreakEvenTime(new Date());
        } else if (POOL_STATUS_BREAK_EVEN.equals(pool.getStatus()) && !reached) {
            pool.setStatus(POOL_STATUS_ACTIVE);
            pool.setBreakEvenTime(null);
        }

        pool.setUpdateBy(SecurityUtils.getUsername());
        int rows = compositeMapper.updateCompositePool(pool);

        List<FinCompositePoolInvestor> investors = compositeMapper.selectPoolInvestorsByPoolId(poolId);
        for (FinCompositePoolInvestor inv : investors) {
            BigDecimal ratio = nvl(inv.getInvestRatio());
            BigDecimal returned = totalReturn.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            compositeMapper.updateInvestorReturnedAmount(inv.getId(), returned);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmBreakEven(Long poolId) {
        FinCompositeAccountingPool pool = requirePool(poolId);
        if (!POOL_STATUS_BREAK_EVEN.equals(pool.getStatus())) {
            throw new ServiceException("只有已达回本状态的复合核算池才能确认回本");
        }
        pool.setStatus(POOL_STATUS_CONFIRMED);
        pool.setConfirmedTime(new Date());
        pool.setConfirmedBy(SecurityUtils.getUsername());
        pool.setUpdateBy(SecurityUtils.getUsername());
        int rows = compositeMapper.updateCompositePool(pool);
        auditTrailRecorder.record("composite_pool_confirm_break_even", "composite_pool", String.valueOf(poolId),
                "{\"status\":\"1\"}", "{\"status\":\"2\"}");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closePool(Long poolId) {
        FinCompositeAccountingPool pool = requirePool(poolId);
        if (POOL_STATUS_CLOSED.equals(pool.getStatus())) {
            throw new ServiceException("复合核算池已关闭,无需重复操作");
        }
        String beforeStatus = pool.getStatus();
        pool.setStatus(POOL_STATUS_CLOSED);
        pool.setUpdateBy(SecurityUtils.getUsername());
        int rows = compositeMapper.updateCompositePool(pool);
        auditTrailRecorder.record("composite_pool_close", "composite_pool", String.valueOf(poolId),
                "{\"status\":\"" + beforeStatus + "\"}", "{\"status\":\"3\"}");
        return rows;
    }

    // ==================== 私有工具方法 ====================

    /**
     * 复合池处于进行中时,补齐参与店面的全部核算周期。
     * 用于店面加入/草稿激活后的周期归集;回本后状态会由 recalculatePool 自动切换,之后不再自动补齐。
     */
    private int autoIncludeAllPeriodsForActivePool(Long poolId) {
        FinCompositeAccountingPool pool = compositeMapper.selectCompositePoolByPoolId(poolId);
        if (pool == null || !POOL_STATUS_ACTIVE.equals(pool.getStatus())) {
            return 0;
        }
        List<Long> deptIds = compositeMapper.selectPoolDeptIdsByPoolId(poolId);
        if (deptIds == null || deptIds.isEmpty()) {
            return 0;
        }

        String username = SecurityUtils.getUsername();
        Date now = new Date();
        int count = 0;
        for (Long deptId : deptIds) {
            FinAccountingPeriod query = new FinAccountingPeriod();
            query.setDeptId(deptId);
            List<FinAccountingPeriod> periods = finAccountingPeriodMapper.selectFinAccountingPeriodList(query);
            for (FinAccountingPeriod period : periods) {
                if (period == null || period.getPeriodId() == null) {
                    continue;
                }
                if (compositeMapper.selectPeriodItemByPeriodId(period.getPeriodId()) != null) {
                    continue;
                }
                FinCompositePeriodItem item = buildPeriodItem(poolId, period, INCLUDE_MODE_AUTO, now, username);
                compositeMapper.insertPeriodItem(item);
                count++;
            }
        }
        if (count > 0) {
            recalculatePool(poolId);
            log.info("复合核算池[{}]批量补齐核算周期{}个", poolId, count);
        }
        return count;
    }

    private FinCompositeAccountingPool requirePool(Long poolId) {
        if (poolId == null) {
            throw new ServiceException("复合核算池ID不能为空");
        }
        // 主表受 TenantSqlInterceptor 保护,跨租户查询会返回 null
        FinCompositeAccountingPool pool = compositeMapper.selectCompositePoolByPoolId(poolId);
        if (pool == null) {
            throw new ServiceException("复合核算池不存在或已删除");
        }
        return pool;
    }

    /**
     * [P1#3/P1#4] 校验店面属于复合池参与店面
     */
    private void assertDeptBelongsToPool(Long poolId, Long deptId) {
        List<Long> poolDeptIds = compositeMapper.selectPoolDeptIdsByPoolId(poolId);
        if (poolDeptIds == null || !poolDeptIds.contains(deptId)) {
            throw new ServiceException("店面[" + deptId + "]不属于该复合核算池");
        }
    }

    private BigDecimal calcIncludeAmount(FinAccountingPeriod period) {
        if (period == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal investorAmount = nvl(period.getInvestorProfitAmount());
        if (investorAmount.compareTo(BigDecimal.ZERO) > 0) {
            return investorAmount;
        }
        BigDecimal fallback = nvl(period.getNetProfit()).subtract(nvl(period.getManagerProfitAmount())).max(BigDecimal.ZERO);
        return fallback;
    }

    private FinCompositePeriodItem buildPeriodItem(Long poolId, FinAccountingPeriod period, String includeMode, Date includedTime, String includedBy) {
        FinCompositePeriodItem item = new FinCompositePeriodItem();
        item.setPoolId(poolId);
        item.setDeptId(period.getDeptId());
        item.setPeriodId(period.getPeriodId());
        item.setPeriodNo(period.getPeriodNo());
        item.setNetProfit(nvl(period.getNetProfit()));
        item.setManagerProfitAmount(nvl(period.getManagerProfitAmount()));
        item.setInvestorProfitAmount(calcIncludeAmount(period));
        item.setIncludedMode(includeMode);
        item.setIncludedTime(includedTime);
        item.setIncludedBy(includedBy);
        item.setCreateBy(includedBy);
        return item;
    }

    private List<CompositeTrialResultVO.InvestorAllocationRow> buildInvestorAllocations(Long poolId, BigDecimal currentInclude) {
        FinCompositeAccountingPool pool = compositeMapper.selectCompositePoolByPoolId(poolId);
        BigDecimal totalReturn = nvl(pool.getTotalReturnAmount()).add(currentInclude);
        List<FinCompositePoolInvestor> investors = compositeMapper.selectPoolInvestorsByPoolId(poolId);
        List<CompositeTrialResultVO.InvestorAllocationRow> rows = new ArrayList<>();
        for (FinCompositePoolInvestor inv : investors) {
            CompositeTrialResultVO.InvestorAllocationRow row = new CompositeTrialResultVO.InvestorAllocationRow();
            row.setInvestorId(inv.getInvestorId());
            row.setInvestorName(inv.getInvestorName());
            row.setInvestAmount(nvl(inv.getInvestAmount()));
            row.setInvestRatio(nvl(inv.getInvestRatio()));
            BigDecimal currentAlloc = currentInclude.multiply(nvl(inv.getInvestRatio())).setScale(2, RoundingMode.HALF_UP);
            row.setCurrentAllocation(currentAlloc);
            BigDecimal totalReturned = totalReturn.multiply(nvl(inv.getInvestRatio())).setScale(2, RoundingMode.HALF_UP);
            row.setTotalReturned(totalReturned);
            rows.add(row);
        }
        return rows;
    }

    private String buildPoolNo(Date date) {
        String time = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(date == null ? new Date() : date);
        int random = (int) (Math.random() * 900) + 100;
        return "CP" + time + random;
    }

    private String formatTime(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
