package com.junsong.workflow.lowcode.service;

import com.junsong.workflow.lowcode.domain.LcBizInstance;
import java.util.List;

public interface LcInstanceService
{
    List<LcBizInstance> selectLcBizInstanceList(LcBizInstance query);

    LcBizInstance selectLcBizInstanceById(Long id);

    LcBizInstance selectByBizCodeAndOrderNo(String bizCode, String orderNo);

    LcBizInstance selectByProcessInstanceId(String processInstanceId);

    int insertLcBizInstance(LcBizInstance instance);

    int updateLcBizInstance(LcBizInstance instance);

    int deleteLcBizInstanceByIds(Long[] ids);

    int updateWorkflowSnapshot(LcBizInstance instance);
}
