(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else {
		var a = factory();
		for(var i in a) (typeof exports === 'object' ? exports : root)[i] = a[i];
	}
})(window, function() {
return /******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, { enumerable: true, get: getter });
/******/ 		}
/******/ 	};
/******/
/******/ 	// define __esModule on exports
/******/ 	__webpack_require__.r = function(exports) {
/******/ 		if(typeof Symbol !== 'undefined' && Symbol.toStringTag) {
/******/ 			Object.defineProperty(exports, Symbol.toStringTag, { value: 'Module' });
/******/ 		}
/******/ 		Object.defineProperty(exports, '__esModule', { value: true });
/******/ 	};
/******/
/******/ 	// create a fake namespace object
/******/ 	// mode & 1: value is a module id, require it
/******/ 	// mode & 2: merge all properties of value into the ns
/******/ 	// mode & 4: return value when already ns object
/******/ 	// mode & 8|1: behave like require
/******/ 	__webpack_require__.t = function(value, mode) {
/******/ 		if(mode & 1) value = __webpack_require__(value);
/******/ 		if(mode & 8) return value;
/******/ 		if((mode & 4) && typeof value === 'object' && value && value.__esModule) return value;
/******/ 		var ns = Object.create(null);
/******/ 		__webpack_require__.r(ns);
/******/ 		Object.defineProperty(ns, 'default', { enumerable: true, value: value });
/******/ 		if(mode & 2 && typeof value != 'string') for(var key in value) __webpack_require__.d(ns, key, function(key) { return value[key]; }.bind(null, key));
/******/ 		return ns;
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = 1);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ (function(module, exports) {

const hex = (color) => {
  let result = null

  if (/^#/.test(color) && (color.length === 7 || color.length === 9)) {
    return color
    // eslint-disable-next-line no-cond-assign
  } else if ((result = /^(rgb|rgba)\((.+)\)/.exec(color)) !== null) {
    return '#' + result[2].split(',').map((part, index) => {
      part = part.trim()
      part = index === 3 ? Math.floor(parseFloat(part) * 255) : parseInt(part, 10)
      part = part.toString(16)
      if (part.length === 1) {
        part = '0' + part
      }
      return part
    }).join('')
  } else {
    return '#00000000'
  }
}

const splitLineToCamelCase = (str) => str.split('-').map((part, index) => {
  if (index === 0) {
    return part
  }
  return part[0].toUpperCase() + part.slice(1)
}).join('')

const compareVersion = (v1, v2) => {
  v1 = v1.split('.')
  v2 = v2.split('.')
  const len = Math.max(v1.length, v2.length)
  while (v1.length < len) {
    v1.push('0')
  }
  while (v2.length < len) {
    v2.push('0')
  }
  for (let i = 0; i < len; i++) {
    const num1 = parseInt(v1[i], 10)
    const num2 = parseInt(v2[i], 10)

    if (num1 > num2) {
      return 1
    } else if (num1 < num2) {
      return -1
    }
  }

  return 0
}

module.exports = {
  hex,
  splitLineToCamelCase,
  compareVersion
}


/***/ }),
/* 1 */
/***/ (function(module, exports, __webpack_require__) {


const xmlParse = __webpack_require__(2)
const {Widget} = __webpack_require__(3)
const {Draw} = __webpack_require__(5)
const {compareVersion} = __webpack_require__(0)

const canvasId = 'weui-canvas'

// 真身实例登记：attached 由微信运行时调用，this 一定是原生组件真身。
// uni-app Vue3 下页面无论通过 $refs 还是 selectComponent 拿到的组件引用，
// 在其上调用组件方法时 this 可能是包装 Proxy —— 包装上的 setData 会触发
// __treeManager__ undefined（真机复现，基础库 3.16.1）。因此所有公开方法
// 入口统一把 this 换回真身 host 再执行，与调用方解耦。
let nativeHost = null

Component({
  properties: {
    width: {
      type: Number,
      value: 400
    },
    height: {
      type: Number,
      value: 300
    }
  },
  data: {
    // 2d 画布为主路径：buffer 尺寸由 node.width/node.height 显式控制（逻辑 × exportDpr），
    // ctx.scale 显式设置，导出整画布 1:1 —— 全链路无隐式 DPR 约定，长/短内容行为一致。
    // legacy canvas-id 仅作 2d 节点不可用时的降级路径。
    use2dCanvas: true,
    // legacy 降级路径的 canvas width/height 属性（物理像素），由 _applySize 同步。
    bufferWidth: 400,
    bufferHeight: 300
  },
  // 说明：不要通过 Vue prop（:width/:height）给本组件传尺寸。uni-app Vue3 会把页面模板
  // 里的组件 props 编译成 u-p="{{F}}" 序列化透传，而本组件是原生 Component()，不认识 u-p，
  // 属性永远不会更新（observers 也不会触发）——画布停留在默认 400×300，导出只剩左上小角。
  // 正确路径：页面用 selectComponent('.export-canvas') 拿到原生实例，调用
  // renderToCanvas({wxml, style, width, height})，由组件内部 this.setData 完成调尺寸。
  lifetimes: {
    attached() {
      // 登记真身：后续任何包装 Proxy 调用本组件方法，方法内部都换回真身执行。
      nativeHost = this
      this._ready = new Promise((resolve, reject) => {
        this._resolveReady = resolve
        this._rejectReady = reject
      })
      const {pixelRatio: dpr} = wx.getSystemInfoSync()
      this.dpr = dpr
      // 初始 buffer 按 dpr；后续 renderToCanvas 会按内容尺寸调用 _applySize 重算。
      const initWidth = Number(this.data.width) || 0
      const initHeight = Number(this.data.height) || 0
      const size = this._computeExportSize(initWidth, initHeight)
      this.exportDpr = size.exportDpr
      this.setData({bufferWidth: size.bufferWidth, bufferHeight: size.bufferHeight}, () => {
        // 探测 2d 节点可用性（基础库 ≥ 2.9.0）。2d 路径的 buffer 与 scale 全部显式
        // 控制，不存在 legacy canvas-id 的隐式 DPR buffer 语义（该隐式约定在长短
        // 内容下行为不一致，曾导致短内容导出放大且不完整）。
        this.createSelectorQuery()
          .select(`#${canvasId}`)
          .fields({node: true})
          .exec(res => {
            if (res && res[0] && res[0].node) {
              this.canvas = res[0].node
              this._resolveReady()
              return
            }
            // 降级 legacy canvas-id：context 重建 + scale 交给 renderToCanvas。
            this.setData({use2dCanvas: false}, () => {
              this.ctx = wx.createCanvasContext(canvasId, this)
              if (this.exportDpr > 1) this.ctx.scale(this.exportDpr, this.exportDpr)
              this._resolveReady()
            })
          })
      })
    },
    detached() {
      if (nativeHost === this) nativeHost = null
    }
  },
  methods: {
    // 解析真身：调用方（页面）拿到的可能是 uni-app 包装 Proxy，其 setData 会报
    // __treeManager__ undefined。注意不能用标记位判断 —— 包装 Proxy 会把属性读取
    // 转发到真身，标记位经 Proxy 读出来同样是 true，会误判“this 即真身”后照样在
    // Proxy 上调用 setData（真机 3.16.1 复现）。恒等比较（===）无法被 Proxy 转发，
    // 是唯一可靠的判据。
    _resolveHost() {
      if (nativeHost && this === nativeHost) return nativeHost
      return nativeHost || this
    },

    // 计算 width/height（逻辑尺寸）对应的导出像素密度与 backing buffer 尺寸。
    // 横纵使用同一 exportDpr：内容超长时自动降密度，保证 buffer 边长不超过设备
    // Canvas 上限（超限会被 native 画布静默截断，表现为导出图只剩顶部一小段）。
    _computeExportSize(width, height) {
      const MAX_BUFFER_SIDE = 8192
      const host = this._resolveHost()
      const dpr = Number(host.dpr) || Number(wx.getSystemInfoSync && wx.getSystemInfoSync().pixelRatio) || 1
      let exportDpr
      if (height * dpr > MAX_BUFFER_SIDE) {
        exportDpr = Math.max(1, Math.floor(MAX_BUFFER_SIDE / height))
      } else {
        exportDpr = dpr
      }
      const bufferWidth = Math.round(width * exportDpr)
      const bufferHeight = Math.round(height * exportDpr)
      return {exportDpr, bufferWidth, bufferHeight}
    },

    // 通过真身 setData 调整画布逻辑尺寸与 backing buffer（必须用真身调用）。
    _applySize(width, height) {
      const host = this._resolveHost()
      const targetWidth = Math.round(Number(width) || 0)
      const targetHeight = Math.round(Number(height) || 0)
      if (!targetWidth || !targetHeight) return Promise.resolve(false)
      const {exportDpr, bufferWidth, bufferHeight} = host._computeExportSize(targetWidth, targetHeight)
      const unchanged = (
        Number(host.data.width) === targetWidth &&
        Number(host.data.height) === targetHeight &&
        Number(host.data.bufferWidth) === bufferWidth &&
        Number(host.data.bufferHeight) === bufferHeight
      )
      if (unchanged) return Promise.resolve(false)
      host.exportDpr = exportDpr
      return new Promise((resolve) => {
        host.setData({
          width: targetWidth,
          height: targetHeight,
          bufferWidth,
          bufferHeight
        }, () => resolve(true))
      })
    },

    // 查询 2d canvas 节点（组件作用域）。返回 {node, width, height} 或 null。
    _query2dNode() {
      const host = this._resolveHost()
      return new Promise((resolve) => {
        host.createSelectorQuery()
          .select(`#${canvasId}`)
          .fields({node: true, size: true})
          .exec(res => {
            const item = res && res[0]
            if (item && item.node) resolve(item)
            else resolve(null)
          })
      })
    },

    async renderToCanvas(args) {
      const host = this._resolveHost()
      if (host._ready) await host._ready
      const {wxml, style, width, height} = args || {wxml: '', style: {}}
      // 尺寸调整：args 里的 width/height（来自 exportStyle().page）通过真身 setData
      // 一次性下发逻辑尺寸 + legacy buffer，随后等渲染层布局生效。
      if (width && height) {
        const sizeChanged = await host._applySize(width, height)
        if (sizeChanged) {
          await new Promise(r => setTimeout(r, 32))
        }
      }
      const layoutWidth = Number(host.data.width) || Number(width) || 375
      const layoutHeight = Number(host.data.height) || Number(height) || 600
      const exportDpr = Number(host.exportDpr) || Number(host.dpr) || 1

      let ctx = host.ctx
      let canvas = host.canvas
      let use2dCanvas = host.data.use2dCanvas

      if (use2dCanvas) {
        // 2d 主路径：布局落地确认 + 显式设置 backing buffer + 显式 scale。
        // node.width 赋值会重置画布状态（含 transform），之后统一 getContext + scale。
        // 用 fields 查询返回的布局宽高校验 setData 的逻辑尺寸已生效，未生效则
        // 短重试（布局未落地时设 buffer 会按旧布局换算，导出必然错位）。
        let item = null
        for (let i = 0; i < 4; i += 1) {
          item = await host._query2dNode()
          if (item && Math.round(Number(item.width)) === layoutWidth) break
          await new Promise(r => setTimeout(r, 50))
        }
        if (item && item.node) {
          canvas = host.canvas = item.node
          canvas.width = Math.round(layoutWidth * exportDpr)
          canvas.height = Math.round(layoutHeight * exportDpr)
          ctx = host.ctx = canvas.getContext('2d')
          ctx.scale(exportDpr, exportDpr)
        } else {
          // 2d 节点不可用 → 降级 legacy canvas-id。
          use2dCanvas = false
          host.setData({use2dCanvas: false})
          await new Promise(r => setTimeout(r, 50))
        }
      }

      if (!use2dCanvas) {
        // legacy 降级：每次渲染重建 context 并重新 scale（ctx.draw() 会清空动作队列，
        // attached 里的一次性 scale 在第二次导出会丢失）。
        ctx = host.ctx = wx.createCanvasContext(canvasId, host)
        const scale = Number(host.exportDpr) || Number(host.dpr) || 1
        if (scale > 1) ctx.scale(scale, scale)
      }

      ctx.clearRect(0, 0, layoutWidth, layoutHeight)
      const {root: xom} = xmlParse(wxml)

      const widget = new Widget(xom, style)
      const container = widget.init()
      host.boundary = {
        top: container.layoutBox.top || 0,
        left: container.layoutBox.left || 0,
        width: container.computedStyle.width || layoutWidth,
        height: container.computedStyle.height || layoutHeight,
        _layoutHeight: layoutHeight,
        _layoutWidth: layoutWidth,
      }
      const draw = new Draw(ctx, canvas, use2dCanvas)
      // wxml-to-canvas 只支持纯色 backgroundColor，但 ctx.fill() 可直接接受
      // CanvasGradient 对象。这里扩展 drawView：样式带 backgroundGradient 时按
      // 135deg 对角线（左上→右下）渐变填充圆角矩形，与小程序页面
      // linear-gradient(135deg, ...) 视觉一致；其余节点走库原逻辑。
      // 说明：backgroundGradient 是自定义样式属性，widget-ui 的布局引擎只处理
      // 已知布局属性，未知属性会原样保留在 computedStyle 中传到 drawView。
      const rawDrawView = draw.drawView.bind(draw)
      draw.drawView = (box, style) => {
        const gradient = style && style.backgroundGradient
        const stops = gradient && Array.isArray(gradient.colors)
          ? gradient.colors.filter(stop => stop && stop.color)
          : null
        if (!stops || !stops.length || typeof ctx.createLinearGradient !== 'function') {
          return rawDrawView(box, style)
        }
        const {left: x, top: y, width: w, height: h} = box
        const radius = Math.min(Number(style.borderRadius) || 0, w / 2, h / 2)
        const fill = ctx.createLinearGradient(x, y, x + w, y + h)
        stops.forEach(stop => {
          fill.addColorStop(Math.min(Math.max(Number(stop.offset) || 0, 0), 1), stop.color)
        })
        ctx.save()
        ctx.fillStyle = fill
        // 圆角路径与 Draw.roundRect 相同的圆弧算法
        ctx.beginPath()
        ctx.arc(x + radius, y + radius, radius, Math.PI, Math.PI * 1.5)
        ctx.arc(x + w - radius, y + radius, radius, Math.PI * 1.5, 0)
        ctx.arc(x + w - radius, y + h - radius, radius, 0, Math.PI * 0.5)
        ctx.arc(x + radius, y + h - radius, radius, Math.PI * 0.5, Math.PI)
        ctx.lineTo(x, y + radius)
        ctx.fill()
        ctx.restore()
      }
      await draw.drawNode(container)

      if (!use2dCanvas) {
        await host.canvasDraw(ctx)
        // legacy canvas：draw() 落盘后再等 1 帧让 native canvas 内容完成合并。
        await new Promise(r => setTimeout(r, 16))
      }
      return Promise.resolve(container)
    },

    canvasDraw(ctx, reserve) {
      return new Promise(resolve => {
        ctx.draw(reserve, () => {
          resolve()
        })
      })
    },

    canvasToTempFilePath(args = {}) {
      const host = this._resolveHost()
      const use2dCanvas = host.data.use2dCanvas

      // 2d 主路径：整画布导出。不传 x/y/width/height —— 2d 的导出坐标系是
      // 物理像素（node.width/height），传逻辑尺寸会只截取左上 1/exportDpr 区域，
      // 导出图表现为“放大且不完整”。destWidth/destHeight = buffer 尺寸，1:1 输出。
      if (use2dCanvas) {
        const exportWithNode = (node) => {
          if (!node) {
            return Promise.reject(new Error('导出 Canvas 实例未初始化'))
          }
          return new Promise((resolve, reject) => {
            wx.canvasToTempFilePath({
              canvas: node,
              destWidth: node.width,
              destHeight: node.height,
              fileType: args.fileType || 'png',
              quality: args.quality || 1,
              success: resolve,
              fail: reject
            })
          })
        }
        if (host.canvas) return exportWithNode(host.canvas)
        return host._query2dNode().then(item => exportWithNode(item ? item.node : null))
      }

      // legacy 降级路径：x/y/w/h 为逻辑（CSS）单位，与绘制坐标系一致。
      return new Promise((resolve, reject) => {
        const boundary = host.boundary || {
          top: 0,
          left: 0,
          width: host.data.width,
          height: host.data.height
        }
        // 横纵使用同一 exportDpr 比例，不混合使用 dpr 与 exportDpr。
        const exportDpr = Number(host.exportDpr) || Number(host.dpr) || 1

        // 使用 canvas 逻辑全尺寸作为导出源区域，而不是 css-layout 引擎
        // 计算的 boundary.width/height（它在 flex + marginTop 场景下会低估）。
        const top = Number(boundary.top) || 0
        const left = Number(boundary.left) || 0
        const srcW = Number(boundary._layoutWidth) || Number(host.data.width) || 375
        const srcH = Number(boundary._layoutHeight) || Number(host.data.height) || 400

        // legacy 路径必须传入真身组件实例，限定 canvas-id 的查找范围。
        wx.canvasToTempFilePath({
          x: left,
          y: top,
          width: srcW,
          height: srcH,
          destWidth: Math.round(srcW * exportDpr),
          destHeight: Math.round(srcH * exportDpr),
          canvasId,
          fileType: args.fileType || 'png',
          quality: args.quality || 1,
          success: resolve,
          fail: reject
        }, host)
      })
    }
  }
})


/***/ }),
/* 2 */
/***/ (function(module, exports) {


/**
 * Module dependencies.
 */


/**
 * Expose `parse`.
 */


/**
 * Parse the given string of `xml`.
 *
 * @param {String} xml
 * @return {Object}
 * @api public
 */

function parse(xml) {
  xml = xml.trim()

  // strip comments
  xml = xml.replace(/<!--[\s\S]*?-->/g, '')

  return document()

  /**
   * XML document.
   */

  function document() {
    return {
      declaration: declaration(),
      root: tag()
    }
  }

  /**
   * Declaration.
   */

  function declaration() {
    const m = match(/^<\?xml\s*/)
    if (!m) return

    // tag
    const node = {
      attributes: {}
    }

    // attributes
    while (!(eos() || is('?>'))) {
      const attr = attribute()
      if (!attr) return node
      node.attributes[attr.name] = attr.value
    }

    match(/\?>\s*/)

    return node
  }

  /**
   * Tag.
   */

  function tag() {
    const m = match(/^<([\w-:.]+)\s*/)
    if (!m) return

    // name
    const node = {
      name: m[1],
      attributes: {},
      children: []
    }

    // attributes
    while (!(eos() || is('>') || is('?>') || is('/>'))) {
      const attr = attribute()
      if (!attr) return node
      node.attributes[attr.name] = attr.value
    }

    // self closing tag
    if (match(/^\s*\/>\s*/)) {
      return node
    }

    match(/\??>\s*/)

    // content
    node.content = content()

    // children
    let child
    while (child = tag()) {
      node.children.push(child)
    }

    // closing
    match(/^<\/[\w-:.]+>\s*/)

    return node
  }

  /**
   * Text content.
   */

  function content() {
    const m = match(/^([^<]*)/)
    if (m) return m[1]
    return ''
  }

  /**
   * Attribute.
   */

  function attribute() {
    const m = match(/([\w:-]+)\s*=\s*("[^"]*"|'[^']*'|\w+)\s*/)
    if (!m) return
    return {name: m[1], value: strip(m[2])}
  }

  /**
   * Strip quotes from `val`.
   */

  function strip(val) {
    return val.replace(/^['"]|['"]$/g, '')
  }

  /**
   * Match `re` and advance the string.
   */

  function match(re) {
    const m = xml.match(re)
    if (!m) return
    xml = xml.slice(m[0].length)
    return m
  }

  /**
   * End-of-source.
   */

  function eos() {
    return xml.length == 0
  }

  /**
   * Check for `prefix`.
   */

  function is(prefix) {
    return xml.indexOf(prefix) == 0
  }
}

module.exports = parse


/***/ }),
/* 3 */
/***/ (function(module, exports, __webpack_require__) {

const Block = __webpack_require__(4)
const {splitLineToCamelCase} = __webpack_require__(0)

class Element extends Block {
  constructor(prop) {
    super(prop.style)
    this.name = prop.name
    this.attributes = prop.attributes
  }
}


class Widget {
  constructor(xom, style) {
    this.xom = xom
    this.style = style

    this.inheritProps = ['fontSize', 'lineHeight', 'textAlign', 'verticalAlign', 'color']
  }

  init() {
    this.container = this.create(this.xom)
    this.container.layout()

    this.inheritStyle(this.container)
    return this.container
  }

  // 继承父节点的样式
  inheritStyle(node) {
    const parent = node.parent || null
    const children = node.children || {}
    const computedStyle = node.computedStyle

    if (parent) {
      this.inheritProps.forEach(prop => {
        computedStyle[prop] = computedStyle[prop] || parent.computedStyle[prop]
      })
    }

    Object.values(children).forEach(child => {
      this.inheritStyle(child)
    })
  }

  create(node) {
    let classNames = (node.attributes.class || '').split(' ')
    classNames = classNames.map(item => splitLineToCamelCase(item.trim()))
    const style = {}
    classNames.forEach(item => {
      Object.assign(style, this.style[item] || {})
    })

    const args = {name: node.name, style}

    const attrs = Object.keys(node.attributes)
    const attributes = {}
    for (const attr of attrs) {
      const value = node.attributes[attr]
      const CamelAttr = splitLineToCamelCase(attr)

      if (value === '' || value === 'true') {
        attributes[CamelAttr] = true
      } else if (value === 'false') {
        attributes[CamelAttr] = false
      } else {
        attributes[CamelAttr] = value
      }
    }
    attributes.text = node.content
    args.attributes = attributes
    const element = new Element(args)
    node.children.forEach(childNode => {
      const childElement = this.create(childNode)
      element.add(childElement)
    })
    return element
  }
}

module.exports = {Widget}


/***/ }),
/* 4 */
/***/ (function(module, exports) {

module.exports = require("widget-ui");

/***/ }),
/* 5 */
/***/ (function(module, exports) {

class Draw {
  constructor(context, canvas, use2dCanvas = false) {
    this.ctx = context
    this.canvas = canvas || null
    this.use2dCanvas = use2dCanvas
  }

  roundRect(x, y, w, h, r, fill = true, stroke = false) {
    if (r < 0) return
    const ctx = this.ctx

    ctx.beginPath()
    ctx.arc(x + r, y + r, r, Math.PI, Math.PI * 3 / 2)
    ctx.arc(x + w - r, y + r, r, Math.PI * 3 / 2, 0)
    ctx.arc(x + w - r, y + h - r, r, 0, Math.PI / 2)
    ctx.arc(x + r, y + h - r, r, Math.PI / 2, Math.PI)
    ctx.lineTo(x, y + r)
    if (stroke) ctx.stroke()
    if (fill) ctx.fill()
  }

  drawView(box, style) {
    const ctx = this.ctx
    const {
      left: x, top: y, width: w, height: h
    } = box
    const {
      borderRadius = 0,
      borderWidth = 0,
      borderColor,
      color = '#000',
      backgroundColor = 'transparent',
    } = style
    ctx.save()
    // 外环
    if (borderWidth > 0) {
      ctx.fillStyle = borderColor || color
      this.roundRect(x, y, w, h, borderRadius)
    }

    // 内环
    ctx.fillStyle = backgroundColor
    const innerWidth = w - 2 * borderWidth
    const innerHeight = h - 2 * borderWidth
    const innerRadius = borderRadius - borderWidth >= 0 ? borderRadius - borderWidth : 0
    this.roundRect(x + borderWidth, y + borderWidth, innerWidth, innerHeight, innerRadius)
    ctx.restore()
  }

  async drawImage(img, box, style) {
    await new Promise((resolve, reject) => {
      const ctx = this.ctx
      const canvas = this.canvas

      const {
        borderRadius = 0
      } = style
      const {
        left: x, top: y, width: w, height: h
      } = box
      ctx.save()
      this.roundRect(x, y, w, h, borderRadius, false, false)
      ctx.clip()

      const _drawImage = (img) => {
        if (this.use2dCanvas) {
          const Image = canvas.createImage()
          Image.onload = () => {
            ctx.drawImage(Image, x, y, w, h)
            ctx.restore()
            resolve()
          }
          Image.onerror = () => { reject(new Error(`createImage fail: ${img}`)) }
          Image.src = img
        } else {
          ctx.drawImage(img, x, y, w, h)
          ctx.restore()
          resolve()
        }
      }

      const isTempFile = /^wxfile:\/\//.test(img)
      const isNetworkFile = /^https?:\/\//.test(img)

      if (isTempFile) {
        _drawImage(img)
      } else if (isNetworkFile) {
        wx.downloadFile({
          url: img,
          success(res) {
            if (res.statusCode === 200) {
              _drawImage(res.tempFilePath)
            } else {
              reject(new Error(`downloadFile:fail ${img}`))
            }
          },
          fail() {
            reject(new Error(`downloadFile:fail ${img}`))
          }
        })
      } else {
        reject(new Error(`image format error: ${img}`))
      }
    })
  }

  // eslint-disable-next-line complexity
  drawText(text, box, style) {
    const ctx = this.ctx
    let {
      left: x, top: y, width: w, height: h
    } = box
    let {
      color = '#000',
      lineHeight = '1.4em',
      fontSize = 14,
      textAlign = 'left',
      verticalAlign = 'top',
      backgroundColor = 'transparent'
    } = style

    if (typeof lineHeight === 'string') { // 2em
      lineHeight = Math.ceil(parseFloat(lineHeight.replace('em')) * fontSize)
    }
    if (!text || (lineHeight > h)) return

    ctx.save()
    ctx.textBaseline = 'top'
    ctx.font = `${fontSize}px sans-serif`
    ctx.textAlign = textAlign

    // 背景色
    ctx.fillStyle = backgroundColor
    this.roundRect(x, y, w, h, 0)

    // 文字颜色
    ctx.fillStyle = color

    // 水平布局
    switch (textAlign) {
      case 'left':
        break
      case 'center':
        x += 0.5 * w
        break
      case 'right':
        x += w
        break
      default: break
    }

    const textWidth = ctx.measureText(text).width
    const actualHeight = Math.ceil(textWidth / w) * lineHeight
    let paddingTop = Math.ceil((h - actualHeight) / 2)
    if (paddingTop < 0) paddingTop = 0

    // 垂直布局
    switch (verticalAlign) {
      case 'top':
        break
      case 'middle':
        y += paddingTop
        break
      case 'bottom':
        y += 2 * paddingTop
        break
      default: break
    }

    const inlinePaddingTop = Math.ceil((lineHeight - fontSize) / 2)

    // 不超过一行
    if (textWidth <= w) {
      ctx.fillText(text, x, y + inlinePaddingTop)
      return
    }

    // 多行文本
    const chars = text.split('')
    const _y = y

    // 逐行绘制
    let line = ''
    for (const ch of chars) {
      const testLine = line + ch
      const testWidth = ctx.measureText(testLine).width

      if (testWidth > w) {
        ctx.fillText(line, x, y + inlinePaddingTop)
        y += lineHeight
        line = ch
        if ((y + lineHeight) > (_y + h)) break
      } else {
        line = testLine
      }
    }

    // 避免溢出
    if ((y + lineHeight) <= (_y + h)) {
      ctx.fillText(line, x, y + inlinePaddingTop)
    }
    ctx.restore()
  }

  async drawNode(element) {
    const {layoutBox, computedStyle, name} = element
    const {src, text} = element.attributes
    if (name === 'view') {
      this.drawView(layoutBox, computedStyle)
    } else if (name === 'image') {
      await this.drawImage(src, layoutBox, computedStyle)
    } else if (name === 'text') {
      this.drawText(text, layoutBox, computedStyle)
    }
    const childs = Object.values(element.children)
    for (const child of childs) {
      await this.drawNode(child)
    }
  }
}


module.exports = {
  Draw
}


/***/ })
/******/ ]);
});
