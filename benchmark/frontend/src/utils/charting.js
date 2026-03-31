export function buildChartOption({ type = 'line', data = [], xKey, yKeys = [] }) {
  if (type === 'line') {
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        textStyle: { color: '#f8fafc' },
        formatter: (params) => {
          let result = `<div style="font-weight: 600; margin-bottom: 4px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 4px;">Run #${params[0].axisValue}</div>`
          params.forEach((item) => {
            result += `<div style="display: flex; justify-content: space-between; gap: 20px;">
              <span>${item.marker} ${item.seriesName}</span>
              <span style="font-weight: 700; margin-left: 10px;">${item.value}</span>
            </div>`
          })
          return result
        }
      },
      legend: {
        bottom: '0',
        textStyle: { color: '#94a3b8' }
      },
      grid: {
        top: '10%',
        left: '3%',
        right: '4%',
        bottom: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: data.map((item) => item[xKey]),
        axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
        axisLabel: { color: '#94a3b8' }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } },
        axisLabel: { color: '#94a3b8' }
      },
      series: yKeys.map((item) => ({
        name: item.name,
        type: 'line',
        smooth: true,
        data: data.map((row) => row[item.key]),
        itemStyle: { color: item.color || '#3b82f6' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: `${item.color || '#3b82f6'}44` },
              { offset: 1, color: `${item.color || '#3b82f6'}00` }
            ]
          }
        }
      }))
    }
  }

  if (type === 'bar') {
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        textStyle: { color: '#f8fafc' },
        formatter: (params) => {
          let result = `<div style="font-weight: 600; margin-bottom: 4px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 4px;">Run #${params[0].axisValue}</div>`
          params.forEach((item) => {
            result += `<div style="display: flex; justify-content: space-between; gap: 20px;">
              <span>${item.marker} 值</span>
              <span style="font-weight: 700; margin-left: 10px;">${item.value}</span>
            </div>`
          })
          return result
        }
      },
      grid: { top: '10%', left: '3%', right: '4%', bottom: '5%', containLabel: true },
      xAxis: {
        type: 'category',
        data: data.map((item) => item[xKey]),
        axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
        axisLabel: { color: '#94a3b8', interval: 0, rotate: 30 }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } },
        axisLabel: { color: '#94a3b8' }
      },
      series: [
        {
          type: 'bar',
          data: data.map((item) => item[yKeys[0]?.key]),
          itemStyle: {
            borderRadius: [4, 4, 0, 0],
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [{ offset: 0, color: '#3b82f6' }, { offset: 1, color: '#6366f1' }]
            }
          }
        }
      ]
    }
  }

  return {}
}
