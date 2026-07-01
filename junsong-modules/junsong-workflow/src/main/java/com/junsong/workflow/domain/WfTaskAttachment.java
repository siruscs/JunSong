package com.junsong.workflow.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class WfTaskAttachment {
    private Long id;
    private String taskId;
    private String processInstanceId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String uploadUser;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date uploadTime;
    private String actionType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getUploadUser() { return uploadUser; }
    public void setUploadUser(String uploadUser) { this.uploadUser = uploadUser; }
    public Date getUploadTime() { return uploadTime; }
    public void setUploadTime(Date uploadTime) { this.uploadTime = uploadTime; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
}
