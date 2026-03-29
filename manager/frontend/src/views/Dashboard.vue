<template>
  <section class="page-shell">
    <div class="page-header">
      <div class="title-group">
        <h1>{{ t('dashboard.title') }}</h1>
        <p class="subtitle">{{ t('dashboard.subtitle') }}</p>
      </div>
      <div class="system-status">
        <el-tag :type="healthTagType" effect="plain" class="status-badge">
          <refresh-cw :size="14" style="margin-right: 4px" :class="{ spin: loading }" />
          {{ healthText }}
        </el-tag>
      </div>
    </div>

    <el-alert
      v-if="error"
      class="status-banner"
      type="error"
      show-icon
      :closable="false"
      :title="error"
    />

    <el-row :gutter="20" class="mb-24">
      <el-col :xs="12" :sm="12" :lg="6">
        <div class="glass-card stat-card">
          <div class="stat-icon bg-blue">
            <activity :size="24" />
          </div>
          <div class="stat-info">
            <div class="stat-label">{{ t('dashboard.totalTraces') }}</div>
            <div class="stat-value">{{ stats.totalTraces }}</div>
            <div class="stats-grid-note">{{ t('dashboard.lastTraceAt', { time: formatDateTime(stats.lastTraceAt) }) }}</div>
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
            <div class="stat-value">{{ stats.patternCount }}</div>
            <div class="stats-grid-note">{{ t('dashboard.patternNote') }}</div>
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
            <div class="stat-value">{{ stats.activeAccelerationCount }}</div>
            <div class="stats-grid-note">{{ t('dashboard.draftAcceleration', { count: stats.draftAccelerationCount }) }}</div>
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
            <div class="stats-grid-note">{{ t('dashboard.cacheHitRate', { value: cacheHitRate }) }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="14">
        <div class="glass-card panel-card">
          <div class="card-header">
            <h3>{{ t('dashboard.recentTraces') }}</h3>
            <el-button text @click="load">{{ t('common.refresh') }}</el-button>
          </div>
          <el-empty v-if="!recentTraces.length && !loading" :description="t('dashboard.noRecentTraces')" />
          <div v-else class="panel-list">
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
      </el-col>
      <el-col :xs="24" :lg="10">
        <div class="glass-card panel-card">
          <div class="card-header">
            <h3>{{ t('dashboard.hotPatterns') }}</h3>
            <el-tag size="small" type="info">{{ t('dashboard.top5') }}</el-tag>
          </div>
          <el-empty v-if="!topPatterns.length && !loading" :description="t('dashboard.noPatterns')" />
          <div v-else class="panel-list">
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

const stats = reactive({
  totalTraces: 0,
  patternCount: 0,
  parseOk: 0,
  parseError: 0,
  activeAccelerationCount: 0,
  draftAccelerationCount: 0,
  cacheHitCount: 0,
  lastTraceAt: null
})

const topPatterns = ref([])
const recentTraces = ref([])
const loading = ref(false)
const error = ref('')

const parseSuccessRate = computed(() => formatPercent(stats.parseOk, stats.totalTraces))
const cacheHitRate = computed(() => formatPercent(stats.cacheHitCount, stats.totalTraces))

const healthText = computed(() => {
  if (loading.value) return t('dashboard.refreshing')
  if (error.value) return t('dashboard.loadFailed')
  if (!stats.totalTraces) return t('dashboard.waiting')
  if (stats.parseError > 0) return t('dashboard.warning')
  return t('dashboard.healthy')
})

const healthTagType = computed(() => {
  if (loading.value) return 'info'
  if (error.value) return 'danger'
  if (!stats.totalTraces) return 'info'
  if (stats.parseError > 0) return 'warning'
  return 'success'
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [summaryRes, patternsRes, tracesRes] = await Promise.all([
      client.get(API_ENDPOINTS.STATS_SUMMARY),
      client.get(API_ENDPOINTS.PATTERNS_TOP, { params: { page: 0, size: 5 } }),
      client.get(API_ENDPOINTS.TRACES, { params: { page: 0, size: 5 } })
    ])

    Object.assign(stats, summaryRes.data)
    topPatterns.value = patternsRes.data?.content || []
    recentTraces.value = tracesRes.data?.content || []
  } catch (e) {
    console.error('Failed to load dashboard data', e)
    error.value = e.response?.data?.message || t('dashboard.loadFailed')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.mb-24 { margin-bottom: 24px; }
.stat-card {
  display: flex;
  align-items: center;
  padding: 24px;
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  color: white;
}
.bg-blue { background: linear-gradient(135deg, #60a5fa, #3b82f6); }
.bg-purple { background: linear-gradient(135deg, #a78bfa, #8b5cf6); }
.bg-emerald { background: linear-gradient(135deg, #34d399, #10b981); }
.bg-amber { background: linear-gradient(135deg, #fbbf24, #f59e0b); }

.stat-label { font-size: 13px; color: #64748b; margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: 700; color: #1e293b; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.card-header h3 { font-size: 16px; font-weight: 600; color: #1e293b; }

.spin { animation: spin 2s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>
