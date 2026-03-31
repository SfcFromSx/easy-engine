<template>
  <section class="page-shell">
    <div class="page-header">
      <div class="title-group">
        <h1>{{ t('acceleration.title') }}</h1>
        <p class="subtitle">{{ t('acceleration.subtitle') }}</p>
      </div>
      <div class="page-header-actions">
        <el-button type="primary" @click="openCreate">
          <el-icon style="margin-right: 4px"><plus :size="16" /></el-icon>
          {{ t('acceleration.createManual') }}
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
        <div class="filter-summary">
          <span>{{ t('acceleration.totalItems', { count: total }) }}</span>
          <el-tag size="small" type="success">{{ t('acceleration.active', { count: activeCount }) }}</el-tag>
          <el-tag size="small" type="info">{{ t('acceleration.draft', { count: draftCount }) }}</el-tag>
          <el-tag size="small" type="warning">{{ t('acceleration.disabled', { count: disabledCount }) }}</el-tag>
        </div>
        <el-button text @click="load" :loading="loading">{{ t('common.refresh') }}</el-button>
      </div>

      <div class="table-content" v-loading="loading">
        <div class="table-shell desktop-table">
          <el-table
            class="data-table acceleration-table"
            :data="tables"
            stripe
            row-key="id"
            table-layout="auto"
            :empty-text="t('acceleration.empty')"
          >
            <el-table-column prop="name" :label="t('acceleration.name')" width="180">
              <template #default="{ row }">
                <div class="accel-name">
                  <database :size="14" color="var(--primary-color)" />
                  <span class="text-card-title">{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="t('acceleration.status')" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'warning'" size="small" effect="plain" class="text-mono">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('acceleration.schema')" width="160">
              <template #default="{ row }">
                <div class="column-stack">
                  <span class="text-mono text-small">{{ row.schemaName }}</span>
                  <span class="text-small" style="opacity: 0.7">{{ row.source }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('acceleration.cronField')" width="140">
              <template #default="{ row }">
                <span class="text-mono text-small">{{ row.cronExpr || '--' }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('acceleration.refreshSql')" min-width="280" show-overflow-tooltip>
              <template #default="{ row }">
                <code class="mini-code text-mono" style="font-size: 11px">{{ row.refreshSql || row.ddlText }}</code>
              </template>
            </el-table-column>
            <el-table-column :label="t('acceleration.actions')" width="180">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button size="small" link :type="row.status === 'ACTIVE' ? 'warning' : 'success'" @click="toggleStatus(row)">
                    {{ row.status === 'ACTIVE' ? t('common.deactivate') : t('common.activate') }}
                  </el-button>
                  <el-button size="small" link @click="edit(row)">{{ t('common.configure') }}</el-button>
                  <el-button size="small" link type="danger" @click="remove(row)">{{ t('common.remove') }}</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="!loading && tables.length" class="mobile-records">
          <article v-for="row in tables" :key="row.id" class="glass-card mobile-record">
            <div class="mobile-record-header">
              <div>
                <div class="mobile-record-title">{{ row.name }}</div>
                <div class="mobile-record-subtitle">{{ row.schemaName }}</div>
              </div>
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'warning'" size="small" effect="dark">
                {{ row.status }}
              </el-tag>
            </div>

            <div class="mobile-record-tags">
              <el-tag size="small" effect="plain">{{ row.source }}</el-tag>
              <el-tag size="small" type="info" effect="plain">
                {{ row.cronExpr || t('common.manualOnly') }}
              </el-tag>
            </div>

            <div class="mobile-record-code">
              <span class="mobile-record-label">{{ t('acceleration.ddl') }}</span>
              <code class="mini-code">{{ row.ddlText }}</code>
            </div>

            <div class="mobile-record-code">
              <span class="mobile-record-label">{{ t('acceleration.refreshSql') }}</span>
              <code class="mini-code">{{ row.refreshSql || t('common.manualOnly') }}</code>
            </div>

            <div class="mobile-record-actions">
              <el-button size="small" link type="success" @click="toggleStatus(row)">
                {{ row.status === 'ACTIVE' ? t('common.deactivate') : t('common.activate') }}
              </el-button>
              <el-button size="small" link @click="edit(row)">{{ t('common.configure') }}</el-button>
              <el-button size="small" link type="danger" @click="remove(row)">{{ t('common.remove') }}</el-button>
            </div>
          </article>
        </div>

        <el-empty v-else-if="!loading" class="mobile-only-empty" :description="t('acceleration.empty')" />
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

      <el-dialog v-model="dlg" :title="c.id ? t('acceleration.editTitle') : t('acceleration.createTitle')" width="560px">
        <el-form :model="c" label-width="120px" label-position="top">
          <el-form-item :label="t('acceleration.nameField')">
            <el-input v-model="c.name" placeholder="rollup_orders_daily" />
          </el-form-item>
          <el-form-item :label="t('acceleration.schema')">
            <el-input v-model="c.schemaName" placeholder="public" />
          </el-form-item>
          <el-form-item :label="t('acceleration.ddlField')">
            <el-input type="textarea" v-model="c.ddlText" :rows="3" placeholder="CREATE TABLE ..." />
          </el-form-item>
          <el-form-item :label="t('acceleration.refreshField')">
            <el-input type="textarea" v-model="c.refreshSql" :rows="3" placeholder="INSERT INTO ... SELECT ..." />
          </el-form-item>
          <el-form-item :label="t('acceleration.cronField')">
            <el-input v-model="c.cronExpr" placeholder="0 0 1 * * ?" />
            <div class="hint">{{ t('acceleration.cronHint') }}</div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dlg = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="saving" @click="save">{{ t('common.save') }}</el-button>
        </template>
      </el-dialog>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Database } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import client from '../api/client'
import { ACCELERATION_BY_ID, ACCELERATION_STATUS, API_ENDPOINTS } from '../api/endpoints'

const { t } = useI18n()
const tables = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const dlg = ref(false)
const activeCount = ref(0)
const draftCount = ref(0)
const disabledCount = ref(0)

const c = reactive({
  name: '',
  schemaName: 'public',
  ddlText: '',
  refreshSql: '',
  cronExpr: ''
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [listRes, summaryRes] = await Promise.all([
      client.get(API_ENDPOINTS.ACCELERATION_TABLES, { params: { page: page.value - 1, size: size.value } }),
      client.get(API_ENDPOINTS.STATS_SUMMARY)
    ])
    tables.value = listRes.data.content || []
    total.value = listRes.data.totalElements || 0
    activeCount.value = summaryRes.data.activeAccelerationCount || 0
    draftCount.value = summaryRes.data.draftAccelerationCount || 0
    disabledCount.value = Math.max(0, total.value - activeCount.value - draftCount.value)
  } catch (e) {
    console.error('Failed to load acceleration tables', e)
    error.value = e.response?.data?.message || t('acceleration.loadFailed')
    tables.value = []
    total.value = 0
    activeCount.value = 0
    draftCount.value = 0
    disabledCount.value = 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(c, { id: null, name: '', schemaName: 'public', ddlText: '', refreshSql: '', cronExpr: '' })
  dlg.value = true
}

function edit(row) {
  Object.assign(c, {
    ...row,
    schemaName: row.schemaName || 'public',
    ddlText: row.ddlText || '',
    refreshSql: row.refreshSql || '',
    cronExpr: row.cronExpr || ''
  })
  dlg.value = true
}

async function remove(row) {
  try {
    await client.delete(ACCELERATION_BY_ID(row.id))
    ElMessage.success(t('acceleration.removed'))
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message)
  }
}

async function toggleStatus(row) {
  const status = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  try {
    await client.patch(ACCELERATION_STATUS(row.id), { status })
    ElMessage.success(status === 'ACTIVE' ? t('acceleration.activated') : t('acceleration.disabledMsg'))
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message)
  }
}

async function save() {
  const name = String(c.name || '').trim()
  const schemaName = String(c.schemaName || '').trim() || 'public'
  const ddlText = String(c.ddlText || '').trim()
  const refreshSql = String(c.refreshSql || '').trim()
  const cronExpr = String(c.cronExpr || '').trim()

  if (!name) {
    ElMessage.error(t('acceleration.nameRequired'))
    return
  }
  if (!ddlText) {
    ElMessage.error(t('acceleration.ddlRequired'))
    return
  }

  saving.value = true
  try {
    const payload = {
      name,
      schemaName,
      ddlText,
      refreshSql: refreshSql || null,
      cronExpr: cronExpr || null
    }
    if (c.id) {
      await client.put(ACCELERATION_BY_ID(c.id), payload)
    } else {
      await client.post(API_ENDPOINTS.ACCELERATION_TABLES, payload)
    }
    ElMessage.success(t('acceleration.saved'))
    dlg.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.accel-name {
  display: flex;
  align-items: center;
  gap: 6px;
}

.column-stack {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hint {
  margin-top: 8px;
  color: var(--color-subtitle);
  font-size: 11px;
}

.row-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

:deep(.el-table .el-button--link) {
  padding: 2px 0;
  font-weight: 600;
}
</style>
