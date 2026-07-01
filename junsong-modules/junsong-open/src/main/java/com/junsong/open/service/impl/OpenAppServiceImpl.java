package com.junsong.open.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.open.domain.OpenApp;
import com.junsong.open.domain.OpenAppSecret;
import com.junsong.open.mapper.OpenAppMapper;
import com.junsong.open.mapper.OpenAppSecretMapper;
import com.junsong.open.service.IOpenAppService;
import com.junsong.open.service.IOpenAppSecretService;

/**
 * 开发者应用Service实现
 *
 * 业务逻辑：
 *   注册应用 → 自动发放测试Key(配额100/天) → 管理员审批通过 → 发放生产Key(配额10000/天)
 *
 * @author junsong
 */
@Service
public class OpenAppServiceImpl implements IOpenAppService
{
    @Autowired
    private OpenAppMapper openAppMapper;

    @Autowired
    private OpenAppSecretMapper openAppSecretMapper;

    @Autowired
    private IOpenAppSecretService openAppSecretService;

    @Override
    public OpenApp selectOpenAppById(Long id)
    {
        return openAppMapper.selectOpenAppById(id);
    }

    @Override
    public List<OpenApp> selectOpenAppList(OpenApp openApp)
    {
        return openAppMapper.selectOpenAppList(openApp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertOpenApp(OpenApp openApp)
    {
        openApp.setStatus("PENDING");
        int rows = openAppMapper.insertOpenApp(openApp);
        if (rows > 0)
        {
            openAppSecretService.generateTestKey(openApp);
        }
        return rows;
    }

    @Override
    public int updateOpenApp(OpenApp openApp)
    {
        return openAppMapper.updateOpenApp(openApp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteOpenAppByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            openAppSecretMapper.deleteOpenAppSecretByAppId(id);
        }
        return openAppMapper.deleteOpenAppByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveApp(Long appId, String updateBy)
    {
        OpenApp app = openAppMapper.selectOpenAppById(appId);
        if (app == null)
        {
            throw new ServiceException("应用不存在");
        }
        if (!"PENDING".equals(app.getStatus()))
        {
            throw new ServiceException("应用状态不允许审批(当前:" + app.getStatus() + ")");
        }
        app.setStatus("APPROVED");
        app.setRejectReason(null);
        app.setUpdateBy(updateBy);
        int rows = openAppMapper.updateOpenApp(app);
        openAppSecretService.generateProductionKey(app);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rejectApp(Long appId, String rejectReason, String updateBy)
    {
        OpenApp app = openAppMapper.selectOpenAppById(appId);
        if (app == null)
        {
            throw new ServiceException("应用不存在");
        }
        if (!"PENDING".equals(app.getStatus()))
        {
            throw new ServiceException("应用状态不允许驳回(当前:" + app.getStatus() + ")");
        }
        app.setStatus("REJECTED");
        app.setRejectReason(rejectReason);
        app.setUpdateBy(updateBy);
        return openAppMapper.updateOpenApp(app);
    }
}
