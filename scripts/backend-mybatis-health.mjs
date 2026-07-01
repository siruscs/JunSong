#!/usr/bin/env node

/**
 * backend-mybatis-health.mjs
 *
 * Scans MyBatis mapper XML files for empty statement bodies.
 * Detects <select>, <insert>, <update>, <delete> tags that have no SQL content
 * between open and close tags (only whitespace or comments).
 *
 * Excludes <sql id="..."> fragments (these are reusable fragments, not statements).
 *
 * Usage:
 *   node scripts/backend-mybatis-health.mjs [--json]
 *
 * Exit codes:
 *   0 = all clear
 *   1 = empty statements found
 */

import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'

const PROJECT_ROOT = new URL('..', import.meta.url).pathname.replace(/\/$/, '')

const STATEMENT_TAGS = ['select', 'insert', 'update', 'delete']

const MAPPER_GLOBS = [
  'junsong-modules/**/src/main/resources/mapper/**/*.xml',
]

function* walkDirs(base, pattern) {
  const parts = pattern.split('/')
  yield* walk(base, parts, 0)
}

function* walk(dir, parts, idx) {
  if (idx >= parts.length) {
    yield dir
    return
  }
  const segment = parts[idx]
  let entries
  try {
    entries = readdirSync(dir)
  } catch {
    return
  }
  if (segment === '**') {
    // match zero or more directory levels
    yield* walk(dir, parts, idx + 1)
    for (const entry of entries) {
      const full = join(dir, entry)
      try {
        if (statSync(full).isDirectory()) {
          yield* walk(full, parts, idx)
        }
      } catch { /* ignore */ }
    }
  } else if (segment.includes('*')) {
    const re = new RegExp('^' + segment.replace(/\*/g, '[^/]*') + '$')
    for (const entry of entries) {
      if (re.test(entry)) {
        yield* walk(join(dir, entry), parts, idx + 1)
      }
    }
  } else {
    yield* walk(join(dir, segment), parts, idx + 1)
  }
}

function findMapperFiles() {
  const files = new Set()
  for (const glob of MAPPER_GLOBS) {
    for (const f of walkDirs(PROJECT_ROOT, glob)) {
      files.add(f)
    }
  }
  return [...files]
}

/**
 * Strip XML comments from content
 */
function stripComments(xml) {
  return xml.replace(/<!--[\s\S]*?-->/g, '')
}

/**
 * Check a single mapper XML file for empty statement bodies.
 * Returns an array of { tag, id, line } for each empty statement found.
 */
export function checkMapperFile(filePath) {
  const raw = readFileSync(filePath, 'utf8')
  const issues = []

  for (const tag of STATEMENT_TAGS) {
    // Match opening tag (with attributes) and closing tag
    const openRe = new RegExp(`<${tag}\\b([^>]*)>`, 'gi')
    let match
    while ((match = openRe.exec(raw)) !== null) {
      const openEnd = match.index + match[0].length
      const closeTag = `</${tag}>`
      const closeIdx = raw.indexOf(closeTag, openEnd)
      if (closeIdx === -1) continue // self-closing or malformed

      // Extract body between open and close tags
      const body = raw.slice(openEnd, closeIdx)
      const stripped = stripComments(body).trim()

      if (stripped.length === 0) {
        // Extract id attribute
        const idMatch = match[1].match(/id\s*=\s*"([^"]*)"/)
        const id = idMatch ? idMatch[1] : '(unknown)'
        // Calculate line number
        const lineNum = raw.slice(0, match.index).split('\n').length
        issues.push({ tag, id, line: lineNum })
      }
    }
  }

  return issues
}

export function runMyBatisHealthCheck() {
  const files = findMapperFiles()
  const allIssues = []

  for (const file of files) {
    const issues = checkMapperFile(file)
    for (const issue of issues) {
      allIssues.push({
        file: relative(PROJECT_ROOT, file),
        ...issue,
      })
    }
  }

  return allIssues
}

// --- CLI ---
const isMain = process.argv[1] &&
  new URL('file://' + process.argv[1]).href === import.meta.url

if (isMain) {
  const issues = runMyBatisHealthCheck()

  if (process.argv.includes('--json')) {
    console.log(JSON.stringify({ ok: issues.length === 0, issues }, null, 2))
  } else if (issues.length === 0) {
    console.log('[mybatis-health] OK: no empty mapper statements found')
  } else {
    console.error(`[mybatis-health] FAIL: ${issues.length} empty mapper statement(s) found:\n`)
    for (const { file, tag, id, line } of issues) {
      console.error(`  ${file}:${line}  <${tag} id="${id}"> — empty body`)
    }
    process.exit(1)
  }
}
