<template>
  <div class="dashboard-container">
    <div class="page-header">
      <h1>Smart Benchmark 控制台</h1>
      <p class="subtitle header-contrast">执行压测任务并分析结果，确保 JDBC 驱动在真实负载下的稳定性与性能。</p>
    </div>

    <el-card class="preflight-card glass-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span style="font-weight: 600">环境连通性预查 (Preflight Control)</span>
          <el-button :loading="pfLoading" size="small" @click="runPreflight">
            <el-icon><RefreshRight /></el-icon>
            重新探测
          </el-button>
        </div>
      </template>

      <div v-if="pfLoading" class="pf-loading">
        <el-skeleton :rows="3" animated />
      </div>
      <el-descriptions v-else-if="pfResult" :column="3" border size="small">
        <el-descriptions-item label="Postgres (Metadata)">
          <div class="status-cell">
            <span class="dot dot-online"></span>
            <span class="status-text">Connected</span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="Kylin OLAP Service">
          <div class="status-cell">
            <span :class="['dot', pfResult.kylinRest?.status === 'OK' ? 'dot-online' : 'dot-offline']"></span>
            <span class="status-text">{{ pfResult.kylinRest?.status === 'OK' ? 'Healthy' : 'Unreachable' }}</span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="Presto Engine">
          <div class="status-cell">
            <span :class="['dot', pfResult.prestoUi?.status === 'OK' ? 'dot-online' : 'dot-offline']"></span>
            <span class="status-text">{{ pfResult.prestoUi?.status === 'OK' ? 'Healthy' : 'Unreachable' }}</span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="探测详情" :span="3">
          <div class="details-cell">
            <code class="mini-code">{{ pfResult.message }}</code>
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="activeRun" class="active-run-card glass-card" shadow="always">
      <template #header>
        <div class="card-header">
          <span class="active-title">
            <el-icon class="is-loading"><Loading /></el-icon>
            {{ $t('dashboard.activeRun.title') }}：{{ getJobName(activeRun.jobId) }}
          </span>
          <el-tag type="success" size="small" effect="dark" round>RUNNING</el-tag>
        </div>
      </template>
      <div class="progress-body">
        <div class="progress-stats">
          <div class="stat-item">
            <span class="stat-label">{{ $t('dashboard.activeRun.completed') }}</span>
            <span class="stat-value">{{ activeRun.currentProgress || 0 }} / {{ activeRun.totalTarget || '—' }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">{{ $t('dashboard.activeRun.successRate') }}</span>
            <span class="stat-value success">{{ calcSuccessRate(activeRun) }}%</span>
          </div>
        </div>
        <el-progress 
          :percentage="calcPercent(activeRun)" 
          :stroke-width="12" 
          striped 
          striped-flow 
          :duration="10"
          color="#3b82f6"
        />
        <p class="progress-hint">{{ $t('dashboard.activeRun.hint') }}</p>
      </div>
    </el-card>

    <el-card class="start-card glass-card" shadow="hover" v-else>
      <template #header>
        <div style="font-weight: 600">{{ $t('dashboard.start.title') }}</div>
      </template>
      <el-form label-width="100px" class="start-form">
        <el-form-item :label="$t('dashboard.start.job')" required>
          <el-select
            v-model="selectedJobId"
            :placeholder="$t('dashboard.start.placeholder')"
            style="width: 100%"
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
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="starting"
            :disabled="!selectedJobId || loading"
            @click="confirmAndStart"
          >
            {{ $t('dashboard.start.action') }}
          </el-button>
          <el-button
            size="large"
            plain
            :disabled="!selectedJobId"
            @click="goRuns"
          >
            {{ $t('dashboard.start.analyze') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

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
      <div class="section-divider">
        <el-divider content-position="left">性能趋势透视 (Performance Insights)</el-divider>
      </div>
      
      <div v-if="!selectedJobId" class="empty-analytics glass-card">
        <el-empty description="请先在上方选择一个「压测任务」以查看性能趋势数据" />
      </div>
      <div v-else class="charts-grid" v-loading="historyLoading">
        <PerformanceCharts
          title="QPS 实时趋势 (Request / Sec)"
          type="line"
          :data="runHistory"
          xKey="id"
          :yKeys="[{ name: 'QPS', key: 'qps', color: '#3b82f6' }]"
        />
        
        <PerformanceCharts
          title="端到端延迟分布 (P50/P95/P99)"
          type="line"
          :data="runHistory"
          xKey="id"
          :yKeys="[
            { name: 'P50', key: 'p50Ms', color: '#10b981' },
            { name: 'P95', key: 'p95Ms', color: '#f59e0b' },
            { name: 'P99', key: 'p99Ms', color: '#ef4444' }
          ]"
        />
      </div>
    </div>

    <div class="links">
      <el-link type="primary" @click="$router.push('/jobs')">任务配置</el-link>
      <el-divider direction="vertical" />
      <el-link type="primary" @click="$router.push('/test-sets')">测试集</el-link>
      <el-divider direction="vertical" />
      <el-link type="primary" @click="$router.push('/templates')">SQL 模板</el-link>
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
  padding-bottom: 60px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.active-run-card {
  margin-bottom: 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(240, 246, 255, 0.9) 100%) !important;
  border-left: 5px solid #3b82f6 !important;
}

.active-title {
  font-weight: 700;
  color: #1e3a8a;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
}

.progress-body {
  padding: 8px 0;
}

.progress-stats {
  display: flex;
  gap: 40px;
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  font-family: 'Outfit', sans-serif;
}

.stat-value.success {
  color: #10b981;
}

.header-contrast {
  color: #475569 !important; /* Higher contrast for light theme header */
  font-weight: 500;
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 120px;
}

.details-cell {
  padding: 4px 0;
  max-height: 60px;
  overflow-y: auto;
}

.progress-hint {
  font-size: 13px;
  color: #64748b;
  margin-top: 16px;
  background: rgba(59, 130, 246, 0.05);
  padding: 10px 16px;
  border-radius: 8px;
}

.analytics-section {
  margin-top: 32px;
}
.section-divider {
  margin-bottom: 24px;
}
.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 20px;
}
.hint {
  color: #606266;
  font-size: 14px;
  margin: 8px 0 20px;
  line-height: 1.6;
}
.subhint {
  color: #909399;
  font-size: 13px;
  margin: 0 0 12px;
}
.preflight-card {
  max-width: 100%;
  margin-bottom: 24px;
}
.start-card {
  max-width: 100%;
  margin-bottom: 24px;
}
.start-form {
  padding-top: 8px;
}
.links {
  margin-top: 32px;
  padding-bottom: 40px;
}
.engine-insight-row {
  margin-bottom: 24px;
}

.insight-card {
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  transition: transform 0.3s ease;
}

.insight-card:hover {
  transform: translateY(-4px);
}

.insight-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.insight-label {
  font-size: 11px;
  color: #64748b;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.insight-icon {
  color: #94a3b8;
  font-size: 16px;
}

.insight-value {
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  font-family: 'Outfit', sans-serif;
  margin-bottom: 4px;
}

.insight-trend {
  font-size: 11px;
  color: #94a3b8;
}

code {
  font-size: 12px;
  background: #f4f4f5;
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
