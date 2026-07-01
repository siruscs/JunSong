package com.junsong.finance.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.service.ExpenseOcrService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExpenseOcrServiceImpl implements ExpenseOcrService
{
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("[¥￥]\\s*([\\d,]+(?:\\.\\d{1,2})?)");
    private static final Pattern AMOUNT_LABEL_PATTERN = Pattern.compile("(?:实付款|实付金额|实付|支付金额|付款金额|订单金额|应付金额|合计|总计|金额|总价|付款)\\s*[¥￥:：]?\\s*([\\d,]+(?:\\.\\d{1,2})?)");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})[年/\\-.](\\d{1,2})[月/\\-.](\\d{1,2})[日号]?");
    private static final Pattern SHORT_DATE_PATTERN = Pattern.compile("(?:支付时间|交易时间|下单时间|创建时间|订单时间)\\s*(\\d{1,2})[月/\\-.](\\d{1,2})[日号]?");
    private static final Pattern ANY_NUMBER_PATTERN = Pattern.compile("(?<!\\d)([1-9]\\d{0,5}(?:\\.\\d{1,2})?)(?!\\d)");

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String OCR_SCRIPT;

    static
    {
        // 优先使用容器内固定路径（Dockerfile/部署脚本会放置），不存在则从classpath提取
        File containerScript = new File("/home/junsong/scripts/paddle_ocr.py");
        if (containerScript.exists())
        {
            OCR_SCRIPT = containerScript.getAbsolutePath();
        }
        else
        {
            File script = new File(System.getProperty("java.io.tmpdir"), "paddle_ocr.py");
            try
            {
                try (InputStream is = new ClassPathResource("scripts/paddle_ocr.py").getInputStream())
                {
                    try (OutputStream os = new FileOutputStream(script))
                    {
                        is.transferTo(os);
                    }
                }
                if (!script.setExecutable(true))
                {
                    // 忽略权限设置失败
                }
            }
            catch (Exception e)
            {
                // 提取失败时尝试直接用classpath路径
            }
            OCR_SCRIPT = script.getAbsolutePath();
        }
    }

    @Override
    public OcrResult recognize(MultipartFile file) throws Exception
    {
        File tempImage = File.createTempFile("ocr_", ".png");
        try
        {
            file.transferTo(tempImage);
            String text = runPaddleOcr(tempImage);
            return parseText(text);
        }
        finally
        {
            tempImage.delete();
        }
    }

    public OcrResult parseText(String text)
    {
        String normalized = normalizeText(text);
        OcrResult result = new OcrResult();
        result.setRawText(text);
        result.setPlatform(detectPlatform(normalized));
        result.setAmount(extractAmount(normalized));
        result.setDate(extractDate(normalized));
        result.setContent(extractContent(normalized));
        return result;
    }

    /**
     * 调用 PaddleOCR Python 脚本进行识别
     * PaddleOCR 自带图片预处理（角度分类、文本检测），无需手动灰度化/增强对比度
     */
    private String runPaddleOcr(File image) throws Exception
    {
        ProcessBuilder pb = new ProcessBuilder("python3", OCR_SCRIPT, image.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String processOutput = readStream(process.getInputStream());
        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            throw new ServiceException("PaddleOCR 识别失败: " + processOutput);
        }
        // 解析 JSON 输出 {"text": "..."}
        try
        {
            JsonNode node = JSON.readTree(processOutput);
            if (node.has("error"))
            {
                throw new ServiceException("PaddleOCR 错误: " + node.get("error").asText());
            }
            return node.has("text") ? node.get("text").asText() : "";
        }
        catch (com.fasterxml.jackson.core.JsonParseException e)
        {
            // 如果不是JSON，直接返回原始输出
            return processOutput;
        }
    }

    private String readStream(InputStream is) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
        {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private String normalizeText(String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace('：', ':')
            .replace('，', ',')
            .replace('。', '.')
            .replace("O", "0")
            .replace("o", "0")
            .replaceAll("[ \\t]+", " ")
            .trim();
    }

    private String detectPlatform(String text)
    {
        String lower = text.toLowerCase(Locale.ROOT);
        if (text.contains("拼多多") || lower.contains("pinduoduo") || lower.contains("pdd"))
        {
            return "拼多多";
        }
        if (text.contains("淘宝") || text.contains("淘寶") || lower.contains("taobao") || text.contains("天猫"))
        {
            return "淘宝";
        }
        if (text.contains("京东") || text.contains("京東") || lower.contains("jd.com"))
        {
            return "京东";
        }
        if (text.contains("1688") || text.contains("阿里巴巴"))
        {
            return "1688";
        }
        if (text.contains("抖音") || lower.contains("douyin"))
        {
            return "抖音";
        }
        if (text.contains("美团") || lower.contains("meituan"))
        {
            return "美团";
        }
        if (text.contains("微信支付") || text.contains("微信零钱") || text.contains("微信"))
        {
            return "微信";
        }
        if (text.contains("支付宝") || lower.contains("alipay"))
        {
            return "支付宝";
        }
        return "";
    }

    private String extractAmount(String text)
    {
        List<AmountCandidate> candidates = new ArrayList<>();
        collectAmountCandidates(text, AMOUNT_LABEL_PATTERN, candidates, 120);
        collectAmountCandidates(text, AMOUNT_PATTERN, candidates, 40);
        collectAmountCandidates(text, ANY_NUMBER_PATTERN, candidates, 0);
        return candidates.stream()
            .filter(candidate -> candidate.amount.compareTo(BigDecimal.ZERO) > 0)
            .max(Comparator.comparingInt((AmountCandidate candidate) -> candidate.score)
                .thenComparing(candidate -> candidate.amount))
            .map(candidate -> formatAmount(candidate.amount))
            .orElse("");
    }

    private void collectAmountCandidates(String text, Pattern pattern, List<AmountCandidate> candidates, int baseScore)
    {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find())
        {
            BigDecimal amount = parseAmount(matcher.group(1));
            if (amount == null)
            {
                continue;
            }
            int start = Math.max(0, matcher.start() - 18);
            int end = Math.min(text.length(), matcher.end() + 18);
            String context = text.substring(start, end);
            int score = baseScore + amountScore(context);
            candidates.add(new AmountCandidate(amount, score));
        }
    }

    private int amountScore(String context)
    {
        int score = 0;
        if (context.matches(".*(实付款|实付金额|实付|支付金额|付款金额|应付金额).*"))
        {
            score += 160;
        }
        if (context.matches(".*(订单金额|合计|总计|总价|金额).*"))
        {
            score += 80;
        }
        if (context.matches(".*(优惠|立减|折扣|退款|退回|运费|单价|数量|余额|红包|积分).*"))
        {
            score -= 180;
        }
        return score;
    }

    private BigDecimal parseAmount(String value)
    {
        try
        {
            return new BigDecimal(value.replaceAll(",", "")).setScale(2, RoundingMode.HALF_UP);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String formatAmount(BigDecimal amount)
    {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String extractDate(String text)
    {
        Matcher m = DATE_PATTERN.matcher(text);
        if (m.find())
        {
            return m.group(1) + "-" + padZero(m.group(2)) + "-" + padZero(m.group(3));
        }
        Matcher shortMatcher = SHORT_DATE_PATTERN.matcher(text);
        if (shortMatcher.find())
        {
            return java.time.LocalDate.now().getYear() + "-" + padZero(shortMatcher.group(1)) + "-" + padZero(shortMatcher.group(2));
        }
        return "";
    }

    private String extractContent(String text)
    {
        String[] lines = text.split("\\n");
        List<String> values = new ArrayList<>();
        for (String line : lines)
        {
            String trimmed = cleanContentLine(line);
            if (!isContentLine(trimmed))
            {
                continue;
            }
            values.add(trimmed);
            if (values.size() >= 3)
            {
                break;
            }
        }
        return String.join(" ", values);
    }

    private String cleanContentLine(String line)
    {
        return line == null ? "" : line.trim().replaceAll("\\s+", " ");
    }

    private boolean isContentLine(String value)
    {
        if (value.length() < 3 || value.length() > 60)
        {
            return false;
        }
        if (value.matches(".*[¥￥]\\s*[\\d,]+.*") || value.matches(".*\\d{4}[年/\\-.]\\d{1,2}[月/\\-.]\\d{1,2}.*"))
        {
            return false;
        }
        if (value.matches(".*(实付款|实付|支付金额|付款金额|订单金额|合计|总计|优惠|运费|交易时间|支付时间|订单编号|商户单号|付款方|收款方).*"))
        {
            return false;
        }
        return value.matches(".*[\\u4e00-\\u9fa5].*");
    }

    private String padZero(String s)
    {
        return s.length() == 1 ? "0" + s : s;
    }

    private static class AmountCandidate
    {
        private final BigDecimal amount;
        private final int score;

        private AmountCandidate(BigDecimal amount, int score)
        {
            this.amount = amount;
            this.score = score;
        }
    }
}
