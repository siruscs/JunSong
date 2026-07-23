package com.junsong.system.api.factory;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.RemoteUserMpBindingService;
import com.junsong.system.api.domain.SysUserMpBinding;

/**
 * 小程序微信账号绑定关系服务降级处理
 */
@Component
public class RemoteUserMpBindingFallbackFactory implements FallbackFactory<RemoteUserMpBindingService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteUserMpBindingFallbackFactory.class);

    @Override
    public RemoteUserMpBindingService create(Throwable throwable)
    {
        log.error("微信绑定服务调用失败:{}", throwable.getMessage());
        return new RemoteUserMpBindingService()
        {
            @Override
            public R<SysUserMpBinding> selectByAppOpenid(String appId, String openid, String source)
            {
                return R.fail("查询微信绑定失败:" + throwable.getMessage());
            }

            @Override
            public R<List<SysUserMpBinding>> selectByUserId(Long tenantId, Long userId, String source)
            {
                return R.fail("查询用户绑定列表失败:" + throwable.getMessage());
            }

            @Override
            public R<Integer> insert(SysUserMpBinding binding, String source)
            {
                return R.fail("创建微信绑定失败:" + throwable.getMessage());
            }

            @Override
            public R<Integer> revoke(SysUserMpBinding binding, String source)
            {
                return R.fail("解绑微信失败:" + throwable.getMessage());
            }

            @Override
            public R<Integer> updateLastLoginTime(Long tenantId, Long bindingId, String source)
            {
                return R.fail("更新微信绑定登录时间失败:" + throwable.getMessage());
            }
        };
    }
}
