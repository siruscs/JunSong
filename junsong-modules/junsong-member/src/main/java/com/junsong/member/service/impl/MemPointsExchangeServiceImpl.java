package com.junsong.member.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.mapper.MemMemberMapper;
import com.junsong.member.mapper.MemPointsExchangeMapper;
import com.junsong.member.mapper.MemPointsGoodsMapper;
import com.junsong.member.domain.MemPointsExchange;
import com.junsong.member.domain.MemPointsRecord;
import com.junsong.member.service.IMemPointsExchangeService;
import com.junsong.member.service.IMemPointsRecordService;
import com.junsong.common.datascope.annotation.DataScope;

/**
 * 积分兑换Service业务层处理
 */
@Service
public class MemPointsExchangeServiceImpl implements IMemPointsExchangeService {

    @Autowired
    private MemPointsExchangeMapper memPointsExchangeMapper;

    @Autowired
    private MemPointsGoodsMapper memPointsGoodsMapper;

    @Autowired
    private IMemPointsRecordService memPointsRecordService;

    @Autowired
    private MemMemberMapper memMemberMapper;

    /**
     * 查询积分兑换
     *
     * @param id 积分兑换ID
     * @return 积分兑换
     */
    @Override
    @DataScope(deptAlias = "mem_points_exchange")
    public MemPointsExchange selectMemPointsExchangeById(Long id) {
        return memPointsExchangeMapper.selectMemPointsExchangeById(id);
    }

    /**
     * 查询积分兑换列表
     *
     * @param memPointsExchange 积分兑换
     * @return 积分兑换
     */
    @Override
    @DataScope(deptAlias = "mem_points_exchange")
    public List<MemPointsExchange> selectMemPointsExchangeList(MemPointsExchange memPointsExchange) {
        return memPointsExchangeMapper.selectMemPointsExchangeList(memPointsExchange);
    }

    /**
     * 新增积分兑换
     *
     * @param memPointsExchange 积分兑换
     * @return 结果
     */
    @Override
    @Transactional
    public int insertMemPointsExchange(MemPointsExchange memPointsExchange) {
        return memPointsExchangeMapper.insertMemPointsExchange(memPointsExchange);
    }

    /**
     * 积分兑换事务操作：编号生成 → 余额校验 → 库存扣减 → 兑换单插入 → 积分扣减记录创建
     * 任何一步失败整体回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exchangePoints(MemPointsExchange exchange, String operator)
    {
        // 1. 生成兑换编号
        if (exchange.getExchangeNo() == null || exchange.getExchangeNo().isEmpty())
        {
            exchange.setExchangeNo("EX" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()));
        }

        // 2. 编号唯一性校验
        if (!checkMemPointsExchangeNoUnique(exchange))
        {
            throw new IllegalArgumentException("兑换编号已存在: " + exchange.getExchangeNo());
        }

        // 3. 读取当前积分余额
        BigDecimal currentBalance = BigDecimal.ZERO;
        MemPointsRecord latest = memPointsRecordService.selectLatestBalanceByMemberId(exchange.getMemberId());
        if (latest != null && latest.getBalance() != null)
        {
            currentBalance = latest.getBalance();
        }

        // 4. 计算实际扣减积分
        BigDecimal deductPoints = exchange.getPointsDeducted() != null ? exchange.getPointsDeducted() : BigDecimal.ZERO;
        if (deductPoints.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new IllegalArgumentException("扣减积分必须大于0");
        }
        if (currentBalance.compareTo(deductPoints) < 0)
        {
            throw new IllegalArgumentException("积分余额不足，当前余额: " + currentBalance);
        }

        BigDecimal actualDeduct = deductPoints.min(currentBalance);
        BigDecimal newBalance = currentBalance.subtract(actualDeduct);

        exchange.setActualPoints(actualDeduct);
        exchange.setCurrentBalance(currentBalance);
        exchange.setNewBalance(newBalance);
        exchange.setCreateBy(operator);

        // 5. 库存校验与扣减（条件更新：stock >= quantity 才成功）
        int quantity = exchange.getQuantity() != null ? exchange.getQuantity() : 1;
        int stockRows = memPointsGoodsMapper.deductStock(exchange.getGoodsId(), quantity);
        if (stockRows <= 0)
        {
            throw new IllegalArgumentException("商品库存不足或商品不存在");
        }

        // 6. 插入兑换单
        int rows = memPointsExchangeMapper.insertMemPointsExchange(exchange);
        if (rows <= 0)
        {
            throw new RuntimeException("兑换单插入失败");
        }

        // 7. 创建积分扣减记录（同一事务内）
        MemPointsRecord pointsRecord = new MemPointsRecord();
        pointsRecord.setDeptId(exchange.getDeptId());
        pointsRecord.setMemberId(exchange.getMemberId());
        pointsRecord.setMemberNo(exchange.getMemberNo());
        pointsRecord.setMemberName(exchange.getMemberName());
        pointsRecord.setRecordType("2");
        pointsRecord.setPoints(actualDeduct.negate());
        pointsRecord.setBalance(newBalance);
        pointsRecord.setRemark("兑换扣减-" + exchange.getExchangeNo());
        pointsRecord.setCreateBy(operator);
        memPointsRecordService.insertMemPointsRecord(pointsRecord);

        // 8. 更新会员最后活跃时间（兑换不写成长流水，不触发等级检查）
        memMemberMapper.updateLastActiveTime(exchange.getMemberId());
    }

    /**
     * 修改积分兑换
     *
     * @param memPointsExchange 积分兑换
     * @return 结果
     */
    @Override
    @Transactional
    public int updateMemPointsExchange(MemPointsExchange memPointsExchange) {
        return memPointsExchangeMapper.updateMemPointsExchange(memPointsExchange);
    }

    /**
     * 删除积分兑换
     *
     * @param id 积分兑换ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemPointsExchangeById(Long id) {
        return memPointsExchangeMapper.deleteMemPointsExchangeById(id);
    }

    /**
     * 批量删除积分兑换
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemPointsExchangeByIds(Long[] ids) {
        return memPointsExchangeMapper.deleteMemPointsExchangeByIds(ids);
    }

    /**
     * 校验积分兑换编号是否唯一
     *
     * @param memPointsExchange 积分兑换
     * @return 结果
     */
    @Override
    public boolean checkMemPointsExchangeNoUnique(MemPointsExchange memPointsExchange) {
        int count = memPointsExchangeMapper.checkMemExchangeNoUnique(memPointsExchange.getExchangeNo());
        return count == 0;
    }
}
