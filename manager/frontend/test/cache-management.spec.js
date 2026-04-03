import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { mountView } from './support/mountView'
import { API_ENDPOINTS, CACHE_KEY_BY_KEY } from '../src/api/endpoints'

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
  Database: { template: '<span />' },
  Pencil: { template: '<span />' },
  Plus: { template: '<span />' },
  RefreshCw: { template: '<span />' },
  Trash2: { template: '<span />' }
}))

import CacheManagement from '../src/views/CacheManagement.vue'

describe('CacheManagement view', () => {
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

  test('loads cache summary and keys on mount', async () => {
    // Covers src/views/CacheManagement.js:loadCacheInfo, src/views/CacheManagement.js:loadCacheKeys, and onMounted refresh.
    client.get
      .mockResolvedValueOnce({ data: { totalSizeBytes: 2097152, keyCount: 1 } })
      .mockResolvedValueOnce({ data: [{ key: 'kylin_cache:key1', sizeBytes: 32, ttlSeconds: 120 }] })

    const { wrapper } = await mountView(CacheManagement, { route: '/cache' })
    await flushPromises()

    expect(client.get).toHaveBeenNthCalledWith(1, API_ENDPOINTS.CACHE_INFO)
    expect(client.get).toHaveBeenNthCalledWith(2, API_ENDPOINTS.CACHE_KEYS, {
      params: { offset: 0, limit: 20 }
    })
    expect(wrapper.text()).toContain('2.00 MB')
    expect(wrapper.text()).toContain('1')
    expect(wrapper.text()).toContain('kylin_cache:key1')
  })

  test('validates cache form before create', async () => {
    // Covers src/views/CacheManagement.js:openCreate and validation failures in save.
    client.get
      .mockResolvedValueOnce({ data: { totalSizeBytes: 0, keyCount: 0 } })
      .mockResolvedValueOnce({ data: [] })

    const { wrapper } = await mountView(CacheManagement, { route: '/cache' })
    await flushPromises()

    const addButton = wrapper.findAll('button').find((button) => button.text() === 'Add Cache Key')
    await addButton.trigger('click')
    await flushPromises()

    const saveButton = wrapper.findAll('button').find((button) => button.text() === 'Create')
    await saveButton.trigger('click')

    expect(message.warning).toHaveBeenCalledWith('Please enter a cache key.')
  })

  test('creates a cache key and refreshes the page data', async () => {
    // Covers src/views/CacheManagement.js:create success path.
    client.get
      .mockResolvedValueOnce({ data: { totalSizeBytes: 0, keyCount: 0 } })
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({ data: { totalSizeBytes: 11, keyCount: 1 } })
      .mockResolvedValueOnce({ data: [{ key: 'kylin_cache:new', sizeBytes: 11, ttlSeconds: 90 }] })
    client.post.mockResolvedValue({ data: {} })

    const { wrapper } = await mountView(CacheManagement, { route: '/cache' })
    await flushPromises()

    wrapper.vm.openCreate()
    wrapper.vm.form.key = 'kylin_cache:new'
    wrapper.vm.form.value = '{"ok":true}'
    wrapper.vm.form.ttlSeconds = 90
    await wrapper.vm.save()
    await flushPromises()

    expect(client.post).toHaveBeenCalledWith(API_ENDPOINTS.CACHE_KEYS, {
      key: 'kylin_cache:new',
      value: '{"ok":true}',
      ttlSeconds: 90
    })
    expect(message.success).toHaveBeenCalledWith('Cache key created successfully')
  })

  test('loads detail before editing and saves the updated payload', async () => {
    // Covers src/views/CacheManagement.js:openEdit and update success path.
    client.get
      .mockResolvedValueOnce({ data: { totalSizeBytes: 32, keyCount: 1 } })
      .mockResolvedValueOnce({ data: [{ key: 'kylin_cache:key1', sizeBytes: 32, ttlSeconds: 120 }] })
      .mockResolvedValueOnce({ data: { key: 'kylin_cache:key1', value: '{"rows":1}', sizeBytes: 10, ttlSeconds: 120 } })
      .mockResolvedValueOnce({ data: { totalSizeBytes: 32, keyCount: 1 } })
      .mockResolvedValueOnce({ data: [{ key: 'kylin_cache:key1', sizeBytes: 32, ttlSeconds: 180 }] })
    client.put.mockResolvedValue({ data: {} })

    const { wrapper } = await mountView(CacheManagement, { route: '/cache' })
    await flushPromises()

    await wrapper.vm.openEdit({ key: 'kylin_cache:key1' })
    await flushPromises()

    expect(client.get).toHaveBeenNthCalledWith(3, CACHE_KEY_BY_KEY('kylin_cache:key1'))

    wrapper.vm.form.value = '{"rows":2}'
    wrapper.vm.form.ttlSeconds = 180
    await wrapper.vm.save()
    await flushPromises()

    expect(client.put).toHaveBeenCalledWith(CACHE_KEY_BY_KEY('kylin_cache:key1'), {
      value: '{"rows":2}',
      ttlSeconds: 180
    })
    expect(message.success).toHaveBeenCalledWith('Cache key updated successfully')
  })

  test('confirms delete and refreshes cache data afterwards', async () => {
    // Covers src/views/CacheManagement.js:deleteKey success path.
    client.get
      .mockResolvedValueOnce({ data: { totalSizeBytes: 32, keyCount: 1 } })
      .mockResolvedValueOnce({ data: [{ key: 'kylin_cache:key1', sizeBytes: 32, ttlSeconds: 120 }] })
      .mockResolvedValueOnce({ data: { totalSizeBytes: 0, keyCount: 0 } })
      .mockResolvedValueOnce({ data: [] })
    client.delete.mockResolvedValue({ data: {} })
    messageBox.confirm.mockResolvedValue()

    const { wrapper } = await mountView(CacheManagement, { route: '/cache' })
    await flushPromises()

    await wrapper.vm.deleteKey('kylin_cache:key1')
    await flushPromises()

    expect(messageBox.confirm).toHaveBeenCalled()
    expect(client.delete).toHaveBeenCalledWith(CACHE_KEY_BY_KEY('kylin_cache:key1'))
    expect(message.success).toHaveBeenCalledWith('Cache key deleted successfully')
  })
})
