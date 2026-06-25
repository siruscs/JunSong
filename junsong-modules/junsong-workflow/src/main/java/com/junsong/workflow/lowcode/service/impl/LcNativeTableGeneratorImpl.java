package com.junsong.workflow.lowcode.service.impl;

import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.service.LcNativeTableGenerator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
        for (LcBizField f : fields)
        {
            String colName = f.getFieldKey();
            String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
            Integer count = queryForInt(sql, tableName, colName);
            if (count == null || count == 0)
            {
                String colDef = buildColumnDef(f);
                if (colDef != null)
                {
                    String alterSql = "ALTER TABLE `" + tableName + "` ADD COLUMN " + colDef;
                    executeSql(alterSql);
                    log.info("NATIVE 表新增列: {}.{}", tableName, colName);
                }
            }
        }
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
