package com.junsong.finance.service;

import com.junsong.finance.service.impl.ExpenseOcrServiceImpl;
import org.springframework.web.multipart.MultipartFile;

public interface ExpenseOcrService
{
    class OcrResult
    {
        private String platform;
        private String amount;
        private String date;
        private String content;
        private String rawText;

        public String getPlatform()
        {
            return platform;
        }

        public void setPlatform(String platform)
        {
            this.platform = platform;
        }

        public String getAmount()
        {
            return amount;
        }

        public void setAmount(String amount)
        {
            this.amount = amount;
        }

        public String getDate()
        {
            return date;
        }

        public void setDate(String date)
        {
            this.date = date;
        }

        public String getContent()
        {
            return content;
        }

        public void setContent(String content)
        {
            this.content = content;
        }

        public String getRawText()
        {
            return rawText;
        }

        public void setRawText(String rawText)
        {
            this.rawText = rawText;
        }
    }

    OcrResult recognize(MultipartFile file) throws Exception;
}
