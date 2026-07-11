package com.junsong.finance.domain.vo;

import java.util.ArrayList;
import java.util.List;

public class ReceivableCollectionSyncParams {
    private Long deptId;
    private List<Long> deptIds = new ArrayList<>();
    private String collectionStatus;
    private String ageBucket;
    private String priorityLevel;
    private Integer minAgeDays;
    private String keyword;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public List<Long> getDeptIds() { return deptIds; }
    public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds == null ? new ArrayList<>() : deptIds; }
    public String getCollectionStatus() { return collectionStatus; }
    public void setCollectionStatus(String collectionStatus) { this.collectionStatus = collectionStatus; }
    public String getAgeBucket() { return ageBucket; }
    public void setAgeBucket(String ageBucket) { this.ageBucket = ageBucket; }
    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }
    public Integer getMinAgeDays() { return minAgeDays; }
    public void setMinAgeDays(Integer minAgeDays) { this.minAgeDays = minAgeDays; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
