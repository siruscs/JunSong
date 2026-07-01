package com.junsong.open.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * JunSong开放平台 Java SDK 使用示例
 *
 * 演示如何使用HMAC-SHA256签名调用开放API
 */
public class JunSongOpenApiExample
{
    private static final String BASE_URL = "http://localhost:8081/openapi/v1";
    private static final String APP_KEY = "js_d30da74ff5b14d5cb6fa2bf97789d1dc";
    private static final String APP_SECRET = "f487dccac151496f8b9d33e6977d80aceec4591be3c64d4188d8dfb9b4d3d707";

    public static void main(String[] args) throws Exception
    {
        String method = "GET";
        String path = "/app/list";
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String body = "";

        String signStr = method + "/openapi/v1" + path + timestamp + nonce + body;
        String signature = hmacSha256(APP_SECRET, signStr);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("X-App-Key", APP_KEY)
                .header("X-App-Timestamp", timestamp)
                .header("X-App-Nonce", nonce)
                .header("X-App-Signature", signature)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("状态码: " + response.statusCode());
        System.out.println("X-API-Version: " + response.headers().firstValue("X-API-Version").orElse("无"));
        System.out.println("X-RateLimit-Limit: " + response.headers().firstValue("X-RateLimit-Limit").orElse("无"));
        System.out.println("X-RateLimit-Remaining: " + response.headers().firstValue("X-RateLimit-Remaining").orElse("无"));
        System.out.println("响应: " + response.body());
    }

    private static String hmacSha256(String secret, String data) throws Exception
    {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash)
        {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
