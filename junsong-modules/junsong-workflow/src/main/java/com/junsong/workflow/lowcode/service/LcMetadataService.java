package com.junsong.workflow.lowcode.service;

import com.junsong.workflow.lowcode.domain.LcBizAction;
import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.LcBizNodeTimer;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import com.junsong.workflow.lowcode.domain.dto.LcBizConfigDTO;
import java.util.List;

public interface LcMetadataService
{
    // ===== 聚合配置（可视化后台用）=====
    /** 按 bizCode 读取完整配置聚合体 */
    LcBizConfigDTO selectBizConfig(String bizCode);

    /** 事务性保存完整配置（对象 upsert + 字段/页面/处理人/分支全量替换） */
    void saveBizConfig(LcBizConfigDTO config);

    // ===== 业务对象 =====
    List<LcBizObject> selectBizObjectList(LcBizObject query);

    LcBizObject selectBizObjectById(Long id);

    LcBizObject selectBizObjectByBizCode(String bizCode);

    /** 列出所有启用流程的 GENERIC 业务对象（供通用 SyncHandler 匹配 processKey） */
    List<LcBizObject> selectGenericWorkflowObjects();

    int insertBizObject(LcBizObject bizObject);

    int updateBizObject(LcBizObject bizObject);

    int deleteBizObjectByIds(Long[] ids);

    // ===== 字段 =====
    List<LcBizField> selectFieldList(LcBizField query);

    List<LcBizField> selectFieldsByBizCode(String bizCode);

    LcBizField selectFieldById(Long id);

    int insertField(LcBizField field);

    int updateField(LcBizField field);

    int deleteFieldByIds(Long[] ids);

    // ===== 页面 Schema =====
    List<LcBizPageSchema> selectPageSchemaList(LcBizPageSchema query);

    List<LcBizPageSchema> selectPageSchemasByBizCode(String bizCode);

    LcBizPageSchema selectPageSchemaById(Long id);

    int insertPageSchema(LcBizPageSchema pageSchema);

    int updatePageSchema(LcBizPageSchema pageSchema);

    int deletePageSchemaByIds(Long[] ids);

    // ===== 节点处理人 =====
    List<LcBizNodeAssignee> selectNodeAssigneeList(LcBizNodeAssignee query);

    List<LcBizNodeAssignee> selectNodeAssigneesByBizCode(String bizCode);

    LcBizNodeAssignee selectNodeAssigneeById(Long id);

    int insertNodeAssignee(LcBizNodeAssignee nodeAssignee);

    int updateNodeAssignee(LcBizNodeAssignee nodeAssignee);

    int deleteNodeAssigneeByIds(Long[] ids);

    // ===== 分支规则 =====
    List<LcBizBranchRule> selectBranchRuleList(LcBizBranchRule query);

    List<LcBizBranchRule> selectBranchRulesByBizCode(String bizCode);

    LcBizBranchRule selectBranchRuleById(Long id);

    int insertBranchRule(LcBizBranchRule branchRule);

    int updateBranchRule(LcBizBranchRule branchRule);

    int deleteBranchRuleByIds(Long[] ids);

    // ===== 动作配置 =====
    /** 查询业务对象的动作配置（供前端动态渲染按钮） */
    List<LcBizAction> selectBizActions(String bizCode);

    // ===== 节点定时器 =====
    List<LcBizNodeTimer> selectNodeTimerList(LcBizNodeTimer query);

    List<LcBizNodeTimer> selectNodeTimersByBizCode(String bizCode);

    LcBizNodeTimer selectNodeTimerById(Long id);

    int insertNodeTimer(LcBizNodeTimer nodeTimer);

    int updateNodeTimer(LcBizNodeTimer nodeTimer);

    int deleteNodeTimerByIds(Long[] ids);
}
