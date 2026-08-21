import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import fs from 'node:fs'
import path from 'node:path'

function patchUniRuntimeSystemInfo(code) {
  return code
    .replaceAll('wx.getSystemInfoSync()', '({})')
    .replaceAll('bn.getSystemInfoSync()', '({})')
}

function stripSystemInfoApis(code) {
  return patchUniRuntimeSystemInfo(code)
    .replace(
      /bn\.getAppBaseInfo&&bn\.getAppBaseInfo\(\)\|\|\(bn\.getAppBaseInfo=bn\.getSystemInfoSync\),bn\.getWindowInfo&&bn\.getWindowInfo\(\)\|\|\(bn\.getWindowInfo=bn\.getSystemInfoSync\),bn\.getDeviceInfo&&bn\.getDeviceInfo\(\)\|\|\(bn\.getDeviceInfo=bn\.getSystemInfoSync\);let ([a-zA-Z_$][\w$]*)=bn\.getAppBaseInfo&&bn\.getAppBaseInfo\(\);\1\|\|\(\1=\(\{\}\)\);/,
      'let $1=({});'
    )
    .replace(
      /\(null===\(([a-zA-Z_$][\w$]*)=wx\.getAppBaseInfo\)\|\|void 0===\1\?void 0:\1\.call\(wx\)\)\|\|\(?\{\}\)?/g,
      '({})'
    )
    .replace(
      /\(null===\(([a-zA-Z_$][\w$]*)=wx\.getWindowInfo\)\|\|void 0===\1\?void 0:\1\.call\(wx\)\)\|\|\(?\{\}\)?/g,
      '({})'
    )
    .replace(
      /\(null===\(([a-zA-Z_$][\w$]*)=wx\.getDeviceInfo\)\|\|void 0===\1\?void 0:\1\.call\(wx\)\)\|\|\(?\{\}\)?/g,
      '({})'
    )
}

function readJsonFile(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function writeJsonFile(filePath, value) {
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`)
}

function patchGeneratedProjectConfig(outDir) {
  const generatedProjectConfigPath = path.resolve(outDir, 'project.config.json')
  const sourceProjectConfigPath = path.resolve(process.cwd(), 'project.config.json')

  if (!fs.existsSync(generatedProjectConfigPath) || !fs.existsSync(sourceProjectConfigPath)) {
    return
  }

  const generatedProjectConfig = readJsonFile(generatedProjectConfigPath)
  const sourceProjectConfig = readJsonFile(sourceProjectConfigPath)
  if (!sourceProjectConfig.libVersion || generatedProjectConfig.libVersion === sourceProjectConfig.libVersion) {
    return
  }

  generatedProjectConfig.libVersion = sourceProjectConfig.libVersion
  writeJsonFile(generatedProjectConfigPath, generatedProjectConfig)
}

function patchGeneratedPrivateProjectConfig(outDir) {
  const generatedPrivateConfigPath = path.resolve(outDir, 'project.private.config.json')
  const sourcePrivateConfigPath = path.resolve(process.cwd(), 'project.private.config.json')

  if (!fs.existsSync(sourcePrivateConfigPath)) {
    return
  }

  const privateConfig = readJsonFile(sourcePrivateConfigPath)
  privateConfig.setting = privateConfig.setting || {}
  privateConfig.setting.useApiHook = false
  writeJsonFile(generatedPrivateConfigPath, privateConfig)
}

function patchUniMpWeixinRuntime() {
  return {
    name: 'patch-uni-mp-weixin-runtime',
    enforce: 'pre',
    transform(code, id) {
      if (!id.includes('@dcloudio/uni-mp-weixin/dist')) {
        return null
      }
      if (!id.endsWith('uni.mp.esm.js')) {
        return null
      }
      let patched = stripSystemInfoApis(code)
      if (patched.includes('preloadAsset();')) {
        patched = patched.replace('preloadAsset();', '/* preloadAsset disabled: avoids idle wx.preloadAssets timeout in WeChat devtools */')
      }
      return patched === code ? null : patched
    },
    generateBundle(_, bundle) {
      for (const chunk of Object.values(bundle)) {
        if (chunk.type === 'chunk') {
          chunk.code = stripSystemInfoApis(chunk.code)
        }
      }
    },
    writeBundle(options) {
      const outDir = options.dir || path.resolve(process.cwd(), 'dist/build/mp-weixin')
      const tabSourceDir = path.join(process.cwd(), 'src/static/tab')
      const tabTargetDir = path.join(process.cwd(), 'dist/static/tab')
      fs.mkdirSync(tabTargetDir, { recursive: true })
      for (const icon of ['home.png', 'home-active.png', 'grid.png', 'grid-active.png', 'user.png', 'user-active.png']) {
        const source = path.join(tabSourceDir, icon)
        if (fs.existsSync(source)) fs.copyFileSync(source, path.join(tabTargetDir, icon))
      }
      // 复制 logo.png 到输出目录
      const logoSource = path.join(process.cwd(), 'src/static/logo.png')
      const logoTarget = path.join(process.cwd(), 'dist/static/logo.png')
      if (fs.existsSync(logoSource)) fs.copyFileSync(logoSource, logoTarget)
      // wxml-to-canvas 是原生小程序组件，Vite 不会自动复制 src/components 下的非 Vue 组件文件。
      // 页面配置已声明该组件，因此必须把完整运行时（wxml/js/wxss/json/utils）同步到最终产物。
      const exportComponentSourceDir = path.join(process.cwd(), 'src/components/WxmlToCanvas')
      const exportComponentTargetDir = path.join(process.cwd(), 'dist/components/WxmlToCanvas')
      if (fs.existsSync(exportComponentSourceDir)) {
        fs.cpSync(exportComponentSourceDir, exportComponentTargetDir, { recursive: true })
      }
      const exportComponentRuntime = path.join(exportComponentTargetDir, 'index.js')
      const widgetUiRuntime = path.join(process.cwd(), 'node_modules/widget-ui/dist/index.js')
      if (fs.existsSync(exportComponentRuntime) && fs.existsSync(widgetUiRuntime)) {
        const componentCode = fs.readFileSync(exportComponentRuntime, 'utf8')
        const widgetUiCode = fs.readFileSync(widgetUiRuntime, 'utf8')
        const embeddedWidgetUi = `(function () { var widgetModule = { exports: {} }; var widgetExports = widgetModule.exports; var module = widgetModule; var exports = widgetExports; ${widgetUiCode}; return widgetModule.exports }())`
        fs.writeFileSync(exportComponentRuntime, componentCode.replace('module.exports = require("widget-ui");', `module.exports = ${embeddedWidgetUi};`))
      }
      for (const vendorPath of [
        path.join(outDir, 'common/vendor.js'),
        path.resolve(process.cwd(), 'dist/common/vendor.js')
      ]) {
        if (!fs.existsSync(vendorPath)) {
          continue
        }
        const code = fs.readFileSync(vendorPath, 'utf8')
        const patched = stripSystemInfoApis(code)
        if (patched !== code) {
          fs.writeFileSync(vendorPath, patched)
        }
      }
      patchGeneratedProjectConfig(path.resolve(process.cwd(), 'dist'))
      patchGeneratedPrivateProjectConfig(path.resolve(process.cwd(), 'dist'))
    }
  }
}

export default defineConfig({
  plugins: [patchUniMpWeixinRuntime(), uni()]
})
