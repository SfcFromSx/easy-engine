<template>
  <section class="page-shell">
    <div class="page-header">
      <div class="title-group">
        <h1>{{ t('traces.title') }}</h1>
        <p class="subtitle">{{ t('traces.subtitle') }}</p>
      </div>
      <div class="page-header-actions">
        <el-button @click="load" :loading="loading">
          <el-icon><refresh-cw :size="16" /></el-icon>
        </el-button>
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

    <div class="glass-card table-card">
      <div class="filter-bar">
        <div class="filter-controls">
          <el-input
            v-model="fingerprintDraft"
            class="filter-input"
            clearable
            :placeholder="t('traces.filterPlaceholder')"
            @clear="applyFilter"
            @keyup.enter="applyFilter"
          >
            <template #prefix>
              <terminal :size="14" />
            </template>
          </el-input>
          <el-button type="primary" plain @click="applyFilter">
            {{ t('common.search') }}
          </el-button>
        </div>
        <div v-if="fingerprint" class="filter-actions">
          <div class="filter-summary">
            <terminal :size="14" />
            <span>{{ t('traces.onlyFingerprint') }}</span>
            <code class="mini-code">{{ fingerprint }}</code>
          </div>
          <el-button text @click="clearFilter">{{ t('common.clearFilter') }}</el-button>
        </div>
      </div>

      <div class="table-content" v-loading="loading">
        <div class="table-shell desktop-table">
          <el-table class="data-table traces-table" :data="rows" stripe row-key="id" :empty-text="t('traces.empty')">
            <el-table-column :label="t('traces.time')" width="180">
              <template #default="{ row }">
                <span class="text-small">{{ formatDateTime(row.receivedAt) }}</span>
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
            <el-table-column :label="t('traces.duration')" width="100">
              <template #default="{ row }">
                <span :class="['status-text', 'text-mono', (row.durationMs || 0) > 500 ? 'text-amber' : 'text-blue']">
                  {{ formatDuration(row.durationMs) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column :label="t('traces.cache')" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.cacheHit" type="success" size="small" effect="plain">HIT</el-tag>
                <el-tag v-else type="info" size="small" effect="plain">MISS</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('traces.parseResult')" width="110">
              <template #default="{ row }">
                <el-tag :type="statusType(row.parseStatus)" size="small">
                  {{ row.parseStatus || t('common.noData') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('traces.fingerprint')" width="160">
              <template #default="{ row }">
                <el-link v-if="row.sqlFingerprint" type="primary" class="text-mono" style="font-size: 11px" @click="goPattern(row.sqlFingerprint)">
                  {{ shortFingerprint(row.sqlFingerprint) }}
                </el-link>
                <span v-else class="text-small">{{ t('traces.noFingerprint') }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('traces.sql')" min-width="320" show-overflow-tooltip>
              <template #default="{ row }">
                <code class="mini-code text-mono">{{ row.originalSql || '--' }}</code>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="!loading && rows.length" class="mobile-records">
          <article v-for="row in rows" :key="row.id" class="glass-card mobile-record">
            <div class="mobile-record-header">
              <div>
                <div class="mobile-record-title">{{ row.datasourceName || t('common.unnamedDatasource') }}</div>
                <div class="mobile-record-subtitle">{{ formatDateTime(row.receivedAt) }}</div>
              </div>
              <span :class="['status-text', (row.durationMs || 0) > 500 ? 'text-amber' : 'text-blue']">
                {{ formatDuration(row.durationMs) }}
              </span>
            </div>

            <div class="mobile-record-tags">
              <el-tag size="small" effect="plain" :type="sourceFlagType(row.sourceFlag)">
                {{ row.sourceFlag || '--' }}
              </el-tag>
              <el-tag size="small" :type="row.datasourceType?.toUpperCase() === 'KYLIN' ? 'primary' : 'warning'">
                {{ row.datasourceType || t('common.noData') }}
              </el-tag>
              <el-tag v-if="row.cacheHit" type="success" size="small" effect="plain">HIT</el-tag>
              <el-tag v-else type="info" size="small" effect="plain">MISS</el-tag>
              <el-tag :type="statusType(row.parseStatus)" size="small">
                {{ row.parseStatus || t('common.noData') }}
              </el-tag>
            </div>

            <div class="mobile-record-field">
              <span class="mobile-record-label">{{ t('traces.fingerprint') }}</span>
              <div class="mobile-record-value">
                <el-link v-if="row.sqlFingerprint" type="primary" style="font-size: 12px" @click="goPattern(row.sqlFingerprint)">
                  <terminal :size="12" style="margin-right: 4px" />
                  {{ shortFingerprint(row.sqlFingerprint) }}
                </el-link>
                <span v-else class="muted-text">{{ t('traces.noFingerprint') }}</span>
              </div>
            </div>

            <div class="mobile-record-code">
              <span class="mobile-record-label">{{ t('traces.sql') }}</span>
              <code class="mini-code">{{ row.originalSql || '--' }}</code>
            </div>

            <div v-if="row.parseError" class="mini-muted">{{ t('traces.parseError', { message: row.parseError }) }}</div>
          </article>
        </div>

        <el-empty v-else-if="!loading" class="mobile-only-empty" :description="t('traces.empty')" />
      </div>

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
const size = ref(10)
const loading = ref(false)
const error = ref('')
const fingerprintDraft = ref('')

const fingerprint = computed(() => {
  const value = route.query.fingerprint
  return typeof value === 'string' && value.trim() ? value.trim() : ''
})

function goPattern(sqlFingerprint) {
  router.push({ path: '/patterns', query: { fingerprint: sqlFingerprint } })
}

function updateFingerprintFilter(nextFingerprint) {
  const normalized = nextFingerprint.trim()
  page.value = 1
  if (normalized === fingerprint.value) {
    load()
    return
  }
  router.push({ path: '/traces', query: normalized ? { fingerprint: normalized } : {} })
}

function applyFilter() {
  updateFingerprintFilter(fingerprintDraft.value)
}

function clearFilter() {
  fingerprintDraft.value = ''
  updateFingerprintFilter('')
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
    fingerprintDraft.value = fingerprint.value
    page.value = 1
    load()
  },
  { immediate: true }
)
</script>
