package com.junsong.member.api;

import java.util.List;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.member.api.domain.MemberActionPredictionItem;
import com.junsong.member.api.factory.RemoteMemberPredictionFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 会员动作预测远程调用接口（内部服务，R24 预测辅助 V2 使用）。
 *
 * <p>fallback 策略：返回空列表，调用方应识别"会员预测服务不可用"因子，
 * 不允许 fallback 伪造成成功预测。</p>
 */
@FeignClient(contextId = "remoteMemberPredictionService", value = ServiceNameConstants.MEMBER_SERVICE, fallbackFactory = RemoteMemberPredictionFallbackFactory.class)
public interface RemoteMemberPredictionService {

    /**
     * 获取门店/动作类型下的会员动作预测信号。
     *
     * @param request 包含 deptId、windowDays、actionType
     * @param source  内部调用来源标识
     * @return 会员动作预测信号列表
     */
    @PostMapping("/member/inner/predictive-ops/action-predictions")
    R<List<MemberActionPredictionItem>> listMemberActionPredictions(
            @RequestBody MemberActionPredictionQuery request,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
