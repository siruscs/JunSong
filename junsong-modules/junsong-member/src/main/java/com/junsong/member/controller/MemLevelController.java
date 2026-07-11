package com.junsong.member.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.MemMemberCardType;
import com.junsong.member.service.IMemberLevelService;

/**
 * 会员等级配置Controller
 * 网关路径: /member/level/**
 */
@RestController
@RequestMapping("/level")
public class MemLevelController extends BaseController
{
    @Autowired
    private IMemberLevelService levelService;

    /**
     * 查询等级配置列表
     */
    @RequiresPermissions("member:level:list")
    @GetMapping("/list")
    public AjaxResult list(MemMemberCardType cardType)
    {
        List<MemMemberCardType> list = levelService.selectLevelList(cardType);
        return AjaxResult.success(list);
    }

    /**
     * 查询等级配置详情
     */
    @RequiresPermissions("member:level:query")
    @GetMapping("/{typeCode}")
    public AjaxResult getInfo(@PathVariable("typeCode") String typeCode)
    {
        return AjaxResult.success(levelService.selectLevelByTypeCode(typeCode));
    }

    /**
     * 修改等级配置
     */
    @RequiresPermissions("member:level:edit")
    @Log(title = "等级配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MemMemberCardType cardType)
    {
        if (cardType.getTypeId() == null)
        {
            return error("类型ID不能为空");
        }
        if (cardType.getMinGrowth() != null && cardType.getMinGrowth() < 0)
        {
            return error("升级成长值不能小于0");
        }
        cardType.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(levelService.updateLevel(cardType));
    }

    /**
     * 新增等级配置
     */
    @RequiresPermissions("member:level:add")
    @Log(title = "等级配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MemMemberCardType cardType)
    {
        if (cardType.getTypeCode() == null || cardType.getTypeCode().isEmpty())
        {
            return error("等级编码不能为空");
        }
        if (cardType.getTypeName() == null || cardType.getTypeName().isEmpty())
        {
            return error("等级名称不能为空");
        }
        if (!levelService.checkTypeCodeUnique(cardType))
        {
            return error("新增失败，等级编码'" + cardType.getTypeCode() + "'已存在");
        }
        if (cardType.getMinGrowth() == null || cardType.getMinGrowth() < 0)
        {
            cardType.setMinGrowth(0L);
        }
        cardType.setCreateBy(SecurityUtils.getUsername());
        return toAjax(levelService.insertLevel(cardType));
    }
}
