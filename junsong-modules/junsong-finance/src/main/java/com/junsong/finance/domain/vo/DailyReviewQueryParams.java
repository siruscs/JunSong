package com.junsong.finance.domain.vo;

import java.util.List;

/**
 * 每日/周复盘查询参数。
 * R8-A/R8-F: reviewDate 为空表示今天；deptId 可选，遵守授权门店。
 */
public class DailyReviewQueryParams {

    /** 门店ID（可选，非管理员只能传授权门店） */
    private Long deptId;

    /** 复盘日期 yyyy-MM-dd，空表示今天 */
    private String reviewDate;

    /** 门店ID列表（兼容多门店，与 deptId 二选一） */
    private List<Long> deptIds;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getReviewDate() { return reviewDate; }
    public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }

    public List<Long> getDeptIds() { return deptIds; }
    public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds; }
}
