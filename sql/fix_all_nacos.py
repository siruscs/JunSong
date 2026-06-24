import pymysql, hashlib

configs = {}

# ================================================================
# 1. application-dev.yml  —  所有服务共享的公共基础配置
#    包含：Feign、Management、MyBatis、Servlet、Logging
#    不包含：Redis、DataSource（各自独立配置）
# ================================================================
configs['application-dev.yml'] = """\
spring:
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
    org.springframework: warn
"""

# ================================================================
# 2. redis-dev.yml  —  Redis 公共配置
# ================================================================
configs['redis-dev.yml'] = """\
spring:
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
"""

# ================================================================
# 3. datasource-druid-dev.yml  —  MySQL + Druid + DynamicDataSource 公共配置
#    使用 Druid 连接池的服务加载此配置
# ================================================================
configs['datasource-druid-dev.yml'] = """\
spring:
  autoconfigure:
    exclude: com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure

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
"""

# ================================================================
# 4. junsong-gateway-dev.yml  —  网关路由配置
# ================================================================
configs['junsong-gateway-dev.yml'] = """\
spring:
  cloud:
    gateway:
      server:
        webflux:
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
                - Path=/code/**
              filters:
                - StripPrefix=1
            - id: junsong-job
              uri: lb://junsong-job
              predicates:
                - Path=/schedule/**
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
            - id: junsong-userDept
              uri: lb://junsong-system
              predicates:
                - Path=/userDept/**
              filters:
                - StripPrefix=0
            - id: junsong-member
              uri: lb://junsong-member
              predicates:
                - Path=/member/**
    sentinel:
      eager: true
      transport:
        dashboard: 127.0.0.1:8718
      datasource:
        ds1:
          nacos:
            server-addr: junsong-nacos:8848
            dataId: sentinel-junsong-gateway
            groupId: DEFAULT_GROUP
            data-type: json
            rule-type: gw-flow

security:
  captcha:
    enabled: false
    type: math
  xss:
    enabled: true
    excludeUrls:
      - /system/notice
  ignore:
    whites:
      - /auth/logout
      - /auth/login
      - /system/user/deptsForLogin
      - /auth/register
      - /*/v2/api-docs
      - /*/v3/api-docs
      - /csrf
"""

# ================================================================
# 5. junsong-auth-dev.yml  —  认证授权中心（不需要数据源）
# ================================================================
configs['junsong-auth-dev.yml'] = """\
security:
  oauth2:
    client:
      default-scopes: read,write
      access-token-validity-seconds: 3600
      refresh-token-validity-seconds: 86400
"""

# ================================================================
# 6. junsong-system-dev.yml  —  系统模块
# ================================================================
configs['junsong-system-dev.yml'] = """\
file:
  upload:
    max-size: 10MB
    allowed-types: jpg,jpeg,png,gif,doc,docx,pdf,xls,xlsx

system:
  user:
    init-password: 123456
    avatar-default: /statics/images/default-avatar.png
"""

# ================================================================
# 7. junsong-gen-dev.yml  —  代码生成
# ================================================================
configs['junsong-gen-dev.yml'] = """\
springdoc:
  gatewayUrl: http://localhost:8080
  api-docs:
    enabled: true
  info:
    title: code gen api
    description: code gen service
    version: 1.0.0

gen:
  author: junsong
  packageName: com.junsong.project
  autoRemovePre: false
  tablePrefix: sys_
"""

# ================================================================
# 8. junsong-job-dev.yml  —  定时任务
#    Job 不用 Druid，使用 HikariCP + Quartz
# ================================================================
configs['junsong-job-dev.yml'] = """\
spring:
  datasource:
    url: jdbc:mysql://junsong-mysql:3306/junsong-cloud?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true
    username: root
    password: change-me-db-password
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      auto-commit: true
      idle-timeout: 30000
      pool-name: JobHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000

  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: never
    properties:
      org.quartz.jobStore.driverDelegateClass: org.quartz.impl.jdbcjobstore.StdJDBCDelegate
      org.quartz.jobStore.tablePrefix: qrtz_
      org.quartz.jobStore.isClustered: true
      org.quartz.jobStore.clusterCheckinInterval: 15000

mybatis:
  typeAliasesPackage: com.junsong.job.domain
  mapperLocations: classpath*:mapper/**/*Mapper.xml

springdoc:
  gatewayUrl: http://localhost:8080
  api-docs:
    enabled: true
  info:
    title: job
    description: job api
    version: 1.0.0

job:
  executor:
    appname: junsong-job-executor
    address:
    ip:
    port: 9999
    logretentiondays: 30
"""

# ================================================================
# 9. junsong-finance-dev.yml  —  财务模块
# ================================================================
configs['junsong-finance-dev.yml'] = """\
springdoc:
  gatewayUrl: http://localhost:8080
  api-docs:
    enabled: true
  info:
    title: finance api
    description: finance service
    version: 1.0.0

finance:
  invoice:
    max-amount: 1000000
    tax-rate: 0.13
  report:
    export-format: pdf,xlsx
    retention-days: 365
"""

# ================================================================
# 10. junsong-member-dev.yml  —  会员模块
# ================================================================
configs['junsong-member-dev.yml'] = """\
springdoc:
  gatewayUrl: http://localhost:8080
  api-docs:
    enabled: true
  info:
    title: member api
    description: member service
    version: 1.0.0

member:
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
    purchase-ratio: 0.01
"""

# ================================================================
# 11. junsong-file-dev.yml  —  文件服务
# ================================================================
configs['junsong-file-dev.yml'] = """\
file:
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
  allowed-domains: localhost,127.0.0.1,junsong.vip,www.junsong.vip
"""

# ================================================================
# 12. junsong-monitor-dev.yml  —  监控服务
# ================================================================
configs['junsong-monitor-dev.yml'] = """\
spring:
  security:
    user:
      name: junsong
      password: 123456
  boot:
    admin:
      ui:
        title: service monitor
"""

# ================================================================
# 写入 Nacos MySQL
# ================================================================
conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', password='change-me-db-password', database='junsong-config', charset='utf8mb4')
cursor = conn.cursor()

cursor.execute("DELETE FROM config_info")
print(f"Cleared all existing configs")

for data_id, content in configs.items():
    content = content.strip() + "\n"
    md5 = hashlib.md5(content.encode('utf-8')).hexdigest()
    cursor.execute(
        "INSERT INTO config_info (data_id, group_id, content, md5, gmt_create, gmt_modified, src_user, src_ip, app_name, tenant_id, c_desc, c_use, effect, type, c_schema) VALUES (%s, %s, %s, %s, NOW(), NOW(), 'admin', '127.0.0.1', '', '', '', '', 'Y', 'yaml', '')",
        (data_id, 'DEFAULT_GROUP', content, md5)
    )
    print(f"  + {data_id}: len={len(content)}")

conn.commit()

cursor.execute("SELECT data_id, id, LENGTH(content) FROM config_info ORDER BY data_id")
rows = cursor.fetchall()
print(f"\nTotal: {len(rows)} configs")
for row in rows:
    print(f"  {row[0]}: id={row[1]}, len={row[2]}")

cursor.close()
conn.close()
print("\nDone!")
