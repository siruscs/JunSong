package com.junsong.workflow.service.health;

import java.util.LinkedHashMap;
import java.util.Map;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 工作流引擎健康检查服务
 * 封装 Flowable RepositoryService / RuntimeService / TaskService 的存在性判断与计数查询，
 * Controller 层不直接依赖引擎 API。
 *
 * @author junsong
 */
@Service
public class WorkflowHealthService
{
    @Autowired(required = false)
    private RepositoryService repositoryService;

    @Autowired(required = false)
    private RuntimeService runtimeService;

    @Autowired(required = false)
    private TaskService taskService;

    /**
     * 引擎健康指标
     */
    public Map<String, Object> engine()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("module", "junsong-workflow");
        result.put("engine", "Flowable 8.0.0");
        result.put("repositoryService", repositoryService != null);
        result.put("runtimeService", runtimeService != null);
        result.put("taskService", taskService != null);
        if (repositoryService != null)
        {
            long deployCount = repositoryService.createDeploymentQuery().count();
            long defCount = repositoryService.createProcessDefinitionQuery().count();
            result.put("deploymentCount", deployCount);
            result.put("processDefinitionCount", defCount);
        }
        return result;
    }
}
