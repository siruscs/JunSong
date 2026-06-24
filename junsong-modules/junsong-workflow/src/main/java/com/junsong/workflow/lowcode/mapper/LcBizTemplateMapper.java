package com.junsong.workflow.lowcode.mapper;

import com.junsong.workflow.lowcode.domain.LcBizTemplate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LcBizTemplateMapper
{
    List<LcBizTemplate> selectTemplateList(@Param("category") String category);

    LcBizTemplate selectTemplateById(@Param("id") Long id);

    LcBizTemplate selectTemplateByCode(@Param("templateCode") String templateCode);

    int insertTemplate(LcBizTemplate template);

    int incrementUsageCount(@Param("id") Long id);

    int deleteTemplateByIds(@Param("ids") Long[] ids);
}
