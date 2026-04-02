import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const { clientGet, clientPost } = vi.hoisted(() => ({
  clientGet: vi.fn(),
  clientPost: vi.fn()
}))

vi.mock('../src/api/client', () => ({
  default: {
    get: clientGet,
    post: clientPost
  }
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key) => key
  })
}))

import Templates from '../src/views/Templates.vue'

const stubs = {
  'el-input': true,
  'el-select': true,
  'el-option': true,
  'el-button': true,
  'el-icon': true,
  'el-table': true,
  'el-table-column': true,
  'el-pagination': true,
  'el-dialog': true,
  'el-form': true,
  'el-form-item': true,
  'el-row': true,
  'el-col': true,
  'el-input-number': true,
  'el-tag': true,
  'el-upload': true,
  DebuggerDialog: true,
  Plus: true,
  Search: true,
  Play: true,
  UploadCloud: true
}

describe('Templates view', () => {
  beforeEach(() => {
    clientGet.mockReset()
    clientPost.mockReset()
    clientGet.mockResolvedValue({
      data: {
        content: [],
        totalElements: 0
      }
    })
  })

  // Covers Templates.vue SQL Lib list filtering request shaping.
  it('sends keyword and execution mode filters when reloading templates', async () => {
    const wrapper = shallowMount(Templates, {
      global: {
        stubs,
        directives: {
          loading: () => {}
        }
      }
    })
    await flushPromises()

    expect(clientGet).toHaveBeenCalledWith('/sql-lib', {
      params: {
        page: 0,
        size: 10,
        keyword: undefined,
        executionMode: undefined,
        sourceFilename: undefined
      }
    })

    wrapper.vm.searchKeyword = 'prepared'
    wrapper.vm.executionModeFilter = 'PREPARED_STATEMENT'
    wrapper.vm.sourceFilenameFilter = 'batch.sql'
    wrapper.vm.handleSearch()
    await flushPromises()

    expect(clientGet).toHaveBeenLastCalledWith('/sql-lib', {
      params: {
        page: 0,
        size: 10,
        keyword: 'prepared',
        executionMode: 'PREPARED_STATEMENT',
        sourceFilename: 'batch.sql'
      }
    })
  })

  // Covers Templates.vue upload flow hitting the SQL Lib upload endpoint.
  it('uploads selected files into sql lib', async () => {
    clientPost.mockResolvedValueOnce({ data: { count: 2 } })
    const wrapper = shallowMount(Templates, {
      global: {
        stubs,
        directives: {
          loading: () => {}
        }
      }
    })
    await flushPromises()

    wrapper.vm.handleUploadFileChange({
      raw: new Blob(['sql']),
      name: 'library.sql'
    })

    await wrapper.vm.uploadSelectedFile()
    await flushPromises()

    expect(clientPost).toHaveBeenCalledTimes(1)
    expect(clientPost.mock.calls[0][0]).toBe('/sql-lib/upload')
    expect(clientPost.mock.calls[0][1].get('file')).toBeTruthy()
  })
})
