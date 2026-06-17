export function toNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

export function applySeckillStats(activity = {}, stats = {}) {
  const soldShares = toNumber(stats.totalShares ?? activity.totalShares)
  const claimedShares = toNumber(stats.claimedShares ?? activity.claimedShares)
  const totalAmount = toNumber(stats.totalAmount ?? activity.totalAmount)
  const seckillCount = toNumber(stats.totalPeople ?? activity.seckillCount ?? activity.buyCount)
  const claimProgress = soldShares > 0 ? Math.max(Math.min(Math.round((claimedShares / soldShares) * 100), 100), 0) : 0
  return {
    ...activity,
    soldShares,
    claimedShares,
    totalAmount,
    seckillCount,
    claimProgress
  }
}
