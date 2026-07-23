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
    singletonCoordinatorPromise = import('../api/index.js').then(({ getToken, refreshAuthSession, refreshWorkContext }) => (
      createForegroundSessionCoordinator({
        hasToken: () => Boolean(getToken()),
        refreshAuth: refreshAuthSession,
        refreshContext: refreshWorkContext
      })
    ))
  }
  return singletonCoordinatorPromise
}

export async function refreshForegroundSession() {
  const coordinator = await getSingletonCoordinator()
  return coordinator.refresh()
}
