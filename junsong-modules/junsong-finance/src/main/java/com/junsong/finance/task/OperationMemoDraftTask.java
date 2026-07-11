package com.junsong.finance.task;

import com.junsong.finance.domain.vo.DailyReviewBoardVO;
import com.junsong.finance.domain.vo.DailyReviewQueryParams;
import com.junsong.finance.domain.vo.WeeklyMemoVO;
import com.junsong.finance.service.IDailyReviewBoardService;
import com.junsong.system.api.domain.R21TaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * R21 日/周经营纪要草稿任务。
 * 复用 R8/R10 IDailyReviewBoardService，只生成草稿摘要，不发送消息。
 * 手动触发可传 periodType=DAILY/WEEKLY。
 */
@Component
public class OperationMemoDraftTask
{
    public static final String JOB_CODE = "R21_OPERATION_MEMO_DRAFT";
    public static final String JOB_NAME = "经营纪要草稿任务";

    @Autowired
    private IDailyReviewBoardService dailyReviewBoardService;

    /**
     * 执行经营纪要草稿生成。
     *
     * @param periodType DAILY 或 WEEKLY
     * @return 任务执行结果
     */
    public R21TaskResult execute(String periodType)
    {
        try {
            DailyReviewQueryParams params = new DailyReviewQueryParams();
            String summary;
            if ("WEEKLY".equalsIgnoreCase(periodType)) {
                WeeklyMemoVO memo = dailyReviewBoardService.getWeeklyMemo(params);
                summary = "Weekly memo draft generated: " + (memo != null ? "ok" : "empty");
            } else {
                DailyReviewBoardVO board = dailyReviewBoardService.getDailyReviewBoard(params);
                summary = "Daily review board draft generated: " + (board != null ? "ok" : "empty");
            }
            return R21TaskResult.success(1, summary);
        } catch (Exception ex) {
            return R21TaskResult.failed(ex);
        }
    }
}
