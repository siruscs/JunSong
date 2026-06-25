package com.junsong.member.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.member.domain.MemMember;
import com.junsong.member.domain.MemSeckillClaimRecord;
import com.junsong.member.domain.MemSeckill;
import com.junsong.member.mapper.MemMemberMapper;
import com.junsong.member.mapper.MemSeckillClaimRecordMapper;
import com.junsong.member.mapper.MemSeckillRecordMapper;
import com.junsong.member.mapper.MemSeckillMapper;
import com.junsong.member.domain.MemSeckillRecord;
import com.junsong.member.service.IMemSeckillRecordService;
import com.junsong.common.datascope.annotation.DataScope;
import com.junsong.common.security.utils.SecurityUtils;

/**
 * 秒杀记录Service业务层处理
 */
@Service
public class MemSeckillRecordServiceImpl implements IMemSeckillRecordService {

    @Autowired
    private MemSeckillRecordMapper memSeckillRecordMapper;

    @Autowired
    private MemSeckillClaimRecordMapper memSeckillClaimRecordMapper;

    @Autowired
    private MemSeckillMapper memSeckillMapper;

    @Autowired
    private MemMemberMapper memMemberMapper;

    /**
     * 查询秒杀记录
     *
     * @param id 秒杀记录ID
     * @return 秒杀记录
     */
    @Override
    @DataScope(deptAlias = "r")
    public MemSeckillRecord selectMemSeckillRecordById(Long id) {
        return memSeckillRecordMapper.selectMemSeckillRecordById(id);
    }

    /**
     * 查询秒杀记录列表
     *
     * @param memSeckillRecord 秒杀记录
     * @return 秒杀记录
     */
    @Override
    @DataScope(deptAlias = "r")
    public List<MemSeckillRecord> selectMemSeckillRecordList(MemSeckillRecord memSeckillRecord) {
        return memSeckillRecordMapper.selectMemSeckillRecordList(memSeckillRecord);
    }

    /**
     * 新增秒杀记录
     *
     * @param memSeckillRecord 秒杀记录
     * @return 结果
     */
    @Override
    @Transactional
    public int insertMemSeckillRecord(MemSeckillRecord memSeckillRecord) {
        fillTotalAmountIfMissing(memSeckillRecord);
        return memSeckillRecordMapper.insertMemSeckillRecord(memSeckillRecord);
    }

    /**
     * 修改秒杀记录
     *
     * @param memSeckillRecord 秒杀记录
     * @return 结果
     */
    @Override
    @Transactional
    public int updateMemSeckillRecord(MemSeckillRecord memSeckillRecord) {
        fillTotalAmountIfMissing(memSeckillRecord);
        return memSeckillRecordMapper.updateMemSeckillRecord(memSeckillRecord);
    }

    private void fillTotalAmountIfMissing(MemSeckillRecord record) {
        if (record == null || record.getTotalAmount() != null || record.getSeckillId() == null || record.getShares() == null) {
            return;
        }
        MemSeckill seckill = memSeckillMapper.selectMemSeckillById(record.getSeckillId());
        if (seckill != null && seckill.getSeckillPrice() != null) {
            record.setTotalAmount(seckill.getSeckillPrice().multiply(BigDecimal.valueOf(record.getShares())));
        }
    }

    /**
     * 删除秒杀记录
     *
     * @param id 秒杀记录ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemSeckillRecordById(Long id) {
        memSeckillClaimRecordMapper.deleteMemSeckillClaimRecordByRecordIds(new Long[] { id });
        return memSeckillRecordMapper.deleteMemSeckillRecordById(id);
    }

    /**
     * 批量删除秒杀记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemSeckillRecordByIds(Long[] ids) {
        memSeckillClaimRecordMapper.deleteMemSeckillClaimRecordByRecordIds(ids);
        return memSeckillRecordMapper.deleteMemSeckillRecordByIds(ids);
    }

    /**
     * 领取秒杀记录
     *
     * @param recordId 秒杀记录ID
     * @return 结果
     */
    @Override
    @Transactional
    public int claimMemSeckillRecord(Long recordId, Integer claimShares, Date claimTime, String remark) {
        if (claimShares == null || claimShares <= 0) {
            throw new ServiceException("领取数量必须大于0");
        }
        MemSeckillRecord record = memSeckillRecordMapper.selectMemSeckillRecordById(recordId);
        if (record == null) {
            throw new ServiceException("秒杀记录不存在");
        }
        if ("2".equals(record.getStatus())) {
            throw new ServiceException("已取消的秒杀记录不能领取");
        }
        int remainingShares = record.getRemainingShares() == null ? 0 : record.getRemainingShares();
        if (remainingShares <= 0) {
            throw new ServiceException("该秒杀记录已全部领取");
        }
        if (claimShares > remainingShares) {
            throw new ServiceException("本次领取数量不能超过剩余可领数量：" + remainingShares);
        }

        String username = SecurityUtils.getUsername();
        MemSeckillClaimRecord claimRecord = new MemSeckillClaimRecord();
        claimRecord.setDeptId(record.getDeptId());
        claimRecord.setRecordId(record.getRecordId());
        claimRecord.setSeckillId(record.getSeckillId());
        claimRecord.setMemberId(record.getMemberId());
        claimRecord.setMemberNo(record.getMemberNo());
        claimRecord.setMemberName(record.getMemberName());
        claimRecord.setClaimShares(claimShares);
        claimRecord.setClaimTime(claimTime == null ? new Date() : claimTime);
        claimRecord.setClaimBy(username);
        claimRecord.setCreateBy(username);
        claimRecord.setRemark(remark);
        memSeckillClaimRecordMapper.insertMemSeckillClaimRecord(claimRecord);

        int newRemainingShares = remainingShares - claimShares;
        int version = record.getVersion() == null ? 0 : record.getVersion();
        int rows = memSeckillRecordMapper.updateClaimStatusWithVersion(recordId, newRemainingShares == 0 ? "1" : "3", username, version);
        if (rows == 0) {
            throw new ServiceException("并发冲突，请稍后重试");
        }
        return rows;
    }

    /**
     * 查询秒杀领取明细
     */
    @Override
    @DataScope(deptAlias = "r")
    public List<MemSeckillClaimRecord> selectClaimRecordList(MemSeckillClaimRecord claimRecord) {
        return memSeckillClaimRecordMapper.selectMemSeckillClaimRecordList(claimRecord);
    }

    /**
     * 统计秒杀记录
     */
    @Override
    public Map<String, Object> getRecordStatistics(MemSeckillRecord memSeckillRecord) {
        return memSeckillRecordMapper.selectRecordStatistics(memSeckillRecord);
    }

    /**
     * 按付款方式统计
     */
    @Override
    public List<Map<String, Object>> getPaymentMethodStats(MemSeckillRecord memSeckillRecord) {
        return memSeckillRecordMapper.selectPaymentMethodStats(memSeckillRecord);
    }

    /**
     * 全员秒杀：根据指定的秒杀活动和日期，为当前部门所有有效会员生成秒杀记录
     */
    @Override
    @Transactional
    public Map<String, Object> batchSeckillForAllMembers(MemSeckillRecord template) {
        if (template == null || template.getSeckillId() == null) {
            throw new ServiceException("请选择秒杀活动");
        }
        if (template.getSeckillDate() == null) {
            throw new ServiceException("请选择秒杀参与日期");
        }

        Long deptId = template.getDeptId();
        if (deptId == null) {
            deptId = SecurityUtils.getDeptId();
            template.setDeptId(deptId);
        }

        MemSeckill seckill = memSeckillMapper.selectMemSeckillById(template.getSeckillId());
        if (seckill == null) {
            throw new ServiceException("秒杀活动不存在");
        }

        Integer shares = template.getShares();
        if (shares == null || shares <= 0) {
            shares = 1;
        }
        String paymentMethod = template.getPaymentMethod();
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            paymentMethod = "cash";
        }
        String username = SecurityUtils.getUsername();

        List<MemMember> members = memMemberMapper.selectActiveMembersForSeckill(deptId, template.getSeckillDate());
        Set<Long> alreadyParticipatedIds = new HashSet<>(
                memSeckillRecordMapper.selectMemberIdsBySeckillAndDate(deptId, template.getSeckillId(), template.getSeckillDate()));

        int successNum = 0;
        int skippedNum = 0;
        List<String> skippedMembers = new ArrayList<>();
        for (MemMember member : members) {
            if (alreadyParticipatedIds.contains(member.getMemberId())) {
                skippedNum++;
                skippedMembers.add(member.getMemberNo());
                continue;
            }
            MemSeckillRecord record = new MemSeckillRecord();
            record.setDeptId(deptId);
            record.setSeckillId(template.getSeckillId());
            record.setMemberId(member.getMemberId());
            record.setMemberNo(member.getMemberNo());
            record.setMemberName(member.getMemberName());
            record.setSeckillDate(template.getSeckillDate());
            record.setPaymentMethod(paymentMethod);
            record.setShares(shares);
            record.setStatus("0");
            record.setCreateBy(username);
            record.setRemark(template.getRemark());
            if (seckill.getSeckillPrice() != null) {
                record.setTotalAmount(seckill.getSeckillPrice().multiply(BigDecimal.valueOf(shares)));
            }
            memSeckillRecordMapper.insertMemSeckillRecord(record);
            successNum++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalNum", members.size());
        result.put("successNum", successNum);
        result.put("skippedNum", skippedNum);
        result.put("skippedMembers", skippedMembers);
        return result;
    }
}
