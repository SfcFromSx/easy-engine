<template>
  <div class="jobs-view-container">
    <div class="page-header">
      <div class="title-group">
        <h1>{{ $t('jobs.title') }}</h1>
        <p class="subtitle">{{ $t('jobs.subtitle') }}</p>
      </div>
      <el-button type="primary" class="premium-btn" @click="openCreate">
        <el-icon style="margin-right: 4px"><plus :size="16" /></el-icon>
        {{ $t('jobs.addBtn') }}
      </el-button>
    </div>

    <div class="glass-card table-card">
      <div class="list-toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            class="toolbar-input"
            :placeholder="$t('jobs.filterKeywordPlaceholder')"
          />
          <el-select
            v-model="selectedDataSourceId"
            clearable
            filterable
            class="toolbar-select"
            :placeholder="$t('jobs.filterDataSourcePlaceholder')"
          >
            <el-option
              v-for="ds in dataSources"
              :key="ds.id"
              :label="ds.name"
              :value="String(ds.id)"
            />
          </el-select>
          <el-select
            v-model="selectedStrategy"
            clearable
            class="toolbar-select"
            :placeholder="$t('jobs.filterStrategyPlaceholder')"
          >
            <el-option
              v-for="strategy in strategyOptions"
              :key="strategy"
              :label="strategy"
              :value="strategy"
            />
          </el-select>
          <el-select
            v-model="selectedTestSetFilter"
            clearable
            filterable
            class="toolbar-select"
            :placeholder="$t('jobs.filterTestSetPlaceholder')"
          >
            <el-option :label="$t('jobs.filterTemplatesOnly')" value="__templates__" />
            <el-option
              v-for="testSet in testSets"
              :key="testSet.id"
              :label="testSet.name"
              :value="String(testSet.id)"
            />
          </el-select>
        </div>
        <div class="toolbar-summary">
          <span>{{ $t('jobs.filterSummary', { count: filteredJobs.length, total: jobs.length }) }}</span>
        </div>
      </div>

      <el-table :data="filteredJobs" v-loading="loading" stripe class="custom-table" size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" :label="$t('jobs.colName')" min-width="180">
          <template #default="{ row }">
            <el-link 
              @click="$router.push(`/jobs/${row.id}`)" 
              style="font-weight: 700; color: #3b82f6; font-size: 14px"
            >
              {{ row.name }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column :label="$t('jobs.colDataSource')" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="info">
              <el-icon style="margin-right: 4px"><connection /></el-icon>
              {{ getDataSourceName(row.dataSourceId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="strategy" :label="$t('jobs.colStrategy')" width="140">
          <template #default="{ row }">
            <el-tag :type="strategyTagType(row.strategy)" size="small" effect="dark" class="strategy-tag">
              {{ row.strategy }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('jobs.colConcurrencyRounds')" width="160">
          <template #default="{ row }">
            <span style="font-size: 13px">{{ row.concurrentThreads }} / {{ row.rounds }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('jobs.colTestSet')" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ getTestSetName(row.testSetId) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              link
              :loading="startingId === row.id"
              :disabled="startingId !== null && startingId !== row.id"
              @click="start(row)"
            >
              {{ $t('jobs.startTest') }}
            </el-button>
            <el-button size="small" link @click="edit(row)">{{ $t('common.edit') }}</el-button>
            <el-button size="small" link type="danger" @click="removeJob(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog 
        v-model="dlg" 
        :title="form.id ? $t('jobs.dlgEdit') : $t('jobs.dlgAdd')" 
        width="720px" 
        class="premium-dialog"
      >
        <el-form label-position="top" class="job-form premium-form">
          <el-form-item :label="$t('jobs.dlgName')" required>
            <el-input v-model="form.name" placeholder="e.g. Kylin Sales Regression" />
          </el-form-item>
          
          <el-form-item :label="$t('jobs.dlgDataSource')" required>
            <el-select 
              v-model="form.dataSourceId" 
              filterable 
              style="width: 100%"
              :placeholder="$t('debugger.selectDataSource')"
            >
              <el-option 
                v-for="ds in dataSources" 
                :key="ds.id" 
                :label="ds.name" 
                :value="ds.id" 
              >
                <div class="ds-option">
                  <span class="ds-name">{{ ds.name }}</span>
                  <span class="ds-url">{{ ds.jdbcUrl }}</span>
                </div>
              </el-option>
            </el-select>
            <p class="hint">Tasks are now decoupled from connection details. Choose a Data Source profile to execute this job.</p>
          </el-form-item>

          <div class="form-section">
            <div class="section-title">Performance & Strategy</div>
            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item :label="$t('jobs.dlgConcurrency')">
                  <el-input-number v-model="form.concurrentThreads" :min="1" :max="1000" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('jobs.dlgRounds')">
                  <el-input-number v-model="form.rounds" :min="1" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item :label="$t('jobs.dlgStrategy')">
                  <el-select v-model="form.strategy" style="width: 100%">
                    <el-option label="RANDOM_WEIGHT" value="RANDOM_WEIGHT" />
                    <el-option label="ROUND_ROBIN" value="ROUND_ROBIN" />
                    <el-option label="CACHE_PENETRATION" value="CACHE_PENETRATION" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <el-form-item :label="$t('jobs.dlgTestSet')">
            <el-select
              v-model="form.testSetId"
              clearable
              filterable
              :placeholder="$t('jobs.dlgTestSetPlaceholder')"
              style="width: 100%"
            >
              <el-option
                v-for="ts in testSets"
                :key="ts.id"
                :label="`${ts.name} (${ts.itemCount} SQLs)`"
                :value="ts.id"
              />
            </el-select>
            <p class="hint">{{ $t('jobs.dlgTestSetHint') }}</p>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="dlg = false">{{ $t('common.cancel') }}</el-button>
            <el-button type="primary" class="premium-btn" @click="save">{{ $t('common.save') }}</el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Plus, Connection } from '@element-plus/icons-vue'
import client from '../api/client'
import {
  filterJobs,
  getDataSourceName as resolveDataSourceName,
  getTestSetName as resolveTestSetName,
  strategyTagType
} from '../utils/benchmarkViewHelpers'

const router = useRouter()
const jobs = ref([])
const testSets = ref([])
const dataSources = ref([])
const loading = ref(false)
const startingId = ref(null)
const dlg = ref(false)
const keyword = ref('')
const selectedDataSourceId = ref('')
const selectedStrategy = ref('')
const selectedTestSetFilter = ref('')
const form = reactive({
  id: null,
  name: '',
  dataSourceId: null,
  concurrentThreads: 4,
  rounds: 100,
  strategy: 'RANDOM_WEIGHT',
  testSetId: null
})

const strategyOptions = ['RANDOM_WEIGHT', 'ROUND_ROBIN', 'CACHE_PENETRATION']

const filteredJobs = computed(() => filterJobs(
  jobs.value,
  keyword.value,
  selectedDataSourceId.value,
  selectedStrategy.value,
  selectedTestSetFilter.value,
  dataSources.value,
  testSets.value
))

async function load() {
  loading.value = true
  try {
    const [jobsRes, tsRes, dsRes] = await Promise.all([
      client.get('/jobs'), 
      client.get('/test-sets'),
      client.get('/datasources')
    ])
    jobs.value = jobsRes.data
    testSets.value = tsRes.data
    dataSources.value = dsRes.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    id: null,
    name: '',
    dataSourceId: dataSources.value.length > 0 ? dataSources.value[0].id : null,
    concurrentThreads: 4,
    rounds: 100,
    strategy: 'RANDOM_WEIGHT',
    testSetId: null
  })
  dlg.value = true
}

function edit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    dataSourceId: row.dataSourceId,
    concurrentThreads: row.concurrentThreads,
    rounds: row.rounds,
    strategy: row.strategy,
    testSetId: row.testSetId
  })
  dlg.value = true
}

function getTestSetName(id) {
  return resolveTestSetName(testSets.value, id)
}

function getDataSourceName(id) {
  return resolveDataSourceName(dataSources.value, id)
}

async function removeJob(row) {
  try {
    await ElMessageBox.confirm('Confirm delete task? Historical runs will be preserved.', 'Warning', {
      type: 'warning'
    })
    await client.delete('/jobs/' + row.id)
    ElMessage.success('Deleted')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e.response?.data?.message || e.message || 'Delete failed')
    }
  }
}

async function save() {
  if (!form.name || !form.dataSourceId) {
    ElMessage.warning('Required fields missing')
    return
  }
  try {
    const payload = { ...form }
    if (form.id) {
      await client.put('/jobs/' + form.id, payload)
    } else {
      await client.post('/jobs', payload)
    }
    ElMessage.success('Saved')
    dlg.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message)
  }
}

async function start(row) {
  try {
    await ElMessageBox.confirm(
      `Confirm start benchmark for "${row.name}"?`,
      'Ready to Execute',
      { type: 'warning' }
    )
  } catch { return }
  
  startingId.value = row.id
  try {
    const { data } = await client.post('/runs/start', { jobId: row.id })
    ElMessage.success(`Benchmark submitted CID: #${data.id}`)
    router.push({ path: '/runs', query: { jobId: String(row.id) } })
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message)
  } finally {
    startingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.jobs-view-container {
  max-width: 100%;
}

.page-header {
  margin-bottom: 16px;
}

.glass-card {
  padding: 0;
}

:deep(.el-table__row) {
  height: 40px;
}

.premium-btn {
  height: 32px;
  padding: 0 12px;
}

.list-toolbar {
  padding: 10px 16px;
  margin-bottom: 0;
  border-bottom: 1px solid #f1f5f9;
}

.hint {
  font-size: var(--font-size-small);
  color: var(--color-subtitle);
  padding: 6px 10px;
  margin: 4px 0;
}

.form-section {
  padding: 16px;
  background: rgba(241, 245, 249, 0.5);
  border-radius: 12px;
  margin-bottom: 24px;
}

.section-title {
  font-size: 11px;
  font-weight: 800;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 16px;
}

:deep(.el-card__body) {
  padding: 0 !important; /* Table card should have no body padding */
}
</style>
