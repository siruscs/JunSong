package com.junsong.common.core.idempotency;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * 幂等指纹参数提取器。
 *
 * 正确识别业务参数，排除非业务上下文参数：
 * - 优先识别 @RequestBody 标注的参数
 * - 排除 @RequestHeader、@PathVariable、HttpServletRequest/Response、Authentication 等
 * - 多个业务参数时组合指纹
 * - 无 @RequestBody 时使用排除法提取剩余业务参数
 *
 * 解决原 args[0] 取错问题：当方法签名为
 *   foo(@RequestHeader("X-Idempotency-Key") String key, @RequestBody Req body)
 * 时，args[0] 是 key 而非 body，导致不同请求体算出相同指纹。
 *
 * @author junsong
 */
public final class IdempotencyFingerprintExtractor {

    private IdempotencyFingerprintExtractor() {}

    /**
     * 非业务参数类型（直接排除）。
     * 注意：junsong-common-core 不依赖 spring-security，
     * Authentication 等安全上下文通过类名匹配排除。
     */
    private static final Class<?>[] NON_BUSINESS_TYPES = {
            HttpServletRequest.class,
            HttpServletResponse.class,
            Model.class,
            BindingResult.class,
            java.security.Principal.class,
            org.springframework.web.context.request.WebRequest.class,
            org.springframework.web.context.request.NativeWebRequest.class,
    };

    /**
     * 需要排除的安全上下文类名（通过类名匹配，避免硬依赖）。
     */
    private static final String[] EXCLUDED_CLASS_NAMES = {
            "org.springframework.security.core.Authentication",
            "org.springframework.security.core.context.SecurityContext",
            "org.springframework.security.authentication.AbstractAuthenticationToken",
    };

    /**
     * 从切点参数中提取业务指纹。
     *
     * 策略：
     * 1. 单遍扫描参数，分类收集：
     *    - @RequestBody 参数（通常 0 或 1 个，作为主请求体）
     *    - @PathVariable 参数（路径变量，如资源 ID、分类 ID）
     *    - @RequestParam 参数（排除分页参数 pageNum/pageSize 等）
     *    - 无注解的复杂对象（隐式 @ModelAttribute 表单）
     * 2. 排除 @RequestHeader、HttpServletRequest/Response、Authentication 等
     * 3. 组合指纹：
     *    - 仅有 @RequestBody：直接计算
     *    - @RequestBody + 其他业务参数（PathVariable/RequestParam）：组合指纹
     *      （确保不同资源 ID + 相同 body 产生不同指纹）
     *    - 多个业务参数：组合指纹
     *
     * @param point AOP 切点
     * @return 指纹字符串
     */
    public static String extract(JoinPoint point) {
        Object[] args = point.getArgs();
        if (args == null || args.length == 0) {
            return IdempotencyFingerprint.compute(null);
        }

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();

        List<Object> businessArgs = new ArrayList<>();
        Object requestBodyArg = null;
        boolean multipleRequestBody = false;

        // 单遍扫描：分类收集所有业务参数
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            Parameter param = parameters[i];
            Object arg = args[i];

            // 跳过 null 值
            if (arg == null) {
                continue;
            }

            // 排除 @RequestHeader
            if (param.isAnnotationPresent(RequestHeader.class)) {
                continue;
            }

            // 排除非业务类型（HttpServletRequest/Response 等）
            if (isNonBusinessType(param.getType())) {
                continue;
            }

            // 排除安全上下文对象（通过类名匹配，避免硬依赖 spring-security）
            if (isAuthenticationOrSecurityContext(arg)) {
                continue;
            }

            // 收集 @RequestBody（主请求体，通常 0 或 1 个）
            RequestBody requestBody = param.getAnnotation(RequestBody.class);
            if (requestBody != null) {
                if (requestBodyArg == null) {
                    requestBodyArg = arg;
                } else {
                    // 多个 @RequestBody：作为业务参数加入列表
                    multipleRequestBody = true;
                    businessArgs.add(arg);
                }
                continue;
            }

            // 收集 @RequestParam（排除分页参数）
            RequestParam requestParam = param.getAnnotation(RequestParam.class);
            if (requestParam != null) {
                String paramName = requestParam.value().isEmpty() ? param.getName() : requestParam.value();
                if (isPaginationParam(paramName)) {
                    continue;
                }
                businessArgs.add(arg);
                continue;
            }

            // 收集 @PathVariable（路径变量，如资源 ID、分类 ID）
            PathVariable pathVariable = param.getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                businessArgs.add(arg);
                continue;
            }

            // 无注解的复杂对象（隐式 @ModelAttribute 表单）
            // 排除简单类型（可能是未注解的 @RequestParam）
            if (!isSimpleType(param.getType())) {
                businessArgs.add(arg);
            }
        }

        // 组合指纹
        if (multipleRequestBody) {
            // 多个 @RequestBody：全部作为业务参数组合
            List<Object> combined = new ArrayList<>();
            combined.add(requestBodyArg);
            combined.addAll(businessArgs);
            return computeCombinedFingerprint(combined);
        }

        if (requestBodyArg != null) {
            if (businessArgs.isEmpty()) {
                // 仅有 @RequestBody：直接计算
                return IdempotencyFingerprint.compute(requestBodyArg);
            }
            // @RequestBody + 其他业务参数（PathVariable/RequestParam）：组合指纹
            // 确保不同资源 ID + 相同 body 产生不同指纹
            List<Object> combined = new ArrayList<>();
            combined.add(requestBodyArg);
            combined.addAll(businessArgs);
            return computeCombinedFingerprint(combined);
        }

        // 无 @RequestBody：根据收集的业务参数计算指纹
        if (businessArgs.isEmpty()) {
            return IdempotencyFingerprint.compute(null);
        }
        if (businessArgs.size() == 1) {
            return IdempotencyFingerprint.compute(businessArgs.get(0));
        }
        return computeCombinedFingerprint(businessArgs);
    }

    /**
     * 判断是否为非业务类型。
     */
    private static boolean isNonBusinessType(Class<?> type) {
        for (Class<?> nonBusiness : NON_BUSINESS_TYPES) {
            if (nonBusiness.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为 Spring Security 上下文对象。
     *
     * 通过遍历继承链匹配类名，避免 junsong-common-core 硬依赖 spring-security。
     * 排除范围包括：
     * - org.springframework.security.core.Authentication 及其所有实现类（如 UsernamePasswordAuthenticationToken）
     * - org.springframework.security.core.context.SecurityContext 及其实现类
     * - org.springframework.security.authentication.AbstractAuthenticationToken 及其子类
     */
    private static boolean isAuthenticationOrSecurityContext(Object arg) {
        if (arg == null) return false;
        Class<?> clazz = arg.getClass();
        while (clazz != null && clazz != Object.class) {
            String className = clazz.getName();
            for (String excluded : EXCLUDED_CLASS_NAMES) {
                if (className.equals(excluded)) {
                    return true;
                }
            }
            // 检查直接实现的接口（如 Authentication 是接口）
            for (Class<?> iface : clazz.getInterfaces()) {
                if (isExcludedInterface(iface, EXCLUDED_CLASS_NAMES)) {
                    return true;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    /**
     * 递归检查接口是否在排除列表中（接口可继承父接口）。
     */
    private static boolean isExcludedInterface(Class<?> iface, String[] excludedNames) {
        String name = iface.getName();
        for (String excluded : excludedNames) {
            if (name.equals(excluded)) {
                return true;
            }
        }
        for (Class<?> parent : iface.getInterfaces()) {
            if (isExcludedInterface(parent, excludedNames)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为简单类型。
     */
    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Integer.class || type == int.class
                || type == Long.class || type == long.class
                || type == Double.class || type == double.class
                || type == Float.class || type == float.class
                || type == Boolean.class || type == boolean.class
                || type == Short.class || type == short.class
                || type == Byte.class || type == byte.class
                || type == Character.class || type == char.class
                || type.isEnum();
    }

    /**
     * 判断是否为分页参数（排除）。
     */
    private static boolean isPaginationParam(String paramName) {
        if (paramName == null) return false;
        String lower = paramName.toLowerCase();
        return lower.equals("pagenum")
                || lower.equals("pagesize")
                || lower.equals("page")
                || lower.equals("size")
                || lower.equals("offset")
                || lower.equals("limit")
                || lower.equals("orderby")
                || lower.equals("sort");
    }

    /**
     * 组合多个业务参数的指纹。
     */
    private static String computeCombinedFingerprint(List<Object> businessArgs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < businessArgs.size(); i++) {
            if (i > 0) {
                sb.append("\n---\n");
            }
            sb.append(IdempotencyFingerprint.compute(businessArgs.get(i)));
        }
        return IdempotencyFingerprint.compute(sb.toString());
    }
}
