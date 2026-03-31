import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { mountView } from './support/mountView'
import { API_ENDPOINTS } from '../src/api/endpoints'

const { client } = vi.hoisted(() => ({
  client: {
    get: vi.fn()
  }
}))

vi.mock('../src/api/client', () => ({
  default: client
}))

vi.mock('lucide-vue-next', () => ({
  Activity: { template: '<span />' },
  Database: { template: '<span />' },
  Rocket: { template: '<span />' },
  Target: { template: '<span />' },
  RefreshCw: { template: '<span />' }
}))

import Dashboard from '../src/views/Dashboard.vue'

describe('Dashboard view', () => {
  beforeEach(() => {
    client.get.mockReset()
  })

  test('loads summary, patterns, and traces and renders healthy metrics', async () => {
    // Covers src/views/Dashboard.vue:applySummary, src/views/Dashboard.vue:loadSummary, src/views/Dashboard.vue:loadPatterns, src/views/Dashboard.vue:loadTraces, src/views/Dashboard.vue:metricValue, and src/views/Dashboard.vue:metricPercent.
    client.get.mockImplementation((url) => {
      if (url === API_ENDPOINTS.STATS_SUMMARY) {
        return Promise.resolve({
          data: {
            totalTraces: 12,
            patternCount: 3,
            parseOk: 12,
            parseError: 0,
            activeAccelerationCount: 2,
            draftAccelerationCount: 1,
            cacheHitCount: 6,
            lastTraceAt: '2026-03-31T00:00:00Z'
          }
        })
      }
      if (url === API_ENDPOINTS.PATTERNS_TOP) {
        return Promise.resolve({
          data: {
            content: [{ id: 1, sqlFingerprint: 'abcdef1234567890', cleanSqlSample: 'SELECT 1', executionCount: 8, avgDurationMs: 15 }]
          }
        })
      }
      return Promise.resolve({
        data: {
          content: [{ id: 2, datasourceName: 'default', sqlFingerprint: 'trace1234567890', originalSql: 'SELECT 1', durationMs: 12, receivedAt: '2026-03-31T00:00:00Z' }]
        }
      })
    })

    const { wrapper } = await mountView(Dashboard)
    await flushPromises()

    expect(client.get).toHaveBeenCalledWith(API_ENDPOINTS.STATS_SUMMARY)
    expect(client.get).toHaveBeenCalledWith(API_ENDPOINTS.PATTERNS_TOP, { params: { page: 0, size: 5 } })
    expect(client.get).toHaveBeenCalledWith(API_ENDPOINTS.TRACES, { params: { page: 0, size: 5 } })
    expect(wrapper.text()).toContain('12')
    expect(wrapper.text()).toContain('Cache Hit Rate 50%')
    expect(wrapper.text()).toContain('Control plane online, trace pipeline healthy')
    expect(wrapper.text()).toContain('Last trace:')
  })

  test('shows summary failure state and fallback note text before first successful load', async () => {
    // Covers src/views/Dashboard.vue:loadSummary error handling and derived health text/tag state.
    client.get.mockImplementation((url) => {
      if (url === API_ENDPOINTS.STATS_SUMMARY) {
        return Promise.reject({ response: { data: { message: 'summary exploded' } } })
      }
      return Promise.resolve({ data: { content: [] } })
    })

    const { wrapper } = await mountView(Dashboard)
    await flushPromises()

    expect(wrapper.text()).toContain('summary exploded')
    expect(wrapper.text()).toContain('Summary metrics are unavailable until the dashboard load succeeds.')
    expect(wrapper.text()).toContain('Control plane data failed to load')
    expect(wrapper.text()).toContain('--')
  })

  test('shows the hot patterns empty state when the API has no live aggregates yet', async () => {
    // Covers src/views/Dashboard.vue empty-state rendering for recent traces and hot patterns.
    client.get.mockImplementation((url) => {
      if (url === API_ENDPOINTS.STATS_SUMMARY) {
        return Promise.resolve({
          data: {
            totalTraces: 0,
            patternCount: 0,
            parseOk: 0,
            parseError: 0,
            activeAccelerationCount: 0,
            draftAccelerationCount: 0,
            cacheHitCount: 0,
            lastTraceAt: null
          }
        })
      }
      return Promise.resolve({ data: { content: [] } })
    })

    const { wrapper } = await mountView(Dashboard)
    await flushPromises()

    expect(wrapper.text()).toContain('No pattern aggregates available.')
    expect(wrapper.text()).toContain('No recent traces available.')
    expect(wrapper.text()).toContain('Control plane online, waiting for traces')
  })

  test('keeps recent traces and hot patterns panels on matching half-width columns', async () => {
    // Covers src/views/Dashboard.vue dashboard panel layout props for matched column sizing.
    client.get.mockResolvedValue({ data: { content: [] } })

    const { wrapper } = await mountView(Dashboard)
    await flushPromises()

    const panelColumns = wrapper.findAll('.dashboard-panel-column')
    expect(panelColumns).toHaveLength(2)
    expect(panelColumns.map((column) => column.attributes('lg'))).toEqual(['12', '12'])
  })
})
