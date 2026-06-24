package com.junsong.workflow.lowcode.service.impl;

import com.alibaba.fastjson2.JSON;
import com.junsong.workflow.lowcode.domain.LcBizAction;
import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import com.junsong.workflow.lowcode.domain.LcBizConfigSnapshot;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.LcBizNodeTimer;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import com.junsong.workflow.lowcode.domain.LcBizPostAction;
import com.junsong.workflow.lowcode.domain.dto.LcBizConfigDTO;
import com.junsong.workflow.lowcode.mapper.LcBizConfigSnapshotMapper;
import com.junsong.workflow.lowcode.mapper.LcBizObjectMapper;
import com.junsong.workflow.lowcode.service.LcConfigVersionService;
import com.junsong.workflow.lowcode.service.LcMetadataService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LcConfigVersionServiceImpl implements LcConfigVersionService
{
    private static final Logger log = LoggerFactory.getLogger(LcConfigVersionServiceImpl.class);

    @Autowired
    private LcBizConfigSnapshotMapper snapshotMapper;

    @Autowired
    private LcBizObjectMapper lcBizObjectMapper;

    @Autowired
    private LcMetadataService metadataService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LcBizConfigSnapshot publish(String bizCode, String publishRemark, String operator)
    {
        // 1. 读取当前草稿配置
        LcBizConfigDTO config = metadataService.selectBizConfig(bizCode);
        if (config == null || config.getBizObject() == null)
        {
            throw new IllegalArgumentException("业务配置不存在: " + bizCode);
        }

        // 2. 计算新版本号
        Integer maxVersion = snapshotMapper.selectMaxVersionByBizCode(bizCode);
        int newVersionNo = (maxVersion == null ? 0 : maxVersion) + 1;

        // 3. 序列化为 JSON
        String configJson = JSON.toJSONString(config);

        // 4. 插入快照
        LcBizConfigSnapshot snapshot = new LcBizConfigSnapshot();
        snapshot.setBizCode(bizCode);
        snapshot.setVersionNo(newVersionNo);
        snapshot.setConfigJson(configJson);
        snapshot.setStatus("PUBLISHED");
        snapshot.setPublishRemark(publishRemark);
        snapshot.setCreateBy(operator);
        snapshot.setRemark("版本 " + newVersionNo);
        snapshotMapper.insertSnapshot(snapshot);

        // 5. 更新 biz_object 状态
        LcBizObject bizObject = config.getBizObject();
        bizObject.setConfigStatus("PUBLISHED");
        bizObject.setPublishedVersion(newVersionNo);
        lcBizObjectMapper.updateLcBizObject(bizObject);

        log.info("配置发布成功: bizCode={}, version={}", bizCode, newVersionNo);
        return snapshot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollback(String bizCode, Integer versionNo, String operator)
    {
        // 1. 读取指定版本快照
        LcBizConfigSnapshot snapshot = snapshotMapper.selectByBizCodeAndVersion(bizCode, versionNo);
        if (snapshot == null)
        {
            throw new IllegalArgumentException("快照不存在: bizCode=" + bizCode + ", version=" + versionNo);
        }

        // 2. 反序列化为配置聚合体
        LcBizConfigDTO config = JSON.parseObject(snapshot.getConfigJson(), LcBizConfigDTO.class);

        // 3. 补全子表的 delFlag（快照 JSON 中不含此字段，反序列化后为 null，插入时会报错）
        if (config.getFields() != null) for (LcBizField f : config.getFields()) if (f.getDelFlag() == null) f.setDelFlag("0");
        if (config.getPageSchemas() != null) for (LcBizPageSchema s : config.getPageSchemas()) if (s.getDelFlag() == null) s.setDelFlag("0");
        if (config.getNodeAssignees() != null) for (LcBizNodeAssignee a : config.getNodeAssignees()) if (a.getDelFlag() == null) a.setDelFlag("0");
        if (config.getBranchRules() != null) for (LcBizBranchRule r : config.getBranchRules()) if (r.getDelFlag() == null) r.setDelFlag("0");
        if (config.getActions() != null) for (LcBizAction a : config.getActions()) if (a.getDelFlag() == null) a.setDelFlag("0");
        if (config.getPostActions() != null) for (LcBizPostAction p : config.getPostActions()) if (p.getDelFlag() == null) p.setDelFlag("0");
        if (config.getNodeTimers() != null) for (LcBizNodeTimer t : config.getNodeTimers()) if (t.getDelFlag() == null) t.setDelFlag("0");

        // 4. 覆盖回 7 张子表（复用现有 saveBizConfig）
        metadataService.saveBizConfig(config);

        // 5. 更新 biz_object 状态为草稿（回滚后需重新发布）
        LcBizObject bizObject = config.getBizObject();
        bizObject.setConfigStatus("DRAFT");
        lcBizObjectMapper.updateLcBizObject(bizObject);

        log.info("配置回滚成功: bizCode={}, version={}", bizCode, versionNo);
    }

    @Override
    public List<LcBizConfigSnapshot> listHistory(String bizCode)
    {
        return snapshotMapper.selectByBizCode(bizCode);
    }

    @Override
    public LcBizConfigSnapshot getSnapshot(String bizCode, Integer versionNo)
    {
        return snapshotMapper.selectByBizCodeAndVersion(bizCode, versionNo);
    }

    @Override
    public String getLatestPublishedConfigJson(String bizCode)
    {
        Integer maxVersion = snapshotMapper.selectMaxVersionByBizCode(bizCode);
        if (maxVersion == null || maxVersion == 0)
        {
            return null;
        }
        LcBizConfigSnapshot snapshot = snapshotMapper.selectByBizCodeAndVersion(bizCode, maxVersion);
        return snapshot == null ? null : snapshot.getConfigJson();
    }
}
