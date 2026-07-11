package com.junsong.member.mapper;

import java.util.List;
import com.junsong.member.domain.MemGrowthRecord;

/**
 * 会员成长值变动记录Mapper接口
 *
 * @author junsong
 */
public interface MemGrowthRecordMapper
{
    /**
     * 查询成长值记录列表
     */
    public List<MemGrowthRecord> selectGrowthRecordList(MemGrowthRecord record);

    /**
     * 新增成长值记录
     */
    public int insertGrowthRecord(MemGrowthRecord record);

    /**
     * 根据幂等键查询（用于防重）
     */
    public MemGrowthRecord selectByDedupKey(@org.apache.ibatis.annotations.Param("dedupKey") String dedupKey);

    /**
     * 查询会员当前成长值余额（最新一条 balance）
     */
    public Long selectLatestBalanceByMemberId(Long memberId);
}
