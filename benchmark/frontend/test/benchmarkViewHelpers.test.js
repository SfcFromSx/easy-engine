import {
  buildDashboardStats,
  buildRunsChartData,
  buildTrendData,
  calcPercent,
  calcSuccessRate,
  filterDataSources,
  filterJobs,
  filterTestSets,
  formatDeltas,
  formatJson,
  formatParamJson,
  getDataSourceName,
  getTestSetName,
  parseEvaluationJson,
  strategyTagType,
  validateDriverUploadFile,
  verdictTag
} from '../src/utils/benchmarkViewHelpers'

describe('benchmarkViewHelpers', () => {
  // Covers benchmarkViewHelpers.js:filterDataSources and validateDriverUploadFile.
  it('filters data sources by keyword, driver class, and validates uploaded driver extensions', () => {
    expect(filterDataSources([
      { name: 'Kylin', jdbcUrl: 'jdbc:kylin://localhost', jdbcUser: 'admin', driverClass: 'org.apache.kylin.jdbc.Driver' },
      { name: 'Presto', jdbcUrl: 'jdbc:presto://localhost', jdbcUser: 'engine', driverClass: 'io.prestosql.Driver' }
    ], 'presto')).toHaveLength(1)
    expect(filterDataSources([
      { name: 'Kylin', jdbcUrl: 'jdbc:kylin://localhost', jdbcUser: 'admin', driverClass: 'org.apache.kylin.jdbc.Driver' },
      { name: 'Presto', jdbcUrl: 'jdbc:presto://localhost', jdbcUser: 'engine', driverClass: 'io.prestosql.Driver' }
    ], '', 'org.apache.kylin.jdbc.Driver')).toEqual([
      { name: 'Kylin', jdbcUrl: 'jdbc:kylin://localhost', jdbcUser: 'admin', driverClass: 'org.apache.kylin.jdbc.Driver' }
    ])
    expect(validateDriverUploadFile({ name: 'driver.jar' })).toEqual({ valid: true, reason: null })
    expect(validateDriverUploadFile({ name: 'driver.txt' })).toEqual({ valid: false, reason: 'invalid_extension' })
  })

  // Covers benchmarkViewHelpers.js:filterJobs, getDataSourceName, getTestSetName, and strategyTagType.
  it('filters jobs with datasource and test-set names and resolves strategy tags', () => {
    const jobs = [
      { id: 1, name: 'Smoke', strategy: 'ROUND_ROBIN', dataSourceId: 10, testSetId: 100, concurrentThreads: 8, rounds: 32 },
      { id: 2, name: 'Cache', strategy: 'CACHE_PENETRATION', dataSourceId: 11, testSetId: null, concurrentThreads: 4, rounds: 64 }
    ]
    const dataSources = [{ id: 10, name: 'Kylin DS' }, { id: 11, name: 'Presto DS' }]
    const testSets = [{ id: 100, name: 'Excel Import' }]

    expect(filterJobs(jobs, 'excel', '', '', '', dataSources, testSets)).toEqual([jobs[0]])
    expect(filterJobs(jobs, '', '11', '', '', dataSources, testSets)).toEqual([jobs[1]])
    expect(filterJobs(jobs, '32', '', '', '', dataSources, testSets)).toEqual([jobs[0]])
    expect(filterJobs(jobs, '', '', 'CACHE_PENETRATION', '', dataSources, testSets)).toEqual([jobs[1]])
    expect(filterJobs(jobs, '', '', '', '__sql_lib__', dataSources, testSets)).toEqual([jobs[1]])
    expect(filterJobs(jobs, '', '', '', '100', dataSources, testSets)).toEqual([jobs[0]])
    expect(getDataSourceName(dataSources, 999)).toBe('DataSource #999')
    expect(getTestSetName(testSets, null)).toBe('Default (SQL Lib)')
    expect(strategyTagType('CACHE_PENETRATION')).toBe('warning')
  })

  // Covers benchmarkViewHelpers.js:filterTestSets and formatParamJson.
  it('filters test sets by source and pretty-prints param json', () => {
    const testSets = [
      { id: 1, name: 'Uploaded', description: 'excel', sourceFilename: 'cases.xlsx', itemCount: 8 },
      { id: 2, name: 'Manual', description: 'handwritten', sourceFilename: null }
    ]

    expect(filterTestSets(testSets, '', 'uploaded')).toEqual([testSets[0]])
    expect(filterTestSets(testSets, 'manual', 'manual')).toEqual([testSets[1]])
    expect(filterTestSets(testSets, '8', 'all')).toEqual([testSets[0]])
    expect(formatParamJson('[{"type":"INTEGER","value":1}]')).toContain('\n  ')
    expect(formatParamJson('not-json')).toBe('not-json')
  })

  // Covers benchmarkViewHelpers.js:parseEvaluationJson, verdictTag, and formatJson.
  it('parses evaluation payloads and formats json fallbacks', () => {
    const row = { evaluationJson: '{"verdict":"PASS"}' }
    expect(parseEvaluationJson(row)).toEqual({ verdict: 'PASS' })
    expect(parseEvaluationJson({ evaluationJson: '{bad json' })).toBeNull()
    expect(verdictTag('PARTIAL')).toBe('warning')
    expect(formatJson('{"a":1}')).toContain('\n  "a": 1\n')
    expect(formatJson('plain-text')).toBe('plain-text')
  })

  // Covers benchmarkViewHelpers.js:buildRunsChartData and formatDeltas.
  it('builds run chart data and comparison delta labels', () => {
    const evalObj = {
      metrics: {
        qpsSuccessful: 12.345,
        latencyMs: { p50: 3.2, p95: 9.1, p99: 11.7 }
      }
    }
    const deltas = {
      qpsSuccessful: { current: 12, baseline: 10, delta: 2 },
      p95Ms: { current: 9, baseline: 12, delta: -3 }
    }

    expect(buildRunsChartData(evalObj)).toEqual([
      { name: 'QPS', value: '12.35' },
      { name: 'P50', value: '3.20' },
      { name: 'P95', value: '9.10' },
      { name: 'P99', value: '11.70' }
    ])
    expect(formatDeltas(deltas)).toEqual([
      expect.objectContaining({ metric: 'QPS (吞吐量)', direction: 'up', deltaText: '+2.00' }),
      expect.objectContaining({ metric: 'P95 (尾延迟)', direction: 'up', deltaText: '-3.00' })
    ])
  })

  // Covers benchmarkViewHelpers.js:calcPercent, calcSuccessRate, buildDashboardStats, and buildTrendData.
  it('computes dashboard statistics and trend shaping for completed runs', () => {
    const runs = [
      { id: 1, status: 'COMPLETED', totalQueries: 10, successCount: 9, errorCount: 1, p50Ms: 12, qps: 22.5 },
      { id: 2, status: 'FAILED', totalQueries: 5, successCount: 0, errorCount: 5, durationMs: 30, qps: null },
      { id: 3, status: 'COMPLETED', totalQueries: 20, successCount: 20, errorCount: 0, durationMs: 18, p50Ms: 8, qps: 31.2 }
    ]

    expect(calcPercent({ currentProgress: 7, totalTarget: 10 })).toBe(70)
    expect(calcSuccessRate({ successCount: 9, errorCount: 1 })).toBe(90)
    expect(buildDashboardStats(runs, [{ id: 10 }, { id: 11 }])).toEqual({
      totalJobs: 2,
      totalRuns: 3,
      successRate: 96.7,
      avgLatency: 10
    })
    expect(buildTrendData(runs)).toEqual([
      { runId: '#3', qps: 31.2, p50: 8 },
      { runId: '#1', qps: 22.5, p50: 12 }
    ])
  })
})
