import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const agents = readFileSync('AGENTS.md', 'utf8')
const handoff = readFileSync('bin/2026-07-01-dev-prod-deployment-agent-handoff.zh-CN.md', 'utf8')

test('agent guidance is compact and preserves universal boundaries', () => {
  const words = agents.trim().split(/\s+/).length
  assert.ok(words <= 1450, `AGENTS.md word budget exceeded: ${words}`)
  for (const phrase of [
    'Backend authorization is authoritative',
    'tenant',
    'locked or carried-forward',
    'SET NAMES utf8mb4',
    'nested Git repository',
    'detect_changes'
  ]) assert.match(agents, new RegExp(phrase))
})

test('Nacos guidance is V3-only and fail-closed', () => {
  for (const text of [agents, handoff]) {
    assert.match(text, /Nacos V3/)
    assert.match(text, /禁止.*\/nacos\/v1\//)
    assert.match(text, /config_info/)
    assert.match(text, /401.*403.*404/s)
    const v1Lines = text.split('\n').filter((line) => line.includes('/nacos/v1/'))
    assert.ok(v1Lines.length > 0)
    for (const line of v1Lines) assert.match(line, /禁止|不得|never|未升级前不得/i)
    assert.doesNotMatch(text, /curl[^\n]*\/nacos\/v1\//i)
    const fallbackLines = text.split('\n').filter((line) => /回退|降级|fallback|fall back/i.test(line) && /V1/i.test(line))
    for (const line of fallbackLines) assert.match(line, /禁止|不得|never/i)
  }
})
