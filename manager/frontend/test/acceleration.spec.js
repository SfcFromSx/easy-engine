import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { mountView } from './support/mountView'
import { ACCELERATION_BY_ID, ACCELERATION_STATUS, API_ENDPOINTS } from '../src/api/endpoints'

const { client, message } = vi.hoisted(() => ({
  client: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn()
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
  Plus: { template: '<span />' },
  Database: { template: '<span />' }
}))

import Acceleration from '../src/views/Acceleration.vue'

describe('Acceleration view', () => {
  beforeEach(() => {
    client.get.mockReset()
    client.post.mockReset()
    client.put.mockReset()
    client.patch.mockReset()
    client.delete.mockReset()
    message.error.mockReset()
    message.success.mockReset()
  })

  test('loads list and summary data and computes disabled counts', async () => {
    // Covers src/views/Acceleration.vue:load and route-backed list params.
    client.get.mockImplementation((url) => {
      if (url === API_ENDPOINTS.STATS_SUMMARY) {
        return Promise.resolve({ data: { activeAccelerationCount: 1, draftAccelerationCount: 1 } })
      }
      return Promise.resolve({
        data: {
          content: [
            { id: 1, name: 'mv_sales', status: 'ACTIVE', schemaName: 'analytics', source: 'RECOMMENDED', ddlText: 'CREATE TABLE mv_sales', refreshSql: 'INSERT INTO mv_sales', cronExpr: '0 0 * * * ?' },
            { id: 2, name: 'mv_custom', status: 'DISABLED', schemaName: 'public', source: 'MANUAL', ddlText: 'CREATE TABLE mv_custom', refreshSql: '', cronExpr: '' },
            { id: 3, name: 'mv_draft', status: 'DRAFT', schemaName: 'public', source: 'MANUAL', ddlText: 'CREATE TABLE mv_draft', refreshSql: '', cronExpr: '' }
          ],
          totalElements: 3
        }
      })
    })

    const { wrapper } = await mountView(Acceleration, { route: '/acceleration?keyword=sales&status=ACTIVE&schemaName=analytics&source=RECOMMENDED' })
    await flushPromises()

    expect(client.get).toHaveBeenCalledWith(API_ENDPOINTS.ACCELERATION_TABLES, {
      params: { page: 0, size: 10, keyword: 'sales', status: 'ACTIVE', schemaName: 'analytics', source: 'RECOMMENDED' }
    })
    expect(wrapper.text()).toContain('ACTIVE 1')
    expect(wrapper.text()).toContain('DRAFT 1')
    expect(wrapper.text()).toContain('DISABLED 1')
  })

  test('validates manual creation before posting', async () => {
    // Covers src/views/Acceleration.vue:openCreate and src/views/Acceleration.vue:save validation branch.
    client.get.mockImplementation((url) => {
      if (url === API_ENDPOINTS.STATS_SUMMARY) {
        return Promise.resolve({ data: { activeAccelerationCount: 0, draftAccelerationCount: 0 } })
      }
      return Promise.resolve({ data: { content: [], totalElements: 0 } })
    })

    const { wrapper } = await mountView(Acceleration, { route: '/acceleration' })
    await flushPromises()

    const createButton = wrapper.findAll('button').find((button) => button.text() === 'Register Acceleration')
    await createButton.trigger('click')
    await flushPromises()

    const saveButton = wrapper.findAll('button').find((button) => button.text() === 'Save')
    await saveButton.trigger('click')

    expect(message.error).toHaveBeenCalledWith('Please enter an acceleration name.')
    expect(client.post).not.toHaveBeenCalled()
  })

  test('toggles activation and uses the status endpoint', async () => {
    // Covers src/views/Acceleration.vue:toggleStatus.
    client.get.mockImplementation((url) => {
      if (url === API_ENDPOINTS.STATS_SUMMARY) {
        return Promise.resolve({ data: { activeAccelerationCount: 0, draftAccelerationCount: 1 } })
      }
      return Promise.resolve({
        data: {
          content: [{ id: 4, name: 'mv_sales', status: 'DRAFT', schemaName: 'analytics', source: 'RECOMMENDED', ddlText: 'CREATE TABLE mv_sales', refreshSql: 'INSERT INTO mv_sales', cronExpr: '0 0 * * * ?' }],
          totalElements: 1
        }
      })
    })
    client.patch.mockResolvedValue({ data: {} })

    const { wrapper } = await mountView(Acceleration, { route: '/acceleration' })
    await flushPromises()

    const activateButton = wrapper.findAll('button').find((button) => button.text() === 'Activate')
    await activateButton.trigger('click')
    await flushPromises()

    expect(client.patch).toHaveBeenCalledWith(ACCELERATION_STATUS(4), { status: 'ACTIVE' })
    expect(message.success).toHaveBeenCalledWith('Acceleration activated')
  })

  test('saves edited rows with a public schema fallback and removes rows', async () => {
    // Covers src/views/Acceleration.vue:edit, src/views/Acceleration.vue:save success path, and src/views/Acceleration.vue:remove.
    client.get.mockImplementation((url) => {
      if (url === API_ENDPOINTS.STATS_SUMMARY) {
        return Promise.resolve({ data: { activeAccelerationCount: 0, draftAccelerationCount: 1 } })
      }
      return Promise.resolve({
        data: {
          content: [{ id: 5, name: 'mv_sales', status: 'DRAFT', schemaName: '', source: 'MANUAL', ddlText: 'CREATE TABLE mv_sales', refreshSql: '', cronExpr: '' }],
          totalElements: 1
        }
      })
    })
    client.put.mockResolvedValue({ data: {} })
    client.delete.mockResolvedValue({ data: {} })

    const { wrapper } = await mountView(Acceleration, { route: '/acceleration' })
    await flushPromises()

    const configureButton = wrapper.findAll('button').find((button) => button.text() === 'Configure')
    await configureButton.trigger('click')
    await flushPromises()

    const saveButton = wrapper.findAll('button').find((button) => button.text() === 'Save')
    await saveButton.trigger('click')
    await flushPromises()

    expect(client.put).toHaveBeenCalledWith(ACCELERATION_BY_ID(5), expect.objectContaining({
      name: 'mv_sales',
      schemaName: 'public'
    }))

    const removeButton = wrapper.findAll('button').find((button) => button.text() === 'Remove')
    await removeButton.trigger('click')
    await flushPromises()

    expect(client.delete).toHaveBeenCalledWith(ACCELERATION_BY_ID(5))
    expect(message.success).toHaveBeenCalledWith('Acceleration removed')
  })

  test('clears route-backed filters before reloading the list', async () => {
    // Covers src/views/Acceleration.vue:clearFilter and route-backed query reset.
    client.get.mockImplementation((url) => {
      if (url === API_ENDPOINTS.STATS_SUMMARY) {
        return Promise.resolve({ data: { activeAccelerationCount: 0, draftAccelerationCount: 0 } })
      }
      return Promise.resolve({ data: { content: [], totalElements: 0 } })
    })

    const { wrapper, router } = await mountView(Acceleration, { route: '/acceleration?keyword=sales&status=ACTIVE' })
    await flushPromises()

    const clearButton = wrapper.findAll('button').find((button) => button.text() === 'Clear Filter')
    await clearButton.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({})
    expect(client.get).toHaveBeenCalledWith(API_ENDPOINTS.ACCELERATION_TABLES, {
      params: { page: 0, size: 10 }
    })
  })
})
