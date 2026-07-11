package com.junsong.finance.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.vo.DailyReviewQueryParams;
import com.junsong.finance.service.IDailyReviewBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 每日/周经营复盘 Controller.
 * R8-A/R8-F: 老板/店长每天打开首页后的行动清单。
 *
 * @author junsong
 */
@RestController
@RequestMapping("/daily-review")
public class DailyReviewBoardController extends BaseController {

    @Autowired
    private IDailyReviewBoardService dailyReviewBoardService;

    /**
     * 获取每日经营复盘看板
     */
    @RequiresPermissions("finance:dailyReview:view")
    @PostMapping("/board")
    public AjaxResult board(@RequestBody DailyReviewQueryParams params) {
        return AjaxResult.success(dailyReviewBoardService.getDailyReviewBoard(params));
    }

    /**
     * 获取每周经营复盘看板
     */
    @RequiresPermissions("finance:dailyReview:view")
    @PostMapping("/weekly-board")
    public AjaxResult weeklyBoard(@RequestBody DailyReviewQueryParams params) {
        return AjaxResult.success(dailyReviewBoardService.getWeeklyReviewBoard(params));
    }

    /**
     * R10-F: 获取周经营纪要
     */
    @RequiresPermissions("finance:dailyReview:view")
    @PostMapping("/weekly-memo")
    public AjaxResult weeklyMemo(@RequestBody DailyReviewQueryParams params) {
        return AjaxResult.success(dailyReviewBoardService.getWeeklyMemo(params));
    }
}
