import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const distRoot = path.join(projectRoot, 'dist')
const viteBin = path.join(projectRoot, 'node_modules', 'vite', 'bin', 'vite.js')

const result = spawnSync(process.execPath, [viteBin, 'build', '--mode', 'production'], {
  cwd: projectRoot,
  env: { ...process.env, UNI_PLATFORM: 'mp-weixin' },
  stdio: 'inherit'
})

if (result.error) throw result.error
if (result.status !== 0) process.exit(result.status || 1)

const projectConfigPath = path.join(distRoot, 'project.config.json')
const requiredFiles = [
  'pages/form/index.js',
  'pages/form/index.wxml',
  'pages/form/form-modules/FormField.js',
  'pages/form/form-modules/FormField.wxml',
  'config/modules.js'
]

if (!fs.existsSync(projectConfigPath)) throw new Error(`小程序构建失败：缺少 ${projectConfigPath}`)
const projectConfig = JSON.parse(fs.readFileSync(projectConfigPath, 'utf8'))
if (projectConfig.miniprogramRoot && projectConfig.miniprogramRoot !== 'dist/') {
  throw new Error(`小程序构建目录异常：miniprogramRoot=${projectConfig.miniprogramRoot}`)
}
for (const relativePath of requiredFiles) {
  const filePath = path.join(distRoot, relativePath)
  if (!fs.existsSync(filePath)) throw new Error(`小程序构建产物缺失：${filePath}`)
}

const formWxml = fs.readFileSync(path.join(distRoot, 'pages/form/index.wxml'), 'utf8')
const formJs = fs.readFileSync(path.join(distRoot, 'pages/form/index.js'), 'utf8')
if (!formWxml.includes('bindchange')) throw new Error('小程序构建校验失败：表单下拉事件未生成')
if (!formJs.includes('scheduleDraftSave') || !formJs.includes('onUnload')) {
  throw new Error('小程序构建校验失败：草稿恢复保护逻辑未生成')
}

const sourceFiles = ['src/pages/form/index.vue', 'src/pages/form/form-modules/FormField.vue', 'src/config/modules.js']
const sourceHashes = Object.fromEntries(sourceFiles.map((relativePath) => {
  const content = fs.readFileSync(path.join(projectRoot, relativePath))
  return [relativePath, crypto.createHash('sha256').update(content).digest('hex')]
}))
fs.writeFileSync(path.join(distRoot, 'build-info.json'), `${JSON.stringify({
  platform: 'mp-weixin',
  builtAt: new Date().toISOString(),
  projectRoot,
  miniprogramRoot: projectConfig.miniprogramRoot || 'dist/',
  sourceHashes
}, null, 2)}\n`)
console.log(`Mini-program build verified: ${path.join(distRoot, 'build-info.json')}`)
