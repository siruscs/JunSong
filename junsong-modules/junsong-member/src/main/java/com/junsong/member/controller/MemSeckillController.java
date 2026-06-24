package com.junsong.member.controller;

import java.util.List;
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
import com.junsong.member.domain.MemSeckill;
import com.junsong.member.service.IMemSeckillService;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.api.model.LoginUser;

/**
 * 秒杀活动Controller
 */
@RestController
@RequestMapping("/seckill")
public class MemSeckillController extends BaseController {

    @Autowired
    private IMemSeckillService memSeckillService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询秒杀活动列表
     */
    @RequiresPermissions("member:seckill:list")
    @GetMapping("/list")
    public TableDataInfo list(MemSeckill memSeckill) {
        startPage();
        List<MemSeckill> list = memSeckillService.selectMemSeckillList(memSeckill);
        return getDataTable(list);
    }

    /**
     * 导出秒杀活动列表
     */
    @RequiresPermissions("member:seckill:export")
    @Log(title = "秒杀活动", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemSeckill memSeckill) {
        List<MemSeckill> list = memSeckillService.selectMemSeckillList(memSeckill);
        ExcelUtil<MemSeckill> util = new ExcelUtil<MemSeckill>(MemSeckill.class);
        util.exportExcel(response, list, "秒杀活动数据");
    }

    /**
     * 获取秒杀活动详细信息
     */
    @RequiresPermissions("member:seckill:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(memSeckillService.selectMemSeckillById(id));
    }

    /**
     * 新增秒杀活动
     */
    @RequiresPermissions("member:seckill:add")
    @Log(title = "秒杀活动", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MemSeckill memSeckill) {
        // 自动生成秒杀编号
        if (memSeckill.getSeckillNo() == null || memSeckill.getSeckillNo().isEmpty()) {
            String seckillNo = "SK" + System.currentTimeMillis();
            memSeckill.setSeckillNo(seckillNo);
        } else if (!memSeckillService.checkMemSeckillNoUnique(memSeckill)) {
            return error("新增秒杀活动失败，编号已存在");
        }
        if (memSeckill.getDeptId() == null) {
            Long deptId = SecurityUtils.getDeptId();
            // 兜底1：从LoginUser.SysUser获取
            if (deptId == null) {
                try {
                    LoginUser loginUser = SecurityUtils.getLoginUser();
                    if (loginUser != null && loginUser.getSysUser() != null && loginUser.getSysUser().getDeptId() != null) {
                        deptId = loginUser.getSysUser().getDeptId();
                    }
                } catch (Exception ignored) {}
            }
            // 兜底2：直接查sys_user_dept表，彻底不依赖token
            if (deptId == null) {
                try {
                    Long userId = SecurityUtils.getUserId();
                    if (userId != null) {
                        deptId = jdbcTemplate.queryForObject(
                            "SELECT dept_id FROM sys_user_dept WHERE user_id = ? AND status = '0' ORDER BY user_dept_id ASC LIMIT 1",
                            Long.class, userId);
                    }
                } catch (Exception ignored) {}
            }
            memSeckill.setDeptId(deptId);
        }
        memSeckill.setCreateBy(SecurityUtils.getUsername());
        return toAjax(memSeckillService.insertMemSeckill(memSeckill));
    }

    /**
     * 修改秒杀活动
     */
    @RequiresPermissions("member:seckill:edit")
    @Log(title = "秒杀活动", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MemSeckill memSeckill) {
        if (!memSeckillService.checkMemSeckillNoUnique(memSeckill)) {
            return error("修改秒杀活动失败，编号已存在");
        }
        memSeckill.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(memSeckillService.updateMemSeckill(memSeckill));
    }

    /**
     * 删除秒杀活动
     */
    @RequiresPermissions("member:seckill:remove")
    @Log(title = "秒杀活动", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(memSeckillService.deleteMemSeckillByIds(ids));
    }

    @RequiresPermissions("member:seckill:edit")
    @Log(title = "秒杀活动", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/close")
    public AjaxResult close(@PathVariable Long id) {
        MemSeckill seckill = new MemSeckill();
        seckill.setSeckillId(id);
        seckill.setStatus("1");
        seckill.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(memSeckillService.updateMemSeckill(seckill));
    }

    @RequiresPermissions("member:seckill:list")
    @GetMapping("/listActive")
    public AjaxResult listActive() {
        MemSeckill query = new MemSeckill();
        query.setStatus("0");
        List<MemSeckill> list = memSeckillService.selectMemSeckillList(query);
        return success(list);
    }
}
