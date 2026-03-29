export const API_ENDPOINTS = {
  TRACES: '/traces',
  PATTERNS_TOP: '/patterns/top',
  ACCELERATION_TABLES: '/acceleration-tables',
  STATS_SUMMARY: '/stats/summary'
}

export const ACCELERATION_BY_ID = (id) => `/acceleration-tables/${id}`
export const ACCEL_FROM_PATTERN = '/acceleration-tables/from-pattern'
export const ACCELERATION_STATUS = (id) => `/acceleration-tables/${id}/status`
