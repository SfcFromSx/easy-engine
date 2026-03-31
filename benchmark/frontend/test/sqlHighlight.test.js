import { highlightSql } from '../src/utils/sqlHighlight'

describe('sql highlighting', () => {
  // Covers sqlHighlight.js:highlightSql keyword, literal, comment, and escaping branches.
  it('escapes html and annotates sql tokens', () => {
    const highlighted = highlightSql("SELECT 1 FROM demo -- note\nWHERE id = '<tag>'")

    expect(highlighted).toContain('<span class="token-keyword">SELECT</span>')
    expect(highlighted).toContain('<span class="token-number">1</span>')
    expect(highlighted).toContain('<span class="token-comment">-- note</span>')
    expect(highlighted).toContain('&lt;tag&gt;')
  })
})
