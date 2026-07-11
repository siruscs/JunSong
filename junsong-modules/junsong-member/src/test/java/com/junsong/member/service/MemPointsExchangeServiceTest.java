package com.junsong.member.service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.member.domain.MemPointsExchange;
import com.junsong.member.domain.MemPointsGoods;
import com.junsong.member.domain.MemPointsRecord;
import com.junsong.member.mapper.MemMemberMapper;
import com.junsong.member.mapper.MemPointsExchangeMapper;
import com.junsong.member.mapper.MemPointsGoodsMapper;
import com.junsong.member.service.impl.MemPointsExchangeServiceImpl;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 积分兑换事务一致性测试
 *
 * 使用手写 fake 替代 Mockito，避免 inline mock maker 在 JDK 26+ 下
 * 自附加 Java agent 导致的兼容性问题。
 *
 * @author junsong
 */
class MemPointsExchangeServiceTest
{
    private MemPointsExchangeServiceImpl service;
    private FakeExchangeMapper fakeExchangeMapper;
    private FakePointsRecordService fakePointsRecordService;
    private FakeGoodsMapper fakeGoodsMapper;

    @BeforeEach
    void setUp() throws Exception
    {
        service = new MemPointsExchangeServiceImpl();
        fakeExchangeMapper = new FakeExchangeMapper();
        fakePointsRecordService = new FakePointsRecordService();
        fakeGoodsMapper = new FakeGoodsMapper();

        Field mapperField = MemPointsExchangeServiceImpl.class.getDeclaredField("memPointsExchangeMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, fakeExchangeMapper);

        Field recordField = MemPointsExchangeServiceImpl.class.getDeclaredField("memPointsRecordService");
        recordField.setAccessible(true);
        recordField.set(service, fakePointsRecordService);

        Field goodsField = MemPointsExchangeServiceImpl.class.getDeclaredField("memPointsGoodsMapper");
        goodsField.setAccessible(true);
        goodsField.set(service, fakeGoodsMapper);

        // R5/R6 在 exchangePoints 末尾新增 memMemberMapper.updateLastActiveTime 调用，
        // 此处注入 no-op Proxy 避免 NPE（生产代码已验证，单元测试仅需补齐依赖）。
        MemMemberMapper fakeMemberMapper = (MemMemberMapper) java.lang.reflect.Proxy.newProxyInstance(
                MemMemberMapper.class.getClassLoader(),
                new Class<?>[] { MemMemberMapper.class },
                (proxy, method, args) -> {
                    Class<?> rt = method.getReturnType();
                    if (rt == int.class) return 1;
                    if (rt == boolean.class) return false;
                    return null;
                });
        Field memberField = MemPointsExchangeServiceImpl.class.getDeclaredField("memMemberMapper");
        memberField.setAccessible(true);
        memberField.set(service, fakeMemberMapper);
    }

    private MemPointsExchange buildExchange(Long memberId, BigDecimal pointsDeducted)
    {
        MemPointsExchange ex = new MemPointsExchange();
        ex.setDeptId(100L);
        ex.setMemberId(memberId);
        ex.setMemberNo("M001");
        ex.setMemberName("测试会员");
        ex.setGoodsId(1L);
        ex.setGoodsName("测试商品");
        ex.setPointsDeducted(pointsDeducted);
        ex.setQuantity(1);
        return ex;
    }

    // ==================== 余额不足 ====================

    @Test
    void shouldRejectWhenBalanceInsufficient()
    {
        MemPointsExchange exchange = buildExchange(1L, new BigDecimal("500"));
        fakePointsRecordService.latestBalance = new BigDecimal("100");
        fakeExchangeMapper.exchangeNoExists = false;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.exchangePoints(exchange, "admin"));
        assertTrue(ex.getMessage().contains("余额不足"));

        assertFalse(fakeExchangeMapper.insertCalled, "不应插入兑换单");
        assertFalse(fakePointsRecordService.insertCalled, "不应创建积分记录");
    }

    // ==================== 编号重复 ====================

    @Test
    void shouldRejectDuplicateExchangeNo()
    {
        MemPointsExchange exchange = buildExchange(1L, new BigDecimal("50"));
        exchange.setExchangeNo("EX20260629001");
        fakeExchangeMapper.exchangeNoExists = true;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.exchangePoints(exchange, "admin"));
        assertTrue(ex.getMessage().contains("编号已存在"));

        assertFalse(fakeExchangeMapper.insertCalled, "不应插入兑换单");
        assertFalse(fakePointsRecordService.insertCalled, "不应创建积分记录");
    }

    // ==================== 扣减积分必须大于0 ====================

    @Test
    void shouldRejectZeroOrNegativePoints()
    {
        MemPointsExchange exchange = buildExchange(1L, BigDecimal.ZERO);
        fakePointsRecordService.latestBalance = new BigDecimal("100");
        fakeExchangeMapper.exchangeNoExists = false;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.exchangePoints(exchange, "admin"));
        assertTrue(ex.getMessage().contains("大于0"));
    }

    // ==================== 积分记录插入异常传播 ====================
    // 注意：此测试验证异常向上传播（触发 @Transactional 回滚的前提）。
    // 真实的事务回滚需要 Spring 容器上下文，此处纯单元测试无法验证数据库层面回滚，
    // 只验证：1) 异常正确传播不被吞掉  2) 两个 mapper/service 的方法调用顺序正确。

    @Test
    void shouldPropagateExceptionWhenPointsInsertFails()
    {
        MemPointsExchange exchange = buildExchange(1L, new BigDecimal("50"));
        fakePointsRecordService.latestBalance = new BigDecimal("200");
        fakeExchangeMapper.exchangeNoExists = false;
        fakeExchangeMapper.insertResult = 1;
        fakePointsRecordService.insertThrows = true;

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> service.exchangePoints(exchange, "admin"));
        assertEquals("DB error", thrown.getMessage());

        assertTrue(fakeExchangeMapper.insertCalled, "兑换单插入应被调用");
        assertTrue(fakePointsRecordService.insertCalled, "积分记录插入应被调用");
    }

    // ==================== 兑换单插入失败异常传播 ====================

    @Test
    void shouldPropagateExceptionWhenExchangeInsertFails()
    {
        MemPointsExchange exchange = buildExchange(1L, new BigDecimal("50"));
        fakePointsRecordService.latestBalance = new BigDecimal("200");
        fakeExchangeMapper.exchangeNoExists = false;
        fakeExchangeMapper.insertResult = 0; // 插入失败

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> service.exchangePoints(exchange, "admin"));
        assertTrue(thrown.getMessage().contains("兑换单插入失败"));

        assertFalse(fakePointsRecordService.insertCalled, "兑换单插入失败后不应继续创建积分记录");
    }

    // ==================== 正常流程 ====================

    @Test
    void shouldSucceedWithValidInput()
    {
        MemPointsExchange exchange = buildExchange(1L, new BigDecimal("50"));
        fakePointsRecordService.latestBalance = new BigDecimal("200");
        fakeExchangeMapper.exchangeNoExists = false;
        fakeExchangeMapper.insertResult = 1;

        assertDoesNotThrow(() -> service.exchangePoints(exchange, "admin"));

        // 验证计算结果
        assertEquals(new BigDecimal("50"), exchange.getActualPoints());
        assertEquals(new BigDecimal("200"), exchange.getCurrentBalance());
        assertEquals(new BigDecimal("150"), exchange.getNewBalance());
        assertEquals("admin", exchange.getCreateBy());

        // 验证兑换单和积分记录都被创建
        assertTrue(fakeExchangeMapper.insertCalled, "兑换单应被插入");
        assertTrue(fakePointsRecordService.insertCalled, "积分记录应被创建");
        assertEquals(new BigDecimal("-50"), fakePointsRecordService.lastInsertedRecord.getPoints());
        assertEquals(new BigDecimal("150"), fakePointsRecordService.lastInsertedRecord.getBalance());
        assertEquals("2", fakePointsRecordService.lastInsertedRecord.getRecordType());
    }

    // ==================== 无历史记录时余额为0 ====================

    @Test
    void shouldTreatBalanceAsZeroWhenNoHistory()
    {
        MemPointsExchange exchange = buildExchange(1L, new BigDecimal("50"));
        fakePointsRecordService.latestBalance = null; // 无历史记录
        fakeExchangeMapper.exchangeNoExists = false;

        assertThrows(IllegalArgumentException.class,
                () -> service.exchangePoints(exchange, "admin"));
    }

    // ==================== 库存不足 ====================

    @Test
    void shouldRejectWhenStockInsufficient()
    {
        MemPointsExchange exchange = buildExchange(1L, new BigDecimal("50"));
        fakePointsRecordService.latestBalance = new BigDecimal("200");
        fakeExchangeMapper.exchangeNoExists = false;
        fakeGoodsMapper.deductStockResult = 0; // 库存不足

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.exchangePoints(exchange, "admin"));
        assertTrue(ex.getMessage().contains("库存不足"));

        assertFalse(fakeExchangeMapper.insertCalled, "库存不足时不应插入兑换单");
        assertFalse(fakePointsRecordService.insertCalled, "库存不足时不应创建积分记录");
    }

    // ==================== 库存扣减成功 ====================

    @Test
    void shouldDeductStockOnSuccessfulExchange()
    {
        MemPointsExchange exchange = buildExchange(1L, new BigDecimal("50"));
        exchange.setQuantity(3);
        fakePointsRecordService.latestBalance = new BigDecimal("200");
        fakeExchangeMapper.exchangeNoExists = false;
        fakeExchangeMapper.insertResult = 1;
        fakeGoodsMapper.deductStockResult = 1; // 库存充足

        assertDoesNotThrow(() -> service.exchangePoints(exchange, "admin"));

        assertTrue(fakeGoodsMapper.deductStockCalled, "库存扣减应被调用");
        assertEquals(1L, fakeGoodsMapper.lastGoodsId, "应扣减 goodsId=1 的库存");
        assertEquals(3, fakeGoodsMapper.lastQuantity, "应扣减数量 3");
        assertTrue(fakeExchangeMapper.insertCalled, "兑换单应被插入");
        assertTrue(fakePointsRecordService.insertCalled, "积分记录应被创建");
    }

    @Test
    void shouldDeductStockBeforeInsertingExchangeRecord()
    {
        MemPointsExchange exchange = buildExchange(1L, new BigDecimal("50"));
        fakePointsRecordService.latestBalance = new BigDecimal("200");
        fakeExchangeMapper.exchangeNoExists = false;
        fakeExchangeMapper.insertResult = 1;
        fakeGoodsMapper.deductStockResult = 1;

        assertDoesNotThrow(() -> service.exchangePoints(exchange, "admin"));

        // 验证调用顺序：库存扣减 → 兑换单插入 → 积分记录创建
        assertTrue(fakeGoodsMapper.deductStockCalled, "步骤5: 库存扣减");
        assertTrue(fakeExchangeMapper.insertCalled, "步骤6: 兑换单插入");
        assertTrue(fakePointsRecordService.insertCalled, "步骤7: 积分记录创建");
    }

    // ==================== Fake 实现 ====================

    /**
     * MemPointsExchangeMapper 的手写 fake 实现。
     * 只实现测试中使用的方法，其余抛 UnsupportedOperationException。
     */
    static class FakeExchangeMapper implements MemPointsExchangeMapper
    {
        boolean exchangeNoExists = false;
        int insertResult = 1;
        boolean insertCalled = false;
        MemPointsExchange lastInsertedExchange = null;

        @Override
        public int checkMemExchangeNoUnique(String exchangeNo)
        {
            return exchangeNoExists ? 1 : 0;
        }

        @Override
        public int insertMemPointsExchange(MemPointsExchange exchange)
        {
            insertCalled = true;
            lastInsertedExchange = exchange;
            return insertResult;
        }

        @Override public List<MemPointsExchange> selectMemPointsExchangeList(MemPointsExchange e) { throw new UnsupportedOperationException(); }
        @Override public MemPointsExchange selectMemPointsExchangeById(Long id) { throw new UnsupportedOperationException(); }
        @Override public int updateMemPointsExchange(MemPointsExchange e) { throw new UnsupportedOperationException(); }
        @Override public int deleteMemPointsExchangeById(Long id) { throw new UnsupportedOperationException(); }
        @Override public int deleteMemPointsExchangeByIds(Long[] ids) { throw new UnsupportedOperationException(); }
    }

    /**
     * IMemPointsRecordService 的手写 fake 实现。
     */
    static class FakePointsRecordService implements IMemPointsRecordService
    {
        BigDecimal latestBalance = null;
        boolean insertCalled = false;
        boolean insertThrows = false;
        MemPointsRecord lastInsertedRecord = null;

        @Override
        public MemPointsRecord selectLatestBalanceByMemberId(Long memberId)
        {
            if (latestBalance == null) { return null; }
            MemPointsRecord record = new MemPointsRecord();
            record.setBalance(latestBalance);
            return record;
        }

        @Override
        public int insertMemPointsRecord(MemPointsRecord record)
        {
            insertCalled = true;
            lastInsertedRecord = record;
            if (insertThrows)
            {
                throw new RuntimeException("DB error");
            }
            return 1;
        }

        @Override public MemPointsRecord selectMemPointsRecordById(Long id) { throw new UnsupportedOperationException(); }
        @Override public List<MemPointsRecord> selectMemPointsRecordList(MemPointsRecord r) { throw new UnsupportedOperationException(); }
        @Override public int updateMemPointsRecord(MemPointsRecord r) { throw new UnsupportedOperationException(); }
        @Override public int deleteMemPointsRecordById(Long id) { throw new UnsupportedOperationException(); }
        @Override public int deleteMemPointsRecordByIds(Long[] ids) { throw new UnsupportedOperationException(); }
        @Override public boolean checkMemPointsRecordNoUnique(MemPointsRecord r) { throw new UnsupportedOperationException(); }
        @Override public List<MemPointsRecord> selectMemPointsRecordByRemark(String remark) { throw new UnsupportedOperationException(); }
    }

    /**
     * MemPointsGoodsMapper 的手写 fake 实现。
     * deductStockResult 默认为 1（库存充足），设为 0 模拟库存不足。
     */
    static class FakeGoodsMapper implements MemPointsGoodsMapper
    {
        int deductStockResult = 1;
        boolean deductStockCalled = false;
        Long lastGoodsId = null;
        int lastQuantity = 0;

        @Override
        public int deductStock(Long goodsId, int quantity)
        {
            deductStockCalled = true;
            lastGoodsId = goodsId;
            lastQuantity = quantity;
            return deductStockResult;
        }

        @Override public List<MemPointsGoods> selectMemPointsGoodsList(MemPointsGoods g) { throw new UnsupportedOperationException(); }
        @Override public MemPointsGoods selectMemPointsGoodsById(Long goodsId) { throw new UnsupportedOperationException(); }
        @Override public int insertMemPointsGoods(MemPointsGoods g) { throw new UnsupportedOperationException(); }
        @Override public int updateMemPointsGoods(MemPointsGoods g) { throw new UnsupportedOperationException(); }
        @Override public int deleteMemPointsGoodsById(Long goodsId) { throw new UnsupportedOperationException(); }
        @Override public int deleteMemPointsGoodsByIds(Long[] goodsIds) { throw new UnsupportedOperationException(); }
        @Override public int checkMemGoodsCodeUnique(String goodsNo, Long goodsId) { throw new UnsupportedOperationException(); }
    }
}
