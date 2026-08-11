package com.junsong.workflow.lowcode.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ConfigContentHasher
{
    private ConfigContentHasher() { }

    public static String sha256(String content)
    {
        if (content == null) throw new IllegalArgumentException("配置内容不能为空");
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
    }
}
