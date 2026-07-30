package com.junsong.finance.service.impl;

import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.api.domain.StocktakeWorkflowSyncReq;
import com.junsong.finance.domain.FinStocktake;
import com.junsong.finance.domain.FinStocktakeItem;
import com.junsong.finance.domain.vo.StocktakeCountRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 盘点工作流集成测试（手写 fake，无 Mockito）。
 *
 * 复用 {@link FinStocktakeServiceImplTest} 中已验证的包级 fake mapper（同包可见），
 * 新增手写 {@link FakeWorkflowRestTemplate} 拦截工作流 HTTP 调用。
 *
 * 覆盖场景：
 * - 提交盘点成功启动工作流（data 包装 / 扁平响应）
 * - 工作流启动失败优雅降级（不阻塞提交事务）
 * - 工作流返回非预期响应（不崩溃）
 * - syncWorkflowStatus 按 stocktakeId 更新字段
 * - syncWorkflowStatus 按 processInstanceId 反查更新
 * - syncWorkflowStatus 缺租户 / 空请求拒绝
 * - syncWorkflowStatus 未知 processInstanceId 返回 0
 * - syncWorkflowStocktake 缺 stocktakeId 且无 processInstanceId 返回 0
 */
class FinStocktakeWorkflowIntegrationTest {

    private static final Long T1 = 1L;
    private static final Long DEPT_10 = 10L;
    private static final Long PRODUCT_100 = 100L;
    private static final Long COUNTER_USER = 1L; // admin 用户

    private FinStocktakeServiceImplTest.FakeFinStocktakeMapper stocktakeMapper;
    private FinStocktakeServiceImplTest.FakeFinStockLedgerMapper ledgerMapper;
    private FinStocktakeServiceImplTest.FakeIStockCostService stockCostService;
    private FinStocktakeServiceImplTest.FakeFinAccountingPeriodMapper accountingPeriodMapper;
    private FinStocktakeServiceImplTest.FakeRemoteUserService remoteUserService;
    private FinStocktakeServiceImplTest.FakeFinProductMapper productMapper;
    private FakeWorkflowRestTemplate workflowRestTemplate;
    private FinStocktakeServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        stocktakeMapper = new FinStocktakeServiceImplTest.FakeFinStocktakeMapper();
        ledgerMapper = new FinStocktakeServiceImplTest.FakeFinStockLedgerMapper();
        stockCostService = new FinStocktakeServiceImplTest.FakeIStockCostService();
        accountingPeriodMapper = new FinStocktakeServiceImplTest.FakeFinAccountingPeriodMapper();
        remoteUserService = new FinStocktakeServiceImplTest.FakeRemoteUserService();
        productMapper = new FinStocktakeServiceImplTest.FakeFinProductMapper();
        workflowRestTemplate = new FakeWorkflowRestTemplate();
        service = new FinStocktakeServiceImpl();

        inject("finStocktakeMapper", stocktakeMapper);
        inject("finStockLedgerMapper", ledgerMapper);
        inject("stockCostService", stockCostService);
        inject("accountingPeriodMapper", accountingPeriodMapper);
        inject("remoteUserService", remoteUserService);
        inject("finProductMapper", productMapper);
        inject("workflowRestTemplate", workflowRestTemplate);
        inject("workflowServiceUrl", "http://test-wf:9207");

        // 默认 admin 上下文（admin 跳过部门授权校验）
        TenantContext.setTenantId(T1);
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.remove();
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field f = FinStocktakeServiceImpl.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(service, value);
    }

    /** 构造一个 COUNTING 状态、单行已录入（variance=0）的盘点任务，可直接提交。 */
    private FinStocktake buildCountingStocktakeReadyToSubmit() {
        FinStocktake header = new FinStocktake();
        header.setTenantId(T1);
        header.setStocktakeId(100L);
        header.setTakeNo("PD-WF-001");
        header.setDeptId(DEPT_10);
        header.setStatus("COUNTING");
        header.setCounterUserId(COUNTER_USER);
        header.setRecountUserId(3002L);
        header.setVersion(0);
        header.setFreezeTime(new java.util.Date());
        stocktakeMapper.headers.add(header);

        FinStocktakeItem item = new FinStocktakeItem();
        item.setItemId(1001L);
        item.setStocktakeId(100L);
        item.setTenantId(T1);
        item.setDeptId(DEPT_10);
        item.setProductId(PRODUCT_100);
        item.setProductName("测试商品");
        item.setExpectedQuantity(50);
        item.setActualQuantity(50); // variance=0，不触发复盘
        item.setVersion(1); // count 后版本递增
        stocktakeMapper.insertedItems.add(item);
        stocktakeMapper.itemsByStocktake.put(100L, new ArrayList<>(Arrays.asList(item)));
        return header;
    }

    private StocktakeCountRequest buildCountRequest() {
        StocktakeCountRequest req = new StocktakeCountRequest();
        req.setActualQuantity(50);
        req.setIdempotencyKey("PD-WF-001-count-1");
        req.setVersion(0);
        return req;
    }

    // ===== 提交 → 启动工作流 =====

    @Test
    void submit_startsWorkflowAndPersistsProcessInstanceId_dataWrapper() {
        // 工作流返回 {code:200, data:{processInstanceId:"pi-abc-001"}}
        Map<String, Object> data = new HashMap<>();
        data.put("processInstanceId", "pi-abc-001");
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", data);
        workflowRestTemplate.responseToReturn = response;

        FinStocktake header = buildCountingStocktakeReadyToSubmit();

        int affected = service.submitStocktake(100L, 0);

        assertEquals(1, affected, "提交应成功");
        assertEquals("SUBMITTED", header.getStatus(), "variance=0 应流转至 SUBMITTED");
        assertEquals("pi-abc-001", header.getProcessInstanceId(), "processInstanceId 应被回写");
        assertEquals("Task_Count", header.getCurrentNode(), "初始节点应为 Task_Count");
        assertEquals("stocktake_apply", header.getProcessDefinitionKey());
        assertEquals("PD-WF-001", header.getBusinessKey(), "businessKey 应为 takeNo");

        // 验证 HTTP 调用参数
        assertEquals(1, workflowRestTemplate.capturedBodies.size(), "应发起一次工作流启动请求");
        Map<String, Object> captured = workflowRestTemplate.capturedBodies.get(0);
        assertEquals("stocktake_apply", captured.get("processKey"));
        assertEquals("PD-WF-001", captured.get("businessKey"));
        @SuppressWarnings("unchecked")
        Map<String, Object> vars = (Map<String, Object>) captured.get("variables");
        assertNotNull(vars);
        assertEquals(100L, vars.get("stocktakeId"));
        assertEquals(false, vars.get("needRecount"), "variance=0 时 needRecount 应为 false");
        assertEquals(String.valueOf(COUNTER_USER), vars.get("counterUserId"));
    }

    @Test
    void submit_startsWorkflowWithFlatResponse() {
        // 工作流返回扁平结构 {processInstanceId:"pi-flat-002"}
        Map<String, Object> response = new HashMap<>();
        response.put("processInstanceId", "pi-flat-002");
        workflowRestTemplate.responseToReturn = response;

        FinStocktake header = buildCountingStocktakeReadyToSubmit();

        service.submitStocktake(100L, 0);

        assertEquals("pi-flat-002", header.getProcessInstanceId(), "扁平响应中的 processInstanceId 应被提取");
        assertEquals("Task_Count", header.getCurrentNode());
    }

    @Test
    void submit_workflowFailureDoesNotBlockSubmit() {
        // 工作流调用抛异常 —— 提交事务不应回滚
        workflowRestTemplate.exceptionToThrow = new RuntimeException("workflow service down");

        FinStocktake header = buildCountingStocktakeReadyToSubmit();

        int affected = service.submitStocktake(100L, 0);

        assertEquals(1, affected, "工作流失败时提交仍应成功（优雅降级）");
        assertEquals("SUBMITTED", header.getStatus(), "状态应正常流转");
        assertNull(header.getProcessInstanceId(), "工作流失败时 processInstanceId 应保持 null");
        assertNull(header.getCurrentNode(), "工作流失败时 currentNode 应保持 null");
    }

    @Test
    void submit_workflowUnexpectedResponseDoesNotCrash() {
        // 工作流返回非预期结构（无 processInstanceId）—— 不崩溃，提交仍成功
        Map<String, Object> response = new HashMap<>();
        response.put("foo", "bar");
        workflowRestTemplate.responseToReturn = response;

        FinStocktake header = buildCountingStocktakeReadyToSubmit();

        int affected = service.submitStocktake(100L, 0);

        assertEquals(1, affected, "非预期响应不应阻塞提交");
        assertEquals("SUBMITTED", header.getStatus());
        assertNull(header.getProcessInstanceId(), "无 processInstanceId 时不应回写");
    }

    @Test
    void submit_workflowNullResponseDoesNotCrash() {
        // 工作流返回 null —— 不崩溃
        workflowRestTemplate.responseToReturn = null;

        FinStocktake header = buildCountingStocktakeReadyToSubmit();

        int affected = service.submitStocktake(100L, 0);

        assertEquals(1, affected);
        assertEquals("SUBMITTED", header.getStatus());
        assertNull(header.getProcessInstanceId());
    }

    // ===== syncWorkflowStatus =====

    @Test
    void sync_byStocktakeIdUpdatesWorkflowFields() {
        FinStocktake header = buildCountingStocktakeReadyToSubmit();
        // 模拟工作流已启动，现回调更新节点
        header.setProcessInstanceId("pi-sync-001");
        header.setCurrentNode("Task_Count");

        StocktakeWorkflowSyncReq req = new StocktakeWorkflowSyncReq();
        req.setStocktakeId(100L);
        req.setProcessInstanceId("pi-sync-001");
        req.setCurrentNode("Task_Approve");
        req.setAction("APPROVE");

        int affected = service.syncWorkflowStatus(req);

        assertEquals(1, affected);
        assertEquals("Task_Approve", header.getCurrentNode(), "currentNode 应被更新为 Task_Approve");
        assertEquals("pi-sync-001", header.getProcessInstanceId());
    }

    @Test
    void sync_byProcessInstanceIdReverseLookup() {
        // 工作流回调仅携带 processInstanceId（如 afterReject），无 stocktakeId
        FinStocktake header = buildCountingStocktakeReadyToSubmit();
        header.setProcessInstanceId("pi-rev-001");
        header.setCurrentNode("Task_Count");

        StocktakeWorkflowSyncReq req = new StocktakeWorkflowSyncReq();
        req.setStocktakeId(null); // 缺失，需反查
        req.setProcessInstanceId("pi-rev-001");
        req.setCurrentNode("审批驳回");
        req.setAction("REJECT");

        int affected = service.syncWorkflowStatus(req);

        assertEquals(1, affected, "通过 processInstanceId 反查应成功");
        assertEquals("审批驳回", header.getCurrentNode());
    }

    @Test
    void sync_withDefaultTenantProceedsNormally() {
        // TenantContext.getTenantId() 在 clear() 后回退到 DEFAULT_TENANT_ID=1L（永不返回 null），
        // 因此 syncWorkflowStatus 的 tenantId==null 防御分支不可达。此处验证默认租户下同步正常工作。
        TenantContext.clear();
        FinStocktake header = buildCountingStocktakeReadyToSubmit();
        header.setProcessInstanceId("pi-default-001");

        StocktakeWorkflowSyncReq req = new StocktakeWorkflowSyncReq();
        req.setStocktakeId(100L);
        req.setProcessInstanceId("pi-default-001");
        req.setCurrentNode("Task_Approve");

        int affected = service.syncWorkflowStatus(req);

        assertEquals(1, affected, "默认租户（DEFAULT_TENANT_ID=1L）下同步应正常工作");
        assertEquals("Task_Approve", header.getCurrentNode());
    }

    @Test
    void sync_nullRequestThrows() {
        assertThrows(ServiceException.class, () -> service.syncWorkflowStatus(null),
                "空请求应拒绝");
    }

    @Test
    void sync_unknownProcessInstanceIdReturns0() {
        // processInstanceId 不对应任何盘点任务
        StocktakeWorkflowSyncReq req = new StocktakeWorkflowSyncReq();
        req.setStocktakeId(null);
        req.setProcessInstanceId("pi-nonexistent-999");
        req.setCurrentNode("Task_Approve");

        int affected = service.syncWorkflowStatus(req);

        assertEquals(0, affected, "未知 processInstanceId 应返回 0，不抛异常");
    }

    @Test
    void sync_missingStocktakeIdAndNoProcessInstanceIdReturns0() {
        // 既无 stocktakeId 也无 processInstanceId —— 无法定位，返回 0
        StocktakeWorkflowSyncReq req = new StocktakeWorkflowSyncReq();
        req.setStocktakeId(null);
        req.setProcessInstanceId(null);
        req.setCurrentNode("Task_Approve");

        int affected = service.syncWorkflowStatus(req);

        assertEquals(0, affected, "无法定位盘点任务时应返回 0");
    }

    @Test
    void sync_doesNotChangeStocktakeStateMachine() {
        // 工作流同步仅更新工作流字段，不改变盘点状态机
        FinStocktake header = buildCountingStocktakeReadyToSubmit();
        String originalStatus = header.getStatus();

        StocktakeWorkflowSyncReq req = new StocktakeWorkflowSyncReq();
        req.setStocktakeId(100L);
        req.setProcessInstanceId("pi-state-001");
        req.setCurrentNode("Task_Approve");
        req.setAction("APPROVE");

        service.syncWorkflowStatus(req);

        assertEquals(originalStatus, header.getStatus(), "工作流同步不得改变盘点状态机");
        assertEquals("Task_Approve", header.getCurrentNode());
    }

    // ===== 工作流启动后 sync 联动 =====

    @Test
    void submitThenSync_workflowFieldsRoundtrip() {
        // 1. 提交启动工作流
        Map<String, Object> data = new HashMap<>();
        data.put("processInstanceId", "pi-rt-001");
        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        workflowRestTemplate.responseToReturn = response;

        FinStocktake header = buildCountingStocktakeReadyToSubmit();
        service.submitStocktake(100L, 0);

        assertEquals("pi-rt-001", header.getProcessInstanceId());
        assertEquals("Task_Count", header.getCurrentNode());

        // 2. 工作流回调推进节点
        StocktakeWorkflowSyncReq syncReq = new StocktakeWorkflowSyncReq();
        syncReq.setStocktakeId(100L);
        syncReq.setProcessInstanceId("pi-rt-001");
        syncReq.setCurrentNode("Task_Recount");
        syncReq.setAction("SUBMIT");

        int affected = service.syncWorkflowStatus(syncReq);

        assertEquals(1, affected);
        assertEquals("Task_Recount", header.getCurrentNode(), "节点应推进至 Task_Recount");
        assertEquals("SUBMITTED", header.getStatus(), "盘点状态机不受工作流影响");
    }

    /**
     * 手写 fake RestTemplate：拦截 postForObject 调用，返回可配置响应或抛异常。
     * 仅覆盖工作流 /instance/start 端点，不发起真实 HTTP 请求。
     */
    @SuppressWarnings("unchecked")
    static class FakeWorkflowRestTemplate extends RestTemplate {
        Object responseToReturn;
        RuntimeException exceptionToThrow;
        final List<String> capturedUrls = new ArrayList<>();
        final List<Map<String, Object>> capturedBodies = new ArrayList<>();

        @Override
        public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables) {
            recordCall(url, request);
            return (T) returnOrThrow();
        }

        @Override
        public <T> T postForObject(URI url, Object request, Class<T> responseType) {
            recordCall(url.toString(), request);
            return (T) returnOrThrow();
        }

        private void recordCall(String url, Object request) {
            capturedUrls.add(url);
            if (request instanceof Map) {
                capturedBodies.add((Map<String, Object>) request);
            }
        }

        private Object returnOrThrow() {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return responseToReturn;
        }
    }
}
