package com.junsong.system.api.factory;

import com.junsong.common.core.domain.R;
import com.junsong.system.api.RemoteMenuService;
import com.junsong.system.api.domain.LcMenuGenerateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 菜单服务降级处理
 */
@Component
public class RemoteMenuFallbackFactory implements FallbackFactory<RemoteMenuService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteMenuFallbackFactory.class);

    @Override
    public RemoteMenuService create(Throwable throwable)
    {
        log.error("菜单服务调用失败:{}", throwable.getMessage());
        return new RemoteMenuService()
        {
            @Override
            public R<Long> generateLowcodeMenu(LcMenuGenerateRequest request, String source)
            {
                return R.fail("菜单生成服务不可用:" + throwable.getMessage());
            }
        };
    }
}
