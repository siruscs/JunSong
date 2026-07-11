package com.junsong.finance.domain.vo;

import java.util.List;

/**
 * 库存底座健康检查结果VO。
 *
 * @author junsong
 */
public class StockHealthVO {

    private String status;
    private Long ledgerCount;
    private Long snapshotCount;
    private Long negativeStockProductCount;
    private Long productsWithoutLedgerCount;
    private Long snapshotMissingCount;
    private Long snapshotMismatchCount;
    private List<StockHealthIssueVO> issues;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getLedgerCount() { return ledgerCount; }
    public void setLedgerCount(Long ledgerCount) { this.ledgerCount = ledgerCount; }

    public Long getSnapshotCount() { return snapshotCount; }
    public void setSnapshotCount(Long snapshotCount) { this.snapshotCount = snapshotCount; }

    public Long getNegativeStockProductCount() { return negativeStockProductCount; }
    public void setNegativeStockProductCount(Long negativeStockProductCount) {
        this.negativeStockProductCount = negativeStockProductCount;
    }

    public Long getProductsWithoutLedgerCount() { return productsWithoutLedgerCount; }
    public void setProductsWithoutLedgerCount(Long productsWithoutLedgerCount) {
        this.productsWithoutLedgerCount = productsWithoutLedgerCount;
    }

    public Long getSnapshotMissingCount() { return snapshotMissingCount; }
    public void setSnapshotMissingCount(Long snapshotMissingCount) {
        this.snapshotMissingCount = snapshotMissingCount;
    }

    public Long getSnapshotMismatchCount() { return snapshotMismatchCount; }
    public void setSnapshotMismatchCount(Long snapshotMismatchCount) {
        this.snapshotMismatchCount = snapshotMismatchCount;
    }

    public List<StockHealthIssueVO> getIssues() { return issues; }
    public void setIssues(List<StockHealthIssueVO> issues) { this.issues = issues; }
}