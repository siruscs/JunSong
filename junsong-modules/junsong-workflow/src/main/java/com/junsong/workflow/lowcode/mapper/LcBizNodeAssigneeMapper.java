package com.junsong.workflow.lowcode.mapper;

import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LcBizNodeAssigneeMapper
{
    List<LcBizNodeAssignee> selectLcBizNodeAssigneeList(LcBizNodeAssignee query);

    LcBizNodeAssignee selectLcBizNodeAssigneeById(Long id);

    List<LcBizNodeAssignee> selectByBizCode(String bizCode);

    int insertLcBizNodeAssignee(LcBizNodeAssignee lcBizNodeAssignee);

    int updateLcBizNodeAssignee(LcBizNodeAssignee lcBizNodeAssignee);

    int deleteLcBizNodeAssigneeByIds(Long[] ids);

    int physicalDeleteByBizCode(String bizCode);
}
