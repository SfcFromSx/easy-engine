import router from '../src/router'

describe('benchmark router', () => {
  // Covers router/index.js route registration for benchmark operator pages.
  it('registers the dashboard, configuration, and history routes', () => {
    const routes = router.getRoutes()

    expect(routes.find((route) => route.path === '/')).toBeTruthy()
    expect(routes.find((route) => route.path === '/jobs/:id')?.meta.title).toBe('任务详情')
    expect(routes.find((route) => route.path === '/runs')?.meta.title).toBe('运行记录')
    expect(routes.find((route) => route.path === '/datasources')).toBeTruthy()
  })
})
