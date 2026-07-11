package com.junsong.common.security.aspect;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.utils.ServletUtils;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.annotation.RequiresLogin;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.annotation.RequiresRoles;
import com.junsong.common.security.auth.AuthUtil;

/**
 * 基于 Spring Aop 的注解鉴权
 * 
 * @author kong
 */
@Aspect
@Component
public class PreAuthorizeAspect
{
    /**
     * 构建
     */
    public PreAuthorizeAspect()
    {
    }

    /**
     * 定义AOP签名 (切入所有使用鉴权注解的方法)
     */
    public static final String POINTCUT_SIGN = " @annotation(com.junsong.common.security.annotation.RequiresLogin) || "
            + "@annotation(com.junsong.common.security.annotation.RequiresPermissions) || "
            + "@annotation(com.junsong.common.security.annotation.RequiresRoles)";

    /**
     * 声明AOP签名
     */
    @Pointcut(POINTCUT_SIGN)
    public void pointcut()
    {
    }

    /**
     * 环绕切入
     * 
     * @param joinPoint 切面对象
     * @return 底层方法执行后的返回值
     * @throws Throwable 底层方法抛出的异常
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable
    {
        // 开放平台可信请求跳过用户权限校验
        // （from-source: open-api 由开放服务内部调用透传，网关已从外部请求中剥离该头）
        if (isOpenApiTrustedRequest())
        {
            return joinPoint.proceed();
        }
        // 注解鉴权
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        checkMethodAnnotation(signature.getMethod());
        // 执行原有逻辑
        return joinPoint.proceed();
    }

    /**
     * 判断是否为开放平台可信内部请求
     */
    private boolean isOpenApiTrustedRequest()
    {
        try
        {
            String source = ServletUtils.getRequest().getHeader(SecurityConstants.FROM_SOURCE);
            return StringUtils.equals(SecurityConstants.OPEN_API, source);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * 对一个Method对象进行注解检查
     */
    public void checkMethodAnnotation(Method method)
    {
        // 校验 @RequiresLogin 注解
        RequiresLogin requiresLogin = method.getAnnotation(RequiresLogin.class);
        if (requiresLogin != null)
        {
            AuthUtil.checkLogin();
        }

        // 校验 @RequiresRoles 注解
        RequiresRoles requiresRoles = method.getAnnotation(RequiresRoles.class);
        if (requiresRoles != null)
        {
            AuthUtil.checkRole(requiresRoles);
        }

        // 校验 @RequiresPermissions 注解
        RequiresPermissions requiresPermissions = method.getAnnotation(RequiresPermissions.class);
        if (requiresPermissions != null)
        {
            AuthUtil.checkPermi(requiresPermissions);
        }
    }
}
