import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshRight, CaretTop, CaretBottom } from '@element-plus/icons-vue'
import client from '../api/client'
import { API_ENDPOINTS } from '../api/endpoints'
import PerformanceCharts from '../components/PerformanceCharts.vue'
import {
  buildRunsChartData,
  formatDeltas,
  formatJson,
  parseEvaluationJson,
  verdictTag
} from '../utils/benchmarkViewHelpers'


export default {
  __name: 'Runs',
  components: {
    RefreshRight,
    CaretTop,
    CaretBottom,
    PerformanceCharts
  },
  setup(__props, { expose: __expose }) {
  __expose();

const route = useRoute()
const router = useRouter()
const jobOptions = ref([])
const jobId = ref(route.query.jobId ? String(route.query.jobId) : '')
const statusFilter = ref(route.query.status ? String(route.query.status) : '')
const runs = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const polling = ref(true)
const statusOptions = ['RUNNING', 'COMPLETED', 'FAILED']
let timer

const drawerVisible = ref(false)
const drawerLoading = ref(false)
const ctx = ref(null)
const evalObj = ref(null)

const chartData = computed(() => buildRunsChartData(evalObj.value))

const failureBreakdown = computed(() => {
  return ctx.value?.failureBreakdown || evalObj.value?.diagnostics?.failureBreakdown || null
})

const failureGroups = computed(() => {
  const groups = failureBreakdown.value?.groups
  return Array.isArray(groups) ? groups : []
})

function parseEval(row) {
  return parseEvaluationJson(row)
}

function fmtJson(raw) {
  return formatJson(raw)
}

async function loadJobs() {
  const { data } = await client.get('/jobs')
  jobOptions.value = data || []
}

function buildRouteQuery() {
  return {
    ...(jobId.value ? { jobId: jobId.value } : {}),
    ...(statusFilter.value ? { status: statusFilter.value } : {})
  }
}

function onFiltersChange() {
  page.value = 1
  router.replace({ path: '/runs', query: buildRouteQuery() })
  load()
}

async function load() {
  loading.value = true
  try {
    const { data } = await client.get(API_ENDPOINTS.RUNS, {
      params: {
        ...(jobId.value ? { jobId: jobId.value } : {}),
        ...(statusFilter.value ? { status: statusFilter.value } : {}),
        page: page.value - 1,
        size: size.value
      }
    })
    runs.value = data.content || []
    total.value = data.totalElements || 0
  } finally {
    loading.value = false
  }
}

async function openReport(row) {
  drawerVisible.value = true
  drawerLoading.value = true
  ctx.value = null
  evalObj.value = parseEval(row)
  try {
    const { data } = await client.get(`/runs/${row.id}/context`)
    ctx.value = data
    if (data?.run?.evaluationJson) {
      try {
        evalObj.value = JSON.parse(data.run.evaluationJson)
      } catch {
        /* keep */
      }
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '加载报告失败')
    drawerVisible.value = false
  } finally {
    drawerLoading.value = false
  }
}

function copyEval() {
  const t = ctx.value?.run?.evaluationJson
  if (!t) {
    ElMessage.warning('无 evaluationJson')
    return
  }
  navigator.clipboard.writeText(t).then(
    () => ElMessage.success('已复制'),
    () => ElMessage.error('复制失败')
  )
}

function downloadEval() {
  const t = ctx.value?.run?.evaluationJson
  if (!t) return
  downloadBlob(`benchmark-run-${ctx.value.run.id}-evaluation.json`, t)
}

function downloadFull() {
  if (!ctx.value) return
  downloadBlob(`benchmark-run-${ctx.value.run.id}-context.json`, JSON.stringify(ctx.value, null, 2))
}

function downloadBlob(name, text) {
  const blob = new Blob([text], { type: 'application/json;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = name
  a.click()
  URL.revokeObjectURL(a.href)
}

watch(
  () => [route.query.jobId, route.query.status],
  ([nextJobId, nextStatus]) => {
    const normalizedJobId = nextJobId ? String(nextJobId) : ''
    const normalizedStatus = nextStatus ? String(nextStatus) : ''
    if (normalizedJobId === jobId.value && normalizedStatus === statusFilter.value) {
      return
    }
    page.value = 1
    jobId.value = normalizedJobId
    statusFilter.value = normalizedStatus
    load()
  }
)

onMounted(async () => {
  await loadJobs()
  if (route.query.jobId) {
    jobId.value = String(route.query.jobId)
  }
  if (route.query.status) {
    statusFilter.value = String(route.query.status)
  }
  await load()
  timer = setInterval(load, 3000)
})

onUnmounted(() => clearInterval(timer))

const __returned__ = { route, router, jobOptions, jobId, statusFilter, runs, total, page, size, loading, polling, statusOptions, get timer() { return timer }, set timer(v) { timer = v }, drawerVisible, drawerLoading, ctx, evalObj, chartData, failureBreakdown, failureGroups, parseEval, fmtJson, loadJobs, buildRouteQuery, onFiltersChange, load, openReport, copyEval, downloadEval, downloadFull, downloadBlob, computed, onMounted, onUnmounted, ref, watch, get useRoute() { return useRoute }, get useRouter() { return useRouter }, get ElMessage() { return ElMessage }, get RefreshRight() { return RefreshRight }, get CaretTop() { return CaretTop }, get CaretBottom() { return CaretBottom }, get client() { return client }, get API_ENDPOINTS() { return API_ENDPOINTS }, PerformanceCharts, get buildRunsChartData() { return buildRunsChartData }, get formatDeltas() { return formatDeltas }, get formatJson() { return formatJson }, get parseEvaluationJson() { return parseEvaluationJson }, get verdictTag() { return verdictTag } }
return __returned__
}

}
