package com.junsong.workflow.lowcode.mapper;

import com.junsong.workflow.lowcode.domain.LcBizPostAction;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LcBizPostActionMapper
{
    List<LcBizPostAction> selectByBizCode(String bizCode);

    List<LcBizPostAction> selectByBizCodeAndEvent(@Param("bizCode") String bizCode, @Param("triggerEvent") String triggerEvent);

    int insertPostAction(LcBizPostAction lcBizPostAction);

    int physicalDeleteByBizCode(String bizCode);
}
