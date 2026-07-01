package com.junsong.open.service;

import java.util.List;
import com.junsong.open.domain.OpenApp;

/**
 * 开发者应用Service接口
 *
 * @author junsong
 */
public interface IOpenAppService
{
    public OpenApp selectOpenAppById(Long id);

    public List<OpenApp> selectOpenAppList(OpenApp openApp);

    public int insertOpenApp(OpenApp openApp);

    public int updateOpenApp(OpenApp openApp);

    public int deleteOpenAppByIds(Long[] ids);

    public int approveApp(Long appId, String updateBy);

    public int rejectApp(Long appId, String rejectReason, String updateBy);
}
