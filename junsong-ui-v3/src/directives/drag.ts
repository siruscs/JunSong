import type { DirectiveBinding, ObjectDirective } from 'vue'

export const vDrag: ObjectDirective = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const handleSelector = binding.arg || '.el-dialog__header'
    const dialogHeaderEl = el.querySelector<HTMLElement>(handleSelector)
    const dragDom = el.querySelector<HTMLElement>('.el-dialog')
    if (!dialogHeaderEl || !dragDom) return

    dialogHeaderEl.style.cursor = 'move'

    const getStyle = (dom: HTMLElement, attr: string): string => {
      if ((document.body as any).currentStyle) {
        return (dom as any).currentStyle[attr]
      }
      return document.defaultView!.getComputedStyle(dom, null).getPropertyValue(attr)
    }

    dialogHeaderEl.onmousedown = (e: MouseEvent) => {
      const disX = e.clientX - dialogHeaderEl.offsetLeft
      const disY = e.clientY - dialogHeaderEl.offsetTop
      const screenWidth = document.body.clientWidth
      const screenHeight = document.documentElement.clientHeight
      const dragDomWidth = dragDom.offsetWidth
      const dragDomHeight = dragDom.offsetHeight
      const minDragDomLeft = dragDom.offsetLeft
      const maxDragDomLeft = screenWidth - dragDom.offsetLeft - dragDomWidth
      const minDragDomTop = dragDom.offsetTop
      const maxDragDomTop = screenHeight - dragDom.offsetTop - dragDomHeight

      let styL = getStyle(dragDom, 'left')
      let styT = getStyle(dragDom, 'top')
      if (styL.includes('%')) {
        styL = String(Math.round(Number(document.body.clientWidth) * (Number(styL.replace(/%/g, '')) / 100)))
        styT = String(Math.round(Number(document.body.clientHeight) * (Number(styT.replace(/%/g, '')) / 100)))
      } else {
        styL = styL.replace(/px/g, '')
        styT = styT.replace(/px/g, '')
      }

      document.onmousemove = function (ev: MouseEvent) {
        let left = ev.clientX - disX
        let top = ev.clientY - disY
        if (-left > minDragDomLeft) left = -minDragDomLeft
        else if (left > maxDragDomLeft) left = maxDragDomLeft
        if (-top > minDragDomTop) top = -minDragDomTop
        else if (top > maxDragDomTop) top = maxDragDomTop
        dragDom.style.left = left + Number(styL) + 'px'
        dragDom.style.top = top + Number(styT) + 'px'
      }

      document.onmouseup = function () {
        document.onmousemove = null
        document.onmouseup = null
      }
    }
  },
}

export default vDrag
