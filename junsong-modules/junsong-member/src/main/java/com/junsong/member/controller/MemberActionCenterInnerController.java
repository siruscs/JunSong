package com.junsong.member.controller;

import java.util.*;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.member.domain.vo.GrowthActionDashboardVO;
import com.junsong.member.domain.vo.GrowthActionQueryParams;
import com.junsong.member.domain.vo.GrowthActionRowVO;
import com.junsong.member.service.IMemberGrowthActionService;
import com.junsong.system.api.domain.ActionCenterSourceItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member/inner/action-center")
public class MemberActionCenterInnerController {
    @Autowired
    private IMemberGrowthActionService growthActionService;

    @InnerAuth
    @GetMapping("/items")
    public R<List<ActionCenterSourceItem>> listItems(@RequestHeader(SecurityConstants.FROM_SOURCE) String source) {
        List<ActionCenterSourceItem> items = new ArrayList<>();
        GrowthActionDashboardVO dashboard = growthActionService.getDashboard(new GrowthActionQueryParams());
        if (dashboard != null && dashboard.getRecentActions() != null) {
            for (GrowthActionRowVO row : dashboard.getRecentActions()) {
                ActionCenterSourceItem item = new ActionCenterSourceItem();
                item.setActionId("MEMBER_GROWTH:" + row.getActionId());
                item.setSourceType("MEMBER_GROWTH");
                item.setSourceId(String.valueOf(row.getActionId()));
                item.setTitle(row.getActionTitle() != null ? row.getActionTitle() : "会员增长动作");
                item.setDescription(row.getActionType());
                item.setPriority(row.getPressureLevel() != null ? row.getPressureLevel() : "MEDIUM");
                item.setStatus(mapGrowthStatus(row.getStatus()));
                item.setDeptId(row.getDeptId());
                item.setDeptName(row.getDeptName());
                item.setDrilldownPath("/member/growthAction");
                items.add(item);
            }
        }
        return R.ok(items);
    }

    private String mapGrowthStatus(String status) {
        if (status == null) return "PENDING";
        if ("DONE".equalsIgnoreCase(status)) return "EFFECT_PENDING";
        if ("IN_PROGRESS".equalsIgnoreCase(status)) return "IN_PROGRESS";
        if ("IGNORED".equalsIgnoreCase(status)) return "IGNORED";
        return "PENDING";
    }
}
