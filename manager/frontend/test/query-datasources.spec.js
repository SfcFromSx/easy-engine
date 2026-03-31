import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { mountView } from './support/mountView'
import { API_ENDPOINTS, QUERY_DATASOURCE_BY_ID } from '../src/api/endpoints'

const { client, message, messageBox } = vi.hoisted(() => ({
  client: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  },
  message: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn()
  },
  messageBox: {
    confirm: vi.fn()
  }
}))

vi.mock('../src/api/client', () => ({
  default: client
}))

vi.mock('element-plus', () => ({
  ElMessage: message,
  ElMessageBox: messageBox
}))

vi.mock('lucide-vue-next', () => ({
  Plus: { template: '<span />' },
  Server: { template: '<span />' }
}))

import QueryDatasources from '../src/views/QueryDatasources.vue'

describe('QueryDatasources view', () => {
  beforeEach(() => {
    client.get.mockReset()
    client.post.mockReset()
    client.put.mockReset()
    client.delete.mockReset()
    message.error.mockReset()
    message.success.mockReset()
    message.warning.mockReset()
    messageBox.confirm.mockReset()
  })

  test('loads datasource counts, filters locally, and validates empty saves', async () => {
    // Covers src/views/QueryDatasources.js:load, src/views/QueryDatasources.js:matchesFilters, src/views/QueryDatasources.js:clearFilters, src/views/QueryDatasources.js:openCreate, src/views/QueryDatasources.js:resetForm, and src/views/QueryDatasources.js:save validation.
    client.get.mockResolvedValue({
      data: [
        { id: 1, name: 'default', type: 'kylin', isDefault: true, driverClass: 'driver', jdbcUrl: 'jdbc:1', maxPoolSize: 4, minIdle: 1, connectionTimeoutMs: 10000 },
        { id: 2, name: 'analytics', type: 'mysql', isDefault: false, driverClass: 'driver', jdbcUrl: 'jdbc:2', maxPoolSize: 4, minIdle: 1, connectionTimeoutMs: 10000 }
      ]
    })

    const { wrapper } = await mountView(QueryDatasources, { route: '/query-datasources' })
    await flushPromises()

    expect(wrapper.text()).toContain('Total 2 datasources')
    expect(wrapper.text()).toContain('Default 1')
    expect(wrapper.text()).toContain('Custom 1')

    await wrapper.find('input').setValue('analytics')
    await flushPromises()

    expect(wrapper.text()).toContain('Total 1 datasources')
    expect(client.get).toHaveBeenCalledTimes(1)

    const clearButton = wrapper.findAll('button').find((button) => button.text() === 'Clear Filter')
    await clearButton.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Total 2 datasources')

    const addButton = wrapper.findAll('button').find((button) => button.text() === 'Add Datasource')
    await addButton.trigger('click')
    await flushPromises()

    const saveButton = wrapper.findAll('button').find((button) => button.text() === 'Save')
    await saveButton.trigger('click')

    expect(message.warning).toHaveBeenCalledWith('Please enter a datasource name.')
  })

  test('edits and promotes a datasource through the save path', async () => {
    // Covers src/views/QueryDatasources.js:edit, src/views/QueryDatasources.js:promote, and src/views/QueryDatasources.js:save success path.
    client.get.mockResolvedValue({
      data: [
        { id: 2, name: 'analytics', type: 'mysql', isDefault: false, driverClass: 'driver', jdbcUrl: 'jdbc:mysql://db', username: 'svc', password: '', maxPoolSize: 6, minIdle: 2, connectionTimeoutMs: 15000 }
      ]
    })
    client.put.mockResolvedValue({ data: {} })

    const { wrapper } = await mountView(QueryDatasources, { route: '/query-datasources' })
    await flushPromises()

    const promoteButton = wrapper.findAll('button').find((button) => button.text() === 'Set Default')
    await promoteButton.trigger('click')
    await flushPromises()

    expect(client.put).toHaveBeenCalledWith(QUERY_DATASOURCE_BY_ID(2), expect.objectContaining({
      name: 'analytics',
      type: 'mysql',
      isDefault: true
    }))
    expect(message.success).toHaveBeenCalledWith('Datasource config saved')
  })

  test('confirms removal and reloads after delete', async () => {
    // Covers src/views/QueryDatasources.js:remove error-free branch.
    client.get.mockResolvedValue({
      data: [
        { id: 2, name: 'analytics', type: 'mysql', isDefault: false, driverClass: 'driver', jdbcUrl: 'jdbc:mysql://db', maxPoolSize: 6, minIdle: 2, connectionTimeoutMs: 15000 }
      ]
    })
    client.delete.mockResolvedValue({ data: {} })
    messageBox.confirm.mockResolvedValue()

    const { wrapper } = await mountView(QueryDatasources, { route: '/query-datasources' })
    await flushPromises()

    const removeButton = wrapper.findAll('button').find((button) => button.text() === 'Remove')
    await removeButton.trigger('click')
    await flushPromises()

    expect(messageBox.confirm).toHaveBeenCalled()
    expect(client.delete).toHaveBeenCalledWith(QUERY_DATASOURCE_BY_ID(2))
    expect(message.success).toHaveBeenCalledWith('Datasource config removed')
  })
})
