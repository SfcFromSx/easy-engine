import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, UploadCloud } from 'lucide-vue-next'
import client from '../api/client'
import {
  API_ENDPOINTS,
  TEST_SET_ITEMS,
  TEST_SET_ITEM_BY_ID,
  TEST_SET_ITEMS_COPY_TEMPLATES,
  TEST_SET_ITEMS_REORDER
} from '../api/endpoints'
import CodeBlock from '../components/CodeBlock.vue'
import { filterTestSets, formatParamJson } from '../utils/benchmarkViewHelpers'
import { getSqlPreview } from '../utils/sqlPreview'

export default {
  __name: 'TestSets',
  components: {
    Plus,
    UploadCloud,
    CodeBlock
  },
  setup(__props, { expose: __expose }) {
    __expose()

    const { t } = useI18n()
    const testSets = ref([])
    const loading = ref(false)
    const dlg = ref(false)
    const savingSet = ref(false)
    const drawerVisible = ref(false)
    const itemsLoading = ref(false)
    const items = ref([])
    const activeSet = ref(null)
    const keyword = ref('')
    const sourceFilter = ref('all')
    const createMode = ref('empty')
    const createFile = ref(null)
    const createFileName = ref('')
    const itemDlg = ref(false)
    const itemSaving = ref(false)
    const templateDlg = ref(false)
    const templateLoading = ref(false)
    const copyingTemplates = ref(false)
    const availableTemplates = ref([])
    const selectedTemplateIds = ref([])
    const templateKeyword = ref('')
    const templateModeFilter = ref('')

    const form = reactive({ id: null, name: '', description: '' })
    const itemForm = reactive({
      id: null,
      label: '',
      sqlText: '',
      weight: 1,
      executionMode: 'STATEMENT',
      paramJson: ''
    })

    const filteredTestSets = computed(() => filterTestSets(testSets.value, keyword.value, sourceFilter.value))
    const isEditingSet = computed(() => Boolean(form.id))
    const isPreparedItemMode = computed(() => itemForm.executionMode === 'PREPARED_STATEMENT')
    const selectedTemplateCount = computed(() => selectedTemplateIds.value.length)

    async function load() {
      loading.value = true
      try {
        const { data } = await client.get(API_ENDPOINTS.TEST_SETS)
        testSets.value = data
        if (activeSet.value?.id) {
          const matched = data.find((row) => row.id === activeSet.value.id)
          if (matched) {
            activeSet.value = matched
          }
        }
      } finally {
        loading.value = false
      }
    }

    function previewSql(sqlText) {
      return getSqlPreview(sqlText).previewText
    }

    function resetCreateFile() {
      createFile.value = null
      createFileName.value = ''
    }

    function handleCreateFileChange(file) {
      createFile.value = file?.raw || file || null
      createFileName.value = file?.name || file?.raw?.name || ''
    }

    function clearCreateFile() {
      resetCreateFile()
    }

    async function fetchItems(testSetId) {
      itemsLoading.value = true
      try {
        const { data } = await client.get(TEST_SET_ITEMS(testSetId))
        items.value = data
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('testSets.loadItemsFailed'))
      } finally {
        itemsLoading.value = false
      }
    }

    async function manageItems(row) {
      activeSet.value = row
      drawerVisible.value = true
      await fetchItems(row.id)
    }

    async function refreshActiveSet() {
      if (!activeSet.value?.id) {
        return
      }
      await Promise.all([load(), fetchItems(activeSet.value.id)])
    }

    function openCreate() {
      Object.assign(form, { id: null, name: '', description: '' })
      createMode.value = 'empty'
      resetCreateFile()
      dlg.value = true
    }

    function edit(row) {
      Object.assign(form, { id: row.id, name: row.name, description: row.description || '' })
      createMode.value = 'empty'
      resetCreateFile()
      dlg.value = true
    }

    async function saveTestSet() {
      if (!form.name?.trim()) {
        ElMessage.warning(t('testSets.nameRequired'))
        return
      }
      if (!isEditingSet.value && createMode.value === 'upload' && !createFile.value) {
        ElMessage.warning(t('testSets.uploadFileRequired'))
        return
      }

      const editingExisting = isEditingSet.value
      let createdSet = null
      savingSet.value = true
      try {
        if (editingExisting) {
          const { data } = await client.put(`${API_ENDPOINTS.TEST_SETS}/${form.id}`, form)
          createdSet = data
          ElMessage.success(t('testSets.saveSuccess'))
        } else if (createMode.value === 'upload') {
          const fd = new FormData()
          fd.append('file', createFile.value)
          fd.append('name', form.name.trim())
          if (form.description?.trim()) {
            fd.append('description', form.description.trim())
          }
          const { data } = await client.post(`${API_ENDPOINTS.TEST_SETS}/upload`, fd)
          createdSet = data.testSet
          ElMessage.success(t('testSets.uploadSuccess', { name: data.testSet.name, count: data.itemCount }))
        } else {
          const { data } = await client.post(API_ENDPOINTS.TEST_SETS, form)
          createdSet = data
          ElMessage.success(t('testSets.emptyCreateSuccess', { name: data.name }))
        }

        dlg.value = false
        await load()

        if (!editingExisting && createMode.value === 'empty' && createdSet?.id) {
          const latest = testSets.value.find((row) => row.id === createdSet.id) || createdSet
          await manageItems(latest)
        }
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('testSets.saveFailed'))
      } finally {
        savingSet.value = false
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
        if (activeSet.value?.id === row.id) {
          drawerVisible.value = false
          activeSet.value = null
          items.value = []
        }
        ElMessage.success(t('testSets.removeSuccess'))
        await load()
      } catch { /* cancel */ }
    }

    function resetItemForm() {
      Object.assign(itemForm, {
        id: null,
        label: '',
        sqlText: '',
        weight: 1,
        executionMode: 'STATEMENT',
        paramJson: ''
      })
    }

    function openAddItem() {
      resetItemForm()
      itemDlg.value = true
    }

    function editItem(item) {
      Object.assign(itemForm, {
        id: item.id,
        label: item.label || '',
        sqlText: item.sqlText || '',
        weight: item.weight || 1,
        executionMode: item.executionMode || 'STATEMENT',
        paramJson: item.paramJson || ''
      })
      itemDlg.value = true
    }

    async function saveItem() {
      if (!activeSet.value?.id) {
        return
      }
      if (!itemForm.sqlText?.trim()) {
        ElMessage.warning(t('testSets.itemSqlRequired'))
        return
      }
      itemSaving.value = true
      try {
        const payload = {
          label: itemForm.label,
          sqlText: itemForm.sqlText,
          weight: itemForm.weight || 1,
          executionMode: itemForm.executionMode,
          paramJson: isPreparedItemMode.value ? (itemForm.paramJson || '') : null
        }
        if (itemForm.id) {
          await client.put(TEST_SET_ITEM_BY_ID(activeSet.value.id, itemForm.id), payload)
        } else {
          await client.post(TEST_SET_ITEMS(activeSet.value.id), payload)
        }
        ElMessage.success(t('testSets.itemSaveSuccess'))
        itemDlg.value = false
        await refreshActiveSet()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('testSets.itemSaveFailed'))
      } finally {
        itemSaving.value = false
      }
    }

    async function removeItem(item) {
      if (!activeSet.value?.id) {
        return
      }
      try {
        await ElMessageBox.confirm(
          t('testSets.itemDeleteConfirm', { label: item.label || `#${item.sortOrder}` }),
          t('testSets.itemDeleteTitle'),
          {
            confirmButtonText: t('testSets.itemDeleteConfirmBtn'),
            cancelButtonText: t('common.cancel'),
            type: 'warning'
          }
        )
        await client.delete(TEST_SET_ITEM_BY_ID(activeSet.value.id, item.id))
        ElMessage.success(t('testSets.itemDeleteSuccess'))
        await refreshActiveSet()
      } catch { /* cancel */ }
    }

    async function moveItem(item, offset) {
      if (!activeSet.value?.id) {
        return
      }
      const currentIndex = items.value.findIndex((entry) => entry.id === item.id)
      const targetIndex = currentIndex + offset
      if (currentIndex < 0 || targetIndex < 0 || targetIndex >= items.value.length) {
        return
      }

      const reorderedIds = items.value.map((entry) => entry.id)
      const swapped = reorderedIds[currentIndex]
      reorderedIds[currentIndex] = reorderedIds[targetIndex]
      reorderedIds[targetIndex] = swapped

      try {
        await client.put(TEST_SET_ITEMS_REORDER(activeSet.value.id), { itemIds: reorderedIds })
        await refreshActiveSet()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('testSets.reorderFailed'))
      }
    }

    async function loadTemplates() {
      templateLoading.value = true
      try {
        const { data } = await client.get(API_ENDPOINTS.TEMPLATES, {
          params: {
            page: 0,
            size: 200,
            keyword: templateKeyword.value || undefined,
            executionMode: templateModeFilter.value || undefined
          }
        })
        availableTemplates.value = data.content || data || []
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('testSets.templateLoadFailed'))
      } finally {
        templateLoading.value = false
      }
    }

    async function openTemplatePicker() {
      selectedTemplateIds.value = []
      templateKeyword.value = ''
      templateModeFilter.value = ''
      templateDlg.value = true
      await loadTemplates()
    }

    function toggleTemplateSelection(templateId, checked) {
      if (checked) {
        if (!selectedTemplateIds.value.includes(templateId)) {
          selectedTemplateIds.value = selectedTemplateIds.value.concat(templateId)
        }
        return
      }
      selectedTemplateIds.value = selectedTemplateIds.value.filter((id) => id !== templateId)
    }

    async function copySelectedTemplates() {
      if (!activeSet.value?.id) {
        return
      }
      const orderedTemplateIds = availableTemplates.value
        .filter((template) => selectedTemplateIds.value.includes(template.id))
        .map((template) => template.id)

      if (orderedTemplateIds.length === 0) {
        ElMessage.warning(t('testSets.templatePickRequired'))
        return
      }

      copyingTemplates.value = true
      try {
        await client.post(TEST_SET_ITEMS_COPY_TEMPLATES(activeSet.value.id), { templateIds: orderedTemplateIds })
        templateDlg.value = false
        ElMessage.success(t('testSets.templateCopySuccess', { count: orderedTemplateIds.length }))
        await refreshActiveSet()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('testSets.templateCopyFailed'))
      } finally {
        copyingTemplates.value = false
      }
    }

    onMounted(load)

    const __returned__ = {
      t,
      testSets,
      loading,
      dlg,
      savingSet,
      drawerVisible,
      itemsLoading,
      items,
      activeSet,
      keyword,
      sourceFilter,
      createMode,
      createFile,
      createFileName,
      itemDlg,
      itemSaving,
      templateDlg,
      templateLoading,
      copyingTemplates,
      availableTemplates,
      selectedTemplateIds,
      templateKeyword,
      templateModeFilter,
      form,
      itemForm,
      filteredTestSets,
      isEditingSet,
      isPreparedItemMode,
      selectedTemplateCount,
      load,
      previewSql,
      handleCreateFileChange,
      clearCreateFile,
      fetchItems,
      manageItems,
      refreshActiveSet,
      openCreate,
      edit,
      saveTestSet,
      remove,
      resetItemForm,
      openAddItem,
      editItem,
      saveItem,
      removeItem,
      moveItem,
      loadTemplates,
      openTemplatePicker,
      toggleTemplateSelection,
      copySelectedTemplates,
      computed,
      onMounted,
      reactive,
      ref,
      get useI18n() { return useI18n },
      get ElMessage() { return ElMessage },
      get ElMessageBox() { return ElMessageBox },
      get Plus() { return Plus },
      get UploadCloud() { return UploadCloud },
      get client() { return client },
      get API_ENDPOINTS() { return API_ENDPOINTS },
      get TEST_SET_ITEMS() { return TEST_SET_ITEMS },
      get TEST_SET_ITEM_BY_ID() { return TEST_SET_ITEM_BY_ID },
      get TEST_SET_ITEMS_COPY_TEMPLATES() { return TEST_SET_ITEMS_COPY_TEMPLATES },
      get TEST_SET_ITEMS_REORDER() { return TEST_SET_ITEMS_REORDER },
      CodeBlock,
      get filterTestSets() { return filterTestSets },
      get formatParamJson() { return formatParamJson },
      get getSqlPreview() { return getSqlPreview }
    }
    return __returned__
  }
}
