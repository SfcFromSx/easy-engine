import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Plus, Connection } from '@element-plus/icons-vue'
import client from '../api/client'
import {
  filterJobs,
  getDataSourceName as resolveDataSourceName,
  getTestSetName as resolveTestSetName,
  strategyTagType
} from '../utils/benchmarkViewHelpers'


export default {
  __name: 'Jobs',
  components: {
    Plus,
    Connection
  },
  setup(__props, { expose: __expose }) {
  __expose();

const router = useRouter()
const jobs = ref([])
const testSets = ref([])
const dataSources = ref([])
const loading = ref(false)
const startingId = ref(null)
const dlg = ref(false)
const keyword = ref('')
const selectedDataSourceId = ref('')
const selectedStrategy = ref('')
const selectedTestSetFilter = ref('')
const form = reactive({
  id: null,
  name: '',
  dataSourceId: null,
  concurrentThreads: 4,
  rounds: 100,
  strategy: 'RANDOM_WEIGHT',
  testSetId: null
})

const strategyOptions = ['RANDOM_WEIGHT', 'ROUND_ROBIN', 'CACHE_PENETRATION']

const filteredJobs = computed(() => filterJobs(
  jobs.value,
  keyword.value,
  selectedDataSourceId.value,
  selectedStrategy.value,
  selectedTestSetFilter.value,
  dataSources.value,
  testSets.value
))

async function load() {
  loading.value = true
  try {
    const [jobsRes, tsRes, dsRes] = await Promise.all([
      client.get('/jobs'), 
      client.get('/test-sets'),
      client.get('/datasources')
    ])
    jobs.value = jobsRes.data
    testSets.value = tsRes.data
    dataSources.value = dsRes.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    id: null,
    name: '',
    dataSourceId: dataSources.value.length > 0 ? dataSources.value[0].id : null,
    concurrentThreads: 4,
    rounds: 100,
    strategy: 'RANDOM_WEIGHT',
    testSetId: null
  })
  dlg.value = true
}

function edit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    dataSourceId: row.dataSourceId,
    concurrentThreads: row.concurrentThreads,
    rounds: row.rounds,
    strategy: row.strategy,
    testSetId: row.testSetId
  })
  dlg.value = true
}

function getTestSetName(id) {
  return resolveTestSetName(testSets.value, id)
}

function getDataSourceName(id) {
  return resolveDataSourceName(dataSources.value, id)
}

async function removeJob(row) {
  try {
    await ElMessageBox.confirm('Confirm delete task? Historical runs will be preserved.', 'Warning', {
      type: 'warning'
    })
    await client.delete('/jobs/' + row.id)
    ElMessage.success('Deleted')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e.response?.data?.message || e.message || 'Delete failed')
    }
  }
}

async function save() {
  if (!form.name || !form.dataSourceId) {
    ElMessage.warning('Required fields missing')
    return
  }
  try {
    const payload = { ...form }
    if (form.id) {
      await client.put('/jobs/' + form.id, payload)
    } else {
      await client.post('/jobs', payload)
    }
    ElMessage.success('Saved')
    dlg.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message)
  }
}

async function start(row) {
  try {
    await ElMessageBox.confirm(
      `Confirm start benchmark for "${row.name}"?`,
      'Ready to Execute',
      { type: 'warning' }
    )
  } catch { return }
  
  startingId.value = row.id
  try {
    const { data } = await client.post('/runs/start', { jobId: row.id })
    ElMessage.success(`Benchmark submitted CID: #${data.id}`)
    router.push({ path: '/runs', query: { jobId: String(row.id) } })
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message)
  } finally {
    startingId.value = null
  }
}

onMounted(load)

const __returned__ = { router, jobs, testSets, dataSources, loading, startingId, dlg, keyword, selectedDataSourceId, selectedStrategy, selectedTestSetFilter, form, strategyOptions, filteredJobs, load, openCreate, edit, getTestSetName, getDataSourceName, removeJob, save, start, computed, onMounted, reactive, ref, get ElMessage() { return ElMessage }, get ElMessageBox() { return ElMessageBox }, get useRouter() { return useRouter }, get Plus() { return Plus }, get Connection() { return Connection }, get client() { return client }, get filterJobs() { return filterJobs }, get resolveDataSourceName() { return resolveDataSourceName }, get resolveTestSetName() { return resolveTestSetName }, get strategyTagType() { return strategyTagType } }
return __returned__
}

}
