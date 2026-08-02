package com.junsong.finance.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 查询财务报表销售数据，使用 Map 保持与现有报表查询参数兼容。
 */
@Mapper
public interface FinSaleReportMapper {

    List<Map<String, Object>> selectSaleTrendStats(Map<String, Object> params);

    int countSaleRecords(Map<String, Object> params);

    BigDecimal sumSaleQuantity(Map<String, Object> params);

    List<Map<String, Object>> selectSalesByDept(Map<String, Object> params);

    BigDecimal selectMemberSales(Map<String, Object> params);

    BigDecimal selectSeckillSales(Map<String, Object> params);

    List<Map<String, Object>> selectProductSalesRank(Map<String, Object> params);
}
