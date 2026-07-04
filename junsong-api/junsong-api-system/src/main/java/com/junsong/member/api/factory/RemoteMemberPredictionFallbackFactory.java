package com.junsong.member.api.factory;

import java.util.Collections;
import java.util.List;

import com.junsong.common.core.domain.R;
import com.junsong.member.api.RemoteMemberPredictionService;
import com.junsong.member.api.MemberActionPredictionQuery;
import com.junsong.member.api.domain.MemberActionPredictionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 会员动作预测服务降级处理。
 *
 * <p>降级策略：返回空列表 + 业务码 503。调用方 (PredictiveOpsServiceImpl)
 * 应识别"会员预测服务不可用"因子并明确展示，不能伪造预测数据。</p>
 */
@Component
public class RemoteMemberPredictionFallbackFactory implements FallbackFactory<RemoteMemberPredictionService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteMemberPredictionFallbackFactory.class);

    @Override
    public RemoteMemberPredictionService create(Throwable throwable) {
        log.warn("R24 会员动作预测服务不可用: {}", throwable.getMessage());
        return new RemoteMemberPredictionService() {
            @Override
            public R<List<MemberActionPredictionItem>> listMemberActionPredictions(MemberActionPredictionQuery request, String source) {
                return R.fail(Collections.emptyList(), "会员预测服务不可用");
            }
        };
    }
}
