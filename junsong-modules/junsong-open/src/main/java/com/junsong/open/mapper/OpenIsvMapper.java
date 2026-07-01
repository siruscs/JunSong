package com.junsong.open.mapper;

import java.util.List;
import com.junsong.open.domain.OpenIsv;

/**
 * ISV注册 Mapper接口
 */
public interface OpenIsvMapper
{
    OpenIsv selectOpenIsvById(Long id);
    List<OpenIsv> selectOpenIsvList(OpenIsv openIsv);
    int insertOpenIsv(OpenIsv openIsv);
    int updateOpenIsv(OpenIsv openIsv);
    int deleteOpenIsvByIds(Long[] ids);
}
