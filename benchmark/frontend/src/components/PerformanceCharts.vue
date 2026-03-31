<template>
  <div class="chart-wrapper glass-card">
    <div class="chart-header">
      <h3 class="chart-title">{{ title }}</h3>
      <div class="chart-actions">
        <slot name="actions"></slot>
      </div>
    </div>
    <v-chart class="chart" :option="chartOption" autoresize />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  VisualMapComponent,
  DatasetComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  VisualMapComponent,
  DatasetComponent
])

const props = defineProps({
  title: String,
  type: {
    type: String,
    default: 'line'
  },
  data: {
    type: Array,
    default: () => []
  },
  xKey: String,
  yKeys: Array, // Array of { name, key, color }
  pKeys: Array  // Array of { name, key } for pie
})

const chartOption = computed(() => {
  if (props.type === 'line') {
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        textStyle: { color: '#f8fafc' },
        formatter: (params) => {
          let res = `<div style="font-weight: 600; margin-bottom: 4px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 4px;">Run #${params[0].axisValue}</div>`
          params.forEach(p => {
            res += `<div style="display: flex; justify-content: space-between; gap: 20px;">
              <span>${p.marker} ${p.seriesName}</span>
              <span style="font-weight: 700; margin-left: 10px;">${p.value}</span>
            </div>`
          })
          return res
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
        data: props.data.map(item => item[props.xKey]),
        axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
        axisLabel: { color: '#94a3b8' }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } },
        axisLabel: { color: '#94a3b8' }
      },
      series: props.yKeys.map((y, index) => ({
        name: y.name,
        type: 'line',
        smooth: true,
        data: props.data.map(item => item[y.key]),
        itemStyle: { color: y.color || '#3b82f6' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: (y.color || '#3b82f6') + '44' },
              { offset: 1, color: (y.color || '#3b82f6') + '00' }
            ]
          }
        }
      }))
    }
  } else if (props.type === 'bar') {
     return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        textStyle: { color: '#f8fafc' },
        formatter: (params) => {
          let res = `<div style="font-weight: 600; margin-bottom: 4px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 4px;">Run #${params[0].axisValue}</div>`
          params.forEach(p => {
             res += `<div style="display: flex; justify-content: space-between; gap: 20px;">
              <span>${p.marker} 值</span>
              <span style="font-weight: 700; margin-left: 10px;">${p.value}</span>
            </div>`
          })
          return res
        }
      },
      grid: { top: '10%', left: '3%', right: '4%', bottom: '5%', containLabel: true },
      xAxis: {
        type: 'category',
        data: props.data.map(item => item[props.xKey]),
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
          data: props.data.map(item => item[props.yKeys[0].key]),
          itemStyle: {
            borderRadius: [4, 4, 0, 0],
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [{ offset: 0, color: '#3b82f6' }, { offset: 1, color: '#6366f1' }]
            }
          }
        }
      ]
    }
  }
  return {}
})
</script>

<style scoped>
.chart-wrapper {
  padding: 16px 20px;
  height: 300px;
  display: flex;
  flex-direction: column;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.chart-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b; /* Darker slate for better contrast */
}
.chart {
  flex: 1;
  min-height: 0;
}
</style>
