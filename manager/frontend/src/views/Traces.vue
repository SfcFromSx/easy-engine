<template>
  <section class="page-shell">
    <div class="page-header">
      <div class="title-group">
        <h1>{{ t('traces.title') }}</h1>
        <p class="subtitle">{{ t('traces.subtitle') }}</p>
      </div>
      <el-button @click="load" :loading="loading">
        <el-icon><refresh-cw :size="16" /></el-icon>
      </el-button>
    </div>

    <el-alert
      v-if="error"
      class="status-banner"
      type="error"
      show-icon
      :closable="false"
      :title="error"
    />

    <div class="glass-card table-card">
      <div v-if="fingerprint" class="filter-bar">
        <div class="filter-summary">
          <terminal :size="14" />
          <span>{{ t('traces.onlyFingerprint') }}</span>
          <code class="mini-code">{{ fingerprint }}</code>
        </div>
        <el-button text @click="clearFilter">{{ t('common.clearFilter') }}</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe row-key="id" :empty-text="t('traces.empty')">
        <el-table-column :label="t('traces.time')" width="190">
          <template #default="{ row }">
            <span style="color: #64748b; font-size: 13px">{{ formatDateTime(row.receivedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('traces.flag')" width="92" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="sourceFlagType(row.sourceFlag)">
              {{ row.sourceFlag || '--' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('traces.datasource')" width="160">
          <template #default="{ row }">
            <el-tag size="small" :type="row.datasourceType?.toUpperCase() === 'KYLIN' ? 'primary' : 'warning'">
              {{ row.datasourceName || t('common.unnamedDatasource') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('traces.duration')" width="110">
          <template #default="{ row }">
            <span :class="['status-text', (row.durationMs || 0) > 500 ? 'text-amber' : 'text-blue']">
              {{ formatDuration(row.durationMs) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column :label="t('traces.cache')" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.cacheHit" type="success" size="small" effect="plain">HIT</el-tag>
            <el-tag v-else type="info" size="small" effect="plain">MISS</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('traces.parseResult')" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.parseStatus)" size="small">
              {{ row.parseStatus || t('common.noData') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('traces.fingerprint')" width="190">
          <template #default="{ row }">
            <el-link v-if="row.sqlFingerprint" type="primary" style="font-size: 12px" @click="goPattern(row.sqlFingerprint)">
              <terminal :size="12" style="margin-right: 4px" />
              {{ shortFingerprint(row.sqlFingerprint) }}
            </el-link>
            <span v-else class="muted-text">{{ t('traces.noFingerprint') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('traces.sql')" min-width="320" show-overflow-tooltip>
          <template #default="{ row }">
            <code class="mini-code">{{ row.originalSql || '--' }}</code>
            <div v-if="row.parseError" class="mini-muted">{{ t('traces.parseError', { message: row.parseError }) }}</div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="size"
          v-model:current-page="page"
          @current-change="load"
        />
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { RefreshCw, Terminal } from 'lucide-vue-next'
import client from '../api/client'
import { API_ENDPOINTS } from '../api/endpoints'
import { formatDateTime, formatDuration, shortFingerprint } from '../utils/formatters'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const error = ref('')

const fingerprint = computed(() => {
  const value = route.query.fingerprint
  return typeof value === 'string' && value.trim() ? value.trim() : ''
})

function goPattern(sqlFingerprint) {
  router.push({ path: '/patterns', query: { fingerprint: sqlFingerprint } })
}

function clearFilter() {
  router.push({ path: '/traces' })
}

function statusType(status) {
  if (status === 'OK') return 'success'
  if (status === 'ERROR') return 'danger'
  return 'info'
}

function sourceFlagType(flag) {
  if (flag === 'SEED') return 'info'
  if (flag === 'SELF') return 'warning'
  if (flag === 'JDBC') return 'success'
  return 'info'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value - 1, size: size.value }
    if (fingerprint.value) {
      params.fingerprint = fingerprint.value
    }
    const { data } = await client.get(API_ENDPOINTS.TRACES, { params })
    rows.value = data.content || []
    total.value = data.totalElements || 0
  } catch (e) {
    console.error('Failed to load traces', e)
    error.value = e.response?.data?.message || t('traces.loadFailed')
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

watch(
  () => fingerprint.value,
  () => {
    page.value = 1
    load()
  },
  { immediate: true }
)
</script>
