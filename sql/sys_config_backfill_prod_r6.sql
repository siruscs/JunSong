-- =====================================================================
-- sys_config 参数补数（PROD 缺行修复 R6 增补）
-- 生成日期: 2026-07-01
-- 用途：补齐 PROD 相对 DEV 缺失的 2 个 config_key（门店覆盖半径、高德地图Key）。
-- 安全：仅 NOT EXISTS 守卫插入，不指定 config_id，幂等可重复执行，绝不覆盖已有值。
-- =====================================================================
SET NAMES utf8mb4;

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '门店覆盖半径(米)', 'sys.dept.coverageRadius', '1500', 'Y', 'system_backfill', NOW(),
       '门店密度查询辐射半径，单位米，默认1500米(1.5公里)'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.dept.coverageRadius');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '高德地图API Key', 'sys.map.amapKey', '354811df16ed437ed7787d2c4ecba142', 'Y', 'system_backfill', NOW(),
       '高德Web服务API Key，用于地图选点逆地理编码（经纬度转详细地址）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.map.amapKey');

-- 校验：应返回 2
SELECT COUNT(*) AS backfilled_count FROM sys_config
WHERE config_key IN ('sys.dept.coverageRadius','sys.map.amapKey');
