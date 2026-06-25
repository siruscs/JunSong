package com.junsong.workflow.lowcode.service.impl;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.workflow.lowcode.domain.LcBizAction;
import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.LcBizNodeTimer;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import com.junsong.workflow.lowcode.domain.LcBizPostAction;
import com.junsong.workflow.lowcode.domain.dto.LcBizConfigDTO;
import com.junsong.workflow.lowcode.mapper.LcBizActionMapper;
import com.junsong.workflow.lowcode.mapper.LcBizBranchRuleMapper;
import com.junsong.workflow.lowcode.mapper.LcBizFieldMapper;
import com.junsong.workflow.lowcode.mapper.LcBizNodeAssigneeMapper;
import com.junsong.workflow.lowcode.mapper.LcBizNodeTimerMapper;
import com.junsong.workflow.lowcode.mapper.LcBizObjectMapper;
import com.junsong.workflow.lowcode.mapper.LcBizPageSchemaMapper;
import com.junsong.workflow.lowcode.mapper.LcBizPostActionMapper;
import com.junsong.workflow.lowcode.service.LcMetadataService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LcMetadataServiceImpl implements LcMetadataService
{
    @Autowired
    private LcBizObjectMapper lcBizObjectMapper;

    @Autowired
    private LcBizFieldMapper lcBizFieldMapper;

    @Autowired
    private LcBizPageSchemaMapper lcBizPageSchemaMapper;

    @Autowired
    private LcBizNodeAssigneeMapper lcBizNodeAssigneeMapper;

    @Autowired
    private LcBizBranchRuleMapper lcBizBranchRuleMapper;

    @Autowired
    private LcBizActionMapper lcBizActionMapper;

    @Autowired
    private LcBizPostActionMapper lcBizPostActionMapper;

    @Autowired
    private LcBizNodeTimerMapper lcBizNodeTimerMapper;

    // ===== 聚合配置（可视化后台用）=====
    @Override
    public LcBizConfigDTO selectBizConfig(String bizCode)
    {
        LcBizConfigDTO dto = new LcBizConfigDTO();
        dto.setBizObject(selectBizObjectByBizCode(bizCode));
        dto.setFields(selectFieldsByBizCode(bizCode));
        dto.setPageSchemas(selectPageSchemasByBizCode(bizCode));
        dto.setNodeAssignees(selectNodeAssigneesByBizCode(bizCode));
        dto.setBranchRules(selectBranchRulesByBizCode(bizCode));
        dto.setActions(lcBizActionMapper.selectByBizCode(bizCode));
        dto.setPostActions(lcBizPostActionMapper.selectByBizCode(bizCode));
        dto.setNodeTimers(lcBizNodeTimerMapper.selectByBizCode(bizCode));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "lc-metadata", allEntries = true)
    public void saveBizConfig(LcBizConfigDTO config)
    {
        LcBizObject obj = config == null ? null : config.getBizObject();
        if (obj == null || obj.getBizCode() == null || obj.getBizCode().isBlank())
        {
            throw new ServiceException("业务对象与 bizCode 不能为空");
        }
        if (obj.getId() == null)
        {
            insertBizObject(obj);
        }
        else
        {
            updateBizObject(obj);
        }
        String bizCode = obj.getBizCode();
        replaceFields(bizCode, config.getFields());
        replacePageSchemas(bizCode, config.getPageSchemas());
        replaceNodeAssignees(bizCode, config.getNodeAssignees());
        replaceBranchRules(bizCode, config.getBranchRules());
        replaceActions(bizCode, config.getActions());
        replacePostActions(bizCode, config.getPostActions());
        replaceNodeTimers(bizCode, config.getNodeTimers());

        // 保存配置后标记为草稿状态（需发布才生效）
        obj.setConfigStatus("DRAFT");
        lcBizObjectMapper.updateLcBizObject(obj);
    }

    private void replaceFields(String bizCode, List<LcBizField> list)
    {
        lcBizFieldMapper.physicalDeleteByBizCode(bizCode);
        if (list != null)
        {
            for (LcBizField f : list)
            {
                f.setId(null);
                f.setBizCode(bizCode);
                insertField(f);
            }
        }
    }

    private void replacePageSchemas(String bizCode, List<LcBizPageSchema> list)
    {
        lcBizPageSchemaMapper.physicalDeleteByBizCode(bizCode);
        if (list != null)
        {
            for (LcBizPageSchema s : list)
            {
                s.setId(null);
                s.setBizCode(bizCode);
                insertPageSchema(s);
            }
        }
    }

    private void replaceNodeAssignees(String bizCode, List<LcBizNodeAssignee> list)
    {
        lcBizNodeAssigneeMapper.physicalDeleteByBizCode(bizCode);
        if (list != null)
        {
            for (LcBizNodeAssignee a : list)
            {
                a.setId(null);
                a.setBizCode(bizCode);
                insertNodeAssignee(a);
            }
        }
    }

    private void replaceBranchRules(String bizCode, List<LcBizBranchRule> list)
    {
        lcBizBranchRuleMapper.physicalDeleteByBizCode(bizCode);
        if (list != null)
        {
            for (LcBizBranchRule r : list)
            {
                r.setId(null);
                r.setBizCode(bizCode);
                insertBranchRule(r);
            }
        }
    }

    private void replaceActions(String bizCode, List<LcBizAction> list)
    {
        lcBizActionMapper.physicalDeleteByBizCode(bizCode);
        if (list != null)
        {
            for (LcBizAction a : list)
            {
                a.setId(null);
                a.setBizCode(bizCode);
                lcBizActionMapper.insertAction(a);
            }
        }
    }

    private void replacePostActions(String bizCode, List<LcBizPostAction> list)
    {
        lcBizPostActionMapper.physicalDeleteByBizCode(bizCode);
        if (list != null)
        {
            for (LcBizPostAction p : list)
            {
                p.setId(null);
                p.setBizCode(bizCode);
                lcBizPostActionMapper.insertPostAction(p);
            }
        }
    }

    private void replaceNodeTimers(String bizCode, List<LcBizNodeTimer> list)
    {
        lcBizNodeTimerMapper.physicalDeleteByBizCode(bizCode);
        if (list != null)
        {
            for (LcBizNodeTimer t : list)
            {
                t.setId(null);
                t.setBizCode(bizCode);
                if (t.getDelFlag() == null) t.setDelFlag("0");
                insertNodeTimer(t);
            }
        }
    }

    // ===== 业务对象 =====
    @Override
    public List<LcBizObject> selectBizObjectList(LcBizObject query)
    {
        return lcBizObjectMapper.selectLcBizObjectList(query);
    }

    @Override
    public LcBizObject selectBizObjectById(Long id)
    {
        return lcBizObjectMapper.selectLcBizObjectById(id);
    }

    @Override
    public LcBizObject selectBizObjectByBizCode(String bizCode)
    {
        return lcBizObjectMapper.selectLcBizObjectByBizCode(bizCode);
    }

    @Override
    public List<LcBizObject> selectGenericWorkflowObjects()
    {
        LcBizObject query = new LcBizObject();
        query.setStorageMode("GENERIC");
        query.setWorkflowEnabled("1");
        return lcBizObjectMapper.selectLcBizObjectList(query);
    }

    @Override
    public int insertBizObject(LcBizObject bizObject)
    {
        return lcBizObjectMapper.insertLcBizObject(bizObject);
    }

    @Override
    public int updateBizObject(LcBizObject bizObject)
    {
        return lcBizObjectMapper.updateLcBizObject(bizObject);
    }

    @Override
    public int deleteBizObjectByIds(Long[] ids)
    {
        return lcBizObjectMapper.deleteLcBizObjectByIds(ids);
    }

    // ===== 字段 =====
    @Override
    public List<LcBizField> selectFieldList(LcBizField query)
    {
        return lcBizFieldMapper.selectLcBizFieldList(query);
    }

    @Override
    @Cacheable(value = "lc-metadata", key = "#bizCode + ':fields'")
    public List<LcBizField> selectFieldsByBizCode(String bizCode)
    {
        return lcBizFieldMapper.selectByBizCode(bizCode);
    }

    @Override
    public LcBizField selectFieldById(Long id)
    {
        return lcBizFieldMapper.selectLcBizFieldById(id);
    }

    @Override
    public int insertField(LcBizField field)
    {
        return lcBizFieldMapper.insertLcBizField(field);
    }

    @Override
    public int updateField(LcBizField field)
    {
        return lcBizFieldMapper.updateLcBizField(field);
    }

    @Override
    public int deleteFieldByIds(Long[] ids)
    {
        return lcBizFieldMapper.deleteLcBizFieldByIds(ids);
    }

    // ===== 页面 Schema =====
    @Override
    public List<LcBizPageSchema> selectPageSchemaList(LcBizPageSchema query)
    {
        return lcBizPageSchemaMapper.selectLcBizPageSchemaList(query);
    }

    @Override
    public List<LcBizPageSchema> selectPageSchemasByBizCode(String bizCode)
    {
        return lcBizPageSchemaMapper.selectByBizCode(bizCode);
    }

    @Override
    public LcBizPageSchema selectPageSchemaById(Long id)
    {
        return lcBizPageSchemaMapper.selectLcBizPageSchemaById(id);
    }

    @Override
    public int insertPageSchema(LcBizPageSchema pageSchema)
    {
        return lcBizPageSchemaMapper.insertLcBizPageSchema(pageSchema);
    }

    @Override
    public int updatePageSchema(LcBizPageSchema pageSchema)
    {
        return lcBizPageSchemaMapper.updateLcBizPageSchema(pageSchema);
    }

    @Override
    public int deletePageSchemaByIds(Long[] ids)
    {
        return lcBizPageSchemaMapper.deleteLcBizPageSchemaByIds(ids);
    }

    // ===== 节点处理人 =====
    @Override
    public List<LcBizNodeAssignee> selectNodeAssigneeList(LcBizNodeAssignee query)
    {
        return lcBizNodeAssigneeMapper.selectLcBizNodeAssigneeList(query);
    }

    @Override
    @Cacheable(value = "lc-metadata", key = "#bizCode + ':assignees'")
    public List<LcBizNodeAssignee> selectNodeAssigneesByBizCode(String bizCode)
    {
        return lcBizNodeAssigneeMapper.selectByBizCode(bizCode);
    }

    @Override
    public LcBizNodeAssignee selectNodeAssigneeById(Long id)
    {
        return lcBizNodeAssigneeMapper.selectLcBizNodeAssigneeById(id);
    }

    @Override
    public int insertNodeAssignee(LcBizNodeAssignee nodeAssignee)
    {
        return lcBizNodeAssigneeMapper.insertLcBizNodeAssignee(nodeAssignee);
    }

    @Override
    public int updateNodeAssignee(LcBizNodeAssignee nodeAssignee)
    {
        return lcBizNodeAssigneeMapper.updateLcBizNodeAssignee(nodeAssignee);
    }

    @Override
    public int deleteNodeAssigneeByIds(Long[] ids)
    {
        return lcBizNodeAssigneeMapper.deleteLcBizNodeAssigneeByIds(ids);
    }

    // ===== 分支规则 =====
    @Override
    public List<LcBizBranchRule> selectBranchRuleList(LcBizBranchRule query)
    {
        return lcBizBranchRuleMapper.selectLcBizBranchRuleList(query);
    }

    @Override
    @Cacheable(value = "lc-metadata", key = "#bizCode + ':rules'")
    public List<LcBizBranchRule> selectBranchRulesByBizCode(String bizCode)
    {
        return lcBizBranchRuleMapper.selectByBizCode(bizCode);
    }

    @Override
    public LcBizBranchRule selectBranchRuleById(Long id)
    {
        return lcBizBranchRuleMapper.selectLcBizBranchRuleById(id);
    }

    @Override
    public int insertBranchRule(LcBizBranchRule branchRule)
    {
        return lcBizBranchRuleMapper.insertLcBizBranchRule(branchRule);
    }

    @Override
    public int updateBranchRule(LcBizBranchRule branchRule)
    {
        return lcBizBranchRuleMapper.updateLcBizBranchRule(branchRule);
    }

    @Override
    public int deleteBranchRuleByIds(Long[] ids)
    {
        return lcBizBranchRuleMapper.deleteLcBizBranchRuleByIds(ids);
    }

    // ===== 动作配置 =====
    @Override
    public List<LcBizAction> selectBizActions(String bizCode)
    {
        return lcBizActionMapper.selectByBizCode(bizCode);
    }

    // ===== 节点定时器 =====
    @Override
    public List<LcBizNodeTimer> selectNodeTimerList(LcBizNodeTimer query)
    {
        return lcBizNodeTimerMapper.selectLcBizNodeTimerList(query);
    }

    @Override
    public List<LcBizNodeTimer> selectNodeTimersByBizCode(String bizCode)
    {
        return lcBizNodeTimerMapper.selectByBizCode(bizCode);
    }

    @Override
    public LcBizNodeTimer selectNodeTimerById(Long id)
    {
        return lcBizNodeTimerMapper.selectLcBizNodeTimerById(id);
    }

    @Override
    public int insertNodeTimer(LcBizNodeTimer nodeTimer)
    {
        return lcBizNodeTimerMapper.insertLcBizNodeTimer(nodeTimer);
    }

    @Override
    public int updateNodeTimer(LcBizNodeTimer nodeTimer)
    {
        return lcBizNodeTimerMapper.updateLcBizNodeTimer(nodeTimer);
    }

    @Override
    public int deleteNodeTimerByIds(Long[] ids)
    {
        return lcBizNodeTimerMapper.deleteLcBizNodeTimerByIds(ids);
    }
}
