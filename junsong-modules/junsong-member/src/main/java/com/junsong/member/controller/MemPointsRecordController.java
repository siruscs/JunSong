package com.junsong.member.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.member.domain.MemPointsRecord;
import com.junsong.member.service.IMemPointsRecordService;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;

/**
 * 积分记录Controller
 */
@RestController
@RequestMapping("/pointsRecord")
public class MemPointsRecordController extends BaseController {

    @Autowired
    private IMemPointsRecordService memPointsRecordService;

    /**
     * 查询积分记录列表
     */
    @RequiresPermissions("member:pointsRecord:list")
    @GetMapping("/list")
    public TableDataInfo list(MemPointsRecord memPointsRecord) {
        startPage();
        List<MemPointsRecord> list = memPointsRecordService.selectMemPointsRecordList(memPointsRecord);
        return getDataTable(list);
    }

    /**
     * 导出积分记录列表
     */
    @RequiresPermissions("member:pointsRecord:export")
    @Log(title = "积分记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemPointsRecord memPointsRecord) {
        List<MemPointsRecord> list = memPointsRecordService.selectMemPointsRecordList(memPointsRecord);
        ExcelUtil<MemPointsRecord> util = new ExcelUtil<MemPointsRecord>(MemPointsRecord.class);
        util.exportExcel(response, list, "积分记录数据");
    }

    /**
     * 获取积分记录详细信息
     */
    @RequiresPermissions("member:pointsRecord:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(memPointsRecordService.selectMemPointsRecordById(id));
    }

    /**
     * 新增积分记录
     */
    @RequiresPermissions("member:pointsRecord:add")
    @Log(title = "积分记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MemPointsRecord memPointsRecord) {
        if (!memPointsRecordService.checkMemPointsRecordNoUnique(memPointsRecord)) {
            return error("新增积分记录失败，编号已存在");
        }
        if (memPointsRecord.getDeptId() == null) {
            memPointsRecord.setDeptId(SecurityUtils.getDeptId());
        }
        memPointsRecord.setCreateBy(SecurityUtils.getUsername());
        return toAjax(memPointsRecordService.insertMemPointsRecord(memPointsRecord));
    }

    /**
     * 修改积分记录
     */
    @RequiresPermissions("member:pointsRecord:edit")
    @Log(title = "积分记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MemPointsRecord memPointsRecord) {
        if (!memPointsRecordService.checkMemPointsRecordNoUnique(memPointsRecord)) {
            return error("修改积分记录失败，编号已存在");
        }
        memPointsRecord.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(memPointsRecordService.updateMemPointsRecord(memPointsRecord));
    }

    /**
     * 删除积分记录
     */
    @RequiresPermissions("member:pointsRecord:remove")
    @Log(title = "积分记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(memPointsRecordService.deleteMemPointsRecordByIds(ids));
    }
}
