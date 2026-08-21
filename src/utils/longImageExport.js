const CANVAS_ID = 'long-image-export-canvas'
// 与小程序页面保持同一逻辑宽度，导出时使用 2 倍像素密度。
const CANVAS_WIDTH = 375
const MAX_CANVAS_HEIGHT = 12000
const PADDING = 14
const LINE_HEIGHT = 22

const text = (value) => String(value == null ? '-' : value)

function roundedCard(ctx, x, y, width, height, radius = 14) {
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.arcTo(x + width, y, x + width, y + height, radius)
  ctx.arcTo(x + width, y + height, x, y + height, radius)
  ctx.arcTo(x, y + height, x, y, radius)
  ctx.arcTo(x, y, x + width, y, radius)
  ctx.closePath()
  ctx.fill()
}

function drawSegment(ctx, title, subtitle, sections, height, summary) {
  ctx.save()
  ctx.clearRect(0, 0, CANVAS_WIDTH, height)
  ctx.fillStyle = '#F4F7FB'
  ctx.fillRect(0, 0, CANVAS_WIDTH, height)
  const heroHeight = summary ? 184 : 96
  // 微信基础库的 2d Canvas 在隐藏节点中对渐变兼容性不稳定，使用纯色确保文字对比度。
  ctx.fillStyle = '#123F73'
  ctx.globalAlpha = 1
  ctx.globalCompositeOperation = 'source-over'
  // 顶部区域使用直接矩形填充，避免隐藏 Canvas 对边缘圆角路径的兼容问题。
  ctx.fillRect(0, 0, CANVAS_WIDTH, heroHeight)
  ctx.fillStyle = '#FFFFFF'
  ctx.font = '700 17px sans-serif'
  ctx.fillText(text(title), PADDING, 29)
  ctx.font = '11px sans-serif'
  ctx.fillStyle = 'rgba(255,255,255,0.72)'
  ctx.fillText(text(subtitle || ''), PADDING, 47)

  let y = 72
  if (summary) {
    const summaryRows = [
      ['费用', `¥${summary.expenseAmount}（${summary.expenseCount}笔）`],
      ['借支', `¥${summary.advanceAmount}（${summary.advanceCount}笔）`],
      ['差额', `¥${summary.difference}`]
    ]
    summaryRows.forEach(([label, value], index) => {
      ctx.fillStyle = '#FFFFFF'
      ctx.font = `${index === 2 ? '700 ' : ''}${index === 2 ? 16 : 14}px sans-serif`
      ctx.fillText(label, PADDING, y)
      ctx.textAlign = 'right'
      ctx.fillText(value, CANVAS_WIDTH - PADDING, y)
      ctx.textAlign = 'left'
      y += 24
    })
    ctx.fillStyle = 'rgba(255,255,255,0.16)'
    ctx.fillRect(PADDING, y - 16, CANVAS_WIDTH - PADDING * 2, 28)
    ctx.fillStyle = '#FFFFFF'
    ctx.font = '10px sans-serif'
    ctx.fillText(text(summary.explanation), PADDING + 8, y + 2)
    y = heroHeight + 18
  } else {
    y = heroHeight + 18
  }
  const wrapText = (value, maxWidth, font) => {
    const valueText = text(value)
    const lines = []
    let current = ''
    for (const char of valueText) {
      if (current && ctx.measureText(current + char).width > maxWidth) {
        lines.push(current)
        current = ''
      }
      current += char
    }
    if (current || !lines.length) lines.push(current)
    return lines
  }

  sections.forEach((section) => {
    ctx.fillStyle = '#1A2332'
    ctx.font = '700 16px sans-serif'
    ctx.fillText(text(section.title), PADDING, y)
    y += 28
    ;(section.rows || []).forEach((row) => {
      const metaLines = row.meta ? wrapText(row.meta, CANVAS_WIDTH - PADDING * 2 - 24, '11px sans-serif') : []
      const cardHeight = Math.max(66, 43 + Math.min(metaLines.length, 3) * 15)
      ctx.fillStyle = '#FFFFFF'
      roundedCard(ctx, PADDING, y - 18, CANVAS_WIDTH - PADDING * 2, cardHeight, 10)
      ctx.strokeStyle = '#D5E0EC'
      ctx.stroke()
      ctx.fillStyle = '#1A2332'
      ctx.font = '700 14px sans-serif'
      const labelLines = wrapText(row.label, 205, '700 14px sans-serif')
      labelLines.slice(0, 2).forEach((line, index) => ctx.fillText(line, PADDING + 12, y + index * 17))
      ctx.fillStyle = '#0F172A'
      ctx.font = '700 14px sans-serif'
      ctx.textAlign = 'right'
      ctx.fillText(text(row.value), CANVAS_WIDTH - PADDING - 18, y + 2)
      ctx.textAlign = 'left'
      if (metaLines.length) {
        ctx.fillStyle = '#94A3B8'
        ctx.font = '11px sans-serif'
        metaLines.slice(0, 3).forEach((line, index) => ctx.fillText(line, PADDING + 12, y + 23 + index * 15))
      }
      y += cardHeight + 10
    })
    y += 8
  })
  ctx.restore()
  return y
}

function canvasToFile(canvas, sourceWidth, sourceHeight, width, height) {
  return new Promise((resolve, reject) => {
    wx.canvasToTempFilePath({ canvas, x: 0, y: 0, width: sourceWidth, height: sourceHeight, destWidth: width, destHeight: height, success: (res) => resolve(res.tempFilePath), fail: reject })
  })
}

function saveToAlbum(filePath) {
  return new Promise((resolve, reject) => {
    uni.saveImageToPhotosAlbum({ filePath, success: resolve, fail: reject })
  })
}

function withTimeout(promise, message, timeout = 10000) {
  return Promise.race([
    promise,
    new Promise((resolve, reject) => setTimeout(() => reject(new Error(message)), timeout))
  ])
}

async function ensureAlbumPermission() {
  const setting = await new Promise((resolve) => uni.getSetting({ success: resolve, fail: () => resolve({}) }))
  if (setting.authSetting?.['scope.writePhotosAlbum'] === false) {
    throw new Error('请在设置中开启保存到相册权限')
  }
  if (setting.authSetting?.['scope.writePhotosAlbum'] !== true) {
    await new Promise((resolve, reject) => uni.authorize({ scope: 'scope.writePhotosAlbum', success: resolve, fail: reject }))
  }
}

export async function exportLongImage({ title, subtitle, sections = [], summary = null, component = null, wxml = '', style = {} }) {
  try {
    await ensureAlbumPermission()
    if (component?.renderToCanvas) {
      // 画布尺寸经 renderToCanvas({width, height}) 参数下发（style.page 的逻辑尺寸），
      // wxml-to-canvas 在原生实例内部 this.setData 调整逻辑尺寸与 backing buffer。
      // 不要改回页面 Vue prop 绑定：uni-app 会把 props 编译成 u-p 透传，原生
      // Component() 收不到，画布将停留在默认 400×300，导出只剩左上小角。
      const canvasWidth = Number(style?.page?.width) || 375
      const canvasHeight = Number(style?.page?.height) || 0
      let renderError
      for (let attempt = 0; attempt < 3; attempt += 1) {
        try {
          await new Promise((resolve) => setTimeout(resolve, 240 + attempt * 300))
          await withTimeout(Promise.resolve(component.renderToCanvas({ wxml, style, width: canvasWidth, height: canvasHeight })), '导出画布初始化超时', 30000)
          renderError = null
          break
        } catch (error) {
          renderError = error
        }
      }
      if (renderError) throw renderError
      const result = await withTimeout(Promise.resolve(component.canvasToTempFilePath({ fileType: 'png' })), '导出图片生成超时', 30000)
      const filePath = result?.tempFilePath
      if (!filePath) throw new Error('插件未返回导出图片')
      await saveToAlbum(filePath)
      uni.showToast({ title: '图片已保存到相册', icon: 'success' })
      return filePath
    }
    const node = await new Promise((resolve, reject) => {
      wx.createSelectorQuery().select(`#${CANVAS_ID}`).fields({ node: true, size: true }).exec((result) => {
        const item = result?.[0]
        if (!item?.node) return reject(new Error('无法创建导出画布'))
        resolve(item.node)
      })
    })
    const rows = sections.flatMap((section) => [{ label: section.title, value: '' }, ...(section.rows || [])])
    const height = Math.min(MAX_CANVAS_HEIGHT, Math.max(420, (summary ? 210 : 120) + rows.length * 90 + sections.length * 24))
    // 直接使用页面逻辑尺寸，避免真机 2d Canvas 在 scale 后丢失填充层。
    node.width = CANVAS_WIDTH
    node.height = height
    const ctx = node.getContext('2d')
    const drawnHeight = drawSegment(ctx, title, subtitle, sections, height, summary)
    await new Promise((resolve) => setTimeout(resolve, 120))
    const exportHeight = Math.min(MAX_CANVAS_HEIGHT, Math.max(420, drawnHeight))
    const filePath = await canvasToFile(node, CANVAS_WIDTH, exportHeight, CANVAS_WIDTH, exportHeight)
    await saveToAlbum(filePath)
    uni.showToast({ title: '图片已保存到相册', icon: 'success' })
    return filePath
  } catch (error) {
    const message = error?.message || error?.errMsg || (typeof error === 'string' ? error : '插件导出失败')
    console.error('[long-image-export]', error)
    uni.showToast({ title: message, icon: 'none' })
    throw new Error(message)
  }
}
