package com.junsong.finance.domain.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExpenseUnverifyVO
{
    @NotBlank(message = "请求编号不能为空")
    @Size(max = 64, message = "请求编号长度不能超过64个字符")
    private String requestId;
    @NotBlank(message = "反核销原因不能为空")
    @Size(max = 500, message = "反核销原因长度不能超过500个字符")
    private String reason;
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
