const PREVIEW_CHAR_LIMIT = 180
const PREVIEW_LINE_THRESHOLD = 3

function normalizeSql(sqlText) {
  return typeof sqlText === 'string' ? sqlText.trim() : ''
}

function countLines(sqlText) {
  const normalized = normalizeSql(sqlText)
  if (!normalized) {
    return 0
  }
  return normalized.split(/\r?\n/).length
}

function compactWhitespace(sqlText) {
  return normalizeSql(sqlText).replace(/\s+/g, ' ')
}

export function getSqlPreview(sqlText) {
  const normalized = normalizeSql(sqlText)
  const compact = compactWhitespace(normalized)
  const lineCount = countLines(normalized)
  const charCount = normalized.length
  const isTruncated = compact.length > PREVIEW_CHAR_LIMIT || lineCount > PREVIEW_LINE_THRESHOLD
  const previewText = isTruncated ? `${compact.slice(0, PREVIEW_CHAR_LIMIT).trimEnd()}...` : compact

  return {
    previewText,
    lineCount,
    charCount,
    isTruncated
  }
}
