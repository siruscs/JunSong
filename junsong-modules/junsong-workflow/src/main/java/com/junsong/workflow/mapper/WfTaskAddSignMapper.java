package com.junsong.workflow.mapper;

import com.junsong.workflow.domain.WfTaskAddSign;

public interface WfTaskAddSignMapper {
    int insert(WfTaskAddSign record);
    WfTaskAddSign selectByAddSignTaskId(String addSignTaskId);
    int updateCompleteTime(Long id);
}
