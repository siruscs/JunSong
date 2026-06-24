package com.junsong.workflow.lowcode.mapper;

import com.junsong.workflow.lowcode.domain.LcBizObject;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LcBizObjectMapper
{
    List<LcBizObject> selectLcBizObjectList(LcBizObject query);

    LcBizObject selectLcBizObjectById(Long id);

    LcBizObject selectLcBizObjectByBizCode(String bizCode);

    int insertLcBizObject(LcBizObject lcBizObject);

    int updateLcBizObject(LcBizObject lcBizObject);

    int deleteLcBizObjectByIds(Long[] ids);
}
