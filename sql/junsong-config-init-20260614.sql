-- MySQL dump 10.13  Distrib 9.2.0, for Linux (x86_64)
--
-- Host: localhost    Database: junsong-config
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
-- Current Database: `junsong-config`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `junsong-config` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `junsong-config`;

--
-- Table structure for table `ai_chat_message`
--

DROP TABLE IF EXISTS `ai_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `namespace_id` varchar(128) NOT NULL DEFAULT '' COMMENT 'namespaceId',
  `session_id` varchar(128) NOT NULL COMMENT 'sessionId',
  `message_id` varchar(128) NOT NULL COMMENT 'messageId',
  `role` varchar(64) NOT NULL COMMENT 'role',
  `content` longtext NOT NULL COMMENT 'content',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmtCreate',
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmtModified',
  `ext_info` text COMMENT 'extInfo',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_namespace_sessionid_messageid` (`namespace_id`,`session_id`,`message_id`),
  KEY `idx_namespace_session` (`namespace_id`,`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ai_chat_message';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_message`
--

LOCK TABLES `ai_chat_message` WRITE;
/*!40000 ALTER TABLE `ai_chat_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_chat_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_session`
--

DROP TABLE IF EXISTS `ai_chat_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_session` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `namespace_id` varchar(128) NOT NULL DEFAULT '' COMMENT 'namespaceId',
  `session_id` varchar(128) NOT NULL COMMENT 'sessionId',
  `session_name` varchar(255) DEFAULT NULL COMMENT 'sessionName',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmtCreate',
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmtModified',
  `ext_info` text COMMENT 'extInfo',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_namespace_sessionid` (`namespace_id`,`session_id`),
  KEY `idx_namespace` (`namespace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ai_chat_session';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_session`
--

LOCK TABLES `ai_chat_session` WRITE;
/*!40000 ALTER TABLE `ai_chat_session` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_chat_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_resource`
--

DROP TABLE IF EXISTS `ai_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_resource` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `namespace_id` varchar(128) NOT NULL DEFAULT '' COMMENT 'namespaceId',
  `type` varchar(128) NOT NULL COMMENT 'type',
  `resource_id` varchar(128) NOT NULL COMMENT 'resourceId',
  `resource_name` varchar(255) DEFAULT NULL COMMENT 'resourceName',
  `config_detail` text COMMENT 'configDetail',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmtCreate',
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmtModified',
  `ext_info` text COMMENT 'extInfo',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_namespace_type_resourceid` (`namespace_id`,`type`,`resource_id`),
  KEY `idx_namespace` (`namespace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ai_resource';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_resource`
--

LOCK TABLES `ai_resource` WRITE;
/*!40000 ALTER TABLE `ai_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_info`
--

DROP TABLE IF EXISTS `config_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'group_id',
  `content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'content',
  `md5` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'md5',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `src_user` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT 'source user',
  `src_ip` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'source ip',
  `app_name` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'app_name',
  `tenant_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户字段',
  `c_desc` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'configuration description',
  `c_use` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'configuration usage',
  `effect` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '配置生效的描述',
  `type` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '配置的类型',
  `c_schema` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT '配置的模式',
  `encrypted_data_key` varchar(1024) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '' COMMENT '密钥',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfo_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=468 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='config_info';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_info`
--

LOCK TABLES `config_info` WRITE;
/*!40000 ALTER TABLE `config_info` DISABLE KEYS */;
INSERT INTO `config_info` VALUES (359,'junsong-auth-dev.yml','DEFAULT_GROUP','security:\n  oauth2:\n    client:\n      default-scopes: read,write\n      access-token-validity-seconds: 3600\n      refresh-token-validity-seconds: 86400','3092ad85b65a14263cd3dc1fe850cd79','2026-06-03 11:53:43','2026-06-03 11:53:43','nacos','127.0.0.1','','','认证服务配置',NULL,NULL,'yaml',NULL,''),(360,'junsong-system-dev.yml','DEFAULT_GROUP','file:\n  upload:\n    max-size: 10MB\n    allowed-types: jpg,jpeg,png,gif,doc,docx,pdf,xls,xlsx\n\nsystem:\n  user:\n    init-password: 123456\n    avatar-default: /statics/images/default-avatar.png','92eb862eb314f766d655df2b01e82ecf','2026-06-03 11:53:43','2026-06-03 11:53:43','nacos','127.0.0.1','','','系统服务配置',NULL,NULL,'yaml',NULL,''),(361,'junsong-finance-dev.yml','DEFAULT_GROUP','finance:\n  invoice:\n    max-amount: 1000000\n    tax-rate: 0.13\n  report:\n    export-format: pdf,xlsx\n    retention-days: 365','a47cc1e53468da4c20e7969759465d9f','2026-06-03 11:53:43','2026-06-03 11:53:43','nacos','127.0.0.1','','','财务服务配置',NULL,NULL,'yaml',NULL,''),(362,'junsong-member-dev.yml','DEFAULT_GROUP','member:\n  level:\n    max-level: 10\n    upgrade-points:\n      - 100\n      - 500\n      - 1000\n      - 5000\n      - 10000\n  points:\n    register: 50\n    daily-login: 10\n    purchase-ratio: 0.01','01a25b9c7b2852eb4ae75c803ba3a439','2026-06-03 11:53:43','2026-06-03 11:53:43','nacos','127.0.0.1','','','会员服务配置',NULL,NULL,'yaml',NULL,''),(363,'junsong-gen-dev.yml','DEFAULT_GROUP','springdoc:\n  gatewayUrl: http://localhost:8080/${spring.application.name}\n  api-docs:\n    enabled: true\n  info:\n    title: Code Generator API\n    description: Code generator API docs\n    contact:\n      name: JunSong\n      url: https://junsong.vip\n\ngen:\n  author: junsong\n  packageName: com.junsong.system\n  autoRemovePre: false\n  tablePrefix: sys_\n  allowOverwrite: false\n','c07e99bcb7978775f450b0eb00b6b505','2026-06-03 11:53:43','2026-06-13 21:54:01','nacos','127.0.0.1','','','代码生成服务配置',NULL,NULL,'yaml',NULL,''),(364,'junsong-job-dev.yml','DEFAULT_GROUP','springdoc:\n  gatewayUrl: http://localhost:8080/${spring.application.name}\n  api-docs:\n    enabled: true\n  info:\n    title: Job API\n    description: Scheduled job API docs\n    contact:\n      name: JunSong\n      url: https://junsong.vip\n','ca37875f8f8b0e63b05fda3e8496dafb','2026-06-03 11:53:43','2026-06-13 21:54:01','nacos','127.0.0.1','','','定时任务服务配置',NULL,NULL,'yaml',NULL,''),(365,'junsong-file-dev.yml','DEFAULT_GROUP','file:\n  domain: http://junsong-modules-file:9300\n  path: /home/junsong/uploadPath\n  prefix: /statics\n\nfdfs:\n  domain: http://junsong-modules-file\n  soTimeout: 3000\n  connectTimeout: 2000\n  trackerList: junsong-modules-file:22122\n\nminio:\n  url: http://127.0.0.1:9000\n  accessKey: change-me\n  secretKey: change-me\n  bucketName: test\n\nreferer:\n  enabled: false\n  allowed-domains: localhost,127.0.0.1,junsong.vip,www.junsong.vip','4c4f26f1d07fe670db2fa96cfa0217a3','2026-06-03 11:53:43','2026-06-03 11:53:43','nacos','127.0.0.1','','','文件服务配置',NULL,NULL,'yaml',NULL,''),(366,'junsong-monitor-dev.yml','DEFAULT_GROUP','spring:\n  security:\n    user:\n      name: junsong\n      password: 123456\n  boot:\n    admin:\n      ui:\n        title: 峻松服务状态监控','eebcb9a0c99bcf5f4edec5a20f90b792','2026-06-03 11:53:43','2026-06-03 11:53:43','nacos','127.0.0.1','','','监控服务配置',NULL,NULL,'yaml',NULL,''),(407,'application-dev.yml','DEFAULT_GROUP','spring:\n  autoconfigure:\n    exclude: com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure\n\n  servlet:\n    encoding:\n      charset: UTF-8\n      enabled: true\n      force: true\n      force-request: true\n      force-response: true\n\nfeign:\n  sentinel:\n    enabled: true\n  okhttp:\n    enabled: true\n  httpclient:\n    enabled: false\n  client:\n    config:\n      default:\n        connectTimeout: 10000\n        readTimeout: 10000\n  compression:\n    request:\n      enabled: true\n      min-request-size: 8192\n    response:\n      enabled: true\n\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: \'*\'\n  endpoint:\n    health:\n      show-details: always\n\nmybatis:\n  configuration:\n    map-underscore-to-camel-case: true\n\nlogging:\n  level:\n    com.junsong: debug\n    org.springframework: warn\n','f039516a4cddaaf828c13b35bca6f0dd','2026-06-03 12:05:05','2026-06-13 21:36:59','nacos_namespace_migrate','127.0.0.1','','public','公共配置',NULL,NULL,'yaml',NULL,''),(408,'junsong-gateway-dev.yml','DEFAULT_GROUP','spring:\n  cloud:\n    gateway:\n      server:\n        webflux:\n          discovery:\n            locator:\n              lowerCaseServiceId: true\n              enabled: true\n          routes:\n            - id: junsong-auth\n              uri: lb://junsong-auth\n              predicates:\n                - Path=/auth/**\n              filters:\n                - name: CacheRequestBody\n                  args:\n                    bodyClass: java.lang.String\n                - ValidateCodeFilter\n                - StripPrefix=1\n            - id: junsong-gen\n              uri: lb://junsong-gen\n              predicates:\n                - Path=/code/**\n              filters:\n                - StripPrefix=1\n            - id: junsong-job\n              uri: lb://junsong-job\n              predicates:\n                - Path=/schedule/**\n              filters:\n                - StripPrefix=1\n            - id: junsong-system\n              uri: lb://junsong-system\n              predicates:\n                - Path=/system/**\n              filters:\n                - StripPrefix=1\n            - id: junsong-file\n              uri: lb://junsong-file\n              predicates:\n                - Path=/file/**\n              filters:\n                - StripPrefix=1\n            - id: junsong-finance\n              uri: lb://junsong-finance\n              predicates:\n                - Path=/finance/**\n              filters:\n                - StripPrefix=1\n            - id: junsong-member\n              uri: lb://junsong-member\n              predicates:\n                - Path=/member/**\n              filters:\n                - StripPrefix=1\n    sentinel:\n      eager: true\n      transport:\n        dashboard: 127.0.0.1:8718\n      datasource:\n        ds1:\n          nacos:\n            server-addr: junsong-nacos:8848\n            dataId: sentinel-junsong-gateway\n            groupId: DEFAULT_GROUP\n            data-type: json\n            rule-type: gw-flow\n\nsecurity:\n  captcha:\n    enabled: true\n    type: math\n  xss:\n    enabled: true\n    excludeUrls:\n      - /system/notice\n  ignore:\n    whites:\n      - /auth/logout\n      - /auth/login\n      - /auth/register\n      - /*/v2/api-docs\n      - /*/v3/api-docs\n      - /csrf\n','c80e2efe559c6d2374569703fbbecb67','2026-06-03 12:05:05','2026-06-03 12:05:05','nacos_namespace_migrate','127.0.0.1','','public','网关路由配置',NULL,NULL,'yaml',NULL,''),(409,'junsong-auth-dev.yml','DEFAULT_GROUP','security:\n  oauth2:\n    client:\n      default-scopes: read,write\n      access-token-validity-seconds: 3600\n      refresh-token-validity-seconds: 86400','3092ad85b65a14263cd3dc1fe850cd79','2026-06-03 12:05:05','2026-06-03 12:05:05','nacos_namespace_migrate','127.0.0.1','','public','认证服务配置',NULL,NULL,'yaml',NULL,''),(410,'junsong-system-dev.yml','DEFAULT_GROUP','file:\n  upload:\n    max-size: 10MB\n    allowed-types: jpg,jpeg,png,gif,doc,docx,pdf,xls,xlsx\n\nsystem:\n  user:\n    init-password: 123456\n    avatar-default: /statics/images/default-avatar.png','92eb862eb314f766d655df2b01e82ecf','2026-06-03 12:05:05','2026-06-03 12:05:05','nacos_namespace_migrate','127.0.0.1','','public','系统服务配置',NULL,NULL,'yaml',NULL,''),(411,'junsong-finance-dev.yml','DEFAULT_GROUP','finance:\n  invoice:\n    max-amount: 1000000\n    tax-rate: 0.13\n  report:\n    export-format: pdf,xlsx\n    retention-days: 365','a47cc1e53468da4c20e7969759465d9f','2026-06-03 12:05:05','2026-06-03 12:05:05','nacos_namespace_migrate','127.0.0.1','','public','财务服务配置',NULL,NULL,'yaml',NULL,''),(412,'junsong-member-dev.yml','DEFAULT_GROUP','member:\n  level:\n    max-level: 10\n    upgrade-points:\n      - 100\n      - 500\n      - 1000\n      - 5000\n      - 10000\n  points:\n    register: 50\n    daily-login: 10\n    purchase-ratio: 0.01','01a25b9c7b2852eb4ae75c803ba3a439','2026-06-03 12:05:05','2026-06-03 12:05:05','nacos_namespace_migrate','127.0.0.1','','public','会员服务配置',NULL,NULL,'yaml',NULL,''),(413,'junsong-gen-dev.yml','DEFAULT_GROUP','springdoc:\n  gatewayUrl: http://localhost:8080/${spring.application.name}\n  api-docs:\n    enabled: true\n  info:\n    title: Code Generator API\n    description: Code generator API docs\n    contact:\n      name: JunSong\n      url: https://junsong.vip\n\ngen:\n  author: junsong\n  packageName: com.junsong.system\n  autoRemovePre: false\n  tablePrefix: sys_\n  allowOverwrite: false\n','c07e99bcb7978775f450b0eb00b6b505','2026-06-03 12:05:05','2026-06-13 21:54:01','nacos_namespace_migrate','127.0.0.1','','public','代码生成服务配置',NULL,NULL,'yaml',NULL,''),(414,'junsong-job-dev.yml','DEFAULT_GROUP','springdoc:\n  gatewayUrl: http://localhost:8080/${spring.application.name}\n  api-docs:\n    enabled: true\n  info:\n    title: Job API\n    description: Scheduled job API docs\n    contact:\n      name: JunSong\n      url: https://junsong.vip\n','ca37875f8f8b0e63b05fda3e8496dafb','2026-06-03 12:05:05','2026-06-13 21:54:01','nacos_namespace_migrate','127.0.0.1','','public','定时任务服务配置',NULL,NULL,'yaml',NULL,''),(415,'junsong-file-dev.yml','DEFAULT_GROUP','file:\n  domain: http://junsong-modules-file:9300\n  path: /home/junsong/uploadPath\n  prefix: /statics\n\nfdfs:\n  domain: http://junsong-modules-file\n  soTimeout: 3000\n  connectTimeout: 2000\n  trackerList: junsong-modules-file:22122\n\nminio:\n  url: http://127.0.0.1:9000\n  accessKey: change-me\n  secretKey: change-me\n  bucketName: test\n\nreferer:\n  enabled: false\n  allowed-domains: localhost,127.0.0.1,junsong.vip,www.junsong.vip','4c4f26f1d07fe670db2fa96cfa0217a3','2026-06-03 12:05:05','2026-06-03 12:05:05','nacos_namespace_migrate','127.0.0.1','','public','文件服务配置',NULL,NULL,'yaml',NULL,''),(416,'junsong-monitor-dev.yml','DEFAULT_GROUP','spring:\n  security:\n    user:\n      name: junsong\n      password: 123456\n  boot:\n    admin:\n      ui:\n        title: 峻松服务状态监控','eebcb9a0c99bcf5f4edec5a20f90b792','2026-06-03 12:05:05','2026-06-03 12:05:05','nacos_namespace_migrate','127.0.0.1','','public','监控服务配置',NULL,NULL,'yaml',NULL,''),(423,'junsong-auth-prod.yml','DEFAULT_GROUP','security:\n  oauth2:\n    client:\n      default-scopes: read,write\n      access-token-validity-seconds: 3600\n      refresh-token-validity-seconds: 86400','3092ad85b65a14263cd3dc1fe850cd79','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos','127.0.0.1','','','认证服务配置',NULL,NULL,'yaml',NULL,''),(424,'junsong-system-prod.yml','DEFAULT_GROUP','file:\n  upload:\n    max-size: 10MB\n    allowed-types: jpg,jpeg,png,gif,doc,docx,pdf,xls,xlsx\n\nsystem:\n  user:\n    init-password: 123456\n    avatar-default: /statics/images/default-avatar.png','92eb862eb314f766d655df2b01e82ecf','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos','127.0.0.1','','','系统服务配置',NULL,NULL,'yaml',NULL,''),(425,'junsong-finance-prod.yml','DEFAULT_GROUP','finance:\n  invoice:\n    max-amount: 1000000\n    tax-rate: 0.13\n  report:\n    export-format: pdf,xlsx\n    retention-days: 365','a47cc1e53468da4c20e7969759465d9f','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos','127.0.0.1','','','财务服务配置',NULL,NULL,'yaml',NULL,''),(426,'junsong-member-prod.yml','DEFAULT_GROUP','member:\n  level:\n    max-level: 10\n    upgrade-points:\n      - 100\n      - 500\n      - 1000\n      - 5000\n      - 10000\n  points:\n    register: 50\n    daily-login: 10\n    purchase-ratio: 0.01','01a25b9c7b2852eb4ae75c803ba3a439','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos','127.0.0.1','','','会员服务配置',NULL,NULL,'yaml',NULL,''),(427,'junsong-gen-prod.yml','DEFAULT_GROUP','springdoc:\n  gatewayUrl: http://localhost:8080/${spring.application.name}\n  api-docs:\n    enabled: true\n  info:\n    title: Code Generator API\n    description: Code generator API docs\n    contact:\n      name: JunSong\n      url: https://junsong.vip\n\ngen:\n  author: junsong\n  packageName: com.junsong.system\n  autoRemovePre: false\n  tablePrefix: sys_\n  allowOverwrite: false\n','c07e99bcb7978775f450b0eb00b6b505','2026-06-13 21:08:00','2026-06-13 21:54:01','nacos','127.0.0.1','','','代码生成服务配置',NULL,NULL,'yaml',NULL,''),(428,'junsong-job-prod.yml','DEFAULT_GROUP','springdoc:\n  gatewayUrl: http://localhost:8080/${spring.application.name}\n  api-docs:\n    enabled: true\n  info:\n    title: Job API\n    description: Scheduled job API docs\n    contact:\n      name: JunSong\n      url: https://junsong.vip\n','ca37875f8f8b0e63b05fda3e8496dafb','2026-06-13 21:08:00','2026-06-13 21:54:01','nacos','127.0.0.1','','','定时任务服务配置',NULL,NULL,'yaml',NULL,''),(429,'junsong-file-prod.yml','DEFAULT_GROUP','file:\n  domain: http://junsong-modules-file:9300\n  path: /home/junsong/uploadPath\n  prefix: /statics\n\nfdfs:\n  domain: http://junsong-modules-file\n  soTimeout: 3000\n  connectTimeout: 2000\n  trackerList: junsong-modules-file:22122\n\nminio:\n  url: http://127.0.0.1:9000\n  accessKey: change-me\n  secretKey: change-me\n  bucketName: test\n\nreferer:\n  enabled: false\n  allowed-domains: localhost,127.0.0.1,junsong.vip,www.junsong.vip','4c4f26f1d07fe670db2fa96cfa0217a3','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos','127.0.0.1','','','文件服务配置',NULL,NULL,'yaml',NULL,''),(430,'junsong-monitor-prod.yml','DEFAULT_GROUP','spring:\n  security:\n    user:\n      name: junsong\n      password: 123456\n  boot:\n    admin:\n      ui:\n        title: 峻松服务状态监控','eebcb9a0c99bcf5f4edec5a20f90b792','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos','127.0.0.1','','','监控服务配置',NULL,NULL,'yaml',NULL,''),(431,'application-prod.yml','DEFAULT_GROUP','spring:\n  autoconfigure:\n    exclude: com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure\n\n  servlet:\n    encoding:\n      charset: UTF-8\n      enabled: true\n      force: true\n      force-request: true\n      force-response: true\n\nfeign:\n  sentinel:\n    enabled: true\n  okhttp:\n    enabled: true\n  httpclient:\n    enabled: false\n  client:\n    config:\n      default:\n        connectTimeout: 10000\n        readTimeout: 10000\n  compression:\n    request:\n      enabled: true\n      min-request-size: 8192\n    response:\n      enabled: true\n\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: \'*\'\n  endpoint:\n    health:\n      show-details: always\n\nmybatis:\n  configuration:\n    map-underscore-to-camel-case: true\n\nlogging:\n  level:\n    com.junsong: debug\n    org.springframework: warn\n','f039516a4cddaaf828c13b35bca6f0dd','2026-06-13 21:08:00','2026-06-13 21:36:04','nacos_namespace_migrate','127.0.0.1','','public','公共配置',NULL,NULL,'yaml',NULL,''),(432,'junsong-gateway-prod.yml','DEFAULT_GROUP','spring:\n  cloud:\n    gateway:\n      server:\n        webflux:\n          discovery:\n            locator:\n              lowerCaseServiceId: true\n              enabled: true\n          routes:\n            - id: junsong-auth\n              uri: lb://junsong-auth\n              predicates:\n                - Path=/auth/**\n              filters:\n                - name: CacheRequestBody\n                  args:\n                    bodyClass: java.lang.String\n                - ValidateCodeFilter\n                - StripPrefix=1\n            - id: junsong-gen\n              uri: lb://junsong-gen\n              predicates:\n                - Path=/code/**\n              filters:\n                - StripPrefix=1\n            - id: junsong-job\n              uri: lb://junsong-job\n              predicates:\n                - Path=/schedule/**\n              filters:\n                - StripPrefix=1\n            - id: junsong-system\n              uri: lb://junsong-system\n              predicates:\n                - Path=/system/**\n              filters:\n                - StripPrefix=1\n            - id: junsong-file\n              uri: lb://junsong-file\n              predicates:\n                - Path=/file/**\n              filters:\n                - StripPrefix=1\n            - id: junsong-finance\n              uri: lb://junsong-finance\n              predicates:\n                - Path=/finance/**\n              filters:\n                - StripPrefix=1\n            - id: junsong-member\n              uri: lb://junsong-member\n              predicates:\n                - Path=/member/**\n              filters:\n                - StripPrefix=1\n    sentinel:\n      eager: true\n      transport:\n        dashboard: 127.0.0.1:8718\n      datasource:\n        ds1:\n          nacos:\n            server-addr: junsong-nacos:8848\n            dataId: sentinel-junsong-gateway\n            groupId: DEFAULT_GROUP\n            data-type: json\n            rule-type: gw-flow\n\nsecurity:\n  captcha:\n    enabled: true\n    type: math\n  xss:\n    enabled: true\n    excludeUrls:\n      - /system/notice\n  ignore:\n    whites:\n      - /auth/logout\n      - /auth/login\n      - /auth/register\n      - /*/v2/api-docs\n      - /*/v3/api-docs\n      - /csrf\n','c80e2efe559c6d2374569703fbbecb67','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos_namespace_migrate','127.0.0.1','','public','网关路由配置',NULL,NULL,'yaml',NULL,''),(433,'junsong-auth-prod.yml','DEFAULT_GROUP','security:\n  oauth2:\n    client:\n      default-scopes: read,write\n      access-token-validity-seconds: 3600\n      refresh-token-validity-seconds: 86400','3092ad85b65a14263cd3dc1fe850cd79','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos_namespace_migrate','127.0.0.1','','public','认证服务配置',NULL,NULL,'yaml',NULL,''),(434,'junsong-system-prod.yml','DEFAULT_GROUP','file:\n  upload:\n    max-size: 10MB\n    allowed-types: jpg,jpeg,png,gif,doc,docx,pdf,xls,xlsx\n\nsystem:\n  user:\n    init-password: 123456\n    avatar-default: /statics/images/default-avatar.png','92eb862eb314f766d655df2b01e82ecf','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos_namespace_migrate','127.0.0.1','','public','系统服务配置',NULL,NULL,'yaml',NULL,''),(435,'junsong-finance-prod.yml','DEFAULT_GROUP','finance:\n  invoice:\n    max-amount: 1000000\n    tax-rate: 0.13\n  report:\n    export-format: pdf,xlsx\n    retention-days: 365','a47cc1e53468da4c20e7969759465d9f','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos_namespace_migrate','127.0.0.1','','public','财务服务配置',NULL,NULL,'yaml',NULL,''),(436,'junsong-member-prod.yml','DEFAULT_GROUP','member:\n  level:\n    max-level: 10\n    upgrade-points:\n      - 100\n      - 500\n      - 1000\n      - 5000\n      - 10000\n  points:\n    register: 50\n    daily-login: 10\n    purchase-ratio: 0.01','01a25b9c7b2852eb4ae75c803ba3a439','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos_namespace_migrate','127.0.0.1','','public','会员服务配置',NULL,NULL,'yaml',NULL,''),(437,'junsong-gen-prod.yml','DEFAULT_GROUP','springdoc:\n  gatewayUrl: http://localhost:8080/${spring.application.name}\n  api-docs:\n    enabled: true\n  info:\n    title: Code Generator API\n    description: Code generator API docs\n    contact:\n      name: JunSong\n      url: https://junsong.vip\n\ngen:\n  author: junsong\n  packageName: com.junsong.system\n  autoRemovePre: false\n  tablePrefix: sys_\n  allowOverwrite: false\n','c07e99bcb7978775f450b0eb00b6b505','2026-06-13 21:08:00','2026-06-13 21:54:01','nacos_namespace_migrate','127.0.0.1','','public','代码生成服务配置',NULL,NULL,'yaml',NULL,''),(438,'junsong-job-prod.yml','DEFAULT_GROUP','springdoc:\n  gatewayUrl: http://localhost:8080/${spring.application.name}\n  api-docs:\n    enabled: true\n  info:\n    title: Job API\n    description: Scheduled job API docs\n    contact:\n      name: JunSong\n      url: https://junsong.vip\n','ca37875f8f8b0e63b05fda3e8496dafb','2026-06-13 21:08:00','2026-06-13 21:54:01','nacos_namespace_migrate','127.0.0.1','','public','定时任务服务配置',NULL,NULL,'yaml',NULL,''),(439,'junsong-file-prod.yml','DEFAULT_GROUP','file:\n  domain: http://junsong-modules-file:9300\n  path: /home/junsong/uploadPath\n  prefix: /statics\n\nfdfs:\n  domain: http://junsong-modules-file\n  soTimeout: 3000\n  connectTimeout: 2000\n  trackerList: junsong-modules-file:22122\n\nminio:\n  url: http://127.0.0.1:9000\n  accessKey: change-me\n  secretKey: change-me\n  bucketName: test\n\nreferer:\n  enabled: false\n  allowed-domains: localhost,127.0.0.1,junsong.vip,www.junsong.vip','4c4f26f1d07fe670db2fa96cfa0217a3','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos_namespace_migrate','127.0.0.1','','public','文件服务配置',NULL,NULL,'yaml',NULL,''),(440,'junsong-monitor-prod.yml','DEFAULT_GROUP','spring:\n  security:\n    user:\n      name: junsong\n      password: 123456\n  boot:\n    admin:\n      ui:\n        title: 峻松服务状态监控','eebcb9a0c99bcf5f4edec5a20f90b792','2026-06-13 21:08:00','2026-06-13 21:08:00','nacos_namespace_migrate','127.0.0.1','','public','监控服务配置',NULL,NULL,'yaml',NULL,''),(442,'redis-prod.yml','DEFAULT_GROUP','spring:\n  data:\n    redis:\n      host: junsong-redis\n      port: 6379\n      password:\n    \n      database: 0\n      timeout: 10s\n      lettuce:\n        pool:\n          min-idle: 0\n          max-idle: 8\n          max-active: 8\n          max-wait: -1ms\n','73a54fe9ffbb3fe543edf12a0ee5ff98','2026-06-13 21:36:04','2026-06-13 21:41:32','nacos','127.0.0.1','','public','','','','yaml','',''),(443,'datasource-druid-prod.yml','DEFAULT_GROUP','spring:\n  datasource:\n    druid:\n      stat-view-servlet:\n        enabled: true\n        loginUsername: junsong\n        loginPassword: change-me\n    dynamic:\n      primary: master\n      strict: false\n      druid:\n        initial-size: 5\n        min-idle: 5\n        maxActive: 20\n        maxWait: 60000\n        connectTimeout: 30000\n        socketTimeout: 60000\n        timeBetweenEvictionRunsMillis: 60000\n        minEvictableIdleTimeMillis: 300000\n        validationQuery: SELECT 1 FROM DUAL\n        testWhileIdle: true\n        testOnBorrow: false\n        testOnReturn: false\n        poolPreparedStatements: true\n        maxPoolPreparedStatementPerConnectionSize: 20\n        filters: stat,slf4j\n        connectionProperties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000\n      datasource:\n        master:\n          username: root\n          password: change-me-db-password\n          driver-class-name: com.mysql.cj.jdbc.Driver\n          url: jdbc:mysql://junsong-mysql:3306/junsong-cloud?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true\n','2f93bcdddbe615126d1bf25d29274413','2026-06-13 21:36:04','2026-06-13 21:43:50','nacos','127.0.0.1','','public','','','','yaml','',''),(444,'datasource-prod.yml','DEFAULT_GROUP','spring:\n  datasource:\n    driver-class-name: com.mysql.cj.jdbc.Driver\n    url: jdbc:mysql://junsong-mysql:3306/junsong-cloud?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8\n    username: root\n    password: change-me-db-password\n\nspringdoc:\n','d6f61d912820160ddd21fba126872028','2026-06-13 21:36:04','2026-06-13 21:36:04','nacos','127.0.0.1','','public','','','','yaml','',''),(448,'redis-dev.yml','DEFAULT_GROUP','spring:\n  data:\n    redis:\n      host: junsong-redis\n      port: 6379\n      password:\n    \n      database: 0\n      timeout: 10s\n      lettuce:\n        pool:\n          min-idle: 0\n          max-idle: 8\n          max-active: 8\n          max-wait: -1ms\n','73a54fe9ffbb3fe543edf12a0ee5ff98','2026-06-13 21:36:59','2026-06-13 21:41:32','nacos','127.0.0.1','','public','','','','yaml','',''),(449,'datasource-druid-dev.yml','DEFAULT_GROUP','spring:\n  datasource:\n    druid:\n      stat-view-servlet:\n        enabled: true\n        loginUsername: junsong\n        loginPassword: change-me\n    dynamic:\n      primary: master\n      strict: false\n      druid:\n        initial-size: 5\n        min-idle: 5\n        maxActive: 20\n        maxWait: 60000\n        connectTimeout: 30000\n        socketTimeout: 60000\n        timeBetweenEvictionRunsMillis: 60000\n        minEvictableIdleTimeMillis: 300000\n        validationQuery: SELECT 1 FROM DUAL\n        testWhileIdle: true\n        testOnBorrow: false\n        testOnReturn: false\n        poolPreparedStatements: true\n        maxPoolPreparedStatementPerConnectionSize: 20\n        filters: stat,slf4j\n        connectionProperties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000\n      datasource:\n        master:\n          username: root\n          password: change-me-db-password\n          driver-class-name: com.mysql.cj.jdbc.Driver\n          url: jdbc:mysql://junsong-mysql:3306/junsong-cloud?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true\n','2f93bcdddbe615126d1bf25d29274413','2026-06-13 21:36:59','2026-06-13 21:43:50','nacos','127.0.0.1','','public','','','','yaml','',''),(450,'datasource-dev.yml','DEFAULT_GROUP','spring:\n  datasource:\n    driver-class-name: com.mysql.cj.jdbc.Driver\n    url: jdbc:mysql://junsong-mysql:3306/junsong-cloud?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8\n    username: root\n    password: change-me-db-password\n\nspringdoc:\n','d6f61d912820160ddd21fba126872028','2026-06-13 21:36:59','2026-06-13 21:36:59','nacos','127.0.0.1','','public','','','','yaml','',''),(453,'application-dev.yml','DEFAULT_GROUP','spring:\n  autoconfigure:\n    exclude: com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure\n\n  servlet:\n    encoding:\n      charset: UTF-8\n      enabled: true\n      force: true\n      force-request: true\n      force-response: true\n\nfeign:\n  sentinel:\n    enabled: true\n  okhttp:\n    enabled: true\n  httpclient:\n    enabled: false\n  client:\n    config:\n      default:\n        connectTimeout: 10000\n        readTimeout: 10000\n  compression:\n    request:\n      enabled: true\n      min-request-size: 8192\n    response:\n      enabled: true\n\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: \'*\'\n  endpoint:\n    health:\n      show-details: always\n\nmybatis:\n  configuration:\n    map-underscore-to-camel-case: true\n\nlogging:\n  level:\n    com.junsong: debug\n    org.springframework: warn\n','f039516a4cddaaf828c13b35bca6f0dd','2026-06-13 21:40:09','2026-06-13 21:40:09','nacos_namespace_migrate','127.0.0.1','','','公共配置',NULL,NULL,'yaml',NULL,''),(454,'application-prod.yml','DEFAULT_GROUP','spring:\n  autoconfigure:\n    exclude: com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure\n\n  servlet:\n    encoding:\n      charset: UTF-8\n      enabled: true\n      force: true\n      force-request: true\n      force-response: true\n\nfeign:\n  sentinel:\n    enabled: true\n  okhttp:\n    enabled: true\n  httpclient:\n    enabled: false\n  client:\n    config:\n      default:\n        connectTimeout: 10000\n        readTimeout: 10000\n  compression:\n    request:\n      enabled: true\n      min-request-size: 8192\n    response:\n      enabled: true\n\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: \'*\'\n  endpoint:\n    health:\n      show-details: always\n\nmybatis:\n  configuration:\n    map-underscore-to-camel-case: true\n\nlogging:\n  level:\n    com.junsong: debug\n    org.springframework: warn\n','f039516a4cddaaf828c13b35bca6f0dd','2026-06-13 21:40:09','2026-06-13 21:40:09','nacos_namespace_migrate','127.0.0.1','','','公共配置',NULL,NULL,'yaml',NULL,''),(455,'datasource-dev.yml','DEFAULT_GROUP','spring:\n  datasource:\n    driver-class-name: com.mysql.cj.jdbc.Driver\n    url: jdbc:mysql://junsong-mysql:3306/junsong-cloud?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8\n    username: root\n    password: change-me-db-password\n\nspringdoc:\n','d6f61d912820160ddd21fba126872028','2026-06-13 21:40:09','2026-06-13 21:40:09','nacos','127.0.0.1','','','','','','yaml','',''),(456,'datasource-druid-dev.yml','DEFAULT_GROUP','spring:\n  datasource:\n    druid:\n      stat-view-servlet:\n        enabled: true\n        loginUsername: junsong\n        loginPassword: change-me\n    dynamic:\n      primary: master\n      strict: false\n      druid:\n        initial-size: 5\n        min-idle: 5\n        maxActive: 20\n        maxWait: 60000\n        connectTimeout: 30000\n        socketTimeout: 60000\n        timeBetweenEvictionRunsMillis: 60000\n        minEvictableIdleTimeMillis: 300000\n        validationQuery: SELECT 1 FROM DUAL\n        testWhileIdle: true\n        testOnBorrow: false\n        testOnReturn: false\n        poolPreparedStatements: true\n        maxPoolPreparedStatementPerConnectionSize: 20\n        filters: stat,slf4j\n        connectionProperties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000\n      datasource:\n        master:\n          username: root\n          password: change-me-db-password\n          driver-class-name: com.mysql.cj.jdbc.Driver\n          url: jdbc:mysql://junsong-mysql:3306/junsong-cloud?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true\n','2f93bcdddbe615126d1bf25d29274413','2026-06-13 21:40:09','2026-06-13 21:43:49','nacos','127.0.0.1','','','','','','yaml','',''),(457,'datasource-druid-prod.yml','DEFAULT_GROUP','spring:\n  datasource:\n    druid:\n      stat-view-servlet:\n        enabled: true\n        loginUsername: junsong\n        loginPassword: change-me\n    dynamic:\n      primary: master\n      strict: false\n      druid:\n        initial-size: 5\n        min-idle: 5\n        maxActive: 20\n        maxWait: 60000\n        connectTimeout: 30000\n        socketTimeout: 60000\n        timeBetweenEvictionRunsMillis: 60000\n        minEvictableIdleTimeMillis: 300000\n        validationQuery: SELECT 1 FROM DUAL\n        testWhileIdle: true\n        testOnBorrow: false\n        testOnReturn: false\n        poolPreparedStatements: true\n        maxPoolPreparedStatementPerConnectionSize: 20\n        filters: stat,slf4j\n        connectionProperties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000\n      datasource:\n        master:\n          username: root\n          password: change-me-db-password\n          driver-class-name: com.mysql.cj.jdbc.Driver\n          url: jdbc:mysql://junsong-mysql:3306/junsong-cloud?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true\n','2f93bcdddbe615126d1bf25d29274413','2026-06-13 21:40:09','2026-06-13 21:43:49','nacos','127.0.0.1','','','','','','yaml','',''),(458,'datasource-prod.yml','DEFAULT_GROUP','spring:\n  datasource:\n    driver-class-name: com.mysql.cj.jdbc.Driver\n    url: jdbc:mysql://junsong-mysql:3306/junsong-cloud?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8\n    username: root\n    password: change-me-db-password\n\nspringdoc:\n','d6f61d912820160ddd21fba126872028','2026-06-13 21:40:09','2026-06-13 21:40:09','nacos','127.0.0.1','','','','','','yaml','',''),(459,'redis-dev.yml','DEFAULT_GROUP','spring:\n  data:\n    redis:\n      host: junsong-redis\n      port: 6379\n      password:\n    \n      database: 0\n      timeout: 10s\n      lettuce:\n        pool:\n          min-idle: 0\n          max-idle: 8\n          max-active: 8\n          max-wait: -1ms\n','73a54fe9ffbb3fe543edf12a0ee5ff98','2026-06-13 21:40:09','2026-06-13 21:41:32','nacos','127.0.0.1','','','','','','yaml','',''),(460,'redis-prod.yml','DEFAULT_GROUP','spring:\n  data:\n    redis:\n      host: junsong-redis\n      port: 6379\n      password:\n    \n      database: 0\n      timeout: 10s\n      lettuce:\n        pool:\n          min-idle: 0\n          max-idle: 8\n          max-active: 8\n          max-wait: -1ms\n','73a54fe9ffbb3fe543edf12a0ee5ff98','2026-06-13 21:40:09','2026-06-13 21:41:32','nacos','127.0.0.1','','','','','','yaml','','');
/*!40000 ALTER TABLE `config_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_info_aggr`
--

DROP TABLE IF EXISTS `config_info_aggr`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_info_aggr` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'data_id',
  `group_id` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'group_id',
  `datum_id` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'datum_id',
  `content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '内容',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `app_name` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `tenant_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户字段',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfoaggr_datagrouptenantdatum` (`data_id`,`group_id`,`tenant_id`,`datum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='增加租户字段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_info_aggr`
--

LOCK TABLES `config_info_aggr` WRITE;
/*!40000 ALTER TABLE `config_info_aggr` DISABLE KEYS */;
/*!40000 ALTER TABLE `config_info_aggr` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_info_beta`
--

DROP TABLE IF EXISTS `config_info_beta`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_info_beta` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'group_id',
  `app_name` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'app_name',
  `content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'content',
  `beta_ips` varchar(1024) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'betaIps',
  `md5` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'md5',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `src_user` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT 'source user',
  `src_ip` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'source ip',
  `tenant_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户字段',
  `encrypted_data_key` varchar(1024) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '' COMMENT '密钥',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfobeta_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='config_info_beta';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_info_beta`
--

LOCK TABLES `config_info_beta` WRITE;
/*!40000 ALTER TABLE `config_info_beta` DISABLE KEYS */;
/*!40000 ALTER TABLE `config_info_beta` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_info_gray`
--

DROP TABLE IF EXISTS `config_info_gray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_info_gray` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) NOT NULL COMMENT 'group_id',
  `content` longtext NOT NULL COMMENT 'content',
  `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
  `src_user` text COMMENT 'src_user',
  `src_ip` varchar(100) DEFAULT NULL COMMENT 'src_ip',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmt_create',
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmt_modified',
  `app_name` varchar(128) DEFAULT NULL COMMENT 'app_name',
  `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
  `gray_name` varchar(128) NOT NULL COMMENT 'gray_name',
  `gray_rule` text NOT NULL COMMENT 'gray_rule',
  `encrypted_data_key` varchar(256) NOT NULL DEFAULT '' COMMENT 'encrypted_data_key',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfogray_datagrouptenantgray` (`data_id`,`group_id`,`tenant_id`,`gray_name`),
  KEY `idx_dataid_gmt_modified` (`data_id`,`gmt_modified`),
  KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='config_info_gray';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_info_gray`
--

LOCK TABLES `config_info_gray` WRITE;
/*!40000 ALTER TABLE `config_info_gray` DISABLE KEYS */;
/*!40000 ALTER TABLE `config_info_gray` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_info_tag`
--

DROP TABLE IF EXISTS `config_info_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_info_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'group_id',
  `tenant_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT 'tenant_id',
  `tag_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'tag_id',
  `app_name` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'app_name',
  `content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'content',
  `md5` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'md5',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `src_user` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT 'source user',
  `src_ip` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'source ip',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfotag_datagrouptenanttag` (`data_id`,`group_id`,`tenant_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='config_info_tag';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_info_tag`
--

LOCK TABLES `config_info_tag` WRITE;
/*!40000 ALTER TABLE `config_info_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `config_info_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_tags_relation`
--

DROP TABLE IF EXISTS `config_tags_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_tags_relation` (
  `id` bigint NOT NULL COMMENT 'id',
  `tag_name` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'tag_name',
  `tag_type` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'tag_type',
  `data_id` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'group_id',
  `tenant_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT 'tenant_id',
  `nid` bigint NOT NULL AUTO_INCREMENT COMMENT 'nid, 自增长标识',
  PRIMARY KEY (`nid`),
  UNIQUE KEY `uk_configtagrelation_configidtag` (`id`,`tag_name`,`tag_type`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='config_tag_relation';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_tags_relation`
--

LOCK TABLES `config_tags_relation` WRITE;
/*!40000 ALTER TABLE `config_tags_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `config_tags_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_capacity`
--

DROP TABLE IF EXISTS `group_capacity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_capacity` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '' COMMENT 'Group ID，空字符表示整个集群',
  `quota` int unsigned NOT NULL DEFAULT '0' COMMENT '配额，0表示使用默认值',
  `usage` int unsigned NOT NULL DEFAULT '0' COMMENT '使用量',
  `max_size` int unsigned NOT NULL DEFAULT '0' COMMENT '单个配置大小上限，单位为字节，0表示使用默认值',
  `max_aggr_count` int unsigned NOT NULL DEFAULT '0' COMMENT '聚合子配置最大个数，，0表示使用默认值',
  `max_aggr_size` int unsigned NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值',
  `max_history_count` int unsigned NOT NULL DEFAULT '0' COMMENT '最大变更历史数量',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_id` (`group_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='集群、各Group容量信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_capacity`
--

LOCK TABLES `group_capacity` WRITE;
/*!40000 ALTER TABLE `group_capacity` DISABLE KEYS */;
/*!40000 ALTER TABLE `group_capacity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `his_config_info`
--

DROP TABLE IF EXISTS `his_config_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `his_config_info` (
  `id` bigint unsigned NOT NULL COMMENT 'id',
  `nid` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'nid, 自增标识',
  `data_id` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'group_id',
  `app_name` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'app_name',
  `content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'content',
  `md5` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'md5',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `src_user` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT 'source user',
  `src_ip` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'source ip',
  `op_type` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'operation type',
  `tenant_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户字段',
  `encrypted_data_key` varchar(1024) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '' COMMENT '密钥',
  `publish_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT 'formal' COMMENT 'publish type gray or formal',
  `gray_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'gray name',
  `ext_info` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT 'ext info',
  PRIMARY KEY (`nid`),
  KEY `idx_gmt_create` (`gmt_create`),
  KEY `idx_gmt_modified` (`gmt_modified`),
  KEY `idx_did` (`data_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='多租户改造';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `his_config_info`
--

LOCK TABLES `his_config_info` WRITE;
/*!40000 ALTER TABLE `his_config_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `his_config_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions` (
  `role` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'role',
  `resource` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'resource',
  `action` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'action',
  UNIQUE KEY `uk_role_permission` (`role`,`resource`,`action`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permissions`
--

LOCK TABLES `permissions` WRITE;
/*!40000 ALTER TABLE `permissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'username',
  `role` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'role',
  UNIQUE KEY `idx_user_role` (`username`,`role`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES ('nacos','ROLE_ADMIN');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_capacity`
--

DROP TABLE IF EXISTS `tenant_capacity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_capacity` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '' COMMENT 'Tenant ID',
  `quota` int unsigned NOT NULL DEFAULT '0' COMMENT '配额，0表示使用默认值',
  `usage` int unsigned NOT NULL DEFAULT '0' COMMENT '使用量',
  `max_size` int unsigned NOT NULL DEFAULT '0' COMMENT '单个配置大小上限，单位为字节，0表示使用默认值',
  `max_aggr_count` int unsigned NOT NULL DEFAULT '0' COMMENT '聚合子配置最大个数',
  `max_aggr_size` int unsigned NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值',
  `max_history_count` int unsigned NOT NULL DEFAULT '0' COMMENT '最大变更历史数量',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='租户容量信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_capacity`
--

LOCK TABLES `tenant_capacity` WRITE;
/*!40000 ALTER TABLE `tenant_capacity` DISABLE KEYS */;
/*!40000 ALTER TABLE `tenant_capacity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_info`
--

DROP TABLE IF EXISTS `tenant_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `kp` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT 'kp',
  `tenant_id` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT 'tenant_id',
  `tenant_name` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '' COMMENT 'tenant_name',
  `tenant_desc` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'tenant_desc',
  `create_source` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'create_source',
  `gmt_create` bigint NOT NULL COMMENT '创建时间',
  `gmt_modified` bigint NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_info_kptenantid` (`kp`,`tenant_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='tenant_info';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_info`
--

LOCK TABLES `tenant_info` WRITE;
/*!40000 ALTER TABLE `tenant_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `tenant_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'username',
  `password` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'password',
  `enabled` tinyint(1) NOT NULL COMMENT 'enabled',
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES ('nacos','$2a$10$vhrxb8iv1ySX68QhLJeiNO2barJHAQAylFyPQ9o7mdQ0LnudHqcze',1);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'junsong-config'
--

--
-- Dumping routines for database 'junsong-config'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-14  2:51:03
