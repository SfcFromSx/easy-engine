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
          style="width: 300px; margin-right: 16px"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="openCreate">
          <el-icon style="margin-right: 4px"><Plus :size="16" /></el-icon>
          {{ $t('templates.addBtn') }}
        </el-button>
      </div>
    </div>
    <DebuggerDialog v-model="debugVisible" :sql="debugSql" />

    <div class="glass-card table-card">
      <el-table :data="templates" v-loading="loading" stripe size="small">
        <el-table-column prop="name" :label="$t('templates.colName')" width="180">
          <template #default="{ row }">
            <span style="font-weight: 700; color: #1e293b">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sqlText" :label="$t('templates.colSql')" min-width="420">
          <template #default="{ row }">
            <div class="sql-preview-text">{{ row.sqlText }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="weight" :label="$t('templates.colWeight')" width="80" align="center" />
        <el-table-column label="Mode" width="170">
          <template #default="{ row }">
            <el-tag size="small" :type="row.executionMode === 'PREPARED_STATEMENT' ? 'warning' : 'info'">
              {{ row.executionMode || 'STATEMENT' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Params" min-width="220" show-overflow-tooltip>
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
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="SQL" required>
          <template #label functional>
            <div style="display: flex; justify-content: space-between; width: 100%">
              <span>SQL Statement</span>
            </div>
          </template>
          <el-input
            type="textarea"
            :rows="12"
            v-model="form.sqlText"
            placeholder="Enter SQL script here..."
            style="font-family: 'Fira Code', 'Courier New', monospace; font-size: 13px"
          />
          <div class="hint">{{ $t('templates.hintWeight') }}</div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="10">
            <el-form-item label="Execution Mode">
              <el-select v-model="form.executionMode" style="width: 100%">
                <el-option label="STATEMENT" value="STATEMENT" />
                <el-option label="PREPARED_STATEMENT" value="PREPARED_STATEMENT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="14">
            <el-form-item label="Prepared Params JSON">
              <el-input
                v-model="form.paramJson"
                :disabled="form.executionMode !== 'PREPARED_STATEMENT'"
                placeholder='[{"type":"INTEGER","value":1}]'
              />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="hint">Prepared templates should provide ordered `paramJson`; statement templates may leave it empty.</div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dlg = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="primary" @click="save">{{ $t('common.save') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Play } from 'lucide-vue-next'
import client from '../api/client'
import { API_ENDPOINTS } from '../api/endpoints'
import DebuggerDialog from '../components/DebuggerDialog.vue'

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

function runDebug(row) {
  debugSql.value = row.sqlText
  debugVisible.value = true
}

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const { data } = await client.get(API_ENDPOINTS.TEMPLATES, {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
        keyword: searchKeyword.value
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
.hint { font-size: 12px; color: #94a3b8; margin-top: 4px; }
.actions {
  display: flex;
  align-items: center;
}
.pagination-container {
  padding: 16px;
  display: flex;
  justify-content: flex-end;
  background: rgba(255, 255, 255, 0.05);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.sql-preview-text {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 13px;
  line-height: 1.4;
  color: #1e293b;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 2.8em;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 8px 0;
  cursor: text;
}

.mini-code {
  display: inline-block;
  max-width: 100%;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 11px;
  background: #f8fafc;
  padding: 4px 6px;
  border-radius: 6px;
  color: #475569;
}

:deep(.el-table__row) {
  height: 48px;
}

:deep(.el-table .cell) {
  line-height: inherit;
}
</style>
