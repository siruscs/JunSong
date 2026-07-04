package com.junsong.system.service;

import com.junsong.system.domain.vo.ActionCenterItemVO;
import com.junsong.system.domain.vo.ActionTouchRequestVO;
import com.junsong.system.domain.vo.ActionTouchResultVO;

public interface ITouchChannelAdapter {
    String channel();
    ActionTouchResultVO send(ActionCenterItemVO action, ActionTouchRequestVO request);
}
