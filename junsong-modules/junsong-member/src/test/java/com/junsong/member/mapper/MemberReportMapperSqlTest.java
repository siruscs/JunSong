package com.junsong.member.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class MemberReportMapperSqlTest
{
    @Test
    void countRepurchaseMembersReturnsSingleCountRow() throws Exception
    {
        String xml = Files.readString(Path.of("src/main/resources/mapper/member/MemberReportMapper.xml"), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("<select id=\"countRepurchaseMembers\"[\\s\\S]*?</select>").matcher(xml);

        assertTrue(matcher.find(), "countRepurchaseMembers SQL should exist");

        String sql = matcher.group();
        assertTrue(sql.contains("SELECT COUNT(*)"), "repurchase count should be an outer COUNT(*)");
        assertTrue(sql.contains("FROM ("), "repurchase count should wrap grouped members in a subquery");
        assertFalse(sql.contains("SELECT COUNT(DISTINCT r.member_id)"),
                "direct grouped COUNT(DISTINCT) can return zero or multiple rows for an int mapper method");
    }

    @Test
    void sumPointsRedemptionCostUsesGoodsValueInsteadOfMissingExchangeColumn() throws Exception
    {
        String xml = Files.readString(Path.of("src/main/resources/mapper/member/MemberReportMapper.xml"), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("<select id=\"sumPointsRedemptionCost\"[\\s\\S]*?</select>").matcher(xml);

        assertTrue(matcher.find(), "sumPointsRedemptionCost SQL should exist");

        String sql = matcher.group();
        assertFalse(sql.contains("points_value"), "mem_points_exchange has no points_value column");
        assertTrue(sql.contains("mem_points_goods"), "redemption cost should come from the goods value table");
        assertTrue(sql.contains("goods_value"), "redemption cost should use mem_points_goods.goods_value");
        assertTrue(sql.contains("quantity"), "redemption cost should account for exchanged quantity");
    }
}
