import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

function walk(dir, output = []) {
  if (!fs.existsSync(dir)) return output
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (['target', 'node_modules', 'dist', '.git'].includes(entry.name)) continue
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) walk(full, output)
    else if (full.endsWith('.xml') || full.endsWith('.java')) output.push(full)
  }
  return output
}

export function scanSqlSource(file, content) {
  const rows = []
  const lines = content.split(/\r?\n/)
  const add = (line, kind, expression, reviewStatus) => rows.push({ file, line, kind, expression: expression.trim(), reviewStatus })
  if (file.endsWith('.xml') && /<mapper\b/.test(content)) {
    lines.forEach((line, index) => {
      for (const match of line.matchAll(/\$\{([^}]+)\}/g)) {
        add(index + 1, 'MAPPER_SUBSTITUTION', match[1], match[1].includes('dataScope') ? 'ALLOWLIST_REQUIRED' : 'HIGH_RISK')
      }
    })
  }
  if (file.endsWith('.java')) {
    const sqlBearingFile = /\b(SELECT|UPDATE|DELETE|INSERT|CREATE|ALTER|DROP|TRUNCATE)\b/i.test(content)
    lines.forEach((line, index) => {
      if (/@(?:Select|Update|Delete|Insert)\b/.test(line) && /\+/.test(line)) add(index + 1, 'ANNOTATION_SQL_CONCAT', line, 'REVIEW')
      else if (sqlBearingFile && (/\.append\s*\(/.test(line) || /\bsql\w*\s*(?:\+?=)/i.test(line))) add(index + 1, 'JAVA_SQL_BUILDER', line, 'REVIEW')
      else if (/\b(SELECT|UPDATE|DELETE|INSERT|CREATE|ALTER|DROP|TRUNCATE)\b/i.test(line) && /["']\s*\+|\+\s*["']/.test(line)) add(index + 1, 'JAVA_SQL_CONCAT', line, 'REVIEW')
    })
  }
  return rows
}

export function inventorySql(root) {
  const roots = ['junsong-modules', 'junsong-common', 'junsong-api'].map((name) => path.join(root, name))
  return roots.flatMap((scanRoot) => walk(scanRoot)).flatMap((file) => scanSqlSource(path.relative(root, file), fs.readFileSync(file, 'utf8')))
    .sort((a, b) => a.file.localeCompare(b.file) || a.line - b.line)
}

if (process.argv[1] === fileURLToPath(import.meta.url)) process.stdout.write(JSON.stringify(inventorySql(process.cwd()), null, 2) + '\n')
