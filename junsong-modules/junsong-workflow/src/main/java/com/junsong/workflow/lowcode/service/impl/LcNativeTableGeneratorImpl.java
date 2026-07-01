package com.junsong.workflow.lowcode.service.impl;

import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.service.LcNativeTableGenerator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * NATIVE 存储模式：自动根据业务对象字段元数据生成独立物理表。
 *
 * <p>表名规则：lc_instance_{bizCode}</p>
 * <p>字段名：fieldKey 作为列名，fieldType 映射为 MySQL 类型</p>
 *
 * @author Genesis·峻松
 */
@Service
public class LcNativeTableGeneratorImpl implements LcNativeTableGenerator
{
    private static final Logger log = LoggerFactory.getLogger(LcNativeTableGeneratorImpl.class);

    /**
     * 系统内置列名集合, 这些列由 createTable 创建, 严禁在 ALTER 时被 DROP.
     */
    private static final Set<String> SYSTEM_COLUMNS = Set.of(
            "id", "biz_code", "order_no",
            "process_instance_id", "workflow_status", "current_task_name",
            "submitter", "submit_time",
            "del_flag", "create_by", "create_time", "update_by", "update_time"
    );

    @Autowired
    private DataSource dataSource;

    @Override
    public void generateOrUpdateTable(LcBizObject bizObject, List<LcBizField> fields)
    {
        if (bizObject == null || bizObject.getBizCode() == null) return;
        String tableName = "lc_instance_" + bizObject.getBizCode();

        boolean tableExists = tableExists(tableName);
        if (!tableExists)
        {
            createTable(tableName, bizObject, fields);
        }
        else
        {
            alterTable(tableName, fields);
        }
    }

    private boolean tableExists(String tableName)
    {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        catch (SQLException e)
        {
            log.error("检查表存在性失败: {}", tableName, e);
        }
        return false;
    }

    private void executeSql(String sql)
    {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            log.error("SQL 执行失败: {}", sql, e);
            throw new RuntimeException("NATIVE 建表失败: " + e.getMessage(), e);
        }
    }

    private Integer queryForInt(String sql, String... params)
    {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next()) return rs.getInt(1);
            }
        }
        catch (SQLException e)
        {
            log.error("查询失败: {}", sql, e);
        }
        return null;
    }

    private void createTable(String tableName, LcBizObject bizObject, List<LcBizField> fields)
    {
        List<String> columns = new ArrayList<>();
        columns.add("`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY");
        columns.add("`biz_code` VARCHAR(64) NOT NULL DEFAULT '" + bizObject.getBizCode() + "'");
        columns.add("`order_no` VARCHAR(64) NOT NULL");

        if (fields != null)
        {
            for (LcBizField f : fields)
            {
                String colDef = buildColumnDef(f);
                if (colDef != null) columns.add(colDef);
            }
        }

        columns.add("`process_instance_id` VARCHAR(64) DEFAULT NULL");
        columns.add("`workflow_status` VARCHAR(64) NOT NULL DEFAULT 'DRAFT'");
        columns.add("`current_task_name` VARCHAR(128) DEFAULT NULL");
        columns.add("`submitter` VARCHAR(64) DEFAULT NULL");
        columns.add("`submit_time` DATETIME DEFAULT NULL");
        columns.add("`del_flag` CHAR(1) NOT NULL DEFAULT '0'");
        columns.add("`create_by` VARCHAR(64) DEFAULT NULL");
        columns.add("`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP");
        columns.add("`update_by` VARCHAR(64) DEFAULT NULL");
        columns.add("`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

        String sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (\n" +
                String.join(",\n", columns) + "\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码-NATIVE实例-" + bizObject.getBizName() + "'";

        executeSql(sql);
        log.info("NATIVE 表创建成功: {}", tableName);

        // 添加索引
        executeSql("CREATE UNIQUE INDEX uk_" + tableName + "_order ON `" + tableName + "` (`biz_code`, `order_no`)");
        executeSql("CREATE INDEX idx_" + tableName + "_status ON `" + tableName + "` (`workflow_status`)");
    }

    private void alterTable(String tableName, List<LcBizField> fields)
    {
        if (fields == null) return;

        // 1. 查询现有表的所有列名及类型 (column_name -> column_type)
        Map<String, String> existingColumns = queryExistingColumns(tableName);
        if (existingColumns == null)
        {
            log.warn("查询现有列失败, 跳过 ALTER: {}", tableName);
            return;
        }

        // 2. 收集最新字段列表 (fieldKey -> field)
        Map<String, LcBizField> latestFields = new HashMap<>();
        for (LcBizField f : fields)
        {
            if (f.getFieldKey() != null) latestFields.put(f.getFieldKey(), f);
        }

        // 3. 处理 DROP COLUMN: 现有列不在最新字段列表中 -> 删除物理列
        for (Map.Entry<String, String> entry : existingColumns.entrySet())
        {
            String colName = entry.getKey();
            // 跳过系统内置列, 绝不删除
            if (isSystemColumn(colName)) continue;

            if (!latestFields.containsKey(colName))
            {
                // 检查该列是否被索引引用, 避免误删导致错误
                if (isColumnIndexed(tableName, colName))
                {
                    log.warn("列 {}.{} 被索引引用, 跳过 DROP 以避免错误", tableName, colName);
                    continue;
                }
                executeSql("ALTER TABLE `" + tableName + "` DROP COLUMN `" + colName + "`");
                log.info("NATIVE 表删除列: {}.{}", tableName, colName);
            }
        }

        // 4. 处理 ADD / MODIFY COLUMN
        for (LcBizField f : fields)
        {
            String colName = f.getFieldKey();
            String colDef = buildColumnDef(f);
            if (colDef == null) continue;

            if (!existingColumns.containsKey(colName))
            {
                // 新字段 -> ADD COLUMN (保留原有逻辑)
                String alterSql = "ALTER TABLE `" + tableName + "` ADD COLUMN " + colDef;
                executeSql(alterSql);
                log.info("NATIVE 表新增列: {}.{}", tableName, colName);
            }
            else
            {
                // 已存在 -> 检查类型是否变更, 只改类型不改列名 (重命名太危险, 跳过)
                String currentType = existingColumns.get(colName);
                String expectedType = mapFieldType(f);
                if (!typeEquals(currentType, expectedType))
                {
                    String alterSql = "ALTER TABLE `" + tableName + "` MODIFY COLUMN " + colDef;
                    executeSql(alterSql);
                    log.info("NATIVE 表修改列类型: {}.{} ({} -> {})", tableName, colName, currentType, expectedType);
                }
            }
        }
    }

    /**
     * 查询现有表的所有列名及类型.
     *
     * @param tableName 表名
     * @return 列名 -> 列类型 (如 "varchar(255)"), 查询失败返回 null
     */
    private Map<String, String> queryExistingColumns(String tableName)
    {
        Map<String, String> columns = new HashMap<>();
        String sql = "SELECT column_name, column_type FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    columns.put(rs.getString("column_name"), rs.getString("column_type"));
                }
            }
        }
        catch (SQLException e)
        {
            log.error("查询现有列失败: {}", tableName, e);
            return null;
        }
        return columns;
    }

    /**
     * 判断是否为系统内置列 (不可删除).
     */
    private boolean isSystemColumn(String colName)
    {
        return SYSTEM_COLUMNS.contains(colName);
    }

    /**
     * 检查列是否被索引引用.
     *
     * @param tableName 表名
     * @param colName   列名
     * @return true 表示该列至少被一个索引引用
     */
    private boolean isColumnIndexed(String tableName, String colName)
    {
        String sql = "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        Integer count = queryForInt(sql, tableName, colName);
        return count != null && count > 0;
    }

    /**
     * 比较列类型是否一致 (忽略大小写).
     *
     * @param currentType  现有列类型 (来自 information_schema, 如 "varchar(255)")
     * @param expectedType 期望类型 (来自 mapFieldType, 如 "VARCHAR(255)")
     * @return true 表示类型一致
     */
    private boolean typeEquals(String currentType, String expectedType)
    {
        if (currentType == null || expectedType == null) return false;
        return currentType.equalsIgnoreCase(expectedType);
    }

    private String buildColumnDef(LcBizField field)
    {
        if (field == null || field.getFieldKey() == null) return null;
        String name = "`" + field.getFieldKey() + "`";
        String type = mapFieldType(field);
        String nullable = "1".equals(field.getRequired()) ? "NOT NULL" : "DEFAULT NULL";
        String comment = field.getFieldLabel() != null ? " COMMENT '" + field.getFieldLabel().replace("'", "\\'") + "'" : "";
        return name + " " + type + " " + nullable + comment;
    }

    private String mapFieldType(LcBizField field)
    {
        String ft = field.getFieldType();
        if (ft == null) return "VARCHAR(255)";
        return switch (ft)
        {
            case "text" -> "VARCHAR(500)";
            case "number" -> "DECIMAL(18,4)";
            case "boolean" -> "TINYINT(1)";
            case "percent" -> "DECIMAL(5,4)";
            case "money" -> "DECIMAL(18,2)";
            case "date" -> "DATE";
            case "datetime" -> "DATETIME";
            case "dict", "select" -> "VARCHAR(64)";
            case "multi-select", "region", "address", "geo", "file", "image" -> "JSON";
            case "sys-ref" -> "VARCHAR(64)";
            case "computed" -> "DECIMAL(18,4)";
            case "richtext" -> "TEXT";
            default -> "VARCHAR(255)";
        };
    }
}
