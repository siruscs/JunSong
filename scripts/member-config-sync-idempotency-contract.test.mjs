import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

test('config sync execute allows retry after a failed write', () => {
  const source = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemConfigSyncController.java', 'utf8')
  const execute = source.match(/@Idempotent\(scene\s*=\s*"member:configSync:execute"[\s\S]*?\)\s*\n\s*@PostMapping\("\/execute"\)/)
  assert.ok(execute, 'execute idempotency annotation should be present')
  assert.match(execute[0], /retryPolicy\s*=\s*IdempotencyRetryPolicy\.ALLOW_SAME_KEY/)
})
