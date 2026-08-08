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
  const switchable = deptCount > 1
  const scopeLabel = currentDeptId === null
    ? '暂无可用数据范围'
    : deptCount > 1 ? `当前部门 · 共 ${deptCount} 个部门` : '当前数据范围'

  return {
    currentDeptId,
    currentDeptName,
    scopeLabel,
    deptCount,
    switchable,
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
 *
 * 强制响应式保证：
 *  1) Vue2 + uni-app 组合下，如果 data() 里没有预声明字段，直接 vm.xxx = yyy 不会响应式。
 *     本函数会兜底：如果某个 key 在 vm 上不存在，就走 vm.$set(vm, key, value) / Vue.set。
 *  2) 即使调用方漏写了 switchable / deptCount，也能确保第一次进入 onShow 后 UI 立刻切到可点击态。
 *     （当然，AGENTS.md 仍要求在 data() 里预声明，这里是兜底。）
 */
function setOnVm(vm, key, value) {
  try {
    if (vm && vm.$set && !(key in vm)) {
      vm.$set(vm, key, value);
    } else {
      vm[key] = value;
    }
  } catch (_) {
    try { vm[key] = value } catch (_) { /* ignore */ }
  }
}

export function applyWorkScopeToPage(vm) {
  if (!vm) return { departmentChanged: false, switchable: false, deptCount: 0 }
  const prior = vm.currentDeptId !== undefined ? vm.currentDeptId : null
  const scope = resolveListWorkScope(workContext.snapshot(), prior)
  setOnVm(vm, 'currentDeptId', scope.currentDeptId)
  setOnVm(vm, 'currentDeptName', scope.currentDeptName)
  setOnVm(vm, 'scopeLabel', scope.scopeLabel)
  setOnVm(vm, 'deptCount', scope.deptCount)
  setOnVm(vm, 'switchable', scope.switchable)
  setOnVm(vm, 'contextVersion', scope.contextVersion)
  return { departmentChanged: scope.departmentChanged, contextVersion: scope.contextVersion, deptCount: scope.deptCount, switchable: scope.switchable }
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
 *   1) 边界检查（0 部门 toast；1 部门 → 安静 return，不弹窗也不弹 toast，视觉上应该通过
 *      work-scope-disabled 样式把它做成不可点击态，让用户从 UI 上就知道不能切换）；
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
  // 单部门时：安静返回。页面层通过 switchable 给 work-scope 加 disabled 样式，
  // 让用户从视觉上就知道不能切换，避免"点了弹一下又关掉"的怪异体验。
  if (depts.length === 1) return
  if (!vm) { shortToast('页面实例缺失'); return }
  // 使用 $set 保证 showDeptSwitcher 一定触发响应式（Vue2 + uni-app 下某些场景直接赋值会被吞）
  try {
    if (vm.$set) vm.$set(vm, 'showDeptSwitcher', true)
    else vm.showDeptSwitcher = true
  } catch (_) {
    vm.showDeptSwitcher = true
  }
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
