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
  RefreshCw: { template: '<span />' },
  Terminal: { template: '<span />' }
}))

import Traces from '../src/views/Traces.vue'

describe('Traces view', () => {
  beforeEach(() => {
    client.get.mockReset()
  })

  test('loads using route-backed filters and updates the route when filtering', async () => {
    // Covers src/views/Traces.vue:load, src/views/Traces.vue:buildQuery, src/views/Traces.vue:applyFilter, and route-backed filter normalization.
    client.get.mockResolvedValue({
      data: {
        content: [{ id: 1, datasourceName: 'default', sqlFingerprint: 'fp-1', originalSql: 'SELECT 1', durationMs: 20, receivedAt: '2026-03-31T00:00:00Z' }],
        totalElements: 1
      }
    })

    const { wrapper, router } = await mountView(Traces, { route: '/traces?fingerprint=fp-1&datasource=default&sqlKeyword=SELECT' })
    await flushPromises()

    expect(client.get).toHaveBeenLastCalledWith(API_ENDPOINTS.TRACES, {
      params: { page: 0, size: 10, fingerprint: 'fp-1', datasource: 'default', sqlKeyword: 'SELECT' }
    })

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('  fp-2  ')
    await inputs[1].setValue(' analytics ')
    await inputs[2].setValue(' orders ')
    const searchButton = wrapper.findAll('button').find((button) => button.text() === 'Search')
    await searchButton.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ fingerprint: 'fp-2', datasource: 'analytics', sqlKeyword: 'orders' })
    expect(client.get).toHaveBeenLastCalledWith(API_ENDPOINTS.TRACES, {
      params: { page: 0, size: 10, fingerprint: 'fp-2', datasource: 'analytics', sqlKeyword: 'orders' }
    })
  })

  test('clears the filter and resets rows after a failed load', async () => {
    // Covers src/views/Traces.vue:clearFilter, src/views/Traces.vue:statusType, src/views/Traces.vue:sourceFlagType, and src/views/Traces.vue:load error handling.
    client.get
      .mockResolvedValueOnce({
        data: {
          content: [{ id: 1, datasourceName: 'default', sqlFingerprint: 'fp-1', originalSql: 'SELECT 1', durationMs: 20, receivedAt: '2026-03-31T00:00:00Z', sourceFlag: 'SELF', parseStatus: 'OK' }],
          totalElements: 1
        }
      })
      .mockRejectedValueOnce({ response: { data: { message: 'trace load failed' } } })
      .mockRejectedValueOnce({ response: { data: { message: 'trace load failed' } } })

    const { wrapper, router } = await mountView(Traces, { route: '/traces?fingerprint=fp-1&datasource=default' })
    await flushPromises()

    const clearButton = wrapper.findAll('button').find((button) => button.text() === 'Clear Filter')
    await clearButton.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({})
    expect(wrapper.text()).toContain('trace load failed')
    expect(wrapper.text()).toContain('No trace records available')
  })
})
