export function highlightSql(code) {
  if (!code) {
    return ''
  }

  let html = code
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  const keywords = [
    'SELECT', 'FROM', 'WHERE', 'GROUP BY', 'ORDER BY', 'LIMIT', 'JOIN', 'LEFT', 'RIGHT', 'INNER', 'ON',
    'AS', 'AND', 'OR', 'NOT', 'IN', 'IS', 'NULL', 'CASE', 'WHEN', 'THEN', 'ELSE', 'END',
    'UNION', 'ALL', 'EXISTS', 'HAVING', 'DISTINCT', 'COUNT', 'SUM', 'AVG', 'MIN', 'MAX',
    'INSERT', 'INTO', 'VALUES', 'UPDATE', 'SET', 'DELETE', 'CREATE', 'TABLE', 'DROP', 'ALTER'
  ]
  const keywordRegex = new RegExp(`\\b(${keywords.join('|')})\\b`, 'gi')
  html = html.replace(keywordRegex, '<span class="token-keyword">$1</span>')
  html = html.replace(/'([^']*)'/g, '<span class="token-string">\'$1\'</span>')
  html = html.replace(/\b(\d+)\b/g, '<span class="token-number">$1</span>')
  html = html.replace(/(--.*$)/gm, '<span class="token-comment">$1</span>')
  html = html.replace(/(\/\*[\s\S]*?\*\/)/g, '<span class="token-comment">$1</span>')
  return html
}
