package com.junsong.system.api.factory;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.RemoteDeptService;
import com.junsong.system.api.domain.SysDept;

/**
 * 部门服务降级处理
 */
@Component
public class RemoteDeptFallbackFactory implements FallbackFactory<RemoteDeptService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteDeptFallbackFactory.class);

    @Override
    public RemoteDeptService create(Throwable throwable)
    {
        log.error("部门服务调用失败:{}", throwable.getMessage());
        return new RemoteDeptService()
        {
            @Override
            public R<List<SysDept>> listStoreGeo(double minLng, double maxLng, double minLat, double maxLat, String source)
            {
                return R.fail("查询门店坐标失败:" + throwable.getMessage());
            }
        };
    }
}
