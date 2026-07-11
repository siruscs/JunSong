import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import test from 'node:test'

const root = join(import.meta.dirname, '..')
const read = (path) => readFileSync(join(root, path), 'utf8')

const backendScripts = [
  'auth',
  'file',
  'finance',
  'gateway',
  'gen',
  'job',
  'member',
  'monitor',
  'open',
  'system',
  'workflow'
]

test('all backend deploy scripts delegate to the shared deployment runtime', () => {
  for (const service of backendScripts) {
    const source = read(`bin/deploy-${service}.sh`)
    assert.match(source, /source .*deploy-common\.sh/, service)
    assert.match(source, /deploy_backend_service/, service)
  }
})

test('production runtime targets the real server and verifies deployed artifacts', () => {
  const source = read('bin/deploy-common.sh')
  assert.match(source, /PROD_HOST="120\.55\.243\.17"/)
  assert.match(source, /PROD_USER="root"/)
  assert.match(source, /DEPLOY_DRY_RUN/)
  assert.match(source, /sha256/)
  assert.match(source, /--force-recreate/)
  assert.match(source, /docker inspect/)
  assert.match(source, /com\.docker\.compose\.project\.config_files/)
  assert.match(source, /docker cp/)
  assert.match(source, /rollback/)
  assert.match(source, /DRY-RUN 计划生成完成/)
  assert.doesNotMatch(source, /\\\\\$1/)
})

test('ui and sql production deploys use the shared remote runtime', () => {
  for (const script of ['bin/deploy-ui.sh', 'bin/deploy-sql.sh']) {
    const source = read(script)
    assert.match(source, /source .*deploy-common\.sh/, script)
    assert.match(source, /prod_ssh/, script)
  }
  assert.match(read('bin/deploy-sql.sh'), /set -o pipefail.*mysqldump/)
})

test('decision console composes the audited deploy scripts', () => {
  const source = read('bin/deploy-decision-console.sh')
  assert.match(source, /deploy-sql\.sh/)
  assert.match(source, /deploy-finance\.sh/)
  assert.match(source, /deploy-member\.sh/)
  assert.match(source, /deploy-ui\.sh/)
  assert.doesNotMatch(source, /docker restart/)
})

test('aggregate deploy entrypoints propagate dev or prod to audited scripts', () => {
  for (const script of ['bin/deploy.sh', 'bin/quick-deploy.sh']) {
    const source = read(script)
    assert.match(source, /ENV="\$\{1:-dev\}"/, script)
    assert.match(source, /deploy-gateway\.sh/, script)
    assert.match(source, /"\$\{ENV\}"/, script)
    assert.doesNotMatch(source, /docker restart/, script)
  }
})

test('production maintenance scripts reuse the fixed remote target', () => {
  const source = read('bin/clear-menu-cache.sh')
  assert.match(source, /source .*deploy-common\.sh/)
  assert.match(source, /prod_ssh/)
  assert.doesNotMatch(source, /ssh \$\{PROD_HOST\}/)
})
