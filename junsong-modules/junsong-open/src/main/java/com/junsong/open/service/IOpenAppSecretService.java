package com.junsong.open.service;

import java.util.List;
import com.junsong.open.domain.OpenApp;
import com.junsong.open.domain.OpenAppSecret;

/**
 * API Key Service接口
 *
 * @author junsong
 */
public interface IOpenAppSecretService
{
    public List<OpenAppSecret> selectOpenAppSecretList(OpenAppSecret openAppSecret);

    public List<OpenAppSecret> selectKeysByAppId(Long appId);

    public OpenAppSecret selectByAppKey(String appKey);

    public void generateTestKey(OpenApp app);

    public void generateProductionKey(OpenApp app);

    public int changeStatus(OpenAppSecret openAppSecret);

    public String generateAppKey();

    public String generateAppSecret();
}
