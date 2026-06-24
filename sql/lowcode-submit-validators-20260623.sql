-- =============================================
-- lc_biz_object 新增提交前校验器配置列
-- 日期：2026-06-23
-- 说明：存储 JSON 格式的提交前校验规则（如空间查重 GEO_DEDUP）
-- =============================================

ALTER TABLE `lc_biz_object`
  ADD COLUMN `submit_validators` TEXT DEFAULT NULL COMMENT '提交前校验器配置(JSON数组)' AFTER `menu_parent_path`;
