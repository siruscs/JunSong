package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.api.domain.StocktakeWorkflowSyncReq;
import com.junsong.finance.domain.FinProduct;
import com.junsong.finance.domain.FinStockLedger;
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
import com.junsong.finance.domain.vo.StocktakeReverseRequest;
import com.junsong.finance.mapper.FinProductMapper;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.mapper.FinStocktakeMapper;
import com.junsong.finance.service.IFinStocktakeService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.model.LoginUser;
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

    private static BigDecimal nzBox(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static final Logger log = LoggerFactory.getLogger(FinStocktakeServiceImpl.class);
    private static final List<Long> SENTINEL_DEPT_IDS = Collections.singletonList(-1L);
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_COUNTING = "COUNTING";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_RECOUNTING = "RECOUNTING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_POSTED = "POSTED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_REVERSED = "REVERSED";
    private static final String STOCK_TAKE_GAIN = "STOCK_TAKE_GAIN";
    private static final String STOCK_TAKE_LOSS = "STOCK_TAKE_LOSS";
    private static final String STOCK_TAKE_REVERSE = "STOCK_TAKE_REVERSE";
    private static final String REF_STOCKTAKE = "STOCKTAKE";
    private static final String REF_STOCKTAKE_REVERSE = "STOCKTAKE_REVERSE";
    private static final String PERIOD_ACTIVE = "0";
    private static final String PROCESS_KEY_STOCKTAKE = "stocktake_apply";

    /** 复盘数量阈值：当任一行 |variance_quantity| 超过此值时触发强制复盘 */
    @Value("${finance.stocktake.recount.quantityThreshold:5}")
    private int recountQuantityThreshold;

    /** 复盘金额阈值（元）：当任一行 |variance_amount| 超过此值时触发强制复盘 */
    @Value("${finance.stocktake.recount.amountThreshold:100}")
    private java.math.BigDecimal recountAmountThreshold;

    /** 工作流服务地址（用于启动盘点流程实例，仅追踪/待办用途） */
    @Value("${finance.workflow.service-url:http://junsong-workflow:9207}")
    private String workflowServiceUrl;

    @Autowired
    private FinStocktakeMapper finStocktakeMapper;

    @Autowired
    private FinStockLedgerMapper finStockLedgerMapper;

    @Autowired
    private FinProductMapper finProductMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private com.junsong.finance.service.IStockCostService stockCostService;

    @Autowired
    private com.junsong.finance.mapper.FinAccountingPeriodMapper accountingPeriodMapper;

    /**
     * RestTemplate（5s 连接 / 10s 读超时）。工作流为追踪用途，失败时优雅降级，
     * 不阻塞盘点主流程。允许测试通过反射注入 fake 实例。
     */
    private RestTemplate workflowRestTemplate;

    private RestTemplate getWorkflowRestTemplate() {
        if (workflowRestTemplate == null) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(10000);
            workflowRestTemplate = new RestTemplate(factory);
        }
        return workflowRestTemplate;
    }

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

            BigDecimal position = finStockLedgerMapper.selectPositionQuantity(tenantId, request.getDeptId(), productId);
            BigDecimal expectedQty = position == null ? BigDecimal.ZERO : position;

            FinStocktakeItem item = new FinStocktakeItem();
            item.setTenantId(tenantId);
            item.setDeptId(request.getDeptId());
            item.setProductId(productId);
            item.setProductName(product.getProductName());
            item.setExpectedQuantity(expectedQty);
            item.setMovementQuantityAfterFreeze(BigDecimal.ZERO);
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
        if (!STATUS_DRAFT.equals(header.getStatus()) && !STATUS_COUNTING.equals(header.getStatus())) {
            throw new ServiceException("仅草稿或盘点中状态可分配盘点人，当前状态: " + header.getStatus());
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
        history.setFromStatus(header.getStatus());
        history.setToStatus(header.getStatus());
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
                SecurityUtils.getUsername(), null, null, null, null, null, null);
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

        // 仅分配的 counter 可录入（管理员也不例外，盲盘由任务角色决定）
        Long currentUserId = SecurityUtils.getUserId();
        // 工作流内部过账在 @InnerAuth 下执行，不携带前台用户；正常接口仍必须校验盘点人。
        if (currentUserId != null && !currentUserId.equals(header.getCounterUserId())) {
            throw new ServiceException("仅分配的盘点人可录入盘点数据");
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
        BigDecimal expected = item.getExpectedQuantity() == null ? BigDecimal.ZERO : item.getExpectedQuantity();
        BigDecimal variance = request.getActualQuantity().subtract(expected);
        if (variance.signum() != 0) {
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
        boolean hasThresholdViolation = false;
        java.math.BigDecimal amountThreshold = recountAmountThreshold == null
                ? java.math.BigDecimal.valueOf(100)
                : recountAmountThreshold;

        for (FinStocktakeItem item : items) {
            BigDecimal expected = item.getExpectedQuantity() == null ? BigDecimal.ZERO : item.getExpectedQuantity();
            BigDecimal movement = finStockLedgerMapper.sumMovementAfterFreeze(
                    tenantId, header.getDeptId(), item.getProductId(), header.getFreezeTime());
            BigDecimal movementAfterFreeze = movement == null ? BigDecimal.ZERO : movement;
            BigDecimal adjustedExpected = expected.add(movementAfterFreeze);
            BigDecimal actual = item.getActualQuantity() == null ? BigDecimal.ZERO : item.getActualQuantity();
            BigDecimal variance = actual.subtract(adjustedExpected);

            int itemAffected = finStocktakeMapper.updateStocktakeItemFinal(
                    tenantId, item.getItemId(),
                    null,
                    variance,
                    null,
                    null,
                    item.getReasonCode(),
                    item.getReason(),
                    movementAfterFreeze,
                    adjustedExpected,
                    item.getVersion());
            if (itemAffected != 1) {
                throw new ServiceException("更新盘点行临时方差失败，可能已被其他操作更新");
            }

            // 阈值检查：数量阈值或金额阈值任一超限即触发强制复盘
            boolean qtyOver = variance.abs().compareTo(BigDecimal.valueOf(recountQuantityThreshold)) > 0;
            boolean amountOver = false;
            if (item.getUnitCost() != null && item.getUnitCost().signum() > 0) {
                java.math.BigDecimal varianceAmt = item.getUnitCost()
                        .multiply(variance.abs())
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                amountOver = varianceAmt.compareTo(amountThreshold) > 0;
            }
            if (qtyOver || amountOver) {
                hasThresholdViolation = true;
                if (header.getRecountUserId() != null) {
                    needRecount = true;
                }
            }
        }

        // 超过阈值但未分配复盘人：禁止提交，必须先分配独立复盘人
        if (hasThresholdViolation && header.getRecountUserId() == null) {
            throw new ServiceException("存在超出复盘阈值的盘点行，必须先分配独立复盘人才能提交");
        }

        String toStatus = needRecount ? STATUS_RECOUNTING : STATUS_SUBMITTED;
        int affected = finStocktakeMapper.updateStocktakeStatus(
                tenantId, stocktakeId, STATUS_COUNTING, toStatus, version,
                SecurityUtils.getUsername(), SecurityUtils.getUsername(), null, null, null, null, null);
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

        // 启动工作流实例（仅用于待办/追踪，失败时优雅降级，不阻塞提交）
        startWorkflowProcess(header, needRecount);

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

        // 仅分配的 recountUserId 可录入（管理员也不例外）
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId == null || !currentUserId.equals(header.getRecountUserId())) {
            throw new ServiceException("仅分配的复盘人可录入复盘数据");
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
        BigDecimal adjustedExpected = item.getAdjustedExpectedQuantity() == null
                ? (item.getExpectedQuantity() == null ? BigDecimal.ZERO : item.getExpectedQuantity())
                : item.getAdjustedExpectedQuantity();
        BigDecimal recountVariance = request.getRecountQuantity().subtract(adjustedExpected);
        if (recountVariance.signum() != 0) {
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
                    SecurityUtils.getUsername(), null, SecurityUtils.getUsername(), null, null, null, null);
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

        // RECOUNTING 状态下：校验所有行都有复盘数量，未完成复盘禁止审批
        if (STATUS_RECOUNTING.equals(fromStatus)) {
            for (FinStocktakeItem item : items) {
                if (item.getRecountQuantity() == null) {
                    throw new ServiceException("存在未完成复盘的盘点行: productId=" + item.getProductId()
                            + "，全部复盘完成后才能审批");
                }
            }
        }

        for (FinStocktakeItem item : items) {
            // finalQuantity 优先使用 recountQuantity（如有），否则 actualQuantity
            BigDecimal finalQty = item.getRecountQuantity() != null
                    ? item.getRecountQuantity()
                    : item.getActualQuantity();
            if (finalQty == null) {
                throw new ServiceException("盘点行缺少实际或复盘数量: productId=" + item.getProductId());
            }

            BigDecimal adjustedExpected = item.getAdjustedExpectedQuantity() == null
                    ? (item.getExpectedQuantity() == null ? BigDecimal.ZERO : item.getExpectedQuantity())
                    : item.getAdjustedExpectedQuantity();
            BigDecimal finalVariance = finalQty.subtract(adjustedExpected);

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
                SecurityUtils.getUsername(), null, SecurityUtils.getUsername(), null, null, null, null);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int postStocktake(Long stocktakeId, Integer version) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止过账盘点任务");
        }
        if (version == null) {
            throw new ServiceException("版本号不能为空");
        }

        FinStocktake header = finStocktakeMapper.selectStocktakeForUpdate(tenantId, stocktakeId);
        if (header == null) {
            throw new ServiceException("盘点任务不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        if (!STATUS_APPROVED.equals(header.getStatus())) {
            throw new ServiceException("仅已审批状态可过账，当前状态: " + header.getStatus());
        }
        if (!version.equals(header.getVersion())) {
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

        // 幂等校验：takeNo 已有库存流水则拒绝重复过账
        int existing = finStockLedgerMapper.countByReferenceNo(tenantId, header.getTakeNo());
        if (existing > 0) {
            throw new ServiceException("盘点任务已过账，禁止重复过账: " + header.getTakeNo());
        }

        // 锁定行表
        List<FinStocktakeItem> items = finStocktakeMapper.selectStocktakeItemsForUpdate(tenantId, stocktakeId);
        if (items == null || items.isEmpty()) {
            throw new ServiceException("盘点任务无明细行，禁止过账");
        }

        // 按 (deptId, productId) 升序排序（避免死锁）
        List<FinStocktakeItem> sortedItems = new ArrayList<>(items);
        sortedItems.sort((a, b) -> {
            int byDept = a.getDeptId().compareTo(b.getDeptId());
            if (byDept != 0) return byDept;
            return a.getProductId().compareTo(b.getProductId());
        });

        String operator = SecurityUtils.getUsername();

        // 第一步：按固定顺序锁定全部库存结存行（包括零差异行），防止并发采购/销售干扰
        for (FinStocktakeItem item : sortedItems) {
            finStockLedgerMapper.insertPositionIfAbsent(tenantId, item.getDeptId(), item.getProductId());
            finStockLedgerMapper.selectPositionQuantityForUpdate(
                    tenantId, item.getDeptId(), item.getProductId());
        }

        // 第二步：持锁状态下重新汇总冻结后流水，计算最终方差
        for (FinStocktakeItem item : sortedItems) {
            BigDecimal movement = finStockLedgerMapper.sumMovementAfterFreeze(
                    tenantId, item.getDeptId(), item.getProductId(), header.getFreezeTime());
            BigDecimal movementAfterFreeze = movement == null ? BigDecimal.ZERO : movement;
            BigDecimal expected = item.getExpectedQuantity() == null ? BigDecimal.ZERO : item.getExpectedQuantity();
            BigDecimal adjustedExpected = expected.add(movementAfterFreeze);

            BigDecimal finalQty = item.getFinalQuantity();
            if (finalQty == null) {
                throw new ServiceException("盘点行缺少最终数量: productId=" + item.getProductId());
            }
            BigDecimal finalVariance = finalQty.subtract(adjustedExpected);

            // 重新读取当前库存数量（已持锁）
            BigDecimal currentQty = nzBox(finStockLedgerMapper.selectPositionQuantityForUpdate(
                    tenantId, item.getDeptId(), item.getProductId()));

            if (finalVariance.signum() == 0) {
                // 无差异：仅更新行表的 movement/adjusted 字段，不写流水
                int itemAffected = finStocktakeMapper.updateStocktakeItemFinal(
                        tenantId, item.getItemId(),
                        finalQty, BigDecimal.ZERO,
                        null, null,
                        item.getReasonCode(), item.getReason(),
                        movementAfterFreeze, adjustedExpected,
                        item.getVersion());
                if (itemAffected != 1) {
                    throw new ServiceException("更新盘点行过账字段失败（行版本冲突）");
                }
                continue;
            }

            BigDecimal absVariance = finalVariance.abs();
            BigDecimal afterQty = currentQty.add(finalVariance);
            if (afterQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("盘亏后库存为负（商品 " + item.getProductId()
                        + " 当前 " + currentQty + "，盘亏 " + absVariance + "），拒绝过账");
            }

            String changeType = finalVariance.signum() > 0 ? STOCK_TAKE_GAIN : STOCK_TAKE_LOSS;

            // 写库存流水
            FinStockLedger ledger = new FinStockLedger();
            ledger.setTenantId(tenantId);
            ledger.setDeptId(item.getDeptId());
            ledger.setProductId(item.getProductId());
            ledger.setProductName(item.getProductName());
            ledger.setChangeType(changeType);
            ledger.setChangeQuantity(finalVariance);
            ledger.setBeforeQuantity(currentQty);
            ledger.setAfterQuantity(afterQty);
            ledger.setReferenceType(REF_STOCKTAKE);
            ledger.setReferenceId(stocktakeId);
            ledger.setReferenceNo(header.getTakeNo());
            // DB 唯一键兜底：同一盘点任务同一商品只能生成一条过账流水
            ledger.setIdempotencyKey(REF_STOCKTAKE + ":" + stocktakeId + ":" + item.getProductId());
            ledger.setDelFlag("0");
            ledger.setCreateBy(operator);
            ledger.setRemark(item.getReason());
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

            // 成本联动：盘亏按当前平均成本，盘盈金额默认按当前平均成本 * 数量
            Long costLedgerId;
            if (finalVariance.signum() < 0) {
                costLedgerId = stockCostService.applyStocktakeLoss(
                        tenantId, item.getDeptId(), item.getProductId(),
                        absVariance, stockLedgerId, operator);
            } else {
                costLedgerId = stockCostService.applyStocktakeGain(
                        tenantId, item.getDeptId(), item.getProductId(),
                        absVariance, null, stockLedgerId, operator);
            }

            if (costLedgerId == null) {
                throw new ServiceException("成本流水ID未生成，过账失败");
            }

            // 从成本流水查询固化的单位成本（过账后立即查询，确保数据一致）
            java.math.BigDecimal solidifiedUnitCost = stockCostService.getCostLedgerUnitCost(tenantId, costLedgerId);
            if (solidifiedUnitCost == null || solidifiedUnitCost.signum() <= 0) {
                // 盘盈时如果没有正成本也拒绝，避免零成本入账
                throw new ServiceException("无法获取固化单位成本（商品 " + item.getProductId()
                        + "，costLedgerId=" + costLedgerId + "），拒绝过账");
            }

            // 更新行表过账字段（含固化单位成本和差异金额）
            java.math.BigDecimal unitCostForItem = solidifiedUnitCost;
            java.math.BigDecimal varianceAmount = unitCostForItem
                    .multiply(absVariance)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            int originalItemVersion = item.getVersion();
            int itemAffected = finStocktakeMapper.updateStocktakeItemFinal(
                    tenantId, item.getItemId(),
                    finalQty, finalVariance,
                    unitCostForItem, varianceAmount,
                    item.getReasonCode(), item.getReason(),
                    movementAfterFreeze, adjustedExpected,
                    originalItemVersion);
            if (itemAffected != 1) {
                throw new ServiceException("更新盘点行过账字段失败（行版本冲突）");
            }

            int refAffected = finStocktakeMapper.updateStocktakeItemPostingRefs(
                    tenantId, item.getItemId(),
                    stockLedgerId, costLedgerId,
                    originalItemVersion + 1);
            if (refAffected != 1) {
                throw new ServiceException("更新盘点行过账引用失败（行版本冲突）");
            }
        }

        // 状态流转 APPROVED → POSTED
        int affected = finStocktakeMapper.updateStocktakeStatus(
                tenantId, stocktakeId, STATUS_APPROVED, STATUS_POSTED, version,
                operator, null, null, operator, null, null, null);
        if (affected != 1) {
            throw new ServiceException("过账盘点任务失败，可能已被其他操作更新");
        }

        FinStocktakeHistory history = new FinStocktakeHistory();
        history.setTenantId(tenantId);
        history.setStocktakeId(stocktakeId);
        history.setAction("POST");
        history.setFromStatus(STATUS_APPROVED);
        history.setToStatus(STATUS_POSTED);
        history.setOperator(operator);
        history.setComment("过账完成，数量与成本原子更新");
        finStocktakeMapper.insertStocktakeHistory(history);

        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelStocktake(Long stocktakeId, Integer version) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止取消盘点任务");
        }
        if (version == null) {
            throw new ServiceException("版本号不能为空");
        }

        FinStocktake header = finStocktakeMapper.selectStocktakeForUpdate(tenantId, stocktakeId);
        if (header == null) {
            throw new ServiceException("盘点任务不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        // 已 POSTED / CANCELLED / REVERSED 的任务禁止取消
        String status = header.getStatus();
        if (STATUS_POSTED.equals(status) || STATUS_CANCELLED.equals(status) || STATUS_REVERSED.equals(status)) {
            throw new ServiceException("仅过账前的盘点任务可取消，当前状态: " + status);
        }
        if (!version.equals(header.getVersion())) {
            throw new ServiceException("版本号不匹配，请刷新后重试");
        }

        // 流转到 CANCELLED（不写库存/成本，因为尚未过账）
        int affected = finStocktakeMapper.updateStocktakeStatus(
                tenantId, stocktakeId, status, STATUS_CANCELLED, version,
                SecurityUtils.getUsername(), null, null, null, null, null, null);
        if (affected != 1) {
            throw new ServiceException("取消盘点任务失败，可能已被其他操作更新");
        }

        FinStocktakeHistory history = new FinStocktakeHistory();
        history.setTenantId(tenantId);
        history.setStocktakeId(stocktakeId);
        history.setAction("CANCEL");
        history.setFromStatus(status);
        history.setToStatus(STATUS_CANCELLED);
        history.setOperator(SecurityUtils.getUsername());
        history.setComment("取消盘点任务（过账前软删除）");
        finStocktakeMapper.insertStocktakeHistory(history);

        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int reverseStocktake(Long stocktakeId, StocktakeReverseRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止冲销盘点任务");
        }
        if (request == null) {
            throw new ServiceException("冲销请求不能为空");
        }
        if (request.getReason() == null || request.getReason().isEmpty()) {
            throw new ServiceException("冲销理由不能为空");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isEmpty()) {
            throw new ServiceException("冲销幂等键不能为空");
        }
        if (request.getVersion() == null) {
            throw new ServiceException("版本号不能为空");
        }

        // 幂等治理三层兜底：
        // 1. AOP 切面：@Idempotent(scene="stocktake:reverse") + sys_idempotency_record 原子占位
        // 2. 业务状态机：仅 POSTED 可冲销，REVERSED 拒绝二次冲销（业务层兜底）
        // 3. DB 唯一索引：finance_stocktake.uk_reverse_idempotency_key (tenant_id, reverse_idempotency_key)
        //    由 sql/finance_high_risk_idempotency_constraints.sql 创建，即使 AOP 失效也能阻止重复写入
        // 同一任务的重复冲销请求由 AOP 返回原结果；DB 唯一索引在 AOP 失效时提供最终兜底。

        FinStocktake header = finStocktakeMapper.selectStocktakeForUpdate(tenantId, stocktakeId);
        if (header == null) {
            throw new ServiceException("盘点任务不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());

        // 仅 POSTED 状态可冲销；已 REVERSED 拒绝二次冲销
        if (!STATUS_POSTED.equals(header.getStatus())) {
            throw new ServiceException("仅已过账状态可冲销，当前状态: " + header.getStatus()
                    + (STATUS_REVERSED.equals(header.getStatus()) ? "（已冲销，拒绝二次冲销）" : ""));
        }
        if (!request.getVersion().equals(header.getVersion())) {
            throw new ServiceException("版本号不匹配，请刷新后重试");
        }

        // 会计期间必须为 ACTIVE（持锁验证，防止与结转并发）
        com.junsong.finance.domain.FinAccountingPeriod period =
                accountingPeriodMapper.selectCurrentPeriodByDeptIdForUpdate(tenantId, header.getDeptId());
        if (period == null || !PERIOD_ACTIVE.equals(period.getStatus())) {
            throw new ServiceException("门店 " + header.getDeptId() + " 会计期间已结转或不存在，禁止冲销");
        }

        // 锁定行表并按 (deptId, productId) 排序（避免死锁）
        List<FinStocktakeItem> items = finStocktakeMapper.selectStocktakeItemsForUpdate(tenantId, stocktakeId);
        if (items == null || items.isEmpty()) {
            throw new ServiceException("盘点任务无明细行，禁止冲销");
        }
        List<FinStocktakeItem> sortedItems = new ArrayList<>(items);
        sortedItems.sort((a, b) -> {
            int byDept = a.getDeptId().compareTo(b.getDeptId());
            return byDept != 0 ? byDept : a.getProductId().compareTo(b.getProductId());
        });

        String operator = SecurityUtils.getUsername();
        String reverseRefNo = header.getTakeNo() + ":REVERSE";
        java.util.Date postedTime = header.getPostedTime();

        // 下游使用检查：过账后若有其他业务流水（销售/进货/其他盘点等），则拒绝冲销
        if (postedTime != null) {
            for (FinStocktakeItem item : sortedItems) {
                if (item.getStockLedgerId() == null || item.getVarianceQuantity() == null
                        || item.getVarianceQuantity().signum() == 0) {
                    continue;
                }
                int downstreamCount = finStockLedgerMapper.countDownstreamLedgersAfterTime(
                        tenantId, item.getDeptId(), item.getProductId(), postedTime);
                if (downstreamCount > 0) {
                    throw new ServiceException("商品 " + item.getProductId() + "（" + item.getProductName()
                            + "）在盘点过账后已有 " + downstreamCount + " 笔下游业务流水，禁止冲销");
                }
            }
        }

        for (FinStocktakeItem item : sortedItems) {
            // 仅过账时写了流水的行需要冲销（stockLedgerId 非空表示有差异已过账）
            if (item.getStockLedgerId() == null) {
                continue;
            }
            if (item.getReverseStockLedgerId() != null) {
                // 已冲销的行跳过（幂等保护，正常不应走到这里因为头表状态校验）
                continue;
            }

            BigDecimal varianceBox = item.getVarianceQuantity();
            BigDecimal originalVariance = varianceBox == null ? BigDecimal.ZERO : varianceBox;
            if (originalVariance.signum() == 0) {
                continue;
            }

            BigDecimal absVariance = originalVariance.abs();

            // 获取原固化单位成本：优先行表，为 null 时通过成本服务查询
            java.math.BigDecimal unitCost = item.getUnitCost();
            if (unitCost == null && item.getCostLedgerId() != null) {
                unitCost = stockCostService.getCostLedgerUnitCost(tenantId, item.getCostLedgerId());
            }
            if (unitCost == null || unitCost.signum() <= 0) {
                throw new ServiceException("冲销行缺少原固化成本（商品 " + item.getProductId()
                        + "，costLedgerId=" + item.getCostLedgerId() + "），拒绝冲销");
            }

            // 锁定 position 行
            finStockLedgerMapper.insertPositionIfAbsent(tenantId, item.getDeptId(), item.getProductId());
            BigDecimal currentQty = nzBox(finStockLedgerMapper.selectPositionQuantityForUpdate(
                    tenantId, item.getDeptId(), item.getProductId()));

            // 反向变动：原盘亏(负) → 冲销为正；原盘盈(正) → 冲销为负
            BigDecimal reverseChange = originalVariance.negate();
            BigDecimal afterQty = currentQty.add(reverseChange);
            if (afterQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("冲销后库存为负（商品 " + item.getProductId()
                        + " 当前 " + currentQty + "，冲销 " + reverseChange.abs() + "），拒绝冲销");
            }

            // 写反向库存流水
            FinStockLedger reverseLedger = new FinStockLedger();
            reverseLedger.setTenantId(tenantId);
            reverseLedger.setDeptId(item.getDeptId());
            reverseLedger.setProductId(item.getProductId());
            reverseLedger.setProductName(item.getProductName());
            reverseLedger.setChangeType(STOCK_TAKE_REVERSE);
            reverseLedger.setChangeQuantity(reverseChange);
            reverseLedger.setBeforeQuantity(currentQty);
            reverseLedger.setAfterQuantity(afterQty);
            reverseLedger.setReferenceType(REF_STOCKTAKE_REVERSE);
            reverseLedger.setReferenceId(stocktakeId);
            reverseLedger.setReferenceNo(reverseRefNo);
            // DB 唯一键兜底：同一盘点任务同一商品只能生成一条冲销流水
            reverseLedger.setIdempotencyKey(REF_STOCKTAKE_REVERSE + ":" + stocktakeId + ":" + item.getProductId());
            reverseLedger.setDelFlag("0");
            reverseLedger.setCreateBy(operator);
            reverseLedger.setRemark("冲销: " + request.getReason());
            finStockLedgerMapper.insertFinStockLedger(reverseLedger);
            Long reverseStockLedgerId = reverseLedger.getLedgerId();
            if (reverseStockLedgerId == null) {
                throw new ServiceException("冲销库存流水ID未生成，冲销失败");
            }

            // 更新结存
            int positionAffected = finStockLedgerMapper.updatePositionQuantity(
                    tenantId, item.getDeptId(), item.getProductId(), afterQty);
            if (positionAffected != 1) {
                throw new ServiceException("冲销库存结存更新影响行数异常（" + positionAffected + "），事务回滚");
            }

            // 成本冲销：复用原 unitCost，关联原 costLedgerId
            Long reverseCostLedgerId = stockCostService.reverseStocktakeAdjustment(
                    tenantId, item.getDeptId(), item.getProductId(),
                    absVariance, unitCost,
                    reverseStockLedgerId, item.getCostLedgerId(),
                    operator);
            if (reverseCostLedgerId == null) {
                throw new ServiceException("冲销成本流水ID未生成，冲销失败");
            }

            // 更新行表冲销引用
            int refAffected = finStocktakeMapper.updateStocktakeItemReverseRefs(
                    tenantId, item.getItemId(),
                    reverseStockLedgerId, reverseCostLedgerId,
                    item.getVersion());
            if (refAffected != 1) {
                throw new ServiceException("更新盘点行冲销引用失败（行版本冲突）");
            }
        }

        // 状态流转 POSTED → REVERSED
        // DB 唯一键兜底：从 AOP ThreadLocal 读取幂等键填充 finance_stocktake.reverse_idempotency_key 列，
        // 使 sql/finance_high_risk_idempotency_constraints.sql 中的 uk_reverse_idempotency_key 约束生效。
        // 优先使用 AOP 幂等键（与 FinCostAccounting/FinInvestorPayment 实现一致），
        // 兜底使用 request.idempotencyKey（如果调用方显式透传）。
        String reverseIdempotencyKey = com.junsong.common.core.idempotency.IdempotencyResultStore.currentKey();
        if (reverseIdempotencyKey == null) {
            reverseIdempotencyKey = request.getIdempotencyKey();
        }
        int affected = finStocktakeMapper.updateStocktakeStatus(
                tenantId, stocktakeId, STATUS_POSTED, STATUS_REVERSED, request.getVersion(),
                operator, null, null, null, operator, request.getReason(), reverseIdempotencyKey);
        if (affected != 1) {
            throw new ServiceException("冲销盘点任务失败，可能已被其他操作更新");
        }

        FinStocktakeHistory history = new FinStocktakeHistory();
        history.setTenantId(tenantId);
        history.setStocktakeId(stocktakeId);
        history.setAction("REVERSE");
        history.setFromStatus(STATUS_POSTED);
        history.setToStatus(STATUS_REVERSED);
        history.setOperator(operator);
        history.setComment("整单冲销: " + request.getReason());
        finStocktakeMapper.insertStocktakeHistory(history);

        return affected;
    }

    @Override
    public List<FinStocktakeItem> listStocktakeItems(Long stocktakeId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止查询盘点明细");
        }
        FinStocktake header = finStocktakeMapper.selectStocktakeById(tenantId, stocktakeId);
        if (header == null) {
            throw new ServiceException("盘点任务不存在或无权访问");
        }
        assertDeptAuthorized(tenantId, header.getDeptId());
        return finStocktakeMapper.listStocktakeItems(tenantId, stocktakeId);
    }

    @Override
    public int syncWorkflowStatus(StocktakeWorkflowSyncReq req) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止同步盘点工作流状态");
        }
        if (req == null) {
            throw new ServiceException("工作流同步请求不能为空");
        }

        Long stocktakeId = req.getStocktakeId();
        if (stocktakeId == null && "COMPLETE".equalsIgnoreCase(req.getAction())
                && req.getFormData() != null && req.getBusinessKey() != null) {
            FinStocktake existing = finStocktakeMapper.selectStocktakeByTakeNo(tenantId, req.getBusinessKey());
            stocktakeId = existing == null ? createAndPostWorkflowStocktake(req) : existing.getStocktakeId();
        }
        if (stocktakeId == null && req.getProcessInstanceId() != null) {
            // 工作流回调可能只携带 processInstanceId（如 afterReject），按实例ID反查
            FinStocktake existing = finStocktakeMapper.selectByProcessInstanceId(tenantId, req.getProcessInstanceId());
            if (existing == null) {
                log.warn("工作流同步：未找到 processInstanceId={} 对应的盘点任务", req.getProcessInstanceId());
                return 0;
            }
            stocktakeId = existing.getStocktakeId();
        }
        if (stocktakeId == null) {
            log.warn("工作流同步：缺少 stocktakeId 且无法通过 processInstanceId 查找");
            return 0;
        }

        if ("COMPLETE".equalsIgnoreCase(req.getAction())) {
            FinStocktake header = finStocktakeMapper.selectStocktakeForUpdate(tenantId, stocktakeId);
            if (header != null && (STATUS_SUBMITTED.equals(header.getStatus()) || STATUS_RECOUNTING.equals(header.getStatus()))) {
                StocktakeApprovalRequest approval = new StocktakeApprovalRequest();
                approval.setDecision("APPROVE");
                approval.setVersion(header.getVersion());
                approval.setComment("工作流审批完成，自动确认盘点结果");
                approveStocktake(stocktakeId, approval);
                FinStocktake approved = finStocktakeMapper.selectStocktakeForUpdate(tenantId, stocktakeId);
                if (approved != null && STATUS_APPROVED.equals(approved.getStatus())) {
                    postStocktake(stocktakeId, approved.getVersion());
                }
            }
        }

        return finStocktakeMapper.updateWorkflowInfo(tenantId, stocktakeId,
                req.getProcessInstanceId(), req.getCurrentNode());
    }

    /** 将通用低代码盘点单落到原生盘点状态机，再执行标准过账。 */
    @Transactional(rollbackFor = Exception.class)
    private Long createAndPostWorkflowStocktake(StocktakeWorkflowSyncReq req) {
        Map<String, Object> form = req.getFormData();
        StocktakeCreateRequest create = new StocktakeCreateRequest();
        create.setTakeNo(req.getBusinessKey());
        create.setDeptId(asLong(form.get("dept_id")));
        create.setCounterUserId(asLong(form.get("counter_user_id")));
        create.setRecountUserId(asLong(form.get("recount_user_id")));
        create.setScopeType(String.valueOf(form.getOrDefault("scope_type", "SELECTED_PRODUCTS")));
        List<Long> productIds = new ArrayList<>();
        Object rawItems = form.get("stocktake_items");
        if (rawItems instanceof List<?> items) {
            for (Object raw : items) {
                if (raw instanceof Map<?, ?> item && asLong(item.get("product_id")) != null) {
                    productIds.add(asLong(item.get("product_id")));
                }
            }
        }
        create.setProductIds(productIds);
        create.setRemark(String.valueOf(form.getOrDefault("remark", "低代码流程自动同步")));
        if (create.getDeptId() == null || create.getCounterUserId() == null) {
            throw new ServiceException("低代码盘点单缺少盘点门店或盘点人，无法自动过账");
        }
        Long id = createStocktake(create);
        startStocktake(id, 0);
        StocktakeDetailVO detail = getStocktakeDetail(id);
        if (detail.getItems() == null || !(rawItems instanceof List<?> items)) {
            throw new ServiceException("低代码盘点明细为空，无法自动过账");
        }
        for (int i = 0; i < detail.getItems().size(); i++) {
            FinStocktakeItem target = detail.getItems().get(i);
            Map<?, ?> source = i < items.size() && items.get(i) instanceof Map<?, ?> m ? m : Map.of();
            StocktakeCountRequest count = new StocktakeCountRequest();
            Integer rawQty = asInt(source.get("actual_quantity"));
            count.setActualQuantity(rawQty == null ? null : BigDecimal.valueOf(rawQty));
            if (count.getActualQuantity() == null || count.getActualQuantity().signum() < 0) {
                throw new ServiceException("低代码盘点明细缺少有效盘点数量，无法自动过账");
            }
            count.setVersion(target.getVersion());
            count.setIdempotencyKey("workflow:" + req.getProcessInstanceId() + ":" + target.getItemId());
            count.setReasonCode("OTHER");
            count.setReason("低代码流程自动同步");
            countItem(id, target.getItemId(), count);
        }
        FinStocktake afterCount = finStocktakeMapper.selectStocktakeForUpdate(TenantContext.getTenantId(), id);
        submitStocktake(id, afterCount.getVersion());
        FinStocktake submitted = finStocktakeMapper.selectStocktakeForUpdate(TenantContext.getTenantId(), id);
        if (STATUS_RECOUNTING.equals(submitted.getStatus())) {
            StocktakeDetailVO recountDetail = getStocktakeDetail(id);
            for (FinStocktakeItem item : recountDetail.getItems()) {
                finStocktakeMapper.updateStocktakeItemRecount(
                        TenantContext.getTenantId(), item.getItemId(), item.getActualQuantity(),
                        "OTHER", "低代码流程自动复盘确认", "workflow:recount:" + req.getProcessInstanceId() + ":" + item.getItemId(),
                        "SYSTEM_WORKFLOW", item.getVersion());
            }
            submitted = finStocktakeMapper.selectStocktakeForUpdate(TenantContext.getTenantId(), id);
        }
        StocktakeApprovalRequest approval = new StocktakeApprovalRequest();
        approval.setDecision("APPROVE");
        approval.setVersion(submitted.getVersion());
        approval.setComment("工作流审批完成，自动确认盘点结果");
        approveStocktake(id, approval);
        FinStocktake approved = finStocktakeMapper.selectStocktakeForUpdate(TenantContext.getTenantId(), id);
        postStocktake(id, approved.getVersion());
        return id;
    }

    private static Long asLong(Object value) { return value == null ? null : Long.valueOf(String.valueOf(value)); }
    private static Integer asInt(Object value) { return value == null ? null : Integer.valueOf(String.valueOf(value)); }

    // ===== 工作流集成（追踪/待办用途，失败优雅降级） =====

    /**
     * 启动盘点工作流流程实例。
     * 工作流仅用于待办/追踪，不作为业务闸门：
     * - 成功：保存 processInstanceId 到头表，初始 currentNode="Task_Count"
     * - 失败：记录警告日志，不抛异常，不回滚提交事务
     */
    private void startWorkflowProcess(FinStocktake header, boolean needRecount) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("counterUsername", requireUsername(header.getCounterUserId()));
            variables.put("recountUsername", header.getRecountUserId() == null
                    ? "" : requireUsername(header.getRecountUserId()));
            variables.put("approverUsername", resolveApproverUsername());
            variables.put("needRecount", needRecount);
            variables.put("stocktakeId", header.getStocktakeId());
            variables.put("deptId", header.getDeptId());
            variables.put("takeNo", header.getTakeNo());
            variables.put("tenantId", header.getTenantId());

            Map<String, Object> body = new HashMap<>();
            body.put("processKey", PROCESS_KEY_STOCKTAKE);
            body.put("businessKey", header.getTakeNo());
            body.put("variables", variables);

            String url = workflowServiceUrl + "/instance/start";
            @SuppressWarnings("rawtypes")
            Map response = getWorkflowRestTemplate().postForObject(url, body, Map.class);

            String processInstanceId = extractProcessInstanceId(response);
            if (processInstanceId != null) {
                finStocktakeMapper.updateWorkflowInfo(header.getTenantId(),
                        header.getStocktakeId(), processInstanceId, "Task_Count");
                header.setProcessInstanceId(processInstanceId);
                header.setProcessDefinitionKey(PROCESS_KEY_STOCKTAKE);
                header.setBusinessKey(header.getTakeNo());
                header.setCurrentNode("Task_Count");
                log.info("盘点工作流已启动: stocktakeId={}, processInstanceId={}",
                        header.getStocktakeId(), processInstanceId);
            } else {
                log.warn("盘点工作流启动返回非预期响应: stocktakeId={}, response={}",
                        header.getStocktakeId(), response);
            }
        } catch (Exception e) {
            log.warn("盘点工作流启动失败（优雅降级，不阻塞提交）: stocktakeId={}, error={}",
                    header.getStocktakeId(), e.getMessage());
        }
    }

    private String requireUsername(Long userId) {
        if (userId == null) {
            throw new ServiceException("库存盘点流程处理人不能为空");
        }
        R<LoginUser> result = remoteUserService.getUserInfoById(userId, SecurityConstants.INNER);
        LoginUser loginUser = result == null ? null : result.getData();
        if (loginUser == null || loginUser.getUsername() == null || loginUser.getUsername().isBlank()) {
            throw new ServiceException("无法解析库存盘点处理人 username: " + userId);
        }
        return loginUser.getUsername().trim();
    }

    private String resolveApproverUsername() {
        R<List<String>> result = remoteUserService.listUsernamesByRoleKey("admin", SecurityConstants.INNER);
        if (result != null && result.getData() != null && !result.getData().isEmpty()) {
            return result.getData().get(0);
        }
        String current = SecurityUtils.getUsername();
        if (current == null || current.isBlank()) {
            throw new ServiceException("无法解析库存盘点审批人 username");
        }
        return current.trim();
    }

    /**
     * 从工作流 /instance/start 响应中提取 processInstanceId。
     * 支持响应结构：{code:200, data:{processInstanceId:"..."}} 或 {processInstanceId:"..."}
     */
    @SuppressWarnings("unchecked")
    private String extractProcessInstanceId(Map response) {
        if (response == null) {
            return null;
        }
        Object data = response.get("data");
        if (data instanceof Map) {
            Object pid = ((Map<Object, Object>) data).get("processInstanceId");
            return pid == null ? null : String.valueOf(pid);
        }
        Object pid = response.get("processInstanceId");
        return pid == null ? null : String.valueOf(pid);
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
        // 工作流内部回调由 @InnerAuth 保护，不携带前台登录用户；业务单据和门店归属
        // 仍在下方创建流程中校验，不能把该内部调用误判为普通用户无授权。
        if (StringUtils.isBlank(SecurityUtils.getUsername())) {
            return;
        }
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
        // 盲盘保护由任务角色决定：counter 视角且任务未提交时隐藏期望值
        // 管理员也不例外，防止管理员身份泄露期望数量
        Long currentUserId = SecurityUtils.getUserId();
        boolean isCounter = header.getCounterUserId() != null
                && header.getCounterUserId().equals(currentUserId);
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
        if (request.getActualQuantity().signum() < 0) {
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
        if (request.getRecountQuantity().signum() < 0) {
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
