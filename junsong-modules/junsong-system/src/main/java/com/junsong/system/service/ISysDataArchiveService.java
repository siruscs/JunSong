package com.junsong.system.service;

import java.util.List;
import com.junsong.system.domain.SysDataArchiveRun;
import com.junsong.system.domain.SysDataRetentionPolicy;
import com.junsong.system.domain.vo.ArchivePreviewVO;

/**
 * R25数据归档 服务层
 */
public interface ISysDataArchiveService
{
    /**
     * 预览归档候选数据量
     */
    ArchivePreviewVO previewArchive(String tableName);

    /**
     * 执行归档（dryRun=true 仅为试运行记录）
     */
    SysDataArchiveRun runArchive(String tableName, boolean dryRun);

    /**
     * 查询全部留存策略
     */
    List<SysDataRetentionPolicy> listPolicies();
}
