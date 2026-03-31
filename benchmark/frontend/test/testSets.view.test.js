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

import TestSets from '../src/views/TestSets.vue'

const stubs = {
  'el-input': true,
  'el-select': true,
  'el-option': true,
  'el-button': true,
  'el-icon': true,
  'el-table': true,
  'el-table-column': true,
  'el-drawer': true,
  'el-dialog': true,
  'el-form': true,
  'el-form-item': true,
  'el-upload': true,
  'el-input-number': true,
  'el-tag': true,
  CodeBlock: true,
  Plus: true,
  UploadCloud: true
}

function buildClientGet(url) {
  if (url === '/test-sets') {
    return Promise.resolve({ data: [] })
  }
  if (url === '/templates') {
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
    clientGet.mockImplementation((url) => buildClientGet(url))
  })

  // Covers dialog-based upload creation in TestSets.vue.
  it('creates a new uploaded test set from the new-set dialog', async () => {
    clientPost.mockResolvedValueOnce({
      data: {
        testSet: { id: 8, name: 'Upload Set' },
        itemCount: 2
      }
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
    wrapper.vm.createMode = 'upload'
    wrapper.vm.form.name = 'Upload Set'
    wrapper.vm.form.description = 'Excel import'
    wrapper.vm.handleCreateFileChange({
      raw: new Blob(['sql']),
      name: 'cases.xlsx'
    })

    await wrapper.vm.saveTestSet()
    await flushPromises()

    expect(clientPost).toHaveBeenCalledTimes(1)
    expect(clientPost.mock.calls[0][0]).toBe('/test-sets/upload')
    expect(clientPost.mock.calls[0][1].get('name')).toBe('Upload Set')
    expect(clientPost.mock.calls[0][1].get('description')).toBe('Excel import')
    expect(clientPost.mock.calls[0][1].get('file')).toBeTruthy()
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
        return Promise.resolve({ data: [] })
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
    expect(wrapper.vm.drawerVisible).toBe(true)
    expect(clientGet).toHaveBeenCalledWith('/test-sets/5/items')
  })

  // Covers ordered template copy request shaping.
  it('copies selected templates into the active test set in visible order', async () => {
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
    wrapper.vm.availableTemplates = [
      { id: 10, name: 'First', sqlText: 'SELECT 1', weight: 1, executionMode: 'STATEMENT' },
      { id: 20, name: 'Second', sqlText: 'SELECT 2', weight: 1, executionMode: 'STATEMENT' }
    ]
    wrapper.vm.selectedTemplateIds = [20, 10]
    clientPost.mockResolvedValueOnce({ data: [] })

    await wrapper.vm.copySelectedTemplates()
    await flushPromises()

    expect(clientPost).toHaveBeenCalledWith('/test-sets/9/items/copy-templates', {
      templateIds: [10, 20]
    })
  })

  // Covers manual item save/delete/reorder actions.
  it('saves deletes and reorders test-set SQL rows through the item APIs', async () => {
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
      { id: 1, sortOrder: 0, sqlText: 'SELECT 1', executionMode: 'STATEMENT', weight: 1 },
      { id: 2, sortOrder: 1, sqlText: 'SELECT 2', executionMode: 'STATEMENT', weight: 1 }
    ]

    clientPost.mockResolvedValueOnce({ data: { id: 3 } })
    wrapper.vm.itemForm.sqlText = 'SELECT 3'
    wrapper.vm.itemForm.weight = 1
    wrapper.vm.itemForm.executionMode = 'STATEMENT'
    await wrapper.vm.saveItem()
    await flushPromises()
    expect(clientPost).toHaveBeenCalledWith('/test-sets/5/items', {
      label: '',
      sqlText: 'SELECT 3',
      weight: 1,
      executionMode: 'STATEMENT',
      paramJson: null
    })

    clientDelete.mockResolvedValueOnce({})
    await wrapper.vm.removeItem({ id: 1, sortOrder: 0, label: 'Row 1' })
    await flushPromises()
    expect(clientDelete).toHaveBeenCalledWith('/test-sets/5/items/1')

    wrapper.vm.items = [
      { id: 1, sortOrder: 0, sqlText: 'SELECT 1', executionMode: 'STATEMENT', weight: 1 },
      { id: 2, sortOrder: 1, sqlText: 'SELECT 2', executionMode: 'STATEMENT', weight: 1 }
    ]
    clientPut.mockResolvedValueOnce({ data: [] })
    await wrapper.vm.moveItem({ id: 1 }, 1)
    await flushPromises()
    expect(clientPut).toHaveBeenCalledWith('/test-sets/5/items/reorder', {
      itemIds: [2, 1]
    })
  })
})
