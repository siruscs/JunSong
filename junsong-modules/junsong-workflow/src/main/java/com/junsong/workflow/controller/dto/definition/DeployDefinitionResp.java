package com.junsong.workflow.controller.dto.definition;

public record DeployDefinitionResp(
        String deploymentId,
        String definitionId,
        String processKey,
        String processName,
        int version)
{
}
