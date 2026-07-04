package com.junsong.system.mapper;

import java.util.List;
import com.junsong.system.domain.vo.ActionCenterCalendarVO;
import com.junsong.system.domain.vo.ActionCenterQueryParams;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysActionCenterMapper {
    List<ActionCenterCalendarVO> selectCalendar(ActionCenterQueryParams params);
}
