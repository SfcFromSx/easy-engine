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
  Search: { template: '<span />' },
  Trash2: { template: '<span />' }
}))

import CacheManagement from '../src/views/CacheManagement.vue'

const cacheInfoResponse = {
  managedKeyPrefix: 'kylin_cache:',
  exactSummaryAvailable: false,
  summaryMessage: 'Exact totals disabled'
}

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

  test('loads cache policy on mount without auto-loading keys', async () => {
    // Covers src/views/CacheManagement.js:loadCacheInfo and onMounted policy-only load.
    client.get.mockResolvedValueOnce({ data: cacheInfoResponse })

    const { wrapper } = await mountView(CacheManagement, { route: '/cache' })
    await flushPromises()

    expect(client.get).toHaveBeenCalledTimes(1)
    expect(client.get).toHaveBeenCalledWith(API_ENDPOINTS.CACHE_INFO)
    expect(wrapper.text()).toContain('kylin_cache:')
    expect(wrapper.text()).toContain('Exact live totals are disabled')
  })

  test('validates the search prefix before loading keys', async () => {
    // Covers src/views/CacheManagement.js:validateSearchPrefix too-broad branch.
    client.get.mockResolvedValueOnce({ data: cacheInfoResponse })

    const { wrapper } = await mountView(CacheManagement, { route: '/cache' })
    await flushPromises()

    wrapper.vm.searchPrefix = 'kylin_cache:'
    await wrapper.vm.submitSearch()

    expect(message.warning).toHaveBeenCalledWith('Cache key prefix must be narrower than kylin_cache:.')
    expect(client.get).toHaveBeenCalledTimes(1)
  })

  test('searches cache keys with prefix and cursor params', async () => {
    // Covers src/views/CacheManagement.js:submitSearch and first-page loadCacheKeys path.
    client.get
      .mockResolvedValueOnce({ data: cacheInfoResponse })
      .mockResolvedValueOnce({
        data: {
          queryPrefix: 'kylin_cache:key',
          nextCursor: '17',
          hasMore: true,
          items: [{ key: 'kylin_cache:key1', sizeBytes: 32, ttlSeconds: 120 }]
        }
      })

    const { wrapper } = await mountView(CacheManagement, { route: '/cache' })
    await flushPromises()

    wrapper.vm.searchPrefix = 'kylin_cache:key'
    await wrapper.vm.submitSearch()
    await flushPromises()

    expect(client.get).toHaveBeenNthCalledWith(2, API_ENDPOINTS.CACHE_KEYS, {
      params: { prefix: 'kylin_cache:key', cursor: '0', limit: 20 }
    })
    expect(wrapper.text()).toContain('kylin_cache:key1')
  })

  test('navigates cache results with next and previous cursors', async () => {
    // Covers src/views/CacheManagement.js:goNext and goPrevious cursor history flow.
    client.get
      .mockResolvedValueOnce({ data: cacheInfoResponse })
      .mockResolvedValueOnce({
        data: {
          queryPrefix: 'kylin_cache:key',
          nextCursor: '17',
          hasMore: true,
          items: [{ key: 'kylin_cache:key1', sizeBytes: 32, ttlSeconds: 120 }]
        }
      })
      .mockResolvedValueOnce({
        data: {
          queryPrefix: 'kylin_cache:key',
          nextCursor: '0',
          hasMore: false,
          items: [{ key: 'kylin_cache:key2', sizeBytes: 16, ttlSeconds: 90 }]
        }
      })
      .mockResolvedValueOnce({
        data: {
          queryPrefix: 'kylin_cache:key',
          nextCursor: '17',
          hasMore: true,
          items: [{ key: 'kylin_cache:key1', sizeBytes: 32, ttlSeconds: 120 }]
        }
      })

    const { wrapper } = await mountView(CacheManagement, { route: '/cache' })
    await flushPromises()

    wrapper.vm.searchPrefix = 'kylin_cache:key'
    await wrapper.vm.submitSearch()
    await flushPromises()
    await wrapper.vm.goNext()
    await flushPromises()
    await wrapper.vm.goPrevious()
    await flushPromises()

    expect(client.get).toHaveBeenNthCalledWith(3, API_ENDPOINTS.CACHE_KEYS, {
      params: { prefix: 'kylin_cache:key', cursor: '17', limit: 20 }
    })
    expect(client.get).toHaveBeenNthCalledWith(4, API_ENDPOINTS.CACHE_KEYS, {
      params: { prefix: 'kylin_cache:key', cursor: '0', limit: 20 }
    })
    expect(wrapper.text()).toContain('kylin_cache:key1')
  })

  test('loads detail before editing and refreshes the active search from cursor zero', async () => {
    // Covers src/views/CacheManagement.js:openEdit, save update path, and refreshAfterMutation.
    client.get
      .mockResolvedValueOnce({ data: cacheInfoResponse })
      .mockResolvedValueOnce({
        data: {
          queryPrefix: 'kylin_cache:key',
          nextCursor: '17',
          hasMore: true,
          items: [{ key: 'kylin_cache:key1', sizeBytes: 32, ttlSeconds: 120 }]
        }
      })
      .mockResolvedValueOnce({ data: { key: 'kylin_cache:key1', value: '{"rows":1}', sizeBytes: 10, ttlSeconds: 120 } })
      .mockResolvedValueOnce({ data: cacheInfoResponse })
      .mockResolvedValueOnce({
        data: {
          queryPrefix: 'kylin_cache:key',
          nextCursor: '0',
          hasMore: false,
          items: [{ key: 'kylin_cache:key1', sizeBytes: 32, ttlSeconds: 180 }]
        }
      })
    client.put.mockResolvedValue({ data: {} })

    const { wrapper } = await mountView(CacheManagement, { route: '/cache' })
    await flushPromises()

    wrapper.vm.searchPrefix = 'kylin_cache:key'
    await wrapper.vm.submitSearch()
    await flushPromises()
    await wrapper.vm.openEdit({ key: 'kylin_cache:key1' })
    await flushPromises()

    wrapper.vm.form.value = '{"rows":2}'
    wrapper.vm.form.ttlSeconds = 180
    await wrapper.vm.save()
    await flushPromises()

    expect(client.get).toHaveBeenNthCalledWith(3, CACHE_KEY_BY_KEY('kylin_cache:key1'))
    expect(client.put).toHaveBeenCalledWith(CACHE_KEY_BY_KEY('kylin_cache:key1'), {
      value: '{"rows":2}',
      ttlSeconds: 180
    })
    expect(client.get).toHaveBeenNthCalledWith(5, API_ENDPOINTS.CACHE_KEYS, {
      params: { prefix: 'kylin_cache:key', cursor: '0', limit: 20 }
    })
    expect(message.success).toHaveBeenCalledWith('Cache key updated successfully')
  })

  test('confirms delete and refreshes the active search from cursor zero', async () => {
    // Covers src/views/CacheManagement.js:deleteKey success path with active-search refresh.
    client.get
      .mockResolvedValueOnce({ data: cacheInfoResponse })
      .mockResolvedValueOnce({
        data: {
          queryPrefix: 'kylin_cache:key',
          nextCursor: '17',
          hasMore: true,
          items: [{ key: 'kylin_cache:key1', sizeBytes: 32, ttlSeconds: 120 }]
        }
      })
      .mockResolvedValueOnce({ data: cacheInfoResponse })
      .mockResolvedValueOnce({
        data: {
          queryPrefix: 'kylin_cache:key',
          nextCursor: '0',
          hasMore: false,
          items: []
        }
      })
    client.delete.mockResolvedValue({ data: {} })
    messageBox.confirm.mockResolvedValue()

    const { wrapper } = await mountView(CacheManagement, { route: '/cache' })
    await flushPromises()

    wrapper.vm.searchPrefix = 'kylin_cache:key'
    await wrapper.vm.submitSearch()
    await flushPromises()
    await wrapper.vm.deleteKey('kylin_cache:key1')
    await flushPromises()

    expect(messageBox.confirm).toHaveBeenCalled()
    expect(client.delete).toHaveBeenCalledWith(CACHE_KEY_BY_KEY('kylin_cache:key1'))
    expect(client.get).toHaveBeenNthCalledWith(4, API_ENDPOINTS.CACHE_KEYS, {
      params: { prefix: 'kylin_cache:key', cursor: '0', limit: 20 }
    })
    expect(message.success).toHaveBeenCalledWith('Cache key deleted successfully')
  })
})
