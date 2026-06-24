package com.junsong.workflow.controller.dto.definition;

public record DeployDefinitionReq(
        String xml,
        String processKey,
        String processName,
        String deploymentName,
        String category)
{
}
