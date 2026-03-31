import { describe, expect, test } from 'vitest'
import { formatDateTime, formatDuration, formatPercent, shortFingerprint } from '../src/utils/formatters'

describe('formatters', () => {
  test('returns fallback text for missing or invalid dates', () => {
    // Covers src/utils/formatters.js:formatDateTime.
    expect(formatDateTime(null)).toBe('暂无数据')
    expect(formatDateTime('not-a-date')).toBe('时间格式异常')
  })

  test('formats duration and percentages with safe defaults', () => {
    // Covers src/utils/formatters.js:formatDuration and src/utils/formatters.js:formatPercent.
    expect(formatDuration(null)).toBe('--')
    expect(formatDuration(42)).toBe('42ms')
    expect(formatPercent(0, 0)).toBe('0%')
    expect(formatPercent(1, 4)).toBe('25%')
  })

  test('shortens long fingerprints without touching short values', () => {
    // Covers src/utils/formatters.js:shortFingerprint.
    expect(shortFingerprint(null)).toBe('--')
    expect(shortFingerprint('abcdef', 12)).toBe('abcdef')
    expect(shortFingerprint('abcdefghijklmnop', 8)).toBe('abcdefgh...')
  })
})
