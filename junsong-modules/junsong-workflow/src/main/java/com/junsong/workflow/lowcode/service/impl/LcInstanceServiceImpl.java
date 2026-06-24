package com.junsong.workflow.lowcode.service.impl;

import com.junsong.workflow.lowcode.domain.LcBizInstance;
import com.junsong.workflow.lowcode.mapper.LcBizInstanceMapper;
import com.junsong.workflow.lowcode.service.LcInstanceService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LcInstanceServiceImpl implements LcInstanceService
{
    @Autowired
    private LcBizInstanceMapper lcBizInstanceMapper;

    @Override
    public List<LcBizInstance> selectLcBizInstanceList(LcBizInstance query)
    {
        return lcBizInstanceMapper.selectLcBizInstanceList(query);
    }

    @Override
    public LcBizInstance selectLcBizInstanceById(Long id)
    {
        return lcBizInstanceMapper.selectLcBizInstanceById(id);
    }

    @Override
    public LcBizInstance selectByBizCodeAndOrderNo(String bizCode, String orderNo)
    {
        return lcBizInstanceMapper.selectByBizCodeAndOrderNo(bizCode, orderNo);
    }

    @Override
    public LcBizInstance selectByProcessInstanceId(String processInstanceId)
    {
        return lcBizInstanceMapper.selectByProcessInstanceId(processInstanceId);
    }

    @Override
    public int insertLcBizInstance(LcBizInstance instance)
    {
        return lcBizInstanceMapper.insertLcBizInstance(instance);
    }

    @Override
    public int updateLcBizInstance(LcBizInstance instance)
    {
        return lcBizInstanceMapper.updateLcBizInstance(instance);
    }

    @Override
    public int deleteLcBizInstanceByIds(Long[] ids)
    {
        return lcBizInstanceMapper.deleteLcBizInstanceByIds(ids);
    }

    @Override
    public int updateWorkflowSnapshot(LcBizInstance instance)
    {
        return lcBizInstanceMapper.updateWorkflowSnapshot(instance);
    }
}
