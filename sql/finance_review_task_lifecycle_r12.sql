-- R12-A: 复盘任务生命周期字段（幂等）

SET @col_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = BINARY 'finance_review_task'
    AND COLUMN_NAME = 'archived'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE finance_review_task ADD COLUMN archived CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''是否归档 1是 0否'' AFTER status',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = BINARY 'finance_review_task'
    AND COLUMN_NAME = 'archive_time'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE finance_review_task ADD COLUMN archive_time DATETIME NULL COMMENT ''归档时间'' AFTER archived',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = BINARY 'finance_review_task'
    AND COLUMN_NAME = 'reopen_count'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE finance_review_task ADD COLUMN reopen_count INT NOT NULL DEFAULT 0 COMMENT ''重开次数'' AFTER archive_time',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回填：已完成的和已忽略的任务自动归档
UPDATE finance_review_task
SET archived = '1', archive_time = COALESCE(update_time, NOW())
WHERE status IN ('DONE', 'IGNORED') AND archived = '0';
