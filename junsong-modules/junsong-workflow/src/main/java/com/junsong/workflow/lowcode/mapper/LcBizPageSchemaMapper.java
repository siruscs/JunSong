package com.junsong.workflow.lowcode.mapper;

import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LcBizPageSchemaMapper
{
    List<LcBizPageSchema> selectLcBizPageSchemaList(LcBizPageSchema query);

    LcBizPageSchema selectLcBizPageSchemaById(Long id);

    List<LcBizPageSchema> selectByBizCode(String bizCode);

    int insertLcBizPageSchema(LcBizPageSchema lcBizPageSchema);

    int updateLcBizPageSchema(LcBizPageSchema lcBizPageSchema);

    int deleteLcBizPageSchemaByIds(Long[] ids);

    int physicalDeleteByBizCode(String bizCode);
}
