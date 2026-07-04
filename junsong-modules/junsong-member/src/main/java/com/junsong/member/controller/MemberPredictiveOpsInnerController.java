package com.junsong.member.controller;

import java.util.Collections;
import java.util.List;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.member.api.MemberActionPredictionQuery;
import com.junsong.member.domain.vo.MemberActionPredictionVO;
import com.junsong.member.service.IMemberActionPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * R24 会员动作预测内部接口。
 *
 * <p>仅供 finance 模块经由 Feign 调用，禁止外部直接访问。
 * 使用 {@code @InnerAuth} 限定内部调用。</p>
 */
@RestController
@RequestMapping("/member/inner/predictive-ops")
public class MemberPredictiveOpsInnerController {

    @Autowired
    private IMemberActionPredictionService memberActionPredictionService;

    /**
     * 会员动作预测信号列表（用于 finance 聚合展示）。
     */
    @InnerAuth
    @PostMapping("/action-predictions")
    public R<List<MemberActionPredictionVO>> listActionPredictions(
            @RequestBody MemberActionPredictionQuery query,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source) {
        if (query == null) {
            return R.ok(Collections.emptyList());
        }
        List<MemberActionPredictionVO> list = memberActionPredictionService.listActionPredictions(
                query.getDeptId(), query.getWindowDays(), query.getActionType());
        return R.ok(list);
    }
}
