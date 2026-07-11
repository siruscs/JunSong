-- R3-H: Stock Ledger Foundation T1
-- DDL only — NO menus, NO report pages
-- Idempotent: safe to run multiple times

-- 库存流水表：记录每一笔库存变动
CREATE TABLE IF NOT EXISTS fin_stock_ledger (
  ledger_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  dept_id BIGINT NOT NULL COMMENT '门店ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  product_name VARCHAR(128) DEFAULT NULL COMMENT '商品名称',
  change_type VARCHAR(32) NOT NULL COMMENT '变动类型: PURCHASE_IN/SALE_OUT/RETURN_IN/ADJUST/LOSS',
  change_quantity INT NOT NULL DEFAULT 0 COMMENT '变动数量(正增负减)',
  before_quantity INT NOT NULL DEFAULT 0 COMMENT '变动前库存',
  after_quantity INT NOT NULL DEFAULT 0 COMMENT '变动后库存',
  unit_cost DECIMAL(18,2) DEFAULT NULL COMMENT '单位成本',
  reference_type VARCHAR(64) DEFAULT NULL COMMENT '关联单据类型',
  reference_id BIGINT DEFAULT NULL COMMENT '关联单据ID',
  reference_no VARCHAR(64) DEFAULT NULL COMMENT '关联单据编号',
  remark VARCHAR(256) DEFAULT NULL COMMENT '备注',
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  del_flag CHAR(1) DEFAULT '0',
  PRIMARY KEY (ledger_id),
  KEY idx_stock_ledger_dept_product (dept_id, product_id, del_flag),
  KEY idx_stock_ledger_type (change_type, del_flag),
  KEY idx_stock_ledger_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

-- 库存快照表：定期快照各门店商品库存
CREATE TABLE IF NOT EXISTS fin_stock_snapshot (
  snapshot_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '快照ID',
  snapshot_date DATE NOT NULL COMMENT '快照日期',
  dept_id BIGINT NOT NULL COMMENT '门店ID',
  dept_name VARCHAR(128) DEFAULT NULL COMMENT '门店名称',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  product_name VARCHAR(128) DEFAULT NULL COMMENT '商品名称',
  quantity INT NOT NULL DEFAULT 0 COMMENT '期末库存数量(closing)',
  opening_quantity INT NOT NULL DEFAULT 0 COMMENT '期初库存数量(opening)',
  in_quantity INT NOT NULL DEFAULT 0 COMMENT '当日入库数量(正向流水合计)',
  out_quantity INT NOT NULL DEFAULT 0 COMMENT '当日出库数量(反向流水绝对值合计)',
  unit_cost DECIMAL(18,2) DEFAULT NULL COMMENT '单位成本',
  total_value DECIMAL(18,2) DEFAULT NULL COMMENT '库存总值',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (snapshot_id),
  UNIQUE KEY uk_stock_snapshot_date_dept_product (snapshot_date, dept_id, product_id),
  KEY idx_stock_snapshot_date (snapshot_date),
  KEY idx_stock_snapshot_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存快照表';

-- Note: 库存报表仍暂停开放，不创建任何菜单项
-- Note: 初始化流水数据暂不执行，待库存源表确认后再补充
-- Rollback:
-- DROP TABLE IF EXISTS fin_stock_snapshot;
-- DROP TABLE IF EXISTS fin_stock_ledger;
