SET NAMES utf8mb4;

-- 开放平台核心表修复。只创建缺失表；已有同名表必须具备 tenant_id，否则停止。
DELIMITER //
DROP PROCEDURE IF EXISTS assert_open_core_preconditions_20260712//
CREATE PROCEDURE assert_open_core_preconditions_20260712()
BEGIN
  DECLARE v_invalid INT DEFAULT 0;
  DECLARE v_existing INT DEFAULT 0;
  SELECT COUNT(*) INTO v_existing FROM information_schema.TABLES
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('open_app','open_app_secret','open_isv','open_contract');
  IF v_existing NOT IN (0,4) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='partial open core schema detected'; END IF;
  SELECT COUNT(*) INTO v_invalid FROM information_schema.TABLES t
  WHERE t.TABLE_SCHEMA=DATABASE()
    AND t.TABLE_NAME IN ('open_app','open_app_secret','open_isv','open_contract')
    AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS c
      WHERE c.TABLE_SCHEMA=t.TABLE_SCHEMA AND c.TABLE_NAME=t.TABLE_NAME AND c.COLUMN_NAME='tenant_id');
  IF v_invalid <> 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='existing open core table lacks tenant_id'; END IF;
END//
CALL assert_open_core_preconditions_20260712()//
DROP PROCEDURE assert_open_core_preconditions_20260712//
DELIMITER ;

CREATE TABLE IF NOT EXISTS `open_app` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `app_name` VARCHAR(100) NOT NULL DEFAULT '',
  `app_type` VARCHAR(30) NOT NULL DEFAULT 'merchant',
  `description` VARCHAR(500) DEFAULT '',
  `website_url` VARCHAR(300) DEFAULT '',
  `callback_url` VARCHAR(500) DEFAULT '',
  `contact_name` VARCHAR(50) DEFAULT '',
  `contact_phone` VARCHAR(20) DEFAULT '',
  `contact_email` VARCHAR(100) DEFAULT '',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `reject_reason` VARCHAR(500) DEFAULT '',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0',
  `create_by` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_open_app_status` (`tenant_id`,`status`,`del_flag`),
  KEY `idx_tenant_open_app_name` (`tenant_id`,`app_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `open_app_secret` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `app_id` BIGINT NOT NULL,
  `app_key` VARCHAR(128) NOT NULL,
  `app_secret` VARCHAR(512) NOT NULL,
  `key_type` VARCHAR(32) NOT NULL DEFAULT 'test',
  `status` VARCHAR(20) NOT NULL DEFAULT '0',
  `daily_quota` INT NOT NULL DEFAULT 1000,
  `expire_time` DATETIME DEFAULT NULL,
  `del_flag` CHAR(1) NOT NULL DEFAULT '0',
  `create_by` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_open_app_key` (`app_key`),
  KEY `idx_tenant_app_secret_app` (`tenant_id`,`app_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `open_isv` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `isv_name` VARCHAR(100) NOT NULL DEFAULT '',
  `contact_name` VARCHAR(50) DEFAULT '',
  `contact_phone` VARCHAR(20) DEFAULT '',
  `contact_email` VARCHAR(100) DEFAULT '',
  `company_name` VARCHAR(200) DEFAULT '',
  `business_license` VARCHAR(50) DEFAULT '',
  `website_url` VARCHAR(300) DEFAULT '',
  `access_type` VARCHAR(30) DEFAULT 'merchant',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `reject_reason` VARCHAR(500) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_isv_status` (`tenant_id`,`status`),
  KEY `idx_tenant_isv_name` (`tenant_id`,`isv_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `open_contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `contract_no` VARCHAR(50) NOT NULL DEFAULT '',
  `isv_id` BIGINT NOT NULL,
  `isv_name` VARCHAR(100) DEFAULT '',
  `contract_type` VARCHAR(20) DEFAULT 'standard',
  `title` VARCHAR(200) DEFAULT '',
  `terms` TEXT DEFAULT NULL,
  `start_date` DATE DEFAULT NULL,
  `end_date` DATE DEFAULT NULL,
  `daily_quota` INT DEFAULT 1000,
  `allowed_capabilities` VARCHAR(500) DEFAULT '',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_contract_no` (`tenant_id`,`contract_no`),
  KEY `idx_tenant_contract_isv` (`tenant_id`,`isv_id`),
  KEY `idx_tenant_contract_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER //
DROP PROCEDURE IF EXISTS assert_open_core_result_20260712//
CREATE PROCEDURE assert_open_core_result_20260712()
BEGIN
  DECLARE v_table_count INT DEFAULT 0;
  DECLARE v_tenant_count INT DEFAULT 0;
  DECLARE v_required_count INT DEFAULT 0;
  DECLARE v_column_count INT DEFAULT 0;
  DECLARE v_tenant_shape_count INT DEFAULT 0;
  DECLARE v_app_key_unique_count INT DEFAULT 0;
  SELECT COUNT(*) INTO v_table_count FROM information_schema.TABLES
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('open_app','open_app_secret','open_isv','open_contract');
  SELECT COUNT(*) INTO v_tenant_count FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='tenant_id'
    AND TABLE_NAME IN ('open_app','open_app_secret','open_isv','open_contract');
  SELECT COUNT(*) INTO v_column_count FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('open_app','open_app_secret','open_isv','open_contract');
  SELECT COUNT(*) INTO v_tenant_shape_count FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='tenant_id' AND DATA_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT='1'
    AND TABLE_NAME IN ('open_app','open_app_secret','open_isv','open_contract');
  SELECT COUNT(*) INTO v_app_key_unique_count FROM (
    SELECT INDEX_NAME FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='open_app_secret' AND INDEX_NAME='uk_open_app_key' AND NON_UNIQUE=0
    GROUP BY INDEX_NAME
    HAVING COUNT(*)=1 AND MIN(SEQ_IN_INDEX)=1 AND MAX(SEQ_IN_INDEX)=1 AND MIN(COLUMN_NAME)='app_key'
  ) exact_global_key;
  SELECT COUNT(*) INTO v_required_count FROM information_schema.COLUMNS c JOIN (
    SELECT 'open_app' t,'del_flag' n UNION ALL SELECT 'open_app','callback_url'
    UNION ALL SELECT 'open_app_secret','app_key' UNION ALL SELECT 'open_app_secret','app_secret'
    UNION ALL SELECT 'open_isv','business_license' UNION ALL SELECT 'open_contract','allowed_capabilities'
  ) r ON r.t=c.TABLE_NAME AND r.n=c.COLUMN_NAME WHERE c.TABLE_SCHEMA=DATABASE();
  IF v_table_count <> 4 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='open core table reconciliation failed'; END IF;
  IF v_tenant_count <> 4 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='open core tenant reconciliation failed'; END IF;
  IF v_column_count <> 61 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='open core complete column reconciliation failed'; END IF;
  IF v_tenant_shape_count <> 4 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='open core tenant shape reconciliation failed'; END IF;
  IF v_app_key_unique_count <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='open app key uniqueness reconciliation failed'; END IF;
  IF v_required_count <> 6 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='open core column reconciliation failed'; END IF;
END//
CALL assert_open_core_result_20260712()//
DROP PROCEDURE assert_open_core_result_20260712//
DELIMITER ;

SELECT t.TABLE_NAME,t.ENGINE,t.TABLE_COLLATION,c.IS_NULLABLE,c.COLUMN_DEFAULT
FROM information_schema.TABLES t
JOIN information_schema.COLUMNS c ON c.TABLE_SCHEMA=t.TABLE_SCHEMA AND c.TABLE_NAME=t.TABLE_NAME AND c.COLUMN_NAME='tenant_id'
WHERE t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME IN ('open_app','open_app_secret','open_isv','open_contract')
ORDER BY t.TABLE_NAME;
