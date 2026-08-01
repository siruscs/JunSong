package com.junsong.finance.domain.vo;

import java.util.Date;
import java.util.List;

/**
 * 期初库存批次创建请求。
 *
 * 业务规则：
 * - deptId：必须在当前用户授权部门集合内（必填）
 * - initDate：期初日期（必填）
 * - items：期初明细行（必填且非空）
 * - remark：备注（可选）
 *
 * 安全契约：
 * - batchNo 不接受客户端传入，由服务端生成（SI + 时间戳）
 * - 商品必须归属于 deptId
 *
 * @author junsong
 */
public class StockInitCreateRequest {

    private Long deptId;
    private Date initDate;
    private Date adjustmentDate;
    private String adjustmentType;
    private String adjustmentDirection;
    private List<StockInitItemInput> items;
    private String remark;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Date getInitDate() { return initDate; }
    public void setInitDate(Date initDate) { this.initDate = initDate; }
    public Date getAdjustmentDate() { return adjustmentDate; }
    public void setAdjustmentDate(Date adjustmentDate) { this.adjustmentDate = adjustmentDate; }

    public String getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(String adjustmentType) { this.adjustmentType = adjustmentType; }

    public String getAdjustmentDirection() { return adjustmentDirection; }
    public void setAdjustmentDirection(String adjustmentDirection) { this.adjustmentDirection = adjustmentDirection; }

    public List<StockInitItemInput> getItems() { return items; }
    public void setItems(List<StockInitItemInput> items) { this.items = items; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
