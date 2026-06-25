package com.junsong.workflow.lowcode.service.impl;

import com.junsong.common.core.utils.StringUtils;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizInstance;
import com.junsong.workflow.lowcode.service.LcBizExcelService;
import com.junsong.workflow.lowcode.service.LcBizService;
import com.junsong.workflow.lowcode.service.LcMetadataService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 低代码业务数据 Excel 导入导出实现。
 *
 * <p>基于 Apache POI 动态生成和解析 Excel，支持低代码动态字段。</p>
 *
 * @author junsong
 */
@Service
public class LcBizExcelServiceImpl implements LcBizExcelService
{
    private static final Logger log = LoggerFactory.getLogger(LcBizExcelServiceImpl.class);

    @Autowired
    private LcMetadataService metadataService;

    @Autowired
    private LcBizService bizService;

    @Override
    public byte[] exportBizData(String bizCode) throws IOException
    {
        List<LcBizField> fields = metadataService.selectFieldsByBizCode(bizCode);
        LcBizInstance query = new LcBizInstance();
        query.setBizCode(bizCode);
        List<LcBizInstance> list = bizService.list(bizCode, query);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            Sheet sheet = wb.createSheet(bizCode);
            CellStyle headerStyle = createHeaderStyle(wb);

            // 表头
            Row header = sheet.createRow(0);
            List<String> fieldKeys = new ArrayList<>();
            int colIdx = 0;
            for (LcBizField f : fields)
            {
                if ("1".equals(f.getIsList()))
                {
                    Cell cell = header.createCell(colIdx++);
                    cell.setCellValue(f.getFieldLabel());
                    cell.setCellStyle(headerStyle);
                    fieldKeys.add(f.getFieldKey());
                }
            }

            // 数据行
            int rowIdx = 1;
            for (LcBizInstance inst : list)
            {
                Row row = sheet.createRow(rowIdx++);
                Map<String, Object> formData = inst.getFormDataMap();
                colIdx = 0;
                for (String key : fieldKeys)
                {
                    Object val = formData != null ? formData.get(key) : null;
                    row.createCell(colIdx++).setCellValue(val != null ? val.toString() : "");
                }
            }

            // 自动列宽
            for (int i = 0; i < fieldKeys.size(); i++)
            {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] generateTemplate(String bizCode) throws IOException
    {
        List<LcBizField> fields = metadataService.selectFieldsByBizCode(bizCode);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            Sheet sheet = wb.createSheet(bizCode + "_template");
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle requiredStyle = createRequiredStyle(wb);

            // 表头行
            Row header = sheet.createRow(0);
            int colIdx = 0;
            for (LcBizField f : fields)
            {
                Cell cell = header.createCell(colIdx++);
                cell.setCellValue(f.getFieldLabel() + ("1".equals(f.getRequired()) ? "*" : ""));
                cell.setCellStyle("1".equals(f.getRequired()) ? requiredStyle : headerStyle);
            }

            // 示例行
            Row example = sheet.createRow(1);
            colIdx = 0;
            for (LcBizField f : fields)
            {
                example.createCell(colIdx++).setCellValue(getExampleValue(f));
            }

            // 提示说明行
            Row tip = sheet.createRow(2);
            colIdx = 0;
            for (LcBizField f : fields)
            {
                tip.createCell(colIdx++).setCellValue(f.getFieldType());
            }

            for (int i = 0; i < colIdx; i++)
            {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public int importBizData(String bizCode, InputStream inputStream) throws IOException
    {
        List<LcBizField> fields = metadataService.selectFieldsByBizCode(bizCode);
        int count = 0;

        try (Workbook wb = new XSSFWorkbook(inputStream))
        {
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (!rows.hasNext()) return 0;

            Row header = rows.next();
            List<String> fieldKeys = new ArrayList<>();
            for (Cell cell : header)
            {
                String label = cell.getStringCellValue();
                // 去掉 * 号匹配字段
                String pureLabel = label.replace("*", "").trim();
                String key = fields.stream()
                    .filter(f -> f.getFieldLabel().equals(pureLabel))
                    .findFirst()
                    .map(LcBizField::getFieldKey)
                    .orElse("");
                fieldKeys.add(key);
            }

            while (rows.hasNext())
            {
                Row row = rows.next();
                if (isEmptyRow(row)) continue;

                Map<String, Object> formData = new java.util.HashMap<>();
                for (int i = 0; i < fieldKeys.size(); i++)
                {
                    String key = fieldKeys.get(i);
                    if (StringUtils.isEmpty(key)) continue;
                    Cell cell = row.getCell(i);
                    if (cell != null)
                    {
                        formData.put(key, getCellValue(cell));
                    }
                }

                if (!formData.isEmpty())
                {
                    bizService.save(bizCode, formData);
                    count++;
                }
            }
        }
        return count;
    }

    private CellStyle createHeaderStyle(Workbook wb)
    {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createRequiredStyle(Workbook wb)
    {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(Font.COLOR_RED);
        style.setFont(font);
        return style;
    }

    private String getExampleValue(LcBizField f)
    {
        return switch (f.getFieldType())
        {
            case "string", "textarea" -> "示例文本";
            case "number", "decimal", "percent" -> "100";
            case "boolean" -> "true";
            case "date" -> "2026-01-01";
            case "datetime" -> "2026-01-01 12:00:00";
            case "dict", "select" -> "option1";
            default -> "";
        };
    }

    private boolean isEmptyRow(Row row)
    {
        for (Cell cell : row)
        {
            if (cell != null && StringUtils.isNotEmpty(getCellValue(cell)))
            {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(Cell cell)
    {
        return switch (cell.getCellType())
        {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
