package com.junsong.open.mapper;

import java.util.List;
import com.junsong.open.domain.OpenApp;

/**
 * 开发者应用Mapper
 *
 * @author junsong
 */
public interface OpenAppMapper
{
    public OpenApp selectOpenAppById(Long id);

    public List<OpenApp> selectOpenAppList(OpenApp openApp);

    public int insertOpenApp(OpenApp openApp);

    public int updateOpenApp(OpenApp openApp);

    public int deleteOpenAppById(Long id);

    public int deleteOpenAppByIds(Long[] ids);
}
