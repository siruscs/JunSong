package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinanceReceivableCollection;
import com.junsong.finance.domain.FinanceReceivableCollectionLog;
import com.junsong.finance.domain.vo.ReceivableCollectionDashboardVO;
import com.junsong.finance.domain.vo.ReceivableCollectionRowVO;
import com.junsong.finance.domain.vo.ReceivableCollectionSyncParams;
import com.junsong.finance.domain.vo.ReceivableCollectionUpdateParams;
import com.junsong.finance.mapper.FinanceReceivableCollectionMapper;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceivableCollectionServiceImplTest {

    private ReceivableCollectionServiceImpl service;
    private RecordingCollectionMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        setupSecurityContext();
        service = new ReceivableCollectionServiceImpl();
        mapper = new RecordingCollectionMapper();
        inject(service, "receivableCollectionMapper", mapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    @Test
    void updateFollow_rejectsEmptyFollowNoteWhenStatusChanges() {
        FinanceReceivableCollection collection = collection(1L, "PENDING", new BigDecimal("800.00"));
        mapper.collections.put(1L, collection);

        ReceivableCollectionUpdateParams params = new ReceivableCollectionUpdateParams();
        params.setCollectionStatus("CONTACTED");

        assertThrows(ServiceException.class, () -> service.updateFollow(1L, params));
        assertEquals(0, mapper.updated.size());
        assertEquals(0, mapper.logs.size());
    }

    @Test
    void updateFollow_requiresNextFollowTimeWhenPromised() {
        FinanceReceivableCollection collection = collection(2L, "CONTACTED", new BigDecimal("800.00"));
        mapper.collections.put(2L, collection);

        ReceivableCollectionUpdateParams params = new ReceivableCollectionUpdateParams();
        params.setCollectionStatus("PROMISED");
        params.setFollowNote("客户承诺本周付款");
        params.setPromisedPayDate(tomorrow());
        params.setPromisedAmount(new BigDecimal("300.00"));

        assertThrows(ServiceException.class, () -> service.updateFollow(2L, params));
        assertEquals(0, mapper.updated.size());
        assertEquals(0, mapper.logs.size());
    }

    @Test
    void updateFollow_recordsPromiseAndNextFollowTime() {
        FinanceReceivableCollection collection = collection(3L, "CONTACTED", new BigDecimal("1000.00"));
        mapper.collections.put(3L, collection);
        Date promisedPayDate = tomorrow();
        Date nextFollowTime = plusHours(2);

        ReceivableCollectionUpdateParams params = new ReceivableCollectionUpdateParams();
        params.setCollectionStatus("PROMISED");
        params.setFollowNote("客户承诺明日先付一半");
        params.setPromisedPayDate(promisedPayDate);
        params.setPromisedAmount(new BigDecimal("500.00"));
        params.setNextFollowTime(nextFollowTime);

        int rows = service.updateFollow(3L, params);

        assertEquals(1, rows);
        assertEquals(1, mapper.updated.size());
        FinanceReceivableCollection update = mapper.updated.get(0);
        assertEquals("PROMISED", update.getCollectionStatus());
        assertEquals(promisedPayDate, update.getPromisedPayDate());
        assertEquals(new BigDecimal("500.00"), update.getPromisedAmount());
        assertEquals(nextFollowTime, update.getNextFollowTime());
        assertNotNull(update.getLastFollowTime());
        assertEquals(1, update.getFollowCount());
        assertEquals(1, mapper.logs.size());
        assertEquals("PROMISED", mapper.logs.get(0).getNewStatus());
        assertTrue(mapper.refreshCalled);
    }

    @Test
    void syncFromReceivables_createsCollectionForUnpaidSaleOnlyOnce() {
        ReceivableCollectionRowVO unpaid = unpaidSale(10L, new BigDecimal("1200.00"), new BigDecimal("300.00"), 18);
        ReceivableCollectionRowVO duplicate = unpaidSale(11L, new BigDecimal("900.00"), new BigDecimal("100.00"), 5);
        mapper.unpaidRows.add(unpaid);
        mapper.unpaidRows.add(duplicate);
        mapper.collectionsBySaleId.put(11L, collection(99L, "PENDING", new BigDecimal("800.00")));

        int count = service.syncFromReceivables(new ReceivableCollectionSyncParams());

        assertEquals(1, count);
        assertEquals(1, mapper.inserted.size());
        FinanceReceivableCollection inserted = mapper.inserted.get(0);
        assertEquals(10L, inserted.getSaleId());
        assertEquals("AGE_15_30", inserted.getAgeBucket());
        assertEquals("HIGH", inserted.getPriorityLevel());
        assertEquals("PENDING", inserted.getCollectionStatus());
    }

    @Test
    void getDashboard_groupsAgeBucketsAndOverduePromises() {
        ReceivableCollectionRowVO risk = dashboardRow(1L, "PENDING", "AGE_30_PLUS", null, null);
        ReceivableCollectionRowVO overduePromise = dashboardRow(2L, "PROMISED", "AGE_8_14", yesterday(), plusHours(2));
        overduePromise.setPromisedAmount(new BigDecimal("600.00"));
        ReceivableCollectionRowVO today = dashboardRow(3L, "CONTACTED", "AGE_0_7", null, plusHours(3));
        mapper.dashboardRows.add(risk);
        mapper.dashboardRows.add(overduePromise);
        mapper.dashboardRows.add(today);

        ReceivableCollectionDashboardVO dashboard = service.getDashboard(new ReceivableCollectionSyncParams());

        assertEquals(1L, dashboard.getSummary().getAge30PlusCount());
        assertEquals(1L, dashboard.getSummary().getPromisedCount());
        assertEquals(1L, dashboard.getSummary().getOverduePromiseCount());
        assertEquals(new BigDecimal("600.00"), dashboard.getSummary().getPromisedAmount());
        assertEquals(1, dashboard.getHighRiskReceivables().size());
        assertEquals(1, dashboard.getOverduePromises().size());
        assertEquals(2, dashboard.getTodayFollowUps().size());
    }

    private static void setupSecurityContext() {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(1L);
        loginUser.setUsername("admin");
        loginUser.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static FinanceReceivableCollection collection(Long id, String status, BigDecimal unpaidAmount) {
        FinanceReceivableCollection collection = new FinanceReceivableCollection();
        collection.setCollectionId(id);
        collection.setSaleId(id);
        collection.setCollectionStatus(status);
        collection.setSaleAmount(unpaidAmount);
        collection.setPaidAmount(BigDecimal.ZERO);
        collection.setUnpaidAmount(unpaidAmount);
        collection.setFollowCount(0);
        return collection;
    }

    private static ReceivableCollectionRowVO unpaidSale(Long saleId, BigDecimal saleAmount, BigDecimal paidAmount, int ageDays) {
        ReceivableCollectionRowVO row = new ReceivableCollectionRowVO();
        row.setSaleId(saleId);
        row.setSaleNo("XS" + saleId);
        row.setDeptId(100L);
        row.setCustomerName("客户" + saleId);
        row.setSaleAmount(saleAmount);
        row.setPaidAmount(paidAmount);
        row.setUnpaidAmount(saleAmount.subtract(paidAmount));
        row.setAgeDays(ageDays);
        return row;
    }

    private static ReceivableCollectionRowVO dashboardRow(Long id, String status, String ageBucket, Date promisedPayDate, Date nextFollowTime) {
        ReceivableCollectionRowVO row = new ReceivableCollectionRowVO();
        row.setCollectionId(id);
        row.setSaleId(id);
        row.setCollectionStatus(status);
        row.setAgeBucket(ageBucket);
        row.setUnpaidAmount(new BigDecimal("100.00"));
        row.setPaidAmount(new BigDecimal("50.00"));
        row.setPromisedPayDate(promisedPayDate);
        row.setNextFollowTime(nextFollowTime);
        return row;
    }

    private static Date tomorrow() {
        return add(Calendar.DATE, 1);
    }

    private static Date yesterday() {
        return add(Calendar.DATE, -1);
    }

    private static Date plusHours(int hours) {
        return add(Calendar.HOUR_OF_DAY, hours);
    }

    private static Date add(int field, int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(field, amount);
        return calendar.getTime();
    }

    static class RecordingCollectionMapper implements FinanceReceivableCollectionMapper {
        final Map<Long, FinanceReceivableCollection> collections = new HashMap<>();
        final Map<Long, FinanceReceivableCollection> collectionsBySaleId = new HashMap<>();
        final List<FinanceReceivableCollection> updated = new ArrayList<>();
        final List<FinanceReceivableCollection> inserted = new ArrayList<>();
        final List<FinanceReceivableCollectionLog> logs = new ArrayList<>();
        final List<ReceivableCollectionRowVO> unpaidRows = new ArrayList<>();
        final List<ReceivableCollectionRowVO> dashboardRows = new ArrayList<>();
        boolean refreshCalled;

        @Override
        public FinanceReceivableCollection selectById(Long collectionId) {
            return collections.get(collectionId);
        }

        @Override
        public FinanceReceivableCollection selectBySaleId(Long saleId) {
            return collectionsBySaleId.get(saleId);
        }

        @Override
        public List<ReceivableCollectionRowVO> selectDashboardRows(ReceivableCollectionSyncParams params) {
            return dashboardRows;
        }

        @Override
        public List<ReceivableCollectionRowVO> selectList(ReceivableCollectionSyncParams params) {
            return dashboardRows;
        }

        @Override
        public List<ReceivableCollectionRowVO> selectUnpaidSalesForSync(ReceivableCollectionSyncParams params) {
            return unpaidRows;
        }

        @Override
        public int insertCollection(FinanceReceivableCollection collection) {
            inserted.add(collection);
            collectionsBySaleId.put(collection.getSaleId(), collection);
            return 1;
        }

        @Override
        public int updateCollection(FinanceReceivableCollection collection) {
            updated.add(collection);
            return 1;
        }

        @Override
        public int insertLog(FinanceReceivableCollectionLog log) {
            logs.add(log);
            return 1;
        }

        @Override
        public int refreshCollectionAmounts(Long collectionId) {
            refreshCalled = true;
            return 1;
        }
    }
}
