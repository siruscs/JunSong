package com.junsong.workflow.lowcode.controller;

import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.workflow.lowcode.service.LcBizExcelService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 低代码业务数据 Excel 导入导出 Controller。
 *
 * @author junsong
 */
@Validated
@RestController
@RequestMapping("/lowcode/excel")
public class LcBizExcelController extends BaseController
{
    @Autowired
    private LcBizExcelService excelService;

    /**
     * 导出业务数据
     */
    @PreAuthorize("@ss.hasPermi('lowcode:biz:export')")
    @GetMapping("/export/{bizCode}")
    public void export(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode,
                       HttpServletResponse response) throws IOException
    {
        byte[] data = excelService.exportBizData(bizCode);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(bizCode + "_data.xlsx", StandardCharsets.UTF_8));
        response.getOutputStream().write(data);
    }

    /**
     * 下载导入模板
     */
    @PreAuthorize("@ss.hasPermi('lowcode:biz:import')")
    @GetMapping("/template/{bizCode}")
    public void template(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode,
                         HttpServletResponse response) throws IOException
    {
        byte[] data = excelService.generateTemplate(bizCode);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(bizCode + "_template.xlsx", StandardCharsets.UTF_8));
        response.getOutputStream().write(data);
    }

    /**
     * 导入业务数据
     */
    @PreAuthorize("@ss.hasPermi('lowcode:biz:import')")
    @PostMapping("/import/{bizCode}")
    public R<String> importData(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode,
                                @RequestParam("file") MultipartFile file) throws IOException
    {
        int count = excelService.importBizData(bizCode, file.getInputStream());
        return R.ok("成功导入 " + count + " 条数据");
    }
}
