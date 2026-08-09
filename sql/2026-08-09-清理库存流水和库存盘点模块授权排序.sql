SET NAMES utf8mb4;

-- ============================================================
-- 清理 stockLedger 和 stocktake 的小程序模块排序和授权记录
--
-- 原因：
-- 1. stockLedger（库存流水）是 stockCost（库存与成本）的三级页面（库存查询→库存流水明细），
--    不应单独设置模块权限。后端 hasFrontendPage 已改为 false，前端通过 authKey:'stockCost' 继承访问权限。
-- 2. stocktake（库存盘点）小程序端无对应功能入口（modules.js 缺少 customPage），
--    后端 hasFrontendPage 已改为 false，不再出现在权限配置页。
--
-- 本脚本可重复执行，清理后不影响已有功能。
-- 执行方式：mysql --default-character-set=utf8mb4 < 本文件
-- ============================================================

-- 1. 清理 sys_mp_module_sort 表中的排序记录
DELETE FROM sys_mp_module_sort WHERE module_key IN ('stockLedger', 'stocktake');
SELECT CONCAT('sys_mp_module_sort 清理行数: ', ROW_COUNT()) AS cleanup_result;

-- 2. 清理 mem_mp_role_module 表中的角色授权记录
DELETE FROM mem_mp_role_module WHERE module_key IN ('stockLedger', 'stocktake');
SELECT CONCAT('mem_mp_role_module 清理行数: ', ROW_COUNT()) AS cleanup_result;

-- 3. 验证清理结果
SELECT '验证：sys_mp_module_sort 中不再包含 stockLedger/stocktake' AS check_item;
SELECT COUNT(*) AS remaining_count FROM sys_mp_module_sort WHERE module_key IN ('stockLedger', 'stocktake');

SELECT '验证：mem_mp_role_module 中不再包含 stockLedger/stocktake' AS check_item;
SELECT COUNT(*) AS remaining_count FROM mem_mp_role_module WHERE module_key IN ('stockLedger', 'stocktake');
