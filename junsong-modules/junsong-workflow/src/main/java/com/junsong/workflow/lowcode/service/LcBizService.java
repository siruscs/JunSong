package com.junsong.workflow.lowcode.service;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.lowcode.domain.LcBizInstance;
import java.util.List;
import java.util.Map;

/**
 * 低代码运行态业务编排服务。
 * 提供 GENERIC 模式下的 CRUD + 流程发起/撤回/履约提交。
 */
public interface LcBizService
{
    /** 列表查询 */
    List<LcBizInstance> list(String bizCode, LcBizInstance query);

    /** 按ID查询 */
    LcBizInstance getById(String bizCode, Long id);

    /** 新增（自动生成单号，状态 DRAFT） */
    R<Long> save(String bizCode, Map<String, Object> formData);

    /** 更新 form_data */
    R<Void> update(String bizCode, Long id, Map<String, Object> formData);

    /** 删除 */
    R<Void> delete(String bizCode, Long[] ids);

    /** 提交审批（装配变量 → 发起流程 → 回写快照） */
    R<Map<String, Object>> submit(String bizCode, Long id);

    /** 撤回（终止流程 → 回写已撤回） */
    R<Void> withdraw(String bizCode, Long id);

    /** 履约提交（合并 FULFILLMENT 字段 → 完成履约任务 → 回写履约完成） */
    R<Void> fulfill(String bizCode, Long id, Map<String, Object> formData);

    /** 流程状态回写（供 SyncHandler 调用） */
    void syncStatus(String processInstanceId, String workflowStatus, String currentTaskName, String operator);
}