package com.junsong.system.service.signal;

import java.util.Calendar;
import java.util.Date;
import org.springframework.stereotype.Component;

/**
 * 经营任务截止时间（due_time）推导工具类。
 * 按设计文档 §5 规则推导不同来源类型的截止时间。
 * 纯函数，无副作用，可测试。
 *
 * @author junsong
 */
@Component
public class OperatingTaskDueTimeCalculator
{
    /**
     * 按来源类型推导截止时间。
     *
     * @param sourceType 来源类型（如 RECEIVABLE_COLLECTION / REVIEW_TASK / NEGATIVE_STOCK 等）
     * @param occurTime 来源发生时间（为 null 时用当前时间）
     * @param sourceNextFollowTime 源下次跟进时间（仅 OVERDUE_RECEIVABLE 使用）
     * @return 推导出的截止时间
     */
    public Date calculateDueTime(String sourceType, Date occurTime, Date sourceNextFollowTime)
    {
        Date base = occurTime != null ? occurTime : new Date();

        if (sourceType == null)
        {
            return addDays(base, 7);
        }

        switch (sourceType)
        {
            case "RECEIVABLE_COLLECTION":
            case "OVERDUE_RECEIVABLE":
                // 源 nextFollowTime；为空时 occur_time + 3天
                if (sourceNextFollowTime != null)
                {
                    return sourceNextFollowTime;
                }
                return addDays(base, 3);
            case "REVIEW_TASK":
                // occur_time + 7天
                return addDays(base, 7);
            case "NEGATIVE_STOCK":
            case "LOW_STOCK":
                // occur_time + 1天（高紧急）
                return addDays(base, 1);
            case "SILENT_MEMBER_HIGH":
            case "POINTS_LIABILITY_HIGH":
                // occur_time + 14天
                return addDays(base, 14);
            default:
                // 其他/手动：occur_time + 7天（默认）
                return addDays(base, 7);
        }
    }

    /**
     * 给日期增加指定天数
     */
    private Date addDays(Date date, int days)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}
