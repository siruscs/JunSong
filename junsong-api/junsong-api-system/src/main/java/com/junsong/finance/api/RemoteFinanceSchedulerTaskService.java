package com.junsong.finance.api;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.domain.R21TaskResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * R21 财务调度任务远程调用接口（system → finance）。
 */
@FeignClient(contextId = "remoteFinanceSchedulerTaskService", value = ServiceNameConstants.FINANCE_SERVICE)
public interface RemoteFinanceSchedulerTaskService
{
    @PostMapping("/finance/inner/scheduler/cashflow-snapshot")
    R<R21TaskResult> cashflowSnapshot(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @PostMapping("/finance/inner/scheduler/stock-snapshot")
    R<R21TaskResult> stockSnapshot(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @PostMapping("/finance/inner/scheduler/memo-draft")
    R<R21TaskResult> memoDraft(@RequestParam("periodType") String periodType,
                               @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
