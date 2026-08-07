import assert from 'node:assert/strict'
import fs from 'node:fs'

const root = 'junsong-miniprogram/src'
const authSession = fs.readFileSync(`${root}/utils/authSession.js`, 'utf8')
const foregroundSession = fs.readFileSync(`${root}/utils/foregroundSession.js`, 'utf8')
const api = fs.readFileSync(`${root}/api/index.js`, 'utf8')
const app = fs.readFileSync(`${root}/App.vue`, 'utf8')
const home = fs.readFileSync(`${root}/pages/index/index.vue`, 'utf8')
const stockInit = fs.readFileSync(`${root}/api/stockInit.js`, 'utf8')
const workbench = fs.readFileSync(`${root}/pages/workbench/index.vue`, 'utf8')

assert.match(authSession, /restoreSession/, 'session restore entry is required')
assert.match(authSession, /refresh/i, 'session refresh is required')
assert.match(foregroundSession, /restoreSession/, 'foreground resume must restore session')
assert.match(api, /refresh/i, 'request layer must coordinate token refresh')
assert.match(app, /onShow/, 'app must restore session on foreground')
assert.match(home, /周期净利|本周期净利/, 'home must prioritize period net profit')
assert.match(home, /回本差额|距回本|距离回本/, 'home must show break-even gap')
assert.match(home, /实际缴款/, 'home must show actual payment income')
assert.match(home, /totalUnverifiedAdvance/, 'period cost must include unverified advances')
assert.match(home, /periodAudienceLabel/, 'home must label the authorized period audience')
assert.match(home, /暂无当前核算周期/, 'home must explain missing current period')
assert.match(home, /periodStale/, 'home must distinguish cached stale period data')
assert.match(home, /60000|lowFrequency/, 'home low-frequency requests must be throttled')
assert.match(api, /contextSensitive/, 'request layer must protect context-sensitive writes')
assert.match(stockInit, /contextSensitive:\s*true/, 'stock writes must validate department context after response')

console.log('miniprogram period v1 baseline checks passed')
