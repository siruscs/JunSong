package com.junsong.system.service;

import com.junsong.system.domain.vo.ActionTouchRequestVO;
import com.junsong.system.domain.vo.ActionTouchResultVO;

public interface ISysActionTouchService {
    ActionTouchResultVO touch(String actionId, ActionTouchRequestVO request);
}
