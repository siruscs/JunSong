-- ============================================================
-- GENERIC 通用表字段级索引增强
-- 为 lc_biz_instance.form_data 增加常用字段的可索引生成列
-- ============================================================

-- 用存储过程实现幂等的 ADD COLUMN / ADD INDEX
DELIMITER $$
DROP PROCEDURE IF EXISTS add_generic_index$$
CREATE PROCEDURE add_generic_index()
BEGIN
    -- 金额类
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND column_name='fd_amount') THEN
        ALTER TABLE lc_biz_instance ADD COLUMN `fd_amount` DECIMAL(18,4) AS (JSON_UNQUOTE(JSON_EXTRACT(form_data, '$.amount'))) STORED COMMENT '表单金额(生成列)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND index_name='idx_fd_amount') THEN
        ALTER TABLE lc_biz_instance ADD INDEX `idx_fd_amount` (`fd_amount`);
    END IF;

    -- 合计金额
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND column_name='fd_total_amount') THEN
        ALTER TABLE lc_biz_instance ADD COLUMN `fd_total_amount` DECIMAL(18,4) AS (JSON_UNQUOTE(JSON_EXTRACT(form_data, '$.totalAmount'))) STORED COMMENT '表单合计金额(生成列)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND index_name='idx_fd_total_amount') THEN
        ALTER TABLE lc_biz_instance ADD INDEX `idx_fd_total_amount` (`fd_total_amount`);
    END IF;

    -- 日期类
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND column_name='fd_start_date') THEN
        ALTER TABLE lc_biz_instance ADD COLUMN `fd_start_date` DATE AS (JSON_UNQUOTE(JSON_EXTRACT(form_data, '$.startDate'))) STORED COMMENT '表单开始日期(生成列)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND column_name='fd_occur_date') THEN
        ALTER TABLE lc_biz_instance ADD COLUMN `fd_occur_date` DATE AS (JSON_UNQUOTE(JSON_EXTRACT(form_data, '$.occurDate'))) STORED COMMENT '表单发生日期(生成列)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND index_name='idx_fd_start_date') THEN
        ALTER TABLE lc_biz_instance ADD INDEX `idx_fd_start_date` (`fd_start_date`);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND index_name='idx_fd_occur_date') THEN
        ALTER TABLE lc_biz_instance ADD INDEX `idx_fd_occur_date` (`fd_occur_date`);
    END IF;

    -- 枚举类
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND column_name='fd_leave_type') THEN
        ALTER TABLE lc_biz_instance ADD COLUMN `fd_leave_type` VARCHAR(64) AS (JSON_UNQUOTE(JSON_EXTRACT(form_data, '$.leaveType'))) STORED COMMENT '请假类型(生成列)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND column_name='fd_expense_type') THEN
        ALTER TABLE lc_biz_instance ADD COLUMN `fd_expense_type` VARCHAR(64) AS (JSON_UNQUOTE(JSON_EXTRACT(form_data, '$.expenseType'))) STORED COMMENT '报销类型(生成列)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND column_name='fd_purchase_type') THEN
        ALTER TABLE lc_biz_instance ADD COLUMN `fd_purchase_type` VARCHAR(64) AS (JSON_UNQUOTE(JSON_EXTRACT(form_data, '$.purchaseType'))) STORED COMMENT '采购类型(生成列)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND index_name='idx_fd_leave_type') THEN
        ALTER TABLE lc_biz_instance ADD INDEX `idx_fd_leave_type` (`fd_leave_type`);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND index_name='idx_fd_expense_type') THEN
        ALTER TABLE lc_biz_instance ADD INDEX `idx_fd_expense_type` (`fd_expense_type`);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND index_name='idx_fd_purchase_type') THEN
        ALTER TABLE lc_biz_instance ADD INDEX `idx_fd_purchase_type` (`fd_purchase_type`);
    END IF;

    -- 组合索引：加速"我的单据"查询
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='lc_biz_instance' AND index_name='idx_biz_submitter_status') THEN
        ALTER TABLE lc_biz_instance ADD INDEX `idx_biz_submitter_status` (`biz_code`, `submitter`, `workflow_status`);
    END IF;
END$$
DELIMITER ;

CALL add_generic_index();
DROP PROCEDURE IF EXISTS add_generic_index;
