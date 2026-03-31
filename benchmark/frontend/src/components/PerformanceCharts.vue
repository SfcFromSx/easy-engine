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
import { buildChartOption } from '../utils/charting'

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

const chartOption = computed(() => buildChartOption(props))
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
