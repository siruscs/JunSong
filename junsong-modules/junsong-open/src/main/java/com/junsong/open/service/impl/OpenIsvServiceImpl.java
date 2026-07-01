package com.junsong.open.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.open.domain.OpenIsv;
import com.junsong.open.mapper.OpenIsvMapper;
import com.junsong.open.service.IOpenIsvService;

/**
 * ISV注册 服务层实现
 */
@Service
public class OpenIsvServiceImpl implements IOpenIsvService
{
    @Autowired
    private OpenIsvMapper openIsvMapper;

    @Override
    public OpenIsv selectOpenIsvById(Long id)
    {
        return openIsvMapper.selectOpenIsvById(id);
    }

    @Override
    public List<OpenIsv> selectOpenIsvList(OpenIsv openIsv)
    {
        return openIsvMapper.selectOpenIsvList(openIsv);
    }

    @Override
    public int insertOpenIsv(OpenIsv openIsv)
    {
        openIsv.setStatus("PENDING");
        return openIsvMapper.insertOpenIsv(openIsv);
    }

    @Override
    public int updateOpenIsv(OpenIsv openIsv)
    {
        return openIsvMapper.updateOpenIsv(openIsv);
    }

    @Override
    public int approveIsv(Long id, String username)
    {
        OpenIsv isv = new OpenIsv();
        isv.setId(id);
        isv.setStatus("APPROVED");
        return openIsvMapper.updateOpenIsv(isv);
    }

    @Override
    public int rejectIsv(Long id, String rejectReason, String username)
    {
        OpenIsv isv = new OpenIsv();
        isv.setId(id);
        isv.setStatus("REJECTED");
        isv.setRejectReason(rejectReason);
        return openIsvMapper.updateOpenIsv(isv);
    }

    @Override
    public int deleteOpenIsvByIds(Long[] ids)
    {
        return openIsvMapper.deleteOpenIsvByIds(ids);
    }
}
