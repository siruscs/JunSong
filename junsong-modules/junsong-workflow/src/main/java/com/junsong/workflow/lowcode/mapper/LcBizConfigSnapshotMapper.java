package com.junsong.workflow.lowcode.mapper;

import com.junsong.workflow.lowcode.domain.LcBizConfigSnapshot;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LcBizConfigSnapshotMapper
{
    /** 查询业务的所有发布快照（按版本号倒序） */
    List<LcBizConfigSnapshot> selectByBizCode(@Param("bizCode") String bizCode);

    /** 查询指定版本的快照 */
    LcBizConfigSnapshot selectByBizCodeAndVersion(@Param("bizCode") String bizCode,
                                                   @Param("versionNo") Integer versionNo);

    /** 查询最新已发布版本号 */
    Integer selectMaxVersionByBizCode(@Param("bizCode") String bizCode);

    /** 插入快照 */
    int insertSnapshot(LcBizConfigSnapshot snapshot);
}
