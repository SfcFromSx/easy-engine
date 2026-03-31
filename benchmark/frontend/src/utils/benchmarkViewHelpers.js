export function filterDataSources(dataSources, searchTerm, selectedDriverClass = '') {
  const keyword = String(searchTerm || '').trim().toLowerCase()
  return dataSources.filter((item) => {
    const matchesKeyword = !keyword || [item.name, item.jdbcUrl, item.jdbcUser, item.driverClass]
      .some((value) => String(value || '').toLowerCase().includes(keyword))
    const matchesDriver = !selectedDriverClass || String(item.driverClass || '') === selectedDriverClass
    return matchesKeyword && matchesDriver
  })
}

export function validateDriverUploadFile(file) {
  if (!file) {
    return { valid: false, reason: 'missing' }
  }
  if (!String(file.name || '').toLowerCase().endsWith('.jar')) {
    return { valid: false, reason: 'invalid_extension' }
  }
  return { valid: true, reason: null }
}

export function strategyTagType(strategy) {
  if (strategy === 'RANDOM_WEIGHT') return 'primary'
  if (strategy === 'ROUND_ROBIN') return 'success'
  if (strategy === 'CACHE_PENETRATION') return 'warning'
  return 'info'
}

export function getTestSetName(testSets, id) {
  if (!id) return 'Default (Templates)'
  const testSet = testSets.find((item) => item.id === id)
  return testSet ? testSet.name : `ID: ${id}`
}

export function getDataSourceName(dataSources, id) {
  if (!id) return 'Not Linked'
  const dataSource = dataSources.find((item) => item.id === id)
  return dataSource ? dataSource.name : `DataSource #${id}`
}

export function filterJobs(
  jobs,
  keyword,
  selectedDataSourceId,
  selectedStrategy,
  selectedTestSetFilter,
  dataSources,
  testSets
) {
  const normalizedKeyword = String(keyword || '').trim().toLowerCase()
  return jobs.filter((job) => {
    const matchesKeyword = !normalizedKeyword || [
      job.id,
      job.name,
      job.strategy,
      job.concurrentThreads,
      job.rounds,
      getDataSourceName(dataSources, job.dataSourceId),
      getTestSetName(testSets, job.testSetId)
    ].some((value) => String(value || '').toLowerCase().includes(normalizedKeyword))
    const matchesDataSource = !selectedDataSourceId || String(job.dataSourceId || '') === selectedDataSourceId
    const matchesStrategy = !selectedStrategy || String(job.strategy || '') === selectedStrategy
    const matchesTestSet = !selectedTestSetFilter
      || (selectedTestSetFilter === '__templates__' && !job.testSetId)
      || String(job.testSetId || '') === selectedTestSetFilter
    return matchesKeyword && matchesDataSource && matchesStrategy && matchesTestSet
  })
}

export function filterTestSets(testSets, keyword, sourceFilter) {
  const normalizedKeyword = String(keyword || '').trim().toLowerCase()
  return testSets.filter((item) => {
    const matchesKeyword = !normalizedKeyword || [
      item.name,
      item.description,
      item.sourceFilename,
      item.itemCount,
      item.sqlCount
    ].some((value) => String(value || '').toLowerCase().includes(normalizedKeyword))
    const matchesSource = sourceFilter === 'all'
      || (sourceFilter === 'uploaded' && Boolean(item.sourceFilename))
      || (sourceFilter === 'manual' && !item.sourceFilename)
    return matchesKeyword && matchesSource
  })
}

export function formatParamJson(value) {
  if (!value) {
    return ''
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

export function parseEvaluationJson(row) {
  if (!row?.evaluationJson) return null
  try {
    return JSON.parse(row.evaluationJson)
  } catch {
    return null
  }
}

export function verdictTag(verdict) {
  if (verdict === 'PASS') return 'success'
  if (verdict === 'PARTIAL') return 'warning'
  if (verdict === 'FAIL') return 'danger'
  return 'info'
}

export function formatJson(raw) {
  if (!raw) return '—'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

export function buildRunsChartData(evalObj) {
  if (!evalObj?.metrics) return []
  const metrics = evalObj.metrics
  const rows = []
  if (metrics.qpsSuccessful) rows.push({ name: 'QPS', value: metrics.qpsSuccessful.toFixed(2) })
  if (metrics.latencyMs) {
    if (metrics.latencyMs.p50) rows.push({ name: 'P50', value: metrics.latencyMs.p50.toFixed(2) })
    if (metrics.latencyMs.p95) rows.push({ name: 'P95', value: metrics.latencyMs.p95.toFixed(2) })
    if (metrics.latencyMs.p99) rows.push({ name: 'P99', value: metrics.latencyMs.p99.toFixed(2) })
  }
  return rows
}

export function formatDeltas(deltas) {
  if (!deltas) return []
  const metricsMap = {
    qps: 'QPS (吞吐量)',
    qpsSuccessful: 'QPS (吞吐量)',
    p50: 'P50 (中位数延迟)',
    p50Ms: 'P50 (中位数延迟)',
    p95: 'P95 (尾延迟)',
    p95Ms: 'P95 (尾延迟)',
    p99: 'P99 (极值延迟)',
    p99Ms: 'P99 (极值延迟)',
    successRate: 'Success Rate'
  }
  return Object.entries(deltas).map(([key, delta]) => {
    const current = delta.current
    const previous = delta.previous ?? delta.baseline
    const value = delta.delta ?? delta.percentChange ?? delta.absoluteDelta
    const isHigherBetter = key === 'qps' || key === 'qpsSuccessful' || key === 'successRate'
    let direction = 'none'
    if (typeof value === 'number' && value !== 0) {
      const improved = isHigherBetter ? value > 0 : value < 0
      direction = improved ? 'up' : 'down'
    }
    const suffix = delta.percentChange != null ? '%' : ''
    return {
      metric: metricsMap[key] || key,
      current: typeof current === 'number' ? current.toFixed(2) : '—',
      previous: typeof previous === 'number' ? previous.toFixed(2) : '—',
      deltaText: typeof value === 'number' ? `${value > 0 ? '+' : ''}${value.toFixed(2)}${suffix}` : '—',
      direction
    }
  })
}

export function calcPercent(run) {
  if (!run || !run.totalTarget) return 0
  return Math.min(100, Math.floor(((run.currentProgress || 0) / run.totalTarget) * 100))
}

export function calcSuccessRate(run) {
  if (!run) return 100
  const processed = (run.successCount || 0) + (run.errorCount || 0)
  if (!processed) return 100
  return Math.round(((run.successCount || 0) / processed) * 1000) / 10
}

export function buildDashboardStats(runHistory, jobs) {
  const completedRuns = runHistory.filter((run) => run.status === 'COMPLETED')
  const totalQueries = completedRuns.reduce((sum, run) => sum + (run.totalQueries || 0), 0)
  const successQueries = completedRuns.reduce((sum, run) => sum + (run.successCount || 0), 0)
  const latencySamples = completedRuns
    .map((run) => run.p50Ms ?? run.durationMs)
    .filter((value) => typeof value === 'number' && !Number.isNaN(value))

  return {
    totalJobs: jobs.length,
    totalRuns: runHistory.length,
    successRate: totalQueries > 0
      ? Math.round((successQueries / totalQueries) * 1000) / 10
      : 100,
    avgLatency: latencySamples.length > 0
      ? Math.round(latencySamples.reduce((sum, value) => sum + value, 0) / latencySamples.length)
      : 0
  }
}

export function buildTrendData(runs) {
  return runs
    .filter((run) => run.status === 'COMPLETED' && run.qps != null)
    .map((run) => ({
      runId: `#${run.id}`,
      qps: run.qps,
      p50: run.p50Ms
    }))
    .reverse()
}
