SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- DEV only: clear runtime workflow records and low-code business records.
-- Preserve users, permissions, workflow definitions, low-code metadata and base inventory.
TRUNCATE TABLE act_ru_variable;
TRUNCATE TABLE act_ru_identitylink;
TRUNCATE TABLE act_ru_task;
TRUNCATE TABLE act_ru_execution;
TRUNCATE TABLE act_ru_actinst;
TRUNCATE TABLE act_ru_deadletter_job;
TRUNCATE TABLE act_ru_event_subscr;
TRUNCATE TABLE act_ru_external_job;
TRUNCATE TABLE act_ru_history_job;
TRUNCATE TABLE act_ru_job;
TRUNCATE TABLE act_ru_suspended_job;
TRUNCATE TABLE act_ru_timer_job;
TRUNCATE TABLE act_hi_varinst;
TRUNCATE TABLE act_hi_identitylink;
TRUNCATE TABLE act_hi_taskinst;
TRUNCATE TABLE act_hi_actinst;
TRUNCATE TABLE act_hi_comment;
TRUNCATE TABLE act_hi_detail;
TRUNCATE TABLE act_hi_attachment;
TRUNCATE TABLE act_hi_entitylink;
TRUNCATE TABLE act_hi_procinst;
TRUNCATE TABLE act_evt_log;
TRUNCATE TABLE lc_biz_instance;
TRUNCATE TABLE finance_stocktake_history;
TRUNCATE TABLE finance_stocktake_item;
TRUNCATE TABLE finance_stocktake;

-- Only remove stocktake-generated ledger rows; preserve sales/purchase/opening inventory history.
DELETE FROM fin_stock_ledger WHERE reference_type IN ('STOCKTAKE', 'STOCKTAKE_REVERSE');

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'dev_workflow_business_clear' AS result,
       (SELECT COUNT(*) FROM lc_biz_instance) AS lowcode_instances,
       (SELECT COUNT(*) FROM finance_stocktake) AS native_stocktakes,
       (SELECT COUNT(*) FROM act_hi_procinst) AS historic_processes;
