package com.junsong.workflow.mapper;

import java.util.List;
import com.junsong.workflow.domain.WfNodeFieldPermission;

public interface WfNodeFieldPermissionMapper
{
    WfNodeFieldPermission selectById(Long id);
    List<WfNodeFieldPermission> selectList(WfNodeFieldPermission permission);
    List<WfNodeFieldPermission> selectByNode(String processDefinitionKey, String activityId);
    int insert(WfNodeFieldPermission permission);
    int update(WfNodeFieldPermission permission);
    int deleteById(Long id);
}
