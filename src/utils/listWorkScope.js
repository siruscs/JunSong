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
