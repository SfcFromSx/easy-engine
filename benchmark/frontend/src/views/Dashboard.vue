<template>
  <div class="dashboard-container">
    <div class="page-header">
      <h1>Smart Benchmark 控制台</h1>
      <p class="subtitle header-contrast">执行压测任务并分析结果，确保 JDBC 驱动在真实负载下的稳定性与性能。</p>
    </div>

    <el-row :gutter="20" class="top-row">
      <el-col :span="10">
        <el-card class="preflight-card glass-card h-100" shadow="never">
          <template #header>
            <div class="card-header">
              <span style="font-weight: 600">环境连通性 (Preflight)</span>
              <el-button :loading="pfLoading" size="small" @click="runPreflight">
                <el-icon><RefreshRight /></el-icon>
              </el-button>
            </div>
          </template>

          <div v-if="pfLoading" class="pf-loading">
            <el-skeleton :rows="2" animated />
          </div>
          <el-descriptions v-else-if="pfResult" :column="1" border size="small">
            <el-descriptions-item label="Postgres">
              <div class="status-cell">
                <span class="dot dot-online"></span>
                <span class="status-text">Connected</span>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="Kylin OLAP">
              <div class="status-cell">
                <span :class="['dot', pfResult.kylinRest?.status === 'OK' ? 'dot-online' : 'dot-offline']"></span>
                <span class="status-text">{{ pfResult.kylinRest?.status === 'OK' ? 'Healthy' : 'Error' }}</span>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="Presto Engine">
              <div class="status-cell">
                <span :class="['dot', pfResult.prestoUi?.status === 'OK' ? 'dot-online' : 'dot-offline']"></span>
                <span class="status-text">{{ pfResult.prestoUi?.status === 'OK' ? 'Healthy' : 'Error' }}</span>
              </div>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card v-if="activeRun" class="active-run-card glass-card h-100" shadow="always">
          <template #header>
            <div class="card-header">
              <span class="active-title">
                <el-icon class="is-loading"><Loading /></el-icon>
                {{ $t('dashboard.activeRun.title') }}
              </span>
              <el-tag type="success" size="small" effect="dark" round>RUNNING</el-tag>
            </div>
          </template>
          <div class="progress-body">
            <div class="progress-stats">
              <div class="stat-item">
                <span class="stat-label">Progress</span>
                <span class="stat-value small">{{ activeRun.currentProgress || 0 }} / {{ activeRun.totalTarget || '—' }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Success</span>
                <span class="stat-value small success">{{ calcSuccessRate(activeRun) }}%</span>
              </div>
            </div>
            <el-progress 
              :percentage="calcPercent(activeRun)" 
              :stroke-width="8" 
              striped 
              striped-flow 
              :duration="10"
              color="#3b82f6"
            />
          </div>
        </el-card>

        <el-card v-else class="start-card glass-card h-100" shadow="hover">
          <template #header>
            <div style="font-weight: 600">{{ $t('dashboard.start.title') }}</div>
          </template>
          <el-form label-width="80px" class="start-form">
            <el-form-item :label="$t('dashboard.start.job')" style="margin-bottom: 12px">
              <el-select
                v-model="selectedJobId"
                :placeholder="$t('dashboard.start.placeholder')"
                style="width: 100%"
                size="default"
                filterable
                :loading="loading"
              >
                <el-option
                  v-for="j in jobs"
                  :key="j.id"
                  :label="`${j.name} (#${j.id})`"
                  :value="j.id"
                />
              </el-select>
            </el-form-item>
            <div style="display: flex; gap: 12px; justify-content: flex-end;">
              <el-button
                plain
                :disabled="!selectedJobId"
                @click="goRuns"
              >
                {{ $t('dashboard.start.analyze') }}
              </el-button>
              <el-button
                type="primary"
                :loading="starting"
                :disabled="!selectedJobId || loading"
                @click="confirmAndStart"
              >
                {{ $t('dashboard.start.action') }}
              </el-button>
            </div>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <div class="engine-insight-row">
      <el-row :gutter="24" class="stat-row">
        <el-col :span="6">
          <div class="glass-card stat-card">
            <div class="stat-label">{{ $t('dashboard.stats.totalJobs') }}</div>
            <div class="stat-value">{{ stats.totalJobs }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="glass-card stat-card">
            <div class="stat-label">{{ $t('dashboard.stats.totalRuns') }}</div>
            <div class="stat-value">{{ stats.totalRuns }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="glass-card stat-card">
            <div class="stat-label">{{ $t('dashboard.stats.successRate') }}</div>
            <div class="stat-value percentage">{{ stats.successRate }}%</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="glass-card stat-card">
            <div class="stat-label">{{ $t('dashboard.stats.avgLatency') }}</div>
            <div class="stat-value latency">{{ stats.avgLatency }}ms</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="analytics-section">
      <div v-if="!selectedJobId" class="empty-analytics glass-card">
        <el-empty :image-size="60" description="请选择压测任务以查看历史性能趋势" />
      </div>
      <div v-else class="charts-grid" v-loading="historyLoading">
        <PerformanceCharts
          title="QPS 趋势"
          type="line"
          :data="runHistory"
          xKey="id"
          :yKeys="[{ name: 'QPS', key: 'qps', color: '#3b82f6' }]"
        />
        
        <PerformanceCharts
          title="延迟趋势 (P95/P99)"
          type="line"
          :data="runHistory"
          xKey="id"
          :yKeys="[
            { name: 'P95', key: 'p95Ms', color: '#f59e0b' },
            { name: 'P99', key: 'p99Ms', color: '#ef4444' }
          ]"
        />
      </div>
    </div>

    <div class="links">
      <el-button link type="primary" @click="$router.push('/jobs')">任务配置</el-button>
      <el-divider direction="vertical" />
      <el-button link type="primary" @click="$router.push('/test-sets')">测试集</el-button>
      <el-divider direction="vertical" />
      <el-button link type="primary" @click="$router.push('/templates')">SQL 模板</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Loading } from '@element-plus/icons-vue'
import client from '../api/client'
import PerformanceCharts from '../components/PerformanceCharts.vue'

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

const stats = computed(() => {
  const completedRuns = runHistory.value.filter((run) => run.status === 'COMPLETED')
  const totalQueries = completedRuns.reduce((sum, run) => sum + (run.totalQueries || 0), 0)
  const successQueries = completedRuns.reduce((sum, run) => sum + (run.successCount || 0), 0)
  const latencySamples = completedRuns
    .map((run) => run.p50Ms ?? run.durationMs)
    .filter((value) => typeof value === 'number' && !Number.isNaN(value))

  return {
    totalJobs: jobs.value.length,
    totalRuns: runHistoryTotal.value,
    successRate: totalQueries > 0
      ? Math.round((successQueries / totalQueries) * 1000) / 10
      : 100,
    avgLatency: latencySamples.length > 0
      ? Math.round(latencySamples.reduce((sum, value) => sum + value, 0) / latencySamples.length)
      : 0
  }
})

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

function calcPercent(run) {
  if (!run || !run.totalTarget) return 0
  return Math.min(100, Math.floor(((run.currentProgress || 0) / run.totalTarget) * 100))
}

function calcSuccessRate(run) {
  if (!run) return 100
  const processed = (run.successCount || 0) + (run.errorCount || 0)
  if (!processed) return 100
  return Math.round(((run.successCount || 0) / processed) * 1000) / 10
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
</script>

<style scoped>
.dashboard-container {
  padding-bottom: 20px;
}

.page-header {
  margin-bottom: 16px;
}
.page-header h1 {
  font-size: 20px;
  margin-bottom: 4px;
}
.page-header p {
  font-size: 13px;
}

.top-row {
  margin-bottom: 16px;
}

.h-100 {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.active-run-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(240, 246, 255, 0.9) 100%) !important;
  border-left: 5px solid #3b82f6 !important;
}

.active-title {
  font-weight: 700;
  color: #1e3a8a;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.progress-body {
  padding: 0;
}

.progress-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 10px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  font-family: 'Outfit', sans-serif;
}
.stat-value.small {
  font-size: 18px;
}

.stat-value.success {
  color: #10b981;
}

.header-contrast {
  color: #475569 !important;
  font-weight: 500;
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.analytics-section {
  margin-top: 16px;
}

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.preflight-card, .start-card {
  margin-bottom: 0px;
}

.start-form {
  padding-top: 0px;
}

.links {
  margin-top: 16px;
  padding-bottom: 10px;
  text-align: center;
}

.engine-insight-row {
  margin-bottom: 16px;
}

.stat-card {
  padding: 12px 16px;
}

.stat-card .stat-value {
  font-size: 20px;
}

code {
  font-size: 11px;
  background: #f4f4f5;
  padding: 1px 4px;
  border-radius: 4px;
}

:deep(.el-card__header) {
  padding: 10px 16px;
}
:deep(.el-card__body) {
  padding: 12px 16px;
}
:deep(.el-divider--horizontal) {
  margin: 12px 0;
}
</style>
