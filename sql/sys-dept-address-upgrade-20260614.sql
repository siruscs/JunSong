ALTER TABLE `sys_dept`
  ADD COLUMN `province_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '省份编码' AFTER `email`,
  ADD COLUMN `province_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '省份名称' AFTER `province_code`,
  ADD COLUMN `city_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '城市编码' AFTER `province_name`,
  ADD COLUMN `city_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '城市名称' AFTER `city_code`,
  ADD COLUMN `district_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '区县编码' AFTER `city_name`,
  ADD COLUMN `district_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '区县名称' AFTER `district_code`,
  ADD COLUMN `street_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '街道编码' AFTER `district_name`,
  ADD COLUMN `street_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '街道名称' AFTER `street_code`,
  ADD COLUMN `detail_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详细地址' AFTER `street_name`;
