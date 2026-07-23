function normalizeDept(dept) {
  const id = dept?.deptId ?? dept?.id
  const name = dept?.deptName || dept?.name || ''
  return id === undefined || id === null || !name ? null : { ...dept, id, name }
}

function normalizeIdentity(value) {
  return value === undefined || value === null ? null : String(value)
}

export function resolveDeptCollection(info = {}, storedUser = {}) {
  const candidates = [info?.depts, info?.user?.depts, storedUser?.depts]
  return candidates.find((depts) => Array.isArray(depts) && depts.length) || []
}

export function mergePersistedUser(existing = {}, incoming = {}, snapshot) {
  const snapshotDepts = snapshot?.depts
  const depts = [snapshotDepts, incoming?.depts, existing?.depts]
    .find((items) => Array.isArray(items) && items.length) || []
  const currentDeptId = snapshot?.currentDeptId ?? incoming?.currentDeptId ?? incoming?.deptId ??
    existing?.currentDeptId ?? existing?.deptId ?? null

  return {
    ...existing,
    ...incoming,
    depts,
    currentDeptId,
    deptId: currentDeptId
  }
}

export function createWorkContext(initial = {}) {
  let version = 0
  let state = { user: null, depts: [], currentDeptId: null, currentDept: null }

  const select = (deptId, increment = true) => {
    const currentDept = state.depts.find((dept) => String(dept.id) === String(deptId))
    if (!currentDept) throw new Error('无权访问该部门')
    state = { ...state, currentDeptId: currentDept.id, currentDept }
    if (increment) version += 1
  }

  const context = {
    hydrate({ user, depts = [], currentDeptId }) {
      const normalized = depts.map(normalizeDept).filter(Boolean)
      const nextDeptId = normalized.length ? currentDeptId ?? normalized[0].id : null
      const identityChanged =
        normalizeIdentity(state.user?.userId) !== normalizeIdentity(user?.userId) ||
        normalizeIdentity(state.currentDeptId) !== normalizeIdentity(nextDeptId)
      state = { user: user || null, depts: normalized, currentDeptId: null, currentDept: null }
      if (normalized.length) select(nextDeptId, false)
      if (identityChanged) version += 1
      return context.snapshot()
    },
    selectDept(deptId) {
      select(deptId, true)
      return context.snapshot()
    },
    clear() {
      state = { user: null, depts: [], currentDeptId: null, currentDept: null }
      version += 1
    },
    snapshot() {
      return { ...state, depts: [...state.depts], version }
    },
    captureVersion() {
      return version
    },
    isCurrent(candidate) {
      return candidate === version
    }
  }

  if (initial.user || initial.depts?.length) context.hydrate(initial)
  return context
}

export const workContext = createWorkContext()
