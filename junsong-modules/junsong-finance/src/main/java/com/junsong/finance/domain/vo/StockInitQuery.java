package com.junsong.finance.domain.vo;

import java.util.Date;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 期初库存批次列表查询参数。
 *
 * 所有字段可选；service 层自动叠加：
 * - tenantId = TenantContext.getTenantId()
 * - deptIds = 当前用户授权部门集合（admin 跳过）
 *
 * @author junsong
 */
public class StockInitQuery extends BaseEntity {

    private Long deptId;
    private String status;
    private String batchNo;
    private Date startDate;
    private Date endDate;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
}
