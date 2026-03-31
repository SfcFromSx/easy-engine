<template>
  <div class="templates-view-container">
    <div class="page-header">
      <div class="title-group">
        <h1>{{ $t('templates.title') }}</h1>
        <p class="subtitle">{{ $t('templates.subtitle') }}</p>
      </div>
      <div class="actions">
        <el-input
          v-model="searchKeyword"
          :placeholder="$t('templates.searchPlaceholder')"
          class="toolbar-input"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="executionModeFilter"
          clearable
          class="toolbar-select"
          :placeholder="$t('templates.filterModePlaceholder')"
          @change="handleSearch"
        >
          <el-option :label="$t('templates.filterModeAll')" value="" />
          <el-option label="STATEMENT" value="STATEMENT" />
          <el-option label="PREPARED_STATEMENT" value="PREPARED_STATEMENT" />
        </el-select>
        <el-button type="primary" @click="openCreate">
          <el-icon style="margin-right: 4px"><Plus :size="16" /></el-icon>
          {{ $t('templates.addBtn') }}
        </el-button>
      </div>
    </div>
    <DebuggerDialog v-model="debugVisible" :sql="debugSql" />

    <div class="flow-callout compact-alert">
      <div class="flow-callout__title">⚡ {{ $t('templates.executionGuideTitle') }}</div>
      <p style="font-size: 11px; margin: 2px 0;">{{ $t('templates.executionGuideGlobal') }} {{ $t('templates.executionGuideOverride') }}</p>
      <div class="flow-callout__mode" style="margin-top: 4px;">
        <span class="mode-chip">STMT</span>
        <span style="font-size: 11px;">{{ $t('templates.executionGuideStatement') }}</span>
        <span class="mode-chip mode-chip--prepared" style="margin-left: 8px;">PREP</span>
        <span style="font-size: 11px;">{{ $t('templates.executionGuidePrepared') }}</span>
      </div>
    </div>

    <div class="glass-card table-card">
      <el-table :data="templates" v-loading="loading" stripe size="small">
        <el-table-column prop="name" :label="$t('templates.colName')" width="180">
          <template #default="{ row }">
            <span class="template-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sqlText" :label="$t('templates.colSql')" min-width="420">
          <template #default="{ row }">
            <div class="sql-preview-cell">
              <div class="sql-preview-text">{{ previewSql(row.sqlText).previewText }}</div>
              <el-button
                v-if="previewSql(row.sqlText).isTruncated"
                link
                type="primary"
                class="sql-preview-link"
                @click="openSqlPreview(row)"
              >
                {{ $t('templates.previewOpen') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="weight" :label="$t('templates.colWeight')" width="80" align="center" />
        <el-table-column :label="$t('templates.colMode')" width="170">
          <template #default="{ row }">
            <el-tag size="small" :type="row.executionMode === 'PREPARED_STATEMENT' ? 'warning' : 'info'">
              {{ row.executionMode || 'STATEMENT' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('templates.colParams')" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <code class="mini-code">{{ row.paramJson || '[]' }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="runDebug(row)">
              <el-icon><Play :size="14" /></el-icon>
            </el-button>
            <el-button size="small" link @click="edit(row)">{{ $t('common.edit') }}</el-button>
            <el-button size="small" type="danger" link @click="remove(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? $t('templates.dlgEdit') : $t('templates.dlgAdd')" width="820px" top="8vh">
      <el-form :model="form" label-position="top" class="form-container">
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item :label="$t('templates.colName')" required>
              <el-input v-model="form.name" :placeholder="$t('templates.searchPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('templates.colWeight')">
              <el-input-number v-model="form.weight" :min="1" :max="1000" style="width: 100%" />
              <div class="hint">{{ $t('templates.hintWeight') }}</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="$t('templates.dlgSqlLabel')" required>
          <template #label functional>
            <div style="display: flex; justify-content: space-between; width: 100%">
              <span>{{ $t('templates.dlgSqlLabel') }}</span>
            </div>
          </template>
          <el-input
            type="textarea"
            :rows="12"
            v-model="form.sqlText"
            :placeholder="$t('templates.dlgSqlPlaceholder')"
            style="font-family: 'Fira Code', 'Courier New', monospace; font-size: 13px"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="10">
            <el-form-item :label="$t('templates.modeLabel')">
              <el-select v-model="form.executionMode" style="width: 100%">
                <el-option :label="$t('templates.modeStatementOption')" value="STATEMENT" />
                <el-option :label="$t('templates.modePreparedOption')" value="PREPARED_STATEMENT" />
              </el-select>
              <div class="hint">
                {{ isPreparedMode ? $t('templates.modeHintPrepared') : $t('templates.modeHintStatement') }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="14">
            <el-form-item :label="$t('templates.paramsLabel')">
              <el-input
                v-model="form.paramJson"
                :disabled="!isPreparedMode"
                :placeholder="$t('templates.paramPlaceholder')"
              />
              <div class="hint">
                {{ $t('templates.paramHint') }}
                <span class="hint-inline">{{ $t('templates.paramExampleLabel') }}</span>
                <code class="inline-code">[{"type":"INTEGER","value":1}]</code>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <div class="mode-summary-grid">
          <div class="mode-summary-card" :class="{ 'mode-summary-card--active': !isPreparedMode }">
            <div class="mode-summary-card__label">STATEMENT</div>
            <p>{{ $t('templates.executionGuideStatement') }}</p>
          </div>
          <div class="mode-summary-card" :class="{ 'mode-summary-card--active': isPreparedMode }">
            <div class="mode-summary-card__label">PREPARED_STATEMENT</div>
            <p>{{ $t('templates.executionGuidePrepared') }}</p>
          </div>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dlg = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="primary" @click="save">{{ $t('common.save') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="sqlPreviewVisible"
      :title="$t('templates.previewDialogTitle', { name: sqlPreviewName })"
      width="920px"
      top="6vh"
    >
      <div class="sql-dialog-summary">{{ describeSql(sqlPreviewContent) }}</div>
      <pre class="sql-preview-full">{{ sqlPreviewContent }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Play } from 'lucide-vue-next'
import client from '../api/client'
import { API_ENDPOINTS } from '../api/endpoints'
import DebuggerDialog from '../components/DebuggerDialog.vue'
import { getSqlPreview } from '../utils/sqlPreview'

const { t } = useI18n()
const templates = ref([])
const loading = ref(false)
const dlg = ref(false)
const form = reactive({
  id: null,
  name: '',
  sqlText: '',
  weight: 1,
  executionMode: 'STATEMENT',
  paramJson: ''
})

const debugVisible = ref(false)
const debugSql = ref('')
const sqlPreviewVisible = ref(false)
const sqlPreviewName = ref('')
const sqlPreviewContent = ref('')

function runDebug(row) {
  debugSql.value = row.sqlText
  debugVisible.value = true
}

function previewSql(sqlText) {
  return getSqlPreview(sqlText)
}

function describeSql(sqlText) {
  const summary = getSqlPreview(sqlText)
  return summary.lineCount === 0
    ? ''
    : t('templates.previewSummary', {
        lines: summary.lineCount,
        chars: summary.charCount
      })
}

function openSqlPreview(row) {
  sqlPreviewName.value = row.name
  sqlPreviewContent.value = row.sqlText || ''
  sqlPreviewVisible.value = true
}

const searchKeyword = ref('')
const executionModeFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const isPreparedMode = computed(() => form.executionMode === 'PREPARED_STATEMENT')

async function load() {
  loading.value = true
  try {
    const { data } = await client.get(API_ENDPOINTS.TEMPLATES, {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
        keyword: searchKeyword.value || undefined,
        executionMode: executionModeFilter.value || undefined
      }
    })

    if (data.content) {
      templates.value = data.content
      total.value = data.totalElements || data.content.length
    } else if (Array.isArray(data)) {
      templates.value = data
      total.value = data.length
    } else {
      templates.value = []
      total.value = 0
    }
  } catch (e) {
    ElMessage.error('加载模板失败: ' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  load()
}

function handleSizeChange() {
  currentPage.value = 1
  load()
}

function handleCurrentChange() {
  load()
}

function openCreate() {
  Object.assign(form, {
    id: null,
    name: '',
    sqlText: '',
    weight: 1,
    executionMode: 'STATEMENT',
    paramJson: ''
  })
  dlg.value = true
}

function edit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    sqlText: row.sqlText,
    weight: row.weight,
    executionMode: row.executionMode || 'STATEMENT',
    paramJson: row.paramJson || ''
  })
  dlg.value = true
}

async function save() {
  if (!form.name || !form.sqlText) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    const payload = {
      ...form,
      paramJson: form.executionMode === 'PREPARED_STATEMENT' ? (form.paramJson || '[]') : null
    }
    if (form.id) await client.put(`${API_ENDPOINTS.TEMPLATES}/${form.id}`, payload)
    else await client.post(API_ENDPOINTS.TEMPLATES, payload)
    ElMessage.success('模板已同步')
    dlg.value = false
    await load()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定移除全局 SQL 模板「${row.name}」吗？这可能会影响未绑定具体测试集的压测任务。`,
      '移除确认',
      {
        confirmButtonText: '立即移除',
        cancelButtonText: '保留模板',
        type: 'warning'
      }
    )
    await client.delete(`${API_ENDPOINTS.TEMPLATES}/${row.id}`)
    ElMessage.success('模板已从库中移除')
    await load()
  } catch { /* cancel */ }
}

onMounted(load)
</script>

<style scoped>
.templates-view-container {
  max-width: 100%;
}

.page-header {
  margin-bottom: 16px;
}

.flow-callout {
  margin-bottom: 12px;
  padding: 8px 12px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: linear-gradient(135deg, #f8fbff 0%, #eef6ff 100%);
  color: var(--color-body);
}

.flow-callout__title {
  font-size: var(--font-size-subtitle);
  font-weight: 700;
  color: #1e3a8a;
  margin-bottom: 4px;
}

.mode-chip {
  padding: 2px 6px;
  border-radius: 4px;
  background: #e0f2fe;
  color: #075985;
  font-size: var(--font-size-small);
  font-weight: 700;
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

.toolbar-input {
  width: 200px;
}

.pagination-container {
  padding: 8px 16px;
}

.template-name {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 700;
  color: #1e293b;
}

.sql-preview-text {
  flex: 1;
  min-width: 0;
  font-family: var(--font-family-mono);
  font-size: var(--font-size-mono);
  padding: 0;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sql-preview-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.sql-preview-link {
  padding: 0;
  min-height: auto;
  flex: none;
}

.sql-dialog-summary {
  margin-bottom: 10px;
  font-size: 12px;
  color: #64748b;
}

.sql-preview-full {
  margin: 0;
  max-height: 60vh;
  overflow: auto;
  padding: 12px;
  border-radius: 10px;
  background: #0f172a;
  color: #e2e8f0;
  font-family: var(--font-family-mono);
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.mini-code {
  font-family: var(--font-family-mono);
  font-size: var(--font-size-mono);
}
</style>
