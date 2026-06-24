package com.junsong.workflow.lowcode.mapper;

import com.junsong.workflow.lowcode.domain.LcBizAction;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LcBizActionMapper
{
    List<LcBizAction> selectByBizCode(String bizCode);

    int insertAction(LcBizAction lcBizAction);

    int physicalDeleteByBizCode(String bizCode);
}
