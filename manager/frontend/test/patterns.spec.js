import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { mountView } from './support/mountView'
import { ACCEL_FROM_PATTERN, API_ENDPOINTS } from '../src/api/endpoints'

const { client, message } = vi.hoisted(() => ({
  client: {
    get: vi.fn(),
    post: vi.fn()
  },
  message: {
    error: vi.fn(),
    success: vi.fn()
  }
}))

vi.mock('../src/api/client', () => ({
  default: client
}))

vi.mock('element-plus', () => ({
  ElMessage: message
}))

vi.mock('lucide-vue-next', () => ({
  RefreshCw: { template: '<span />' },
  Terminal: { template: '<span />' }
}))

import Patterns from '../src/views/Patterns.vue'

describe('Patterns view', () => {
  beforeEach(() => {
    client.get.mockReset()
    client.post.mockReset()
    message.error.mockReset()
    message.success.mockReset()
  })

  test('loads route-backed filters and highlights the matching mobile record', async () => {
    // Covers src/views/Patterns.vue:load, src/views/Patterns.vue:rowClassName, and route-backed filter normalization.
    client.get.mockResolvedValue({
      data: {
        content: [{ id: 1, sqlFingerprint: 'fp-1', cleanSqlSample: 'SELECT 1', executionCount: 5, avgDurationMs: 80 }],
        totalElements: 1
      }
    })

    const { wrapper } = await mountView(Patterns, { route: '/patterns?fingerprint=fp-1&sqlKeyword=SELECT&minExecutionCount=3' })
    await flushPromises()

    expect(client.get).toHaveBeenLastCalledWith(API_ENDPOINTS.PATTERNS_TOP, {
      params: { page: 0, size: 10, fingerprint: 'fp-1', sqlKeyword: 'SELECT', minExecutionCount: 3 }
    })
    expect(wrapper.find('.mobile-record--selected').exists()).toBe(true)
  })

  test('opens the dialog with default rollup values and submits an acceleration draft', async () => {
    // Covers src/views/Patterns.vue:openDialog, src/views/Patterns.vue:submit, and src/views/Patterns.vue:goTraces.
    client.get.mockResolvedValue({
      data: {
        content: [{ id: 9, sqlFingerprint: 'abcdef1234567890', cleanSqlSample: 'SELECT 1', executionCount: 2, avgDurationMs: 10 }],
        totalElements: 1
      }
    })
    client.post.mockResolvedValue({ data: {} })

    const { wrapper, router } = await mountView(Patterns)
    await flushPromises()

    const traceButton = wrapper.findAll('button').find((button) => button.text() === 'View Traces')
    await traceButton.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/traces?fingerprint=abcdef1234567890')

    const createButton = wrapper.findAll('button').find((button) => button.text() === 'Create Acceleration')
    await createButton.trigger('click')
    await flushPromises()

    const dialogInputs = wrapper.findAll('input').slice(-2)
    expect(dialogInputs[0].element.value).toBe('public')
    expect(dialogInputs[1].element.value).toBe('rollup_abcdef12')

    await dialogInputs[1].setValue('custom_rollup')
    const submitButton = wrapper.findAll('button').find((button) => button.text() === 'Create')
    await submitButton.trigger('click')
    await flushPromises()

    expect(client.post).toHaveBeenCalledWith(ACCEL_FROM_PATTERN, {
      patternStatsId: 9,
      tableName: 'custom_rollup',
      schemaName: 'public'
    })
    expect(message.success).toHaveBeenCalledWith('Acceleration draft created')
  })

  test('rejects empty table names before posting', async () => {
    // Covers src/views/Patterns.vue:submit validation branch for empty table names.
    client.get.mockResolvedValue({
      data: {
        content: [{ id: 9, sqlFingerprint: 'abcdef1234567890', cleanSqlSample: 'SELECT 1', executionCount: 2, avgDurationMs: 10 }],
        totalElements: 1
      }
    })

    const { wrapper } = await mountView(Patterns)
    await flushPromises()

    const createButton = wrapper.findAll('button').find((button) => button.text() === 'Create Acceleration')
    await createButton.trigger('click')
    await flushPromises()

    const dialogInputs = wrapper.findAll('input').slice(-2)
    await dialogInputs[1].setValue('   ')
    const submitButton = wrapper.findAll('button').find((button) => button.text() === 'Create')
    await submitButton.trigger('click')

    expect(client.post).not.toHaveBeenCalled()
    expect(message.error).toHaveBeenCalledWith('Please enter a table name.')
  })

  test('updates the route when multiple filters are submitted', async () => {
    // Covers src/views/Patterns.vue:buildQuery and src/views/Patterns.vue:applyFilter with multiple filter params.
    client.get.mockResolvedValue({ data: { content: [], totalElements: 0 } })

    const { wrapper, router } = await mountView(Patterns)
    await flushPromises()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue(' fp-2 ')
    await inputs[1].setValue(' sales ')
    await inputs[2].setValue(' 25 ')

    const searchButton = wrapper.findAll('button').find((button) => button.text() === 'Search')
    await searchButton.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ fingerprint: 'fp-2', sqlKeyword: 'sales', minExecutionCount: '25' })
    expect(client.get).toHaveBeenLastCalledWith(API_ENDPOINTS.PATTERNS_TOP, {
      params: { page: 0, size: 10, fingerprint: 'fp-2', sqlKeyword: 'sales', minExecutionCount: 25 }
    })
  })
})
