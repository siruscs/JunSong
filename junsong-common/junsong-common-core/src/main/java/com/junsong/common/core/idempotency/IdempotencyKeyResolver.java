package com.junsong.common.core.idempotency;

import org.aspectj.lang.JoinPoint;

/**
 * 幂等键解析器接口。
 *
 * 默认实现：从请求头 X-Idempotency-Key 读取。
 * 特殊接口（如批量导入、工作流动作）可自定义解析器，从业务键和版本号生成。
 *
 * @author junsong
 */
public interface IdempotencyKeyResolver {

    /**
     * 解析幂等键。
     *
     * @param point AOP 切点
     * @param idempotent 注解配置
     * @return 幂等键，null 表示未提供
     */
    String resolve(JoinPoint point, Idempotent idempotent);
}
