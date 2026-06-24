package com.junsong.member.mapper;

import com.junsong.member.domain.MemSeckillClaimRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀领取明细Mapper接口
 */
@Mapper
public interface MemSeckillClaimRecordMapper
{
    List<MemSeckillClaimRecord> selectMemSeckillClaimRecordList(MemSeckillClaimRecord record);

    int insertMemSeckillClaimRecord(MemSeckillClaimRecord record);

    int deleteMemSeckillClaimRecordByRecordIds(Long[] recordIds);
}
