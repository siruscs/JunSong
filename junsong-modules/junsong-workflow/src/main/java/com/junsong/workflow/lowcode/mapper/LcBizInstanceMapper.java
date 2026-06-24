package com.junsong.workflow.lowcode.mapper;

import com.junsong.workflow.lowcode.domain.LcBizInstance;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LcBizInstanceMapper
{
    List<LcBizInstance> selectLcBizInstanceList(LcBizInstance query);

    LcBizInstance selectLcBizInstanceById(Long id);

    LcBizInstance selectByBizCodeAndOrderNo(@Param("bizCode") String bizCode, @Param("orderNo") String orderNo);

    LcBizInstance selectByProcessInstanceId(String processInstanceId);

    int insertLcBizInstance(LcBizInstance lcBizInstance);

    int updateLcBizInstance(LcBizInstance lcBizInstance);

    int deleteLcBizInstanceByIds(Long[] ids);

    int updateWorkflowSnapshot(LcBizInstance lcBizInstance);
}
