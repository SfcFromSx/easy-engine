<template>
  <div class="datasources-container">
    <div class="page-header">
      <div class="title-section">
        <div class="main-title">{{ $t('datasources.title') }}</div>
        <div class="subtitle">{{ $t('datasources.subtitle') }}</div>
      </div>
      <div class="actions">
        <el-button type="primary" class="premium-btn" @click="handleAdd">
          <el-icon><plus /></el-icon>
          <span>{{ $t('datasources.addBtn') }}</span>
        </el-button>
      </div>
    </div>

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
import { Plus, Edit, Delete, Connection, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import client from '../api/client'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const dataSources = ref([])
const searchTerm = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
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
    const { data } = await client.get('/datasources')
    dataSources.value = data
  } catch (err) {
    ElMessage.error(t('common.error'))
  } finally {
    loading.value = false
  }
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
      await client.put(`/datasources/${form.value.id}`, form.value)
    } else {
      await client.post('/datasources', form.value)
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
    await client.delete(`/datasources/${row.id}`)
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
    const { data } = await client.post('/datasources/test', row)
    if (data === 'SUCCESS') {
      ElMessage.success(t('datasources.testSuccess'))
    } else {
      ElMessage.error(t('datasources.testFailed', { msg: data }))
    }
  } catch (err) {
    ElMessage.error(t('common.error'))
  }
}

onMounted(fetchDataSources)
</script>

<style scoped>
.datasources-container {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;
}

.title-section .main-title {
  font-family: 'Outfit', sans-serif;
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.title-section .subtitle {
  color: #64748b;
  margin-top: 4px;
  font-size: 15px;
}

.glass-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.04);
  border-radius: 16px;
}

.table-card {
  padding: 8px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  border: none;
  transition: all 0.2s;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.el-table) {
  --el-table-header-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-bg-color: transparent;
}

:deep(.el-table__row) {
  height: 48px;
}

:deep(.el-table__header th) {
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  font-size: 11px;
  letter-spacing: 0.05em;
  border-bottom: 2px solid #f1f5f9;
}

.premium-btn {
  height: 42px;
  padding: 0 24px;
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.4);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.premium-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.5);
}

.premium-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #334155;
  padding-bottom: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 12px;
}

@media (max-width: 960px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .actions {
    width: 100%;
  }

  .premium-btn {
    width: 100%;
  }
}
</style>
