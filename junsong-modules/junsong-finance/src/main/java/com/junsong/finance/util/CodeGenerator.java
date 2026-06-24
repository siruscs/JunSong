package com.junsong.finance.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 编码生成工具类
 * 
 * @author junsong
 */
public class CodeGenerator
{
    private static final Random random = new Random();
    private static final String DIGITS = "0123456789";
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * 生成8位数字商品编码
     */
    public static String generateProductCode()
    {
        return generateRandomCode(DIGITS, 8);
    }

    /**
     * 生成6位数字供应商编码
     */
    public static String generateSupplierCode()
    {
        return generateRandomCode(DIGITS, 6);
    }

    /**
     * 生成进货单号：年月日 + 4位数字字母
     * 格式：yyyyMMdd + 4位数字字母（当数字不够用时自动切换为字母数字组合）
     */
    public static String generatePurchaseNo(int todayCount)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        
        String suffix;
        if (todayCount < 10000)
        {
            // 小于10000时，用4位数字
            suffix = String.format("%04d", todayCount);
        }
        else
        {
            // 超过10000时，用数字字母组合
            suffix = generateRandomCode(ALPHANUMERIC, 4);
        }
        
        return dateStr + suffix;
    }

    /**
     * 生成销售单号：SX + 年月日 + 4位数字字母
     * 格式：SXyyyyMMdd + 4位数字字母
     */
    public static String generateSaleNo(int todayCount)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        
        String suffix;
        if (todayCount < 10000)
        {
            // 小于10000时，用4位数字
            suffix = String.format("%04d", todayCount);
        }
        else
        {
            // 超过10000时，用数字字母组合
            suffix = generateRandomCode(ALPHANUMERIC, 4);
        }
        
        return "SX" + dateStr + suffix;
    }

    /**
     * 生成缴款单号：JK + 年月日 + 4位数字字母
     * 格式：JKyyyyMMdd + 4位数字字母
     */
    public static String generatePaymentNo(int todayCount)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        
        String suffix;
        if (todayCount < 10000)
        {
            // 小于10000时，用4位数字
            suffix = String.format("%04d", todayCount);
        }
        else
        {
            // 超过10000时，用数字字母组合
            suffix = generateRandomCode(ALPHANUMERIC, 4);
        }
        
        return "JK" + dateStr + suffix;
    }
    
    /**
     * 生成费用单号：FY + 年月日 + 4位数字字母
     * 格式：FYyyyyMMdd + 4位数字字母
     */
    public static String generateExpenseNo(int todayCount)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        
        String suffix;
        if (todayCount < 10000)
        {
            suffix = String.format("%04d", todayCount);
        }
        else
        {
            suffix = generateRandomCode(ALPHANUMERIC, 4);
        }
        
        return "FY" + dateStr + suffix;
    }
    
    /**
     * 生成借支单号：JZ + 年月日 + 4位数字字母
     * 格式：JZyyyyMMdd + 4位数字字母
     */
    public static String generateAdvanceNo(int todayCount)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        
        String suffix;
        if (todayCount < 10000)
        {
            suffix = String.format("%04d", todayCount);
        }
        else
        {
            suffix = generateRandomCode(ALPHANUMERIC, 4);
        }
        
        return "JZ" + dateStr + suffix;
    }
    
    /**
     * 生成成本核算单号：HS + 年月日 + 4位数字字母
     * 格式：HSyyyyMMdd + 4位数字字母
     */
    public static String generateCostAccountingNo(int todayCount)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        
        String suffix;
        if (todayCount < 10000)
        {
            suffix = String.format("%04d", todayCount);
        }
        else
        {
            suffix = generateRandomCode(ALPHANUMERIC, 4);
        }
        
        return "HS" + dateStr + suffix;
    }
    
    /**
     * 生成投资人返款单号：FK + 年月日 + 4位数字字母
     * 格式：FKyyyyMMdd + 4位数字字母
     */
    public static String generateInvestorPaymentNo(int todayCount)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        
        String suffix;
        if (todayCount < 10000)
        {
            suffix = String.format("%04d", todayCount);
        }
        else
        {
            suffix = generateRandomCode(ALPHANUMERIC, 4);
        }
        
        return "FK" + dateStr + suffix;
    }

    /**
     * 生成指定长度的随机编码
     */
    private static String generateRandomCode(String chars, int length)
    {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
        {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
