import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Play, UploadCloud } from 'lucide-vue-next'
import client from '../api/client'
import { API_ENDPOINTS, SQL_LIB_BY_ID, SQL_LIB_UPLOAD } from '../api/endpoints'
import DebuggerDialog from '../components/DebuggerDialog.vue'
import { getSqlPreview } from '../utils/sqlPreview'

function defaultForm() {
  return {
    id: null,
    name: '',
    sqlText: '',
    description: '',
    weight: 1,
    executionMode: 'STATEMENT',
    paramJson: '',
    sourceFilename: '',
    uploadedAt: null
  }
}

export default {
  __name: 'Templates',
  components: {
    Plus,
    Search,
    Play,
    UploadCloud,
    DebuggerDialog
  },
  setup(__props, { expose: __expose }) {
    __expose()

    const { t } = useI18n()
    const sqlLibRows = ref([])
    const loading = ref(false)
    const detailLoading = ref(false)
    const saving = ref(false)
    const uploadLoading = ref(false)
    const searchKeyword = ref('')
    const executionModeFilter = ref('')
    const sourceFilenameFilter = ref('')
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)
    const selectedId = ref(null)
    const uploadFile = ref(null)
    const uploadFileName = ref('')
    const form = reactive(defaultForm())

    const debugVisible = ref(false)
    const debugSql = ref('')
    const sqlPreviewVisible = ref(false)
    const sqlPreviewName = ref('')
    const sqlPreviewContent = ref('')

    const isPreparedMode = computed(() => form.executionMode === 'PREPARED_STATEMENT')
    const hasSelectedRow = computed(() => Boolean(form.id))

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

    function resetForm() {
      Object.assign(form, defaultForm())
    }

    function formatUploadedAt(value) {
      if (!value) {
        return '—'
      }
      try {
        return new Date(value).toLocaleString()
      } catch {
        return value
      }
    }

    async function load() {
      loading.value = true
      try {
        const { data } = await client.get(API_ENDPOINTS.SQL_LIB, {
          params: {
            page: currentPage.value - 1,
            size: pageSize.value,
            keyword: searchKeyword.value || undefined,
            executionMode: executionModeFilter.value || undefined,
            sourceFilename: sourceFilenameFilter.value || undefined
          }
        })

        sqlLibRows.value = data.content || data || []
        total.value = data.totalElements || sqlLibRows.value.length

        if (!sqlLibRows.value.length) {
          selectedId.value = null
          if (!hasSelectedRow.value) {
            resetForm()
          }
          return
        }

        const preferred = selectedId.value && sqlLibRows.value.find((row) => row.id === selectedId.value)
        if (preferred) {
          return
        }

        await loadDetail(sqlLibRows.value[0].id)
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('templates.loadFailed'))
      } finally {
        loading.value = false
      }
    }

    async function loadDetail(id) {
      if (!id) {
        resetForm()
        selectedId.value = null
        return
      }
      detailLoading.value = true
      try {
        const { data } = await client.get(SQL_LIB_BY_ID(id))
        selectedId.value = data.id
        Object.assign(form, {
          id: data.id,
          name: data.name || '',
          sqlText: data.sqlText || '',
          description: data.description || '',
          weight: data.weight || 1,
          executionMode: data.executionMode || 'STATEMENT',
          paramJson: data.paramJson || '',
          sourceFilename: data.sourceFilename || '',
          uploadedAt: data.uploadedAt || null
        })
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('templates.loadDetailFailed'))
      } finally {
        detailLoading.value = false
      }
    }

    function selectRow(row) {
      if (!row?.id) {
        return
      }
      loadDetail(row.id)
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
      selectedId.value = null
      resetForm()
    }

    async function save() {
      if (!form.name?.trim() || !form.sqlText?.trim()) {
        ElMessage.warning(t('templates.requiredFields'))
        return
      }
      saving.value = true
      try {
        const payload = {
          name: form.name.trim(),
          sqlText: form.sqlText.trim(),
          description: form.description?.trim() || null,
          weight: form.weight || 1,
          executionMode: form.executionMode,
          paramJson: form.executionMode === 'PREPARED_STATEMENT' ? (form.paramJson || '') : null
        }
        const { data } = form.id
          ? await client.put(SQL_LIB_BY_ID(form.id), payload)
          : await client.post(API_ENDPOINTS.SQL_LIB, payload)

        ElMessage.success(t('templates.saveSuccess'))
        await load()
        await loadDetail(data.id)
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('templates.saveFailed'))
      } finally {
        saving.value = false
      }
    }

    async function remove(row = form) {
      if (!row?.id) {
        return
      }
      try {
        await ElMessageBox.confirm(
          t('templates.removeConfirm', { name: row.name }),
          t('templates.removeTitle'),
          {
            confirmButtonText: t('common.delete'),
            cancelButtonText: t('common.cancel'),
            type: 'warning'
          }
        )
        await client.delete(SQL_LIB_BY_ID(row.id))
        ElMessage.success(t('templates.removeSuccess'))
        resetForm()
        selectedId.value = null
        await load()
      } catch (e) {
        if (e !== 'cancel' && e !== 'close') {
          ElMessage.error(e.response?.data?.message || t('templates.removeFailed'))
        }
      }
    }

    function handleUploadFileChange(file) {
      uploadFile.value = file?.raw || file || null
      uploadFileName.value = file?.name || file?.raw?.name || ''
    }

    function clearUploadFile() {
      uploadFile.value = null
      uploadFileName.value = ''
    }

    async function uploadSelectedFile() {
      if (!uploadFile.value) {
        ElMessage.warning(t('templates.uploadFileRequired'))
        return
      }
      uploadLoading.value = true
      try {
        const formData = new FormData()
        formData.append('file', uploadFile.value)
        const { data } = await client.post(SQL_LIB_UPLOAD, formData)
        ElMessage.success(t('templates.uploadSuccess', { count: data.count }))
        clearUploadFile()
        currentPage.value = 1
        await load()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('templates.uploadFailed'))
      } finally {
        uploadLoading.value = false
      }
    }

    onMounted(load)

    const __returned__ = {
      t,
      sqlLibRows,
      loading,
      detailLoading,
      saving,
      uploadLoading,
      searchKeyword,
      executionModeFilter,
      sourceFilenameFilter,
      currentPage,
      pageSize,
      total,
      selectedId,
      uploadFile,
      uploadFileName,
      form,
      debugVisible,
      debugSql,
      sqlPreviewVisible,
      sqlPreviewName,
      sqlPreviewContent,
      isPreparedMode,
      hasSelectedRow,
      runDebug,
      previewSql,
      describeSql,
      openSqlPreview,
      formatUploadedAt,
      load,
      loadDetail,
      selectRow,
      handleSearch,
      handleSizeChange,
      handleCurrentChange,
      openCreate,
      save,
      remove,
      handleUploadFileChange,
      clearUploadFile,
      uploadSelectedFile,
      computed,
      onMounted,
      reactive,
      ref,
      get useI18n() { return useI18n },
      get ElMessage() { return ElMessage },
      get ElMessageBox() { return ElMessageBox },
      get Plus() { return Plus },
      get Search() { return Search },
      get Play() { return Play },
      get UploadCloud() { return UploadCloud },
      get client() { return client },
      get API_ENDPOINTS() { return API_ENDPOINTS },
      get SQL_LIB_BY_ID() { return SQL_LIB_BY_ID },
      get SQL_LIB_UPLOAD() { return SQL_LIB_UPLOAD },
      DebuggerDialog,
      get getSqlPreview() { return getSqlPreview }
    }
    return __returned__
  }
}
