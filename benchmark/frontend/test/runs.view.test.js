import { nextTick, reactive } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, vi } from 'vitest'

const { replace, clientGet } = vi.hoisted(() => ({
  replace: vi.fn(),
  clientGet: vi.fn()
}))

let routeState

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({ replace })
}))

vi.mock('../src/api/client', () => ({
  default: {
    get: clientGet
  }
}))

import Runs from '../src/views/Runs.vue'

const stubs = {
  'el-select': true,
  'el-option': true,
  'el-button': true,
  'el-tag': true,
  'el-table': true,
  'el-table-column': true,
  'el-pagination': true,
  'el-drawer': true,
  'el-descriptions': true,
  'el-descriptions-item': true,
  'el-icon': true,
  PerformanceCharts: true,
  RefreshRight: true,
  CaretTop: true,
  CaretBottom: true
}

describe('Runs view', () => {
  beforeEach(() => {
    routeState = reactive({ query: { jobId: '7', status: 'RUNNING' } })
    replace.mockReset()
    clientGet.mockReset()
    clientGet.mockImplementation((url, options) => {
      if (url === '/jobs') {
        return Promise.resolve({ data: [{ id: 7, name: 'Primary Job' }, { id: 9, name: 'Fallback Job' }] })
      }
      if (url === '/runs') {
        return Promise.resolve({
          data: {
            content: [{ id: 3, status: 'RUNNING', currentProgress: 2, totalTarget: 5 }],
            totalElements: 1
          }
        })
      }
      if (url === '/runs/active') {
        return Promise.resolve({ data: null })
      }
      throw new Error(`Unexpected request: ${url} ${JSON.stringify(options || {})}`)
    })
    vi.useFakeTimers()
  })

  // Covers Runs.vue:onMounted, Runs.vue:loadJobs, Runs.vue:load, and Runs.vue route watcher sync.
  it('loads the selected job and refreshes when the route query changes', async () => {
    shallowMount(Runs, {
      global: {
        stubs,
        mocks: {
          $t: (key) => key
        },
        directives: {
          loading: () => {}
        }
      }
    })
    await flushPromises()

    expect(clientGet).toHaveBeenCalledWith('/jobs')
    expect(clientGet).toHaveBeenCalledWith('/runs', {
      params: { jobId: '7', status: 'RUNNING', page: 0, size: 20 }
    })

    routeState.query.jobId = '9'
    routeState.query.status = 'FAILED'
    await nextTick()
    await flushPromises()

    expect(clientGet).toHaveBeenCalledWith('/runs', {
      params: { jobId: '9', status: 'FAILED', page: 0, size: 20 }
    })
  })
})
