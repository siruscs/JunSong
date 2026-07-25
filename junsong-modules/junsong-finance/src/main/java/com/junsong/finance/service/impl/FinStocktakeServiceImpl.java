package com.junsong.finance.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinProduct;
import com.junsong.finance.domain.FinStocktake;
import com.junsong.finance.domain.FinStocktakeHistory;
import com.junsong.finance.domain.FinStocktakeItem;
import com.junsong.finance.domain.vo.StocktakeApprovalRequest;
import com.junsong.finance.domain.vo.StocktakeAssignRequest;
import com.junsong.finance.domain.vo.StocktakeCountRequest;
import com.junsong.finance.domain.vo.StocktakeCreateRequest;
import com.junsong.finance.domain.vo.StocktakeDetailVO;
import com.junsong.finance.domain.vo.StocktakeQuery;
import com.junsong.finance.domain.vo.StocktakeRecountRequest;
import com.junsong.finance.mapper.FinProductMapper;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.mapper.FinStocktakeMapper;
import com.junsong.finance.service.IFinStocktakeService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.common.core.domain.R;

/**
 * 库存盘点 Service 实现（Task 3-5：创建、分配、列表、详情、启动、行录入、提交、复盘、审批）。
 *
 * 安全契约：
 * 1. 所有方法从 TenantContext 获取租户ID，缺失即拒绝
 * 2. 所有读写按授权部门集合过滤（admin 跳过；非 admin 与 RemoteUserService.getUserDeptList 求交集）
 * 3. 状态流转使用乐观锁 version 谓词
 * 4. 创建时冻结期望数量（从 fin_stock_position 无锁读取）
 * 5. counter 视角隐藏期望值（盲盘保护）
 * 6. 复盘人必须与盘点人不同
 * 7. 审批人不能是 counter/recountUser
 *
 * @author junsong
 */
@Service
public class FinStocktakeServiceImpl implements IFinStocktakeService {

    private static final List<Long> SENTINEL_DEPT_IDS = Collections.singletonList(-1L);
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_COUNTING = "COUNTING";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_RECOUNTING = "RECOUNTING";
    private static final String STATUS_APPROVED = "APPROVED";

    /** 复盘阈值：当任一行 |variance| 超过此值且已分配复盘人时，流转至 RECOUNTING */
    @Value("${finance.stocktake.recount.quantityThreshold:5}")
    private int recountQuantityThreshold;

    @Autowired
    private FinStocktakeMapper finStocktakeMapper;

    @Autowired
    private FinStockLedgerMapper finStockLedgerMapper;

    @Autowired
    private FinProductMapper finProductMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStocktake(StocktakeCreateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止创建盘点任务");
        }

        // 参数校验
        assertCreateRequestValid(request);

        // 部门授权校验
        assertDeptAuthorized(tenantId, request.getDeptId());

        // takeNo 唯一性校验
        if (finStocktakeMapper.countByTakeNo(tenantId, request.getTakeNo()) > 0) {
            throw new ServiceException("盘点单号已存在: " + request.getTakeNo());
        }

        // 商品归属校验 + 冻结期望数量
        List<FinStocktakeItem> items = new ArrayList<>();
        Date freezeTime = new Date();
        for (Long productId : request.getProductIds()) {
            FinProduct product = finProductMapper.selectFinProductByProductIdAndDeptId(productId, request.getDeptId());
            if (product == null) {
                throw new ServiceException("商品 " + productId + " 不属于门店 " + request.getDeptId());
            }

            Integer position = finStockLedgerMapper.selectPositionQuantity(tenantId, request.getDeptId(), productId);
            int expectedQty = position == null ? 0 : position;

            FinStocktakeItem item = new FinStocktakeItem();
            item.setTenantId(tenantId);
            item.setDeptId(request.getDeptId());
            item.setProductId(productId);
            item.setProductName(product.getProductName());
            item.setExpectedQuantity(expectedQty);
            item.setMovementQuantityAfterFreeze(0);
            item.setVersion(0);
            item.setCreateBy(SecurityUtils.getUsername());
            items.add(item);
        }

        // 插入头表
        FinStocktake header = new FinStocktake();
        header.setTenantId(tenantId);
        header.setTakeNo(request.getTakeNo());
        header.setDeptId(request.getDeptId());
        header.setScopeType(request.getScopeType());
        header.setStatus(STATUS_DRAFT);
        header.setFreezeTime(freezeTime);
        header.setCounterUserId(request.getCounterUserId());
        header.setRecountUserId(request.getRecountUserId());
        header.setVersion(0);
        header.setRemark(request.getRemark());
        header.setCreateBy(SecurityUtils.getUsername());
        int affected = finStocktakeMapper.insertStocktake(header);
        if (affected != 1 || header.getStocktakeId() == null) {
            throw new ServiceException("盘点任务头表插入失败");
        }

        // 插入行表
        for (FinStocktakeItem item : items) {
            item.setStocktakeId(header.getStocktakeId());
            int itemAffected = finStocktakeMapper.insertStocktakeItem(item);
            if (itemAffected != 1 || item.getItemId() == null) {
                throw new ServiceException("盘点任务行表插入失败");
            }
        }

        // 插入历史
        FinStocktakeHistory history = new FinStocktakeHistory();
        history.setTenantId(tenantId);
        history.setStocktakeId(header.getStocktakeId());
        history.setAction("CREATE");
        history.setFromStatus(null);
        history.setToStatus(STATUS_DRAFT);
        history.setOperator(SecurityUtils.getUsername());
        history.setComment("创建盘点任务");
        finStocktakeMapper.insertStocktakeHistory(history);

        return header.getStocktakeId();
    }

    @Override
    public List<FinStocktake> listStocktakes(StocktakeQuery query) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止查询盘点任务");
        }

        List<Long> deptIds = resolveAuthorizedDeptIds();
        return finStocktakeMapper.listStocktakes(tenantId, deptIds, query.getStatus(), query.getCounterUserId());
    }

    @Override
    public StocktakeDetailVO getStocktakeDetail(Long stocktakeId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止查询盘点详情");
        }

        FinStocktake header = finStocktakeMapper.selectStocktakeById(tenantId, stocktakeId);
        if (header == null) {
            throw new ServiceException("盘点任务不存在或无权访问");
        }

        // 部门授权校验
        assertDeptAuthorized(tenantId, header.getDeptId());

        List<FinStocktakeItem> items = finStocktakeMapper.listStocktakeItems(tenantId, stocktakeId);
        List<FinStocktakeHistory> history = finStocktakeMapper.listStocktakeHistory(tenantId, stocktakeId);

        // 盲盘保护：counter 视角且任务未提交时，隐藏期望值
        boolean hideExpected = shouldHideExpected(header);

        StocktakeDetailVO vo = new StocktakeDetailVO();
        vo.setStocktake(header);
        vo.setItems(items);
        vo.setHistory(history);
        vo.setHideExpected(hideExpected);

        if (hideExpected) {
            // 复制一份并置空敏感字段，避免污染原对象
            List<FinStocktakeItem> maskedItems = items.stream()
                    .map(this::maskItemForBlindCount)
                    .collect(Collectors.toList());
            vo.setItems(maskedItems);
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int assignCounter(Long stocktakeId, StocktakeAssignRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止分配盘点人");
        }

        FinStocktake header = finStocktakeMapper.selectStocktakeById(tenantId, stocktakeId);
        if (header == null) {
            throw new ServiceException("盘点任务不存在或无权访问");
        }

        // 部门授权校验
        assertDeptAuthorized(tenantId, header.getDeptId());

        // 状态校验
        if (!STATUS_DRAFT.equals(header.getStatus())) {
            throw new ServiceException("仅草稿状态可分配盘点人，当前状态: " + header.getStatus());
        }

        // 乐观锁校验
        if (request.getVersion() == null || !request.getVersion().equals(header.getVersion())) {
            throw new ServiceException("版本号不匹配，请刷新后重试");
        }

        int affected = finStocktakeMapper.assignCounter(
                tenantId, stocktakeId, request.getCounterUserId(),
                request.getRecountUserId(), request.getVersion(), SecurityUtils.getUsername());

        if (affected != 1) {
            throw new ServiceException("分配盘点人失败，可能已被其他操作更新");
        }

        // 历史
        FinStocktakeHistory history = new FinStocktakeHistory();
        history.setTenantId(tenantId);
        history.setStocktakeId(stocktakeId);
        history.setAction("ASSIGN");
        history.setFromStatus(STATUS_DRAFT);
        history.setToStatus(STATUS_DRAFT);
        history.setOperator(SecurityUtils.getUsername());
        history.setComment("分配盘点人: " + request.getCounterUserId());
        finStocktakeMapper.insertStocktakeHistory(history);

        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int startStocktake(Long stocktakeId, Integer version) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止启动盘点任务");
        }
        if (version == null) {
            throw new ServiceException("版本号不能为空");
        }

        FinStocktake header = finStocktakeMapper.selectStocktakeById(tenantId, stocktakeId);
        if (header == null) {
            throw new ServiceException("盘点任务不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        if (!STATUS_DRAFT.equals(header.getStatus())) {
            throw new ServiceException("仅草稿状态可启动盘点，当前状态: " + header.getStatus());
        }
        if (header.getCounterUserId() == null) {
            throw new ServiceException("盘点任务尚未分配盘点人，无法启动");
        }
        if (!version.equals(header.getVersion())) {
            throw new ServiceException("版本号不匹配，请刷新后重试");
        }

        int affected = finStocktakeMapper.updateStocktakeStatus(
                tenantId, stocktakeId, STATUS_DRAFT, STATUS_COUNTING, version,
                SecurityUtils.getUsername(), null, null, null, null, null);
        if (affected != 1) {
            throw new ServiceException("启动盘点失败，可能已被其他操作更新");
        }

        FinStocktakeHistory history = new FinStocktakeHistory();
        history.setTenantId(tenantId);
        history.setStocktakeId(stocktakeId);
        history.setAction("START");
        history.setFromStatus(STATUS_DRAFT);
        history.setToStatus(STATUS_COUNTING);
        history.setOperator(SecurityUtils.getUsername());
        history.setComment("启动盘点任务");
        finStocktakeMapper.insertStocktakeHistory(history);

        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int countItem(Long stocktakeId, Long itemId, StocktakeCountRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止录入盘点数据");
        }

        assertCountRequestValid(request);

        FinStocktake header = finStocktakeMapper.selectStocktakeById(tenantId, stocktakeId);
        if (header == null) {
            throw new ServiceException("盘点任务不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        if (!STATUS_COUNTING.equals(header.getStatus())) {
            throw new ServiceException("仅盘点中状态可录入数据，当前状态: " + header.getStatus());
        }

        // 非 admin 时仅分配的 counter 可录入
        if (!SecurityUtils.isAdmin()) {
            Long currentUserId = SecurityUtils.getUserId();
            if (currentUserId == null || !currentUserId.equals(header.getCounterUserId())) {
                throw new ServiceException("仅分配的盘点人可录入盘点数据");
            }
        }

        FinStocktakeItem item = finStocktakeMapper.selectStocktakeItemById(tenantId, itemId);
        if (item == null || !stocktakeId.equals(item.getStocktakeId())) {
            throw new ServiceException("盘点行不存在或不属于该盘点任务");
        }

        // 幂等键校验：相同键相同负载返回原结果；相同键不同负载/被其他行占用拒绝
        int existing = finStocktakeMapper.countByCountIdempotencyKey(tenantId, request.getIdempotencyKey());
        if (existing > 0) {
            if (request.getIdempotencyKey().equals(item.getCountIdempotencyKey())) {
                // 当前行已有此幂等键：相同负载幂等重放，不同负载拒绝
                if (isSameCountPayload(item, request)) {
                    return 1;
                }
                throw new ServiceException("幂等键已存在但负载不同，拒绝重复录入: " + request.getIdempotencyKey());
            }
            throw new ServiceException("幂等键已被其他盘点行占用: " + request.getIdempotencyKey());
        }

        // 版本校验
        if (request.getVersion() == null || !request.getVersion().equals(item.getVersion())) {
            throw new ServiceException("行版本号不匹配，请刷新后重试");
        }

        // 方差校验：非零方差需要 reasonCode 和 reason
        int expected = item.getExpectedQuantity() == null ? 0 : item.getExpectedQuantity();
        int variance = request.getActualQuantity() - expected;
        if (variance != 0) {
            if (request.getReasonCode() == null || request.getReasonCode().isEmpty()) {
                throw new ServiceException("盘亏或盘盈时原因代码必填");
            }
            if (request.getReason() == null || request.getReason().isEmpty()) {
                throw new ServiceException("盘亏或盘盈时原因说明必填");
            }
        }

        int affected = finStocktakeMapper.updateStocktakeItemCount(
                tenantId, itemId, request.getActualQuantity(),
                request.getReasonCode(), request.getReason(), request.getAttachments(),
                request.getIdempotencyKey(), SecurityUtils.getUsername(), request.getVersion());
        if (affected != 1) {
            throw new ServiceException("录入盘点数据失败，可能已被其他操作更新");
        }

        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitStocktake(Long stocktakeId, Integer version) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止提交盘点任务");
        }
        if (version == null) {
            throw new ServiceException("版本号不能为空");
        }

        FinStocktake header = finStocktakeMapper.selectStocktakeForUpdate(tenantId, stocktakeId);
        if (header == null) {
            throw new ServiceException("盘点任务不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        if (!STATUS_COUNTING.equals(header.getStatus())) {
            throw new ServiceException("仅盘点中状态可提交，当前状态: " + header.getStatus());
        }
        if (!version.equals(header.getVersion())) {
            throw new ServiceException("版本号不匹配，请刷新后重试");
        }

        List<FinStocktakeItem> items = finStocktakeMapper.selectStocktakeItemsForUpdate(tenantId, stocktakeId);
        if (items == null || items.isEmpty()) {
            throw new ServiceException("盘点任务无明细行，禁止提交");
        }

        // 校验所有行已录入 actualQuantity
        for (FinStocktakeItem item : items) {
            if (item.getActualQuantity() == null) {
                throw new ServiceException("存在未录入实际数量的盘点行: productId=" + item.getProductId());
            }
        }

        // 计算临时方差并决定流转目标状态
        boolean needRecount = false;
        for (FinStocktakeItem item : items) {
            int expected = item.getExpectedQuantity() == null ? 0 : item.getExpectedQuantity();
            // Task 5: movementAfterFreeze 暂通过 mapper 查询（Task 6 会真正使用）
            Integer movement = finStocktakeMapper.sumMovementAfterFreeze(
                    tenantId, header.getDeptId(), item.getProductId(), header.getFreezeTime());
            int movementAfterFreeze = movement == null ? 0 : movement;
            int adjustedExpected = expected + movementAfterFreeze;
            int variance = item.getActualQuantity() - adjustedExpected;

            int itemAffected = finStocktakeMapper.updateStocktakeItemFinal(
                    tenantId, item.getItemId(),
                    null, // finalQuantity 留待审批确定
                    variance,
                    null, // unitCost 留待 Task 6 过账时锁定
                    null, // varianceAmount 留待 Task 6
                    item.getReasonCode(),
                    item.getReason(),
                    movementAfterFreeze,
                    adjustedExpected,
                    item.getVersion());
            if (itemAffected != 1) {
                throw new ServiceException("更新盘点行临时方差失败，可能已被其他操作更新");
            }

            // 阈值检查：|variance| > 阈值 且已分配复盘人 → 触发复盘
            if (Math.abs(variance) > recountQuantityThreshold && header.getRecountUserId() != null) {
                needRecount = true;
            }
        }

        String toStatus = needRecount ? STATUS_RECOUNTING : STATUS_SUBMITTED;
        int affected = finStocktakeMapper.updateStocktakeStatus(
                tenantId, stocktakeId, STATUS_COUNTING, toStatus, version,
                SecurityUtils.getUsername(), SecurityUtils.getUsername(), null, null, null, null);
        if (affected != 1) {
            throw new ServiceException("提交盘点任务失败，可能已被其他操作更新");
        }

        FinStocktakeHistory history = new FinStocktakeHistory();
        history.setTenantId(tenantId);
        history.setStocktakeId(stocktakeId);
        history.setAction("SUBMIT");
        history.setFromStatus(STATUS_COUNTING);
        history.setToStatus(toStatus);
        history.setOperator(SecurityUtils.getUsername());
        history.setComment(needRecount ? "提交并触发阈值复盘" : "提交盘点任务");
        finStocktakeMapper.insertStocktakeHistory(history);

        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recountItem(Long stocktakeId, Long itemId, StocktakeRecountRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止录入复盘数据");
        }

        assertRecountRequestValid(request);

        FinStocktake header = finStocktakeMapper.selectStocktakeById(tenantId, stocktakeId);
        if (header == null) {
            throw new ServiceException("盘点任务不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        if (!STATUS_RECOUNTING.equals(header.getStatus())) {
            throw new ServiceException("仅复盘中状态可录入复盘数据，当前状态: " + header.getStatus());
        }

        // 复盘人必须与盘点人不同（防御性校验）
        if (header.getCounterUserId() != null && header.getRecountUserId() != null
                && header.getCounterUserId().equals(header.getRecountUserId())) {
            throw new ServiceException("复盘人与盘点人不能为同一人");
        }

        // 非 admin 时仅分配的 recountUserId 可录入
        if (!SecurityUtils.isAdmin()) {
            Long currentUserId = SecurityUtils.getUserId();
            if (currentUserId == null || !currentUserId.equals(header.getRecountUserId())) {
                throw new ServiceException("仅分配的复盘人可录入复盘数据");
            }
        }

        FinStocktakeItem item = finStocktakeMapper.selectStocktakeItemById(tenantId, itemId);
        if (item == null || !stocktakeId.equals(item.getStocktakeId())) {
            throw new ServiceException("盘点行不存在或不属于该盘点任务");
        }

        // 幂等键校验：相同键相同负载返回原结果；不同负载/被其他行占用拒绝
        int existing = finStocktakeMapper.countByCountIdempotencyKey(tenantId, request.getIdempotencyKey());
        if (existing > 0) {
            if (request.getIdempotencyKey().equals(item.getCountIdempotencyKey())) {
                if (isSameRecountPayload(item, request)) {
                    return 1;
                }
                throw new ServiceException("复盘幂等键已存在但负载不同，拒绝重复录入: " + request.getIdempotencyKey());
            }
            throw new ServiceException("复盘幂等键已被其他盘点行占用: " + request.getIdempotencyKey());
        }

        // 版本校验
        if (request.getVersion() == null || !request.getVersion().equals(item.getVersion())) {
            throw new ServiceException("行版本号不匹配，请刷新后重试");
        }

        // 复盘方差校验：与 adjustedExpected 不等时需 reasonCode/reason
        int adjustedExpected = item.getAdjustedExpectedQuantity() == null
                ? (item.getExpectedQuantity() == null ? 0 : item.getExpectedQuantity())
                : item.getAdjustedExpectedQuantity();
        int recountVariance = request.getRecountQuantity() - adjustedExpected;
        if (recountVariance != 0) {
            if (request.getReasonCode() == null || request.getReasonCode().isEmpty()) {
                throw new ServiceException("复盘方差非零时原因代码必填");
            }
            if (request.getReason() == null || request.getReason().isEmpty()) {
                throw new ServiceException("复盘方差非零时原因说明必填");
            }
        }

        int affected = finStocktakeMapper.updateStocktakeItemRecount(
                tenantId, itemId, request.getRecountQuantity(),
                request.getReasonCode(), request.getReason(), request.getIdempotencyKey(),
                SecurityUtils.getUsername(), request.getVersion());
        if (affected != 1) {
            throw new ServiceException("录入复盘数据失败，可能已被其他操作更新");
        }

        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveStocktake(Long stocktakeId, StocktakeApprovalRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止审批盘点任务");
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

        FinStocktake header = finStocktakeMapper.selectStocktakeForUpdate(tenantId, stocktakeId);
        if (header == null) {
            throw new ServiceException("盘点任务不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        if (!STATUS_SUBMITTED.equals(header.getStatus()) && !STATUS_RECOUNTING.equals(header.getStatus())) {
            throw new ServiceException("仅已提交或复盘中状态可审批，当前状态: " + header.getStatus());
        }
        if (!request.getVersion().equals(header.getVersion())) {
            throw new ServiceException("版本号不匹配，请刷新后重试");
        }

        // 审批人不能是 counter 或 recountUser
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null && !SecurityUtils.isAdmin()) {
            if (currentUserId.equals(header.getCounterUserId())) {
                throw new ServiceException("盘点人不能审批自己提交的盘点任务");
            }
            if (currentUserId.equals(header.getRecountUserId())) {
                throw new ServiceException("复盘人不能审批自己复盘的盘点任务");
            }
        }

        String fromStatus = header.getStatus();

        if ("REJECT".equals(decision)) {
            // 驳回：回到 COUNTING 重新盘点
            int affected = finStocktakeMapper.updateStocktakeStatus(
                    tenantId, stocktakeId, fromStatus, STATUS_COUNTING, request.getVersion(),
                    SecurityUtils.getUsername(), null, SecurityUtils.getUsername(), null, null, null);
            if (affected != 1) {
                throw new ServiceException("驳回盘点任务失败，可能已被其他操作更新");
            }

            FinStocktakeHistory history = new FinStocktakeHistory();
            history.setTenantId(tenantId);
            history.setStocktakeId(stocktakeId);
            history.setAction("REJECT");
            history.setFromStatus(fromStatus);
            history.setToStatus(STATUS_COUNTING);
            history.setOperator(SecurityUtils.getUsername());
            history.setComment(request.getComment() == null ? "审批驳回，回到盘点中" : request.getComment());
            finStocktakeMapper.insertStocktakeHistory(history);

            return affected;
        }

        // APPROVE：确定 finalQuantity 并流转至 APPROVED
        List<FinStocktakeItem> items = finStocktakeMapper.selectStocktakeItemsForUpdate(tenantId, stocktakeId);
        if (items == null || items.isEmpty()) {
            throw new ServiceException("盘点任务无明细行，禁止审批");
        }

        for (FinStocktakeItem item : items) {
            // finalQuantity 优先使用 recountQuantity（如有），否则 actualQuantity
            Integer finalQty = item.getRecountQuantity() != null
                    ? item.getRecountQuantity()
                    : item.getActualQuantity();
            if (finalQty == null) {
                throw new ServiceException("盘点行缺少实际或复盘数量: productId=" + item.getProductId());
            }

            int adjustedExpected = item.getAdjustedExpectedQuantity() == null
                    ? (item.getExpectedQuantity() == null ? 0 : item.getExpectedQuantity())
                    : item.getAdjustedExpectedQuantity();
            int finalVariance = finalQty - adjustedExpected;

            int itemAffected = finStocktakeMapper.updateStocktakeItemFinal(
                    tenantId, item.getItemId(),
                    finalQty,
                    finalVariance,
                    item.getUnitCost(), // 保留原值（Task 6 过账时锁定）
                    item.getVarianceAmount(), // 保留原值（Task 6 过账时计算）
                    item.getReasonCode(),
                    item.getReason(),
                    item.getMovementQuantityAfterFreeze(),
                    item.getAdjustedExpectedQuantity(),
                    item.getVersion());
            if (itemAffected != 1) {
                throw new ServiceException("更新盘点行最终数量失败，可能已被其他操作更新");
            }
        }

        int affected = finStocktakeMapper.updateStocktakeStatus(
                tenantId, stocktakeId, fromStatus, STATUS_APPROVED, request.getVersion(),
                SecurityUtils.getUsername(), null, SecurityUtils.getUsername(), null, null, null);
        if (affected != 1) {
            throw new ServiceException("审批盘点任务失败，可能已被其他操作更新");
        }

        FinStocktakeHistory history = new FinStocktakeHistory();
        history.setTenantId(tenantId);
        history.setStocktakeId(stocktakeId);
        history.setAction("APPROVE");
        history.setFromStatus(fromStatus);
        history.setToStatus(STATUS_APPROVED);
        history.setOperator(SecurityUtils.getUsername());
        history.setComment(request.getComment() == null ? "审批通过" : request.getComment());
        finStocktakeMapper.insertStocktakeHistory(history);

        return affected;
    }

    // ===== 私有辅助方法 =====

    private void assertCreateRequestValid(StocktakeCreateRequest request) {
        if (request.getTakeNo() == null || request.getTakeNo().isEmpty()) {
            throw new ServiceException("盘点单号不能为空");
        }
        if (request.getDeptId() == null) {
            throw new ServiceException("门店ID不能为空");
        }
        if (request.getScopeType() == null || request.getScopeType().isEmpty()) {
            throw new ServiceException("盘点范围类型不能为空");
        }
        if ("SELECTED_PRODUCTS".equals(request.getScopeType())) {
            if (request.getProductIds() == null || request.getProductIds().isEmpty()) {
                throw new ServiceException("按商品盘点时商品列表不能为空");
            }
        }
        if (request.getCounterUserId() == null) {
            throw new ServiceException("盘点人不能为空");
        }
    }

    private void assertDeptAuthorized(Long tenantId, Long deptId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            // fail-closed：无授权部门信息时，只允许当前用户部门
            throw new ServiceException("无法获取授权门店列表，禁止操作");
        }
        if (!allowed.contains(deptId)) {
            throw new ServiceException("无权操作门店 " + deptId + " 的盘点任务");
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
            // fail-closed
            return Collections.emptyList();
        }
    }

    private boolean shouldHideExpected(FinStocktake header) {
        if (SecurityUtils.isAdmin()) {
            return false;
        }
        // counter 视角且任务未提交时隐藏期望值
        boolean isCounter = header.getCounterUserId() != null
                && header.getCounterUserId().equals(SecurityUtils.getUserId());
        boolean isPreSubmit = STATUS_DRAFT.equals(header.getStatus())
                || STATUS_COUNTING.equals(header.getStatus());
        return isCounter && isPreSubmit;
    }

    private FinStocktakeItem maskItemForBlindCount(FinStocktakeItem original) {
        FinStocktakeItem masked = new FinStocktakeItem();
        masked.setItemId(original.getItemId());
        masked.setStocktakeId(original.getStocktakeId());
        masked.setTenantId(original.getTenantId());
        masked.setDeptId(original.getDeptId());
        masked.setProductId(original.getProductId());
        masked.setProductName(original.getProductName());
        // 期望值/方差/成本 置 null（盲盘保护）
        masked.setExpectedQuantity(null);
        masked.setAdjustedExpectedQuantity(null);
        masked.setVarianceQuantity(null);
        masked.setVarianceAmount(null);
        masked.setUnitCost(null);
        // 实际录入保留
        masked.setActualQuantity(original.getActualQuantity());
        masked.setRecountQuantity(original.getRecountQuantity());
        masked.setReasonCode(original.getReasonCode());
        masked.setReason(original.getReason());
        masked.setAttachments(original.getAttachments());
        masked.setCountedBy(original.getCountedBy());
        masked.setCountedTime(original.getCountedTime());
        masked.setRecountedBy(original.getRecountedBy());
        masked.setRecountedTime(original.getRecountedTime());
        masked.setVersion(original.getVersion());
        return masked;
    }

    private void assertCountRequestValid(StocktakeCountRequest request) {
        if (request.getActualQuantity() == null) {
            throw new ServiceException("实际盘点数量不能为空");
        }
        if (request.getActualQuantity() < 0) {
            throw new ServiceException("实际盘点数量不能为负数");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isEmpty()) {
            throw new ServiceException("幂等键不能为空");
        }
    }

    private boolean isSameCountPayload(FinStocktakeItem item, StocktakeCountRequest request) {
        if (item.getActualQuantity() == null
                || !item.getActualQuantity().equals(request.getActualQuantity())) {
            return false;
        }
        boolean reasonCodeSame = (item.getReasonCode() == null && request.getReasonCode() == null)
                || (item.getReasonCode() != null && item.getReasonCode().equals(request.getReasonCode()));
        if (!reasonCodeSame) {
            return false;
        }
        boolean reasonSame = (item.getReason() == null && request.getReason() == null)
                || (item.getReason() != null && item.getReason().equals(request.getReason()));
        return reasonSame;
    }

    private void assertRecountRequestValid(StocktakeRecountRequest request) {
        if (request.getRecountQuantity() == null) {
            throw new ServiceException("复盘数量不能为空");
        }
        if (request.getRecountQuantity() < 0) {
            throw new ServiceException("复盘数量不能为负数");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isEmpty()) {
            throw new ServiceException("复盘幂等键不能为空");
        }
    }

    private boolean isSameRecountPayload(FinStocktakeItem item, StocktakeRecountRequest request) {
        if (item.getRecountQuantity() == null
                || !item.getRecountQuantity().equals(request.getRecountQuantity())) {
            return false;
        }
        boolean reasonCodeSame = (item.getReasonCode() == null && request.getReasonCode() == null)
                || (item.getReasonCode() != null && item.getReasonCode().equals(request.getReasonCode()));
        if (!reasonCodeSame) {
            return false;
        }
        boolean reasonSame = (item.getReason() == null && request.getReason() == null)
                || (item.getReason() != null && item.getReason().equals(request.getReason()));
        return reasonSame;
    }
}
