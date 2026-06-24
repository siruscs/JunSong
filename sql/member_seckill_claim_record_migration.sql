-- 秒杀记录分批领取升级脚本
-- 适用于已有数据库：新增领取明细表、扩展秒杀记录状态、补领取权限。

CREATE TABLE IF NOT EXISTS `mem_seckill_claim_record` (
  `claim_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '领取ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `record_id` bigint(20) NOT NULL COMMENT '秒杀记录ID',
  `seckill_id` bigint(20) NOT NULL COMMENT '秒杀活动ID',
  `member_id` bigint(20) DEFAULT NULL COMMENT '会员ID',
  `member_no` varchar(64) DEFAULT NULL COMMENT '会员编号',
  `member_name` varchar(64) DEFAULT NULL COMMENT '会员姓名',
  `claim_shares` int(11) DEFAULT 1 COMMENT '本次领取份额',
  `claim_time` datetime DEFAULT NULL COMMENT '领取时间',
  `claim_by` varchar(64) DEFAULT NULL COMMENT '领取人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`claim_id`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_seckill_id` (`seckill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='秒杀领取明细表';

ALTER TABLE `mem_seckill_record`
  MODIFY COLUMN `status` char(1) DEFAULT '0' COMMENT '状态(0待领取/1已领取/2已取消/3部分领取)';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '秒杀记录领取', m.menu_id, 6, '', '', 1, 0, 'F', '0', '0', 'member:seckillRecord:receive', '#', 'admin', NOW(), '', NULL, ''
FROM `sys_menu` m
WHERE m.perms = 'member:seckillRecord:list'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu` p WHERE p.perms = 'member:seckillRecord:receive'
  )
LIMIT 1;
