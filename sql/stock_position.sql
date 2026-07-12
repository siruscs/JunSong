-- R6-HOTFIX: 库存当前结存表（并发安全底座）
-- 幂等：CREATE TABLE IF NOT EXISTS
-- 作用：为每个 (dept_id, product_id) 维护唯一当前库存行，
--       库存变动时先 INSERT IGNORE 建行再 SELECT ... FOR UPDATE 锁行，
--       解决"首笔流水并发下无行可锁、并发读到 0"的口径失真问题。

CREATE TABLE IF NOT EXISTS fin_stock_position (
  position_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存结存ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  dept_id BIGINT NOT NULL COMMENT '门店ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  quantity INT NOT NULL DEFAULT 0 COMMENT '当前库存数量',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (position_id),
  UNIQUE KEY uk_stock_position_tenant_dept_product (tenant_id, dept_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存当前结存表';

-- Note: change_type 新增 PURCHASE_REVERSE / SALE_REVERSE 两种反向/差额流水类型（无需改表结构）。
-- Rollback:
-- DROP TABLE IF EXISTS fin_stock_position;
