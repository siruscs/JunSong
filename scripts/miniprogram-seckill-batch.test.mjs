import assert from 'node:assert/strict'
import fs from 'node:fs'

const controller = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemSeckillRecordController.java', 'utf8')
const service = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemSeckillRecordServiceImpl.java', 'utf8')
const mapper = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/mapper/MemSeckillRecordMapper.java', 'utf8')
const xml = fs.readFileSync('junsong-modules/junsong-member/src/main/resources/mapper/member/MemSeckillRecordMapper.xml', 'utf8')
const home = fs.readFileSync('junsong-miniprogram/src/pages/index/index.vue', 'utf8')
const api = fs.readFileSync('junsong-miniprogram/src/api/seckill.js', 'utf8')

assert.match(controller, /statistics\/batch/)
assert.match(controller, /seckillIds/)
assert.match(service, /getRecordStatisticsBatch/)
assert.match(mapper, /selectRecordStatisticsBatch/)
assert.match(xml, /selectRecordStatisticsBatch/)
assert.match(xml, /seckillIds/)
assert.match(api, /seckillRecord\/statistics\/batch/)
assert.match(home, /getSeckillStatisticsBatch/)
assert.doesNotMatch(home, /activities\.map\(async \(item\)/)

console.log('miniprogram seckill batch checks passed')
