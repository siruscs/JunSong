package com.junsong.system.mapper;

import java.util.Date;
import org.apache.ibatis.annotations.Param;
import com.junsong.system.domain.SysDataArchiveRun;

/**
 * R25数据归档执行记录 数据层
 */
public interface SysDataArchiveRunMapper
{
    /**
     * 新增归档执行记录
     */
    int insertArchiveRun(SysDataArchiveRun run);

    /**
     * 统计近7天最大候选数据量
     */
    Long sumLatestCandidateCount();

    /**
     * 统计归档候选数据量
     */
    Long countArchiveCandidates(@Param("tableName") String tableName, @Param("cutoffTime") Date cutoffTime);
}
