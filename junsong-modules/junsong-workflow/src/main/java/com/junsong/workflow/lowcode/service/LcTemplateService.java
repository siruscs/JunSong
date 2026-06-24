package com.junsong.workflow.lowcode.service;

import com.junsong.workflow.lowcode.domain.LcBizTemplate;
import java.util.List;

public interface LcTemplateService
{
    /** 查询模板列表（可按分类筛选） */
    List<LcBizTemplate> listTemplates(String category);

    /** 读取模板详情（包含 configJson） */
    LcBizTemplate getTemplate(Long id);

    /** 保存当前业务配置为模板 */
    LcBizTemplate saveAsTemplate(String bizCode, String templateName, String category, String description, String operator);

    /** 从模板克隆新业务（返回新 bizCode） */
    String cloneFromTemplate(Long templateId, String newBizCode, String newBizName, String operator);

    /** 删除模板 */
    void deleteTemplate(Long[] ids);
}
