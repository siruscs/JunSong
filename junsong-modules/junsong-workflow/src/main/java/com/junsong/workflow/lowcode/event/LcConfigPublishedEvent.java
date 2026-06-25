package com.junsong.workflow.lowcode.event;

import org.springframework.context.ApplicationEvent;

/**
 * 低代码业务对象配置发布/变更事件。
 *
 * 当业务对象配置被保存、发布或回滚时触发，
 * 监听器可据此刷新相关缓存（元数据缓存、SpEL 表达式缓存等）。
 *
 * @author Genesis·峻松
 */
public class LcConfigPublishedEvent extends ApplicationEvent
{
    private final String bizCode;

    public LcConfigPublishedEvent(Object source, String bizCode)
    {
        super(source);
        this.bizCode = bizCode;
    }

    public String getBizCode()
    {
        return bizCode;
    }
}
