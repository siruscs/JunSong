package com.junsong.finance.controller;

import com.junsong.common.core.domain.R;
import com.junsong.finance.service.ExpenseOcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/expense")
public class ExpenseOcrController
{
    @Autowired
    private ExpenseOcrService ocrService;

    @PostMapping("/ocr")
    public R<ExpenseOcrService.OcrResult> recognizeExpense(@RequestParam("file") MultipartFile file)
    {
        try
        {
            ExpenseOcrService.OcrResult result = ocrService.recognize(file);
            return R.ok(result);
        }
        catch (Exception e)
        {
            return R.fail("OCR\u8bc6\u522b\u5931\u8d25: " + e.getMessage());
        }
    }
}
