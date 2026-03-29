const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit'
})

export function formatDateTime(value) {
  if (!value) return '暂无数据'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '时间格式异常'
  return dateTimeFormatter.format(date)
}

export function formatDuration(value) {
  if (value == null) return '--'
  return `${value}ms`
}

export function formatPercent(part, total) {
  if (!total) return '0%'
  return `${Math.round((part / total) * 100)}%`
}

export function shortFingerprint(value, length = 12) {
  if (!value) return '--'
  return value.length <= length ? value : `${value.slice(0, length)}...`
}
