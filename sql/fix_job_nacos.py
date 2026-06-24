import pymysql, hashlib

config_job = """spring:
  autoconfigure:
    exclude:
      - com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure
      - com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration
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
  configuration:
    map-underscore-to-camel-case: true
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

md5 = hashlib.md5(config_job.encode('utf-8')).hexdigest()

conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', password='change-me-db-password', database='junsong-config', charset='utf8mb4')
cursor = conn.cursor()
cursor.execute("DELETE FROM config_info WHERE data_id = 'junsong-job-dev.yml'")
cursor.execute(
    "INSERT INTO config_info (data_id, group_id, content, md5, gmt_create, gmt_modified, src_user, src_ip, app_name, tenant_id, c_desc, c_use, effect, type, c_schema) VALUES (%s, %s, %s, %s, NOW(), NOW(), 'admin', '127.0.0.1', '', '', '', '', 'Y', 'yaml', '')",
    ('junsong-job-dev.yml', 'DEFAULT_GROUP', config_job, md5)
)
conn.commit()
print(f"OK, md5={md5}, len={len(config_job)}")
cursor.close()
conn.close()
