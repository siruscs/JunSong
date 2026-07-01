package com.junsong.workflow.mapper;

import java.util.List;
import com.junsong.workflow.domain.WfTaskAttachment;

public interface WfTaskAttachmentMapper {
    List<WfTaskAttachment> selectByTaskId(String taskId);
    List<WfTaskAttachment> selectByProcessInstanceId(String processInstanceId);
    int insert(WfTaskAttachment attachment);
    int deleteById(Long id);
}
