SET NAMES utf8mb4;

-- 阶段 0.1：为低代码字段增加业务值类型和统一精度/时区元数据。
-- 仅新增可空字段，不修改既有数据；历史配置保持兼容读取。
ALTER TABLE lc_biz_field
    ADD COLUMN IF NOT EXISTS value_type VARCHAR(16) NULL COMMENT '业务值类型：quantity/amount/date/datetime' AFTER field_type,
    ADD COLUMN IF NOT EXISTS scale INT NULL COMMENT '业务数值小数位：数量3位、金额2位' AFTER value_type,
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(64) NULL COMMENT '业务时区，日期时间固定 Asia/Shanghai' AFTER scale,
    ADD COLUMN IF NOT EXISTS display_format VARCHAR(32) NULL COMMENT '页面与导出展示格式' AFTER timezone;

SELECT 'lc_biz_field' AS table_name,
       COUNT(*) AS total_rows,
       SUM(value_type = 'quantity') AS quantity_rows,
       SUM(value_type = 'amount') AS amount_rows,
       SUM(value_type IN ('date', 'datetime')) AS date_rows
FROM lc_biz_field
WHERE del_flag = '0';
