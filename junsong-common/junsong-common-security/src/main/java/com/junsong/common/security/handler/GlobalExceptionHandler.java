package com.junsong.common.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.junsong.common.core.constant.HttpStatus;
import com.junsong.common.core.exception.DemoModeException;
import com.junsong.common.core.exception.InnerAuthException;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.exception.auth.NotPermissionException;
import com.junsong.common.core.exception.auth.NotRoleException;
import com.junsong.common.core.text.Convert;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.core.utils.html.EscapeUtil;
import com.junsong.common.core.web.domain.AjaxResult;

/**
 * 全局异常处理器
 *
 * @author junsong
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private AjaxResult wrapTraceId(AjaxResult result)
    {
        String traceId = MDC.get("traceId");
        if (StringUtils.isNotEmpty(traceId))
        {
            result.put(AjaxResult.TRACE_ID_TAG, traceId);
        }
        return result;
    }

    /**
     * 权限码异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public AjaxResult handleNotPermissionException(NotPermissionException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',权限码校验失败'{}'", requestURI, e.getMessage());
        return wrapTraceId(AjaxResult.error(HttpStatus.FORBIDDEN, "当前操作没有权限，缺少权限码[" + e.getMessage() + "]，请联系管理员授权"));
    }

    /**
     * 角色权限异常
     */
    @ExceptionHandler(NotRoleException.class)
    public AjaxResult handleNotRoleException(NotRoleException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',角色权限校验失败'{}'", requestURI, e.getMessage());
        return wrapTraceId(AjaxResult.error(HttpStatus.FORBIDDEN, "没有访问权限，请联系管理员授权"));
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return wrapTraceId(AjaxResult.error(e.getMessage()));
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e, HttpServletRequest request)
    {
        log.error(e.getMessage(), e);
        Integer code = e.getCode();
        AjaxResult result = StringUtils.isNotNull(code) ? AjaxResult.error(code, e.getMessage()) : AjaxResult.error(e.getMessage());
        return wrapTraceId(result);
    }

    /**
     * 请求路径中缺少必需的路径变量
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求路径中缺少必需的路径变量'{}',发生系统异常.", requestURI, e);
        return wrapTraceId(AjaxResult.error(String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName())));
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        String value = Convert.toStr(e.getValue());
        if (StringUtils.isNotEmpty(value))
        {
            value = EscapeUtil.clean(value);
        }
        log.error("请求参数类型不匹配'{}',发生系统异常.", requestURI, e);
        return wrapTraceId(AjaxResult.error(String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", e.getName(), e.getRequiredType().getName(), value)));
    }

    /**
     * 唯一键冲突异常（DB 层幂等兜底）
     *
     * 当高风险业务表的幂等键唯一索引拦截到重复提交时，返回友好的业务结果而非 500 错误。
     * 幂等键冲突意味着 AOP 层（@Idempotent）未能拦截（如 Redis 故障、极端并发），
     * DB 唯一索引作为最后一道防线阻止了重复写入。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public AjaxResult handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        String message = e.getMessage() != null ? e.getMessage() : "";
        // 幂等键唯一索引名称
        boolean isIdempotencyConflict = message.contains("uk_stock_ledger_idempotency_key")
                || message.contains("uk_accounting_period_carry_forward_key")
                || message.contains("uk_cost_accounting_idempotency_key")
                || message.contains("uk_investor_payment_idempotency_key")
                || message.contains("uk_reverse_idempotency_key")
                || message.contains("uk_idempotency_tenant_scene_key")
                || message.contains("idempotency_key")
                || message.contains("reverse_idempotency_key")
                || message.contains("carry_forward_idempotency_key");
        if (isIdempotencyConflict)
        {
            log.warn("请求地址'{}',幂等键唯一索引拦截重复提交（DB 层兜底生效）。", requestURI);
            return wrapTraceId(AjaxResult.error(HttpStatus.CONFLICT, "操作已处理，请勿重复提交"));
        }
        // 非幂等键的唯一键冲突（如业务编码冲突），由 Service 层重试逻辑处理
        log.error("请求地址'{}',唯一键冲突'{}'", requestURI, message);
        return wrapTraceId(AjaxResult.error("数据唯一性冲突，请稍后重试"));
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return wrapTraceId(AjaxResult.error(e.getMessage()));
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return wrapTraceId(AjaxResult.error(e.getMessage()));
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e)
    {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return wrapTraceId(AjaxResult.error(message));
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e)
    {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return wrapTraceId(AjaxResult.error(message));
    }

    /**
     * 内部认证异常
     */
    @ExceptionHandler(InnerAuthException.class)
    public AjaxResult handleInnerAuthException(InnerAuthException e)
    {
        return wrapTraceId(AjaxResult.error(e.getMessage()));
    }

    /**
     * 演示模式异常
     */
    @ExceptionHandler(DemoModeException.class)
    public AjaxResult handleDemoModeException(DemoModeException e)
    {
        return wrapTraceId(AjaxResult.error("演示模式，不允许操作"));
    }
}
