import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Loading } from '@element-plus/icons-vue'
import client from '../api/client'
import PerformanceCharts from '../components/PerformanceCharts.vue'
import { buildDashboardStats, calcPercent, calcSuccessRate } from '../utils/benchmarkViewHelpers'


export default {
  __name: 'Dashboard',
  components: {
    RefreshRight,
    Loading,
    PerformanceCharts
  },
  setup(__props, { expose: __expose }) {
  __expose();

const router = useRouter()
const jobs = ref([])
const loading = ref(false)
const starting = ref(false)
const selectedJobId = ref(null)

const pfLoading = ref(false)
const pfResult = ref(null)

const runHistory = ref([])
const runHistoryTotal = ref(0)
const historyLoading = ref(false)
const activeRun = ref(null)
let activeTimer = null

const stats = computed(() => ({
  ...buildDashboardStats(runHistory.value, jobs.value),
  totalRuns: runHistoryTotal.value
}))

async function runPreflight() {
  pfLoading.value = true
  try {
    const { data } = await client.get('/preflight')
    pfResult.value = data
    ElMessage.success('预检完成')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '预检失败')
  } finally {
    pfLoading.value = false
  }
}

async function loadJobs() {
  loading.value = true
  try {
    const { data } = await client.get('/jobs')
    jobs.value = data || []
    if (!selectedJobId.value && jobs.value.length > 0) {
      selectedJobId.value = jobs.value[0].id
    }
  } finally {
    loading.value = false
  }
}

async function loadActiveRun() {
  try {
    const { data } = await client.get('/runs/active')
    activeRun.value = data
  } catch (e) { /* ignore */ }
}

function getJobName(id) {
  const j = jobs.value.find(x => x.id === id)
  return j ? j.name : `#${id}`
}

async function loadRunHistory() {
  if (!selectedJobId.value) {
    runHistory.value = []
    runHistoryTotal.value = 0
    return
  }
  historyLoading.value = true
  try {
    const { data } = await client.get('/runs', { 
      params: { jobId: selectedJobId.value, page: 0, size: 10 } 
    })
    runHistory.value = [...(data.content || [])].reverse()
    runHistoryTotal.value = data.totalElements || runHistory.value.length
  } catch (e) {
    console.error('Failed to load history', e)
  } finally {
    historyLoading.value = false
  }
}

watch(selectedJobId, () => {
  loadRunHistory()
})

function goRuns() {
  router.push({ path: '/runs', query: { jobId: String(selectedJobId.value) } })
}

async function confirmAndStart() {
  if (!selectedJobId.value) return
  try {
    await ElMessageBox.confirm(
      '将立即按任务参数发起一轮压测（异步执行）。结束后可在「运行记录」查看结构化评价 JSON，便于 JDBC 改动前后对比。',
      '确认启动压测',
      { type: 'warning', confirmButtonText: '开始', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  starting.value = true
  try {
    const { data } = await client.post('/runs/start', { jobId: selectedJobId.value })
    ElMessage.success(`压测已提交，Run #${data.id}，请在运行记录中查看进度与评价`)
    router.push({ path: '/runs', query: { jobId: String(selectedJobId.value) } })
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '启动失败')
  } finally {
    starting.value = false
  }
}

onMounted(async () => {
  await Promise.all([
    loadJobs(),
    runPreflight(),
    loadActiveRun()
  ])
  if (selectedJobId.value) {
    await loadRunHistory()
  }
  activeTimer = setInterval(loadActiveRun, 3000)
})

onUnmounted(() => {
  if (activeTimer) clearInterval(activeTimer)
})

const __returned__ = { router, jobs, loading, starting, selectedJobId, pfLoading, pfResult, runHistory, runHistoryTotal, historyLoading, activeRun, get activeTimer() { return activeTimer }, set activeTimer(v) { activeTimer = v }, stats, runPreflight, loadJobs, loadActiveRun, getJobName, loadRunHistory, goRuns, confirmAndStart, computed, onMounted, onUnmounted, ref, watch, get useRouter() { return useRouter }, get ElMessage() { return ElMessage }, get ElMessageBox() { return ElMessageBox }, get RefreshRight() { return RefreshRight }, get Loading() { return Loading }, get client() { return client }, PerformanceCharts, get buildDashboardStats() { return buildDashboardStats }, get calcPercent() { return calcPercent }, get calcSuccessRate() { return calcSuccessRate } }
return __returned__
}

}
