package com.junsong.system.service;

import java.util.List;
import com.junsong.system.domain.vo.*;

public interface ISysActionCenterService {
    List<ActionCenterItemVO> listActions(ActionCenterQueryParams params);
    List<ActionCenterCalendarVO> getCalendar(ActionCenterQueryParams params);
    ActionCenterItemVO getAction(String actionId);
}
