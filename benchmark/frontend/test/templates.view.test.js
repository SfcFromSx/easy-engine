import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const { clientGet } = vi.hoisted(() => ({
  clientGet: vi.fn()
}))

vi.mock('../src/api/client', () => ({
  default: {
    get: clientGet
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
  DebuggerDialog: true,
  Plus: true,
  Search: true,
  Play: true
}

describe('Templates view', () => {
  beforeEach(() => {
    clientGet.mockReset()
    clientGet.mockResolvedValue({
      data: {
        content: [],
        totalElements: 0
      }
    })
  })

  // Covers Templates.vue:load and Templates.vue:handleSearch request parameter shaping.
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

    expect(clientGet).toHaveBeenCalledWith('/templates', {
      params: {
        page: 0,
        size: 10,
        keyword: undefined,
        executionMode: undefined
      }
    })

    wrapper.vm.searchKeyword = 'prepared'
    wrapper.vm.executionModeFilter = 'PREPARED_STATEMENT'
    wrapper.vm.handleSearch()
    await flushPromises()

    expect(clientGet).toHaveBeenLastCalledWith('/templates', {
      params: {
        page: 0,
        size: 10,
        keyword: 'prepared',
        executionMode: 'PREPARED_STATEMENT'
      }
    })
  })
})
