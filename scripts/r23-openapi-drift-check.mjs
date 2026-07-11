import fs from 'node:fs'
import assert from 'node:assert/strict'

const catalogPath = 'junsong-ui-v3/src/views/open/data/catalog.ts'
const openapiPath = 'sdk/openapi.json'

const catalog = fs.readFileSync(catalogPath, 'utf8')
const openapi = JSON.parse(fs.readFileSync(openapiPath, 'utf8'))

const PREFIX = '/openapi/v1'

const catalogPaths = [...new Set(
  [...catalog.matchAll(/path:\s*'([^']+)'/g)].map((m) => `${PREFIX}${m[1]}`)
)].sort()

const contractPaths = Object.keys(openapi.paths || {})
  .map((p) => `${PREFIX}${p}`)
  .sort()

const missingInContract = catalogPaths.filter((path) => !contractPaths.includes(path))
const missingInCatalog = contractPaths.filter((path) => !catalogPaths.includes(path))

if (missingInContract.length || missingInCatalog.length) {
  console.error('[r23-openapi-drift] FAIL')
  console.error('missingInContract=', missingInContract)
  console.error('missingInCatalog=', missingInCatalog)
  process.exit(1)
}

for (const [rawPath, item] of Object.entries(openapi.paths || {})) {
  for (const method of Object.keys(item)) {
    const operation = item[method]
    const fullpath = `${PREFIX}${rawPath}`
    assert.ok(operation.operationId, `${method.toUpperCase()} ${fullpath} missing operationId`)
    assert.ok(operation.summary, `${method.toUpperCase()} ${fullpath} missing summary`)
    assert.ok(operation.security, `${method.toUpperCase()} ${fullpath} missing security`)
  }
}

console.log('[r23-openapi-drift] PASS')
