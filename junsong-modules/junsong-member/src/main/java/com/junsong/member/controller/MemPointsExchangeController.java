package com.junsong.member.controller;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.member.domain.MemPointsExchange;
import com.junsong.member.domain.MemPointsRecord;
import com.junsong.member.service.IMemPointsExchangeService;
import com.junsong.member.service.IMemPointsRecordService;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;

import java.math.BigDecimal;

@RestController
@RequestMapping("/pointsExchange")
public class MemPointsExchangeController extends BaseController {

    @Autowired
    private IMemPointsExchangeService memPointsExchangeService;

    @Autowired
    private IMemPointsRecordService memPointsRecordService;

    @RequiresPermissions("member:pointsExchange:list")
    @GetMapping("/list")
    public TableDataInfo list(MemPointsExchange memPointsExchange) {
        startPage();
        List<MemPointsExchange> list = memPointsExchangeService.selectMemPointsExchangeList(memPointsExchange);
        return getDataTable(list);
    }

    @RequiresPermissions("member:pointsExchange:export")
    @Log(title = "积分兑换", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemPointsExchange memPointsExchange) {
        List<MemPointsExchange> list = memPointsExchangeService.selectMemPointsExchangeList(memPointsExchange);
        ExcelUtil<MemPointsExchange> util = new ExcelUtil<MemPointsExchange>(MemPointsExchange.class);
        util.exportExcel(response, list, "积分兑换数据");
    }

    @RequiresPermissions("member:pointsExchange:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(memPointsExchangeService.selectMemPointsExchangeById(id));
    }

    @RequiresPermissions("member:pointsExchange:add")
    @Log(title = "积分兑换", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MemPointsExchange memPointsExchange) {
        if (memPointsExchange.getExchangeNo() == null || memPointsExchange.getExchangeNo().isEmpty()) {
            String exchangeNo = "EX" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
            memPointsExchange.setExchangeNo(exchangeNo);
        }
        if (!memPointsExchangeService.checkMemPointsExchangeNoUnique(memPointsExchange)) {
            return error("新增积分兑换失败，编号已存在");
        }
        if (memPointsExchange.getDeptId() == null) {
            memPointsExchange.setDeptId(SecurityUtils.getDeptId());
        }
        memPointsExchange.setCreateBy(SecurityUtils.getUsername());
        calcAndSetActualPoints(memPointsExchange);
        int rows = memPointsExchangeService.insertMemPointsExchange(memPointsExchange);
        if (rows > 0) {
            try {
                createDeductPointsRecord(memPointsExchange);
            } catch (Exception e) {
                return error("兑换成功但积分扣减记录创建失败：" + e.getMessage());
            }
        }
        return toAjax(rows);
    }

    private void calcAndSetActualPoints(MemPointsExchange exchange) {
        BigDecimal currentBalance = BigDecimal.ZERO;
        MemPointsRecord latest = memPointsRecordService.selectLatestBalanceByMemberId(exchange.getMemberId());
        if (latest != null && latest.getBalance() != null) {
            currentBalance = latest.getBalance();
        }
        BigDecimal deductPoints = exchange.getPointsDeducted() != null ? exchange.getPointsDeducted() : BigDecimal.ZERO;
        BigDecimal actualDeduct = deductPoints.min(currentBalance);
        exchange.setActualPoints(actualDeduct);
        exchange.setCurrentBalance(currentBalance);
        exchange.setNewBalance(currentBalance.subtract(actualDeduct));
    }

    private void createDeductPointsRecord(MemPointsExchange exchange) {
        MemPointsRecord pointsRecord = new MemPointsRecord();
        pointsRecord.setDeptId(exchange.getDeptId());
        pointsRecord.setMemberId(exchange.getMemberId());
        pointsRecord.setMemberNo(exchange.getMemberNo());
        pointsRecord.setMemberName(exchange.getMemberName());
        pointsRecord.setRecordType("2");
        pointsRecord.setPoints(exchange.getActualPoints().negate());
        pointsRecord.setBalance(exchange.getNewBalance());
        pointsRecord.setRemark("兑换扣减-" + exchange.getExchangeNo());
        pointsRecord.setCreateBy(SecurityUtils.getUsername());
        memPointsRecordService.insertMemPointsRecord(pointsRecord);
    }

    @RequiresPermissions("member:pointsExchange:edit")
    @Log(title = "积分兑换", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MemPointsExchange memPointsExchange) {
        if (!memPointsExchangeService.checkMemPointsExchangeNoUnique(memPointsExchange)) {
            return error("修改积分兑换失败，编号已存在");
        }
        memPointsExchange.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(memPointsExchangeService.updateMemPointsExchange(memPointsExchange));
    }

    @RequiresPermissions("member:pointsExchange:remove")
    @Log(title = "积分兑换", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        for (Long exchangeId : ids) {
            try {
                MemPointsExchange exchange = memPointsExchangeService.selectMemPointsExchangeById(exchangeId);
                if (exchange != null && exchange.getExchangeNo() != null) {
                    List<MemPointsRecord> records = memPointsRecordService.selectMemPointsRecordByRemark(
                        exchange.getExchangeNo());
                    for (MemPointsRecord record : records) {
                        memPointsRecordService.deleteMemPointsRecordById(record.getRecordId());
                    }
                }
            } catch (Exception e) {
            }
        }
        return toAjax(memPointsExchangeService.deleteMemPointsExchangeByIds(ids));
    }
}
