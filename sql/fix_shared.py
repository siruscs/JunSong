import pymysql, hashlib

config_shared = """spring:
  autoconfigure:
    exclude:
      - com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure
      - com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration

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
    org.springframework: warn
"""

conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', password='change-me-db-password', database='junsong-config', charset='utf8mb4')
cursor = conn.cursor()
md5 = hashlib.md5(config_shared.encode('utf-8')).hexdigest()
cursor.execute("DELETE FROM config_info WHERE data_id = 'application-dev.yml'")
cursor.execute(
    "INSERT INTO config_info (data_id, group_id, content, md5, gmt_create, gmt_modified, src_user, src_ip, app_name, tenant_id, c_desc, c_use, effect, type, c_schema) VALUES (%s, %s, %s, %s, NOW(), NOW(), 'admin', '127.0.0.1', '', '', '', '', 'Y', 'yaml', '')",
    ('application-dev.yml', 'DEFAULT_GROUP', config_shared, md5)
)
conn.commit()
print(f"application-dev.yml: md5={md5}, len={len(config_shared)}")
cursor.close()
conn.close()
