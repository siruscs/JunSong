package com.junsong.member.mapper;

import com.junsong.member.domain.MemSeckillRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 秒杀记录Mapper接口
 */
@Mapper
public interface MemSeckillRecordMapper {

    /**
     * 查询秒杀记录列表
     *
     * @param record 秒杀记录信息
     * @return 秒杀记录列表
     */
    List<MemSeckillRecord> selectMemSeckillRecordList(MemSeckillRecord record);

    /**
     * 根据ID查询秒杀记录
     *
     * @param recordId 秒杀记录ID
     * @return 秒杀记录信息
     */
    MemSeckillRecord selectMemSeckillRecordById(Long recordId);

    /**
     * 插入秒杀记录
     *
     * @param record 秒杀记录信息
     * @return 影响行数
     */
    int insertMemSeckillRecord(MemSeckillRecord record);

    /**
     * 更新秒杀记录
     *
     * @param record 秒杀记录信息
     * @return 影响行数
     */
    int updateMemSeckillRecord(MemSeckillRecord record);

    /**
     * 删除秒杀记录
     *
     * @param recordId 秒杀记录ID
     * @return 影响行数
     */
    int deleteMemSeckillRecordById(Long recordId);

    /**
     * 批量删除秒杀记录
     *
     * @param recordIds 秒杀记录ID数组
     * @return 影响行数
     */
    int deleteMemSeckillRecordByIds(Long[] recordIds);

    /**
     * 领取秒杀记录
     *
     * @param recordId 秒杀记录ID
     * @return 影响行数
     */
    int updateClaimStatus(@Param("recordId") Long recordId, @Param("status") String status, @Param("claimBy") String claimBy);

    /**
     * 统计秒杀记录（总人数、总份额、总金额、已领取）
     *
     * @param record 查询条件
     * @return 统计结果Map
     */
    Map<String, Object> selectRecordStatistics(MemSeckillRecord record);

    /**
     * 按付款方式统计
     *
     * @param record 查询条件
     * @return 付款方式统计列表
     */
    List<Map<String, Object>> selectPaymentMethodStats(MemSeckillRecord record);

    /**
     * 查询某个部门下，已经为指定秒杀活动 + 秒杀日期生成过记录的会员ID集合
     */
    List<Long> selectMemberIdsBySeckillAndDate(@Param("deptId") Long deptId,
                                               @Param("seckillId") Long seckillId,
                                               @Param("seckillDate") Date seckillDate);
}
