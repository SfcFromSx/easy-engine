<template>
  <div class="test-sets-view-container">
    <div class="page-header">
      <div class="title-group">
        <h1>{{ $t('testSets.title') }}</h1>
        <p class="subtitle">{{ $t('testSets.subtitle') }}</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon style="margin-right: 4px"><Plus :size="16" /></el-icon>
        {{ $t('testSets.addBtn') }}
      </el-button>
    </div>

    <div class="workflow-callout compact-alert">
      <div class="workflow-callout__title">💡 {{ $t('testSets.workflowTitle') }}</div>
      <p style="font-size: 11px; margin: 2px 0;">{{ $t('testSets.workflowOverride') }} {{ $t('testSets.workflowPrepared') }} {{ $t('testSets.workflowColumns') }}</p>
    </div>

    <el-card class="glass-card mb-16" shadow="never">
      <div class="upload-area">
        <el-upload
          class="excel-uploader"
          drag
          action="#"
          :auto-upload="false"
          :on-change="handleExcelChange"
          :show-file-list="false"
          accept=".xlsx, .xls"
        >
          <el-icon class="el-icon--upload"><upload-cloud /></el-icon>
          <div class="el-upload__text">
            {{ $t('testSets.uploadHint') }} <em>{{ $t('testSets.uploadClick') }}</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              {{ $t('testSets.uploadTip') }}
              <span class="upload-tip-accent">{{ $t('testSets.uploadOptionalColumns') }}</span>
            </div>
          </template>
        </el-upload>
      </div>
    </el-card>

    <div class="glass-card table-card">
      <div class="list-toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="keyword"
            clearable
            class="toolbar-input"
            :placeholder="$t('testSets.filterKeywordPlaceholder')"
          />
          <el-select
            v-model="sourceFilter"
            class="toolbar-select"
            :placeholder="$t('testSets.filterSourcePlaceholder')"
          >
            <el-option :label="$t('testSets.sourceAll')" value="all" />
            <el-option :label="$t('testSets.sourceUploaded')" value="uploaded" />
            <el-option :label="$t('testSets.sourceManual')" value="manual" />
          </el-select>
        </div>
        <div class="toolbar-summary">
          <span>{{ $t('testSets.filterSummary', { count: filteredTestSets.length, total: testSets.length }) }}</span>
        </div>
      </div>

      <el-table :data="filteredTestSets" v-loading="loading" stripe>
        <el-table-column prop="name" :label="$t('testSets.colName')" width="220" />
        <el-table-column prop="sourceFilename" :label="$t('testSets.colType')" width="180">
          <template #default="{ row }">
            <el-tag :type="row.sourceFilename ? 'primary' : 'info'" size="small">
              {{ row.sourceFilename || $t('testSets.manualLabel') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('testSets.colQueries')" width="120">
          <template #default="{ row }">
            {{ row.itemCount ?? row.sqlCount ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('testSets.colDesc')" min-width="200" />
        <el-table-column :label="$t('common.actions')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="viewItems(row)">{{ $t('testSets.viewBtn') }}</el-button>
            <el-button size="small" link @click="edit(row)">{{ $t('common.edit') }}</el-button>
            <el-button size="small" type="danger" link @click="remove(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="$t('testSets.drawerTitle', { name: activeSet?.name || '-' })"
      size="650px"
      destroy-on-close
    >
      <div class="drawer-guide">
        <div class="drawer-guide__title">{{ $t('testSets.drawerGuideTitle') }}</div>
        <p>{{ $t('testSets.drawerHint') }}</p>
      </div>
      <div v-loading="itemsLoading">
        <div v-for="item in items" :key="item.id" class="item-entry">
          <div class="item-meta">
            <el-tag size="small" type="info">#{{ item.sortOrder }}</el-tag>
            <span class="item-label">{{ item.label || '无标签' }}</span>
            <el-tag size="small" effect="plain">权重: {{ item.weight }}</el-tag>
            <el-tag size="small" :type="item.executionMode === 'PREPARED_STATEMENT' ? 'warning' : 'info'">
              {{ item.executionMode || 'STATEMENT' }}
            </el-tag>
          </div>
          <p class="item-mode-hint">
            {{
              item.executionMode === 'PREPARED_STATEMENT'
                ? $t('testSets.itemPreparedHint')
                : $t('testSets.itemStatementHint')
            }}
          </p>
          <CodeBlock :code="item.sqlText" />
          <div v-if="item.paramJson" class="param-json">
            <strong>{{ $t('testSets.itemParamsLabel') }}:</strong>
            <pre>{{ formatParamJson(item.paramJson) }}</pre>
          </div>
          <div v-else-if="item.executionMode === 'PREPARED_STATEMENT'" class="param-json param-json--missing">
            <strong>{{ $t('testSets.itemParamsLabel') }}:</strong>
            <span>{{ $t('testSets.itemParamsMissing') }}</span>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="dlg" :title="form.id ? $t('testSets.dlgEdit') : $t('testSets.dlgAdd')" width="600px">
      <el-form :model="form" label-width="120px">
        <el-form-item :label="$t('testSets.colName')" required>
          <el-input v-model="form.name" :placeholder="$t('testSets.colName')" />
        </el-form-item>
        <el-form-item :label="$t('testSets.colDesc')">
          <el-input type="textarea" v-model="form.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, UploadCloud } from 'lucide-vue-next'
import client from '../api/client'
import { API_ENDPOINTS, TEST_SET_ITEMS } from '../api/endpoints'
import CodeBlock from '../components/CodeBlock.vue'
import { filterTestSets, formatParamJson } from '../utils/benchmarkViewHelpers'

const testSets = ref([])
const loading = ref(false)
const dlg = ref(false)
const drawerVisible = ref(false)
const itemsLoading = ref(false)
const items = ref([])
const activeSet = ref(null)
const keyword = ref('')
const sourceFilter = ref('all')

const form = reactive({ id: null, name: '', description: '' })

const filteredTestSets = computed(() => filterTestSets(testSets.value, keyword.value, sourceFilter.value))

async function load() {
  loading.value = true
  try {
    const { data } = await client.get(API_ENDPOINTS.TEST_SETS)
    testSets.value = data
  } finally {
    loading.value = false
  }
}

async function handleExcelChange(file) {
  loading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.raw)
    const { data } = await client.post(`${API_ENDPOINTS.TEST_SETS}/upload`, fd)
    ElMessage.success(`导入成功: 「${data.testSet.name}」已同步，共 ${data.itemCount} 条 SQL`)
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Excel 导入失败')
  } finally {
    loading.value = false
  }
}

async function viewItems(row) {
  activeSet.value = row
  drawerVisible.value = true
  itemsLoading.value = true
  try {
    const { data } = await client.get(TEST_SET_ITEMS(row.id))
    items.value = data
  } catch (e) {
    ElMessage.error('加载项目失败')
  } finally {
    itemsLoading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, name: '', description: '' })
  dlg.value = true
}

function edit(row) {
  Object.assign(form, { id: row.id, name: row.name, description: row.description })
  dlg.value = true
}

async function save() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写测试集名称')
    return
  }
  try {
    if (form.id) await client.put(`${API_ENDPOINTS.TEST_SETS}/${form.id}`, form)
    else await client.post(API_ENDPOINTS.TEST_SETS, form)
    ElMessage.success('配置已保存')
    dlg.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除测试集「${row.name}」吗？这将导致关联的所有 SQL 明细被永久物理删除，且不可恢复。`,
      '危险操作提示',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await client.delete(`${API_ENDPOINTS.TEST_SETS}/${row.id}`)
    ElMessage.success('测试集已永久删除')
    await load()
  } catch { /* cancel */ }
}

onMounted(load)
</script>

<style scoped>
.test-sets-view-container {
  max-width: 100%;
}

.page-header {
  margin-bottom: 16px;
}

.mb-16 { margin-bottom: 12px; }
.upload-area { padding: 10px; text-align: center; }
.excel-uploader { width: 100%; }

:deep(.el-upload-dragger) {
  padding: 10px 20px !important;
}

:deep(.el-icon--upload) {
  font-size: 32px !important;
  margin-bottom: 8px !important;
}

.workflow-callout {
  margin-bottom: 12px;
  padding: 8px 12px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: linear-gradient(135deg, #f8fbff 0%, #eef6ff 100%);
  color: var(--color-body);
}

.workflow-callout__title {
  font-size: var(--font-size-subtitle);
  font-weight: 700;
  color: #1e3a8a;
  margin-bottom: 4px;
}

.glass-card {
  padding: 0;
}

.table-card {
  padding: 0;
}

:deep(.el-table__row) {
  height: 40px;
}

:deep(.el-card__body) {
  padding: 0 !important;
}

.upload-area :deep(.el-card__body) {
  padding: 10px !important;
}
</style>
