package com.junsong.common.core.interceptor;

import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * MyBatis 租户拦截器
 * 拦截 INSERT/UPDATE 操作，自动填充 tenant_id 字段
 *
 * SELECT 查询的租户过滤需配合 Mapper XML 中的 ${params.tenantId} 使用
 * （与现有 DataScope 数据权限机制一致）
 *
 * 通过配置 junsong.tenant.enabled=true 开启
 *
 * @author junsong
 */
@Intercepts({
    @Signature(type = Executor.class, method = "update",
               args = {MappedStatement.class, Object.class})
})
public class TenantInterceptor implements Interceptor
{
    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable
    {
        if (TenantContext.isIgnore())
        {
            return invocation.proceed();
        }

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        if (parameter != null)
        {
            fillTenantId(parameter);
        }

        return invocation.proceed();
    }

    /**
     * 填充 tenant_id 到 BaseEntity 子类
     */
    private void fillTenantId(Object target)
    {
        if (target instanceof BaseEntity)
        {
            fillEntity((BaseEntity) target);
            return;
        }

        try
        {
            Field[] fields = target.getClass().getDeclaredFields();
            for (Field field : fields)
            {
                if (field.getType() == BaseEntity.class)
                {
                    field.setAccessible(true);
                    BaseEntity entity = (BaseEntity) field.get(target);
                    if (entity != null)
                    {
                        fillEntity(entity);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.debug("租户ID自动填充跳过非BaseEntity参数: {}", target.getClass().getSimpleName());
        }
    }

    private void fillEntity(BaseEntity entity)
    {
        Long tenantId = TenantContext.getTenantId();
        if (entity.getTenantId() == null)
        {
            entity.setTenantId(tenantId);
            entity.getParams().put("tenantId", " AND tenant_id = " + tenantId);
        }
    }

    @Override
    public Object plugin(Object target)
    {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties)
    {
    }
}
