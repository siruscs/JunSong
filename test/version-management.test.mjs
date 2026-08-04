import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const mine = fs.readFileSync('src/pages/mine/index.vue', 'utf8')
const buildScript = fs.readFileSync('scripts/build-mp-weixin.mjs', 'utf8')
const bumpScript = fs.readFileSync('scripts/bump-version.mjs', 'utf8')
const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf8'))

test('mine page displays the package version instead of a hardcoded release', () => {
  assert.match(mine, /packageInfo\.version/)
  assert.doesNotMatch(mine, /松·云助手 v1\.7\.0/)
})

test('verified build metadata records the package version', () => {
  assert.match(buildScript, /const packageJson = JSON\.parse\(fs\.readFileSync\(path\.join\(projectRoot, 'package\.json'\)/)
  assert.match(buildScript, /version: packageJson\.version/)
  assert.match(buildScript, /bump-version\.mjs.*'build'/)
  assert.match(bumpScript, /YY\.MM\.N/)
  assert.match(bumpScript, /export function generateBuildVersion/)
})

test('package version follows semantic versioning', () => {
  assert.match(packageJson.version, /^\d+\.\d+\.\d+$/)
})
