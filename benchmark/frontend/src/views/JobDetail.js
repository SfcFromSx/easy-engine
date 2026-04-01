import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FileJson } from 'lucide-vue-next'
import { Setting, DataLine, Clock, VideoPlay } from '@element-plus/icons-vue'
import client from '../api/client'
import { API_ENDPOINTS, JOB_BY_ID, RUN_START } from '../api/endpoints'
import PerformanceCharts from '../components/PerformanceCharts.vue'
import { buildTrendData, strategyTagType } from '../utils/benchmarkViewHelpers'


export default {
  __name: 'JobDetail',
  components: {
    FileJson,
    Setting,
    DataLine,
    Clock,
    VideoPlay,
    PerformanceCharts
  },
  setup(__props, { expose: __expose }) {
  __expose();

const route = useRoute()
const router = useRouter()
const job = ref(null)
const dataSource = ref(null)
const runs = ref([])
const loading = ref(false)
const starting = ref(false)

const trendData = computed(() => buildTrendData(runs.value))

async function load() {
  const id = route.params.id
  loading.value = true
  try {
    const res = await client.get(JOB_BY_ID(id))
    job.value = res.data
    dataSource.value = null
    if (job.value?.dataSourceId) {
      const dsRes = await client.get(`/datasources/${job.value.dataSourceId}`)
      dataSource.value = dsRes.data
    }
    const runsRes = await client.get(API_ENDPOINTS.RUNS, { params: { jobId: id, size: 10 } })
    runs.value = runsRes.data.content || []
  } catch (e) {
    ElMessage.error('加载任务详情失败')
  } finally {
    loading.value = false
  }
}

async function startJob() {
  try {
    await ElMessageBox.confirm(`确认启动任务「${job.value.name}」？`, '提示')
    starting.value = true
    const { data } = await client.post(RUN_START, { jobId: job.value.id })
    ElMessage.success(`压测已提交: Run #${data.id}`)
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('启动失败')
  } finally {
    starting.value = false
  }
}

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString()
}

function goRun(id) {
  router.push({ path: '/runs', query: { id } })
}

onMounted(load)

const __returned__ = { route, router, job, dataSource, runs, loading, starting, trendData, load, startJob, formatDate, goRun, computed, onMounted, ref, get useRoute() { return useRoute }, get useRouter() { return useRouter }, get ElMessage() { return ElMessage }, get ElMessageBox() { return ElMessageBox }, get FileJson() { return FileJson }, get Setting() { return Setting }, get DataLine() { return DataLine }, get Clock() { return Clock }, get VideoPlay() { return VideoPlay }, get client() { return client }, get API_ENDPOINTS() { return API_ENDPOINTS }, get JOB_BY_ID() { return JOB_BY_ID }, get RUN_START() { return RUN_START }, PerformanceCharts, get buildTrendData() { return buildTrendData }, get strategyTagType() { return strategyTagType } }
return __returned__
}

}
