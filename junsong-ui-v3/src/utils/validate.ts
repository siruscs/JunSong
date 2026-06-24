export function isEmpty(value: any): boolean {
  return value == null || value === '' || value === undefined
}

export function isPathMatch(pattern: string, path: string): boolean {
  if (pattern === path) return true
  const regex = new RegExp('^' + pattern.replace(/:[^/]+/g, '[^/]+') + '$')
  return regex.test(path)
}

export function isExternal(path: string): boolean {
  return /^(https?:|mailto:|tel:|\/\/)/.test(path)
}
