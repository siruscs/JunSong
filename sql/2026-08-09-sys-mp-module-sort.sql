SET NAMES utf8mb4;

-- 小程序模块显示顺序配置表（PC端可拖动调整，后端按此顺序向小程序返回模块）
CREATE TABLE IF NOT EXISTS `sys_mp_module_sort` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `module_key` varchar(64) NOT NULL COMMENT '模块key(对应MpModuleCatalog.Module.key)',
  `group_name` varchar(32) DEFAULT NULL COMMENT '所属分组(会员服务/会员运营/财务管理/系统管理/移动办公)',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '排序值(越小越靠前)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_key` (`module_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序模块显示顺序配置表';

-- 把默认顺序写入（基于当前MpModuleCatalog ALL顺序），避免刚建表时DB里没数据，所有模块DB排序都为0导致前端乱序
-- 写入顺序采用 MpModuleCatalog.java 里的 ALL 数组下标
INSERT IGNORE INTO `sys_mp_module_sort` (`module_key`, `group_name`, `sort_order`) VALUES
('member',                 '会员服务', 10),
('memberPurchase',         '会员服务', 20),
('memberPurchaseReturn',   '会员服务', 30),
('memberLevel',            '会员服务', 40),
('campaignPolicy',         '会员服务', 50),
('pointsGoods',            '会员服务', 60),
('pointsRule',             '会员服务', 70),
('pointsRecord',           '会员服务', 80),
('pointsExchange',         '会员服务', 90),
('seckill',                '会员服务', 100),
('seckillRecord',          '会员服务', 110),
('configSync',             '会员服务', 120),

('dashboard',              '会员运营', 10),
('growth',                 '会员运营', 20),
('actions',                '会员运营', 30),
('points',                 '会员运营', 40),

('expense',                '财务管理', 10),
('advance',                '财务管理', 20),
('product',                '财务管理', 30),
('supplier',               '财务管理', 40),
('purchase',               '财务管理', 50),
('sale',                   '财务管理', 60),
('investorPayment',        '财务管理', 70),
('investor',               '财务管理', 80),
('investRecord',           '财务管理', 90),
('deptProfitConfig',       '财务管理', 100),
('accountingPeriod',       '财务管理', 110),
('profitShare',            '财务管理', 120),
('costAccounting',         '财务管理', 130),
('stockCost',              '财务管理', 140),
('stockAdjustment',        '财务管理', 160),
('verificationRecord',     '财务管理', 180),

('userManage',             '系统管理', 10),
('deptManage',             '系统管理', 20),

('wfTodo',                 '移动办公', 10),
('wfDone',                 '移动办公', 20),
('wfNotify',               '移动办公', 30);
