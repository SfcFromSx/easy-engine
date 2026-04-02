export const API_ENDPOINTS = {
  JOBS: '/jobs',
  RUNS: '/runs',
  TEST_SETS: '/test-sets',
  SQL_LIB: '/sql-lib',
  TEMPLATES: '/templates',
  DATASOURCES: '/datasources',
  DRIVERS: '/drivers',
  PREFLIGHT: '/preflight'
}

export const JOB_BY_ID = (id) => `/jobs/${id}`
export const RUNS_BY_JOB = (jobId) => `/runs?jobId=${jobId}`
export const RUN_START = '/runs/start'
export const TEST_SET_ITEMS = (id) => `/test-sets/${id}/items`
export const TEST_SET_ITEM_BY_ID = (testSetId, itemId) => `/test-sets/${testSetId}/items/${itemId}`
export const TEST_SET_ITEMS_ADD_SQL_LIB = (id) => `/test-sets/${id}/items/add-sql-lib`
export const TEST_SET_ITEMS_COPY_TEMPLATES = (id) => `/test-sets/${id}/items/copy-templates`
export const TEST_SET_ITEMS_REORDER = (id) => `/test-sets/${id}/items/reorder`
export const SQL_LIB_BY_ID = (id) => `/sql-lib/${id}`
export const SQL_LIB_UPLOAD = '/sql-lib/upload'
export const DATA_SOURCE_BY_ID = (id) => `/datasources/${id}`
export const DATA_SOURCE_QUERY = (id) => `/datasources/${id}/query`
export const DATA_SOURCE_TEST = '/datasources/test'
export const DRIVER_UPLOAD = '/drivers/upload'
