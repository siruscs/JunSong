package com.junsong.workflow.controller.dto.definition;

import java.util.Date;

public record DefinitionSummaryResp(
        String definitionId,
        String processKey,
        String processName,
        int version,
        boolean suspended,
        String deploymentId,
        String resourceName,
        Date deploymentTime,
        String category)
{
}
