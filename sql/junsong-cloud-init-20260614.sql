-- MySQL dump 10.13  Distrib 9.2.0, for Linux (x86_64)
--
-- Host: localhost    Database: junsong-cloud
-- ------------------------------------------------------
-- Server version	9.2.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `junsong-cloud`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `junsong-cloud` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `junsong-cloud`;

--
-- Table structure for table `fin_accounting_period`
--

DROP TABLE IF EXISTS `fin_accounting_period`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_accounting_period` (
  `period_id` bigint NOT NULL AUTO_INCREMENT COMMENT '周期ID',
  `dept_id` bigint NOT NULL COMMENT '机构ID',
  `period_no` varchar(64) NOT NULL COMMENT '周期编号',
  `start_time` datetime NOT NULL COMMENT '周期开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '周期结束时间',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0进行中 1已回本待结转 2已结转）',
  `break_even_time` datetime DEFAULT NULL COMMENT '回本时间',
  `carry_forward_time` datetime DEFAULT NULL COMMENT '结转时间',
  `total_verified_expense` decimal(18,2) DEFAULT '0.00' COMMENT '已核销费用',
  `total_purchase` decimal(18,2) DEFAULT '0.00' COMMENT '进货款',
  `total_sale_payment` decimal(18,2) DEFAULT '0.00' COMMENT '销售缴款总额',
  `total_sale_amount` decimal(24,6) DEFAULT '0.000000' COMMENT '总销售金额',
  `total_unverified_advance` decimal(18,2) DEFAULT '0.00' COMMENT '借支未核销',
  `net_profit` decimal(18,2) DEFAULT '0.00' COMMENT '净利',
  `manager_profit_rate` decimal(10,4) DEFAULT '0.0000' COMMENT '店长分润比例',
  `manager_profit_amount` decimal(18,2) DEFAULT '0.00' COMMENT '店长分润金额',
  `investor_profit_amount` decimal(18,2) DEFAULT '0.00' COMMENT '投资人分润总额',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`period_id`),
  UNIQUE KEY `uk_dept_period_no` (`dept_id`,`period_no`),
  KEY `idx_dept_status` (`dept_id`,`status`),
  KEY `idx_dept_time` (`dept_id`,`start_time`,`end_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='财务核算周期表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_accounting_period`
--

LOCK TABLES `fin_accounting_period` WRITE;
/*!40000 ALTER TABLE `fin_accounting_period` DISABLE KEYS */;
INSERT INTO `fin_accounting_period` VALUES (1,103,'AP20260614023316103','2026-06-14 10:33:17',NULL,'0',NULL,NULL,0.00,0.00,0.00,0.000000,0.00,0.00,NULL,0.00,0.00,'0',NULL,'2026-06-14 10:33:16','','2026-06-14 10:43:03',NULL);
/*!40000 ALTER TABLE `fin_accounting_period` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_advance`
--

DROP TABLE IF EXISTS `fin_advance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_advance` (
  `advance_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'å€Ÿæ”¯è®°å½•ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `advance_no` varchar(64) NOT NULL COMMENT 'å€Ÿæ”¯å•å·',
  `advance_date` datetime NOT NULL COMMENT 'å€Ÿæ”¯æ—¥æœŸ',
  `advance_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT 'å€Ÿæ”¯é‡‘é¢',
  `borrower` varchar(64) DEFAULT NULL COMMENT 'å€Ÿæ¬¾äºº',
  `purpose` varchar(256) DEFAULT NULL COMMENT 'å€Ÿæ”¯ç”¨é€”',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€(0æœªæ ¸é”€ 1å·²æ ¸é”€)',
  `verify_by` varchar(64) DEFAULT '' COMMENT 'æ ¸é”€äºº',
  `verify_time` datetime DEFAULT NULL COMMENT 'æ ¸é”€æ—¶é—´',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—(0ä»£è¡¨å­˜åœ¨ 2ä»£è¡¨åˆ é™¤)',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `period_id` bigint DEFAULT NULL COMMENT '周期ID',
  PRIMARY KEY (`advance_id`),
  UNIQUE KEY `uk_advance_no` (`advance_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_advance_date` (`advance_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='å€Ÿæ”¯è®°å½•è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_advance`
--

LOCK TABLES `fin_advance` WRITE;
/*!40000 ALTER TABLE `fin_advance` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_advance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_cost_accounting`
--

DROP TABLE IF EXISTS `fin_cost_accounting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_cost_accounting` (
  `accounting_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æˆæœ¬æ ¸ç®—ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `accounting_no` varchar(64) NOT NULL COMMENT 'æ ¸ç®—å•å·',
  `start_date` datetime NOT NULL COMMENT 'æ ¸ç®—å¼€å§‹æ—¥æœŸ',
  `end_date` datetime NOT NULL COMMENT 'æ ¸ç®—ç»“æŸæ—¥æœŸ',
  `total_expense` decimal(12,2) DEFAULT '0.00' COMMENT 'æ€»èŠ±é”€è´¹ç”¨',
  `total_purchase` decimal(12,2) DEFAULT '0.00' COMMENT 'æ€»è¿›è´§é‡‘é¢',
  `total_sale` decimal(12,2) DEFAULT '0.00' COMMENT 'æ€»é”€å”®é‡‘é¢',
  `total_payment` decimal(12,2) DEFAULT '0.00' COMMENT 'æ€»ç¼´æ¬¾é‡‘é¢',
  `total_invest` decimal(18,2) DEFAULT '0.00' COMMENT 'æ€»æŠ•èµ„é‡‘é¢',
  `current_advance` decimal(18,2) DEFAULT '0.00' COMMENT 'å½“å‰å€Ÿæ”¯é‡‘é¢',
  `return_situation` decimal(12,2) DEFAULT '0.00' COMMENT 'å›žæœ¬æƒ…å†µ(æ€»ç¼´æ¬¾-æ€»èŠ±é”€-æ€»è¿›è´§)',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—(0ä»£è¡¨å­˜åœ¨ 2ä»£è¡¨åˆ é™¤)',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`accounting_id`),
  UNIQUE KEY `uk_accounting_no` (`accounting_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_start_date` (`start_date`),
  KEY `idx_end_date` (`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æˆæœ¬æ ¸ç®—è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_cost_accounting`
--

LOCK TABLES `fin_cost_accounting` WRITE;
/*!40000 ALTER TABLE `fin_cost_accounting` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_cost_accounting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_dept_profit_config`
--

DROP TABLE IF EXISTS `fin_dept_profit_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_dept_profit_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `dept_id` bigint NOT NULL COMMENT '机构ID',
  `manager_profit_rate` decimal(10,4) NOT NULL DEFAULT '0.0000' COMMENT '店长分润比例',
  `auto_create_investor_payment` char(1) NOT NULL DEFAULT '1' COMMENT '是否自动生成投资人返款（0否 1是）',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='店面分润配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_dept_profit_config`
--

LOCK TABLES `fin_dept_profit_config` WRITE;
/*!40000 ALTER TABLE `fin_dept_profit_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_dept_profit_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_expense`
--

DROP TABLE IF EXISTS `fin_expense`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_expense` (
  `expense_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'è´¹ç”¨è®°å½•ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `expense_no` varchar(64) NOT NULL COMMENT 'è´¹ç”¨å•å·',
  `expense_date` datetime NOT NULL COMMENT 'è´¹ç”¨æ—¥æœŸ',
  `expense_type` varchar(32) NOT NULL COMMENT 'è´¹ç”¨ç±»åˆ«(é¢„æ”¯ å¼€æ”¯ æ”¶å…¥)',
  `expense_content` text COMMENT 'èŠ±é”€å†…å®¹',
  `payment_method` varchar(32) NOT NULL COMMENT 'ä»˜æ¬¾æ–¹å¼(ç›´æŽ¥ä»˜æ¬¾ é¢„æ”¯èµ„é‡‘ è‡ªè¡Œåž«ä»˜ æ”¶å…¥)',
  `expense_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT 'è´¹ç”¨é‡‘é¢',
  `advance_id` bigint DEFAULT NULL COMMENT 'å…³è”å€Ÿæ”¯ID',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€(0æœªæ ¸é”€ 1å·²æ ¸é”€)',
  `verify_by` varchar(64) DEFAULT '' COMMENT 'æ ¸é”€äºº',
  `verify_time` datetime DEFAULT NULL COMMENT 'æ ¸é”€æ—¶é—´',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—(0ä»£è¡¨å­˜åœ¨ 2ä»£è¡¨åˆ é™¤)',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `period_id` bigint DEFAULT NULL COMMENT '周期ID',
  PRIMARY KEY (`expense_id`),
  UNIQUE KEY `uk_expense_no` (`expense_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_expense_date` (`expense_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='è´¹ç”¨è®°å½•è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_expense`
--

LOCK TABLES `fin_expense` WRITE;
/*!40000 ALTER TABLE `fin_expense` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_expense` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_invest_record`
--

DROP TABLE IF EXISTS `fin_invest_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_invest_record` (
  `invest_id` bigint NOT NULL AUTO_INCREMENT COMMENT '投资记录ID',
  `dept_id` bigint NOT NULL COMMENT '机构ID',
  `period_id` bigint DEFAULT NULL COMMENT '周期ID',
  `investor_id` bigint NOT NULL COMMENT '投资人ID',
  `investor_name` varchar(64) NOT NULL COMMENT '投资人姓名',
  `invest_amount` decimal(18,2) NOT NULL COMMENT '投资金额',
  `invest_time` datetime NOT NULL COMMENT '投资时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`invest_id`),
  KEY `idx_dept_period` (`dept_id`,`period_id`),
  KEY `idx_investor_id` (`investor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投资来源记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_invest_record`
--

LOCK TABLES `fin_invest_record` WRITE;
/*!40000 ALTER TABLE `fin_invest_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_invest_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_investor`
--

DROP TABLE IF EXISTS `fin_investor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_investor` (
  `investor_id` bigint NOT NULL AUTO_INCREMENT COMMENT '投资人ID',
  `dept_id` bigint NOT NULL COMMENT '机构ID',
  `investor_name` varchar(64) NOT NULL COMMENT '投资人姓名',
  `phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`investor_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投资人表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_investor`
--

LOCK TABLES `fin_investor` WRITE;
/*!40000 ALTER TABLE `fin_investor` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_investor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_investor_payment`
--

DROP TABLE IF EXISTS `fin_investor_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_investor_payment` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'è¿”æ¬¾è®°å½•ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `payment_no` varchar(64) NOT NULL COMMENT 'è¿”æ¬¾å•å·',
  `payment_date` datetime NOT NULL COMMENT 'æ—¥æœŸ',
  `payment_type` varchar(32) NOT NULL COMMENT 'ç±»åž‹(æ‰“æ¬¾ è¿”æ¬¾)',
  `investor_name` varchar(64) DEFAULT NULL COMMENT 'æŠ•èµ„äººå§“å',
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT 'é‡‘é¢',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—(0ä»£è¡¨å­˜åœ¨ 2ä»£è¡¨åˆ é™¤)',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `period_id` bigint DEFAULT NULL COMMENT '周期ID',
  `share_id` bigint DEFAULT NULL COMMENT '分润单ID',
  `share_detail_id` bigint DEFAULT NULL COMMENT '分润明细ID',
  `investor_id` bigint DEFAULT NULL COMMENT '投资人ID',
  `source_type` char(1) DEFAULT '0' COMMENT '来源类型（0手工返款 1结转分润自动返款）',
  `payment_status` char(1) DEFAULT '1' COMMENT '返款状态（0待返款 1已返款）',
  `invest_ratio` decimal(10,4) DEFAULT '0.0000' COMMENT '本次投资占比',
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_payment_date` (`payment_date`),
  KEY `idx_payment_type` (`payment_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æŠ•èµ„äººè¿”æ¬¾è®°å½•è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_investor_payment`
--

LOCK TABLES `fin_investor_payment` WRITE;
/*!40000 ALTER TABLE `fin_investor_payment` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_investor_payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_product`
--

DROP TABLE IF EXISTS `fin_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_product` (
  `product_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'å•†å“ID',
  `product_code` varchar(64) DEFAULT NULL COMMENT 'å•†å“ç¼–ç ',
  `product_name` varchar(128) NOT NULL COMMENT 'å•†å“åç§°',
  `category_id` bigint DEFAULT NULL COMMENT 'å•†å“åˆ†ç±»ID',
  `unit` varchar(32) DEFAULT NULL COMMENT 'è®¡é‡å•ä½',
  `purchase_price` decimal(12,2) DEFAULT '0.00' COMMENT 'è¿›è´§ä»·æ ¼',
  `sale_price` decimal(12,2) DEFAULT '0.00' COMMENT 'é”€å”®ä»·æ ¼',
  `stock_num` int DEFAULT '0' COMMENT 'åº“å­˜æ•°é‡',
  `min_stock` int DEFAULT '0' COMMENT 'æœ€ä½Žåº“å­˜é¢„è­¦',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€ï¼ˆ0æ­£å¸¸ 1åœç”¨ï¼‰',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—ï¼ˆ0ä»£è¡¨å­˜åœ¨ 2ä»£è¡¨åˆ é™¤ï¼‰',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`product_id`),
  KEY `idx_product_code` (`product_code`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='å•†å“è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_product`
--

LOCK TABLES `fin_product` WRITE;
/*!40000 ALTER TABLE `fin_product` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_product_category`
--

DROP TABLE IF EXISTS `fin_product_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_product_category` (
  `category_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'åˆ†ç±»ID',
  `category_name` varchar(128) NOT NULL COMMENT 'åˆ†ç±»åç§°',
  `parent_id` bigint DEFAULT '0' COMMENT 'çˆ¶åˆ†ç±»ID',
  `order_num` int DEFAULT '0' COMMENT 'æ˜¾ç¤ºé¡ºåº',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€ï¼ˆ0æ­£å¸¸ 1åœç”¨ï¼‰',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—ï¼ˆ0ä»£è¡¨å­˜åœ¨ 2ä»£è¡¨åˆ é™¤ï¼‰',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`category_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='å•†å“åˆ†ç±»è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_product_category`
--

LOCK TABLES `fin_product_category` WRITE;
/*!40000 ALTER TABLE `fin_product_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_product_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_profit_share_detail`
--

DROP TABLE IF EXISTS `fin_profit_share_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_profit_share_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分润明细ID',
  `share_id` bigint NOT NULL COMMENT '分润ID',
  `dept_id` bigint NOT NULL COMMENT '机构ID',
  `period_id` bigint NOT NULL COMMENT '周期ID',
  `receiver_type` char(1) NOT NULL COMMENT '接收方类型（1店长 2投资人）',
  `receiver_id` bigint DEFAULT NULL COMMENT '接收方ID',
  `receiver_name` varchar(64) NOT NULL COMMENT '接收方名称',
  `invest_amount` decimal(18,2) DEFAULT '0.00' COMMENT '投资金额',
  `invest_ratio` decimal(10,4) DEFAULT '0.0000' COMMENT '投资占比',
  `share_amount` decimal(18,2) NOT NULL COMMENT '分润金额',
  `payment_id` bigint DEFAULT NULL COMMENT '自动生成的投资返款ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_share_id` (`share_id`),
  KEY `idx_dept_period` (`dept_id`,`period_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分润记录明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_profit_share_detail`
--

LOCK TABLES `fin_profit_share_detail` WRITE;
/*!40000 ALTER TABLE `fin_profit_share_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_profit_share_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_profit_share_record`
--

DROP TABLE IF EXISTS `fin_profit_share_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_profit_share_record` (
  `share_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分润ID',
  `dept_id` bigint NOT NULL COMMENT '机构ID',
  `period_id` bigint NOT NULL COMMENT '周期ID',
  `share_no` varchar(64) NOT NULL COMMENT '分润单号',
  `net_profit` decimal(18,2) NOT NULL COMMENT '净利',
  `manager_profit_rate` decimal(10,4) NOT NULL COMMENT '店长分润比例',
  `manager_profit_amount` decimal(18,2) NOT NULL COMMENT '店长分润金额',
  `investor_profit_amount` decimal(18,2) NOT NULL COMMENT '投资人分润金额',
  `status` char(1) NOT NULL DEFAULT '1' COMMENT '状态（0草稿 1已完成）',
  `share_time` datetime NOT NULL COMMENT '分润时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`share_id`),
  UNIQUE KEY `uk_share_no` (`share_no`),
  KEY `idx_dept_period` (`dept_id`,`period_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分润记录主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_profit_share_record`
--

LOCK TABLES `fin_profit_share_record` WRITE;
/*!40000 ALTER TABLE `fin_profit_share_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_profit_share_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_purchase`
--

DROP TABLE IF EXISTS `fin_purchase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_purchase` (
  `purchase_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'è¿›è´§å•ID',
  `purchase_no` varchar(64) NOT NULL COMMENT 'è¿›è´§å•å·',
  `supplier_id` bigint NOT NULL COMMENT 'ä¾›åº”å•†ID',
  `purchase_date` datetime NOT NULL COMMENT 'è¿›è´§æ—¥æœŸ',
  `total_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'æ€»é‡‘é¢',
  `paid_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'å·²ä»˜é‡‘é¢',
  `payment_method` varchar(32) DEFAULT NULL COMMENT 'ä»˜æ¬¾æ–¹å¼',
  `total_quantity` int DEFAULT NULL COMMENT 'æ€»æ•°é‡ï¼ˆå«èµ å“ï¼‰',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€ï¼ˆ0è‰ç¨¿ 1å·²ç¡®è®¤ 2å·²å®Œæˆï¼‰',
  `receiver_name` varchar(64) DEFAULT NULL COMMENT 'æ”¶è´§äººå§“å',
  `receiver_phone` varchar(32) DEFAULT NULL COMMENT 'æ”¶è´§äººç”µè¯',
  `receiver_address` varchar(256) DEFAULT NULL COMMENT 'æ”¶è´§äººåœ°å€',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—ï¼ˆ0ä»£è¡¨å­˜åœ¨ 2ä»£è¡¨åˆ é™¤ï¼‰',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `period_id` bigint DEFAULT NULL COMMENT '周期ID',
  PRIMARY KEY (`purchase_id`),
  UNIQUE KEY `uk_purchase_no` (`purchase_no`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_purchase_date` (`purchase_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='è¿›è´§å•è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_purchase`
--

LOCK TABLES `fin_purchase` WRITE;
/*!40000 ALTER TABLE `fin_purchase` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_purchase` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_purchase_detail`
--

DROP TABLE IF EXISTS `fin_purchase_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_purchase_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ˜Žç»†ID',
  `purchase_id` bigint NOT NULL COMMENT 'è¿›è´§å•ID',
  `product_id` bigint NOT NULL COMMENT 'å•†å“ID',
  `product_name` varchar(128) DEFAULT NULL COMMENT 'å•†å“åç§°',
  `unit` varchar(32) DEFAULT NULL COMMENT 'è®¡é‡å•ä½',
  `quantity` int NOT NULL COMMENT 'æ•°é‡',
  `price` decimal(12,2) NOT NULL COMMENT 'å•ä»·',
  `amount` decimal(12,2) NOT NULL COMMENT 'é‡‘é¢',
  `is_gift` varchar(10) DEFAULT '0' COMMENT 'æ˜¯å¦èµ å“ï¼ˆ0å¦ 1æ˜¯ï¼‰',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—ï¼ˆ0ä»£è¡¨å­˜åœ¨ 2ä»£è¡¨åˆ é™¤ï¼‰',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`detail_id`),
  KEY `idx_purchase_id` (`purchase_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='è¿›è´§å•æ˜Žç»†è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_purchase_detail`
--

LOCK TABLES `fin_purchase_detail` WRITE;
/*!40000 ALTER TABLE `fin_purchase_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_purchase_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_sale_payment`
--

DROP TABLE IF EXISTS `fin_sale_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_sale_payment` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ç¼´æ¬¾è®°å½•ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `sale_id` bigint NOT NULL COMMENT 'é”€å”®è®°å½•ID',
  `payment_no` varchar(64) NOT NULL COMMENT 'ç¼´æ¬¾å•å·',
  `payment_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT 'ç¼´æ¬¾é‡‘é¢',
  `payment_method` varchar(32) DEFAULT NULL COMMENT 'ä»˜æ¬¾æ–¹å¼',
  `payment_date` datetime NOT NULL COMMENT 'ç¼´æ¬¾æ—¥æœŸ',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—(0ä»£è¡¨å­˜åœ¨ 2ä»£è¡¨åˆ é™¤)',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `period_id` bigint DEFAULT NULL COMMENT '周期ID',
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_sale_id` (`sale_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_payment_date` (`payment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ç¼´æ¬¾è®°å½•è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_sale_payment`
--

LOCK TABLES `fin_sale_payment` WRITE;
/*!40000 ALTER TABLE `fin_sale_payment` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_sale_payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_sale_record`
--

DROP TABLE IF EXISTS `fin_sale_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_sale_record` (
  `sale_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'é”€å”®è®°å½•ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `sale_no` varchar(64) NOT NULL COMMENT 'é”€å”®å•å·',
  `product_id` bigint NOT NULL COMMENT 'å•†å“ID',
  `product_name` varchar(128) DEFAULT NULL COMMENT 'å•†å“åç§°',
  `sale_quantity` int NOT NULL DEFAULT '0' COMMENT 'é”€å”®æ•°é‡',
  `gift_quantity` int NOT NULL DEFAULT '0' COMMENT 'èµ å“æ•°é‡',
  `total_quantity` int NOT NULL DEFAULT '0' COMMENT 'æ€»æ•°é‡(é”€å”®+èµ å“)',
  `sale_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT 'é”€å”®é‡‘é¢',
  `unit_price` decimal(12,2) DEFAULT '0.00' COMMENT 'å•ä»·(é”€å”®é‡‘é¢/é”€å”®æ•°é‡)',
  `paid_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'å·²ç¼´é‡‘é¢',
  `sale_date` datetime NOT NULL COMMENT 'é”€å”®æ—¥æœŸ',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€(0å¾…ç¼´æ¬¾ 1éƒ¨åˆ†ç¼´æ¬¾ 2å·²ç¼´æ¸…)',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—(0ä»£è¡¨å­˜åœ¨ 2ä»£è¡¨åˆ é™¤)',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `period_id` bigint DEFAULT NULL COMMENT '周期ID',
  PRIMARY KEY (`sale_id`),
  UNIQUE KEY `uk_sale_no` (`sale_no`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_sale_date` (`sale_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='é”€å”®è®°å½•è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_sale_record`
--

LOCK TABLES `fin_sale_record` WRITE;
/*!40000 ALTER TABLE `fin_sale_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_sale_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fin_supplier`
--

DROP TABLE IF EXISTS `fin_supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fin_supplier` (
  `supplier_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¾›åº”å•†ID',
  `supplier_code` varchar(64) DEFAULT NULL COMMENT 'ä¾›åº”å•†ç¼–ç ',
  `supplier_name` varchar(128) NOT NULL COMMENT 'ä¾›åº”å•†åç§°',
  `contact_person` varchar(64) DEFAULT NULL COMMENT 'è”ç³»äºº',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT 'è”ç³»ç”µè¯',
  `address` varchar(256) DEFAULT NULL COMMENT 'åœ°å€',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€ï¼ˆ0æ­£å¸¸ 1åœç”¨ï¼‰',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—ï¼ˆ0ä»£è¡¨å­˜åœ¨ 2ä»£è¡¨åˆ é™¤ï¼‰',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`supplier_id`),
  KEY `idx_supplier_code` (`supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ä¾›åº”å•†è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fin_supplier`
--

LOCK TABLES `fin_supplier` WRITE;
/*!40000 ALTER TABLE `fin_supplier` DISABLE KEYS */;
/*!40000 ALTER TABLE `fin_supplier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gen_table`
--

DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `tpl_web_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成功能作者',
  `form_col_num` int DEFAULT '1' COMMENT '表单布局（单列 双列 三列）',
  `gen_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码生成业务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table`
--

LOCK TABLES `gen_table` WRITE;
/*!40000 ALTER TABLE `gen_table` DISABLE KEYS */;
/*!40000 ALTER TABLE `gen_table` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gen_table_column`
--

DROP TABLE IF EXISTS `gen_table_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` bigint DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
  `sort` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码生成业务表字段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table_column`
--

LOCK TABLES `gen_table_column` WRITE;
/*!40000 ALTER TABLE `gen_table_column` DISABLE KEYS */;
/*!40000 ALTER TABLE `gen_table_column` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mem_member`
--

DROP TABLE IF EXISTS `mem_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mem_member` (
  `member_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¼šå‘˜ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `member_no` varchar(64) NOT NULL COMMENT 'ä¼šå‘˜ç¼–å·',
  `member_name` varchar(64) NOT NULL COMMENT 'ä¼šå‘˜å§“å',
  `phone` varchar(32) DEFAULT NULL COMMENT 'æ‰‹æœºå·ç ',
  `age` int DEFAULT NULL COMMENT 'å¹´é¾„',
  `address` varchar(256) DEFAULT NULL COMMENT 'ä½å€',
  `id_card` varchar(32) DEFAULT NULL COMMENT 'èº«ä»½è¯å·',
  `card_type` varchar(32) DEFAULT NULL COMMENT 'ä¼šå‘˜å¡ç±»åž‹(ä½“éªŒå¡/æ­£å¼å¡/ä¸€æ˜Ÿä¼šå‘˜/äºŒæ˜Ÿä¼šå‘˜/ä¸‰æ˜Ÿä¼šå‘˜/å››æ˜Ÿä¼šå‘˜/äº”æ˜Ÿä¼šå‘˜)',
  `card_id` bigint DEFAULT NULL COMMENT 'ä¼šå‘˜å¡ID',
  `join_date` datetime DEFAULT NULL COMMENT 'å…¥ä¼šæ—¥æœŸ',
  `expire_date` datetime DEFAULT NULL COMMENT 'æœ‰æ•ˆæœŸè‡³',
  `total_points` decimal(12,2) DEFAULT '0.00' COMMENT 'æ€»ç§¯åˆ†',
  `available_points` decimal(12,2) DEFAULT '0.00' COMMENT 'å¯ç”¨ç§¯åˆ†',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€(0æ­£å¸¸/1æ— æ•ˆ/2å·²é€€å¡)',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—(0å­˜åœ¨/2åˆ é™¤)',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_member_no` (`member_no`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 COMMENT='ä¼šå‘˜ä¿¡æ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mem_member`
--

LOCK TABLES `mem_member` WRITE;
/*!40000 ALTER TABLE `mem_member` DISABLE KEYS */;
INSERT INTO `mem_member` VALUES (1,NULL,'RY0001','王桂英','18518181136',67,NULL,NULL,'1',NULL,'2026-06-02 08:00:00','2027-10-01 08:00:00',0.00,0.00,'0',NULL,'0','admin','2026-06-02 12:24:12','',NULL),(2,NULL,'R#0001','FDSA ','13800009999',NULL,NULL,NULL,'experience',NULL,'2026-07-01 08:00:00',NULL,0.00,0.00,'0',NULL,'0','admin','2026-06-02 15:24:07','',NULL);
/*!40000 ALTER TABLE `mem_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mem_member_card_type`
--

DROP TABLE IF EXISTS `mem_member_card_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mem_member_card_type` (
  `type_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ç±»åž‹ID',
  `type_name` varchar(64) NOT NULL COMMENT 'ç±»åž‹åç§°',
  `type_code` varchar(32) NOT NULL COMMENT 'ç±»åž‹ä»£ç ',
  `card_fee` decimal(10,2) DEFAULT '9.90' COMMENT 'åŠžå¡è´¹ç”¨',
  `discount_rate` decimal(5,2) DEFAULT '1.00' COMMENT 'æŠ˜æ‰£çŽ‡',
  `points_rate` decimal(5,2) DEFAULT '1.00' COMMENT 'ç§¯åˆ†å€çŽ‡',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€(0æ­£å¸¸/1ç¦ç”¨)',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`type_id`),
  UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb3 COMMENT='ä¼šå‘˜å¡ç±»åž‹è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mem_member_card_type`
--

LOCK TABLES `mem_member_card_type` WRITE;
/*!40000 ALTER TABLE `mem_member_card_type` DISABLE KEYS */;
INSERT INTO `mem_member_card_type` VALUES (1,'ä½“éªŒå¡','experience',9.90,1.00,1.00,'0','0','admin','2026-06-01 16:57:13','',NULL),(2,'æ­£å¼ä¼šå‘˜å¡','formal',9.90,0.95,1.00,'0','0','admin','2026-06-01 16:57:13','',NULL),(3,'ä¸€æ˜Ÿä¼šå‘˜','star1',9.90,0.90,1.20,'0','0','admin','2026-06-01 16:57:13','',NULL),(4,'äºŒæ˜Ÿä¼šå‘˜','star2',9.90,0.85,1.40,'0','0','admin','2026-06-01 16:57:13','',NULL),(5,'ä¸‰æ˜Ÿä¼šå‘˜','star3',9.90,0.80,1.60,'0','0','admin','2026-06-01 16:57:13','',NULL),(6,'å››æ˜Ÿä¼šå‘˜','star4',9.90,0.75,1.80,'0','0','admin','2026-06-01 16:57:13','',NULL),(7,'äº”æ˜Ÿä¼šå‘˜','star5',9.90,0.70,2.00,'0','0','admin','2026-06-01 16:57:13','',NULL);
/*!40000 ALTER TABLE `mem_member_card_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mem_mp_role_module`
--

DROP TABLE IF EXISTS `mem_mp_role_module`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mem_mp_role_module` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL COMMENT 'è§’è‰²ID',
  `module_key` varchar(50) NOT NULL COMMENT 'æ¨¡å—æ ‡è¯†',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨IDï¼ŒNULLè¡¨ç¤ºé€šç”¨',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_module_dept` (`role_id`,`module_key`,`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=156 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='å°ç¨‹åºè§’è‰²æ¨¡å—æƒé™';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mem_mp_role_module`
--

LOCK TABLES `mem_mp_role_module` WRITE;
/*!40000 ALTER TABLE `mem_mp_role_module` DISABLE KEYS */;
INSERT INTO `mem_mp_role_module` VALUES (116,1,'accountingPeriod',0),(117,1,'advance',0),(118,1,'costAccounting',0),(119,1,'deptProfitConfig',0),(120,1,'expense',0),(121,1,'investor',0),(122,1,'investorPayment',0),(123,1,'investRecord',0),(124,1,'member',0),(125,1,'pointsExchange',0),(126,1,'pointsGoods',0),(127,1,'pointsRecord',0),(128,1,'product',0),(129,1,'profitShare',0),(130,1,'purchase',0),(131,1,'sale',0),(132,1,'seckill',0),(133,1,'seckillRecord',0),(134,1,'supplier',0),(135,1,'userManage',0),(136,100,'advance',0),(137,100,'expense',0),(138,100,'member',0),(139,100,'pointsExchange',0),(140,100,'pointsGoods',0),(141,100,'pointsRecord',0),(142,100,'seckill',0),(143,100,'seckillRecord',0),(144,100,'supplier',0),(145,101,'advance',0),(146,101,'costAccounting',0),(147,101,'expense',0),(148,101,'member',0),(149,101,'pointsExchange',0),(150,101,'pointsGoods',0),(151,101,'pointsRecord',0),(152,101,'product',0),(153,101,'sale',0),(154,101,'seckill',0),(155,101,'seckillRecord',0);
/*!40000 ALTER TABLE `mem_mp_role_module` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mem_points_exchange`
--

DROP TABLE IF EXISTS `mem_points_exchange`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mem_points_exchange` (
  `exchange_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'å…‘æ¢ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `exchange_no` varchar(64) NOT NULL COMMENT 'å…‘æ¢å•å·',
  `member_id` bigint NOT NULL COMMENT 'ä¼šå‘˜ID',
  `member_no` varchar(64) DEFAULT NULL COMMENT 'ä¼šå‘˜ç¼–å·',
  `member_name` varchar(64) DEFAULT NULL COMMENT 'ä¼šå‘˜å§“å',
  `goods_id` bigint NOT NULL COMMENT 'ç‰©å“ID',
  `goods_name` varchar(128) DEFAULT NULL COMMENT 'ç‰©å“åç§°',
  `exchange_date` datetime DEFAULT NULL COMMENT 'å…‘æ¢æ—¥æœŸ',
  `quantity` int DEFAULT '1' COMMENT 'å…‘æ¢æ•°é‡',
  `points_deducted` decimal(12,2) NOT NULL COMMENT 'æ‰£å‡ç§¯åˆ†',
  `actual_points` decimal(12,2) DEFAULT NULL COMMENT 'å®žé™…æ‰£å‡ç§¯åˆ†',
  `payment_method` varchar(32) DEFAULT NULL COMMENT 'è¡¥å·®ä»·æ–¹å¼(çŽ°é‡‘/å¾®ä¿¡/æ”¯ä»˜å®)',
  `extra_amount` decimal(10,2) DEFAULT '0.00' COMMENT 'è¡¥å·®ä»·é‡‘é¢',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€(0å¾…é¢†å–/1å·²é¢†å–/2å·²å–æ¶ˆ)',
  `claim_time` datetime DEFAULT NULL COMMENT 'é¢†å–æ—¶é—´',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`exchange_id`),
  UNIQUE KEY `uk_exchange_no` (`exchange_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='ç§¯åˆ†å…‘æ¢è®°å½•è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mem_points_exchange`
--

LOCK TABLES `mem_points_exchange` WRITE;
/*!40000 ALTER TABLE `mem_points_exchange` DISABLE KEYS */;
/*!40000 ALTER TABLE `mem_points_exchange` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mem_points_goods`
--

DROP TABLE IF EXISTS `mem_points_goods`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mem_points_goods` (
  `goods_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ç‰©å“ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `goods_name` varchar(128) NOT NULL COMMENT 'ç‰©å“åç§°',
  `goods_code` varchar(64) DEFAULT NULL COMMENT 'ç‰©å“ç¼–ç ',
  `goods_value` decimal(10,2) DEFAULT '0.00' COMMENT 'ç‰©å“ä»·å€¼(å…ƒ)',
  `points_price` decimal(12,2) NOT NULL COMMENT 'ç§¯åˆ†ä»·æ ¼',
  `stock` int DEFAULT '0' COMMENT 'åº“å­˜',
  `exchanged` int DEFAULT '0' COMMENT 'å·²å…‘æ¢æ•°é‡',
  `goods_image` varchar(256) DEFAULT NULL COMMENT 'ç‰©å“å›¾ç‰‡',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€(0ä¸Šæž¶/1ä¸‹æž¶)',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`goods_id`),
  UNIQUE KEY `uk_goods_code` (`goods_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='ç§¯åˆ†ç‰©å“è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mem_points_goods`
--

LOCK TABLES `mem_points_goods` WRITE;
/*!40000 ALTER TABLE `mem_points_goods` DISABLE KEYS */;
/*!40000 ALTER TABLE `mem_points_goods` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mem_points_record`
--

DROP TABLE IF EXISTS `mem_points_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mem_points_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'è®°å½•ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `member_id` bigint NOT NULL COMMENT 'ä¼šå‘˜ID',
  `member_no` varchar(64) DEFAULT NULL COMMENT 'ä¼šå‘˜ç¼–å·',
  `member_name` varchar(64) DEFAULT NULL COMMENT 'ä¼šå‘˜å§“å',
  `record_type` varchar(32) NOT NULL COMMENT 'ç±»åž‹(æ¶ˆè´¹å¾—ç§¯åˆ†/å…‘æ¢æ‰£ç§¯åˆ†/è¿‡æœŸæ¸…é›¶/æ‰‹åŠ¨è°ƒæ•´)',
  `consume_amount` decimal(10,2) DEFAULT '0.00' COMMENT 'æ¶ˆè´¹é‡‘é¢',
  `points` decimal(12,2) NOT NULL COMMENT 'ç§¯åˆ†å˜åŠ¨',
  `balance` decimal(12,2) DEFAULT '0.00' COMMENT 'å˜åŠ¨åŽä½™é¢',
  `rule_code` varchar(32) DEFAULT NULL COMMENT 'ä½¿ç”¨çš„è§„åˆ™ä»£ç ',
  `expire_date` datetime DEFAULT NULL COMMENT 'ç§¯åˆ†è¿‡æœŸæ—¥æœŸ',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='ç§¯åˆ†è®°å½•è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mem_points_record`
--

LOCK TABLES `mem_points_record` WRITE;
/*!40000 ALTER TABLE `mem_points_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `mem_points_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mem_points_rule`
--

DROP TABLE IF EXISTS `mem_points_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mem_points_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_name` varchar(64) NOT NULL COMMENT '规则名称',
  `rule_code` varchar(32) NOT NULL COMMENT '规则代码',
  `rule_type` varchar(32) NOT NULL COMMENT '计算方式(进一法/四舍五入/舍零取整)',
  `points_per_yuan` decimal(5,2) DEFAULT '1.00' COMMENT '每元积分',
  `validity_days` int DEFAULT '99' COMMENT '积分有效期(天)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0启用/1禁用)',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mem_points_rule`
--

LOCK TABLES `mem_points_rule` WRITE;
/*!40000 ALTER TABLE `mem_points_rule` DISABLE KEYS */;
INSERT INTO `mem_points_rule` VALUES (1,'进一法','ceil','进一法',1.00,99,'0','0','admin','2026-06-01 16:57:13','',NULL),(2,'四舍五入','round','四舍五入',1.00,99,'0','0','admin','2026-06-01 16:57:13','',NULL),(3,'舍零取整','floor','舍零取整',1.00,99,'0','0','admin','2026-06-01 16:57:13','',NULL);
/*!40000 ALTER TABLE `mem_points_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mem_seckill`
--

DROP TABLE IF EXISTS `mem_seckill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mem_seckill` (
  `seckill_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ç§’æ€ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `seckill_no` varchar(64) NOT NULL DEFAULT '' COMMENT '秒杀编号',
  `seckill_name` varchar(128) NOT NULL COMMENT 'ç§’æ€åç§°',
  `seckill_type` varchar(32) NOT NULL COMMENT 'ç±»åž‹(ç§’æ€/å›¢è´­)',
  `seckill_date` date DEFAULT NULL COMMENT 'ç§’æ€æ—¥æœŸ',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `seckill_time` varchar(32) DEFAULT NULL COMMENT 'ç§’æ€æ—¶é—´æ®µ',
  `seckill_amount` decimal(10,2) NOT NULL COMMENT 'ç§’æ€é‡‘é¢',
  `seckill_price` decimal(10,2) NOT NULL COMMENT 'ç§’æ€å•ä»·',
  `total_shares` int DEFAULT '0' COMMENT 'æ€»ä»½é¢',
  `remain_shares` int DEFAULT '0' COMMENT 'å‰©ä½™ä»½é¢',
  `policy` varchar(256) DEFAULT NULL COMMENT 'ç§’æ€æ”¿ç­–',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€(0è¿›è¡Œä¸­/1å·²ç»“æŸ/2å·²å–æ¶ˆ)',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`seckill_id`),
  UNIQUE KEY `uk_seckill_no` (`seckill_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='ç§’æ€/å›¢è´­æ´»åŠ¨è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mem_seckill`
--

LOCK TABLES `mem_seckill` WRITE;
/*!40000 ALTER TABLE `mem_seckill` DISABLE KEYS */;
/*!40000 ALTER TABLE `mem_seckill` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mem_seckill_claim_record`
--

DROP TABLE IF EXISTS `mem_seckill_claim_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mem_seckill_claim_record` (
  `claim_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'é¢†å–ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `record_id` bigint NOT NULL COMMENT 'ç§’æ€è®°å½•ID',
  `seckill_id` bigint NOT NULL COMMENT 'ç§’æ€æ´»åŠ¨ID',
  `member_id` bigint DEFAULT NULL COMMENT 'ä¼šå‘˜ID',
  `member_no` varchar(64) DEFAULT NULL COMMENT 'ä¼šå‘˜ç¼–å·',
  `member_name` varchar(64) DEFAULT NULL COMMENT 'ä¼šå‘˜å§“å',
  `claim_shares` int DEFAULT '1' COMMENT 'æœ¬æ¬¡é¢†å–ä»½é¢',
  `claim_time` datetime DEFAULT NULL COMMENT 'é¢†å–æ—¶é—´',
  `claim_by` varchar(64) DEFAULT NULL COMMENT 'é¢†å–äºº',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  PRIMARY KEY (`claim_id`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_seckill_id` (`seckill_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb3 COMMENT='ç§’æ€é¢†å–æ˜Žç»†è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mem_seckill_claim_record`
--

LOCK TABLES `mem_seckill_claim_record` WRITE;
/*!40000 ALTER TABLE `mem_seckill_claim_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `mem_seckill_claim_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mem_seckill_record`
--

DROP TABLE IF EXISTS `mem_seckill_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mem_seckill_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'è®°å½•ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `seckill_id` bigint NOT NULL COMMENT 'ç§’æ€ID',
  `member_id` bigint DEFAULT NULL COMMENT 'ä¼šå‘˜ID',
  `member_no` varchar(64) DEFAULT NULL COMMENT 'ä¼šå‘˜ç¼–å·',
  `member_name` varchar(64) DEFAULT NULL COMMENT 'ä¼šå‘˜å§“å',
  `seckill_date` date DEFAULT NULL COMMENT 'ç§’æ€æ—¥æœŸ',
  `payment_method` varchar(32) DEFAULT NULL COMMENT 'äº¤è´¹æ–¹å¼',
  `shares` int DEFAULT '1' COMMENT 'ç§’æ€ä»½é¢',
  `total_amount` decimal(10,2) DEFAULT '0.00' COMMENT 'æ€»é‡‘é¢',
  `status` char(1) DEFAULT '0' COMMENT 'çŠ¶æ€(0å¾…é¢†å–/1å·²é¢†å–/2å·²å–æ¶ˆ)',
  `claim_time` datetime DEFAULT NULL COMMENT 'é¢†å–æ—¶é—´',
  `claim_by` varchar(64) DEFAULT NULL COMMENT 'é¢†å–äºº',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `del_flag` char(1) DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—',
  `create_by` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='ç§’æ€è®°å½•è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mem_seckill_record`
--

LOCK TABLES `mem_seckill_record` WRITE;
/*!40000 ALTER TABLE `mem_seckill_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `mem_seckill_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_blob_triggers`
--

DROP TABLE IF EXISTS `qrtz_blob_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_blob_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `blob_data` blob COMMENT '存放持久化Trigger对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Blob类型的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_blob_triggers`
--

LOCK TABLES `qrtz_blob_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_blob_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_blob_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_calendars`
--

DROP TABLE IF EXISTS `qrtz_calendars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_calendars` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '日历名称',
  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日历信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_calendars`
--

LOCK TABLES `qrtz_calendars` WRITE;
/*!40000 ALTER TABLE `qrtz_calendars` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_calendars` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_cron_triggers`
--

DROP TABLE IF EXISTS `qrtz_cron_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_cron_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `cron_expression` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'cron表达式',
  `time_zone_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Cron类型的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_cron_triggers`
--

LOCK TABLES `qrtz_cron_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_cron_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_cron_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_fired_triggers`
--

DROP TABLE IF EXISTS `qrtz_fired_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_fired_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度名称',
  `entry_id` varchar(95) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度器实例id',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度器实例名',
  `fired_time` bigint NOT NULL COMMENT '触发的时间',
  `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
  `priority` int NOT NULL COMMENT '优先级',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务组名',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否并发',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否接受恢复执行',
  PRIMARY KEY (`sched_name`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='已触发的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_fired_triggers`
--

LOCK TABLES `qrtz_fired_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_fired_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_fired_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_job_details`
--

DROP TABLE IF EXISTS `qrtz_job_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_job_details` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度名称',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务组名',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '相关介绍',
  `job_class_name` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '执行任务类名称',
  `is_durable` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '是否持久化',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '是否并发',
  `is_update_data` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '是否更新数据',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '是否接受恢复执行',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务详细信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_job_details`
--

LOCK TABLES `qrtz_job_details` WRITE;
/*!40000 ALTER TABLE `qrtz_job_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_job_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_locks`
--

DROP TABLE IF EXISTS `qrtz_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_locks` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储的悲观锁信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_locks`
--

LOCK TABLES `qrtz_locks` WRITE;
/*!40000 ALTER TABLE `qrtz_locks` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_locks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_paused_trigger_grps`
--

DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_paused_trigger_grps` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='暂停的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_paused_trigger_grps`
--

LOCK TABLES `qrtz_paused_trigger_grps` WRITE;
/*!40000 ALTER TABLE `qrtz_paused_trigger_grps` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_paused_trigger_grps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_scheduler_state`
--

DROP TABLE IF EXISTS `qrtz_scheduler_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_scheduler_state` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度名称',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '实例名称',
  `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
  `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
  PRIMARY KEY (`sched_name`,`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调度器状态表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_scheduler_state`
--

LOCK TABLES `qrtz_scheduler_state` WRITE;
/*!40000 ALTER TABLE `qrtz_scheduler_state` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_scheduler_state` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_simple_triggers`
--

DROP TABLE IF EXISTS `qrtz_simple_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simple_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
  `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
  `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简单触发器的信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_simple_triggers`
--

LOCK TABLES `qrtz_simple_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_simple_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_simple_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_simprop_triggers`
--

DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simprop_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `str_prop_1` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
  `str_prop_2` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
  `str_prop_3` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
  `int_prop_1` int DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
  `int_prop_2` int DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
  `long_prop_1` bigint DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
  `long_prop_2` bigint DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
  `dec_prop_1` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
  `dec_prop_2` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
  `bool_prop_1` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
  `bool_prop_2` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同步机制的行锁表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_simprop_triggers`
--

LOCK TABLES `qrtz_simprop_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_simprop_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_simprop_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_triggers`
--

DROP TABLE IF EXISTS `qrtz_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发器的名字',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发器所属组的名字',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '相关介绍',
  `next_fire_time` bigint DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
  `prev_fire_time` bigint DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `trigger_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发器状态',
  `trigger_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发器的类型',
  `start_time` bigint NOT NULL COMMENT '开始时间',
  `end_time` bigint DEFAULT NULL COMMENT '结束时间',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '日程表名称',
  `misfire_instr` smallint DEFAULT NULL COMMENT '补偿执行的策略',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  KEY `sched_name` (`sched_name`,`job_name`,`job_group`),
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='触发器详细信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_triggers`
--

LOCK TABLES `qrtz_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=103 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config`
--

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` VALUES (1,'主框架页-默认皮肤样式名称','sys.index.skinName','skin-blue','Y','admin','2026-06-01 16:53:29','',NULL,'蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow'),(2,'用户管理-账号初始密码','sys.user.initPassword','123456','Y','admin','2026-06-01 16:53:29','',NULL,'初始化密码 123456'),(3,'主框架页-侧边栏主题','sys.index.sideTheme','theme-dark','Y','admin','2026-06-01 16:53:29','',NULL,'深色主题theme-dark，浅色主题theme-light'),(4,'账号自助-是否开启用户注册功能','sys.account.registerUser','false','Y','admin','2026-06-01 16:53:29','',NULL,'是否开启注册用户功能（true开启，false关闭）'),(5,'用户登录-黑名单列表','sys.login.blackIPList','','Y','admin','2026-06-01 16:53:29','',NULL,'设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）'),(6,'用户管理-初始密码修改策略','sys.account.initPasswordModify','1','Y','admin','2026-06-01 16:53:29','',NULL,'0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框'),(7,'用户管理-账号密码更新周期','sys.account.passwordValidateDays','0','Y','admin','2026-06-01 16:53:29','',NULL,'密码更新周期（填写数字，数据初始化值为0不限制，若修改必须为大于0小于365的正整数），如果超过这个周期登录系统时，则在登录时就会提醒修改密码对话框'),(8,'用户管理-密码字符范围','sys.account.chrtype','0','Y','admin','2026-06-01 16:53:29','',NULL,'默认任意字符范围，0任意（密码可以输入任意字符），1数字（密码只能为0-9数字），2英文字母（密码只能为a-z和A-Z字母），3字母和数字（密码必须包含字母，数字）,4字母数字和特殊字符（目前支持的特殊字符包括：~!@#$%^&*()-=_+）'),(100,'PC端单点登录','sys.login.singleLogin','false','Y','admin','2026-06-14 06:05:29','',NULL,'开启后同一账号PC端只允许一个在线，新登录踢掉旧登录'),(101,'登录页禁止保存密码','sys.login.preventSavePassword','false','Y','admin','2026-06-14 06:05:29','',NULL,'开启后登录页清空浏览器自动填充的密码，阻止密码保存'),(102,'登录验证码开关','sys.account.captchaEnabled','false','Y','admin','2026-06-14 06:05:29','admin','2026-06-14 06:06:48','是否开启登录验证码（true开启/false关闭）');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `province_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '省份编码',
  `province_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '省份名称',
  `city_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '城市编码',
  `city_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '城市名称',
  `district_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '区县编码',
  `district_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '区县名称',
  `street_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '街道编码',
  `street_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '街道名称',
  `detail_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详细地址',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dept`
--

LOCK TABLES `sys_dept` WRITE;
/*!40000 ALTER TABLE `sys_dept` DISABLE KEYS */;
INSERT INTO `sys_dept` VALUES (100,0,'0','若依科技',0,'若依','15888888888','ry@qq.com',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'0','0','admin','2026-06-01 16:53:28','',NULL),(101,100,'0,100','深圳总公司',1,'若依','15888888888','ry@qq.com',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'0','0','admin','2026-06-01 16:53:28','',NULL),(102,100,'0,100','长沙分公司',2,'若依','15888888888','ry@qq.com',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'0','0','admin','2026-06-01 16:53:28','',NULL),(103,101,'0,100,101','研发部门',1,'若依','15888888888','ry@qq.com',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'0','0','admin','2026-06-01 16:53:28','',NULL),(104,101,'0,100,101','市场部门',2,'若依','15888888888','ry@qq.com',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'0','0','admin','2026-06-01 16:53:28','',NULL),(105,101,'0,100,101','测试部门',3,'若依','15888888888','ry@qq.com',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'0','0','admin','2026-06-01 16:53:28','',NULL),(106,101,'0,100,101','财务部门',4,'若依','15888888888','ry@qq.com',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'0','0','admin','2026-06-01 16:53:28','',NULL),(107,101,'0,100,101','运维部门',5,'若依','15888888888','ry@qq.com',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'0','0','admin','2026-06-01 16:53:28','',NULL),(108,102,'0,100,102','市场部门',1,'若依','15888888888','ry@qq.com',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'0','0','admin','2026-06-01 16:53:28','',NULL),(109,102,'0,100,102','财务部门',2,'若依','15888888888','ry@qq.com',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'0','0','admin','2026-06-01 16:53:28','',NULL);
/*!40000 ALTER TABLE `sys_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB AUTO_INCREMENT=107 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
INSERT INTO `sys_dict_data` VALUES (1,1,'男','0','sys_user_sex','','','Y','0','admin','2026-06-01 16:53:29','',NULL,'性别男'),(2,2,'女','1','sys_user_sex','','','N','0','admin','2026-06-01 16:53:29','',NULL,'性别女'),(3,3,'未知','2','sys_user_sex','','','N','0','admin','2026-06-01 16:53:29','',NULL,'性别未知'),(4,1,'显示','0','sys_show_hide','','primary','Y','0','admin','2026-06-01 16:53:29','',NULL,'显示菜单'),(5,2,'隐藏','1','sys_show_hide','','danger','N','0','admin','2026-06-01 16:53:29','',NULL,'隐藏菜单'),(6,1,'正常','0','sys_normal_disable','','primary','Y','0','admin','2026-06-01 16:53:29','',NULL,'正常状态'),(7,2,'停用','1','sys_normal_disable','','danger','N','0','admin','2026-06-01 16:53:29','',NULL,'停用状态'),(8,1,'正常','0','sys_job_status','','primary','Y','0','admin','2026-06-01 16:53:29','',NULL,'正常状态'),(9,2,'暂停','1','sys_job_status','','danger','N','0','admin','2026-06-01 16:53:29','',NULL,'停用状态'),(10,1,'默认','DEFAULT','sys_job_group','','','Y','0','admin','2026-06-01 16:53:29','',NULL,'默认分组'),(11,2,'系统','SYSTEM','sys_job_group','','','N','0','admin','2026-06-01 16:53:29','',NULL,'系统分组'),(12,1,'是','Y','sys_yes_no','','primary','Y','0','admin','2026-06-01 16:53:29','',NULL,'系统默认是'),(13,2,'否','N','sys_yes_no','','danger','N','0','admin','2026-06-01 16:53:29','',NULL,'系统默认否'),(14,1,'通知','1','sys_notice_type','','warning','Y','0','admin','2026-06-01 16:53:29','',NULL,'通知'),(15,2,'公告','2','sys_notice_type','','success','N','0','admin','2026-06-01 16:53:29','',NULL,'公告'),(16,1,'正常','0','sys_notice_status','','primary','Y','0','admin','2026-06-01 16:53:29','',NULL,'正常状态'),(17,2,'关闭','1','sys_notice_status','','danger','N','0','admin','2026-06-01 16:53:29','',NULL,'关闭状态'),(18,99,'其他','0','sys_oper_type','','info','N','0','admin','2026-06-01 16:53:29','',NULL,'其他操作'),(19,1,'新增','1','sys_oper_type','','info','N','0','admin','2026-06-01 16:53:29','',NULL,'新增操作'),(20,2,'修改','2','sys_oper_type','','info','N','0','admin','2026-06-01 16:53:29','',NULL,'修改操作'),(21,3,'删除','3','sys_oper_type','','danger','N','0','admin','2026-06-01 16:53:29','',NULL,'删除操作'),(22,4,'授权','4','sys_oper_type','','primary','N','0','admin','2026-06-01 16:53:29','',NULL,'授权操作'),(23,5,'导出','5','sys_oper_type','','warning','N','0','admin','2026-06-01 16:53:29','',NULL,'导出操作'),(24,6,'导入','6','sys_oper_type','','warning','N','0','admin','2026-06-01 16:53:29','',NULL,'导入操作'),(25,7,'强退','7','sys_oper_type','','danger','N','0','admin','2026-06-01 16:53:29','',NULL,'强退操作'),(26,8,'生成代码','8','sys_oper_type','','warning','N','0','admin','2026-06-01 16:53:29','',NULL,'生成操作'),(27,9,'清空数据','9','sys_oper_type','','danger','N','0','admin','2026-06-01 16:53:29','',NULL,'清空操作'),(28,1,'成功','0','sys_common_status','','primary','N','0','admin','2026-06-01 16:53:29','',NULL,'正常状态'),(29,2,'失败','1','sys_common_status','','danger','N','0','admin','2026-06-01 16:53:29','',NULL,'停用状态'),(100,1,'体验卡','experience','mem_card_type',NULL,NULL,'Y','0','admin','2026-06-02 12:42:14','',NULL,'体验卡'),(101,2,'正式会员卡','formal','mem_card_type',NULL,NULL,'N','0','admin','2026-06-02 12:42:14','',NULL,'正式会员卡'),(102,3,'一星会员','star1','mem_card_type',NULL,NULL,'N','0','admin','2026-06-02 12:42:14','',NULL,'一星会员'),(103,4,'二星会员','star2','mem_card_type',NULL,NULL,'N','0','admin','2026-06-02 12:42:14','',NULL,'二星会员'),(104,5,'三星会员','star3','mem_card_type',NULL,NULL,'N','0','admin','2026-06-02 12:42:14','',NULL,'三星会员'),(105,6,'四星会员','star4','mem_card_type',NULL,NULL,'N','0','admin','2026-06-02 12:42:14','',NULL,'四星会员'),(106,7,'五星会员','star5','mem_card_type',NULL,NULL,'N','0','admin','2026-06-02 12:42:14','',NULL,'五星会员');
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_type`
--

LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
INSERT INTO `sys_dict_type` VALUES (1,'用户性别','sys_user_sex','0','admin','2026-06-01 16:53:29','',NULL,'用户性别列表'),(2,'菜单状态','sys_show_hide','0','admin','2026-06-01 16:53:29','',NULL,'菜单状态列表'),(3,'系统开关','sys_normal_disable','0','admin','2026-06-01 16:53:29','',NULL,'系统开关列表'),(4,'任务状态','sys_job_status','0','admin','2026-06-01 16:53:29','',NULL,'任务状态列表'),(5,'任务分组','sys_job_group','0','admin','2026-06-01 16:53:29','',NULL,'任务分组列表'),(6,'系统是否','sys_yes_no','0','admin','2026-06-01 16:53:29','',NULL,'系统是否列表'),(7,'通知类型','sys_notice_type','0','admin','2026-06-01 16:53:29','',NULL,'通知类型列表'),(8,'通知状态','sys_notice_status','0','admin','2026-06-01 16:53:29','',NULL,'通知状态列表'),(9,'操作类型','sys_oper_type','0','admin','2026-06-01 16:53:29','',NULL,'操作类型列表'),(10,'系统状态','sys_common_status','0','admin','2026-06-01 16:53:29','',NULL,'登录状态列表'),(100,'会员卡类型','mem_card_type','0','admin','2026-06-02 12:42:14','',NULL,'会员卡类型字典');
/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_job`
--

DROP TABLE IF EXISTS `sys_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`,`job_name`,`job_group`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务调度表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_job`
--

LOCK TABLES `sys_job` WRITE;
/*!40000 ALTER TABLE `sys_job` DISABLE KEYS */;
INSERT INTO `sys_job` VALUES (1,'系统默认（无参）','DEFAULT','ryTask.ryNoParams','0/10 * * * * ?','3','1','1','admin','2026-06-01 16:53:29','',NULL,''),(2,'系统默认（有参）','DEFAULT','ryTask.ryParams(\'ry\')','0/15 * * * * ?','3','1','1','admin','2026-06-01 16:53:29','',NULL,''),(3,'系统默认（多参）','DEFAULT','ryTask.ryMultipleParams(\'ry\', true, 2000L, 316.50D, 100)','0/20 * * * * ?','3','1','1','admin','2026-06-01 16:53:29','',NULL,'');
/*!40000 ALTER TABLE `sys_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_job_log`
--

DROP TABLE IF EXISTS `sys_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job_log` (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '日志信息',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '异常信息',
  `start_time` datetime DEFAULT NULL COMMENT '执行开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '执行结束时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务调度日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_job_log`
--

LOCK TABLES `sys_job_log` WRITE;
/*!40000 ALTER TABLE `sys_job_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_job_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_logininfor`
--

DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '登录IP地址',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '提示信息',
  `access_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`access_time`)
) ENGINE=InnoDB AUTO_INCREMENT=193 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_logininfor`
--

LOCK TABLES `sys_logininfor` WRITE;
/*!40000 ALTER TABLE `sys_logininfor` DISABLE KEYS */;
INSERT INTO `sys_logininfor` VALUES (100,'admin','172.20.0.5','0','登录成功','2026-06-02 10:05:03'),(101,'admin','192.168.65.1','0','退出成功','2026-06-02 10:05:04'),(102,'admin','172.20.0.5','0','登录成功','2026-06-02 10:08:20'),(103,'admin','192.168.65.1','0','登录成功','2026-06-02 10:45:28'),(104,'admin','192.168.65.1','0','退出成功','2026-06-02 10:45:29'),(105,'admin','172.20.0.5','0','登录成功','2026-06-02 10:46:37'),(106,'admin','192.168.65.1','0','登录成功','2026-06-02 10:48:41'),(107,'admin','192.168.65.1','0','退出成功','2026-06-02 14:58:37'),(108,'admin','172.20.0.8','0','登录成功','2026-06-02 15:07:15'),(109,'admin','192.168.65.1','0','登录成功','2026-06-02 15:07:43'),(110,'admin','192.168.65.1','0','退出成功','2026-06-02 15:07:43'),(111,'admin','192.168.65.1','0','登录成功','2026-06-02 15:08:31'),(112,'admin','192.168.65.1','0','退出成功','2026-06-02 15:08:31'),(113,'admin','172.20.0.7','0','登录成功','2026-06-02 15:12:03'),(114,'admin','172.20.0.7','0','登录成功','2026-06-02 15:17:29'),(115,'admin','192.168.65.1','0','登录成功','2026-06-02 15:23:05'),(116,'admin','192.168.65.1','0','退出成功','2026-06-02 15:33:42'),(117,'SD','192.168.65.1','1','密码输入错误1次','2026-06-02 15:33:56'),(118,'admin','192.168.65.1','0','登录成功','2026-06-02 16:13:26'),(119,'admin','192.168.65.1','0','退出成功','2026-06-02 16:13:52'),(120,'SD','192.168.65.1','1','密码输入错误1次','2026-06-02 16:14:02'),(121,'SD','192.168.65.1','0','登录成功','2026-06-02 16:14:12'),(122,'SD','192.168.65.1','0','退出成功','2026-06-02 16:14:42'),(123,'admin','192.168.65.1','0','登录成功','2026-06-02 16:27:15'),(124,'admin','192.168.65.1','0','退出成功','2026-06-02 16:27:30'),(125,'admin','192.168.65.1','0','登录成功','2026-06-02 16:28:49'),(126,'admin','192.168.65.1','0','登录成功','2026-06-03 09:57:18'),(127,'admin','192.168.65.1','0','登录成功','2026-06-03 11:27:14'),(128,'admin','192.168.65.1','0','登录成功','2026-06-03 11:27:21'),(129,'admin','127.0.0.1','0','登录成功','2026-06-03 12:49:33'),(130,'admin','192.168.65.1','1','密码输入错误1次','2026-06-03 13:24:38'),(131,'admin','172.20.0.7','1','密码输入错误1次','2026-06-03 13:55:08'),(132,'admin','172.20.0.7','1','密码输入错误1次','2026-06-03 14:10:48'),(133,'admin','172.20.0.7','1','密码输入错误2次','2026-06-03 14:16:03'),(134,'admin','192.168.65.1','1','密码输入错误3次','2026-06-03 14:17:16'),(135,'admin','192.168.65.1','0','登录成功','2026-06-03 16:29:35'),(136,'admin','192.168.65.1','0','退出成功','2026-06-03 17:41:21'),(137,'admin','192.168.65.1','0','登录成功','2026-06-03 17:41:25'),(138,'admin','192.168.65.1','0','退出成功','2026-06-03 17:50:41'),(139,'admin','192.168.65.1','0','登录成功','2026-06-03 17:59:49'),(140,'admin','192.168.65.1','0','退出成功','2026-06-03 18:02:41'),(141,'admin','192.168.65.1','0','登录成功','2026-06-03 18:02:45'),(142,'admin','111.201.228.78','0','登录成功','2026-06-13 20:55:30'),(143,'admin','111.201.228.78','0','退出成功','2026-06-13 20:55:31'),(144,'admin','111.201.228.78','0','登录成功','2026-06-13 21:20:42'),(145,'admin','111.201.228.78','0','退出成功','2026-06-13 21:20:44'),(146,'admin','111.201.228.78','0','登录成功','2026-06-13 21:27:36'),(147,'admin','111.201.228.78','0','退出成功','2026-06-13 21:27:37'),(148,'admin','111.201.228.78','0','登录成功','2026-06-13 21:47:14'),(149,'admin','111.201.228.78','0','退出成功','2026-06-13 21:47:16'),(150,'admin','111.201.228.78','0','登录成功','2026-06-13 21:57:25'),(151,'admin','111.201.228.78','0','退出成功','2026-06-13 21:57:26'),(152,'admin','111.201.228.78','0','登录成功','2026-06-13 22:02:37'),(153,'admin','111.201.228.78','0','退出成功','2026-06-13 22:02:37'),(154,'admin','111.201.228.78','0','登录成功','2026-06-13 22:04:18'),(155,'admin','111.201.228.78','0','退出成功','2026-06-13 22:04:18'),(156,'admin','111.201.228.78','0','登录成功','2026-06-13 22:10:48'),(157,'admin','111.201.228.78','0','登录成功','2026-06-13 22:13:02'),(158,'admin','111.201.228.78','0','登录成功','2026-06-13 22:14:29'),(159,'admin','111.201.228.78','0','退出成功','2026-06-13 22:14:30'),(160,'admin','111.201.228.78','0','登录成功','2026-06-13 22:14:37'),(161,'admin','111.201.228.78','0','退出成功','2026-06-13 22:14:37'),(162,'admin','127.0.0.1','0','登录成功','2026-06-13 22:23:49'),(163,'admin','127.0.0.1','0','登录成功','2026-06-13 22:29:01'),(164,'admin','38.106.18.2','0','登录成功','2026-06-13 22:29:24'),(165,'admin','38.106.18.2','0','退出成功','2026-06-13 22:29:28'),(166,'admin','127.0.0.1','0','登录成功','2026-06-13 22:31:05'),(167,'admin','38.106.18.2','0','登录成功','2026-06-13 22:32:22'),(168,'admin','38.106.18.2','0','退出成功','2026-06-13 22:32:25'),(169,'admin','127.0.0.1','0','登录成功','2026-06-13 22:35:10'),(170,'admin','38.106.18.2','0','登录成功','2026-06-13 22:38:20'),(171,'admin','38.106.18.2','0','退出成功','2026-06-13 22:39:28'),(172,'admin','38.106.18.2','0','登录成功','2026-06-13 22:46:34'),(173,'admin','111.201.228.78','0','退出成功','2026-06-13 22:46:35'),(174,'admin','111.201.228.78','0','登录成功','2026-06-13 22:47:01'),(175,'admin','111.201.228.78','0','登录成功','2026-06-14 05:56:17'),(176,'admin','111.201.228.78','0','退出成功','2026-06-14 06:02:51'),(177,'admin','111.201.228.78','0','登录成功','2026-06-14 06:02:58'),(178,'admin','111.201.228.78','0','登录成功','2026-06-14 06:05:49'),(179,'admin','111.201.228.78','0','登录成功','2026-06-14 06:05:58'),(180,'admin','111.201.228.78','0','退出成功','2026-06-14 06:06:16'),(181,'admin','111.201.228.78','0','登录成功','2026-06-14 06:06:23'),(182,'admin','111.201.228.78','0','退出成功','2026-06-14 06:06:52'),(183,'admin','111.201.228.78','0','登录成功','2026-06-14 06:09:32'),(184,'admin','111.201.228.78','0','登录成功','2026-06-14 06:18:43'),(185,'admin','111.201.228.78','0','登录成功','2026-06-14 06:23:49'),(186,'admin','111.201.228.78','0','登录成功','2026-06-14 06:24:46'),(187,'Admin','114.244.128.172','0','登录成功','2026-06-14 07:21:51'),(188,'Admin','114.244.128.172','0','登录成功','2026-06-14 10:07:29'),(189,'admin','114.244.128.172','0','退出成功','2026-06-14 10:24:29'),(190,'Admin','114.244.128.172','0','登录成功','2026-06-14 10:24:32'),(191,'admin','111.201.228.78','0','退出成功','2026-06-14 10:33:45'),(192,'admin','111.201.228.78','0','登录成功','2026-06-14 10:33:56');
/*!40000 ALTER TABLE `sys_logininfor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由参数',
  `route_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '路由名称',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2179 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (1,'系统管理',0,1,'system',NULL,'','',1,0,'M','0','0','','system','admin','2026-06-01 16:53:28','',NULL,'系统管理目录'),(2,'系统监控',0,2,'monitor',NULL,'','',1,0,'M','0','0','','monitor','admin','2026-06-01 16:53:28','',NULL,'系统监控目录'),(3,'系统工具',0,3,'tool',NULL,'','',1,0,'M','0','0','','tool','admin','2026-06-01 16:53:28','',NULL,'系统工具目录'),(100,'用户管理',1,1,'user','system/user/index','','',1,0,'C','0','0','system:user:list','user','admin','2026-06-01 16:53:28','',NULL,'用户管理菜单'),(101,'角色管理',1,2,'role','system/role/index','','',1,0,'C','0','0','system:role:list','peoples','admin','2026-06-01 16:53:28','',NULL,'角色管理菜单'),(102,'菜单管理',1,3,'menu','system/menu/index','','',1,0,'C','0','0','system:menu:list','tree-table','admin','2026-06-01 16:53:28','',NULL,'菜单管理菜单'),(103,'部门管理',1,4,'dept','system/dept/index','','',1,0,'C','0','0','system:dept:list','tree','admin','2026-06-01 16:53:28','',NULL,'部门管理菜单'),(104,'岗位管理',1,5,'post','system/post/index','','',1,0,'C','0','0','system:post:list','post','admin','2026-06-01 16:53:28','',NULL,'岗位管理菜单'),(105,'字典管理',1,6,'dict','system/dict/index','','',1,0,'C','0','0','system:dict:list','dict','admin','2026-06-01 16:53:28','',NULL,'字典管理菜单'),(106,'参数设置',1,7,'config','system/config/index','','',1,0,'C','0','0','system:config:list','edit','admin','2026-06-01 16:53:28','',NULL,'参数设置菜单'),(107,'通知公告',1,8,'notice','system/notice/index','','',1,0,'C','0','0','system:notice:list','message','admin','2026-06-01 16:53:28','',NULL,'通知公告菜单'),(108,'日志管理',1,9,'log','','','',1,0,'M','0','0','','log','admin','2026-06-01 16:53:28','',NULL,'日志管理菜单'),(109,'在线用户',2,1,'online','monitor/online/index','','',1,0,'C','0','0','monitor:online:list','online','admin','2026-06-01 16:53:28','',NULL,'在线用户菜单'),(110,'定时任务',2,2,'job','monitor/job/index','','',1,0,'C','0','0','monitor:job:list','job','admin','2026-06-01 16:53:28','',NULL,'定时任务菜单'),(111,'Sentinel控制台',2,3,'http://localhost:8718','','','',0,0,'C','0','0','monitor:sentinel:list','sentinel','admin','2026-06-01 16:53:28','',NULL,'流量控制菜单'),(112,'Nacos控制台',2,4,'http://localhost:8848/nacos','','','',0,0,'C','0','0','monitor:nacos:list','nacos','admin','2026-06-01 16:53:28','',NULL,'服务治理菜单'),(113,'Admin控制台',2,5,'http://localhost:9100/login','','','',0,0,'C','0','0','monitor:server:list','server','admin','2026-06-01 16:53:28','',NULL,'服务监控菜单'),(114,'表单构建',3,1,'build','tool/build/index','','',1,0,'C','0','0','tool:build:list','build','admin','2026-06-01 16:53:28','',NULL,'表单构建菜单'),(115,'代码生成',3,2,'gen','tool/gen/index','','',1,0,'C','0','0','tool:gen:list','code','admin','2026-06-01 16:53:28','',NULL,'代码生成菜单'),(116,'系统接口',3,3,'http://localhost:8080/swagger-ui/index.html','','','',0,0,'C','0','0','tool:swagger:list','swagger','admin','2026-06-01 16:53:28','',NULL,'系统接口菜单'),(500,'操作日志',108,1,'operlog','system/operlog/index','','',1,0,'C','0','0','system:operlog:list','form','admin','2026-06-01 16:53:28','',NULL,'操作日志菜单'),(501,'登录日志',108,2,'logininfor','system/logininfor/index','','',1,0,'C','0','0','system:logininfor:list','logininfor','admin','2026-06-01 16:53:28','',NULL,'登录日志菜单'),(1000,'用户查询',100,1,'','','','',1,0,'F','0','0','system:user:query','#','admin','2026-06-01 16:53:28','',NULL,''),(1001,'用户新增',100,2,'','','','',1,0,'F','0','0','system:user:add','#','admin','2026-06-01 16:53:28','',NULL,''),(1002,'用户修改',100,3,'','','','',1,0,'F','0','0','system:user:edit','#','admin','2026-06-01 16:53:28','',NULL,''),(1003,'用户删除',100,4,'','','','',1,0,'F','0','0','system:user:remove','#','admin','2026-06-01 16:53:28','',NULL,''),(1004,'用户导出',100,5,'','','','',1,0,'F','0','0','system:user:export','#','admin','2026-06-01 16:53:28','',NULL,''),(1005,'用户导入',100,6,'','','','',1,0,'F','0','0','system:user:import','#','admin','2026-06-01 16:53:28','',NULL,''),(1006,'重置密码',100,7,'','','','',1,0,'F','0','0','system:user:resetPwd','#','admin','2026-06-01 16:53:28','',NULL,''),(1007,'角色查询',101,1,'','','','',1,0,'F','0','0','system:role:query','#','admin','2026-06-01 16:53:28','',NULL,''),(1008,'角色新增',101,2,'','','','',1,0,'F','0','0','system:role:add','#','admin','2026-06-01 16:53:28','',NULL,''),(1009,'角色修改',101,3,'','','','',1,0,'F','0','0','system:role:edit','#','admin','2026-06-01 16:53:28','',NULL,''),(1010,'角色删除',101,4,'','','','',1,0,'F','0','0','system:role:remove','#','admin','2026-06-01 16:53:28','',NULL,''),(1011,'角色导出',101,5,'','','','',1,0,'F','0','0','system:role:export','#','admin','2026-06-01 16:53:28','',NULL,''),(1012,'菜单查询',102,1,'','','','',1,0,'F','0','0','system:menu:query','#','admin','2026-06-01 16:53:28','',NULL,''),(1013,'菜单新增',102,2,'','','','',1,0,'F','0','0','system:menu:add','#','admin','2026-06-01 16:53:28','',NULL,''),(1014,'菜单修改',102,3,'','','','',1,0,'F','0','0','system:menu:edit','#','admin','2026-06-01 16:53:28','',NULL,''),(1015,'菜单删除',102,4,'','','','',1,0,'F','0','0','system:menu:remove','#','admin','2026-06-01 16:53:28','',NULL,''),(1016,'部门查询',103,1,'','','','',1,0,'F','0','0','system:dept:query','#','admin','2026-06-01 16:53:28','',NULL,''),(1017,'部门新增',103,2,'','','','',1,0,'F','0','0','system:dept:add','#','admin','2026-06-01 16:53:28','',NULL,''),(1018,'部门修改',103,3,'','','','',1,0,'F','0','0','system:dept:edit','#','admin','2026-06-01 16:53:28','',NULL,''),(1019,'部门删除',103,4,'','','','',1,0,'F','0','0','system:dept:remove','#','admin','2026-06-01 16:53:28','',NULL,''),(1020,'岗位查询',104,1,'','','','',1,0,'F','0','0','system:post:query','#','admin','2026-06-01 16:53:28','',NULL,''),(1021,'岗位新增',104,2,'','','','',1,0,'F','0','0','system:post:add','#','admin','2026-06-01 16:53:28','',NULL,''),(1022,'岗位修改',104,3,'','','','',1,0,'F','0','0','system:post:edit','#','admin','2026-06-01 16:53:28','',NULL,''),(1023,'岗位删除',104,4,'','','','',1,0,'F','0','0','system:post:remove','#','admin','2026-06-01 16:53:28','',NULL,''),(1024,'岗位导出',104,5,'','','','',1,0,'F','0','0','system:post:export','#','admin','2026-06-01 16:53:28','',NULL,''),(1025,'字典查询',105,1,'#','','','',1,0,'F','0','0','system:dict:query','#','admin','2026-06-01 16:53:28','',NULL,''),(1026,'字典新增',105,2,'#','','','',1,0,'F','0','0','system:dict:add','#','admin','2026-06-01 16:53:28','',NULL,''),(1027,'字典修改',105,3,'#','','','',1,0,'F','0','0','system:dict:edit','#','admin','2026-06-01 16:53:28','',NULL,''),(1028,'字典删除',105,4,'#','','','',1,0,'F','0','0','system:dict:remove','#','admin','2026-06-01 16:53:28','',NULL,''),(1029,'字典导出',105,5,'#','','','',1,0,'F','0','0','system:dict:export','#','admin','2026-06-01 16:53:28','',NULL,''),(1030,'参数查询',106,1,'#','','','',1,0,'F','0','0','system:config:query','#','admin','2026-06-01 16:53:28','',NULL,''),(1031,'参数新增',106,2,'#','','','',1,0,'F','0','0','system:config:add','#','admin','2026-06-01 16:53:28','',NULL,''),(1032,'参数修改',106,3,'#','','','',1,0,'F','0','0','system:config:edit','#','admin','2026-06-01 16:53:29','',NULL,''),(1033,'参数删除',106,4,'#','','','',1,0,'F','0','0','system:config:remove','#','admin','2026-06-01 16:53:29','',NULL,''),(1034,'参数导出',106,5,'#','','','',1,0,'F','0','0','system:config:export','#','admin','2026-06-01 16:53:29','',NULL,''),(1035,'公告查询',107,1,'#','','','',1,0,'F','0','0','system:notice:query','#','admin','2026-06-01 16:53:29','',NULL,''),(1036,'公告新增',107,2,'#','','','',1,0,'F','0','0','system:notice:add','#','admin','2026-06-01 16:53:29','',NULL,''),(1037,'公告修改',107,3,'#','','','',1,0,'F','0','0','system:notice:edit','#','admin','2026-06-01 16:53:29','',NULL,''),(1038,'公告删除',107,4,'#','','','',1,0,'F','0','0','system:notice:remove','#','admin','2026-06-01 16:53:29','',NULL,''),(1039,'操作查询',500,1,'#','','','',1,0,'F','0','0','system:operlog:query','#','admin','2026-06-01 16:53:29','',NULL,''),(1040,'操作删除',500,2,'#','','','',1,0,'F','0','0','system:operlog:remove','#','admin','2026-06-01 16:53:29','',NULL,''),(1041,'日志导出',500,3,'#','','','',1,0,'F','0','0','system:operlog:export','#','admin','2026-06-01 16:53:29','',NULL,''),(1042,'登录查询',501,1,'#','','','',1,0,'F','0','0','system:logininfor:query','#','admin','2026-06-01 16:53:29','',NULL,''),(1043,'登录删除',501,2,'#','','','',1,0,'F','0','0','system:logininfor:remove','#','admin','2026-06-01 16:53:29','',NULL,''),(1044,'日志导出',501,3,'#','','','',1,0,'F','0','0','system:logininfor:export','#','admin','2026-06-01 16:53:29','',NULL,''),(1045,'账户解锁',501,4,'#','','','',1,0,'F','0','0','system:logininfor:unlock','#','admin','2026-06-01 16:53:29','',NULL,''),(1046,'在线查询',109,1,'#','','','',1,0,'F','0','0','monitor:online:query','#','admin','2026-06-01 16:53:29','',NULL,''),(1047,'批量强退',109,2,'#','','','',1,0,'F','0','0','monitor:online:batchLogout','#','admin','2026-06-01 16:53:29','',NULL,''),(1048,'单条强退',109,3,'#','','','',1,0,'F','0','0','monitor:online:forceLogout','#','admin','2026-06-01 16:53:29','',NULL,''),(1049,'任务查询',110,1,'#','','','',1,0,'F','0','0','monitor:job:query','#','admin','2026-06-01 16:53:29','',NULL,''),(1050,'任务新增',110,2,'#','','','',1,0,'F','0','0','monitor:job:add','#','admin','2026-06-01 16:53:29','',NULL,''),(1051,'任务修改',110,3,'#','','','',1,0,'F','0','0','monitor:job:edit','#','admin','2026-06-01 16:53:29','',NULL,''),(1052,'任务删除',110,4,'#','','','',1,0,'F','0','0','monitor:job:remove','#','admin','2026-06-01 16:53:29','',NULL,''),(1053,'状态修改',110,5,'#','','','',1,0,'F','0','0','monitor:job:changeStatus','#','admin','2026-06-01 16:53:29','',NULL,''),(1054,'任务导出',110,6,'#','','','',1,0,'F','0','0','monitor:job:export','#','admin','2026-06-01 16:53:29','',NULL,''),(1055,'生成查询',115,1,'#','','','',1,0,'F','0','0','tool:gen:query','#','admin','2026-06-01 16:53:29','',NULL,''),(1056,'生成修改',115,2,'#','','','',1,0,'F','0','0','tool:gen:edit','#','admin','2026-06-01 16:53:29','',NULL,''),(1057,'生成删除',115,3,'#','','','',1,0,'F','0','0','tool:gen:remove','#','admin','2026-06-01 16:53:29','',NULL,''),(1058,'导入代码',115,2,'#','','','',1,0,'F','0','0','tool:gen:import','#','admin','2026-06-01 16:53:29','',NULL,''),(1059,'预览代码',115,4,'#','','','',1,0,'F','0','0','tool:gen:preview','#','admin','2026-06-01 16:53:29','',NULL,''),(1060,'生成代码',115,5,'#','','','',1,0,'F','0','0','tool:gen:code','#','admin','2026-06-01 16:53:29','',NULL,''),(2000,'财务管理',0,4,'finance',NULL,'1','0',1,0,'M','0','0','','money','admin','2026-06-03 17:35:30','',NULL,'财务管理目录'),(2001,'商品管理',2000,1,'product','finance/product/index','1','0',1,0,'C','0','0','finance:product:list','shopping','admin','2026-06-03 17:35:30','',NULL,'商品管理菜单'),(2002,'商品查询',2001,1,'','','1','0',1,0,'F','0','0','finance:product:query','#','admin','2026-06-03 17:35:30','',NULL,''),(2003,'商品新增',2001,2,'','','1','0',1,0,'F','0','0','finance:product:add','#','admin','2026-06-03 17:35:30','',NULL,''),(2004,'商品修改',2001,3,'','','1','0',1,0,'F','0','0','finance:product:edit','#','admin','2026-06-03 17:35:30','',NULL,''),(2005,'商品删除',2001,4,'','','1','0',1,0,'F','0','0','finance:product:remove','#','admin','2026-06-03 17:35:30','',NULL,''),(2006,'商品导出',2001,5,'','','1','0',1,0,'F','0','0','finance:product:export','#','admin','2026-06-03 17:35:30','',NULL,''),(2010,'供应商管理',2000,2,'supplier','finance/supplier/index','1','0',1,0,'C','0','0','finance:supplier:list','peoples','admin','2026-06-03 17:35:30','',NULL,'供应商管理菜单'),(2011,'供应商查询',2010,1,'','','1','0',1,0,'F','0','0','finance:supplier:query','#','admin','2026-06-03 17:35:30','',NULL,''),(2012,'供应商新增',2010,2,'','','1','0',1,0,'F','0','0','finance:supplier:add','#','admin','2026-06-03 17:35:30','',NULL,''),(2013,'供应商修改',2010,3,'','','1','0',1,0,'F','0','0','finance:supplier:edit','#','admin','2026-06-03 17:35:30','',NULL,''),(2014,'供应商删除',2010,4,'','','1','0',1,0,'F','0','0','finance:supplier:remove','#','admin','2026-06-03 17:35:30','',NULL,''),(2015,'供应商导出',2010,5,'','','1','0',1,0,'F','0','0','finance:supplier:export','#','admin','2026-06-03 17:35:30','',NULL,''),(2020,'进货管理',2000,3,'purchase','finance/purchase/index','1','0',1,0,'C','0','0','finance:purchase:list','shopping','admin','2026-06-03 17:35:30','',NULL,'进货管理菜单'),(2021,'进货查询',2020,1,'','','1','0',1,0,'F','0','0','finance:purchase:query','#','admin','2026-06-03 17:35:30','',NULL,''),(2022,'进货新增',2020,2,'','','1','0',1,0,'F','0','0','finance:purchase:add','#','admin','2026-06-03 17:35:30','',NULL,''),(2023,'进货修改',2020,3,'','','1','0',1,0,'F','0','0','finance:purchase:edit','#','admin','2026-06-03 17:35:30','',NULL,''),(2024,'进货删除',2020,4,'','','1','0',1,0,'F','0','0','finance:purchase:remove','#','admin','2026-06-03 17:35:30','',NULL,''),(2025,'进货导出',2020,5,'','','1','0',1,0,'F','0','0','finance:purchase:export','#','admin','2026-06-03 17:35:30','',NULL,''),(2030,'销售记录',2000,4,'sale','finance/sale/index','1','0',1,0,'C','0','0','finance:sale:list','date','admin','2026-06-03 17:35:30','',NULL,'销售记录菜单'),(2031,'销售查询',2030,1,'','','1','0',1,0,'F','0','0','finance:sale:query','#','admin','2026-06-03 17:35:30','',NULL,''),(2032,'销售新增',2030,2,'','','1','0',1,0,'F','0','0','finance:sale:add','#','admin','2026-06-03 17:35:30','',NULL,''),(2033,'销售修改',2030,3,'','','1','0',1,0,'F','0','0','finance:sale:edit','#','admin','2026-06-03 17:35:30','',NULL,''),(2034,'销售删除',2030,4,'','','1','0',1,0,'F','0','0','finance:sale:remove','#','admin','2026-06-03 17:35:30','',NULL,''),(2035,'缴款修改',2030,5,'','','1','0',1,0,'F','0','0','finance:sale:payment','#','admin','2026-06-03 17:35:30','',NULL,''),(2036,'销售导出',2030,6,'','','1','0',1,0,'F','0','0','finance:sale:export','#','admin','2026-06-03 17:35:30','',NULL,''),(2040,'费用记录',2000,5,'expense','finance/expense/index','1','0',1,0,'C','0','0','finance:expense:list','shopping','admin','2026-06-03 17:35:30','',NULL,'费用记录菜单'),(2041,'费用查询',2040,1,'','','1','0',1,0,'F','0','0','finance:expense:query','#','admin','2026-06-03 17:35:30','',NULL,''),(2042,'费用新增',2040,2,'','','1','0',1,0,'F','0','0','finance:expense:add','#','admin','2026-06-03 17:35:30','',NULL,''),(2043,'费用修改',2040,3,'','','1','0',1,0,'F','0','0','finance:expense:edit','#','admin','2026-06-03 17:35:30','',NULL,''),(2044,'费用删除',2040,4,'','','1','0',1,0,'F','0','0','finance:expense:remove','#','admin','2026-06-03 17:35:30','',NULL,''),(2045,'费用核销',2040,5,'','','1','0',1,0,'F','0','0','finance:expense:edit','#','admin','2026-06-03 17:35:30','',NULL,''),(2046,'费用导出',2040,6,'','','1','0',1,0,'F','0','0','finance:expense:export','#','admin','2026-06-03 17:35:30','',NULL,''),(2050,'借支记录',2000,6,'advance','finance/advance/index','1','0',1,0,'C','0','0','finance:advance:list','money','admin','2026-06-03 17:35:30','',NULL,'借支记录菜单'),(2051,'借支查询',2050,1,'','','1','0',1,0,'F','0','0','finance:advance:query','#','admin','2026-06-03 17:35:30','',NULL,''),(2052,'借支新增',2050,2,'','','1','0',1,0,'F','0','0','finance:advance:add','#','admin','2026-06-03 17:35:30','',NULL,''),(2053,'借支修改',2050,3,'','','1','0',1,0,'F','0','0','finance:advance:edit','#','admin','2026-06-03 17:35:30','',NULL,''),(2054,'借支删除',2050,4,'','','1','0',1,0,'F','0','0','finance:advance:remove','#','admin','2026-06-03 17:35:30','',NULL,''),(2055,'借支核销',2050,5,'','','1','0',1,0,'F','0','0','finance:advance:edit','#','admin','2026-06-03 17:35:30','',NULL,''),(2056,'借支导出',2050,6,'','','1','0',1,0,'F','0','0','finance:advance:export','#','admin','2026-06-03 17:35:30','',NULL,''),(2060,'成本核算',2000,7,'costAccounting','finance/costAccounting/index','1','0',1,0,'C','0','0','finance:costAccounting:list','chart','admin','2026-06-03 17:35:30','',NULL,'成本核算菜单'),(2061,'成本查询',2060,1,'','','1','0',1,0,'F','0','0','finance:costAccounting:query','#','admin','2026-06-03 17:35:30','',NULL,''),(2062,'成本新增',2060,2,'','','1','0',1,0,'F','0','0','finance:costAccounting:add','#','admin','2026-06-03 17:35:30','',NULL,''),(2063,'成本修改',2060,3,'','','1','0',1,0,'F','0','0','finance:costAccounting:edit','#','admin','2026-06-03 17:35:30','',NULL,''),(2064,'成本删除',2060,4,'','','1','0',1,0,'F','0','0','finance:costAccounting:remove','#','admin','2026-06-03 17:35:30','',NULL,''),(2065,'成本导出',2060,5,'','','1','0',1,0,'F','0','0','finance:costAccounting:export','#','admin','2026-06-03 17:35:30','',NULL,''),(2070,'投资人返款',2000,8,'investorPayment','finance/investorPayment/index','1','0',1,0,'C','0','0','finance:investorPayment:list','peoples','admin','2026-06-03 17:35:30','',NULL,'投资人返款菜单'),(2071,'返款查询',2070,1,'','','1','0',1,0,'F','0','0','finance:investorPayment:query','#','admin','2026-06-03 17:35:30','',NULL,''),(2072,'返款新增',2070,2,'','','1','0',1,0,'F','0','0','finance:investorPayment:add','#','admin','2026-06-03 17:35:30','',NULL,''),(2073,'返款修改',2070,3,'','','1','0',1,0,'F','0','0','finance:investorPayment:edit','#','admin','2026-06-03 17:35:30','',NULL,''),(2074,'返款删除',2070,4,'','','1','0',1,0,'F','0','0','finance:investorPayment:remove','#','admin','2026-06-03 17:35:30','',NULL,''),(2075,'返款导出',2070,5,'','','1','0',1,0,'F','0','0','finance:investorPayment:export','#','admin','2026-06-03 17:35:30','',NULL,''),(2076,'会员服务',0,6,'member',NULL,NULL,'',1,0,'M','0','0','','peoples','admin','2026-06-03 17:49:09','',NULL,'会员服务目录'),(2077,'会员信息',2076,1,'member','member/member/index',NULL,'',1,0,'C','0','0','member:member:list','user','admin','2026-06-03 17:49:09','',NULL,'会员信息菜单'),(2078,'秒杀活动',2076,2,'seckill','member/seckill/index',NULL,'',1,0,'C','0','0','member:seckill:list','time-range','admin','2026-06-03 17:49:09','',NULL,'秒杀活动菜单'),(2079,'秒杀记录',2076,3,'seckillRecord','member/seckill/record',NULL,'',1,0,'C','0','0','member:seckillRecord:list','documentation','admin','2026-06-03 17:49:09','',NULL,'秒杀记录菜单'),(2080,'积分物品',2076,4,'pointsGoods','member/pointsGoods/index',NULL,'',1,0,'C','0','0','member:pointsGoods:list','shopping','admin','2026-06-03 17:49:09','',NULL,'积分物品菜单'),(2081,'积分规则',2076,5,'pointsRule','member/pointsRule/index',NULL,'',1,0,'C','0','0','member:pointsRule:list','tool','admin','2026-06-03 17:49:09','',NULL,'积分规则菜单'),(2082,'积分记录',2076,6,'pointsRecord','member/pointsRecord/index',NULL,'',1,0,'C','0','0','member:pointsRecord:list','list','admin','2026-06-03 17:49:09','',NULL,'积分记录菜单'),(2083,'积分兑换',2076,7,'pointsExchange','member/pointsExchange/index',NULL,'',1,0,'C','0','0','member:pointsExchange:list','shopping','admin','2026-06-03 17:49:09','',NULL,'积分兑换菜单'),(2084,'会员信息查询',2077,1,'','',NULL,'',1,0,'F','0','0','member:member:query','#','admin','2026-06-03 17:49:09','',NULL,''),(2085,'会员信息新增',2077,2,'','',NULL,'',1,0,'F','0','0','member:member:add','#','admin','2026-06-03 17:49:09','',NULL,''),(2086,'会员信息修改',2077,3,'','',NULL,'',1,0,'F','0','0','member:member:edit','#','admin','2026-06-03 17:49:09','',NULL,''),(2087,'会员信息删除',2077,4,'','',NULL,'',1,0,'F','0','0','member:member:remove','#','admin','2026-06-03 17:49:09','',NULL,''),(2088,'会员信息导出',2077,5,'','',NULL,'',1,0,'F','0','0','member:member:export','#','admin','2026-06-03 17:49:09','',NULL,''),(2089,'秒杀活动查询',2078,1,'','',NULL,'',1,0,'F','0','0','member:seckill:query','#','admin','2026-06-03 17:49:09','',NULL,''),(2090,'秒杀活动新增',2078,2,'','',NULL,'',1,0,'F','0','0','member:seckill:add','#','admin','2026-06-03 17:49:09','',NULL,''),(2091,'秒杀活动修改',2078,3,'','',NULL,'',1,0,'F','0','0','member:seckill:edit','#','admin','2026-06-03 17:49:09','',NULL,''),(2092,'秒杀活动删除',2078,4,'','',NULL,'',1,0,'F','0','0','member:seckill:remove','#','admin','2026-06-03 17:49:09','',NULL,''),(2093,'秒杀活动导出',2078,5,'','',NULL,'',1,0,'F','0','0','member:seckill:export','#','admin','2026-06-03 17:49:09','',NULL,''),(2094,'秒杀记录查询',2079,1,'','',NULL,'',1,0,'F','0','0','member:seckillRecord:query','#','admin','2026-06-03 17:49:09','',NULL,''),(2095,'秒杀记录新增',2079,2,'','',NULL,'',1,0,'F','0','0','member:seckillRecord:add','#','admin','2026-06-03 17:49:09','',NULL,''),(2096,'秒杀记录修改',2079,3,'','',NULL,'',1,0,'F','0','0','member:seckillRecord:edit','#','admin','2026-06-03 17:49:09','',NULL,''),(2097,'秒杀记录删除',2079,4,'','',NULL,'',1,0,'F','0','0','member:seckillRecord:remove','#','admin','2026-06-03 17:49:09','',NULL,''),(2098,'秒杀记录导出',2079,5,'','',NULL,'',1,0,'F','0','0','member:seckillRecord:export','#','admin','2026-06-03 17:49:09','',NULL,''),(2099,'积分物品查询',2080,1,'','',NULL,'',1,0,'F','0','0','member:pointsGoods:query','#','admin','2026-06-03 17:49:09','',NULL,''),(2100,'积分物品新增',2080,2,'','',NULL,'',1,0,'F','0','0','member:pointsGoods:add','#','admin','2026-06-03 17:49:09','',NULL,''),(2101,'积分物品修改',2080,3,'','',NULL,'',1,0,'F','0','0','member:pointsGoods:edit','#','admin','2026-06-03 17:49:09','',NULL,''),(2102,'积分物品删除',2080,4,'','',NULL,'',1,0,'F','0','0','member:pointsGoods:remove','#','admin','2026-06-03 17:49:09','',NULL,''),(2103,'积分物品导出',2080,5,'','',NULL,'',1,0,'F','0','0','member:pointsGoods:export','#','admin','2026-06-03 17:49:09','',NULL,''),(2104,'积分规则查询',2081,1,'','',NULL,'',1,0,'F','0','0','member:pointsRule:query','#','admin','2026-06-03 17:49:09','',NULL,''),(2105,'积分规则新增',2081,2,'','',NULL,'',1,0,'F','0','0','member:pointsRule:add','#','admin','2026-06-03 17:49:09','',NULL,''),(2106,'积分规则修改',2081,3,'','',NULL,'',1,0,'F','0','0','member:pointsRule:edit','#','admin','2026-06-03 17:49:09','',NULL,''),(2107,'积分规则删除',2081,4,'','',NULL,'',1,0,'F','0','0','member:pointsRule:remove','#','admin','2026-06-03 17:49:09','',NULL,''),(2108,'积分规则导出',2081,5,'','',NULL,'',1,0,'F','0','0','member:pointsRule:export','#','admin','2026-06-03 17:49:09','',NULL,''),(2109,'积分记录查询',2082,1,'','',NULL,'',1,0,'F','0','0','member:pointsRecord:query','#','admin','2026-06-03 17:49:09','',NULL,''),(2110,'积分记录新增',2082,2,'','',NULL,'',1,0,'F','0','0','member:pointsRecord:add','#','admin','2026-06-03 17:49:09','',NULL,''),(2111,'积分记录修改',2082,3,'','',NULL,'',1,0,'F','0','0','member:pointsRecord:edit','#','admin','2026-06-03 17:49:09','',NULL,''),(2112,'积分记录删除',2082,4,'','',NULL,'',1,0,'F','0','0','member:pointsRecord:remove','#','admin','2026-06-03 17:49:09','',NULL,''),(2113,'积分记录导出',2082,5,'','',NULL,'',1,0,'F','0','0','member:pointsRecord:export','#','admin','2026-06-03 17:49:09','',NULL,''),(2114,'积分兑换查询',2083,1,'','',NULL,'',1,0,'F','0','0','member:pointsExchange:query','#','admin','2026-06-03 17:49:09','',NULL,''),(2115,'积分兑换新增',2083,2,'','',NULL,'',1,0,'F','0','0','member:pointsExchange:add','#','admin','2026-06-03 17:49:09','',NULL,''),(2116,'积分兑换修改',2083,3,'','',NULL,'',1,0,'F','0','0','member:pointsExchange:edit','#','admin','2026-06-03 17:49:09','',NULL,''),(2117,'积分兑换删除',2083,4,'','',NULL,'',1,0,'F','0','0','member:pointsExchange:remove','#','admin','2026-06-03 17:49:09','',NULL,''),(2118,'积分兑换导出',2083,5,'','',NULL,'',1,0,'F','0','0','member:pointsExchange:export','#','admin','2026-06-03 17:49:09','',NULL,''),(2119,'小程序权限',1,10,'mpPerm','member/mpPerm/index',NULL,'',1,0,'C','0','0','member:mpPerm:list','phone','admin','2026-06-05 17:49:11','',NULL,'小程序角色模块权限配置'),(2120,'权限查询',2119,1,'','',NULL,'',1,0,'F','0','0','member:mpPerm:list','#','admin','2026-06-05 17:49:11','',NULL,''),(2121,'权限配置',2119,2,'','',NULL,'',1,0,'F','0','0','member:mpPerm:add','#','admin','2026-06-05 17:49:11','',NULL,''),(2122,'权限删除',2119,3,'','',NULL,'',1,0,'F','0','0','member:mpPerm:remove','#','admin','2026-06-05 17:49:11','',NULL,''),(2123,'核算周期',2000,9,'accountingPeriod','finance/accountingPeriod/index',NULL,'',1,0,'C','0','0','finance:accountingPeriod:list','chart','admin','2026-06-06 06:24:08','',NULL,'核算周期与回本状态菜单'),(2124,'周期查询',2123,1,'','',NULL,'',1,0,'F','0','0','finance:accountingPeriod:query','#','admin','2026-06-06 06:24:08','',NULL,''),(2125,'周期新增',2123,2,'','',NULL,'',1,0,'F','0','0','finance:accountingPeriod:add','#','admin','2026-06-06 06:24:08','',NULL,''),(2126,'周期修改',2123,3,'','',NULL,'',1,0,'F','0','0','finance:accountingPeriod:edit','#','admin','2026-06-06 06:24:08','',NULL,''),(2127,'周期删除',2123,4,'','',NULL,'',1,0,'F','0','0','finance:accountingPeriod:remove','#','admin','2026-06-06 06:24:08','',NULL,''),(2128,'分润结转',2123,5,'','',NULL,'',1,0,'F','0','0','finance:profitShare:add','#','admin','2026-06-06 06:24:08','',NULL,''),(2129,'分润配置',2000,10,'deptProfitConfig','finance/deptProfitConfig/index',NULL,'',1,0,'C','0','0','finance:deptProfitConfig:list','edit','admin','2026-06-06 06:24:37','',NULL,'店面分润配置菜单'),(2130,'配置查询',2129,1,'','',NULL,'',1,0,'F','0','0','finance:deptProfitConfig:query','#','admin','2026-06-06 06:24:37','',NULL,''),(2131,'配置新增',2129,2,'','',NULL,'',1,0,'F','0','0','finance:deptProfitConfig:add','#','admin','2026-06-06 06:24:37','',NULL,''),(2132,'配置修改',2129,3,'','',NULL,'',1,0,'F','0','0','finance:deptProfitConfig:edit','#','admin','2026-06-06 06:24:37','',NULL,''),(2133,'配置删除',2129,4,'','',NULL,'',1,0,'F','0','0','finance:deptProfitConfig:remove','#','admin','2026-06-06 06:24:37','',NULL,''),(2134,'分润记录',2000,11,'profitShare','finance/profitShare/index',NULL,'',1,0,'C','0','0','finance:profitShare:list','money','admin','2026-06-06 06:24:37','',NULL,'分润记录菜单'),(2135,'分润查询',2134,1,'','',NULL,'',1,0,'F','0','0','finance:profitShare:query','#','admin','2026-06-06 06:24:37','',NULL,''),(2136,'分润结转',2134,2,'','',NULL,'',1,0,'F','0','0','finance:profitShare:add','#','admin','2026-06-06 06:24:37','',NULL,''),(2137,'分润修改',2134,3,'','',NULL,'',1,0,'F','0','0','finance:profitShare:edit','#','admin','2026-06-06 06:24:37','',NULL,''),(2138,'分润作废',2134,4,'','',NULL,'',1,0,'F','0','0','finance:profitShare:remove','#','admin','2026-06-06 06:24:37','',NULL,''),(2139,'投资人',2000,12,'investor','finance/investor/index',NULL,'',1,0,'C','0','0','finance:investor:list','peoples','admin','2026-06-06 06:25:01','',NULL,'投资人菜单'),(2140,'投资人查询',2139,1,'','',NULL,'',1,0,'F','0','0','finance:investor:query','#','admin','2026-06-06 06:25:01','',NULL,''),(2141,'投资人新增',2139,2,'','',NULL,'',1,0,'F','0','0','finance:investor:add','#','admin','2026-06-06 06:25:01','',NULL,''),(2142,'投资人修改',2139,3,'','',NULL,'',1,0,'F','0','0','finance:investor:edit','#','admin','2026-06-06 06:25:01','',NULL,''),(2143,'投资人删除',2139,4,'','',NULL,'',1,0,'F','0','0','finance:investor:remove','#','admin','2026-06-06 06:25:01','',NULL,''),(2144,'投资来源记录',2000,13,'investRecord','finance/investRecord/index',NULL,'',1,0,'C','0','0','finance:investRecord:list','money','admin','2026-06-06 06:25:01','',NULL,'投资来源记录菜单'),(2145,'投资来源查询',2144,1,'','',NULL,'',1,0,'F','0','0','finance:investRecord:query','#','admin','2026-06-06 06:25:01','',NULL,''),(2146,'投资来源新增',2144,2,'','',NULL,'',1,0,'F','0','0','finance:investRecord:add','#','admin','2026-06-06 06:25:01','',NULL,''),(2147,'投资来源修改',2144,3,'','',NULL,'',1,0,'F','0','0','finance:investRecord:edit','#','admin','2026-06-06 06:25:01','',NULL,''),(2148,'投资来源删除',2144,4,'','',NULL,'',1,0,'F','0','0','finance:investRecord:remove','#','admin','2026-06-06 06:25:01','',NULL,''),(2150,'费用分析报表',2000,10,'expenseReport','finance/report/expense','','',0,0,'C','0','0','finance:report:expense','chart','admin','2026-06-07 21:49:10','',NULL,'费用分析报表菜单'),(2151,'成本分析报表',2000,11,'costReport','finance/report/cost','','',0,0,'C','0','0','finance:report:cost','chart','admin','2026-06-07 21:49:10','',NULL,'成本分析报表菜单'),(2152,'分润报表',2000,12,'profitShareReport','finance/report/profitShare','','',0,0,'C','0','0','finance:report:profitShare','money','admin','2026-06-07 21:49:10','',NULL,'分润报表菜单'),(2153,'销售报表',2000,13,'saleReport','finance/report/sale','','',0,0,'C','0','0','finance:report:sale','shopping','admin','2026-06-07 21:49:10','',NULL,'销售报表菜单'),(2154,'利润报表',2000,14,'profitReport','finance/report/profit','','',0,0,'C','0','0','finance:report:profit','money','admin','2026-06-07 21:49:10','',NULL,'利润报表菜单'),(2155,'库存报表',2000,15,'stockReport','finance/report/stock','','',0,0,'C','0','0','finance:report:stock','shopping','admin','2026-06-07 21:49:10','',NULL,'库存报表菜单'),(2156,'秒杀团购活动汇总',2076,8,'seckillReport','member/report/seckill','','',0,0,'C','0','0','member:report:seckill','list','admin','2026-06-07 21:49:10','',NULL,'秒杀团购活动汇总报表菜单'),(2157,'会员分析报表',2076,9,'memberReport','member/report/member','','',0,0,'C','0','0','member:report:member','chart','admin','2026-06-07 21:49:10','',NULL,'会员分析报表菜单'),(2158,'ç§’æ€è®°å½•é¢†å–',2079,6,'','',NULL,'',1,0,'F','0','0','member:seckillRecord:receive','#','admin','2026-06-12 21:58:44','',NULL,'');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notice`
--

DROP TABLE IF EXISTS `sys_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice` (
  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob COMMENT '公告内容',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notice`
--

LOCK TABLES `sys_notice` WRITE;
/*!40000 ALTER TABLE `sys_notice` DISABLE KEYS */;
INSERT INTO `sys_notice` VALUES (1,'温馨提醒：2018-07-01 若依新版本发布啦','2',0xE696B0E78988E69CACE58685E5AEB9,'0','admin','2026-06-01 16:53:29','',NULL,'管理员'),(2,'维护通知：2018-07-01 若依系统凌晨维护','1',0xE7BBB4E68AA4E58685E5AEB9,'0','admin','2026-06-01 16:53:29','',NULL,'管理员'),(3,'若依开源框架介绍','1',0x3C703E3C7370616E207374796C653D22636F6C6F723A20726762283233302C20302C2030293B223EE9A1B9E79BAEE4BB8BE7BB8D3C2F7370616E3E3C2F703E3C703E3C666F6E7420636F6C6F723D2223333333333333223E52756F5969E5BC80E6BA90E9A1B9E79BAEE698AFE4B8BAE4BC81E4B89AE794A8E688B7E5AE9AE588B6E79A84E5908EE58FB0E8849AE6898BE69EB6E6A186E69EB6EFBC8CE4B8BAE4BC81E4B89AE68993E980A0E79A84E4B880E7AB99E5BC8FE8A7A3E586B3E696B9E6A188EFBC8CE9998DE4BD8EE4BC81E4B89AE5BC80E58F91E68890E69CACEFBC8CE68F90E58D87E5BC80E58F91E69588E78E87E38082E4B8BBE8A681E58C85E68BACE794A8E688B7E7AEA1E79086E38081E8A792E889B2E7AEA1E79086E38081E983A8E997A8E7AEA1E79086E38081E88F9CE58D95E7AEA1E79086E38081E58F82E695B0E7AEA1E79086E38081E5AD97E585B8E7AEA1E79086E380813C2F666F6E743E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE5B297E4BD8DE7AEA1E790863C2F7370616E3E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE38081E5AE9AE697B6E4BBBBE58AA13C2F7370616E3E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE380813C2F7370616E3E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE69C8DE58AA1E79B91E68EA7E38081E799BBE5BD95E697A5E5BF97E38081E6938DE4BD9CE697A5E5BF97E38081E4BBA3E7A081E7949FE68890E7AD89E58A9FE883BDE38082E585B6E4B8ADEFBC8CE8BF98E694AFE68C81E5A49AE695B0E68DAEE6BA90E38081E695B0E68DAEE69D83E99990E38081E59BBDE99985E58C96E380815265646973E7BC93E5AD98E38081446F636B6572E983A8E7BDB2E38081E6BB91E58AA8E9AA8CE8AF81E7A081E38081E7ACACE4B889E696B9E8AEA4E8AF81E799BBE5BD95E38081E58886E5B883E5BC8FE4BA8BE58AA1E380813C2F7370616E3E3C666F6E7420636F6C6F723D2223333333333333223EE58886E5B883E5BC8FE69687E4BBB6E5AD98E582A83C2F666F6E743E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE38081E58886E5BA93E58886E8A1A8E5A484E79086E7AD89E68A80E69CAFE789B9E782B9E380823C2F7370616E3E3C2F703E3C703E3C696D67207372633D2268747470733A2F2F666F727564612E67697465652E636F6D2F696D616765732F313737333933313834383334323433393033322F61346432323331335F313831353039352E706E6722207374796C653D2277696474683A20363470783B223E3C62723E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A20726762283233302C20302C2030293B223EE5AE98E7BD91E58F8AE6BC94E7A4BA3C2F7370616E3E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE88BA5E4BE9DE5AE98E7BD91E59CB0E59D80EFBC9A266E6273703B3C2F7370616E3E3C6120687265663D22687474703A2F2F72756F79692E76697022207461726765743D225F626C616E6B223E687474703A2F2F72756F79692E7669703C2F613E3C6120687265663D22687474703A2F2F72756F79692E76697022207461726765743D225F626C616E6B223E3C2F613E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE88BA5E4BE9DE69687E6A1A3E59CB0E59D80EFBC9A266E6273703B3C2F7370616E3E3C6120687265663D22687474703A2F2F646F632E72756F79692E76697022207461726765743D225F626C616E6B223E687474703A2F2F646F632E72756F79692E7669703C2F613E3C62723E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE6BC94E7A4BAE59CB0E59D80E38090E4B88DE58886E7A6BBE78988E38091EFBC9A266E6273703B3C2F7370616E3E3C6120687265663D22687474703A2F2F64656D6F2E72756F79692E76697022207461726765743D225F626C616E6B223E687474703A2F2F64656D6F2E72756F79692E7669703C2F613E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE6BC94E7A4BAE59CB0E59D80E38090E58886E7A6BBE78988E69CACE38091EFBC9A266E6273703B3C2F7370616E3E3C6120687265663D22687474703A2F2F7675652E72756F79692E76697022207461726765743D225F626C616E6B223E687474703A2F2F7675652E72756F79692E7669703C2F613E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE6BC94E7A4BAE59CB0E59D80E38090E5BEAEE69C8DE58AA1E78988E38091EFBC9A266E6273703B3C2F7370616E3E3C6120687265663D22687474703A2F2F636C6F75642E72756F79692E76697022207461726765743D225F626C616E6B223E687474703A2F2F636C6F75642E72756F79692E7669703C2F613E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE6BC94E7A4BAE59CB0E59D80E38090E7A7BBE58AA8E7ABAFE78988E38091EFBC9A266E6273703B3C2F7370616E3E3C6120687265663D22687474703A2F2F68352E72756F79692E76697022207461726765743D225F626C616E6B223E687474703A2F2F68352E72756F79692E7669703C2F613E3C2F703E3C703E3C6272207374796C653D22636F6C6F723A207267622834382C2034392C203531293B20666F6E742D66616D696C793A202671756F743B48656C766574696361204E6575652671756F743B2C2048656C7665746963612C20417269616C2C2073616E732D73657269663B20666F6E742D73697A653A20313270783B223E3C2F703E,'0','admin','2026-06-01 16:53:29','',NULL,'管理员');
/*!40000 ALTER TABLE `sys_notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notice_read`
--

DROP TABLE IF EXISTS `sys_notice_read`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice_read` (
  `read_id` bigint NOT NULL AUTO_INCREMENT COMMENT '已读主键',
  `notice_id` int NOT NULL COMMENT '公告id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `read_time` datetime NOT NULL COMMENT '阅读时间',
  PRIMARY KEY (`read_id`),
  UNIQUE KEY `uk_user_notice` (`user_id`,`notice_id`) COMMENT '同一用户同一公告只记录一次'
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告已读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notice_read`
--

LOCK TABLES `sys_notice_read` WRITE;
/*!40000 ALTER TABLE `sys_notice_read` DISABLE KEYS */;
INSERT INTO `sys_notice_read` VALUES (1,3,1,'2026-06-02 12:33:13'),(2,2,1,'2026-06-02 12:33:13'),(3,1,1,'2026-06-02 12:33:13');
/*!40000 ALTER TABLE `sys_notice_read` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB AUTO_INCREMENT=121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
INSERT INTO `sys_oper_log` VALUES (100,'会员信息',1,'com.junsong.member.controller.MemMemberController.add()','POST',1,'admin',NULL,'/member/member','192.168.65.1','','{\"age\":67,\"availablePoints\":0,\"cardType\":\"1\",\"createBy\":\"admin\",\"expireDate\":\"2027-10-01\",\"joinDate\":\"2026-06-02\",\"memberId\":1,\"memberName\":\"王桂英\",\"memberNo\":\"RY0001\",\"params\":{},\"phone\":\"18518181136\",\"status\":\"0\",\"totalPoints\":0} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-06-02 12:24:12',96),(101,'秒杀活动',1,'com.junsong.member.controller.MemSeckillController.add()','POST',1,'admin',NULL,'/member/seckill','192.168.65.1','','{\"params\":{},\"seckillAmount\":2,\"seckillDate\":\"2026-06-02\",\"seckillName\":\"黄瓜秒杀260602\",\"seckillPrice\":9.9,\"seckillType\":\"1\",\"status\":\"0\",\"totalShares\":300} ',NULL,1,'class java.lang.Integer cannot be cast to class com.junsong.member.domain.MemSeckill (java.lang.Integer is in module java.base of loader \'bootstrap\'; com.junsong.member.domain.MemSeckill is in unnamed module of loader org.springframework.boot.loader.launch.LaunchedClassLoader @28a418fc)','2026-06-02 12:25:18',21),(102,'秒杀活动',1,'com.junsong.member.controller.MemSeckillController.add()','POST',1,'admin',NULL,'/member/seckill','192.168.65.1','','{\"params\":{},\"seckillAmount\":2,\"seckillDate\":\"2026-06-02\",\"seckillName\":\"黄瓜秒杀260602\",\"seckillPrice\":9.9,\"seckillType\":\"1\",\"status\":\"0\",\"totalShares\":300} ',NULL,1,'class java.lang.Integer cannot be cast to class com.junsong.member.domain.MemSeckill (java.lang.Integer is in module java.base of loader \'bootstrap\'; com.junsong.member.domain.MemSeckill is in unnamed module of loader org.springframework.boot.loader.launch.LaunchedClassLoader @28a418fc)','2026-06-02 12:25:27',3),(103,'个人信息',2,'com.junsong.system.controller.SysProfileController.updateProfile()','PUT',1,'admin',NULL,'/user/profile','192.168.65.1','','{\"admin\":false,\"email\":\"ry@163.com\",\"nickName\":\"峻松\",\"params\":{},\"phonenumber\":\"15888888888\",\"sex\":\"1\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-06-02 12:33:26',31),(104,'用户头像',2,'com.junsong.system.controller.SysProfileController.avatar()','POST',1,'admin',NULL,'/user/profile/avatar','192.168.65.1','','',NULL,1,'[503] during [POST] to [http://junsong-file/upload] [RemoteFileService#upload(MultipartFile)]: [Load balancer does not contain an instance for the service junsong-file]','2026-06-02 12:33:41',82),(105,'用户管理',1,'com.junsong.system.controller.SysUserController.add()','POST',1,'admin',NULL,'/user','192.168.65.1','','{\"admin\":false,\"createBy\":\"admin\",\"deptId\":100,\"nickName\":\"魂牵梦萦\",\"params\":{},\"postIds\":[],\"roleIds\":[],\"status\":\"0\",\"userId\":100,\"userName\":\"admini4\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-06-02 12:38:44',163),(106,'用户管理',2,'com.junsong.system.controller.SysUserController.edit()','PUT',1,'admin',NULL,'/user','192.168.65.1','','{\"admin\":false,\"avatar\":\"\",\"createBy\":\"admin\",\"createTime\":\"2026-06-02 04:38:44\",\"delFlag\":\"0\",\"dept\":{\"ancestors\":\"0\",\"children\":[],\"deptId\":100,\"deptName\":\"若依科技\",\"leader\":\"若依\",\"orderNum\":0,\"params\":{},\"parentId\":0,\"status\":\"0\"},\"email\":\"\",\"loginIp\":\"\",\"nickName\":\"魂牵梦萦\",\"params\":{},\"phonenumber\":\"\",\"postIds\":[],\"roleIds\":[],\"roles\":[],\"sex\":\"0\",\"status\":\"0\",\"updateBy\":\"admin\",\"userId\":100,\"userName\":\"admini4\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-06-02 12:38:50',15),(107,'用户管理',2,'com.junsong.system.controller.SysUserController.edit()','PUT',1,'admin',NULL,'/user','192.168.65.1','','{\"admin\":false,\"avatar\":\"\",\"createBy\":\"admin\",\"createTime\":\"2026-06-02 04:38:44\",\"delFlag\":\"0\",\"deptId\":102,\"email\":\"\",\"loginIp\":\"\",\"nickName\":\"魂牵梦萦\",\"params\":{},\"phonenumber\":\"\",\"postIds\":[],\"roleIds\":[],\"roles\":[],\"sex\":\"0\",\"status\":\"0\",\"updateBy\":\"admin\",\"updateTime\":\"2026-06-02 04:38:50\",\"userId\":100,\"userName\":\"admini4\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-06-02 12:39:15',13),(108,'秒杀活动',1,'com.junsong.member.controller.MemSeckillController.add()','POST',1,'admin',NULL,'/member/seckill','192.168.65.1','','{\"createBy\":\"admin\",\"params\":{},\"seckillAmount\":2,\"seckillDate\":\"2026-06-02\",\"seckillName\":\"huangguo0601\",\"seckillPrice\":4.9,\"seckillType\":\"1\",\"status\":\"0\",\"totalShares\":300} ',NULL,1,'\n### Error updating database.  Cause: java.sql.SQLException: Field \'seckill_no\' doesn\'t have a default value\n### The error may exist in class path resource [mapper/member/MemSeckillMapper.xml]\n### The error may involve com.junsong.member.mapper.MemSeckillMapper.insertMemSeckill-Inline\n### The error occurred while setting parameters\n### SQL: insert into mem_seckill          ( seckill_name,             seckill_type,             seckill_date,                          seckill_amount,             seckill_price,             total_shares,                                       status,                          create_by,             create_time )           values ( ?,             ?,             ?,                          ?,             ?,             ?,                                       ?,                          ?,             sysdate() )\n### Cause: java.sql.SQLException: Field \'seckill_no\' doesn\'t have a default value\n; Field \'seckill_no\' doesn\'t have a default value','2026-06-02 12:49:33',275),(109,'会员信息',2,'com.junsong.member.controller.MemMemberController.edit()','PUT',1,'admin',NULL,'/member/member','192.168.65.1','','{\"age\":67,\"availablePoints\":0,\"cardType\":\"experience\",\"createBy\":\"admin\",\"createTime\":\"2026-06-02 04:24:12\",\"delFlag\":\"0\",\"expireDate\":\"2027-10-01\",\"joinDate\":\"2026-06-02\",\"memberId\":1,\"memberName\":\"王桂英\",\"memberNo\":\"RY0001\",\"params\":{},\"phone\":\"18518181136\",\"status\":\"0\",\"totalPoints\":0,\"updateBy\":\"\"} ','{\"msg\":\"修改会员信息失败，编号已存在\",\"code\":500}',0,NULL,'2026-06-02 15:23:27',55),(110,'会员信息',1,'com.junsong.member.controller.MemMemberController.add()','POST',1,'admin',NULL,'/member/member','192.168.65.1','','{\"availablePoints\":0,\"cardType\":\"experience\",\"createBy\":\"admin\",\"joinDate\":\"2026-07-01\",\"memberId\":2,\"memberName\":\"FDSA \",\"memberNo\":\"R#0001\",\"params\":{},\"phone\":\"13800009999\",\"status\":\"0\",\"totalPoints\":0} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-06-02 15:24:07',40),(111,'会员信息',2,'com.junsong.member.controller.MemMemberController.edit()','PUT',1,'admin',NULL,'/member/member','192.168.65.1','','{\"age\":0,\"availablePoints\":0,\"cardType\":\"formal\",\"createBy\":\"admin\",\"createTime\":\"2026-06-02 07:24:07\",\"delFlag\":\"0\",\"joinDate\":\"2026-07-01\",\"memberId\":2,\"memberName\":\"FDSA \",\"memberNo\":\"R#0001\",\"params\":{},\"phone\":\"13800009999\",\"status\":\"0\",\"totalPoints\":0,\"updateBy\":\"\"} ','{\"msg\":\"修改会员信息失败，编号已存在\",\"code\":500}',0,NULL,'2026-06-02 15:24:21',3),(112,'用户管理',1,'com.junsong.system.controller.SysUserController.add()','POST',1,'admin',NULL,'/user','192.168.65.1','','{\"admin\":false,\"createBy\":\"admin\",\"nickName\":\"SD\",\"params\":{},\"postIds\":[],\"roleIds\":[],\"status\":\"0\",\"userId\":101,\"userName\":\"SD\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-06-02 15:25:07',158),(113,'角色管理',4,'com.junsong.system.controller.SysRoleController.selectAuthUserAll()','PUT',1,'admin',NULL,'/role/authUser/selectAll','192.168.65.1','','{\"roleId\":\"2\",\"userIds\":\"101\"}','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-06-02 15:30:36',7),(114,'用户管理',2,'com.junsong.system.controller.SysUserController.resetPwd()','PUT',1,'admin',NULL,'/user/resetPwd','192.168.65.1','','{\"admin\":false,\"params\":{},\"updateBy\":\"admin\",\"userId\":101} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-06-02 16:13:49',164),(115,'秒杀活动',1,'com.junsong.member.controller.MemSeckillController.add()','POST',1,'admin',NULL,'/member/seckill','192.168.65.1','','{\"createBy\":\"admin\",\"params\":{},\"seckillAmount\":1,\"seckillDate\":\"2026-06-02\",\"seckillName\":\"huanggua\",\"seckillPrice\":4.9,\"seckillType\":\"1\",\"status\":\"0\",\"totalShares\":300} ',NULL,1,'\n### Error updating database.  Cause: java.sql.SQLException: Field \'seckill_no\' doesn\'t have a default value\n### The error may exist in class path resource [mapper/member/MemSeckillMapper.xml]\n### The error may involve com.junsong.member.mapper.MemSeckillMapper.insertMemSeckill-Inline\n### The error occurred while setting parameters\n### SQL: insert into mem_seckill          ( seckill_name,             seckill_type,             seckill_date,                          seckill_amount,             seckill_price,             total_shares,                                       status,                          create_by,             create_time )           values ( ?,             ?,             ?,                          ?,             ?,             ?,                                       ?,                          ?,             sysdate() )\n### Cause: java.sql.SQLException: Field \'seckill_no\' doesn\'t have a default value\n; Field \'seckill_no\' doesn\'t have a default value','2026-06-02 18:37:23',447),(116,'会员信息',2,'com.junsong.member.controller.MemMemberController.edit()','PUT',1,'admin',NULL,'/member/member','192.168.65.1','','{\"age\":67,\"availablePoints\":0,\"cardType\":\"1\",\"createBy\":\"admin\",\"createTime\":\"2026-06-02 04:24:12\",\"delFlag\":\"0\",\"expireDate\":\"2027-10-01\",\"joinDate\":\"2026-06-02\",\"memberId\":1,\"memberName\":\"王桂英\",\"memberNo\":\"RY0001\",\"params\":{},\"phone\":\"18518181136\",\"status\":\"0\",\"totalPoints\":0,\"updateBy\":\"\"} ','{\"msg\":\"修改会员信息失败，编号已存在\",\"code\":500}',0,NULL,'2026-06-02 18:37:36',21),(117,'会员信息',2,'com.junsong.member.controller.MemMemberController.edit()','PUT',1,'admin',NULL,'/member/member','192.168.65.1','','{\"age\":0,\"availablePoints\":0,\"cardType\":\"experience\",\"createBy\":\"admin\",\"createTime\":\"2026-06-02 07:24:07\",\"delFlag\":\"0\",\"joinDate\":\"2026-07-01\",\"memberId\":2,\"memberName\":\"FDSA \",\"memberNo\":\"R#0001\",\"params\":{},\"phone\":\"13800009999\",\"status\":\"0\",\"totalPoints\":0,\"updateBy\":\"\"} ','{\"msg\":\"修改会员信息失败，编号已存在\",\"code\":500}',0,NULL,'2026-06-03 18:04:06',50),(118,'会员信息',2,'com.junsong.member.controller.MemMemberController.edit()','PUT',1,'admin',NULL,'/member/member','192.168.65.1','','{\"age\":0,\"availablePoints\":0,\"cardType\":\"experience\",\"createBy\":\"admin\",\"createTime\":\"2026-06-02 07:24:07\",\"delFlag\":\"0\",\"joinDate\":\"2026-07-01\",\"memberId\":2,\"memberName\":\"FDSA \",\"memberNo\":\"R#0001\",\"params\":{},\"phone\":\"13800009999\",\"status\":\"0\",\"totalPoints\":0,\"updateBy\":\"\"} ','{\"msg\":\"修改会员信息失败，编号已存在\",\"code\":500}',0,NULL,'2026-06-03 18:04:12',3),(119,'秒杀活动',1,'com.junsong.member.controller.MemSeckillController.add()','POST',1,'admin',NULL,'/member/seckill','192.168.65.1','','{\"createBy\":\"admin\",\"params\":{},\"seckillAmount\":2,\"seckillDate\":\"2026-06-03\",\"seckillName\":\"黄瓜0603\",\"seckillPrice\":9.9,\"seckillType\":\"1\",\"status\":\"0\",\"totalShares\":300} ',NULL,1,'\n### Error updating database.  Cause: java.sql.SQLException: Field \'seckill_no\' doesn\'t have a default value\n### The error may exist in class path resource [mapper/member/MemSeckillMapper.xml]\n### The error may involve com.junsong.member.mapper.MemSeckillMapper.insertMemSeckill-Inline\n### The error occurred while setting parameters\n### SQL: insert into mem_seckill          ( seckill_name,             seckill_type,             seckill_date,                          seckill_amount,             seckill_price,             total_shares,                                       status,                          create_by,             create_time )           values ( ?,             ?,             ?,                          ?,             ?,             ?,                                       ?,                          ?,             sysdate() )\n### Cause: java.sql.SQLException: Field \'seckill_no\' doesn\'t have a default value\n; Field \'seckill_no\' doesn\'t have a default value','2026-06-03 18:05:46',300),(120,'参数管理',2,'com.junsong.system.controller.SysConfigController.edit()','PUT',1,'admin',NULL,'/config','111.201.228.78','','{\"configId\":102,\"configKey\":\"sys.account.captchaEnabled\",\"configName\":\"登录验证码开关\",\"configType\":\"Y\",\"configValue\":\"false\",\"createBy\":\"admin\",\"createTime\":\"2026-06-13 22:05:29\",\"params\":{},\"remark\":\"是否开启登录验证码（true开启/false关闭）\",\"updateBy\":\"admin\"} ','{\"msg\":\"操作成功\",\"code\":200}',0,NULL,'2026-06-14 06:06:49',17);
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_post`
--

LOCK TABLES `sys_post` WRITE;
/*!40000 ALTER TABLE `sys_post` DISABLE KEYS */;
INSERT INTO `sys_post` VALUES (1,'ceo','董事长',1,'0','admin','2026-06-01 16:53:28','',NULL,''),(2,'se','项目经理',2,'0','admin','2026-06-01 16:53:28','',NULL,''),(3,'hr','人力资源',3,'0','admin','2026-06-01 16:53:28','',NULL,''),(4,'user','普通员工',4,'0','admin','2026-06-01 16:53:28','',NULL,'');
/*!40000 ALTER TABLE `sys_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'超级管理员','admin',1,'1',1,1,'0','0','admin','2026-06-01 16:53:28','',NULL,'超级管理员'),(2,'普通角色','common',2,'2',1,1,'0','0','admin','2026-06-01 16:53:28','',NULL,'普通角色');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_dept`
--

DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_dept`
--

LOCK TABLES `sys_role_dept` WRITE;
/*!40000 ALTER TABLE `sys_role_dept` DISABLE KEYS */;
INSERT INTO `sys_role_dept` VALUES (2,100),(2,101),(2,105);
/*!40000 ALTER TABLE `sys_role_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (1,2000),(1,2001),(1,2002),(1,2003),(1,2004),(1,2005),(1,2006),(1,2010),(1,2011),(1,2012),(1,2013),(1,2014),(1,2015),(1,2020),(1,2021),(1,2022),(1,2023),(1,2024),(1,2025),(1,2030),(1,2031),(1,2032),(1,2033),(1,2034),(1,2035),(1,2036),(1,2040),(1,2041),(1,2042),(1,2043),(1,2044),(1,2045),(1,2046),(1,2050),(1,2051),(1,2052),(1,2053),(1,2054),(1,2055),(1,2056),(1,2060),(1,2061),(1,2062),(1,2063),(1,2064),(1,2065),(1,2070),(1,2071),(1,2072),(1,2073),(1,2074),(1,2075),(1,2076),(1,2077),(1,2078),(1,2079),(1,2080),(1,2081),(1,2082),(1,2083),(1,2084),(1,2085),(1,2086),(1,2087),(1,2088),(1,2089),(1,2090),(1,2091),(1,2092),(1,2093),(1,2094),(1,2095),(1,2096),(1,2097),(1,2098),(1,2099),(1,2100),(1,2101),(1,2102),(1,2103),(1,2104),(1,2105),(1,2106),(1,2107),(1,2108),(1,2109),(1,2110),(1,2111),(1,2112),(1,2113),(1,2114),(1,2115),(1,2116),(1,2117),(1,2118),(1,2123),(1,2124),(1,2125),(1,2126),(1,2127),(1,2128),(1,2129),(1,2130),(1,2131),(1,2132),(1,2133),(1,2134),(1,2135),(1,2136),(1,2137),(1,2138),(1,2139),(1,2140),(1,2141),(1,2142),(1,2143),(1,2144),(1,2145),(1,2146),(1,2147),(1,2148),(1,2150),(1,2151),(1,2152),(1,2153),(1,2154),(1,2155),(1,2156),(1,2157),(2,1),(2,2),(2,3),(2,4),(2,100),(2,101),(2,102),(2,103),(2,104),(2,105),(2,106),(2,107),(2,108),(2,109),(2,110),(2,111),(2,112),(2,113),(2,114),(2,115),(2,116),(2,500),(2,501),(2,1000),(2,1001),(2,1002),(2,1003),(2,1004),(2,1005),(2,1006),(2,1007),(2,1008),(2,1009),(2,1010),(2,1011),(2,1012),(2,1013),(2,1014),(2,1015),(2,1016),(2,1017),(2,1018),(2,1019),(2,1020),(2,1021),(2,1022),(2,1023),(2,1024),(2,1025),(2,1026),(2,1027),(2,1028),(2,1029),(2,1030),(2,1031),(2,1032),(2,1033),(2,1034),(2,1035),(2,1036),(2,1037),(2,1038),(2,1039),(2,1040),(2,1041),(2,1042),(2,1043),(2,1044),(2,1045),(2,1046),(2,1047),(2,1048),(2,1049),(2,1050),(2,1051),(2,1052),(2,1053),(2,1054),(2,1055),(2,1056),(2,1057),(2,1058),(2,1059),(2,1060),(100,2000),(100,2001),(100,2002),(100,2003),(100,2004),(100,2005),(100,2006),(100,2010),(100,2011),(100,2012),(100,2013),(100,2014),(100,2015),(100,2020),(100,2021),(100,2022),(100,2023),(100,2024),(100,2025),(100,2030),(100,2031),(100,2032),(100,2033),(100,2034),(100,2035),(100,2036),(100,2040),(100,2041),(100,2042),(100,2043),(100,2044),(100,2045),(100,2046),(100,2050),(100,2051),(100,2052),(100,2053),(100,2054),(100,2055),(100,2056),(100,2060),(100,2061),(100,2062),(100,2063),(100,2064),(100,2065),(100,2070),(100,2071),(100,2072),(100,2073),(100,2074),(100,2075),(100,2076),(100,2077),(100,2078),(100,2079),(100,2080),(100,2081),(100,2082),(100,2083),(100,2084),(100,2085),(100,2086),(100,2087),(100,2088),(100,2089),(100,2090),(100,2091),(100,2092),(100,2093),(100,2094),(100,2095),(100,2096),(100,2097),(100,2098),(100,2099),(100,2100),(100,2101),(100,2102),(100,2103),(100,2104),(100,2105),(100,2106),(100,2107),(100,2108),(100,2109),(100,2110),(100,2111),(100,2112),(100,2113),(100,2114),(100,2115),(100,2116),(100,2117),(100,2118),(100,2123),(100,2124),(100,2125),(100,2126),(100,2128),(100,2134),(100,2135),(100,2136),(100,2137),(100,2138),(100,2139),(100,2140),(100,2144),(100,2145),(100,2146),(100,2147),(100,2148),(100,2150),(100,2151),(100,2152),(100,2153),(100,2154),(100,2155),(100,2156),(100,2157),(101,1),(101,100),(101,1000),(101,2000),(101,2001),(101,2002),(101,2003),(101,2004),(101,2005),(101,2006),(101,2020),(101,2021),(101,2025),(101,2030),(101,2031),(101,2036),(101,2040),(101,2041),(101,2042),(101,2043),(101,2044),(101,2046),(101,2050),(101,2051),(101,2060),(101,2061),(101,2076),(101,2077),(101,2078),(101,2079),(101,2080),(101,2081),(101,2082),(101,2083),(101,2084),(101,2085),(101,2086),(101,2087),(101,2088),(101,2089),(101,2090),(101,2091),(101,2092),(101,2093),(101,2094),(101,2095),(101,2096),(101,2097),(101,2098),(101,2099),(101,2100),(101,2101),(101,2102),(101,2103),(101,2104),(101,2105),(101,2106),(101,2107),(101,2108),(101,2109),(101,2110),(101,2111),(101,2112),(101,2113),(101,2114),(101,2115),(101,2116),(101,2117),(101,2118),(101,2119),(101,2120),(101,2121),(101,2122);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `pwd_update_date` datetime DEFAULT NULL COMMENT '密码最后更新时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,103,'admin','峻松','00','ry@163.com','15888888888','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','111.201.228.78','2026-06-14 10:33:56','2026-06-01 16:53:28','admin','2026-06-01 16:53:28','','2026-06-02 12:33:26','管理员'),(2,105,'ry','若依','00','ry@qq.com','15666666666','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1','2026-06-01 16:53:28','2026-06-01 16:53:28','admin','2026-06-01 16:53:28','',NULL,'测试员'),(100,102,'admini4','魂牵梦萦','00','','','0','','$2a$10$JDayE68FtQKnT5NvDvepO.tkkcLvJSRo35tz3R6FLH7LzIen1b8HC','0','0','',NULL,NULL,'admin','2026-06-02 12:38:44','admin','2026-06-02 12:39:15',NULL),(101,NULL,'SD','SD','00','','','0','','$2a$10$wCXAvT72GY0oHa2cPmCumOqNfJ4KVGBRhV4QhgzVDVD.dhgYecYC.','0','0','192.168.65.1','2026-06-02 16:14:12','2026-06-02 16:13:49','admin','2026-06-02 15:25:06','','2026-06-02 16:13:49',NULL);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_dept`
--

DROP TABLE IF EXISTS `sys_user_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_dept` (
  `user_dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dept_id` bigint NOT NULL COMMENT '部门/店面ID',
  `status` char(1) DEFAULT '0' COMMENT '状态（0在职 1离职',
  `hire_date` datetime DEFAULT NULL COMMENT '入职日期',
  `leave_date` datetime DEFAULT NULL COMMENT '离职日期',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`user_dept_id`),
  UNIQUE KEY `uk_user_dept` (`user_id`,`dept_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_dept`
--

LOCK TABLES `sys_user_dept` WRITE;
/*!40000 ALTER TABLE `sys_user_dept` DISABLE KEYS */;
INSERT INTO `sys_user_dept` VALUES (1,1,100,'0',NULL,NULL,'','2026-06-02 10:46:28','','2026-06-02 10:46:28','');
/*!40000 ALTER TABLE `sys_user_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_post`
--

LOCK TABLES `sys_user_post` WRITE;
/*!40000 ALTER TABLE `sys_user_post` DISABLE KEYS */;
INSERT INTO `sys_user_post` VALUES (1,1),(2,2);
/*!40000 ALTER TABLE `sys_user_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,1),(2,2),(101,2);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'junsong-cloud'
--

--
-- Dumping routines for database 'junsong-cloud'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-14  2:47:53
