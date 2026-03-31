<template>
  <div class="datasources-container">
    <div class="page-header">
      <div class="title-group">
        <h1>{{ $t('datasources.title') }}</h1>
        <p class="subtitle">{{ $t('datasources.subtitle') }}</p>
      </div>
      <div class="actions">
        <input
          ref="driverFileInput"
          type="file"
          accept=".jar"
          class="driver-file-input"
          @change="handleDriverFileChange"
        />
        <el-button
          type="warning"
          plain
          class="premium-btn"
          :loading="uploadingDriver"
          @click="openDriverPicker"
        >
          <el-icon><upload /></el-icon>
          <span>{{ $t('datasources.uploadBtn') }}</span>
        </el-button>
        <el-button type="primary" class="premium-btn" @click="handleAdd">
          <el-icon><plus /></el-icon>
          <span>{{ $t('datasources.addBtn') }}</span>
        </el-button>
      </div>
    </div>

    <el-card class="glass-card driver-card">
      <div class="driver-library">
        <div class="driver-library__copy">
          <span class="driver-library__eyebrow">{{ $t('datasources.driverLibraryEyebrow') }}</span>
          <h2>{{ $t('datasources.driverLibraryTitle') }}</h2>
          <p>{{ $t('datasources.driverLibraryHint') }}</p>
        </div>
        <div class="driver-library__meta">
          <span>{{ $t('datasources.driverCount', { count: uploadedDrivers.length }) }}</span>
        </div>
      </div>
      <div v-loading="driversLoading" class="driver-list">
        <template v-if="uploadedDrivers.length">
          <span
            v-for="driver in uploadedDrivers"
            :key="driver"
            class="driver-pill"
          >
            {{ driver }}
          </span>
        </template>
        <span v-else class="driver-empty">{{ $t('datasources.driverEmpty') }}</span>
      </div>
    </el-card>

    <el-card class="glass-card table-card">
      <div class="list-toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="searchTerm"
            clearable
            class="toolbar-input"
            :placeholder="$t('datasources.filterPlaceholder')"
          >
            <template #prefix>
              <el-icon><search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="toolbar-summary">
          <span>{{ $t('datasources.filterSummary', { count: filteredDataSources.length, total: dataSources.length }) }}</span>
        </div>
      </div>

      <el-table 
        v-loading="loading" 
        :data="filteredDataSources" 
        stripe 
        class="custom-table"
        size="small"
      >
        <el-table-column prop="name" :label="$t('datasources.colName')" min-width="150" show-overflow-tooltip />
        <el-table-column prop="jdbcUrl" :label="$t('datasources.colUrl')" min-width="300" show-overflow-tooltip />
        <el-table-column prop="jdbcUser" :label="$t('datasources.colUser')" min-width="120" />
        <el-table-column prop="driverClass" :label="$t('datasources.colDriver')" min-width="200" show-overflow-tooltip />
        
        <el-table-column :label="$t('common.actions')" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip :content="$t('common.test')" placement="top">
                <el-button circle size="small" type="success" class="action-btn" @click="testConnection(row)">
                  <el-icon><connection /></el-icon>
                </el-button>
              </el-tooltip>
              <el-button circle size="small" type="primary" class="action-btn" @click="handleEdit(row)">
                <el-icon><edit /></el-icon>
              </el-button>
              <el-button circle size="small" type="danger" class="action-btn" @click="handleDelete(row)">
                <el-icon><delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? $t('datasources.dlgEdit') : $t('datasources.dlgAdd')"
      width="600px"
      custom-class="premium-dialog"
    >
      <el-form :model="form" label-position="top" class="premium-form">
        <el-form-item :label="$t('datasources.dlgName')" required>
          <el-input v-model="form.name" placeholder="e.g. Production Kylin" />
        </el-form-item>
        <el-form-item :label="$t('datasources.dlgUrl')" required>
          <el-input v-model="form.jdbcUrl" type="textarea" :rows="3" placeholder="jdbc:kylin://..." />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="$t('datasources.dlgUser')">
              <el-input v-model="form.jdbcUser" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('datasources.dlgPassword')">
              <el-input v-model="form.jdbcPassword" type="password" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('datasources.dlgDriver')" required>
          <el-input v-model="form.driverClass" placeholder="org.apache.kylin.jdbc.Driver" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="saving" @click="saveDataSource">
            {{ $t('common.save') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { Plus, Edit, Delete, Connection, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import client from '../api/client'
import {
  API_ENDPOINTS,
  DATA_SOURCE_BY_ID,
  DATA_SOURCE_TEST,
  DRIVER_UPLOAD
} from '../api/endpoints'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const driversLoading = ref(false)
const uploadingDriver = ref(false)
const dataSources = ref([])
const uploadedDrivers = ref([])
const searchTerm = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const driverFileInput = ref(null)
const form = ref({
  id: null,
  name: '',
  jdbcUrl: '',
  jdbcUser: '',
  jdbcPassword: '',
  driverClass: 'org.apache.kylin.jdbc.Driver'
})

const filteredDataSources = computed(() => {
  const keyword = searchTerm.value.trim().toLowerCase()
  if (!keyword) {
    return dataSources.value
  }
  return dataSources.value.filter((item) => {
    return [item.name, item.jdbcUrl, item.jdbcUser, item.driverClass]
      .some((value) => String(value || '').toLowerCase().includes(keyword))
  })
})

async function fetchDataSources() {
  loading.value = true
  try {
    const { data } = await client.get(API_ENDPOINTS.DATASOURCES)
    dataSources.value = data
  } catch (err) {
    ElMessage.error(t('common.error'))
  } finally {
    loading.value = false
  }
}

async function fetchDrivers() {
  driversLoading.value = true
  try {
    const { data } = await client.get(API_ENDPOINTS.DRIVERS)
    uploadedDrivers.value = data
  } catch (err) {
    ElMessage.error(err.response?.data?.message || t('common.error'))
  } finally {
    driversLoading.value = false
  }
}

function openDriverPicker() {
  driverFileInput.value?.click()
}

function handleAdd() {
  isEdit.value = false
  form.value = {
    id: null,
    name: '',
    jdbcUrl: '',
    jdbcUser: '',
    jdbcPassword: '',
    driverClass: 'org.apache.kylin.jdbc.Driver'
  }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function saveDataSource() {
  if (!form.value.name || !form.value.jdbcUrl || !form.value.driverClass) {
    ElMessage.warning('Required fields missing')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await client.put(DATA_SOURCE_BY_ID(form.value.id), form.value)
    } else {
      await client.post(API_ENDPOINTS.DATASOURCES, form.value)
    }
    ElMessage.success(t('common.success'))
    dialogVisible.value = false
    fetchDataSources()
  } catch (err) {
    ElMessage.error(t('common.error'))
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      t('jobs.confirmDelete', { name: row.name }),
      t('common.warning'),
      { type: 'warning' }
    )
    await client.delete(DATA_SOURCE_BY_ID(row.id))
    ElMessage.success(t('common.success'))
    fetchDataSources()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err.response?.data?.message || t('common.error'))
    }
  }
}

async function testConnection(row) {
  try {
    const { data } = await client.post(DATA_SOURCE_TEST, row)
    if (data === 'SUCCESS') {
      ElMessage.success(t('datasources.testSuccess'))
    } else {
      ElMessage.error(t('datasources.testFailed', { msg: data }))
    }
  } catch (err) {
    ElMessage.error(t('common.error'))
  }
}

async function handleDriverFileChange(event) {
  const [file] = event.target.files || []
  event.target.value = ''
  if (!file) {
    return
  }
  if (!file.name.toLowerCase().endsWith('.jar')) {
    ElMessage.warning(t('datasources.uploadTypeError'))
    return
  }

  const payload = new FormData()
  payload.append('file', file)

  uploadingDriver.value = true
  try {
    await client.post(DRIVER_UPLOAD, payload, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    ElMessage.success(t('datasources.uploadSuccess', { name: file.name }))
    await fetchDrivers()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || t('common.error'))
  } finally {
    uploadingDriver.value = false
  }
}

onMounted(async () => {
  await Promise.all([fetchDataSources(), fetchDrivers()])
})
</script>

<style scoped>
.datasources-container {
  max-width: 100%;
}

.driver-file-input {
  display: none;
}

.page-header {
  margin-bottom: 16px;
}

.actions {
  display: flex;
  gap: 10px;
}

.glass-card {
  padding: 0;
}

.driver-card {
  margin-bottom: 14px;
  padding: 16px;
}

.table-card {
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

.driver-library {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.driver-library__copy h2 {
  margin: 4px 0 6px;
  font-size: 18px;
}

.driver-library__copy p {
  margin: 0;
  color: #64748b;
}

.driver-library__eyebrow {
  display: inline-flex;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #b45309;
}

.driver-library__meta {
  color: #475569;
  font-size: 13px;
  white-space: nowrap;
}

.driver-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  min-height: 32px;
}

.driver-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: #fff7ed;
  border: 1px solid #fdba74;
  color: #9a3412;
  font-size: 12px;
  line-height: 1;
}

.driver-empty {
  color: #94a3b8;
  font-size: 13px;
}
</style>
