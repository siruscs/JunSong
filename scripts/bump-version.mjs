import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const packagePath = path.join(projectRoot, 'package.json')
const packageJson = JSON.parse(fs.readFileSync(packagePath, 'utf8'))
const part = process.argv[2] || 'build'

/**
 * 生成小程序构建版本号：YY.MM.N
 * YY：年份后两位；MM：月份；N：当月构建序号。
 * 同年月构建序号递增，跨月从 0 开始。
 */
export function generateBuildVersion(previousVersion, buildDate = new Date()) {
  const [previousYear, previousMonth, previousBuild] = String(previousVersion || '').split('.').map(Number)
  const year = Number(String(buildDate.getFullYear()).slice(-2))
  const month = buildDate.getMonth() + 1
  const buildNumber = previousYear === year && previousMonth === month ? previousBuild + 1 : 0
  return `${year}.${month}.${buildNumber}`
}

const [major, minor, patch] = String(packageJson.version || '').split('.').map(Number)
const now = new Date()
const next = part === 'build'
  ? generateBuildVersion(packageJson.version, now).split('.').map(Number)
  : { major: [major + 1, 0, 0], minor: [major, minor + 1, 0], patch: [major, minor, patch + 1] }[part]

if (!next || next.some((value) => !Number.isInteger(value) || value < 0)) {
  throw new Error(`不支持的版本递增类型：${part}`)
}

packageJson.version = next.join('.')
fs.writeFileSync(packagePath, `${JSON.stringify(packageJson, null, 2)}\n`)
console.log(`版本已更新为 ${packageJson.version}`)
