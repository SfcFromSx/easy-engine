<template>
  <div class="runs-container">
    <div class="page-header">
      <div class="title-group">
        <h1>{{ $t('runs.title') }}</h1>
        <p class="subtitle">{{ $t('runs.subtitle') }}</p>
      </div>
      <div class="toolbar-actions">
        <span class="label">{{ $t('runs.currentJob') }}</span>
        <el-select
          v-model="jobId"
          :placeholder="$t('runs.selectJob')"
          class="toolbar-select"
          clearable
          filterable
          @change="onFiltersChange"
        >
          <el-option
            v-for="j in jobOptions"
            :key="j.id"
            :label="`${j.name}（#${j.id}）`"
            :value="String(j.id)"
          />
        </el-select>
        <el-select
          v-model="statusFilter"
          clearable
          class="toolbar-select"
          :placeholder="$t('runs.filterStatusPlaceholder')"
          @change="onFiltersChange"
        >
          <el-option :label="$t('runs.filterStatusAll')" value="" />
          <el-option
            v-for="status in statusOptions"
            :key="status"
            :label="status"
            :value="status"
          />
        </el-select>
        <el-button type="primary" plain @click="load">
          <el-icon><RefreshRight /></el-icon>
          {{ $t('common.refresh') }}
        </el-button>
        <el-tag v-if="polling" type="success" size="small" effect="light" round>
          <span class="dot dot-online" style="margin-right: 4px; width: 6px; height: 6px"></span>
          {{ $t('runs.autoRefreshing') }}
        </el-tag>
      </div>
    </div>

    <div class="glass-card table-card">
      <el-table :data="runs" v-loading="loading" stripe>
        <el-table-column prop="id" label="Run" width="80" />
        <el-table-column :label="$t('runs.colVerdict')" width="100">
          <template #default="{ row }">
            <el-tag v-if="parseEval(row)" :type="verdictTag(parseEval(row).verdict)" size="small">
              {{ parseEval(row).verdict || '—' }}
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('runs.colStatus')" width="140">
          <template #default="{ row }">
            <span>{{ row.status }}</span>
            <span v-if="row.status === 'RUNNING'" class="progress-mini">
              ({{ row.currentProgress || 0 }}/{{ row.totalTarget || '?' }})
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="totalQueries" :label="$t('runs.colTotal')" width="80" />
        <el-table-column prop="successCount" :label="$t('runs.colSuccess')" width="80" />
        <el-table-column prop="errorCount" :label="$t('runs.colError')" width="80" />
        <el-table-column prop="durationMs" :label="$t('runs.colDuration')" width="100" />
        <el-table-column label="QPS" width="90">
          <template #default="{ row }">
            {{ row.qps?.toFixed(2) || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="P50" width="80">
          <template #default="{ row }">
            {{ row.p50Ms?.toFixed(2) || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="P95" width="80">
          <template #default="{ row }">
            {{ row.p95Ms?.toFixed(2) || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="P99" width="80">
          <template #default="{ row }">
            {{ row.p99Ms?.toFixed(2) || '—' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openReport(row)">{{ $t('runs.viewReport') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 20px; justify-content: flex-end"
        layout="prev, pager, next, total"
        :total="total"
        :page-size="size"
        v-model:current-page="page"
        @current-change="load"
      />
    </div>

    <el-drawer v-model="drawerVisible" title="压测结构化报告" size="560px" destroy-on-close>
      <div v-if="drawerLoading" class="drawer-loading">加载中…</div>
      <template v-else-if="ctx">
        <el-descriptions v-if="evalObj" :column="1" border size="small" class="mb">
          <el-descriptions-item label="结论">{{ evalObj.summary }}</el-descriptions-item>
          <el-descriptions-item label="判定">{{ evalObj.verdict }}</el-descriptions-item>
          <el-descriptions-item v-if="evalObj.jdbcComparisonHints?.tailLatencyNote" label="尾延迟">
            {{ evalObj.jdbcComparisonHints.tailLatencyNote }}
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="failureGroups.length">
          <h4 class="section-title">失败诊断分组</h4>
          <div class="failure-summary">
            <el-tag type="danger" effect="light">
              共 {{ failureBreakdown?.totalFailures || 0 }} 次失败
            </el-tag>
            <el-tag effect="plain">
              {{ failureBreakdown?.groupCount || failureGroups.length }} 个诊断分组
            </el-tag>
          </div>
          <el-table :data="failureGroups" size="small" border stripe class="failure-table">
            <el-table-column prop="sqlLabel" label="SQL Label" min-width="150" />
            <el-table-column prop="executionMode" label="执行模式" width="160" />
            <el-table-column prop="routedTarget" label="路由目标" width="150" />
            <el-table-column prop="failureCount" label="失败次数" width="100" />
            <el-table-column label="样本报错" min-width="220">
              <template #default="{ row }">
                <span class="failure-message">{{ row.sampleMessage || '—' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <p v-else-if="evalObj?.verdict !== 'PASS'" class="muted">
          当前 Run 没有可分组的失败诊断明细。
        </p>

        <template v-if="ctx.comparisonDelta?.available">
          <h4 class="section-title">与同任务上一次 Run 的对比分析</h4>
          <p class="delta-hint">{{ ctx.comparisonDelta.interpretationHint }}</p>
          <el-table :data="formatDeltas(ctx.comparisonDelta.deltas)" size="small" border stripe>
            <el-table-column prop="metric" label="指标" width="120" />
            <el-table-column prop="current" label="本次" width="100" />
            <el-table-column prop="previous" label="上次" width="100" />
            <el-table-column label="增益/偏差">
              <template #default="{ row }">
                <span :class="['delta-value', row.direction]">
                  {{ row.deltaText }}
                  <el-icon v-if="row.direction === 'up'"><CaretTop /></el-icon>
                  <el-icon v-else-if="row.direction === 'down'"><CaretBottom /></el-icon>
                </span>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <p v-else-if="ctx.comparisonDelta && !ctx.comparisonDelta.available" class="muted">
          {{ ctx.comparisonDelta.reason }}
        </p>

        <h4 class="section-title">性能指标可视化</h4>
        <div class="visual-report glass-card">
          <PerformanceCharts
            v-if="chartData.length"
            title="延迟统计 (ms)"
            type="bar"
            :data="chartData"
            xKey="name"
            :yKeys="[{ name: 'Value', key: 'value' }]"
          />
          <div v-else class="no-data-hint">该 Run 无有效的评价指标数据。</div>
        </div>

        <h4 class="section-title">任务快照（jobSnapshotJson）</h4>
        <pre class="json-block">{{ fmtJson(ctx.run?.jobSnapshotJson) }}</pre>

        <h4 class="section-title">完整评价 JSON（evaluationJson）</h4>
        <p class="muted">可整段复制到 PR 描述或存档，便于 JDBC 迭代对比。</p>
        <pre class="json-block">{{ fmtJson(ctx.run?.evaluationJson) }}</pre>

        <div class="drawer-actions">
          <el-button @click="copyEval">复制 evaluationJson</el-button>
          <el-button type="primary" @click="downloadEval">下载 evaluation.json</el-button>
          <el-button @click="downloadFull">下载完整上下文</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
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
</script>

<style scoped>
.runs-container {
  max-width: 100%;
}

.page-header {
  margin-bottom: 16px;
}

.glass-card {
  padding: 0;
}

.table-card {
  padding: 0;
}

:deep(.el-table__row) {
  height: 40px;
}

.section-title {
  font-family: var(--font-family-title);
  font-size: var(--font-size-card-title);
  font-weight: 600;
  margin: 16px 0 8px;
  color: var(--color-title);
}

.json-block {
  background: #0f172a;
  color: #94a3b8;
  padding: 12px;
  border-radius: 8px;
  font-size: var(--font-size-mono);
  line-height: 1.4;
  border: 1px solid rgba(255, 255, 255, 0.1);
  font-family: var(--font-family-mono);
  max-height: 200px;
  overflow-y: auto;
}

.failure-message {
  font-family: var(--font-family-mono);
  font-size: var(--font-size-small);
}
</style>
