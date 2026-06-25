package com.junsong.workflow.lowcode.event;

import com.junsong.workflow.lowcode.service.impl.LcExpressionServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 低代码配置发布事件监听器。
 *
 * 收到配置变更事件后，清除 SpEL 表达式编译缓存，
 * 确保新的分支规则、处理人表达式等立即生效。
 *
 * <p>注：Spring Cache（@Cacheable）的缓存由 {@code @CacheEvict} 注解自动处理，
 * 本监听器负责补充非 Spring Cache 管理的自定义缓存。</p>
 *
 * @author Genesis·峻松
 */
@Component
public class LcConfigCacheRefreshListener implements ApplicationListener<LcConfigPublishedEvent>
{
    private static final Logger log = LoggerFactory.getLogger(LcConfigCacheRefreshListener.class);

    @Autowired
    private LcExpressionServiceImpl expressionService;

    @Override
    public void onApplicationEvent(LcConfigPublishedEvent event)
    {
        String bizCode = event.getBizCode();
        log.info("收到配置发布事件，清空 SpEL 缓存: bizCode={}", bizCode);
        expressionService.clearCache();
    }
}
