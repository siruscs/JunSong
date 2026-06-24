package com.junsong.member.service;

import java.util.List;
import java.util.Map;
import java.util.Date;
import com.junsong.member.domain.MemSeckillClaimRecord;
import com.junsong.member.domain.MemSeckillRecord;

/**
 * 秒杀记录Service接口
 */
public interface IMemSeckillRecordService {

    /**
     * 查询秒杀记录
     *
     * @param id 秒杀记录ID
     * @return 秒杀记录
     */
    public MemSeckillRecord selectMemSeckillRecordById(Long id);

    /**
     * 查询秒杀记录列表
     *
     * @param memSeckillRecord 秒杀记录
     * @return 秒杀记录集合
     */
    public List<MemSeckillRecord> selectMemSeckillRecordList(MemSeckillRecord memSeckillRecord);

    /**
     * 新增秒杀记录
     *
     * @param memSeckillRecord 秒杀记录
     * @return 结果
     */
    public int insertMemSeckillRecord(MemSeckillRecord memSeckillRecord);

    /**
     * 修改秒杀记录
     *
     * @param memSeckillRecord 秒杀记录
     * @return 结果
     */
    public int updateMemSeckillRecord(MemSeckillRecord memSeckillRecord);

    /**
     * 删除秒杀记录
     *
     * @param id 秒杀记录ID
     * @return 结果
     */
    public int deleteMemSeckillRecordById(Long id);

    /**
     * 批量删除秒杀记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMemSeckillRecordByIds(Long[] ids);

    /**
     * 领取秒杀记录
     *
     * @param recordId 秒杀记录ID
     * @return 结果
     */
    public int claimMemSeckillRecord(Long recordId, Integer claimShares, Date claimTime, String remark);

    /**
     * 查询秒杀领取明细
     *
     * @param claimRecord 查询条件
     * @return 领取明细集合
     */
    public List<MemSeckillClaimRecord> selectClaimRecordList(MemSeckillClaimRecord claimRecord);

    /**
     * 统计秒杀记录（总人数、总份额、总金额、已领取）
     *
     * @param memSeckillRecord 查询条件
     * @return 统计结果Map
     */
    public Map<String, Object> getRecordStatistics(MemSeckillRecord memSeckillRecord);

    /**
     * 按付款方式统计
     *
     * @param memSeckillRecord 查询条件
     * @return 付款方式统计列表
     */
    public List<Map<String, Object>> getPaymentMethodStats(MemSeckillRecord memSeckillRecord);

    /**
     * 全员秒杀：根据指定的秒杀活动和日期，为当前部门所有有效会员生成秒杀记录
     *
     * @param template 含有 seckillId / seckillDate / paymentMethod / shares / remark 的模板
     * @return 包含 totalNum / successNum / skippedNum 的结果
     */
    public Map<String, Object> batchSeckillForAllMembers(MemSeckillRecord template);
}
