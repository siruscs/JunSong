#!/usr/bin/env python3
"""
Nacos 配置管理脚本 - 微服务最佳实践
- 公共配置（MySQL、Redis）集中在 application-dev.yml
- 各服务配置只包含特有配置
"""

import pymysql
from pymysql import IntegrityError

# ============================================
# 1. 公共配置 - application-dev.yml
# 包含所有服务共享的基础设施配置
# ============================================
application_config = """spring:
  autoconfigure:
    exclude: com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure

  # Redis 公共配置
  data:
    redis:
      host: junsong-redis
      port: 6379
      password:
      database: 0
      timeout: 10s
      lettuce:
        pool:
          min-idle: 0
          max-idle: 8
          max-active: 8
          max-wait: -1ms

  # MySQL 公共配置
  datasource:
    druid:
      stat-view-servlet:
        enabled: true
        loginUsername: junsong
        loginPassword: change-me
    dynamic:
      druid:
        initial-size: 5
        min-idle: 5
        maxActive: 20
        maxWait: 60000
        connectTimeout: 30000
        socketTimeout: 60000
        timeBetweenEvictionRunsMillis: 60000
        minEvictableIdleTimeMillis: 300000
        validationQuery: SELECT 1 FROM DUAL
        testWhileIdle: true
        testOnBorrow: false
        testOnReturn: false
        poolPreparedStatements: true
        maxPoolPreparedStatementPerConnectionSize: 20
        filters: stat,slf4j
        connectionProperties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000
      datasource:
        master:
          username: root
          password: change-me-db-password
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://junsong-mysql:3306/junsong-cloud?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true

  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
      force-request: true
      force-response: true

feign:
  sentinel:
    enabled: true
  okhttp:
    enabled: true
  httpclient:
    enabled: false
  client:
    config:
      default:
        connectTimeout: 10000
        readTimeout: 10000
  compression:
    request:
      enabled: true
      min-request-size: 8192
    response:
      enabled: true

management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    health:
      show-details: always

mybatis:
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    com.junsong: debug
    org.springframework: warn"""

# ============================================
# 2. Gateway 服务配置 - 只包含路由规则
# ============================================
gateway_config = """spring:
  cloud:
    gateway:
      discovery:
        locator:
          lowerCaseServiceId: true
          enabled: true
      routes:
        - id: junsong-auth
          uri: lb://junsong-auth
          predicates:
            - Path=/auth/**
          filters:
            - name: CacheRequestBody
              args:
                bodyClass: java.lang.String
            - ValidateCodeFilter
            - StripPrefix=1
        - id: junsong-gen
          uri: lb://junsong-gen
          predicates:
            - Path=/gen/**
          filters:
            - StripPrefix=1
        - id: junsong-job
          uri: lb://junsong-job
          predicates:
            - Path=/job/**
          filters:
            - StripPrefix=1
        - id: junsong-system
          uri: lb://junsong-system
          predicates:
            - Path=/system/**
          filters:
            - StripPrefix=1
        - id: junsong-file
          uri: lb://junsong-file
          predicates:
            - Path=/file/**
          filters:
            - StripPrefix=1
        - id: junsong-finance
          uri: lb://junsong-finance
          predicates:
            - Path=/finance/**
          filters:
            - StripPrefix=1
        - id: junsong-member
          uri: lb://junsong-member
          predicates:
            - Path=/member/**
          filters:
            - StripPrefix=1

security:
  captcha:
    enabled: true
  ignore:
    whites:
      - /auth/logout
      - /auth/login
      - /auth/register"""

# ============================================
# 3. Auth 服务配置
# ============================================
auth_config = """security:
  oauth2:
    client:
      default-scopes: read,write
      access-token-validity-seconds: 3600
      refresh-token-validity-seconds: 86400"""

# ============================================
# 4. System 服务配置
# ============================================
system_config = """file:
  upload:
    max-size: 10MB
    allowed-types: jpg,jpeg,png,gif,doc,docx,pdf,xls,xlsx

system:
  user:
    init-password: 123456
    avatar-default: /statics/images/default-avatar.png"""

# ============================================
# 5. Finance 服务配置
# ============================================
finance_config = """finance:
  invoice:
    max-amount: 1000000
    tax-rate: 0.13
  report:
    export-format: pdf,xlsx
    retention-days: 365"""

# ============================================
# 6. Member 服务配置
# ============================================
member_config = """member:
  level:
    max-level: 10
    upgrade-points:
      - 100
      - 500
      - 1000
      - 5000
      - 10000
  points:
    register: 50
    daily-login: 10
    purchase-ratio: 0.01"""

# ============================================
# 7. Gen 服务配置
# ============================================
gen_config = """springdoc:
  gatewayUrl: http://localhost:8080/${spring.application.name}
  api-docs:
    enabled: true
  info:
    title: 代码生成接口文档
    description: 峻松代码生成服务API文档
    version: 1.0.0

gen:
  author: junsong
  packageName: com.junsong.project
  autoRemovePre: false
  tablePrefix: sys_"""

# ============================================
# 8. Job 服务配置
# ============================================
job_config = """springdoc:
  gatewayUrl: http://localhost:8080/${spring.application.name}
  api-docs:
    enabled: true
  info:
    title: 定时任务接口文档
    description: 峻松定时任务服务API文档
    version: 1.0.0

job:
  executor:
    appname: junsong-job-executor
    address:
    ip:
    port: 9999
    logretentiondays: 30"""

# ============================================
# 9. File 服务配置
# ============================================
file_config = """file:
  domain: http://junsong-modules-file:9300
  path: /home/junsong/uploadPath
  prefix: /statics

fdfs:
  domain: http://junsong-modules-file
  soTimeout: 3000
  connectTimeout: 2000
  trackerList: junsong-modules-file:22122

minio:
  url: http://127.0.0.1:9000
  accessKey: change-me
  secretKey: change-me
  bucketName: test

referer:
  enabled: false
  allowed-domains: localhost,127.0.0.1,junsong.vip,www.junsong.vip"""

# ============================================
# 10. Monitor 服务配置
# ============================================
monitor_config = """spring:
  security:
    user:
      name: junsong
      password: 123456
  boot:
    admin:
      ui:
        title: 峻松服务状态监控"""

# 配置字典
configs = {
    'application-dev.yml': (application_config, '公共配置 - 包含MySQL、Redis等基础设施'),
    'junsong-gateway-dev.yml': (gateway_config, '网关路由配置'),
    'junsong-auth-dev.yml': (auth_config, '认证服务配置'),
    'junsong-system-dev.yml': (system_config, '系统服务配置'),
    'junsong-finance-dev.yml': (finance_config, '财务服务配置'),
    'junsong-member-dev.yml': (member_config, '会员服务配置'),
    'junsong-gen-dev.yml': (gen_config, '代码生成服务配置'),
    'junsong-job-dev.yml': (job_config, '定时任务服务配置'),
    'junsong-file-dev.yml': (file_config, '文件服务配置'),
    'junsong-monitor-dev.yml': (monitor_config, '监控服务配置'),
}


def deploy_configs():
    """部署 Nacos 配置"""
    try:
        print("正在连接到 MySQL...")
        connection = pymysql.connect(
            host='localhost',
            port=3306,
            user='root',
            password='change-me-db-password',
            database='junsong-config',
            charset='utf8mb4'
        )

        cursor = connection.cursor()

        print("\n正在清空旧配置...")
        cursor.execute("DELETE FROM config_info WHERE group_id = 'DEFAULT_GROUP'")
        connection.commit()
        print("  ✓ 已清空旧配置")

        print("\n正在部署新配置...")
        for data_id, (content, desc) in configs.items():
            print(f"  → {data_id}")

            # 删除旧配置（如果存在）
            cursor.execute(
                "DELETE FROM config_info WHERE data_id = %s AND group_id = 'DEFAULT_GROUP'",
                (data_id,)
            )

            # 插入新配置
            cursor.execute("""
                INSERT INTO config_info (
                    data_id, group_id, content, md5, gmt_create, gmt_modified,
                    src_user, src_ip, app_name, tenant_id, c_desc, type
                ) VALUES (
                    %s, 'DEFAULT_GROUP', %s,
                    MD5(%s),
                    NOW(), NOW(),
                    'nacos', '127.0.0.1', '', '',
                    %s, 'yaml'
                )
            """, (data_id, content, content, desc))

        connection.commit()

        print("\n" + "="*60)
        print("✅ 所有配置已成功部署到 Nacos!")
        print("="*60)
        print("\n配置架构：")
        print("  • application-dev.yml")
        print("    - Redis 配置（所有服务共享）")
        print("    - MySQL 配置（所有服务共享）")
        print("    - Feign 配置")
        print("    - MyBatis 配置")
        print("  • junsong-gateway-dev.yml")
        print("    - 路由规则（仅 Gateway 特有）")
        print("  • junsong-auth-dev.yml")
        print("    - OAuth2 配置（仅 Auth 特有）")
        print("  • junsong-system-dev.yml")
        print("    - 文件上传、业务参数（仅 System 特有）")
        print("  • 其他服务配置...")
        print("\n" + "="*60)

        cursor.close()
        connection.close()

    except Exception as e:
        print(f"\n❌ 错误: {e}")
        import traceback
        traceback.print_exc()


if __name__ == '__main__':
    deploy_configs()
