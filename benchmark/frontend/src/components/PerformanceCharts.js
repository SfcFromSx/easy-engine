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


export default {
  __name: 'PerformanceCharts',
  components: {
    VChart
  },
  props: {
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
},
  setup(__props, { expose: __expose }) {
  __expose();

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

const props = __props

const chartOption = computed(() => buildChartOption(props))

const __returned__ = { props, chartOption, computed, get use() { return use }, get CanvasRenderer() { return CanvasRenderer }, get LineChart() { return LineChart }, get BarChart() { return BarChart }, get PieChart() { return PieChart }, get GridComponent() { return GridComponent }, get TooltipComponent() { return TooltipComponent }, get LegendComponent() { return LegendComponent }, get TitleComponent() { return TitleComponent }, get VisualMapComponent() { return VisualMapComponent }, get DatasetComponent() { return DatasetComponent }, get VChart() { return VChart }, get buildChartOption() { return buildChartOption } }
return __returned__
}

}
