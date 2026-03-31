import { describe, expect, it } from 'vitest'
import { getSqlPreview } from '../src/utils/sqlPreview'

describe('sqlPreview', () => {
  // Covers sqlPreview#getSqlPreview short SQL branch.
  it('keeps short single-line SQL unchanged', () => {
    const preview = getSqlPreview('select 1')

    expect(preview.previewText).toBe('select 1')
    expect(preview.lineCount).toBe(1)
    expect(preview.isTruncated).toBe(false)
  })

  // Covers sqlPreview#getSqlPreview long multi-line truncation branch.
  it('collapses long multi-line SQL into a compact preview', () => {
    const sql = `
      select *
      from huge_table
      where region = 'east'
      and metric > 100
      order by created_at desc
    `.trim()

    const preview = getSqlPreview(sql)

    expect(preview.lineCount).toBe(5)
    expect(preview.charCount).toBe(sql.length)
    expect(preview.isTruncated).toBe(true)
    expect(preview.previewText).toContain('select * from huge_table')
    expect(preview.previewText.endsWith('...')).toBe(true)
  })
})
