package com.junsong.finance.domain.vo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 经营库存报表查询参数。
 *
 * <p>租户隔离由服务层注入 tenantId；deptIds 为当前用户授权可见的门店集合。
 * 日期区间为闭区间 [startDate, endDate]，Mapper 内部转换为半开区间
 * [startDate, endDate+1day) 以避免对 create_time 使用 DATE() 函数。</p>
 *
 * @author junsong
 */
public class StockReportQuery {

    /** 授权门店ID集合（空表示不按门店过滤，由服务层保证已授权） */
    private List<Long> deptIds;

    /** 区间开始日期（含） */
    private LocalDate startDate;

    /** 区间结束日期（含） */
    private LocalDate endDate;

    /** 商品编码/名称模糊搜索 */
    private String keyword;

    /** 库存状态过滤：NORMAL/LOW_STOCK/ZERO_STOCK/NEGATIVE_STOCK/STALE/SNAPSHOT_ANOMALY */
    private String status;

    /** 页码（1基） */
    private Integer pageNum;

    /** 每页大小（1..200，默认20） */
    private Integer pageSize;

    public List<Long> getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(List<Long> deptIds) {
        this.deptIds = deptIds;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 校验并填充默认值：pageNum 默认 1，pageSize 默认 20 且不超过 200；
     * 日期区间最长 366 天。
     */
    public void validate() {
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 200) {
            pageSize = 200;
        }
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (startDate != null && endDate != null) {
            long days = ChronoUnit.DAYS.between(startDate, endDate);
            if (days > 366) {
                throw new IllegalArgumentException("日期区间最长366天");
            }
        }
    }
}
