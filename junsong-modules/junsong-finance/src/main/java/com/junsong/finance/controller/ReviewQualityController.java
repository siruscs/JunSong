package com.junsong.finance.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.vo.ReviewQualityQueryParams;
import com.junsong.finance.service.IReviewQualityService;

/**
 * 复盘质量看板 Controller
 */
@RestController
@RequestMapping("/review-quality")
public class ReviewQualityController extends BaseController {

    private final IReviewQualityService service;

    public ReviewQualityController(IReviewQualityService service) {
        this.service = service;
    }

    @RequiresPermissions("finance:reviewQuality:view")
    @PostMapping("/dashboard")
    public AjaxResult dashboard(@RequestBody ReviewQualityQueryParams params) {
        return AjaxResult.success(service.getDashboard(params));
    }
}
