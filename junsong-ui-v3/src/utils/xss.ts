/**
 * HTML 内容消毒工具
 * 移除危险标签和事件处理器，防止 XSS 攻击
 */

const DANGEROUS_TAGS = [
  'script', 'iframe', 'object', 'embed', 'form',
  'input', 'textarea', 'select', 'style', 'link',
  'meta', 'base', 'head',
]

const DANGEROUS_ATTRS = [
  /^on\w+$/i,           // onclick, onerror, onload...
  /^javascript:/i,      // javascript: 协议
  /^data:text\/html/i, // data URI
]

/**
 * 对 HTML 字符串进行安全消毒
 * @param html 原始 HTML 字符串
 * @returns 消毒后的安全 HTML
 */
export function sanitizeHtml(html: string | undefined | null): string {
  if (!html) return ''

  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  const body = doc.body

  // 递归清理节点
  function cleanNode(node: Node) {
    if (node.nodeType === Node.ELEMENT_NODE) {
      const el = node as Element
      const tagName = el.tagName.toLowerCase()

      // 移除危险标签
      if (DANGEROUS_TAGS.includes(tagName)) {
        el.remove()
        return
      }

      // 清理危险属性
      Array.from(el.attributes).forEach((attr) => {
        const name = attr.name.toLowerCase()
        const value = attr.value
        for (const pattern of DANGEROUS_ATTRS) {
          if (pattern.test(name) || pattern.test(value)) {
            el.removeAttribute(attr.name)
            break
          }
        }
      })
    }

    // 递归处理子节点（倒序遍历，避免删除影响索引）
    const children = Array.from(node.childNodes)
    for (let i = children.length - 1; i >= 0; i--) {
      cleanNode(children[i])
    }
  }

  cleanNode(body)
  return body.innerHTML
}
