<template>
  <section class="page-shell">
    <div class="page-header">
      <div class="title-group">
        <h1>{{ t('datasources.title') }}</h1>
        <p class="subtitle">{{ t('datasources.subtitle') }}</p>
      </div>
      <div class="page-header-actions">
        <el-button type="primary" @click="openCreate">
          <el-icon style="margin-right: 4px"><plus :size="16" /></el-icon>
          {{ t('datasources.add') }}
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
          <span>{{ t('datasources.totalItems', { count: configs.length }) }}</span>
          <el-tag size="small" type="success">{{ t('datasources.defaultCount', { count: defaultCount }) }}</el-tag>
          <el-tag size="small" type="info">{{ t('datasources.customCount', { count: customCount }) }}</el-tag>
        </div>
        <el-button text :loading="loading" @click="load">{{ t('common.refresh') }}</el-button>
      </div>

      <div class="table-content" v-loading="loading">
        <div class="table-shell desktop-table">
          <el-table
            class="data-table datasource-table"
            :data="configs"
            stripe
            row-key="id"
            table-layout="auto"
            :empty-text="t('datasources.empty')"
          >
            <el-table-column :label="t('datasources.name')" min-width="220">
              <template #default="{ row }">
                <div class="datasource-name">
                  <server :size="16" color="var(--primary-color)" />
                  <div>
                    <div class="text-card-title">{{ row.name }}</div>
                    <div class="datasource-meta">{{ row.type || '--' }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('datasources.default')" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isDefault ? 'success' : 'info'" effect="plain" size="small">
                  {{ row.isDefault ? t('datasources.default') : row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('datasources.jdbcUrl')" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">
                <code class="mini-code datasource-code">{{ row.jdbcUrl }}</code>
              </template>
            </el-table-column>
            <el-table-column :label="t('datasources.driverClass')" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="datasource-meta">{{ row.driverClass }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('datasources.pool')" width="160">
              <template #default="{ row }">
                <div class="pool-stack">
                  <span>{{ row.maxPoolSize }}/{{ row.minIdle }}</span>
                  <span class="datasource-meta">{{ row.connectionTimeoutMs }}ms</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('datasources.actions')" width="180">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button size="small" link @click="edit(row)">{{ t('common.configure') }}</el-button>
                  <el-button
                    v-if="!row.isDefault"
                    size="small"
                    link
                    type="success"
                    @click="promote(row)"
                  >
                    {{ t('datasources.setDefault') }}
                  </el-button>
                  <el-button size="small" link type="danger" @click="remove(row)">{{ t('common.remove') }}</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="!loading && configs.length" class="mobile-records">
          <article v-for="row in configs" :key="row.id" class="glass-card mobile-record">
            <div class="mobile-record-header">
              <div>
                <div class="mobile-record-title">{{ row.name }}</div>
                <div class="mobile-record-subtitle">{{ row.type || '--' }}</div>
              </div>
              <el-tag :type="row.isDefault ? 'success' : 'info'" size="small" effect="dark">
                {{ row.isDefault ? t('datasources.default') : row.type }}
              </el-tag>
            </div>

            <div class="mobile-record-code">
              <span class="mobile-record-label">{{ t('datasources.jdbcUrl') }}</span>
              <code class="mini-code">{{ row.jdbcUrl }}</code>
            </div>

            <div class="mobile-record-code">
              <span class="mobile-record-label">{{ t('datasources.driverClass') }}</span>
              <code class="mini-code">{{ row.driverClass }}</code>
            </div>

            <div class="mobile-record-grid">
              <div>
                <span class="mobile-record-label">{{ t('datasources.username') }}</span>
                <div>{{ row.username || '--' }}</div>
              </div>
              <div>
                <span class="mobile-record-label">{{ t('datasources.pool') }}</span>
                <div>{{ row.maxPoolSize }}/{{ row.minIdle }} · {{ row.connectionTimeoutMs }}ms</div>
              </div>
            </div>

            <div class="mobile-record-actions">
              <el-button size="small" link @click="edit(row)">{{ t('common.configure') }}</el-button>
              <el-button v-if="!row.isDefault" size="small" link type="success" @click="promote(row)">
                {{ t('datasources.setDefault') }}
              </el-button>
              <el-button size="small" link type="danger" @click="remove(row)">{{ t('common.remove') }}</el-button>
            </div>
          </article>
        </div>

        <el-empty v-else-if="!loading" class="mobile-only-empty" :description="t('datasources.empty')" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? t('datasources.editTitle') : t('datasources.createTitle')" width="640px">
      <el-form :model="form" label-position="top">
        <div class="form-grid">
          <el-form-item :label="t('datasources.name')">
            <el-input v-model="form.name" placeholder="default" />
          </el-form-item>
          <el-form-item :label="t('datasources.type')">
            <el-input v-model="form.type" placeholder="kylin / presto / hive" />
          </el-form-item>
          <el-form-item class="form-span-2" :label="t('datasources.jdbcUrl')">
            <el-input v-model="form.jdbcUrl" placeholder="jdbc:kylin://localhost:17070/learn_kylin" />
          </el-form-item>
          <el-form-item class="form-span-2" :label="t('datasources.driverClass')">
            <el-input v-model="form.driverClass" placeholder="org.apache.kylin.jdbc.Driver" />
          </el-form-item>
          <el-form-item :label="t('datasources.username')">
            <el-input v-model="form.username" placeholder="ADMIN" />
          </el-form-item>
          <el-form-item :label="t('datasources.password')">
            <el-input v-model="form.password" type="password" show-password placeholder="KYLIN" />
          </el-form-item>
          <el-form-item :label="t('datasources.pool')">
            <el-input-number v-model="form.maxPoolSize" :min="1" :max="64" />
          </el-form-item>
          <el-form-item label="Min Idle">
            <el-input-number v-model="form.minIdle" :min="0" :max="64" />
          </el-form-item>
          <el-form-item :label="t('datasources.default')" class="form-span-2">
            <div class="default-toggle">
              <el-switch v-model="form.isDefault" />
              <span class="datasource-meta">{{ t('datasources.setDefaultHint') }}</span>
            </div>
          </el-form-item>
          <el-form-item label="Connection Timeout (ms)" class="form-span-2">
            <el-input-number v-model="form.connectionTimeoutMs" :min="1000" :step="1000" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Server } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import { API_ENDPOINTS, QUERY_DATASOURCE_BY_ID } from '../api/endpoints'

const { t } = useI18n()
const configs = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const dialogVisible = ref(false)

const form = reactive({
  id: null,
  name: '',
  type: '',
  driverClass: '',
  jdbcUrl: '',
  username: '',
  password: '',
  maxPoolSize: 4,
  minIdle: 1,
  connectionTimeoutMs: 10000,
  isDefault: false
})

const defaultCount = computed(() => configs.value.filter((row) => row.isDefault).length)
const customCount = computed(() => Math.max(0, configs.value.length - defaultCount.value))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await client.get(API_ENDPOINTS.QUERY_DATASOURCES)
    configs.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('Failed to load datasource configs', e)
    error.value = e.response?.data?.message || t('datasources.loadFailed')
    configs.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: null,
    name: '',
    type: '',
    driverClass: '',
    jdbcUrl: '',
    username: '',
    password: '',
    maxPoolSize: 4,
    minIdle: 1,
    connectionTimeoutMs: 10000,
    isDefault: false
  })
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function edit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    type: row.type || '',
    driverClass: row.driverClass || '',
    jdbcUrl: row.jdbcUrl || '',
    username: row.username || '',
    password: row.password || '',
    maxPoolSize: row.maxPoolSize ?? 4,
    minIdle: row.minIdle ?? 1,
    connectionTimeoutMs: row.connectionTimeoutMs ?? 10000,
    isDefault: Boolean(row.isDefault)
  })
  dialogVisible.value = true
}

async function promote(row) {
  edit(row)
  form.isDefault = true
  await save()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`${row.name}`, t('common.remove'), {
      confirmButtonText: t('common.remove'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
    await client.delete(QUERY_DATASOURCE_BY_ID(row.id))
    ElMessage.success(t('datasources.removed'))
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e.response?.data?.message || e.message)
    }
  }
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning(t('datasources.nameRequired'))
    return
  }
  if (!form.type.trim()) {
    ElMessage.warning(t('datasources.typeRequired'))
    return
  }
  if (!form.driverClass.trim()) {
    ElMessage.warning(t('datasources.driverRequired'))
    return
  }
  if (!form.jdbcUrl.trim()) {
    ElMessage.warning(t('datasources.jdbcRequired'))
    return
  }

  const payload = {
    name: form.name.trim(),
    type: form.type.trim(),
    driverClass: form.driverClass.trim(),
    jdbcUrl: form.jdbcUrl.trim(),
    username: form.username?.trim() || null,
    password: form.password ?? '',
    maxPoolSize: form.maxPoolSize,
    minIdle: form.minIdle,
    connectionTimeoutMs: form.connectionTimeoutMs,
    isDefault: Boolean(form.isDefault)
  }

  saving.value = true
  try {
    if (form.id) {
      await client.put(QUERY_DATASOURCE_BY_ID(form.id), payload)
    } else {
      await client.post(API_ENDPOINTS.QUERY_DATASOURCES, payload)
    }
    dialogVisible.value = false
    ElMessage.success(t('datasources.saved'))
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
.datasource-name {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.datasource-meta {
  color: var(--color-subtitle);
  font-size: 12px;
  line-height: 1.45;
}

.datasource-code {
  display: inline-block;
  max-width: 100%;
  font-size: 11px;
  white-space: normal;
  word-break: break-all;
}

.pool-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.form-span-2 {
  grid-column: span 2;
}

.default-toggle {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
}

.mobile-record-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mobile-record-label {
  display: inline-block;
  margin-bottom: 4px;
  color: var(--color-subtitle);
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

@media (max-width: 900px) {
  .form-grid,
  .mobile-record-grid {
    grid-template-columns: 1fr;
  }

  .form-span-2 {
    grid-column: span 1;
  }
}
</style>
