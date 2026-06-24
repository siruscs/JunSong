package com.junsong.workflow.lowcode.validator;

import java.util.Map;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.workflow.lowcode.domain.LcBizObject;

/**
 * 低代码提交前校验器。
 *
 * <p>校验器配置存储在 {@code lc_biz_object.submit_validators}（JSON 数组），
 * 每个元素形如：
 * <pre>
 * {
 *   "type": "GEO_DEDUP",
 *   "geoField": "store_location",
 *   "radiusKm": 1.5,
 *   "message": "周边1.5公里内已存在门店「${nearestName}」(距离${distance}米)"
 * }
 * </pre>
 *
 * <p>实现类需用 {@code @Component} 注册，并通过 {@link #type()} 声明自身处理的类型；
 * 框架按 type 字段分发到对应实现。
 *
 * @author junsong
 */
public interface LcSubmitValidator
{
    /**
     * 校验器类型标识（与配置 JSON 中的 {@code type} 字段匹配）。
     *
     * @return 类型字符串，如 "GEO_DEDUP"
     */
    String type();

    /**
     * 执行校验。校验失败应抛出 {@link ServiceException}，由调用方统一捕获并回传前端。
     *
     * @param bizObject      业务对象（含 bizCode / bizName 等元信息）
     * @param formData       表单数据（提交时的原始数据）
     * @param validatorConfig 当前校验器的配置项（已从 JSON 解析为 Map）
     * @throws ServiceException 校验失败时抛出
     */
    void validate(LcBizObject bizObject, Map<String, Object> formData, Map<String, Object> validatorConfig)
            throws ServiceException;
}
