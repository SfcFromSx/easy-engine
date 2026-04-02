import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Plus } from 'lucide-vue-next'
import client from '../api/client'
import {
  API_ENDPOINTS,
  TEST_SET_ITEMS,
  TEST_SET_ITEM_BY_ID,
  TEST_SET_ITEMS_ADD_SQL_LIB,
  TEST_SET_ITEMS_REORDER
} from '../api/endpoints'
import CodeBlock from '../components/CodeBlock.vue'
import { filterTestSets, formatParamJson } from '../utils/benchmarkViewHelpers'

export default {
  __name: 'TestSets',
  components: {
    Plus,
    CodeBlock
  },
  setup(__props, { expose: __expose }) {
    __expose()

    const router = useRouter()
    const { t } = useI18n()
    const testSets = ref([])
    const loading = ref(false)
    const dlg = ref(false)
    const savingSet = ref(false)
    const managerVisible = ref(false)
    const pickerVisible = ref(false)
    const itemsLoading = ref(false)
    const items = ref([])
    const itemsTotal = ref(0)
    const itemsPage = ref(1)
    const itemsPageSize = ref(20)
    const itemsKeyword = ref('')
    const activeSet = ref(null)
    const selectedItemId = ref(null)
    const keyword = ref('')
    const sourceFilter = ref('all')

    const sqlLibLoading = ref(false)
    const addingSqlLib = ref(false)
    const availableSqlLib = ref([])
    const selectedSqlLibIds = ref([])
    const sqlLibKeyword = ref('')
    const sqlLibModeFilter = ref('')
    const sqlLibPage = ref(1)
    const sqlLibPageSize = ref(20)
    const sqlLibTotal = ref(0)

    const form = reactive({ id: null, name: '', description: '' })

    const filteredTestSets = computed(() => filterTestSets(testSets.value, keyword.value, sourceFilter.value))
    const isEditingSet = computed(() => Boolean(form.id))
    const selectedItem = computed(() => items.value.find((item) => item.id === selectedItemId.value) || null)
    const selectedSqlLibCount = computed(() => selectedSqlLibIds.value.length)

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

    async function fetchItems(testSetId) {
      itemsLoading.value = true
      try {
        const { data } = await client.get(TEST_SET_ITEMS(testSetId), {
          params: {
            page: itemsPage.value - 1,
            size: itemsPageSize.value,
            keyword: itemsKeyword.value || undefined
          }
        })
        items.value = data.content || []
        itemsTotal.value = data.totalElements || items.value.length
        if (!items.value.length) {
          selectedItemId.value = null
          return
        }
        const stillVisible = items.value.find((item) => item.id === selectedItemId.value)
        if (!stillVisible) {
          selectedItemId.value = items.value[0].id
        }
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('testSets.loadItemsFailed'))
      } finally {
        itemsLoading.value = false
      }
    }

    async function manageItems(row) {
      activeSet.value = row
      managerVisible.value = true
      itemsPage.value = 1
      itemsKeyword.value = ''
      selectedItemId.value = null
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
      dlg.value = true
    }

    function edit(row) {
      Object.assign(form, { id: row.id, name: row.name, description: row.description || '' })
      dlg.value = true
    }

    async function saveTestSet() {
      if (!form.name?.trim()) {
        ElMessage.warning(t('testSets.nameRequired'))
        return
      }
      const editingExisting = isEditingSet.value
      savingSet.value = true
      try {
        const payload = {
          id: form.id,
          name: form.name.trim(),
          description: form.description?.trim() || ''
        }
        const { data } = editingExisting
          ? await client.put(`${API_ENDPOINTS.TEST_SETS}/${form.id}`, payload)
          : await client.post(API_ENDPOINTS.TEST_SETS, payload)

        ElMessage.success(editingExisting ? t('testSets.saveSuccess') : t('testSets.emptyCreateSuccess', { name: data.name }))
        dlg.value = false
        await load()
        if (!editingExisting && data?.id) {
          const latest = testSets.value.find((row) => row.id === data.id) || data
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
          t('testSets.removeConfirm', { name: row.name }),
          t('testSets.removeTitle'),
          {
            confirmButtonText: t('common.delete'),
            cancelButtonText: t('common.cancel'),
            type: 'warning'
          }
        )
        await client.delete(`${API_ENDPOINTS.TEST_SETS}/${row.id}`)
        if (activeSet.value?.id === row.id) {
          managerVisible.value = false
          activeSet.value = null
          items.value = []
          selectedItemId.value = null
        }
        ElMessage.success(t('testSets.removeSuccess'))
        await load()
      } catch (e) {
        if (e !== 'cancel' && e !== 'close') {
          ElMessage.error(e.response?.data?.message || t('testSets.removeFailed'))
        }
      }
    }

    function selectItem(item) {
      selectedItemId.value = item.id
    }

    function handleItemsSearch() {
      itemsPage.value = 1
      fetchItems(activeSet.value.id)
    }

    function handleItemsPageChange() {
      fetchItems(activeSet.value.id)
    }

    async function removeItem(item) {
      if (!activeSet.value?.id) {
        return
      }
      try {
        await ElMessageBox.confirm(
          t('testSets.itemDeleteConfirm', { label: item.name || `#${item.sortOrder}` }),
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
      } catch (e) {
        if (e !== 'cancel' && e !== 'close') {
          ElMessage.error(e.response?.data?.message || t('testSets.itemDeleteFailed'))
        }
      }
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

    async function loadSqlLib() {
      sqlLibLoading.value = true
      try {
        const { data } = await client.get(API_ENDPOINTS.SQL_LIB, {
          params: {
            page: sqlLibPage.value - 1,
            size: sqlLibPageSize.value,
            keyword: sqlLibKeyword.value || undefined,
            executionMode: sqlLibModeFilter.value || undefined
          }
        })
        availableSqlLib.value = data.content || []
        sqlLibTotal.value = data.totalElements || availableSqlLib.value.length
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('testSets.templateLoadFailed'))
      } finally {
        sqlLibLoading.value = false
      }
    }

    async function openSqlLibPicker() {
      selectedSqlLibIds.value = []
      sqlLibKeyword.value = ''
      sqlLibModeFilter.value = ''
      sqlLibPage.value = 1
      pickerVisible.value = true
      await loadSqlLib()
    }

    function toggleSqlLibSelection(sqlLibId, checked) {
      if (checked) {
        if (!selectedSqlLibIds.value.includes(sqlLibId)) {
          selectedSqlLibIds.value = selectedSqlLibIds.value.concat(sqlLibId)
        }
        return
      }
      selectedSqlLibIds.value = selectedSqlLibIds.value.filter((id) => id !== sqlLibId)
    }

    async function addSelectedSqlLib() {
      if (!activeSet.value?.id) {
        return
      }
      const orderedSqlLibIds = availableSqlLib.value
        .filter((row) => selectedSqlLibIds.value.includes(row.id))
        .map((row) => row.id)

      if (!orderedSqlLibIds.length) {
        ElMessage.warning(t('testSets.templatePickRequired'))
        return
      }

      addingSqlLib.value = true
      try {
        await client.post(TEST_SET_ITEMS_ADD_SQL_LIB(activeSet.value.id), { sqlLibIds: orderedSqlLibIds })
        pickerVisible.value = false
        ElMessage.success(t('testSets.templateCopySuccess', { count: orderedSqlLibIds.length }))
        await refreshActiveSet()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || t('testSets.templateCopyFailed'))
      } finally {
        addingSqlLib.value = false
      }
    }

    function openSelectedInSqlLib() {
      router.push('/sql-lib')
    }

    onMounted(load)

    const __returned__ = {
      t,
      testSets,
      loading,
      dlg,
      savingSet,
      managerVisible,
      pickerVisible,
      itemsLoading,
      items,
      itemsTotal,
      itemsPage,
      itemsPageSize,
      itemsKeyword,
      activeSet,
      selectedItemId,
      keyword,
      sourceFilter,
      sqlLibLoading,
      addingSqlLib,
      availableSqlLib,
      selectedSqlLibIds,
      sqlLibKeyword,
      sqlLibModeFilter,
      sqlLibPage,
      sqlLibPageSize,
      sqlLibTotal,
      form,
      filteredTestSets,
      isEditingSet,
      selectedItem,
      selectedSqlLibCount,
      load,
      fetchItems,
      manageItems,
      refreshActiveSet,
      openCreate,
      edit,
      saveTestSet,
      remove,
      selectItem,
      handleItemsSearch,
      handleItemsPageChange,
      removeItem,
      moveItem,
      loadSqlLib,
      openSqlLibPicker,
      toggleSqlLibSelection,
      addSelectedSqlLib,
      openSelectedInSqlLib,
      formatParamJson,
      computed,
      onMounted,
      reactive,
      ref,
      get useI18n() { return useI18n },
      get ElMessage() { return ElMessage },
      get ElMessageBox() { return ElMessageBox },
      get useRouter() { return useRouter },
      get Plus() { return Plus },
      get client() { return client },
      get API_ENDPOINTS() { return API_ENDPOINTS },
      get TEST_SET_ITEMS() { return TEST_SET_ITEMS },
      get TEST_SET_ITEM_BY_ID() { return TEST_SET_ITEM_BY_ID },
      get TEST_SET_ITEMS_ADD_SQL_LIB() { return TEST_SET_ITEMS_ADD_SQL_LIB },
      get TEST_SET_ITEMS_REORDER() { return TEST_SET_ITEMS_REORDER },
      CodeBlock,
      get filterTestSets() { return filterTestSets }
    }
    return __returned__
  }
}
