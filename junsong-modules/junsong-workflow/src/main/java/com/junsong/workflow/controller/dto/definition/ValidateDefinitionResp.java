package com.junsong.workflow.controller.dto.definition;

import java.util.List;

public record ValidateDefinitionResp(
        boolean valid,
        List<String> validationMessages,
        String processKey,
        String processName)
{
}
