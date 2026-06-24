package com.junsong.workflow.controller.dto.definition;

public record ValidateDefinitionReq(
        String xml,
        String processKey,
        String processName)
{
}
