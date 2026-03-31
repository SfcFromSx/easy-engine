import { afterEach, vi } from 'vitest'
import { config } from '@vue/test-utils'

global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
})

Object.defineProperty(globalThis, 'navigator', {
  value: {
    userAgent: 'vitest',
    clipboard: {
      writeText: vi.fn().mockResolvedValue()
    }
  },
  configurable: true
})

global.URL.createObjectURL = vi.fn(() => 'blob:test')
global.URL.revokeObjectURL = vi.fn()

config.global.mocks = {
  ...config.global.mocks,
  $t: (key) => key
}

config.global.directives = {
  ...config.global.directives,
  loading: {}
}

afterEach(() => {
  vi.clearAllMocks()
})
