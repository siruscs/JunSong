package com.junsong.workflow.mapper;

import java.util.List;
import com.junsong.workflow.domain.WfNodeTimeout;

public interface WfNodeTimeoutMapper
{
    WfNodeTimeout selectById(Long id);
    List<WfNodeTimeout> selectList(WfNodeTimeout timeout);
    List<WfNodeTimeout> selectAll();
    WfNodeTimeout selectByNode(String processDefinitionKey, String activityId);
    int insert(WfNodeTimeout timeout);
    int update(WfNodeTimeout timeout);
    int deleteById(Long id);
}
