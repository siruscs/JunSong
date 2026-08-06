import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

test('mini-program exposes the four configuration sync types through a permission-gated module', () => {
  const modules = fs.readFileSync('src/config/modules.js', 'utf8')
  const page = fs.readFileSync('src/pages/config-sync/index.vue', 'utf8')
  const pages = fs.readFileSync('src/pages.json', 'utf8')
  assert.match(modules, /configSync:/)
  assert.match(modules, /customPage:\s*['"]\/pages\/config-sync\/index['"]/)
  for (const syncType of ['PRODUCT', 'SUPPLIER', 'LEVEL', 'CAMPAIGN_POLICY']) assert.match(page, new RegExp(syncType))
  assert.match(page, /\/member\/config-sync\/preview/)
  assert.match(page, /\/member\/config-sync\/execute/)
  assert.match(pages, /pages\/config-sync\/index/)
})

test('mini-program form standards declare three-decimal quantities and two-decimal money', () => {
  const standards = fs.readFileSync('src/config/formStandards.js', 'utf8')
  assert.match(standards, /QUANTITY_PRECISION\s*=\s*3/)
  assert.match(standards, /MONEY_PRECISION\s*=\s*2/)
})
