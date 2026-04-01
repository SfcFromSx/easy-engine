import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Play } from 'lucide-vue-next'
import client from '../api/client'
import { API_ENDPOINTS } from '../api/endpoints'
import DebuggerDialog from '../components/DebuggerDialog.vue'
import { getSqlPreview } from '../utils/sqlPreview'


export default {
  __name: 'Templates',
  components: {
    Plus,
    Search,
    Play,
    DebuggerDialog
  },
  setup(__props, { expose: __expose }) {
  __expose();

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

const __returned__ = { t, templates, loading, dlg, form, debugVisible, debugSql, sqlPreviewVisible, sqlPreviewName, sqlPreviewContent, runDebug, previewSql, describeSql, openSqlPreview, searchKeyword, executionModeFilter, currentPage, pageSize, total, isPreparedMode, load, handleSearch, handleSizeChange, handleCurrentChange, openCreate, edit, save, remove, computed, onMounted, reactive, ref, get useI18n() { return useI18n }, get ElMessage() { return ElMessage }, get ElMessageBox() { return ElMessageBox }, get Plus() { return Plus }, get Search() { return Search }, get Play() { return Play }, get client() { return client }, get API_ENDPOINTS() { return API_ENDPOINTS }, DebuggerDialog, get getSqlPreview() { return getSqlPreview } }
return __returned__
}

}
