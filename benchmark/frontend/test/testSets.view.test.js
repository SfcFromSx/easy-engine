import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const {
  clientGet,
  clientPost,
  clientPut,
  clientDelete,
  messageSuccess,
  messageError,
  messageWarning,
  confirmDialog
} = vi.hoisted(() => ({
  clientGet: vi.fn(),
  clientPost: vi.fn(),
  clientPut: vi.fn(),
  clientDelete: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
  messageWarning: vi.fn(),
  confirmDialog: vi.fn()
}))

vi.mock('../src/api/client', () => ({
  default: {
    get: clientGet,
    post: clientPost,
    put: clientPut,
    delete: clientDelete
  }
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: messageSuccess,
    error: messageError,
    warning: messageWarning
  },
  ElMessageBox: {
    confirm: confirmDialog
  }
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key, params) => (params ? `${key}:${JSON.stringify(params)}` : key)
  })
}))

const push = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push
  })
}))

import TestSets from '../src/views/TestSets.vue'

const stubs = {
  'el-input': true,
  'el-select': true,
  'el-option': true,
  'el-button': true,
  'el-icon': true,
  'el-table': true,
  'el-table-column': true,
  'el-dialog': true,
  'el-form': true,
  'el-form-item': true,
  'el-input-number': true,
  'el-tag': true,
  CodeBlock: true,
  'el-pagination': true,
  Plus: true
}

function buildClientGet(url) {
  if (url === '/test-sets') {
    return Promise.resolve({ data: [] })
  }
  if (url === '/sql-lib') {
    return Promise.resolve({
      data: {
        content: [],
        totalElements: 0
      }
    })
  }
  if (url.includes('/items')) {
    return Promise.resolve({ data: [] })
  }
  return Promise.resolve({ data: [] })
}

describe('TestSets view', () => {
  beforeEach(() => {
    clientGet.mockReset()
    clientPost.mockReset()
    clientPut.mockReset()
    clientDelete.mockReset()
    messageSuccess.mockReset()
    messageError.mockReset()
    messageWarning.mockReset()
    confirmDialog.mockReset()
    confirmDialog.mockResolvedValue(true)
    push.mockReset()
    clientGet.mockImplementation((url) => buildClientGet(url))
  })

  // Covers empty-set creation opening the SQL manager immediately.
  it('opens the SQL manager after creating an empty test set', async () => {
    clientGet.mockImplementation((url) => {
      if (url === '/test-sets') {
        return Promise.resolve({
          data: [{ id: 5, name: 'Manual Set', description: 'desc', itemCount: 0 }]
        })
      }
      if (url === '/test-sets/5/items') {
        return Promise.resolve({
          data: {
            content: [],
            totalElements: 0
          }
        })
      }
      return buildClientGet(url)
    })
    clientPost.mockResolvedValueOnce({
      data: { id: 5, name: 'Manual Set', description: 'desc' }
    })

    const wrapper = shallowMount(TestSets, {
      global: {
        stubs,
        directives: {
          loading: () => {}
        }
      }
    })
    await flushPromises()

    wrapper.vm.openCreate()
    wrapper.vm.form.name = 'Manual Set'
    wrapper.vm.form.description = 'desc'

    await wrapper.vm.saveTestSet()
    await flushPromises()

    expect(clientPost).toHaveBeenCalledWith('/test-sets', {
      id: null,
      name: 'Manual Set',
      description: 'desc'
    })
    expect(wrapper.vm.managerVisible).toBe(true)
    expect(clientGet).toHaveBeenCalledWith('/test-sets/5/items', {
      params: {
        page: 0,
        size: 20,
        keyword: undefined
      }
    })
  })

  // Covers ordered SQL Lib add request shaping.
  it('adds selected sql lib rows into the active test set in visible order', async () => {
    const wrapper = shallowMount(TestSets, {
      global: {
        stubs,
        directives: {
          loading: () => {}
        }
      }
    })
    await flushPromises()

    wrapper.vm.activeSet = { id: 9, name: 'Target Set' }
    wrapper.vm.availableSqlLib = [
      { id: 10, name: 'First', sqlText: 'SELECT 1', weight: 1, executionMode: 'STATEMENT' },
      { id: 20, name: 'Second', sqlText: 'SELECT 2', weight: 1, executionMode: 'STATEMENT' }
    ]
    wrapper.vm.selectedSqlLibIds = [20, 10]
    clientPost.mockResolvedValueOnce({ data: [] })

    await wrapper.vm.addSelectedSqlLib()
    await flushPromises()

    expect(clientPost).toHaveBeenCalledWith('/test-sets/9/items/add-sql-lib', {
      sqlLibIds: [10, 20]
    })
  })

  // Covers delete/reorder actions for SQL Lib-backed test-set memberships.
  it('deletes and reorders test-set sql memberships through the item APIs', async () => {
    const wrapper = shallowMount(TestSets, {
      global: {
        stubs,
        directives: {
          loading: () => {}
        }
      }
    })
    await flushPromises()

    wrapper.vm.activeSet = { id: 5, name: 'Manual Set' }
    wrapper.vm.items = [
      { id: 1, sortOrder: 0, name: 'SELECT 1', sqlText: 'SELECT 1', executionMode: 'STATEMENT', weight: 1 },
      { id: 2, sortOrder: 1, name: 'SELECT 2', sqlText: 'SELECT 2', executionMode: 'STATEMENT', weight: 1 }
    ]

    clientDelete.mockResolvedValueOnce({})
    await wrapper.vm.removeItem({ id: 1, sortOrder: 0, name: 'SQL 1' })
    await flushPromises()
    expect(clientDelete).toHaveBeenCalledWith('/test-sets/5/items/1')

    wrapper.vm.items = [
      { id: 1, sortOrder: 0, name: 'SELECT 1', sqlText: 'SELECT 1', executionMode: 'STATEMENT', weight: 1 },
      { id: 2, sortOrder: 1, name: 'SELECT 2', sqlText: 'SELECT 2', executionMode: 'STATEMENT', weight: 1 }
    ]
    clientPut.mockResolvedValueOnce({ data: [] })
    await wrapper.vm.moveItem({ id: 1 }, 1)
    await flushPromises()
    expect(clientPut).toHaveBeenCalledWith('/test-sets/5/items/reorder', {
      itemIds: [2, 1]
    })
  })

  // Covers opening the SQL Lib page from the selected test-set item.
  it('navigates to sql lib from the selected item detail action', async () => {
    const wrapper = shallowMount(TestSets, {
      global: {
        stubs,
        directives: {
          loading: () => {}
        }
      }
    })
    await flushPromises()

    wrapper.vm.openSelectedInSqlLib()
    expect(push).toHaveBeenCalledWith('/sql-lib')
  })
})
