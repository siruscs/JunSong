package com.junsong.member.controller;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.member.domain.MemMember;
import com.junsong.member.service.IMemMemberService;
import com.junsong.member.service.impl.MemAuditTrailRecorder;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.common.security.auth.AuthUtil;
import org.springframework.util.StringUtils;

/**
 * 会员信息Controller
 */
@RestController
@RequestMapping("/member")
public class MemMemberController extends BaseController {

    @Autowired
    private IMemMemberService memMemberService;

    @Autowired
    private MemAuditTrailRecorder auditTrailRecorder;

    // ==================== PII 脱敏 ====================

    /**
     * 判断当前用户是否有权查看明文PII
     */
    private boolean canViewPii() {
        return AuthUtil.hasPermi("member:member:pii");
    }

    /**
     * 对会员列表执行PII脱敏（无权限时）
     */
    private void maskPiiIfNeeded(List<MemMember> list) {
        if (canViewPii()) return;
        for (MemMember m : list) {
            maskMemberPii(m);
        }
    }

    static void maskMemberPii(MemMember m) {
        if (m == null) return;
        m.setPhone(maskPhone(m.getPhone()));
        m.setAddress(maskAddress(m.getAddress()));
        m.setIdCard(maskIdCard(m.getIdCard()));
    }

    /** 手机号：前三后四，如 138****1234；过短返回 *** */
    static String maskPhone(String phone) {
        if (phone == null) return null;
        if (phone.length() <= 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 身份证：前六后四，如 110101****1234；过短返回 *** */
    static String maskIdCard(String idCard) {
        if (idCard == null) return null;
        if (idCard.length() <= 10) return "***";
        return idCard.substring(0, 6) + "****" + idCard.substring(idCard.length() - 4);
    }

    /** 地址：保留前6个字符（大约到区县级别），后续用***代替；过短返回 *** */
    static String maskAddress(String address) {
        if (address == null) return null;
        if (address.length() <= 6) return "***";
        return address.substring(0, 6) + "***";
    }

    // ==================== CRUD ====================

    /**
     * 查询会员信息列表
     */
    @RequiresPermissions("member:member:list")
    @GetMapping("/list")
    public TableDataInfo list(MemMember memMember) {
        startPage();
        List<MemMember> list = memMemberService.selectMemMemberList(memMember);
        return getDataTable(list);
    }

    /**
     * 导出会员信息列表（无PII权限时导出脱敏数据，明文导出需 member:member:piiExport 权限）
     */
    @RequiresPermissions("member:member:export")
    @Log(title = "会员信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemMember memMember) {
        List<MemMember> list = memMemberService.selectMemMemberList(memMember);
        boolean plaintext = AuthUtil.hasPermi("member:member:piiExport");
        if (!plaintext) {
            maskPiiIfNeeded(list);
        }
        if (plaintext) {
            auditTrailRecorder.record("pii_export", "member_export", String.valueOf(SecurityUtils.getDeptId()),
                    null,
                    "{\"recordCount\":" + list.size() + ",\"plaintext\":true,\"fields\":[\"phone\",\"address\",\"idCard\"]}");
        }
        ExcelUtil<MemMember> util = new ExcelUtil<MemMember>(MemMember.class);
        util.exportExcel(response, list, "会员信息数据");
    }

    /**
     * 获取会员信息详细信息
     */
    @RequiresPermissions("member:member:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        MemMember member = memMemberService.selectMemMemberById(id);
        return success(member);
    }

    /**
     * 新增会员信息
     */
    @RequiresPermissions("member:member:add")
    @Log(title = "会员信息", businessType = BusinessType.INSERT)
    @Idempotent(scene = "member:member:add")
    @PostMapping
    public AjaxResult add(@RequestBody MemMember memMember) {
        if (!StringUtils.hasText(memMember.getMemberNo())) {
            Long deptId = memMember.getDeptId();
            if (deptId == null) {
                deptId = SecurityUtils.getDeptId();
            }
            memMember.setMemberNo(memMemberService.generateMemberNo(deptId));
        } else if (!memMemberService.checkMemMemberNoUnique(memMember)) {
            return error("新增会员信息失败，编号已存在");
        }
        memMember.setCreateBy(SecurityUtils.getUsername());
        if (memMember.getDeptId() == null) {
            memMember.setDeptId(SecurityUtils.getDeptId());
        }
        int rows = memMemberService.insertMemMember(memMember);
        if (rows > 0) {
            AjaxResult ajax = AjaxResult.success("新增成功");
            ajax.put("data", memMember);
            return ajax;
        }
        return error("新增失败");
    }

    /**
     * 获取下一个会员编号
     */
    @RequiresPermissions("member:member:add")
    @GetMapping("/nextNo")
    public AjaxResult getNextMemberNo(@RequestParam(required = false) Long deptId) {
        if (deptId == null) {
            deptId = SecurityUtils.getDeptId();
        }
        return success(memMemberService.generateMemberNo(deptId));
    }

    /**
     * 根据会员编号查询会员（仅限当前部门，用于秒杀记录等场景的会员检索）
     */
    @RequiresPermissions("member:member:query")
    @GetMapping("/no/{memberNo}")
    public AjaxResult getByNo(@PathVariable("memberNo") String memberNo) {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            return error("当前用户未关联部门，无法查询会员");
        }
        MemMember member = memMemberService.selectMemMemberByNo(memberNo, deptId);
        if (member == null) {
            return error("未找到该部门下的会员编号：" + memberNo);
        }
        return success(member);
    }

    /**
     * 修改会员信息
     */
    @RequiresPermissions("member:member:edit")
    @Log(title = "会员信息", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "member:member:edit")
    @PutMapping
    public AjaxResult edit(@RequestBody MemMember memMember) {
        if (!memMemberService.checkMemMemberNoUnique(memMember)) {
            return error("修改会员信息失败，编号已存在");
        }
        memMember.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(memMemberService.updateMemMember(memMember));
    }

    /**
     * 删除会员信息
     */
    @RequiresPermissions("member:member:remove")
    @Log(title = "会员信息", businessType = BusinessType.DELETE)
    @Idempotent(scene = "member:member:delete")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(memMemberService.deleteMemMemberByIds(ids));
    }

    /**
     * 导入会员信息列表
     */
    @RequiresPermissions("member:member:import")
    @Log(title = "会员信息", businessType = BusinessType.IMPORT)
    @Idempotent(scene = "member:member:import")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<MemMember> util = new ExcelUtil<MemMember>(MemMember.class);
        List<MemMember> memberList = util.importExcel(file.getInputStream());
        String operName = SecurityUtils.getUsername();
        Long deptId = SecurityUtils.getDeptId();
        String message = memMemberService.importMember(memberList, updateSupport, operName, deptId);
        return success(message);
    }

    /**
     * 下载会员信息导入模板
     */
    @RequiresPermissions("member:member:import")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<MemMember> util = new ExcelUtil<MemMember>(MemMember.class);
        util.importTemplateExcel(response, "会员信息数据");
    }
}
