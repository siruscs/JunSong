package com.junsong.finance.service;

import com.junsong.finance.domain.vo.DailyReviewBoardVO;
import com.junsong.finance.domain.vo.DailyReviewQueryParams;
import com.junsong.finance.domain.vo.WeeklyMemoVO;
import com.junsong.finance.domain.vo.WeeklyReviewBoardVO;

/**
 * 每日经营复盘 Service
 * R8-A: 聚合当日销售/实收/费用/净现金流/待办/关注项。
 * R8-F: 周复盘含上周对比、环比变化率、完成任务数、周总结/下周重点。
 * R10-F: 周经营纪要。
 *
 * @author junsong
 */
public interface IDailyReviewBoardService {

    /**
     * 获取每日经营复盘看板
     */
    DailyReviewBoardVO getDailyReviewBoard(DailyReviewQueryParams params);

    /**
     * 获取每周经营复盘看板
     */
    WeeklyReviewBoardVO getWeeklyReviewBoard(DailyReviewQueryParams params);

    /**
     * R10-F: 获取周经营纪要
     */
    WeeklyMemoVO getWeeklyMemo(DailyReviewQueryParams params);
}
