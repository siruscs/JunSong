package com.junsong.finance.service.impl;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.service.ExpenseOcrService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
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
    private static final String[] PSM_MODES = { "6", "11", "12", "4" };

    @Override
    public OcrResult recognize(MultipartFile file) throws Exception
    {
        File tempImage = File.createTempFile("ocr_", ".png");
        File processedImage = File.createTempFile("ocr_processed_", ".png");
        try
        {
            file.transferTo(tempImage);
            File imageForOcr = preprocessImage(tempImage, processedImage) ? processedImage : tempImage;
            String bestText = recognizeBestText(imageForOcr);
            return parseText(bestText);
        }
        finally
        {
            tempImage.delete();
            processedImage.delete();
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

    private boolean preprocessImage(File source, File target)
    {
        try
        {
            BufferedImage input = ImageIO.read(source);
            if (input == null)
            {
                return false;
            }
            int scale = input.getWidth() < 1400 ? 2 : 1;
            int width = input.getWidth() * scale;
            int height = input.getHeight() * scale;
            BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D graphics = output.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(input, 0, 0, width, height, null);
            graphics.dispose();
            enhanceContrast(output);
            return ImageIO.write(output, "png", target);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private void enhanceContrast(BufferedImage image)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                int rgb = image.getRGB(x, y);
                int gray = rgb & 0xff;
                int enhanced = Math.max(0, Math.min(255, (int) ((gray - 128) * 1.25 + 128)));
                int value = enhanced < 168 ? 0 : 255;
                int out = (value << 16) | (value << 8) | value;
                image.setRGB(x, y, out);
            }
        }
    }

    private String recognizeBestText(File image) throws Exception
    {
        String bestText = "";
        int bestScore = -1;
        String error = "";
        for (String psm : PSM_MODES)
        {
            File tempOutput = File.createTempFile("ocr_out_", "");
            try
            {
                String text = runTesseract(image, tempOutput, psm);
                int score = scoreText(text);
                if (score > bestScore)
                {
                    bestScore = score;
                    bestText = text;
                }
            }
            catch (Exception e)
            {
                error = e.getMessage();
            }
            finally
            {
                new File(tempOutput.getAbsolutePath() + ".txt").delete();
                tempOutput.delete();
            }
        }
        if (bestText.isBlank() && !error.isBlank())
        {
            throw new ServiceException(error);
        }
        return bestText;
    }

    private String runTesseract(File image, File output, String psm) throws Exception
    {
        ProcessBuilder pb = new ProcessBuilder(
            "tesseract", image.getAbsolutePath(), output.getAbsolutePath(), "-l", "chi_sim+eng", "--psm", psm
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String processOutput = readStream(process.getInputStream());
        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            throw new ServiceException("tesseract exit code " + exitCode + ": " + processOutput);
        }
        return readFile(new File(output.getAbsolutePath() + ".txt"));
    }

    private int scoreText(String text)
    {
        OcrResult result = parseText(text);
        int score = normalizeText(text).length();
        if (!result.getAmount().isBlank())
        {
            score += 120;
        }
        if (!result.getDate().isBlank())
        {
            score += 80;
        }
        if (!result.getPlatform().isBlank())
        {
            score += 40;
        }
        return score;
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

    private String readFile(File file) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line).append("\n");
            }
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
