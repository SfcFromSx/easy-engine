import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { nextTick } from 'vue'
import i18n from '../../src/i18n'

const ROUTES = [
  { path: '/', component: { template: '<div />' } },
  { path: '/traces', component: { template: '<div />' } },
  { path: '/patterns', component: { template: '<div />' } },
  { path: '/query-datasources', component: { template: '<div />' } },
  { path: '/acceleration', component: { template: '<div />' } }
]

export async function mountView(component, { route = '/', options = {} } = {}) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: ROUTES
  })

  await router.push(route)
  await router.isReady()

  const wrapper = mount(component, {
    ...options,
    global: {
      plugins: [router, i18n],
      ...(options.global || {})
    }
  })

  await nextTick()

  return { wrapper, router }
}
