package com.junsong.finance.api.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 盘点工作流状态同步请求。
 *
 * action 取值：
 * - SUBMIT：提交盘点时启动工作流后回写
 * - APPROVE：审批通过
 * - REJECT：审批驳回
 * - CANCEL：取消盘点任务时终止工作流
 */
public class StocktakeWorkflowSyncReq implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    private Long stocktakeId;
    private String processInstanceId;
    private String currentNode;
    private String action;
    private String businessKey;
    private Map<String, Object> formData;

    public Long getStocktakeId()
    {
        return stocktakeId;
    }

    public void setStocktakeId(Long stocktakeId)
    {
        this.stocktakeId = stocktakeId;
    }

    public String getProcessInstanceId()
    {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId)
    {
        this.processInstanceId = processInstanceId;
    }

    public String getCurrentNode()
    {
        return currentNode;
    }

    public void setCurrentNode(String currentNode)
    {
        this.currentNode = currentNode;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }
    public Map<String, Object> getFormData() { return formData; }
    public void setFormData(Map<String, Object> formData) { this.formData = formData; }
}
