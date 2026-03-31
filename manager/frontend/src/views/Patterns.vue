<template>
  <section class="page-shell">
    <div class="page-header">
      <div class="title-group">
        <h1>{{ t('patterns.title') }}</h1>
        <p class="subtitle">{{ t('patterns.subtitle') }}</p>
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
            v-model="filterDraft.fingerprint"
            class="filter-input"
            clearable
            :placeholder="t('patterns.filterPlaceholder')"
            @keyup.enter="applyFilter"
          >
            <template #prefix>
              <terminal :size="14" />
            </template>
          </el-input>
          <el-input
            v-model="filterDraft.sqlKeyword"
            class="filter-input"
            clearable
            :placeholder="t('patterns.filterSqlPlaceholder')"
            @keyup.enter="applyFilter"
          />
          <el-input
            v-model="filterDraft.minExecutionCount"
            class="filter-select"
            clearable
            :placeholder="t('patterns.minExecutionPlaceholder')"
            @keyup.enter="applyFilter"
          />
          <el-button type="primary" plain @click="applyFilter">
            {{ t('common.search') }}
          </el-button>
        </div>
        <div v-if="hasActiveFilters" class="filter-actions">
          <div class="filter-summary">
            <terminal :size="14" />
            <span>{{ t('patterns.filteredBy') }}</span>
            <el-tag v-for="entry in activeFilterEntries" :key="entry.label" size="small" effect="plain">
              {{ entry.label }}: {{ entry.value }}
            </el-tag>
          </div>
          <el-button text @click="clearFilter">{{ t('common.viewAll') }}</el-button>
        </div>
      </div>

      <div class="table-content" v-loading="loading">
        <div class="table-shell desktop-table">
          <el-table
            class="data-table patterns-table"
            :data="patterns"
            stripe
            row-key="id"
            :empty-text="t('patterns.empty')"
            :row-class-name="rowClassName"
          >
            <el-table-column prop="sqlFingerprint" :label="t('patterns.fingerprintId')" width="140">
              <template #default="{ row }">
                <code class="mini-code text-mono" style="font-size: 11px">{{ shortFingerprint(row.sqlFingerprint, 12) }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="executionCount" :label="t('patterns.executionCount')" width="90" align="right">
              <template #default="{ row }">
                <span class="text-mono" style="font-weight: 700; color: var(--primary-color)">{{ row.executionCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="avgDurationMs" :label="t('patterns.avgDuration')" width="100">
              <template #default="{ row }">
                <span :class="['status-text', 'text-mono', row.avgDurationMs > 300 ? 'text-amber' : 'text-blue']">
                  {{ Math.round(row.avgDurationMs || 0) }}ms
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="cleanSqlSample" :label="t('patterns.sampleSql')" min-width="320" show-overflow-tooltip>
              <template #default="{ row }">
                <code class="text-mono text-small">{{ row.cleanSqlSample || t('common.noData') }}</code>
              </template>
            </el-table-column>
            <el-table-column :label="t('patterns.actions')" width="180">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button size="small" type="primary" plain @click="goTraces(row)">{{ t('patterns.viewTraces') }}</el-button>
                  <el-button size="small" type="success" plain @click="openDialog(row)">{{ t('patterns.accel') }}</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="!loading && patterns.length" class="mobile-records">
          <article
            v-for="row in patterns"
            :key="row.id"
            :class="['glass-card', 'mobile-record', { 'mobile-record--selected': row.sqlFingerprint === activeFilters.fingerprint }]"
          >
            <div class="mobile-record-header">
              <div>
                <div class="mobile-record-title">{{ shortFingerprint(row.sqlFingerprint, 16) }}</div>
                <div class="mobile-record-subtitle">{{ row.cleanSqlSample || t('common.noData') }}</div>
              </div>
              <span :class="['status-text', row.avgDurationMs > 300 ? 'text-amber' : 'text-blue']">
                {{ Math.round(row.avgDurationMs || 0) }}ms
              </span>
            </div>

            <div class="mobile-record-grid">
              <div class="mobile-record-field">
                <span class="mobile-record-label">{{ t('patterns.executionCount') }}</span>
                <span class="mobile-record-value">{{ row.executionCount }}</span>
              </div>
              <div class="mobile-record-field">
                <span class="mobile-record-label">{{ t('patterns.fingerprintId') }}</span>
                <code class="mini-code">{{ shortFingerprint(row.sqlFingerprint, 12) }}</code>
              </div>
            </div>

            <div class="mobile-record-actions">
              <el-button size="small" type="primary" plain @click="goTraces(row)">{{ t('patterns.viewTraces') }}</el-button>
              <el-button size="small" type="success" @click="openDialog(row)">{{ t('patterns.createAcceleration') }}</el-button>
            </div>
          </article>
        </div>

        <el-empty v-else-if="!loading" class="mobile-only-empty" :description="t('patterns.empty')" />
      </div>

      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="size"
          :page-sizes="[10, 20, 50]"
          v-model:current-page="page"
          v-model:page-size="size"
          @current-change="load"
          @size-change="load"
        />
      </div>

      <el-dialog v-model="visible" :title="t('patterns.dialogTitle')" width="480px">
        <el-form :model="form" label-width="100px" label-position="top">
          <el-form-item :label="t('patterns.schema')">
            <el-input v-model="form.schemaName" placeholder="public" />
          </el-form-item>
          <el-form-item :label="t('patterns.table')">
            <el-input v-model="form.tableName" placeholder="rollup_xxx" />
          </el-form-item>
          <div class="hint">{{ t('patterns.hint') }}</div>
        </el-form>
        <template #footer>
          <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">{{ t('common.create') }}</el-button>
        </template>
      </el-dialog>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { RefreshCw, Terminal } from 'lucide-vue-next'
import client from '../api/client'
import { ACCEL_FROM_PATTERN, API_ENDPOINTS } from '../api/endpoints'
import { shortFingerprint } from '../utils/formatters'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const patterns = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const visible = ref(false)
const selected = ref(null)
const filterDraft = reactive(emptyFilters())
const form = reactive({ tableName: '', schemaName: 'public' })

const activeFilters = computed(() => ({
  fingerprint: normalizeQuery(route.query.fingerprint),
  sqlKeyword: normalizeQuery(route.query.sqlKeyword),
  minExecutionCount: normalizeQuery(route.query.minExecutionCount)
}))

const hasActiveFilters = computed(() => Object.values(activeFilters.value).some(Boolean))
const activeFilterEntries = computed(() => {
  const entries = []
  if (activeFilters.value.fingerprint) {
    entries.push({ label: t('patterns.fingerprintId'), value: activeFilters.value.fingerprint })
  }
  if (activeFilters.value.sqlKeyword) {
    entries.push({ label: t('patterns.sampleSql'), value: activeFilters.value.sqlKeyword })
  }
  if (activeFilters.value.minExecutionCount) {
    entries.push({ label: t('patterns.minExecutionCount'), value: activeFilters.value.minExecutionCount })
  }
  return entries
})

function goTraces(row) {
  router.push({ path: '/traces', query: { fingerprint: row.sqlFingerprint } })
}

function emptyFilters() {
  return {
    fingerprint: '',
    sqlKeyword: '',
    minExecutionCount: ''
  }
}

function normalizeQuery(value) {
  return typeof value === 'string' && value.trim() ? value.trim() : ''
}

function buildQuery(filters) {
  const query = {}
  if (filters.fingerprint.trim()) query.fingerprint = filters.fingerprint.trim()
  if (filters.sqlKeyword.trim()) query.sqlKeyword = filters.sqlKeyword.trim()
  if (filters.minExecutionCount.trim()) query.minExecutionCount = filters.minExecutionCount.trim()
  return query
}

function isSameQuery(nextQuery) {
  return JSON.stringify(buildQuery(activeFilters.value)) === JSON.stringify(nextQuery)
}

function updateFilters() {
  const nextQuery = buildQuery(filterDraft)
  page.value = 1
  if (isSameQuery(nextQuery)) {
    load()
    return
  }
  router.push({ path: '/patterns', query: nextQuery })
}

function applyFilter() {
  updateFilters()
}

function clearFilter() {
  Object.assign(filterDraft, emptyFilters())
  page.value = 1
  if (!hasActiveFilters.value) {
    load()
    return
  }
  router.push({ path: '/patterns', query: {} })
}

function rowClassName({ row }) {
  return row.sqlFingerprint === activeFilters.value.fingerprint ? 'is-selected-row' : ''
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value - 1, size: size.value }
    if (activeFilters.value.fingerprint) params.fingerprint = activeFilters.value.fingerprint
    if (activeFilters.value.sqlKeyword) params.sqlKeyword = activeFilters.value.sqlKeyword
    if (activeFilters.value.minExecutionCount) params.minExecutionCount = Number(activeFilters.value.minExecutionCount)
    const { data } = await client.get(API_ENDPOINTS.PATTERNS_TOP, { params })
    patterns.value = data.content || []
    total.value = data.totalElements || 0
  } catch (e) {
    console.error('Failed to load patterns', e)
    error.value = e.response?.data?.message || t('patterns.loadFailed')
    patterns.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  selected.value = row
  form.tableName = `rollup_${(row.sqlFingerprint || '').slice(0, 8)}`
  form.schemaName = 'public'
  visible.value = true
}

async function submit() {
  if (!selected.value) {
    ElMessage.error(t('patterns.selectPatternFirst'))
    return
  }
  if (!form.tableName.trim()) {
    ElMessage.error(t('patterns.tableNameRequired'))
    return
  }

  submitting.value = true
  try {
    await client.post(ACCEL_FROM_PATTERN, {
      patternStatsId: selected.value.id,
      tableName: form.tableName.trim(),
      schemaName: form.schemaName.trim() || 'public'
    })
    ElMessage.success(t('patterns.created'))
    visible.value = false
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message)
  } finally {
    submitting.value = false
  }
}

watch(
  activeFilters,
  (nextFilters) => {
    Object.assign(filterDraft, nextFilters)
    page.value = 1
    load()
  },
  { immediate: true }
)
</script>

<style scoped>
.hint {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}
</style>
