-- 销售超库存出库开关
-- 默认 false：新增/修改销售记录时，库存不足仍然阻断。
-- 改为 true：允许先销售后进货，销售出库可使 fin_stock_position.quantity 暂时为负数。
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT
  '财务库存-允许销售超库存出库',
  'finance.stock.allowNegativeSaleOut',
  'false',
  'Y',
  'admin',
  NOW(),
  'true=允许销售出库后库存暂时为负，适用于先销售后进货发放；false=库存不足时阻断销售出库'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_config WHERE config_key = 'finance.stock.allowNegativeSaleOut'
);
