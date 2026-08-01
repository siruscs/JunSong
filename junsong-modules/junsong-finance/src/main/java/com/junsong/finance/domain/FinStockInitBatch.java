package com.junsong.finance.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 期初库存批次头表 finance_stock_init_batch。
 *
 * 状态机：DRAFT → VALIDATED → SUBMITTED → APPROVED → POSTED
 *
 * 与普通盘点（finance_stocktake）相互独立：
 * - 无复盘阈值规则
 * - 无盲盘/复盘流程
 * - 过账幂等键 post_idempotency_key 租户内唯一
 * - batchNo 由服务端生成（SI + 时间戳），不接受客户端传入
 *
 * @author junsong
 */
public class FinStockInitBatch extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "批次ID", cellType = ColumnType.NUMERIC)
    private Long batchId;
    private Long tenantId;
    @Excel(name = "期初批次号")
    private String batchNo;
    @Excel(name = "门店ID", cellType = ColumnType.NUMERIC)
    private Long deptId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "期初日期", width = 20, dateFormat = "yyyy-MM-dd")
    private Date initDate;
    private String adjustmentType;
    private String adjustmentDirection;
    @Excel(name = "状态")
    private String status;
    @Excel(name = "提交人")
    private String submittedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "提交时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date submittedTime;
    @Excel(name = "审批人")
    private String approvedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审批时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date approvedTime;
    @Excel(name = "过账人")
    private String postedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "过账时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date postedTime;
    private String postIdempotencyKey;
    @Excel(name = "备注")
    private String remark;
    private Integer version;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Date getInitDate() { return initDate; }
    public void setInitDate(Date initDate) { this.initDate = initDate; }

    public String getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(String adjustmentType) { this.adjustmentType = adjustmentType; }

    public String getAdjustmentDirection() { return adjustmentDirection; }
    public void setAdjustmentDirection(String adjustmentDirection) { this.adjustmentDirection = adjustmentDirection; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public Date getSubmittedTime() { return submittedTime; }
    public void setSubmittedTime(Date submittedTime) { this.submittedTime = submittedTime; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public Date getApprovedTime() { return approvedTime; }
    public void setApprovedTime(Date approvedTime) { this.approvedTime = approvedTime; }

    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

    public Date getPostedTime() { return postedTime; }
    public void setPostedTime(Date postedTime) { this.postedTime = postedTime; }

    public String getPostIdempotencyKey() { return postIdempotencyKey; }
    public void setPostIdempotencyKey(String postIdempotencyKey) { this.postIdempotencyKey = postIdempotencyKey; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
