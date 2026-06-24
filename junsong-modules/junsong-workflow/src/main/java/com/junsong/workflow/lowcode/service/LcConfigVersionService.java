package com.junsong.workflow.lowcode.service;

import com.junsong.workflow.lowcode.domain.LcBizConfigSnapshot;
import java.util.List;

public interface LcConfigVersionService
{
    /** 发布当前草稿配置为新版本 */
    LcBizConfigSnapshot publish(String bizCode, String publishRemark, String operator);

    /** 回滚到指定版本（覆盖当前草稿） */
    void rollback(String bizCode, Integer versionNo, String operator);

    /** 查询历史发布版本列表 */
    List<LcBizConfigSnapshot> listHistory(String bizCode);

    /** 读取指定版本快照 */
    LcBizConfigSnapshot getSnapshot(String bizCode, Integer versionNo);

    /** 读取最新已发布快照的配置 JSON（运行态使用） */
    String getLatestPublishedConfigJson(String bizCode);
}
