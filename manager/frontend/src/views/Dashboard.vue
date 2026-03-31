<template>
  <section class="page-shell">
    <div class="page-header">
      <div class="title-group">
        <h1>{{ t('dashboard.title') }}</h1>
        <p class="subtitle">{{ t('dashboard.subtitle') }}</p>
      </div>
      <div class="page-header-actions system-status">
        <el-tag :type="healthTagType" effect="plain" class="status-badge">
          <refresh-cw :size="14" style="margin-right: 4px" :class="{ spin: refreshing }" />
          {{ healthText }}
        </el-tag>
      </div>
    </div>

    <el-alert
      v-if="summaryState.error"
      class="status-banner"
      type="error"
      show-icon
      :closable="false"
      :title="summaryState.error"
    />

    <el-row :gutter="20" class="mb-24">
      <el-col :xs="12" :sm="12" :lg="6">
        <div class="glass-card stat-card">
          <div class="stat-icon bg-blue">
            <activity :size="24" />
          </div>
          <div class="stat-info">
            <div class="stat-label">{{ t('dashboard.totalTraces') }}</div>
            <div class="stat-value">{{ totalTracesValue }}</div>
            <div class="stats-grid-note">{{ lastTraceNote }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :lg="6">
        <div class="glass-card stat-card">
          <div class="stat-icon bg-purple">
            <database :size="24" />
          </div>
          <div class="stat-info">
            <div class="stat-label">{{ t('dashboard.patternCount') }}</div>
            <div class="stat-value">{{ patternCountValue }}</div>
            <div class="stats-grid-note">{{ patternNote }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :lg="6">
        <div class="glass-card stat-card">
          <div class="stat-icon bg-emerald">
            <rocket :size="24" />
          </div>
          <div class="stat-info">
            <div class="stat-label">{{ t('dashboard.activeAcceleration') }}</div>
            <div class="stat-value">{{ activeAccelerationValue }}</div>
            <div class="stats-grid-note">{{ accelerationNote }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :lg="6">
        <div class="glass-card stat-card">
          <div class="stat-icon bg-amber">
            <target :size="24" />
          </div>
          <div class="stat-info">
            <div class="stat-label">{{ t('dashboard.parseSuccessRate') }}</div>
            <div class="stat-value">{{ parseSuccessRate }}</div>
            <div class="stats-grid-note">{{ rateNote }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="14">
        <div
          v-loading="tracesState.loading"
          class="glass-card panel-card"
          :element-loading-text="t('dashboard.refreshing')"
        >
          <div class="card-header">
            <h3>{{ t('dashboard.recentTraces') }}</h3>
            <el-button text :loading="refreshing" @click="load">{{ t('common.refresh') }}</el-button>
          </div>
          <div class="dashboard-panel-body">
            <el-alert
              v-if="tracesState.error"
              class="panel-status"
              type="error"
              show-icon
              :closable="false"
              :title="tracesState.error"
            />
            <el-empty v-if="showRecentTracesEmpty" :description="t('dashboard.noRecentTraces')" />
            <div v-else-if="recentTraces.length" class="panel-list">
              <div v-for="trace in recentTraces" :key="trace.id" class="panel-item">
                <div>
                  <div class="panel-item-title">{{ trace.datasourceName || t('common.unnamedDatasource') }}</div>
                  <div class="panel-item-meta">{{ shortFingerprint(trace.sqlFingerprint) }}</div>
                  <div class="panel-item-meta">{{ trace.originalSql || t('common.noSqlText') }}</div>
                </div>
                <div style="text-align: right">
                  <div class="panel-item-title">{{ formatDuration(trace.durationMs) }}</div>
                  <div class="panel-item-meta">{{ formatDateTime(trace.receivedAt) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="10">
        <div
          v-loading="patternsState.loading"
          class="glass-card panel-card"
          :element-loading-text="t('dashboard.refreshing')"
        >
          <div class="card-header">
            <h3>{{ t('dashboard.hotPatterns') }}</h3>
            <el-tag size="small" type="info">{{ t('dashboard.top5') }}</el-tag>
          </div>
          <div class="dashboard-panel-body">
            <el-alert
              v-if="patternsState.error"
              class="panel-status"
              type="error"
              show-icon
              :closable="false"
              :title="patternsState.error"
            />
            <el-empty v-if="showTopPatternsEmpty" :description="t('dashboard.noPatterns')" />
            <div v-else-if="topPatterns.length" class="panel-list">
              <div v-for="pattern in topPatterns" :key="pattern.id" class="panel-item">
                <div>
                  <div class="panel-item-title">{{ shortFingerprint(pattern.sqlFingerprint) }}</div>
                  <div class="panel-item-meta">{{ pattern.cleanSqlSample || t('common.noData') }}</div>
                </div>
                <div style="text-align: right">
                  <div class="panel-item-title">{{ pattern.executionCount }}</div>
                  <div class="panel-item-meta">{{ formatDuration(Math.round(pattern.avgDurationMs || 0)) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Activity, Database, Rocket, Target, RefreshCw } from 'lucide-vue-next'
import client from '../api/client'
import { API_ENDPOINTS } from '../api/endpoints'
import { formatDateTime, formatDuration, formatPercent, shortFingerprint } from '../utils/formatters'

const { t } = useI18n()
const EMPTY_STATS = {
  totalTraces: null,
  patternCount: null,
  parseOk: null,
  parseError: null,
  activeAccelerationCount: null,
  draftAccelerationCount: null,
  cacheHitCount: null,
  lastTraceAt: null
}

const stats = reactive({ ...EMPTY_STATS })
const summaryState = reactive({ loading: false, loaded: false, error: '' })
const tracesState = reactive({ loading: false, loaded: false, error: '' })
const patternsState = reactive({ loading: false, loaded: false, error: '' })

const topPatterns = ref([])
const recentTraces = ref([])
const refreshing = computed(() => (
  summaryState.loading || tracesState.loading || patternsState.loading
))

const showInitialState = computed(() => !summaryState.loaded)
const showRecentTracesEmpty = computed(() => (
  tracesState.loaded && !tracesState.loading && !tracesState.error && !recentTraces.value.length
))
const showTopPatternsEmpty = computed(() => (
  patternsState.loaded && !patternsState.loading && !patternsState.error && !topPatterns.value.length
))

const totalTracesValue = computed(() => metricValue(stats.totalTraces))
const patternCountValue = computed(() => metricValue(stats.patternCount))
const activeAccelerationValue = computed(() => metricValue(stats.activeAccelerationCount))
const parseSuccessRate = computed(() => metricPercent(stats.parseOk, stats.totalTraces))
const cacheHitRate = computed(() => metricPercent(stats.cacheHitCount, stats.totalTraces))
const lastTraceNote = computed(() => {
  if (showInitialState.value) {
    return t(summaryState.error ? 'dashboard.summaryUnavailable' : 'dashboard.loadingMetrics')
  }
  if (!stats.lastTraceAt) {
    return t('dashboard.noTraceYet')
  }
  return t('dashboard.lastTraceAt', { time: formatDateTime(stats.lastTraceAt) })
})
const patternNote = computed(() => (
  showInitialState.value
    ? t(summaryState.error ? 'dashboard.summaryUnavailable' : 'dashboard.loadingMetrics')
    : t('dashboard.patternNote')
))
const accelerationNote = computed(() => (
  showInitialState.value
    ? t(summaryState.error ? 'dashboard.summaryUnavailable' : 'dashboard.loadingMetrics')
    : t('dashboard.draftAcceleration', { count: stats.draftAccelerationCount })
))
const rateNote = computed(() => (
  showInitialState.value
    ? t(summaryState.error ? 'dashboard.summaryUnavailable' : 'dashboard.loadingMetrics')
    : t('dashboard.cacheHitRate', { value: cacheHitRate.value })
))

const healthText = computed(() => {
  if (summaryState.loading) return t('dashboard.refreshing')
  if (summaryState.error) return t('dashboard.loadFailed')
  if (!summaryState.loaded || !stats.totalTraces) return t('dashboard.waiting')
  if (stats.parseError > 0) return t('dashboard.warning')
  return t('dashboard.healthy')
})

const healthTagType = computed(() => {
  if (summaryState.loading) return 'info'
  if (summaryState.error) return 'danger'
  if (!summaryState.loaded || !stats.totalTraces) return 'info'
  if (stats.parseError > 0) return 'warning'
  return 'success'
})

function metricValue(value) {
  return value == null ? '--' : value
}

function metricPercent(part, total) {
  if (!summaryState.loaded || part == null || total == null) {
    return '--'
  }
  return formatPercent(part, total)
}

function applySummary(summary = {}) {
  stats.totalTraces = summary.totalTraces ?? 0
  stats.patternCount = summary.patternCount ?? 0
  stats.parseOk = summary.parseOk ?? 0
  stats.parseError = summary.parseError ?? 0
  stats.activeAccelerationCount = summary.activeAccelerationCount ?? 0
  stats.draftAccelerationCount = summary.draftAccelerationCount ?? 0
  stats.cacheHitCount = summary.cacheHitCount ?? 0
  stats.lastTraceAt = summary.lastTraceAt ?? null
}

async function loadSummary() {
  summaryState.loading = true
  summaryState.error = ''
  try {
    const { data } = await client.get(API_ENDPOINTS.STATS_SUMMARY)
    applySummary(data)
    summaryState.loaded = true
  } catch (e) {
    console.error('Failed to load dashboard summary', e)
    summaryState.error = e.response?.data?.message || t('dashboard.loadFailed')
    if (!summaryState.loaded) {
      Object.assign(stats, EMPTY_STATS)
    }
  } finally {
    summaryState.loading = false
  }
}

async function loadPatterns() {
  patternsState.loading = true
  patternsState.error = ''
  try {
    const { data } = await client.get(API_ENDPOINTS.PATTERNS_TOP, { params: { page: 0, size: 5 } })
    topPatterns.value = data?.content || []
    patternsState.loaded = true
  } catch (e) {
    console.error('Failed to load dashboard patterns', e)
    patternsState.error = e.response?.data?.message || t('patterns.loadFailed')
    if (!patternsState.loaded) {
      topPatterns.value = []
    }
  } finally {
    patternsState.loading = false
  }
}

async function loadTraces() {
  tracesState.loading = true
  tracesState.error = ''
  try {
    const { data } = await client.get(API_ENDPOINTS.TRACES, { params: { page: 0, size: 5 } })
    recentTraces.value = data?.content || []
    tracesState.loaded = true
  } catch (e) {
    console.error('Failed to load dashboard traces', e)
    tracesState.error = e.response?.data?.message || t('traces.loadFailed')
    if (!tracesState.loaded) {
      recentTraces.value = []
    }
  } finally {
    tracesState.loading = false
  }
}

async function load() {
  await Promise.allSettled([
    loadSummary(),
    loadPatterns(),
    loadTraces()
  ])
}

onMounted(load)
</script>

<style scoped>
.mb-24 { margin-bottom: 20px; }
.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
}
.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 14px;
  color: white;
  flex-shrink: 0;
}
.bg-blue { background: linear-gradient(135deg, #60a5fa, #3b82f6); }
.bg-purple { background: linear-gradient(135deg, #a78bfa, #8b5cf6); }
.bg-emerald { background: linear-gradient(135deg, #34d399, #10b981); }
.bg-amber { background: linear-gradient(135deg, #fbbf24, #f59e0b); }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.dashboard-panel-body {
  min-height: 200px;
}

.panel-status {
  margin-bottom: 12px;
}

.spin { animation: spin 2s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>
