package com.junsong.open.service;

import java.util.List;
import com.junsong.open.domain.OpenIsv;

/**
 * ISV注册 服务层接口
 */
public interface IOpenIsvService
{
    OpenIsv selectOpenIsvById(Long id);
    List<OpenIsv> selectOpenIsvList(OpenIsv openIsv);
    int insertOpenIsv(OpenIsv openIsv);
    int updateOpenIsv(OpenIsv openIsv);
    int approveIsv(Long id, String username);
    int rejectIsv(Long id, String rejectReason, String username);
    int deleteOpenIsvByIds(Long[] ids);
}
