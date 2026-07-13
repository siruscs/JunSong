package com.junsong.finance.domain.vo;

import java.util.List;

/**
 * 经营库存报表复合 VO。
 *
 * <p>由汇总指标 {@link StockReportSummaryVO}、分页明细 {@link StockReportItemVO}
 * 与分页元信息组成。</p>
 *
 * @author junsong
 */
public class StockReportVO {

    /** 汇总指标 */
    private StockReportSummaryVO summary;

    /** 分页明细行 */
    private List<StockReportItemVO> items;

    /** 分页前总条数 */
    private long total;

    /** 当前页码（1基） */
    private int pageNum;

    /** 每页大小 */
    private int pageSize;

    public StockReportSummaryVO getSummary() {
        return summary;
    }

    public void setSummary(StockReportSummaryVO summary) {
        this.summary = summary;
    }

    public List<StockReportItemVO> getItems() {
        return items;
    }

    public void setItems(List<StockReportItemVO> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
