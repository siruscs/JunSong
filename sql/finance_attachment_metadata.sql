-- ============================================================
-- 财务附件一期：为费用和借支增加附件元数据列
-- attachments 字段存储 JSON 数组，每项包含 fileName/fileUrl/fileSize
-- 示例: [{"fileName":"发票.jpg","fileUrl":"/files/2026/06/29/xxx.jpg","fileSize":102400}]
-- ============================================================

ALTER TABLE `fin_expense` ADD COLUMN `attachments` TEXT DEFAULT NULL COMMENT '附件元数据（JSON 数组）';

ALTER TABLE `fin_advance` ADD COLUMN `attachments` TEXT DEFAULT NULL COMMENT '附件元数据（JSON 数组）';
