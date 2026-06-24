package com.junsong.member.controller;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.member.domain.MemPointsGoods;
import com.junsong.member.service.IMemPointsGoodsService;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;

/**
 * 积分物品Controller
 */
@RestController
@RequestMapping("/pointsGoods")
public class MemPointsGoodsController extends BaseController {

    @Autowired
    private IMemPointsGoodsService memPointsGoodsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询积分物品列表
     */
    @RequiresPermissions("member:pointsGoods:list")
    @GetMapping("/list")
    public TableDataInfo list(MemPointsGoods memPointsGoods) {
        startPage();
        List<MemPointsGoods> list = memPointsGoodsService.selectMemPointsGoodsList(memPointsGoods);
        return getDataTable(list);
    }

    /**
     * 导出积分物品列表
     */
    @RequiresPermissions("member:pointsGoods:export")
    @Log(title = "积分物品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemPointsGoods memPointsGoods) {
        List<MemPointsGoods> list = memPointsGoodsService.selectMemPointsGoodsList(memPointsGoods);
        ExcelUtil<MemPointsGoods> util = new ExcelUtil<MemPointsGoods>(MemPointsGoods.class);
        util.exportExcel(response, list, "积分物品数据");
    }

    /**
     * 获取积分物品详细信息
     */
    @RequiresPermissions("member:pointsGoods:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(memPointsGoodsService.selectMemPointsGoodsById(id));
    }

    /**
     * 新增积分物品
     */
    @RequiresPermissions("member:pointsGoods:add")
    @Log(title = "积分物品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MemPointsGoods memPointsGoods) {
        if (memPointsGoods.getGoodsCode() == null || memPointsGoods.getGoodsCode().isEmpty()) {
            // 自动生成物品编码：SP+部门编号后2位+年月日+3位序号
            Long deptId = memPointsGoods.getDeptId() != null ? memPointsGoods.getDeptId() : SecurityUtils.getDeptId();
            String deptSuffix = deptId != null ? String.valueOf(deptId).substring(Math.max(0, String.valueOf(deptId).length() - 2)) : "00";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String dateStr = sdf.format(new Date());
            String prefix = "SP" + deptSuffix + dateStr;
            try {
                Integer maxNo = jdbcTemplate.queryForObject(
                    "SELECT MAX(CAST(SUBSTRING(goods_code, LENGTH(?) + 1) AS SIGNED)) FROM mem_points_goods WHERE goods_code LIKE CONCAT(?, '%') AND del_flag = '0'",
                    Integer.class, prefix, prefix);
                int nextNo = (maxNo != null ? maxNo : 0) + 1;
                memPointsGoods.setGoodsCode(prefix + String.format("%03d", nextNo));
            } catch (Exception e) {
                memPointsGoods.setGoodsCode(prefix + "001");
            }
        }
        if (!memPointsGoodsService.checkMemPointsGoodsNoUnique(memPointsGoods)) {
            return error("新增积分物品失败，编号已存在");
        }
        if (memPointsGoods.getDeptId() == null) {
            memPointsGoods.setDeptId(SecurityUtils.getDeptId());
        }
        memPointsGoods.setCreateBy(SecurityUtils.getUsername());
        return toAjax(memPointsGoodsService.insertMemPointsGoods(memPointsGoods));
    }

    /**
     * 修改积分物品
     */
    @RequiresPermissions("member:pointsGoods:edit")
    @Log(title = "积分物品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MemPointsGoods memPointsGoods) {
        if (!memPointsGoodsService.checkMemPointsGoodsNoUnique(memPointsGoods)) {
            return error("修改积分物品失败，编号已存在");
        }
        memPointsGoods.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(memPointsGoodsService.updateMemPointsGoods(memPointsGoods));
    }

    /**
     * 删除积分物品
     */
    @RequiresPermissions("member:pointsGoods:remove")
    @Log(title = "积分物品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(memPointsGoodsService.deleteMemPointsGoodsByIds(ids));
    }
}
