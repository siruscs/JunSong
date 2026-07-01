package com.junsong.system.service.impl;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Webhook URL安全校验器
 *
 * 防御SSRF攻击：
 * - 仅允许 http / https 协议
 * - 解析DNS后禁止回环、内网、链路本地、多播地址
 * - 禁止空host或无效URL
 *
 * @author junsong
 */
public final class WebhookUrlValidator
{
    private WebhookUrlValidator() {}

    /**
     * 校验Webhook回调URL是否安全。
     *
     * @param url 回调URL
     * @throws IllegalArgumentException 如果URL不安全
     */
    public static void validate(String url)
    {
        if (url == null || url.trim().isEmpty())
        {
            throw new IllegalArgumentException("Webhook回调URL不能为空");
        }

        URI uri;
        try
        {
            uri = new URI(url.trim());
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("无效的URL格式: " + url);
        }

        // 1. 协议检查：仅允许 http 和 https
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)))
        {
            throw new IllegalArgumentException("仅允许http和https协议，当前: " + scheme);
        }

        // 2. Host检查
        String host = uri.getHost();
        if (host == null || host.trim().isEmpty())
        {
            throw new IllegalArgumentException("URL缺少有效的主机名");
        }

        // 2a. 禁止URL中嵌入凭据（防钓鱼/凭据泄露）
        if (uri.getUserInfo() != null)
        {
            throw new IllegalArgumentException("禁止URL中包含用户凭据: " + uri.getUserInfo());
        }

        // 2b. Host长度限制（防止DNS滥用/DoS）
        if (host.length() > 255)
        {
            throw new IllegalArgumentException("主机名过长: " + host.length() + " 字符（上限255）");
        }

        // 3. DNS解析后检查IP地址类别
        InetAddress[] addresses;
        try
        {
            addresses = InetAddress.getAllByName(host);
        }
        catch (UnknownHostException e)
        {
            throw new IllegalArgumentException("无法解析主机名: " + host);
        }

        for (InetAddress addr : addresses)
        {
            if (addr.isLoopbackAddress())
            {
                throw new IllegalArgumentException("禁止使用回环地址: " + host);
            }
            if (addr.isSiteLocalAddress())
            {
                throw new IllegalArgumentException("禁止使用内网地址: " + host + " -> " + addr.getHostAddress());
            }
            if (addr.isLinkLocalAddress())
            {
                throw new IllegalArgumentException("禁止使用链路本地地址: " + host);
            }
            if (addr.isAnyLocalAddress())
            {
                throw new IllegalArgumentException("禁止使用通配地址: " + host);
            }
            if (addr.isMulticastAddress())
            {
                throw new IllegalArgumentException("禁止使用多播地址: " + host);
            }
        }
    }
}
