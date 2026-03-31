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
            v-model="fingerprintDraft"
            class="filter-input"
            clearable
            :placeholder="t('patterns.filterPlaceholder')"
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
        <div v-if="selectedFingerprint" class="filter-actions">
          <div class="filter-summary">
            <terminal :size="14" />
            <span>{{ t('patterns.onlyFingerprint') }}</span>
            <code class="mini-code">{{ selectedFingerprint }}</code>
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
            :class="['glass-card', 'mobile-record', { 'mobile-record--selected': row.sqlFingerprint === selectedFingerprint }]"
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
const size = ref(20)
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const visible = ref(false)
const selected = ref(null)
const fingerprintDraft = ref('')
const form = reactive({ tableName: '', schemaName: 'public' })

const selectedFingerprint = computed(() => {
  const value = route.query.fingerprint
  return typeof value === 'string' && value.trim() ? value.trim() : ''
})

function goTraces(row) {
  router.push({ path: '/traces', query: { fingerprint: row.sqlFingerprint } })
}

function updateFingerprintFilter(nextFingerprint) {
  const normalized = nextFingerprint.trim()
  page.value = 1
  if (normalized === selectedFingerprint.value) {
    load()
    return
  }
  router.push({ path: '/patterns', query: normalized ? { fingerprint: normalized } : {} })
}

function applyFilter() {
  updateFingerprintFilter(fingerprintDraft.value)
}

function clearFilter() {
  fingerprintDraft.value = ''
  updateFingerprintFilter('')
}

function rowClassName({ row }) {
  return row.sqlFingerprint === selectedFingerprint.value ? 'is-selected-row' : ''
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value - 1, size: size.value }
    if (selectedFingerprint.value) {
      params.fingerprint = selectedFingerprint.value
    }
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
  () => selectedFingerprint.value,
  () => {
    fingerprintDraft.value = selectedFingerprint.value
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
