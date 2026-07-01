package com.junsong.open.mapper;

import java.util.List;
import com.junsong.open.domain.OpenAppSecret;

/**
 * API Key Mapper
 *
 * @author junsong
 */
public interface OpenAppSecretMapper
{
    public OpenAppSecret selectOpenAppSecretById(Long id);

    public List<OpenAppSecret> selectOpenAppSecretList(OpenAppSecret openAppSecret);

    public OpenAppSecret selectByAppKey(String appKey);

    public List<OpenAppSecret> selectByAppId(Long appId);

    public int insertOpenAppSecret(OpenAppSecret openAppSecret);

    public int updateOpenAppSecret(OpenAppSecret openAppSecret);

    public int deleteOpenAppSecretByAppId(Long appId);
}
