import { buildChartOption } from '../src/utils/charting'

describe('charting utilities', () => {
  // Covers charting.js:buildChartOption line-series shaping.
  it('builds line chart options from named y-series', () => {
    const option = buildChartOption({
      type: 'line',
      data: [{ runId: '#1', qps: 10, p95: 20 }],
      xKey: 'runId',
      yKeys: [{ name: 'QPS', key: 'qps', color: '#123456' }, { name: 'P95', key: 'p95' }]
    })

    expect(option.xAxis.data).toEqual(['#1'])
    expect(option.series).toHaveLength(2)
    expect(option.series[0]).toEqual(expect.objectContaining({ name: 'QPS', data: [10] }))
  })

  // Covers charting.js:buildChartOption bar-series shaping and default fallback.
  it('builds bar chart options and returns empty config for unknown types', () => {
    const option = buildChartOption({
      type: 'bar',
      data: [{ metric: 'P50', value: '4.20' }],
      xKey: 'metric',
      yKeys: [{ name: 'Value', key: 'value' }]
    })

    expect(option.series[0].type).toBe('bar')
    expect(option.xAxis.data).toEqual(['P50'])
    expect(buildChartOption({ type: 'pie' })).toEqual({})
  })
})
