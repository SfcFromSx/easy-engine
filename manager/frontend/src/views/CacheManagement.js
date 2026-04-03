import { computed, defineComponent, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Database, Pencil, Plus, RefreshCw, Search, Trash2 } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import { API_ENDPOINTS, CACHE_KEY_BY_KEY } from '../api/endpoints'

const DEFAULT_MANAGED_PREFIX = 'kylin_cache:'

export default defineComponent({
  name: 'CacheManagementView',
  components: {
    Database,
    Pencil,
    Plus,
    RefreshCw,
    Search,
    Trash2
  },
  setup() {
    const { t } = useI18n()
    const cacheInfo = reactive({
      managedKeyPrefix: DEFAULT_MANAGED_PREFIX,
      exactSummaryAvailable: false,
      summaryMessage: ''
    })
    const cacheKeys = ref([])
    const infoLoading = ref(false)
    const keysLoading = ref(false)
    const detailLoading = ref(false)
    const submitting = ref(false)
    const dialogVisible = ref(false)
    const dialogMode = ref('create')
    const searchPrefix = ref(DEFAULT_MANAGED_PREFIX)
    const activePrefix = ref('')
    const currentCursor = ref('0')
    const nextCursor = ref('0')
    const hasMore = ref(false)
    const searchPerformed = ref(false)
    const cursorHistory = ref([])
    const pageSize = ref(20)
    const form = reactive({
      key: '',
      value: '',
      ttlSeconds: 300
    })

    const dialogTitle = computed(() => (
      dialogMode.value === 'create' ? t('cache.createTitle') : t('cache.editTitle')
    ))
    const valuePlaceholder = computed(() => '{\n  "columnMetas": [],\n  "results": []\n}')
    const managedPrefix = computed(() => cacheInfo.managedKeyPrefix || DEFAULT_MANAGED_PREFIX)
    const summaryMessage = computed(() => (
      cacheInfo.exactSummaryAvailable
        ? t('cache.summaryAvailable')
        : t('cache.summaryUnavailableHint', { prefix: managedPrefix.value })
    ))
    const canGoPrevious = computed(() => cursorHistory.value.length > 0)
    const emptyText = computed(() => (
      searchPerformed.value ? t('cache.searchEmpty') : t('cache.searchIdle')
    ))

    const loadCacheInfo = async () => {
      infoLoading.value = true
      try {
        const response = await client.get(API_ENDPOINTS.CACHE_INFO)
        Object.assign(cacheInfo, response.data)
        if (!searchPerformed.value) {
          searchPrefix.value = cacheInfo.managedKeyPrefix || DEFAULT_MANAGED_PREFIX
        }
      } catch (error) {
        ElMessage.error(t('cache.loadInfoError'))
        console.error('Failed to load cache info:', error)
      } finally {
        infoLoading.value = false
      }
    }

    const resetSearchState = ({ preserveInput = true } = {}) => {
      cacheKeys.value = []
      activePrefix.value = ''
      currentCursor.value = '0'
      nextCursor.value = '0'
      hasMore.value = false
      searchPerformed.value = false
      cursorHistory.value = []
      if (!preserveInput) {
        searchPrefix.value = managedPrefix.value
      }
    }

    const validateSearchPrefix = () => {
      const prefix = searchPrefix.value.trim()
      if (!prefix) {
        ElMessage.warning(t('cache.searchRequired'))
        return null
      }
      if (!prefix.startsWith(managedPrefix.value) || prefix.includes('/') || /\s/.test(prefix)) {
        ElMessage.warning(t('cache.invalidSearchPrefix'))
        return null
      }
      if (prefix === managedPrefix.value) {
        ElMessage.warning(t('cache.searchTooBroad'))
        return null
      }
      return prefix
    }

    const loadCacheKeys = async ({ prefix, cursor = '0' }) => {
      keysLoading.value = true
      try {
        const response = await client.get(API_ENDPOINTS.CACHE_KEYS, {
          params: { prefix, cursor, limit: pageSize.value }
        })
        cacheKeys.value = response.data.items || []
        activePrefix.value = response.data.queryPrefix || prefix
        currentCursor.value = cursor
        nextCursor.value = response.data.nextCursor || '0'
        hasMore.value = Boolean(response.data.hasMore)
        searchPerformed.value = true
      } catch (error) {
        ElMessage.error(error.response?.data?.message || t('cache.loadKeysError'))
        console.error('Failed to load cache keys:', error)
      } finally {
        keysLoading.value = false
      }
    }

    const submitSearch = async () => {
      const prefix = validateSearchPrefix()
      if (!prefix) {
        return
      }
      cursorHistory.value = []
      await loadCacheKeys({ prefix, cursor: '0' })
    }

    const goNext = async () => {
      if (!hasMore.value || !activePrefix.value) {
        return
      }
      cursorHistory.value.push(currentCursor.value)
      await loadCacheKeys({ prefix: activePrefix.value, cursor: nextCursor.value })
    }

    const goPrevious = async () => {
      if (!canGoPrevious.value || !activePrefix.value) {
        return
      }
      const previousCursor = cursorHistory.value.pop()
      await loadCacheKeys({ prefix: activePrefix.value, cursor: previousCursor })
    }

    const clearSearch = () => {
      resetSearchState({ preserveInput: false })
    }

    const resetForm = () => {
      form.key = ''
      form.value = ''
      form.ttlSeconds = 300
    }

    const openCreate = () => {
      dialogMode.value = 'create'
      resetForm()
      dialogVisible.value = true
    }

    const normalizeValue = (value) => {
      if (!value) {
        return ''
      }
      try {
        return JSON.stringify(JSON.parse(value), null, 2)
      } catch (_error) {
        return value
      }
    }

    const openEdit = async (row) => {
      dialogMode.value = 'edit'
      dialogVisible.value = true
      detailLoading.value = true
      resetForm()
      form.key = row.key
      try {
        const response = await client.get(CACHE_KEY_BY_KEY(row.key))
        form.key = response.data.key || row.key
        form.value = normalizeValue(response.data.value)
        form.ttlSeconds = response.data.ttlSeconds > 0 ? response.data.ttlSeconds : 300
      } catch (error) {
        dialogVisible.value = false
        ElMessage.error(t('cache.loadDetailError'))
        console.error('Failed to load cache key detail:', error)
      } finally {
        detailLoading.value = false
      }
    }

    const validateForm = () => {
      if (dialogMode.value === 'create') {
        if (!form.key.trim()) {
          ElMessage.warning(t('cache.keyRequired'))
          return false
        }
        const key = form.key.trim()
        if (!key.startsWith(managedPrefix.value) || key.includes('/') || /\s/.test(key)) {
          ElMessage.warning(t('cache.invalidKey'))
          return false
        }
      }
      if (!form.value.trim()) {
        ElMessage.warning(t('cache.valueRequired'))
        return false
      }
      try {
        JSON.parse(form.value)
      } catch (_error) {
        ElMessage.warning(t('cache.invalidJson'))
        return false
      }
      if (!Number.isFinite(Number(form.ttlSeconds)) || Number(form.ttlSeconds) <= 0) {
        ElMessage.warning(t('cache.invalidTtl'))
        return false
      }
      return true
    }

    const refreshAfterMutation = async () => {
      await loadCacheInfo()
      if (activePrefix.value) {
        cursorHistory.value = []
        await loadCacheKeys({ prefix: activePrefix.value, cursor: '0' })
      }
    }

    const save = async () => {
      if (!validateForm()) {
        return
      }
      const payload = {
        value: form.value,
        ttlSeconds: Number(form.ttlSeconds)
      }
      if (dialogMode.value === 'create') {
        payload.key = form.key.trim()
      }

      submitting.value = true
      try {
        if (dialogMode.value === 'create') {
          await client.post(API_ENDPOINTS.CACHE_KEYS, payload)
          ElMessage.success(t('cache.createSuccess'))
        } else {
          await client.put(CACHE_KEY_BY_KEY(form.key), payload)
          ElMessage.success(t('cache.updateSuccess'))
        }
        dialogVisible.value = false
        await refreshAfterMutation()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || t('cache.saveError'))
        console.error('Failed to save cache key:', error)
      } finally {
        submitting.value = false
      }
    }

    const deleteKey = async (key) => {
      try {
        await ElMessageBox.confirm(
          t('cache.deleteConfirm', { key }),
          t('cache.deleteTitle'),
          {
            confirmButtonText: t('common.confirm'),
            cancelButtonText: t('common.cancel'),
            type: 'warning'
          }
        )
        await client.delete(CACHE_KEY_BY_KEY(key))
        ElMessage.success(t('cache.deleteSuccess'))
        await refreshAfterMutation()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(t('cache.deleteError'))
          console.error('Failed to delete cache key:', error)
        }
      }
    }

    const refresh = async () => {
      await loadCacheInfo()
      if (activePrefix.value) {
        cursorHistory.value = []
        await loadCacheKeys({ prefix: activePrefix.value, cursor: '0' })
      }
    }

    const formatBytes = (bytes) => {
      if (bytes < 1024) return `${bytes} B`
      if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`
      return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
    }

    const formatTTL = (seconds) => {
      if (seconds < 0) return t('cache.noExpiry')
      if (seconds < 60) return `${seconds}s`
      if (seconds < 3600) return `${Math.floor(seconds / 60)}m`
      return `${Math.floor(seconds / 3600)}h`
    }

    const estimateValueSize = (value) => {
      if (!value) {
        return '0 B'
      }
      return formatBytes(new TextEncoder().encode(value).length)
    }

    onMounted(() => {
      loadCacheInfo()
    })

    return {
      t,
      cacheInfo,
      cacheKeys,
      infoLoading,
      keysLoading,
      detailLoading,
      submitting,
      dialogVisible,
      dialogMode,
      dialogTitle,
      valuePlaceholder,
      searchPrefix,
      activePrefix,
      hasMore,
      searchPerformed,
      canGoPrevious,
      summaryMessage,
      emptyText,
      form,
      openCreate,
      openEdit,
      deleteKey,
      save,
      refresh,
      submitSearch,
      goNext,
      goPrevious,
      clearSearch,
      formatBytes,
      formatTTL,
      estimateValueSize,
      RefreshCw,
      Plus,
      Search,
      Pencil,
      Trash2
    }
  }
})
