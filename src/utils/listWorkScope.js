import { workContext } from './workContext.js'

function normalizedId(value) {
  return value === undefined || value === null ? null : String(value)
}

function normalizedDept(dept) {
  if (!dept) return null
  const id = dept.id ?? dept.deptId
  const name = dept.name || dept.deptName || ''
  return id === undefined || id === null || !name ? null : { id, name }
}

export function resolveListWorkScope(snapshot = {}, previousDeptId = null) {
  const depts = (Array.isArray(snapshot.depts) ? snapshot.depts : []).map(normalizedDept).filter(Boolean)
  const requestedId = snapshot.currentDeptId ?? normalizedDept(snapshot.currentDept)?.id ?? null
  const current = depts.find((dept) => normalizedId(dept.id) === normalizedId(requestedId)) || null
  const currentDeptId = current?.id ?? null
  const currentDeptName = current?.name || '未选择部门'
  const deptCount = depts.length
  const scopeLabel = currentDeptId === null
    ? '暂无可用数据范围'
    : deptCount > 1 ? `当前部门 · 共 ${deptCount} 个部门` : '当前数据范围'

  return {
    currentDeptId,
    currentDeptName,
    scopeLabel,
    contextVersion: Number(snapshot.version) || 0,
    departmentChanged: normalizedId(previousDeptId) !== normalizedId(currentDeptId)
  }
}

export function canRequestListScope(deptId) {
  return deptId !== undefined && deptId !== null && String(deptId).trim() !== ''
}

export function shouldRestoreListPage(loaded, requestStillCurrent) {
  return !loaded && requestStillCurrent
}

function shortToast(title, icon = 'none') {
  try { uni.showToast({ title, icon, duration: 2200 }) } catch (_) { /* ignore */ }
}

/**
 * 将 work-scope 显示字段统一注入到目标 Vue 页面实例 data 中。
 * 使用方式：data 里声明 { currentDeptId: null, currentDeptName: '未选择部门', scopeLabel: '暂无可用数据范围', contextVersion: 0 }，
 * 然后在 onLoad / onShow 中调用 applyWorkScopeToPage(this)，返回 departmentChanged 供调用方判断是否清空列表。
 */
export function applyWorkScopeToPage(vm) {
  if (!vm) return { departmentChanged: false }
  const prior = vm.currentDeptId !== undefined ? vm.currentDeptId : null
  const scope = resolveListWorkScope(workContext.snapshot(), prior)
  vm.currentDeptId = scope.currentDeptId
  vm.currentDeptName = scope.currentDeptName
  vm.scopeLabel = scope.scopeLabel
  vm.contextVersion = scope.contextVersion
  return { departmentChanged: scope.departmentChanged, contextVersion: scope.contextVersion }
}

/**
 * 打开 DeptSwitcher 组件显示部门切换列表（点击式 sheet，与首页/登录页体验完全一致）。
 * 前置：调用方页面 data 中必须声明 showDeptSwitcher，并在模板最底部嵌入：
 *   <dept-switcher
 *     v-model:visible="showDeptSwitcher"
 *     :current-dept-id="currentDeptId"
 *     :request-fn="request"
 *     @change="onDeptSwitcherChanged"
 *   />
 *   methods 中要有：onDeptSwitcherChanged() { applyWorkScopeToPage(this); this.reload() }
 *
 * 这个函数只做两件事：
 *   1) 边界检查（0 部门 → toast 提示，不弹窗；1 个部门也正常打开列表，保持与登录/首页一致的体验）；
 *   2) 设置 vm.showDeptSwitcher = true 交给组件渲染。
 */
export function openDeptSwitcher(vm, reloadFn, requestFn) {
  const snapshot = workContext.snapshot()
  const rawDepts = Array.isArray(snapshot.depts) ? snapshot.depts : []
  const depts = rawDepts
    .map((dept) => {
      if (!dept) return null
      const id = dept.id ?? dept.deptId
      let name = dept.name || dept.deptName
      if (typeof name !== 'string') name = name ? String(name) : ''
      if (id === undefined || id === null || !String(name).trim()) return null
      return { id, name: String(name).trim() }
    })
    .filter(Boolean)
  if (depts.length === 0) {
    shortToast('暂无可用部门，请联系管理员分配权限')
    return
  }
  if (!vm) { shortToast('页面实例缺失'); return }
  vm.showDeptSwitcher = true
}

/**
 * DeptSwitcher 组件 @change 事件触发后的默认处理：
 * 重新应用 workContext → 执行 reloadFn（如果传了）。
 *
 * 使用示例：
 *   onDeptSwitcherChanged() { handleDeptChanged(this, () => this.load()) }
 */
export async function handleDeptChanged(vm, reloadFn) {
  applyWorkScopeToPage(vm)
  if (typeof reloadFn === 'function') {
    try { await reloadFn() } catch (_) { /* 调用方自行处理错误提示 */ }
  }
}
