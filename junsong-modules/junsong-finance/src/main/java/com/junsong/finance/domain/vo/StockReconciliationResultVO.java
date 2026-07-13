package com.junsong.finance.domain.vo;

import java.util.List;
import java.util.Map;

/**
 * 库存对账结果VO。
 *
 * <p>包含全部异常明细行、异常总数、按异常代码分组的计数，以及整体状态。</p>
 *
 * <p>状态取值：
 * <ul>
 *   <li>HEALTHY - 无异常</li>
 *   <li>WARN - 存在异常，需人工核查</li>
 *   <li>BLOCKED - 存在阻断级异常（预留，当前对账不产生 BLOCKED）</li>
 * </ul></p>
 *
 * @author junsong
 */
public class StockReconciliationResultVO {

    private List<StockReconciliationRowVO> rows;
    private int totalAnomalyCount;
    private Map<String, Integer> anomalyCounts;
    private String status;

    public List<StockReconciliationRowVO> getRows() { return rows; }
    public void setRows(List<StockReconciliationRowVO> rows) { this.rows = rows; }

    public int getTotalAnomalyCount() { return totalAnomalyCount; }
    public void setTotalAnomalyCount(int totalAnomalyCount) { this.totalAnomalyCount = totalAnomalyCount; }

    public Map<String, Integer> getAnomalyCounts() { return anomalyCounts; }
    public void setAnomalyCounts(Map<String, Integer> anomalyCounts) { this.anomalyCounts = anomalyCounts; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
