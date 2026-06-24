package com.junsong.workflow.lowcode.service.impl;

import com.alibaba.fastjson2.JSON;
import com.junsong.workflow.lowcode.domain.LcBizAction;
import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import com.junsong.workflow.lowcode.domain.LcBizPostAction;
import com.junsong.workflow.lowcode.domain.LcBizTemplate;
import com.junsong.workflow.lowcode.domain.dto.LcBizConfigDTO;
import com.junsong.workflow.lowcode.mapper.LcBizTemplateMapper;
import com.junsong.workflow.lowcode.service.LcConfigVersionService;
import com.junsong.workflow.lowcode.service.LcMetadataService;
import com.junsong.workflow.lowcode.service.LcTemplateService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LcTemplateServiceImpl implements LcTemplateService
{
    private static final Logger log = LoggerFactory.getLogger(LcTemplateServiceImpl.class);

    @Autowired
    private LcBizTemplateMapper templateMapper;

    @Autowired
    private LcConfigVersionService configVersionService;

    @Autowired
    private LcMetadataService metadataService;

    @Override
    public List<LcBizTemplate> listTemplates(String category)
    {
        return templateMapper.selectTemplateList(category);
    }

    @Override
    public LcBizTemplate getTemplate(Long id)
    {
        return templateMapper.selectTemplateById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LcBizTemplate saveAsTemplate(String bizCode, String templateName,
                                          String category, String description, String operator)
    {
        LcBizConfigDTO config = readEffectiveConfig(bizCode);
        if (config == null || config.getBizObject() == null)
        {
            throw new IllegalArgumentException("业务配置不存在: " + bizCode);
        }

        String templateCode = "tpl_" + bizCode + "_" + System.currentTimeMillis();
        String configJson = JSON.toJSONString(config);

        LcBizTemplate template = new LcBizTemplate();
        template.setTemplateCode(templateCode);
        template.setTemplateName(templateName);
        template.setCategory(category);
        template.setDescription(description);
        template.setProcessKey(config.getBizObject().getProcessKey());
        template.setConfigJson(configJson);
        template.setIsStarter("0");
        template.setCreateBy(operator);
        template.setRemark("从 " + bizCode + " 保存");
        templateMapper.insertTemplate(template);

        log.info("保存为模板: templateCode={}, from bizCode={}", templateCode, bizCode);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String cloneFromTemplate(Long templateId, String newBizCode,
                                     String newBizName, String operator)
    {
        LcBizTemplate template = templateMapper.selectTemplateById(templateId);
        if (template == null)
        {
            throw new IllegalArgumentException("模板不存在");
        }

        LcBizConfigDTO config = JSON.parseObject(template.getConfigJson(), LcBizConfigDTO.class);

        LcBizObject bizObject = config.getBizObject();
        bizObject.setId(null);
        bizObject.setBizCode(newBizCode);
        bizObject.setBizName(newBizName);
        if (bizObject.getPkField() == null) bizObject.setPkField("id");
        if (bizObject.getTableName() == null) bizObject.setTableName(null);
        if (bizObject.getOrderNoField() == null) bizObject.setOrderNoField("order_no");
        if (bizObject.getOrderNoPrefix() == null) bizObject.setOrderNoPrefix(newBizCode.toUpperCase());
        if (bizObject.getStatusField() == null) bizObject.setStatusField("workflow_status");
        if (bizObject.getStorageMode() == null) bizObject.setStorageMode("GENERIC");
        if (bizObject.getDelFlag() == null) bizObject.setDelFlag("0");
        if (template.getProcessKey() != null)
        {
            bizObject.setProcessKey(template.getProcessKey());
        }

        clearIds(config.getFields());
        clearIds(config.getPageSchemas());
        clearIds(config.getNodeAssignees());
        clearIds(config.getBranchRules());
        clearIds(config.getActions());
        clearIds(config.getPostActions());

        // 补全 delFlag（内联，避免泛型类型转换问题）
        if (config.getFields() != null) for (LcBizField f : config.getFields()) if (f.getDelFlag() == null) f.setDelFlag("0");
        if (config.getPageSchemas() != null) for (LcBizPageSchema s : config.getPageSchemas()) if (s.getDelFlag() == null) s.setDelFlag("0");
        if (config.getNodeAssignees() != null) for (LcBizNodeAssignee a : config.getNodeAssignees()) if (a.getDelFlag() == null) a.setDelFlag("0");
        if (config.getBranchRules() != null) for (LcBizBranchRule r : config.getBranchRules()) if (r.getDelFlag() == null) r.setDelFlag("0");
        if (config.getActions() != null) for (LcBizAction a : config.getActions()) if (a.getDelFlag() == null) a.setDelFlag("0");
        if (config.getPostActions() != null) for (LcBizPostAction p : config.getPostActions()) if (p.getDelFlag() == null) p.setDelFlag("0");

        metadataService.saveBizConfig(config);
        templateMapper.incrementUsageCount(templateId);

        log.info("从模板克隆: templateId={}, newBizCode={}", templateId, newBizCode);
        return newBizCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long[] ids)
    {
        for (Long id : ids)
        {
            LcBizTemplate t = templateMapper.selectTemplateById(id);
            if (t != null && "1".equals(t.getIsStarter()))
            {
                throw new IllegalArgumentException("内置模板不允许删除: " + t.getTemplateName());
            }
        }
        templateMapper.deleteTemplateByIds(ids);
    }

    private LcBizConfigDTO readEffectiveConfig(String bizCode)
    {
        String json = configVersionService.getLatestPublishedConfigJson(bizCode);
        if (json != null)
        {
            return JSON.parseObject(json, LcBizConfigDTO.class);
        }
        return metadataService.selectBizConfig(bizCode);
    }

    @SuppressWarnings("rawtypes")
    private void clearIds(List list)
    {
        if (list == null) return;
        for (Object item : list)
        {
            try
            {
                item.getClass().getMethod("setId", Long.class).invoke(item, (Long) null);
            }
            catch (Exception ignored) {}
        }
    }

}
