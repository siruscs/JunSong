package com.junsong.finance.domain.vo;

import java.util.List;

/**
 * 库存盘点任务创建请求。
 *
 * 业务规则：
 * - takeNo：租户内唯一，重复拒绝
 * - deptId：必须在当前用户授权部门集合内
 * - scopeType：SELECTED_PRODUCTS（按指定商品）/ FULL_DEPT（全店盘点）
 * - productIds：SELECTED_PRODUCTS 时必填且非空；FULL_DEPT 时忽略
 * - counterUserId：盘点人，必填
 * - recountUserId：复盘人，可选（提交时若触发阈值复盘且未指定则报错）
 *
 * @author junsong
 */
public class StocktakeCreateRequest {

    private String takeNo;
    private Long deptId;
    private String scopeType;
    private List<Long> productIds;
    private Long counterUserId;
    private Long recountUserId;
    private String remark;

    public String getTakeNo() { return takeNo; }
    public void setTakeNo(String takeNo) { this.takeNo = takeNo; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }

    public List<Long> getProductIds() { return productIds; }
    public void setProductIds(List<Long> productIds) { this.productIds = productIds; }

    public Long getCounterUserId() { return counterUserId; }
    public void setCounterUserId(Long counterUserId) { this.counterUserId = counterUserId; }

    public Long getRecountUserId() { return recountUserId; }
    public void setRecountUserId(Long recountUserId) { this.recountUserId = recountUserId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
