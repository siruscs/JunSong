package com.junsong.workflow.controller.dto.definition;

public record DefinitionXmlResp(
        String definitionId,
        String processKey,
        String processName,
        int version,
        String xml)
{
}
