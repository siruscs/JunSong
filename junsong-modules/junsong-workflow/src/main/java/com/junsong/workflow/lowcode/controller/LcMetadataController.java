package com.junsong.workflow.lowcode.controller;

import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.workflow.lowcode.domain.LcBizAction;
import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import com.junsong.workflow.lowcode.domain.LcBizConfigSnapshot;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import com.junsong.workflow.lowcode.domain.LcBizTemplate;
import com.junsong.workflow.lowcode.domain.dto.LcBizConfigDTO;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.system.api.RemoteMenuService;
import com.junsong.system.api.domain.LcMenuGenerateRequest;
import com.junsong.workflow.lowcode.service.LcConfigVersionService;
import com.junsong.workflow.lowcode.service.LcMetadataService;
import com.junsong.workflow.lowcode.service.LcTemplateService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lowcode/meta")
public class LcMetadataController extends BaseController
{
    @Autowired
    private LcMetadataService lcMetadataService;

    @Autowired
    private LcConfigVersionService configVersionService;

    @Autowired(required = false)
    private RemoteMenuService remoteMenuService;

    @Autowired
    private LcTemplateService templateService;

    // ===== 业务对象 =====
    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/object/list")
    public R<List<LcBizObject>> objectList(LcBizObject query)
    {
        return R.ok(lcMetadataService.selectBizObjectList(query));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/object/{id}")
    public R<LcBizObject> objectInfo(@PathVariable("id") Long id)
    {
        return R.ok(lcMetadataService.selectBizObjectById(id));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/object/code/{bizCode}")
    public R<LcBizObject> objectByCode(@PathVariable("bizCode") String bizCode)
    {
        return R.ok(lcMetadataService.selectBizObjectByBizCode(bizCode));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @PostMapping("/object")
    public R<Integer> objectAdd(@RequestBody LcBizObject bizObject)
    {
        return R.ok(lcMetadataService.insertBizObject(bizObject));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @PutMapping("/object")
    public R<Integer> objectEdit(@RequestBody LcBizObject bizObject)
    {
        return R.ok(lcMetadataService.updateBizObject(bizObject));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:remove')")
    @DeleteMapping("/object/{ids}")
    public R<Integer> objectRemove(@PathVariable("ids") Long[] ids)
    {
        return R.ok(lcMetadataService.deleteBizObjectByIds(ids));
    }

    // ===== 字段 =====
    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/field/list")
    public R<List<LcBizField>> fieldList(LcBizField query)
    {
        return R.ok(lcMetadataService.selectFieldList(query));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/field/code/{bizCode}")
    public R<List<LcBizField>> fieldByCode(@PathVariable("bizCode") String bizCode)
    {
        return R.ok(lcMetadataService.selectFieldsByBizCode(bizCode));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/field/{id}")
    public R<LcBizField> fieldInfo(@PathVariable("id") Long id)
    {
        return R.ok(lcMetadataService.selectFieldById(id));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @PostMapping("/field")
    public R<Integer> fieldAdd(@RequestBody LcBizField field)
    {
        return R.ok(lcMetadataService.insertField(field));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @PutMapping("/field")
    public R<Integer> fieldEdit(@RequestBody LcBizField field)
    {
        return R.ok(lcMetadataService.updateField(field));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:remove')")
    @DeleteMapping("/field/{ids}")
    public R<Integer> fieldRemove(@PathVariable("ids") Long[] ids)
    {
        return R.ok(lcMetadataService.deleteFieldByIds(ids));
    }

    // ===== 页面 Schema =====
    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/schema/list")
    public R<List<LcBizPageSchema>> schemaList(LcBizPageSchema query)
    {
        return R.ok(lcMetadataService.selectPageSchemaList(query));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/schema/code/{bizCode}")
    public R<List<LcBizPageSchema>> schemaByCode(@PathVariable("bizCode") String bizCode)
    {
        return R.ok(lcMetadataService.selectPageSchemasByBizCode(bizCode));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/schema/{id}")
    public R<LcBizPageSchema> schemaInfo(@PathVariable("id") Long id)
    {
        return R.ok(lcMetadataService.selectPageSchemaById(id));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @PostMapping("/schema")
    public R<Integer> schemaAdd(@RequestBody LcBizPageSchema pageSchema)
    {
        return R.ok(lcMetadataService.insertPageSchema(pageSchema));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @PutMapping("/schema")
    public R<Integer> schemaEdit(@RequestBody LcBizPageSchema pageSchema)
    {
        return R.ok(lcMetadataService.updatePageSchema(pageSchema));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:remove')")
    @DeleteMapping("/schema/{ids}")
    public R<Integer> schemaRemove(@PathVariable("ids") Long[] ids)
    {
        return R.ok(lcMetadataService.deletePageSchemaByIds(ids));
    }

    // ===== 节点处理人 =====
    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/assignee/list")
    public R<List<LcBizNodeAssignee>> assigneeList(LcBizNodeAssignee query)
    {
        return R.ok(lcMetadataService.selectNodeAssigneeList(query));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/assignee/code/{bizCode}")
    public R<List<LcBizNodeAssignee>> assigneeByCode(@PathVariable("bizCode") String bizCode)
    {
        return R.ok(lcMetadataService.selectNodeAssigneesByBizCode(bizCode));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/assignee/{id}")
    public R<LcBizNodeAssignee> assigneeInfo(@PathVariable("id") Long id)
    {
        return R.ok(lcMetadataService.selectNodeAssigneeById(id));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @PostMapping("/assignee")
    public R<Integer> assigneeAdd(@RequestBody LcBizNodeAssignee nodeAssignee)
    {
        return R.ok(lcMetadataService.insertNodeAssignee(nodeAssignee));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @PutMapping("/assignee")
    public R<Integer> assigneeEdit(@RequestBody LcBizNodeAssignee nodeAssignee)
    {
        return R.ok(lcMetadataService.updateNodeAssignee(nodeAssignee));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:remove')")
    @DeleteMapping("/assignee/{ids}")
    public R<Integer> assigneeRemove(@PathVariable("ids") Long[] ids)
    {
        return R.ok(lcMetadataService.deleteNodeAssigneeByIds(ids));
    }

    // ===== 分支规则 =====
    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/branch/list")
    public R<List<LcBizBranchRule>> branchList(LcBizBranchRule query)
    {
        return R.ok(lcMetadataService.selectBranchRuleList(query));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/branch/code/{bizCode}")
    public R<List<LcBizBranchRule>> branchByCode(@PathVariable("bizCode") String bizCode)
    {
        return R.ok(lcMetadataService.selectBranchRulesByBizCode(bizCode));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/branch/{id}")
    public R<LcBizBranchRule> branchInfo(@PathVariable("id") Long id)
    {
        return R.ok(lcMetadataService.selectBranchRuleById(id));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @PostMapping("/branch")
    public R<Integer> branchAdd(@RequestBody LcBizBranchRule branchRule)
    {
        return R.ok(lcMetadataService.insertBranchRule(branchRule));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @PutMapping("/branch")
    public R<Integer> branchEdit(@RequestBody LcBizBranchRule branchRule)
    {
        return R.ok(lcMetadataService.updateBranchRule(branchRule));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:remove')")
    @DeleteMapping("/branch/{ids}")
    public R<Integer> branchRemove(@PathVariable("ids") Long[] ids)
    {
        return R.ok(lcMetadataService.deleteBranchRuleByIds(ids));
    }

    // ===== 聚合配置（可视化后台用）=====
    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/config/{bizCode}")
    public R<LcBizConfigDTO> configByCode(@PathVariable("bizCode") String bizCode)
    {
        return R.ok(lcMetadataService.selectBizConfig(bizCode));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @Log(title = "低代码配置", businessType = BusinessType.UPDATE)
    @PostMapping("/config")
    public R<Void> configSave(@RequestBody LcBizConfigDTO config)
    {
        lcMetadataService.saveBizConfig(config);
        return R.ok();
    }

    // ===== 菜单/权限一键生成（转发到 system 服务）=====
    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @Log(title = "低代码菜单生成", businessType = BusinessType.INSERT)
    @PostMapping("/menu/generate")
    public R<Long> menuGenerate(@RequestBody LcMenuGenerateRequest req)
    {
        return remoteMenuService.generateLowcodeMenu(req, SecurityConstants.INNER);
    }

    // ===== 动作配置 =====
    /** 查询业务对象的动作配置（前端动态渲染按钮用） */
    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/action/{bizCode}")
    public R<List<LcBizAction>> actionsByCode(@PathVariable("bizCode") String bizCode)
    {
        return R.ok(lcMetadataService.selectBizActions(bizCode));
    }

    // ===== 配置版本管理 =====
    /** 发布当前草稿配置为新版本 */
    @PreAuthorize("@ss.hasPermi('lowcode:meta:publish')")
    @Log(title = "低代码配置发布", businessType = BusinessType.INSERT)
    @PostMapping("/config/publish/{bizCode}")
    public R<LcBizConfigSnapshot> publish(@PathVariable("bizCode") String bizCode,
                                            @RequestBody(required = false) PublishReq req)
    {
        String remark = req == null ? null : req.remark;
        String operator = SecurityUtils.getUsername();
        return R.ok(configVersionService.publish(bizCode, remark, operator));
    }

    /** 回滚到指定版本 */
    @PreAuthorize("@ss.hasPermi('lowcode:meta:publish')")
    @Log(title = "低代码配置回滚", businessType = BusinessType.UPDATE)
    @PostMapping("/config/rollback/{bizCode}")
    public R<Void> rollback(@PathVariable("bizCode") String bizCode,
                             @RequestParam("versionNo") Integer versionNo)
    {
        String operator = SecurityUtils.getUsername();
        configVersionService.rollback(bizCode, versionNo, operator);
        return R.ok();
    }

    /** 查询历史发布版本列表 */
    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/config/history/{bizCode}")
    public R<List<LcBizConfigSnapshot>> history(@PathVariable("bizCode") String bizCode)
    {
        return R.ok(configVersionService.listHistory(bizCode));
    }

    /** 读取指定版本快照 */
    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/config/snapshot/{bizCode}/{versionNo}")
    public R<LcBizConfigSnapshot> snapshot(@PathVariable("bizCode") String bizCode,
                                             @PathVariable("versionNo") Integer versionNo)
    {
        return R.ok(configVersionService.getSnapshot(bizCode, versionNo));
    }

    /** 发布请求体 */
    static class PublishReq
    {
        public String remark;
    }

    // ===== 模板管理 =====
    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/template/list")
    public R<List<LcBizTemplate>> templateList(@RequestParam(required = false) String category)
    {
        return R.ok(templateService.listTemplates(category));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:list')")
    @GetMapping("/template/{id}")
    public R<LcBizTemplate> templateInfo(@PathVariable("id") Long id)
    {
        return R.ok(templateService.getTemplate(id));
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @Log(title = "低代码模板保存", businessType = BusinessType.INSERT)
    @PostMapping("/template/save")
    public R<LcBizTemplate> saveAsTemplate(@RequestBody SaveTemplateReq req)
    {
        String operator = SecurityUtils.getUsername();
        LcBizTemplate t = templateService.saveAsTemplate(
            req.bizCode, req.templateName, req.category, req.description, operator);
        return R.ok(t);
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:config')")
    @Log(title = "低代码模板克隆", businessType = BusinessType.INSERT)
    @PostMapping("/template/clone")
    public R<String> cloneFromTemplate(@RequestBody CloneTemplateReq req)
    {
        String operator = SecurityUtils.getUsername();
        String newBizCode = templateService.cloneFromTemplate(
            req.templateId, req.newBizCode, req.newBizName, operator);
        return R.ok(newBizCode);
    }

    @PreAuthorize("@ss.hasPermi('lowcode:meta:remove')")
    @Log(title = "低代码模板删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/template/{ids}")
    public R<Void> templateRemove(@PathVariable("ids") Long[] ids)
    {
        templateService.deleteTemplate(ids);
        return R.ok();
    }

    static class SaveTemplateReq
    {
        public String bizCode;
        public String templateName;
        public String category;
        public String description;
    }

    static class CloneTemplateReq
    {
        public Long templateId;
        public String newBizCode;
        public String newBizName;
    }
}
