package com.junsong.open.service.impl;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.open.domain.OpenApp;
import com.junsong.open.domain.OpenAppSecret;
import com.junsong.open.mapper.OpenAppSecretMapper;
import com.junsong.open.service.IOpenAppSecretService;

/**
 * API Key Service实现
 *
 * Key类型：
 *   test       - 测试Key，注册时自动发放，配额100/天
 *   production - 生产Key，审批通过后发放，配额10000/天
 *
 * @author junsong
 */
@Service
public class OpenAppSecretServiceImpl implements IOpenAppSecretService
{
    private static final int TEST_QUOTA = 100;
    private static final int PRODUCTION_QUOTA = 10000;

    @Autowired
    private OpenAppSecretMapper openAppSecretMapper;

    @Override
    public List<OpenAppSecret> selectOpenAppSecretList(OpenAppSecret openAppSecret)
    {
        return openAppSecretMapper.selectOpenAppSecretList(openAppSecret);
    }

    @Override
    public List<OpenAppSecret> selectKeysByAppId(Long appId)
    {
        return openAppSecretMapper.selectByAppId(appId);
    }

    @Override
    public OpenAppSecret selectByAppKey(String appKey)
    {
        return openAppSecretMapper.selectByAppKey(appKey);
    }

    @Override
    public void generateTestKey(OpenApp app)
    {
        OpenAppSecret secret = new OpenAppSecret();
        secret.setTenantId(app.getTenantId());
        secret.setAppId(app.getId());
        secret.setAppKey(generateAppKey());
        secret.setAppSecret(generateAppSecret());
        secret.setKeyType("test");
        secret.setStatus("0");
        secret.setDailyQuota(TEST_QUOTA);
        secret.setCreateBy(app.getCreateBy());
        openAppSecretMapper.insertOpenAppSecret(secret);
    }

    @Override
    public void generateProductionKey(OpenApp app)
    {
        OpenAppSecret secret = new OpenAppSecret();
        secret.setTenantId(app.getTenantId());
        secret.setAppId(app.getId());
        secret.setAppKey(generateAppKey());
        secret.setAppSecret(generateAppSecret());
        secret.setKeyType("production");
        secret.setStatus("0");
        secret.setDailyQuota(PRODUCTION_QUOTA);
        secret.setCreateBy(app.getUpdateBy());
        openAppSecretMapper.insertOpenAppSecret(secret);
    }

    @Override
    public int changeStatus(OpenAppSecret openAppSecret)
    {
        return openAppSecretMapper.updateOpenAppSecret(openAppSecret);
    }

    @Override
    public String generateAppKey()
    {
        return "js_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String generateAppSecret()
    {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
