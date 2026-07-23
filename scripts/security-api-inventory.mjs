import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

function walk(dir, output = []) {
  if (!fs.existsSync(dir)) return output
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (['target', 'node_modules', 'dist', '.git'].includes(entry.name)) continue
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) walk(full, output)
    else if (full.endsWith('Controller.java')) output.push(full)
  }
  return output
}

function annotationsBefore(source, boundary, floor = 0) {
  const prefix = source.slice(0, boundary)
  const start = Math.max(floor, prefix.lastIndexOf('\n\n') + 1, prefix.lastIndexOf(';') + 1)
  return prefix.slice(start)
}

function mappings(annotations) {
  const result = []
  const pattern = /@(Get|Post|Put|Delete|Patch|Request)Mapping(?:\s*\(([\s\S]*?)\))?/g
  for (const match of annotations.matchAll(pattern)) {
    const values = [...(match[2] || '').matchAll(/["']([^"']*)["']/g)].map((item) => item[1])
    result.push({ method: match[1] === 'Request' ? 'ANY' : match[1].toUpperCase(), paths: values.length ? values : [''] })
  }
  return result
}

function joinPath(base, child) {
  const value = `${base || ''}/${child || ''}`.replace(/\/{2,}/g, '/')
  return value === '/' ? '/' : value.replace(/\/$/, '')
}

function permissionOf(annotations) {
  return annotations.match(/@RequiresPermissions\s*\(\s*["']([^"']+)["']/)?.[1] ||
    annotations.match(/hasPermi\s*\(\s*["']([^"']+)["']\s*\)/)?.[1] ||
    (/@PreAuthorize\b/.test(annotations) ? 'PRE_AUTHORIZE_EXPRESSION' : '')
}

function methodBody(source, methodIndex) {
  const start = source.indexOf('{', methodIndex)
  if (start < 0) return ''
  let depth = 0
  let quote = ''
  let escaped = false
  for (let index = start; index < source.length; index += 1) {
    const char = source[index]
    if (quote) {
      if (escaped) escaped = false
      else if (char === '\\') escaped = true
      else if (char === quote) quote = ''
      continue
    }
    if (char === '"' || char === "'") {
      quote = char
      continue
    }
    if (char === '{') depth += 1
    if (char === '}' && --depth === 0) return source.slice(start, index + 1)
  }
  return source.slice(start)
}

export function parseControllerSource(source, file = 'fixture.java') {
  const classMatch = /\bclass\s+\w+Controller\b/.exec(source)
  if (!classMatch) return []
  const classRoutes = mappings(annotationsBefore(source, classMatch.index)).flatMap((item) => item.paths)
  const bases = classRoutes.length ? classRoutes : ['']
  const rows = []
  const methodPattern = /\bpublic\s+(?:static\s+)?[\w<>, ?\[\]]+\s+(\w+)\s*\(/g
  for (const method of source.matchAll(methodPattern)) {
    const annotations = annotationsBefore(source, method.index, classMatch.index + classMatch[0].length)
    const routes = mappings(annotations)
    const permission = permissionOf(annotations)
    for (const route of routes) {
      for (const base of bases) {
        for (const child of route.paths) {
          rows.push({
            file,
            line: source.slice(0, method.index).split(/\r?\n/).length,
            method: route.method,
            path: joinPath(base, child),
            permission,
            isExport: /BusinessType\.EXPORT|\/export\b/i.test(`${annotations} ${child}`),
            isPaged: /startPage\s*\(|getDataTable\s*\(/.test(methodBody(source, method.index)),
            internalOnly: /@InnerAuth\b|SecurityConstants\.INNER/.test(annotations),
            reviewStatus: permission ? 'PROTECTED' : 'REVIEW'
          })
        }
      }
    }
  }
  return rows
}

export function inventoryApis(root) {
  return walk(root).flatMap((file) => parseControllerSource(fs.readFileSync(file, 'utf8'), path.relative(root, file)))
    .sort((a, b) => a.file.localeCompare(b.file) || a.line - b.line || a.path.localeCompare(b.path))
}

if (process.argv[1] === fileURLToPath(import.meta.url)) process.stdout.write(JSON.stringify(inventoryApis(process.cwd()), null, 2) + '\n')
