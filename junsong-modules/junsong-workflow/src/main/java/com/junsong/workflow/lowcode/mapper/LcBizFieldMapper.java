package com.junsong.workflow.lowcode.mapper;

import com.junsong.workflow.lowcode.domain.LcBizField;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LcBizFieldMapper
{
    List<LcBizField> selectLcBizFieldList(LcBizField query);

    LcBizField selectLcBizFieldById(Long id);

    List<LcBizField> selectByBizCode(String bizCode);

    int insertLcBizField(LcBizField lcBizField);

    int updateLcBizField(LcBizField lcBizField);

    int deleteLcBizFieldByIds(Long[] ids);

    int physicalDeleteByBizCode(String bizCode);
}
