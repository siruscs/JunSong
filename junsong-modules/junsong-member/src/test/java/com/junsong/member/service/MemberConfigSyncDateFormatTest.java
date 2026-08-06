package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

/**
 * 验证配置同步快照的日期序列化格式统一使用 yyyy-MM-dd HH:mm:ss，
 * 避免 ISO T 分隔符或时间戳导致前端解析不一致。
 */
class MemberConfigSyncDateFormatTest
{
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ObjectMapper buildObjectMapper()
    {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DT_FMT));
        javaTimeModule.addSerializer(java.time.LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        return new ObjectMapper()
                .registerModule(javaTimeModule)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void localDateTimeSerializedAsSpaceSeparatedString() throws Exception
    {
        ObjectMapper mapper = buildObjectMapper();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        LocalDateTime createTime = LocalDateTime.of(2026, 8, 6, 14, 30, 0);
        snapshot.put("createTime", createTime);

        String json = mapper.writeValueAsString(snapshot);

        assertTrue(json.contains("\"2026-08-06 14:30:00\""),
                "LocalDateTime 应序列化为 yyyy-MM-dd HH:mm:ss，实际: " + json);
        assertTrue(!json.contains("T14:30:00"),
                "不应包含 ISO T 分隔符，实际: " + json);
    }

    @Test
    void localDateSerializedAsDateString() throws Exception
    {
        ObjectMapper mapper = buildObjectMapper();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        java.time.LocalDate saleDate = java.time.LocalDate.of(2026, 8, 6);
        snapshot.put("saleDate", saleDate);

        String json = mapper.writeValueAsString(snapshot);

        assertTrue(json.contains("\"2026-08-06\""),
                "LocalDate 应序列化为 yyyy-MM-dd，实际: " + json);
    }

    @Test
    void timestampsNotSerializedAsNumbers() throws Exception
    {
        ObjectMapper mapper = buildObjectMapper();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("updateTime", LocalDateTime.of(2026, 8, 6, 9, 0, 0));

        String json = mapper.writeValueAsString(snapshot);

        assertTrue(!json.matches(".*\"updateTime\"\\s*:\\s*\\d+.*"),
                "不应序列化为时间戳数字，实际: " + json);
    }
}
