package com.junsong.workflow.lowcode.controller;

import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizInstance;
import com.junsong.workflow.lowcode.service.LcBizService;
import com.junsong.workflow.lowcode.service.LcMetadataService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 低代码业务数据 CSV 导入导出 Controller。
 *
 * <p>导出以 CSV（UTF-8 BOM）形式下发，第一行为字段标签（fieldLabel），后续行为实例数据；
 * 导入支持 CSV 与 Excel(.xlsx) 两种文件，按 fieldLabel/fieldKey 映射回 formData 后调用 LcBizService.save。</p>
 *
 * @author junsong
 */
@Validated
@RestController
@RequestMapping("/lowcode/biz")
public class LcBizImportExportController extends BaseController
{
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final LcBizService bizService;
    private final LcMetadataService metadataService;

    public LcBizImportExportController(LcBizService bizService, LcMetadataService metadataService)
    {
        this.bizService = bizService;
        this.metadataService = metadataService;
    }

    /**
     * 导出业务数据为 CSV
     */
    @PreAuthorize("@ss.hasPermi('lowcode:biz:export')")
    @PostMapping("/{bizCode}/export")
    public void export(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode,
                       HttpServletResponse response) throws IOException
    {
        List<LcBizField> fields = metadataService.selectFieldsByBizCode(bizCode);
        List<String> fieldKeys = new ArrayList<>();
        List<String> fieldLabels = new ArrayList<>();
        for (LcBizField f : fields)
        {
            if ("1".equals(f.getIsList()))
            {
                fieldKeys.add(f.getFieldKey());
                fieldLabels.add(f.getFieldLabel());
            }
        }

        LcBizInstance query = new LcBizInstance();
        List<LcBizInstance> list = bizService.list(bizCode, query);

        StringBuilder csv = new StringBuilder();
        csv.append(toCsvRow(fieldLabels));
        for (LcBizInstance inst : list)
        {
            Map<String, Object> formData = inst.getFormDataMap();
            List<String> row = new ArrayList<>();
            for (String key : fieldKeys)
            {
                Object val = formData != null ? formData.get(key) : null;
                row.add(val == null ? "" : val.toString());
            }
            csv.append(toCsvRow(row));
        }

        byte[] data = csv.toString().getBytes(StandardCharsets.UTF_8);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(bizCode + "_data.csv", StandardCharsets.UTF_8));
        response.getOutputStream().write(UTF8_BOM);
        response.getOutputStream().write(data);
    }

    /**
     * 导入业务数据（CSV 或 Excel）
     */
    @PreAuthorize("@ss.hasPermi('lowcode:biz:import')")
    @PostMapping("/{bizCode}/import")
    public R<String> importData(@PathVariable("bizCode") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "业务编码格式非法") String bizCode,
                                @RequestParam("file") MultipartFile file) throws IOException
    {
        List<LcBizField> fields = metadataService.selectFieldsByBizCode(bizCode);
        Map<String, String> headerToKey = new LinkedHashMap<>();
        for (LcBizField f : fields)
        {
            if (f.getFieldKey() != null)
            {
                headerToKey.putIfAbsent(f.getFieldKey(), f.getFieldKey());
            }
            if (f.getFieldLabel() != null)
            {
                headerToKey.putIfAbsent(f.getFieldLabel(), f.getFieldKey());
            }
        }

        List<String[]> rows = readRows(file);
        if (rows.isEmpty())
        {
            return R.fail("文件为空或无可识别数据");
        }

        String[] header = rows.get(0);
        List<String> colKeys = new ArrayList<>();
        for (String h : header)
        {
            String label = h == null ? "" : h.trim();
            colKeys.add(headerToKey.getOrDefault(label, ""));
        }

        int success = 0;
        int failed = 0;
        for (int r = 1; r < rows.size(); r++)
        {
            String[] row = rows.get(r);
            if (isEmptyRow(row))
            {
                continue;
            }
            Map<String, Object> formData = new LinkedHashMap<>();
            for (int c = 0; c < colKeys.size() && c < row.length; c++)
            {
                String key = colKeys.get(c);
                if (key == null || key.isEmpty())
                {
                    continue;
                }
                String val = row[c];
                if (val != null && !val.isEmpty())
                {
                    formData.put(key, val);
                }
            }
            if (formData.isEmpty())
            {
                continue;
            }
            try
            {
                R<Long> res = bizService.save(bizCode, formData);
                if (res != null && res.getCode() == R.SUCCESS)
                {
                    success++;
                }
                else
                {
                    failed++;
                }
            }
            catch (Exception e)
            {
                failed++;
            }
        }

        if (success == 0)
        {
            return R.fail("导入失败，成功 0 条" + (failed > 0 ? ("，失败 " + failed + " 条") : ""));
        }
        return R.ok("成功导入 " + success + " 条数据" + (failed > 0 ? ("，失败 " + failed + " 条") : ""));
    }

    private List<String[]> readRows(MultipartFile file) throws IOException
    {
        String name = file.getOriginalFilename();
        if (name != null && (name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls")))
        {
            return readExcelRows(file.getInputStream());
        }
        return readCsvRows(file.getInputStream());
    }

    private List<String[]> readCsvRows(InputStream in) throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        in.transferTo(buf);
        byte[] bytes = buf.toByteArray();
        int start = 0;
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF)
        {
            start = 3;
        }
        String content = new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8);
        return parseCsv(content);
    }

    private List<String[]> readExcelRows(InputStream in) throws IOException
    {
        List<String[]> rows = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(in))
        {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet)
            {
                int last = row.getLastCellNum();
                String[] arr = new String[Math.max(last, 0)];
                for (int c = 0; c < arr.length; c++)
                {
                    Cell cell = row.getCell(c);
                    arr[c] = cell == null ? "" : toCellString(cell);
                }
                rows.add(arr);
            }
        }
        return rows;
    }

    private String toCellString(Cell cell)
    {
        return switch (cell.getCellType())
        {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private List<String[]> parseCsv(String content)
    {
        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        int n = content.length();
        while (i < n)
        {
            char c = content.charAt(i);
            if (inQuotes)
            {
                if (c == '"')
                {
                    if (i + 1 < n && content.charAt(i + 1) == '"')
                    {
                        field.append('"');
                        i += 2;
                        continue;
                    }
                    inQuotes = false;
                    i++;
                    continue;
                }
                field.append(c);
                i++;
                continue;
            }
            if (c == '"')
            {
                inQuotes = true;
                i++;
                continue;
            }
            if (c == ',')
            {
                row.add(field.toString());
                field.setLength(0);
                i++;
                continue;
            }
            if (c == '\r')
            {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row.toArray(new String[0]));
                row = new ArrayList<>();
                if (i + 1 < n && content.charAt(i + 1) == '\n')
                {
                    i++;
                }
                i++;
                continue;
            }
            if (c == '\n')
            {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row.toArray(new String[0]));
                row = new ArrayList<>();
                i++;
                continue;
            }
            field.append(c);
            i++;
        }
        if (field.length() > 0 || !row.isEmpty())
        {
            row.add(field.toString());
            rows.add(row.toArray(new String[0]));
        }
        return rows;
    }

    private String toCsvRow(List<String> values)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++)
        {
            if (i > 0)
            {
                sb.append(',');
            }
            sb.append(escapeCsv(values.get(i)));
        }
        sb.append("\r\n");
        return sb.toString();
    }

    private String escapeCsv(String value)
    {
        if (value == null)
        {
            return "";
        }
        boolean needQuote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        if (!needQuote)
        {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private boolean isEmptyRow(String[] row)
    {
        for (String cell : row)
        {
            if (cell != null && !cell.trim().isEmpty())
            {
                return false;
            }
        }
        return true;
    }
}
