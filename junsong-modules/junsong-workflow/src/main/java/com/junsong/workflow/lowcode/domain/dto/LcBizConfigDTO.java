package com.junsong.workflow.lowcode.domain.dto;

import com.junsong.workflow.lowcode.domain.LcBizAction;
import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.LcBizNodeTimer;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import com.junsong.workflow.lowcode.domain.LcBizPostAction;
import java.util.List;

/**
 * 低代码业务配置聚合体：一次性承载 1 个业务对象及其全部字段/页面schema/节点处理人/分支规则。
 * 用于可视化配置后台的事务性保存与整体读取。
 */
public class LcBizConfigDTO
{
    private String modelVersion;
    private Integer schemaVersion;
    private Integer sourceVersion;
    private String timezone;
    private String publishRemark;
    private LcBizObject bizObject;
    private List<LcBizField> fields;
    private List<LcBizPageSchema> pageSchemas;
    private List<LcBizNodeAssignee> nodeAssignees;
    private List<LcBizBranchRule> branchRules;
    private List<LcBizAction> actions;
    private List<LcBizPostAction> postActions;
    private List<LcBizNodeTimer> nodeTimers;

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public Integer getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(Integer schemaVersion) { this.schemaVersion = schemaVersion; }
    public Integer getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(Integer sourceVersion) { this.sourceVersion = sourceVersion; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getPublishRemark() { return publishRemark; }
    public void setPublishRemark(String publishRemark) { this.publishRemark = publishRemark; }

    public LcBizObject getBizObject() { return bizObject; }
    public void setBizObject(LcBizObject bizObject) { this.bizObject = bizObject; }
    public List<LcBizField> getFields() { return fields; }
    public void setFields(List<LcBizField> fields) { this.fields = fields; }
    public List<LcBizPageSchema> getPageSchemas() { return pageSchemas; }
    public void setPageSchemas(List<LcBizPageSchema> pageSchemas) { this.pageSchemas = pageSchemas; }
    public List<LcBizNodeAssignee> getNodeAssignees() { return nodeAssignees; }
    public void setNodeAssignees(List<LcBizNodeAssignee> nodeAssignees) { this.nodeAssignees = nodeAssignees; }
    public List<LcBizBranchRule> getBranchRules() { return branchRules; }
    public void setBranchRules(List<LcBizBranchRule> branchRules) { this.branchRules = branchRules; }
    public List<LcBizAction> getActions() { return actions; }
    public void setActions(List<LcBizAction> actions) { this.actions = actions; }
    public List<LcBizPostAction> getPostActions() { return postActions; }
    public void setPostActions(List<LcBizPostAction> postActions) { this.postActions = postActions; }
    public List<LcBizNodeTimer> getNodeTimers() { return nodeTimers; }
    public void setNodeTimers(List<LcBizNodeTimer> nodeTimers) { this.nodeTimers = nodeTimers; }
}
