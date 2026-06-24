-- ==========================================
-- 多店面支持 - 数据库迁移脚本
-- ==========================================

-- 1. 给财务模块表添加 dept_id 字段
-- ==========================================

-- 商品表添加部门ID
ALTER TABLE fin_product ADD COLUMN dept_id BIGINT COMMENT '部门ID' AFTER product_id;

-- 供应商表添加部门ID
ALTER TABLE fin_supplier ADD COLUMN dept_id BIGINT COMMENT '部门ID' AFTER supplier_id;

-- 进货单表添加部门ID
ALTER TABLE fin_purchase ADD COLUMN dept_id BIGINT COMMENT '部门ID' AFTER purchase_id;

-- 进货单明细表添加部门ID
ALTER TABLE fin_purchase_detail ADD COLUMN dept_id BIGINT COMMENT '部门ID' AFTER detail_id;

-- ==========================================
-- 2. 创建员工-店面关联表
-- ==========================================

-- 员工-店面关联表
CREATE TABLE IF NOT EXISTS sys_user_dept (
    user_dept_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    dept_id BIGINT NOT NULL COMMENT '部门/店面ID',
    status CHAR(1) DEFAULT '0' COMMENT '状态（0在职 1离职',
    hire_date DATETIME COMMENT '入职日期',
    leave_date DATETIME COMMENT '离职日期',
    create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (user_dept_id),
    UNIQUE KEY uk_user_dept (user_id, dept_id),
    KEY idx_dept_id (dept_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-部门关联表';
