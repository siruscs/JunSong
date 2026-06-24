package com.junsong.member.controller;

import java.util.List;
import java.util.Map;
import java.util.Date;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.utils.DateUtils;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.member.domain.MemSeckillClaimRecord;
import com.junsong.member.domain.MemSeckillRecord;
import com.junsong.member.service.IMemSeckillRecordService;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;

/**
 * 秒杀记录Controller
 */
@RestController
@RequestMapping("/seckillRecord")
public class MemSeckillRecordController extends BaseController {

    @Autowired
    private IMemSeckillRecordService memSeckillRecordService;

    /**
     * 查询秒杀记录列表
     */
    @RequiresPermissions("member:seckillRecord:list")
    @GetMapping("/list")
    public TableDataInfo list(MemSeckillRecord memSeckillRecord) {
        startPage();
        List<MemSeckillRecord> list = memSeckillRecordService.selectMemSeckillRecordList(memSeckillRecord);
        return getDataTable(list);
    }

    /**
     * 导出秒杀记录列表
     */
    @RequiresPermissions("member:seckillRecord:export")
    @Log(title = "秒杀记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemSeckillRecord memSeckillRecord) {
        List<MemSeckillRecord> list = memSeckillRecordService.selectMemSeckillRecordList(memSeckillRecord);
        ExcelUtil<MemSeckillRecord> util = new ExcelUtil<MemSeckillRecord>(MemSeckillRecord.class);
        util.exportExcel(response, list, "秒杀记录数据");
    }

    /**
     * 获取秒杀记录详细信息
     */
    @RequiresPermissions("member:seckillRecord:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(memSeckillRecordService.selectMemSeckillRecordById(id));
    }

    /**
     * 新增秒杀记录
     */
    @RequiresPermissions("member:seckillRecord:add")
    @Log(title = "秒杀记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MemSeckillRecord memSeckillRecord) {
        if (memSeckillRecord.getDeptId() == null) {
            memSeckillRecord.setDeptId(SecurityUtils.getDeptId());
        }
        memSeckillRecord.setCreateBy(SecurityUtils.getUsername());
        return toAjax(memSeckillRecordService.insertMemSeckillRecord(memSeckillRecord));
    }

    /**
     * 全员秒杀：为当前部门所有有效会员批量生成秒杀记录
     */
    @RequiresPermissions("member:seckillRecord:add")
    @Log(title = "秒杀记录", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batchAddForAll(@RequestBody MemSeckillRecord memSeckillRecord) {
        if (memSeckillRecord.getDeptId() == null) {
            memSeckillRecord.setDeptId(SecurityUtils.getDeptId());
        }
        memSeckillRecord.setCreateBy(SecurityUtils.getUsername());
        return AjaxResult.success(memSeckillRecordService.batchSeckillForAllMembers(memSeckillRecord));
    }

    /**
     * 修改秒杀记录
     */
    @RequiresPermissions("member:seckillRecord:edit")
    @Log(title = "秒杀记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MemSeckillRecord memSeckillRecord) {
        memSeckillRecord.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(memSeckillRecordService.updateMemSeckillRecord(memSeckillRecord));
    }

    /**
     * 删除秒杀记录
     */
    @RequiresPermissions("member:seckillRecord:remove")
    @Log(title = "秒杀记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(memSeckillRecordService.deleteMemSeckillRecordByIds(ids));
    }

    /**
     * 领取秒杀记录
     */
    @RequiresPermissions("member:seckillRecord:receive")
    @Log(title = "秒杀记录", businessType = BusinessType.UPDATE)
    @PutMapping("/claim/{recordId}")
    public AjaxResult claim(@PathVariable Long recordId, @RequestBody(required = false) Map<String, Object> body) {
        Integer claimShares = 1;
        Date claimTime = new Date();
        String remark = null;
        if (body != null) {
            Object sharesValue = body.get("claimShares");
            if (sharesValue instanceof Number) {
                claimShares = ((Number) sharesValue).intValue();
            } else if (sharesValue != null) {
                claimShares = Integer.valueOf(String.valueOf(sharesValue));
            }
            if (body.get("remark") != null) {
                remark = String.valueOf(body.get("remark"));
            }
            if (body.get("claimTime") != null) {
                claimTime = DateUtils.parseDate(String.valueOf(body.get("claimTime")));
            }
        }
        return toAjax(memSeckillRecordService.claimMemSeckillRecord(recordId, claimShares, claimTime, remark));
    }

    /**
     * 查询秒杀领取明细
     */
    @RequiresPermissions("member:seckillRecord:list")
    @GetMapping("/claim/list")
    public TableDataInfo claimList(MemSeckillClaimRecord claimRecord) {
        startPage();
        List<MemSeckillClaimRecord> list = memSeckillRecordService.selectClaimRecordList(claimRecord);
        return getDataTable(list);
    }

    /**
     * 统计秒杀记录
     */
    @RequiresPermissions("member:seckillRecord:list")
    @GetMapping("/statistics")
    public AjaxResult statistics(MemSeckillRecord memSeckillRecord) {
        Map<String, Object> stats = memSeckillRecordService.getRecordStatistics(memSeckillRecord);
        List<Map<String, Object>> paymentStats = memSeckillRecordService.getPaymentMethodStats(memSeckillRecord);
        stats.put("paymentMethodStats", paymentStats);
        return success(stats);
    }
}
