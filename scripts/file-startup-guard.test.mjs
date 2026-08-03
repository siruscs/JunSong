import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const source = fs.readFileSync('junsong-common/junsong-common-core/src/main/java/com/junsong/common/core/idempotency/IdempotencyAutoConfiguration.java', 'utf8')
const fileBootstrap = fs.readFileSync('junsong-modules/junsong-file/src/main/resources/bootstrap.yml', 'utf8')
const fileApplication = fs.readFileSync('junsong-modules/junsong-file/src/main/java/com/junsong/file/JunSongFileApplication.java', 'utf8')

test('idempotency mapper is conditional on a datasource session factory', () => {
  assert.match(source, /@ConditionalOnBean\(SqlSessionFactory\.class\)\s+@Bean\s+public MapperFactoryBean<IdempotencyRecordMapper> idempotencyRecordMapper/)
})

test('file service disables database-backed idempotency auto configuration', () => {
  assert.match(fileBootstrap, /junsong:\s*\n\s+idempotency:\s*\n\s+enabled:\s*false/)
})

test('file service excludes MyBatis auto configuration without a database', () => {
  assert.match(fileApplication, /DataSourceAutoConfiguration\.class,\s*MybatisAutoConfiguration\.class,\s*IdempotencyAutoConfiguration\.class/)
})
