package com.junsong.workflow.lowcode.service.impl;

import com.junsong.workflow.lowcode.service.LcReportService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 低代码报表服务实现（基于 NATIVE 存储）。
 *
 * <p>使用原生 JDBC 动态查询 NATIVE 物理表，避免 MyBatis 动态表名限制。</p>
 *
 * @author junsong
 */
@Service
public class LcReportServiceImpl implements LcReportService
{
    private static final Logger log = LoggerFactory.getLogger(LcReportServiceImpl.class);

    @Autowired
    private DataSource dataSource;

    @Override
    public List<Map<String, Object>> queryNativeData(String bizCode, Map<String, Object> params)
    {
        String tableName = "lc_instance_" + bizCode;
        StringBuilder sql = new StringBuilder("SELECT * FROM `" + tableName + "` WHERE del_flag = '0'");
        List<Object> args = new ArrayList<>();

        // 动态过滤条件（仅支持等值匹配，防止注入）
        if (params != null)
        {
            for (Map.Entry<String, Object> entry : params.entrySet())
            {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value == null || value.toString().isEmpty()) continue;
                // 排除分页和排序参数
                if (key.equals("pageNum") || key.equals("pageSize") || key.equals("orderByColumn") || key.equals("isAsc")) continue;
                // 只允许字母数字下划线的字段名
                if (!key.matches("^[a-zA-Z0-9_]+$")) continue;
                sql.append(" AND `").append(key).append("` = ?");
                args.add(value);
            }
        }

        // 排序
        String orderBy = params != null ? (String) params.get("orderByColumn") : null;
        String isAsc = params != null ? (String) params.get("isAsc") : null;
        if (orderBy != null && orderBy.matches("^[a-zA-Z0-9_]+$"))
        {
            sql.append(" ORDER BY `").append(orderBy).append("`");
            if ("desc".equalsIgnoreCase(isAsc))
            {
                sql.append(" DESC");
            }
            else
            {
                sql.append(" ASC");
            }
        }
        else
        {
            sql.append(" ORDER BY id DESC");
        }

        return executeQuery(sql.toString(), args);
    }

    @Override
    public Map<String, Object> queryNativeStatistics(String bizCode, Map<String, Object> params)
    {
        String tableName = "lc_instance_" + bizCode;
        Map<String, Object> result = new HashMap<>();

        // 总记录数
        String countSql = "SELECT COUNT(*) FROM `" + tableName + "` WHERE del_flag = '0'";
        result.put("totalCount", queryForObject(countSql, Long.class));

        // 状态分布
        String statusSql = "SELECT workflow_status, COUNT(*) as cnt FROM `" + tableName
            + "` WHERE del_flag = '0' GROUP BY workflow_status";
        result.put("statusDistribution", executeQuery(statusSql, List.of()));

        // 本月新增
        String monthSql = "SELECT COUNT(*) FROM `" + tableName
            + "` WHERE del_flag = '0' AND create_time >= DATE_FORMAT(NOW(),'%Y-%m-01')";
        result.put("monthCount", queryForObject(monthSql, Long.class));

        return result;
    }

    @Override
    public List<Map<String, Object>> queryNativeColumns(String bizCode)
    {
        String tableName = "lc_instance_" + bizCode;
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT "
            + "FROM information_schema.columns "
            + "WHERE table_schema = DATABASE() AND table_name = ?";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    Map<String, Object> col = new HashMap<>();
                    col.put("columnName", rs.getString("COLUMN_NAME"));
                    col.put("dataType", rs.getString("DATA_TYPE"));
                    col.put("comment", rs.getString("COLUMN_COMMENT"));
                    result.add(col);
                }
            }
        }
        catch (SQLException e)
        {
            log.error("查询表列信息失败: {}", tableName, e);
        }
        return result;
    }

    private List<Map<String, Object>> executeQuery(String sql, List<Object> args)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            for (int i = 0; i < args.size(); i++)
            {
                ps.setObject(i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery())
            {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next())
                {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= colCount; i++)
                    {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    result.add(row);
                }
            }
        }
        catch (SQLException e)
        {
            log.error("报表查询失败: {}", sql, e);
        }
        return result;
    }

    private <T> T queryForObject(String sql, Class<T> clazz)
    {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery())
        {
            if (rs.next())
            {
                Object value = rs.getObject(1);
                if (clazz.isInstance(value))
                {
                    return clazz.cast(value);
                }
            }
        }
        catch (SQLException e)
        {
            log.error("报表统计查询失败: {}", sql, e);
        }
        return null;
    }
}
