export function createForegroundSessionCoordinator({ hasToken, refreshAuth, refreshContext }) {
  let activeRefresh = null

  return {
    refresh() {
      if (!hasToken()) return Promise.resolve(null)
      if (activeRefresh) return activeRefresh

      activeRefresh = (async () => {
        await refreshAuth()
        return refreshContext()
      })()
        .finally(() => {
          activeRefresh = null
        })
      return activeRefresh
    }
  }
}

let singletonCoordinatorPromise = null

function getSingletonCoordinator() {
  if (!singletonCoordinatorPromise) {
    singletonCoordinatorPromise = import('../api/index.js').then(({ restoreSession }) => (
      createForegroundSessionCoordinator({
        hasToken: () => Boolean(uni.getStorageSync('token')),
        refreshAuth: restoreSession,
        refreshContext: async () => null
      })
    ))
  }
  return singletonCoordinatorPromise
}

export async function refreshForegroundSession() {
  const coordinator = await getSingletonCoordinator()
  return coordinator.refresh()
}
