package com.junsong.workflow.service.fieldperm;

import java.util.List;
import java.util.stream.Collectors;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.domain.WfNodeFieldPermission;
import com.junsong.workflow.mapper.WfNodeFieldPermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowFieldPermissionService
{
    @Autowired
    private WfNodeFieldPermissionMapper fieldPermissionMapper;

    public R<List<WfNodeFieldPermission>> list(WfNodeFieldPermission permission)
    {
        return R.ok(fieldPermissionMapper.selectList(permission));
    }

    public R<List<WfNodeFieldPermission>> getByNode(String processDefinitionKey, String activityId)
    {
        return R.ok(fieldPermissionMapper.selectByNode(processDefinitionKey, activityId));
    }

    public R<Void> add(WfNodeFieldPermission permission)
    {
        fieldPermissionMapper.insert(permission);
        return R.ok();
    }

    public R<Void> update(WfNodeFieldPermission permission)
    {
        fieldPermissionMapper.update(permission);
        return R.ok();
    }

    public R<Void> delete(Long id)
    {
        fieldPermissionMapper.deleteById(id);
        return R.ok();
    }
}
