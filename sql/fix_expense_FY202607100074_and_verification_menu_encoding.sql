SET NAMES utf8mb4;

-- 生产数据纠错：FY202607100074 为人工误核销。
-- 保留 LEGACY 批次及明细快照，仅将批次标记为人工纠错反核销。
DELIMITER //
DROP PROCEDURE IF EXISTS correct_expense_FY202607100074//
CREATE PROCEDURE correct_expense_FY202607100074()
correction: BEGIN
  DECLARE v_batch_status VARCHAR(16);
  DECLARE v_source_type VARCHAR(16);
  DECLARE v_batch_expense DECIMAL(18,2);
  DECLARE v_batch_advance DECIMAL(18,2);
  DECLARE v_batch_difference DECIMAL(18,2);
  DECLARE v_reverse_request_id VARCHAR(64);
  DECLARE v_expense_status CHAR(1);
  DECLARE v_expense_amount DECIMAL(18,2);
  DECLARE v_expense_advance_id BIGINT;
  DECLARE v_period_status CHAR(1);
  DECLARE v_carry_forward_time DATETIME;
  DECLARE v_period_total DECIMAL(18,2);
  DECLARE v_detail_count INT;
  DECLARE v_detail_match_count INT;
  DECLARE v_advance_detail_count INT;
  DECLARE v_verified_sum DECIMAL(18,2);

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT status, source_type, total_expense_amount, total_advance_amount, difference_amount, reverse_request_id
    INTO v_batch_status, v_source_type, v_batch_expense, v_batch_advance, v_batch_difference, v_reverse_request_id
  FROM fin_expense_verify_batch
  WHERE batch_id = 2451 AND tenant_id = 1 AND dept_id = 213
  FOR UPDATE;

  SELECT status, expense_amount, advance_id
    INTO v_expense_status, v_expense_amount, v_expense_advance_id
  FROM fin_expense
  WHERE expense_id = 12884 AND expense_no = 'FY202607100074'
    AND tenant_id = 1 AND dept_id = 213 AND period_id = 11 AND del_flag = '0'
  FOR UPDATE;

  SELECT status, carry_forward_time, total_verified_expense
    INTO v_period_status, v_carry_forward_time, v_period_total
  FROM fin_accounting_period
  WHERE period_id = 11 AND tenant_id = 1 AND dept_id = 213 AND del_flag = '0'
  FOR UPDATE;

  SELECT COUNT(*) INTO v_detail_count
  FROM fin_expense_verify_detail
  WHERE batch_id = 2451 AND tenant_id = 1 AND dept_id = 213;

  SELECT COUNT(*) INTO v_detail_match_count
  FROM fin_expense_verify_detail
  WHERE batch_id = 2451 AND expense_id = 12884
    AND tenant_id = 1 AND dept_id = 213
    AND expense_amount = 3260.00 AND original_status = '1'
    AND original_advance_id IS NULL AND period_id = 11;

  SELECT COUNT(*) INTO v_advance_detail_count
  FROM fin_advance_verify_detail
  WHERE batch_id = 2451 AND tenant_id = 1 AND dept_id = 213;

  SELECT COALESCE(SUM(expense_amount), 0) INTO v_verified_sum
  FROM fin_expense
  WHERE tenant_id = 1 AND dept_id = 213 AND period_id = 11
    AND status = '1' AND del_flag = '0';

  -- 若上次执行已完成费用纠错但后续菜单修复失败，精确识别完成态并允许重试后半段。
  IF (v_batch_status <=> 'REVERSED') AND (v_source_type <=> 'LEGACY')
     AND (v_reverse_request_id <=> 'MANUAL-FY202607100074-20260711')
     AND (v_batch_expense <=> 3260.00) AND (v_batch_advance <=> 0.00)
     AND (v_batch_difference <=> 3260.00) AND (v_expense_status <=> '0')
     AND (v_expense_amount <=> 3260.00) AND v_expense_advance_id IS NULL
     AND (v_period_status <=> '0') AND v_carry_forward_time IS NULL
     AND (v_period_total <=> 69500.00) AND (v_verified_sum <=> 69500.00)
     AND (v_detail_count <=> 1) AND (v_detail_match_count <=> 1)
     AND (v_advance_detail_count <=> 0) THEN
    COMMIT;
    LEAVE correction;
  END IF;

  IF NOT (v_batch_status <=> 'VERIFIED') OR NOT (v_source_type <=> 'LEGACY')
     OR NOT (v_batch_expense <=> 3260.00) OR NOT (v_batch_advance <=> 0.00)
     OR NOT (v_batch_difference <=> 3260.00) OR NOT (v_expense_status <=> '1')
     OR NOT (v_expense_amount <=> 3260.00) OR v_expense_advance_id IS NOT NULL
     OR NOT (v_period_status <=> '0') OR v_carry_forward_time IS NOT NULL
     OR NOT (v_period_total <=> 72760.00) OR NOT (v_verified_sum <=> 72760.00)
     OR NOT (v_detail_count <=> 1) OR NOT (v_detail_match_count <=> 1)
     OR NOT (v_advance_detail_count <=> 0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FY202607100074 correction preconditions failed';
  END IF;

  UPDATE fin_expense_verify_batch
  SET status = 'REVERSED', reverse_by = 'system', reverse_time = NOW(),
      reverse_reason = '人工纠错：FY202607100074 此前被手工误核销',
      reverse_request_id = 'MANUAL-FY202607100074-20260711',
      version = version + 1, update_time = NOW()
  WHERE batch_id = 2451 AND tenant_id = 1 AND dept_id = 213
    AND source_type = 'LEGACY' AND status = 'VERIFIED';
  IF ROW_COUNT() <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'expense verification batch correction failed';
  END IF;

  UPDATE fin_expense
  SET status = '0', advance_id = NULL, verify_by = NULL, verify_time = NULL,
      update_by = 'system', update_time = NOW()
  WHERE expense_id = 12884 AND expense_no = 'FY202607100074'
    AND tenant_id = 1 AND dept_id = 213 AND period_id = 11
    AND status = '1' AND del_flag = '0';
  IF ROW_COUNT() <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'expense status correction failed';
  END IF;

  UPDATE fin_accounting_period
  SET total_verified_expense = 69500.00, update_by = 'system', update_time = NOW()
  WHERE period_id = 11 AND tenant_id = 1 AND dept_id = 213
    AND status = '0' AND carry_forward_time IS NULL
    AND total_verified_expense = 72760.00 AND del_flag = '0';
  IF ROW_COUNT() <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'accounting period total correction failed';
  END IF;

  SELECT COALESCE(SUM(expense_amount), 0) INTO v_verified_sum
  FROM fin_expense
  WHERE tenant_id = 1 AND dept_id = 213 AND period_id = 11
    AND status = '1' AND del_flag = '0';
  IF NOT (v_verified_sum <=> 69500.00) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'verified expense reconciliation failed';
  END IF;

  COMMIT;
END//
CALL correct_expense_FY202607100074()//
DROP PROCEDURE correct_expense_FY202607100074//
DELIMITER ;

-- 菜单乱码独立事务修复，只接受本次已确认的错误字节，避免覆盖后续合法改名。
DELIMITER //
DROP PROCEDURE IF EXISTS correct_verification_menu_encoding//
CREATE PROCEDURE correct_verification_menu_encoding()
BEGIN
  DECLARE v_verify_valid INT;
  DECLARE v_unverify_valid INT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;
  UPDATE sys_menu
  SET menu_name = '费用核销', remark = '费用单笔及批量核销权限',
      update_by = 'system', update_time = NOW()
  WHERE menu_id = 3225 AND perms = 'finance:expense:verify'
    AND HEX(menu_name) = 'C3A8C2B4C2B9C3A7E2809DC2A8C3A6C2A0C2B8C3A9E2809DE282AC';
  UPDATE sys_menu
  SET menu_name = '费用反核销', remark = '费用整批反核销权限',
      update_by = 'system', update_time = NOW()
  WHERE menu_id = 3226 AND perms = 'finance:expense:unverify'
    AND HEX(menu_name) = 'C3A8C2B4C2B9C3A7E2809DC2A8C3A5C28FC28DC3A6C2A0C2B8C3A9E2809DE282AC';
  SELECT COUNT(*) INTO v_verify_valid
  FROM sys_menu
  WHERE menu_id = 3225 AND parent_id = 2040 AND perms = 'finance:expense:verify'
    AND menu_name = '费用核销' AND remark = '费用单笔及批量核销权限';
  SELECT COUNT(*) INTO v_unverify_valid
  FROM sys_menu
  WHERE menu_id = 3226 AND parent_id = 2040 AND perms = 'finance:expense:unverify'
    AND menu_name = '费用反核销' AND remark = '费用整批反核销权限';

  IF v_verify_valid <> 1 OR v_unverify_valid <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'verification menu encoding correction failed';
  END IF;
  COMMIT;
END//
CALL correct_verification_menu_encoding()//
DROP PROCEDURE correct_verification_menu_encoding//
DELIMITER ;

SELECT expense_id, expense_no, status, advance_id, verify_by, verify_time
FROM fin_expense WHERE expense_id = 12884 AND tenant_id = 1 AND dept_id = 213;
SELECT batch_id, status, source_type, reverse_by, reverse_time, reverse_reason
FROM fin_expense_verify_batch WHERE batch_id = 2451 AND tenant_id = 1 AND dept_id = 213;
SELECT period_id, status, carry_forward_time, total_verified_expense
FROM fin_accounting_period WHERE period_id = 11 AND tenant_id = 1 AND dept_id = 213;
SELECT menu_id, menu_name, HEX(menu_name), perms, remark
FROM sys_menu WHERE menu_id IN (3225, 3226) AND parent_id = 2040;
