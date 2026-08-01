package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinProduct;
import com.junsong.finance.domain.FinStockInitBatch;
import com.junsong.finance.domain.FinStockInitItem;
import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.domain.vo.StockInitApproveRequest;
import com.junsong.finance.domain.vo.StockInitCreateRequest;
import com.junsong.finance.domain.vo.StockInitDetailVO;
import com.junsong.finance.domain.vo.StockInitItemInput;
import com.junsong.finance.domain.vo.StockInitPostRequest;
import com.junsong.finance.domain.vo.StockInitQuery;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.mapper.FinProductMapper;
import com.junsong.finance.mapper.FinStockInitBatchMapper;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.service.IFinStockInitService;
import com.junsong.finance.service.IStockCostService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.common.core.domain.R;

/**
 * 期初库存 Service 实现。
 *
 * 状态机：DRAFT → VALIDATED → SUBMITTED → APPROVED → POSTED
 *
 * 安全契约：
 * 1. 所有方法从 TenantContext 获取租户ID，缺失即拒绝
 * 2. 所有读写按授权部门集合过滤（admin 跳过；非 admin 与 RemoteUserService.getUserDeptList 求交集）
 * 3. 状态流转使用乐观锁 version 谓词
 * 4. batchNo 服务端生成（SI + 时间戳），不接受客户端传入
 * 5. 创建时校验商品归属门店，金额 = 数量 × 单位成本（scale 2 HALF_UP）
 * 6. 过账幂等键 post_idempotency_key 租户内唯一
 * 7. 过账时会计期间必须为 ACTIVE，按 (deptId, productId) 升序锁定行避免死锁
 * 8. 审批人不能是创建人
 *
 * @author junsong
 */
@Service
public class FinStockInitServiceImpl implements IFinStockInitService {

    private static final List<Long> SENTINEL_DEPT_IDS = Collections.singletonList(-1L);
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_VALIDATED = "VALIDATED";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_POSTED = "POSTED";
    private static final String STOCK_INIT = "STOCK_INIT";
    private static final String REF_STOCK_INIT = "STOCK_INIT";
    private static final String PERIOD_ACTIVE = "0";

    private static BigDecimal nzDec(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Autowired
    private FinStockInitBatchMapper finStockInitBatchMapper;

    @Autowired
    private FinStockLedgerMapper finStockLedgerMapper;

    @Autowired
    private FinProductMapper finProductMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private IStockCostService stockCostService;

    @Autowired
    private FinAccountingPeriodMapper accountingPeriodMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockInit(StockInitCreateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止创建期初库存批次");
        }

        if (request.getAdjustmentDate() != null) request.setInitDate(request.getAdjustmentDate());
        // 参数校验
        assertCreateRequestValid(request);

        // 部门授权校验
        assertDeptAuthorized(tenantId, request.getDeptId());

        // 服务端生成 batchNo（SI + 时间戳），不接受客户端传入
        String batchNo = generateBatchNo();
        if (finStockInitBatchMapper.countByBatchNo(tenantId, batchNo) > 0) {
            // 极小概率冲突，重新生成一次
            batchNo = generateBatchNo();
            if (finStockInitBatchMapper.countByBatchNo(tenantId, batchNo) > 0) {
                throw new ServiceException("期初批次号生成冲突，请重试");
            }
        }

        // 商品归属校验 + 计算金额
        List<FinStockInitItem> items = new ArrayList<>();
        for (StockInitItemInput input : request.getItems()) {
            FinProduct product = finProductMapper.selectFinProductByProductIdAndDeptId(
                    input.getProductId(), request.getDeptId());
            if (product == null) {
                throw new ServiceException("商品 " + input.getProductId()
                        + " 不属于门店 " + request.getDeptId());
            }

            BigDecimal quantity = input.getQuantity();
            BigDecimal unitCost = input.getUnitCost();
            BigDecimal amount = quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);

            FinStockInitItem item = new FinStockInitItem();
            item.setTenantId(tenantId);
            item.setDeptId(request.getDeptId());
            item.setProductId(input.getProductId());
            item.setProductName(product.getProductName());
            item.setQuantity(quantity);
            item.setUnitCost(unitCost);
            item.setAmount(amount);
            item.setVersion(0);
            item.setCreateBy(SecurityUtils.getUsername());
            items.add(item);
        }

        // 插入头表
        FinStockInitBatch header = new FinStockInitBatch();
        header.setTenantId(tenantId);
        header.setBatchNo(batchNo);
        header.setDeptId(request.getDeptId());
        header.setInitDate(request.getInitDate());
        header.setAdjustmentType(request.getAdjustmentType());
        header.setAdjustmentDirection(request.getAdjustmentDirection());
        header.setStatus(STATUS_DRAFT);
        header.setVersion(0);
        header.setRemark(request.getRemark());
        header.setCreateBy(SecurityUtils.getUsername());
        int affected = finStockInitBatchMapper.insertBatch(header);
        if (affected != 1 || header.getBatchId() == null) {
            throw new ServiceException("期初库存批次头表插入失败");
        }

        // 插入行表
        for (FinStockInitItem item : items) {
            item.setBatchId(header.getBatchId());
            int itemAffected = finStockInitBatchMapper.insertBatchItem(item);
            if (itemAffected != 1 || item.getItemId() == null) {
                throw new ServiceException("期初库存批次行表插入失败");
            }
        }

        return header.getBatchId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int validateStockInit(Long batchId, Integer version) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止校验期初库存批次");
        }
        if (version == null) {
            throw new ServiceException("版本号不能为空");
        }

        FinStockInitBatch header = finStockInitBatchMapper.selectBatchForUpdate(tenantId, batchId);
        if (header == null) {
            throw new ServiceException("期初库存批次不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        if (!STATUS_DRAFT.equals(header.getStatus())) {
            throw new ServiceException("仅草稿状态可校验，当前状态: " + header.getStatus());
        }
        if (!version.equals(header.getVersion())) {
            throw new ServiceException("版本号不匹配，请刷新后重试");
        }

        // 校验所有行：quantity > 0 且 unitCost >= 0
        List<FinStockInitItem> items = finStockInitBatchMapper.selectBatchItemsForUpdate(tenantId, batchId);
        if (items == null || items.isEmpty()) {
            throw new ServiceException("期初库存批次无明细行，禁止校验");
        }
        for (FinStockInitItem item : items) {
            if (item.getQuantity() == null
                    || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("存在数量非正的明细行: productId=" + item.getProductId());
            }
            if (item.getUnitCost() == null
                    || item.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("存在单位成本为负的明细行: productId=" + item.getProductId());
            }
        }

        int affected = finStockInitBatchMapper.updateBatchStatus(
                tenantId, batchId, STATUS_DRAFT, STATUS_VALIDATED, version,
                SecurityUtils.getUsername(), null, null, null, null);
        if (affected != 1) {
            throw new ServiceException("校验期初库存批次失败，可能已被其他操作更新");
        }

        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitStockInit(Long batchId, Integer version) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止提交期初库存批次");
        }
        if (version == null) {
            throw new ServiceException("版本号不能为空");
        }

        FinStockInitBatch header = finStockInitBatchMapper.selectBatchForUpdate(tenantId, batchId);
        if (header == null) {
            throw new ServiceException("期初库存批次不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        if (!STATUS_VALIDATED.equals(header.getStatus())) {
            throw new ServiceException("仅已校验状态可提交，当前状态: " + header.getStatus());
        }
        if (!version.equals(header.getVersion())) {
            throw new ServiceException("版本号不匹配，请刷新后重试");
        }

        int affected = finStockInitBatchMapper.updateBatchStatus(
                tenantId, batchId, STATUS_VALIDATED, STATUS_SUBMITTED, version,
                SecurityUtils.getUsername(), SecurityUtils.getUsername(), null, null, null);
        if (affected != 1) {
            throw new ServiceException("提交期初库存批次失败，可能已被其他操作更新");
        }

        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveStockInit(Long batchId, StockInitApproveRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止审批期初库存批次");
        }
        if (request == null || request.getDecision() == null || request.getDecision().isEmpty()) {
            throw new ServiceException("审批决定不能为空");
        }
        if (request.getVersion() == null) {
            throw new ServiceException("版本号不能为空");
        }

        String decision = request.getDecision().toUpperCase();
        if (!"APPROVE".equals(decision) && !"REJECT".equals(decision)) {
            throw new ServiceException("无效的审批决定: " + request.getDecision() + "，仅支持 APPROVE/REJECT");
        }

        FinStockInitBatch header = finStockInitBatchMapper.selectBatchForUpdate(tenantId, batchId);
        if (header == null) {
            throw new ServiceException("期初库存批次不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        if (!STATUS_SUBMITTED.equals(header.getStatus())) {
            throw new ServiceException("仅已提交状态可审批，当前状态: " + header.getStatus());
        }
        if (!request.getVersion().equals(header.getVersion())) {
            throw new ServiceException("版本号不匹配，请刷新后重试");
        }

        // 审批人不能是创建人（admin 例外，便于运维兜底）
        String currentUser = SecurityUtils.getUsername();
        if (!SecurityUtils.isAdmin() && currentUser != null
                && currentUser.equals(header.getCreateBy())) {
            throw new ServiceException("审批人不能是创建人");
        }

        String fromStatus = header.getStatus();

        if ("REJECT".equals(decision)) {
            // 驳回：回到 DRAFT 重新编辑
            int affected = finStockInitBatchMapper.updateBatchStatus(
                    tenantId, batchId, fromStatus, STATUS_DRAFT, request.getVersion(),
                    SecurityUtils.getUsername(), null, SecurityUtils.getUsername(), null, null);
            if (affected != 1) {
                throw new ServiceException("驳回期初库存批次失败，可能已被其他操作更新");
            }
            return affected;
        }

        // APPROVE：流转至 APPROVED
        int affected = finStockInitBatchMapper.updateBatchStatus(
                tenantId, batchId, fromStatus, STATUS_APPROVED, request.getVersion(),
                SecurityUtils.getUsername(), null, SecurityUtils.getUsername(), null, null);
        if (affected != 1) {
            throw new ServiceException("审批期初库存批次失败，可能已被其他操作更新");
        }

        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int postStockInit(Long batchId, StockInitPostRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止过账期初库存批次");
        }
        if (request == null) {
            throw new ServiceException("过账请求不能为空");
        }
        if (request.getPostIdempotencyKey() == null || request.getPostIdempotencyKey().isEmpty()) {
            throw new ServiceException("过账幂等键不能为空");
        }
        if (request.getVersion() == null) {
            throw new ServiceException("版本号不能为空");
        }

        // 幂等键预检查：若已存在
        if (finStockInitBatchMapper.countByPostIdempotencyKey(tenantId, request.getPostIdempotencyKey()) > 0) {
            FinStockInitBatch existing = finStockInitBatchMapper.selectBatchByPostIdempotencyKey(
                    tenantId, request.getPostIdempotencyKey());
            if (existing != null && existing.getBatchId().equals(batchId)) {
                // 同一批次的重复过账请求：幂等成功
                return 1;
            }
            // 不同批次但同幂等键：拒绝
            throw new ServiceException("过账幂等键已被其他批次使用，请更换后重试");
        }

        FinStockInitBatch header = finStockInitBatchMapper.selectBatchForUpdate(tenantId, batchId);
        if (header == null) {
            throw new ServiceException("期初库存批次不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        if (!STATUS_APPROVED.equals(header.getStatus())) {
            throw new ServiceException("仅已审批状态可过账，当前状态: " + header.getStatus());
        }
        if (!request.getVersion().equals(header.getVersion())) {
            throw new ServiceException("版本号不匹配，请刷新后重试");
        }

        // 会计期间必须为 ACTIVE（持锁验证，防止与结转并发）
        com.junsong.finance.domain.FinAccountingPeriod period =
                accountingPeriodMapper.selectCurrentPeriodByDeptIdForUpdate(tenantId, header.getDeptId());
        if (period == null) {
            throw new ServiceException("门店 " + header.getDeptId() + " 无 ACTIVE 会计期间，禁止过账");
        }
        if (!PERIOD_ACTIVE.equals(period.getStatus())) {
            throw new ServiceException("门店 " + header.getDeptId() + " 会计期间已结转，禁止过账");
        }

        // 幂等校验：batchNo 已有库存流水则拒绝重复过账
        int existing = finStockLedgerMapper.countByReferenceNo(tenantId, header.getBatchNo());
        if (existing > 0) {
            throw new ServiceException("期初库存批次已过账，禁止重复过账: " + header.getBatchNo());
        }

        // 锁定行表
        List<FinStockInitItem> items = finStockInitBatchMapper.selectBatchItemsForUpdate(tenantId, batchId);
        if (items == null || items.isEmpty()) {
            throw new ServiceException("期初库存批次无明细行，禁止过账");
        }

        // 按 (deptId, productId) 升序排序（避免死锁）
        List<FinStockInitItem> sortedItems = new ArrayList<>(items);
        sortedItems.sort((a, b) -> {
            int byDept = a.getDeptId().compareTo(b.getDeptId());
            if (byDept != 0) return byDept;
            return a.getProductId().compareTo(b.getProductId());
        });

        String operator = SecurityUtils.getUsername();

        // 第一步：按固定顺序锁定全部库存结存行
        for (FinStockInitItem item : sortedItems) {
            finStockLedgerMapper.insertPositionIfAbsent(tenantId, item.getDeptId(), item.getProductId());
            finStockLedgerMapper.selectPositionQuantityForUpdate(
                    tenantId, item.getDeptId(), item.getProductId());
        }

        // 第二步：逐行写库存流水 + 更新结存 + 成本入账
        for (FinStockInitItem item : sortedItems) {
            BigDecimal qty = item.getQuantity();
            boolean increase = isIncrease(header.getAdjustmentType(), header.getAdjustmentDirection());
            BigDecimal ledgerQty = increase ? qty : qty.negate();

            BigDecimal currentQty = nzDec(finStockLedgerMapper.selectPositionQuantityForUpdate(
                    tenantId, item.getDeptId(), item.getProductId()));
            BigDecimal afterQty = currentQty.add(ledgerQty);

            // 写库存流水
            FinStockLedger ledger = new FinStockLedger();
            ledger.setTenantId(tenantId);
            ledger.setDeptId(item.getDeptId());
            ledger.setProductId(item.getProductId());
            ledger.setProductName(item.getProductName());
            ledger.setChangeType(header.getAdjustmentType());
            ledger.setChangeQuantity(ledgerQty);
            ledger.setBeforeQuantity(currentQty);
            ledger.setAfterQuantity(afterQty);
            ledger.setReferenceType(REF_STOCK_INIT);
            ledger.setReferenceId(header.getBatchId());
            ledger.setReferenceNo(header.getBatchNo());
            // DB 唯一键兜底：同一初始化批次同一商品只能生成一条流水
            ledger.setIdempotencyKey(REF_STOCK_INIT + ":" + header.getBatchId() + ":" + item.getProductId());
            ledger.setDelFlag("0");
            ledger.setCreateBy(operator);
            ledger.setRemark("库存调整：" + header.getAdjustmentType());
            finStockLedgerMapper.insertFinStockLedger(ledger);
            Long stockLedgerId = ledger.getLedgerId();
            if (stockLedgerId == null) {
                throw new ServiceException("库存流水ID未生成，过账失败");
            }

            // 更新结存
            int positionAffected = finStockLedgerMapper.updatePositionQuantity(
                    tenantId, item.getDeptId(), item.getProductId(), afterQty);
            if (positionAffected != 1) {
                throw new ServiceException("库存结存更新影响行数异常（" + positionAffected + "），事务回滚");
            }

            // 成本入账：复用 applyStocktakeGain（期初入账视同盘盈入库）
            // amount = quantity * unitCost（已 scale 2 HALF_UP）
            BigDecimal amount = item.getAmount();
            Long costLedgerId = increase
                    ? stockCostService.applyStocktakeGain(
                        tenantId, item.getDeptId(), item.getProductId(),
                        qty, amount, stockLedgerId, operator)
                    : stockCostService.applyStocktakeLoss(
                        tenantId, item.getDeptId(), item.getProductId(),
                        qty, stockLedgerId, operator);
            if (costLedgerId == null) {
                throw new ServiceException("成本流水ID未生成，过账失败");
            }

            // 更新行表过账引用
            int refAffected = finStockInitBatchMapper.updateBatchItemPostingRefs(
                    tenantId, item.getItemId(),
                    stockLedgerId, costLedgerId,
                    item.getVersion());
            if (refAffected != 1) {
                throw new ServiceException("更新期初行过账引用失败（行版本冲突）");
            }
        }

        // 状态流转 APPROVED → POSTED，写入幂等键
        int affected = finStockInitBatchMapper.updateBatchStatus(
                tenantId, batchId, STATUS_APPROVED, STATUS_POSTED, request.getVersion(),
                operator, null, null, operator, request.getPostIdempotencyKey());
        if (affected != 1) {
            throw new ServiceException("过账期初库存批次失败，可能已被其他操作更新");
        }

        return affected;
    }

    @Override
    public StockInitDetailVO getStockInitDetail(Long batchId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止查询期初库存详情");
        }

        FinStockInitBatch header = finStockInitBatchMapper.selectBatchById(tenantId, batchId);
        if (header == null) {
            throw new ServiceException("期初库存批次不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        List<FinStockInitItem> items = finStockInitBatchMapper.listBatchItems(tenantId, batchId);

        StockInitDetailVO vo = new StockInitDetailVO();
        vo.setBatch(header);
        vo.setItems(items);
        return vo;
    }

    @Override
    public List<FinStockInitBatch> listStockInit(StockInitQuery query) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止查询期初库存列表");
        }

        List<Long> deptIds = resolveAuthorizedDeptIds();
        if (query != null && query.getDeptId() != null) {
            // 若指定了 deptId，与授权集合求交集
            if (deptIds != null && !deptIds.contains(query.getDeptId())) {
                return new ArrayList<>();
            }
        }
        String status = query == null ? null : query.getStatus();
        String batchNo = query == null ? null : query.getBatchNo();
        return finStockInitBatchMapper.listBatches(tenantId, deptIds, status, batchNo);
    }

    // ===== 私有辅助方法 =====

    private void assertCreateRequestValid(StockInitCreateRequest request) {
        if (request.getDeptId() == null) {
            throw new ServiceException("门店ID不能为空");
        }
        if (request.getInitDate() == null) {
            throw new ServiceException("期初日期不能为空");
        }
        if (!List.of("OPENING_STOCK", "HISTORY_REPLENISH", "TRIAL_CONSUMPTION", "STORE_USE", "DAMAGE_LOSS", "OTHER")
                .contains(request.getAdjustmentType())) {
            throw new ServiceException("调整类型无效");
        }
        if ("OTHER".equals(request.getAdjustmentType())
                && !List.of("INCREASE", "DECREASE").contains(request.getAdjustmentDirection())) {
            throw new ServiceException("其他类型必须选择库存方向");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ServiceException("期初明细行不能为空");
        }
        for (StockInitItemInput input : request.getItems()) {
            if (input.getProductId() == null) {
                throw new ServiceException("商品ID不能为空");
            }
            if (input.getQuantity() == null
                    || input.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("期初数量必须为正数: productId=" + input.getProductId());
            }
            if (input.getQuantity().scale() > 3) throw new ServiceException("数量最多保留3位小数");
            if (input.getUnitCost() == null
                    || input.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("单位成本不能为负数: productId=" + input.getProductId());
            }
            if (input.getUnitCost().scale() > 2) throw new ServiceException("单位成本最多保留2位小数");
        }
    }

    private boolean isIncrease(String type, String direction) {
        if ("OTHER".equals(type)) return "INCREASE".equals(direction);
        return "OPENING_STOCK".equals(type) || "HISTORY_REPLENISH".equals(type);
    }

    private void assertDeptAuthorized(Long tenantId, Long deptId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            throw new ServiceException("无法获取授权门店列表，禁止操作");
        }
        if (!allowed.contains(deptId)) {
            throw new ServiceException("无权操作门店 " + deptId + " 的期初库存批次");
        }
    }

    private List<Long> resolveAuthorizedDeptIds() {
        if (SecurityUtils.isAdmin()) {
            return null; // null 表示不过滤
        }
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            return SENTINEL_DEPT_IDS; // 返回哨兵值确保查询返回空
        }
        return allowed;
    }

    private List<Long> loadAllowedDeptIds() {
        String username = SecurityUtils.getUsername();
        if (username == null || username.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            R<List<SysDept>> response = remoteUserService.getUserDeptList(username, SecurityConstants.INNER);
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData().stream()
                    .map(SysDept::getDeptId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 服务端生成期初批次号：SI + yyyyMMddHHmmssSSS。
     * 租户内唯一，由 countByBatchNo 校验冲突。
     */
    private String generateBatchNo() {
        return "SI" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
    }
}
