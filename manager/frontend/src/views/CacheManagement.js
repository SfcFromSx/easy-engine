import { computed, defineComponent, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Database, Pencil, Plus, RefreshCw, Trash2 } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import { API_ENDPOINTS, CACHE_KEY_BY_KEY } from '../api/endpoints'

export default defineComponent({
  name: 'CacheManagementView',
  components: {
    Database,
    Pencil,
    Plus,
    RefreshCw,
    Trash2
  },
  setup() {
    const { t } = useI18n()
    const cacheInfo = reactive({ totalSizeBytes: 0, keyCount: 0 })
    const cacheKeys = ref([])
    const infoLoading = ref(false)
    const keysLoading = ref(false)
    const detailLoading = ref(false)
    const submitting = ref(false)
    const dialogVisible = ref(false)
    const dialogMode = ref('create')
    const currentPage = ref(1)
    const pageSize = ref(20)
    const form = reactive({
      key: '',
      value: '',
      ttlSeconds: 300
    })

    const totalSizeMB = computed(() => (cacheInfo.totalSizeBytes / (1024 * 1024)).toFixed(2))
    const dialogTitle = computed(() => (
      dialogMode.value === 'create' ? t('cache.createTitle') : t('cache.editTitle')
    ))
    const valuePlaceholder = computed(() => '{\n  "columnMetas": [],\n  "results": []\n}')

    const loadCacheInfo = async () => {
      infoLoading.value = true
      try {
        const response = await client.get(API_ENDPOINTS.CACHE_INFO)
        Object.assign(cacheInfo, response.data)
      } catch (error) {
        ElMessage.error(t('cache.loadInfoError'))
        console.error('Failed to load cache info:', error)
      } finally {
        infoLoading.value = false
      }
    }

    const loadCacheKeys = async () => {
      keysLoading.value = true
      try {
        const offset = (currentPage.value - 1) * pageSize.value
        const response = await client.get(API_ENDPOINTS.CACHE_KEYS, {
          params: { offset, limit: pageSize.value }
        })
        cacheKeys.value = response.data
      } catch (error) {
        ElMessage.error(t('cache.loadKeysError'))
        console.error('Failed to load cache keys:', error)
      } finally {
        keysLoading.value = false
      }
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
        if (!key.startsWith('kylin_cache:') || key.includes('/') || /\s/.test(key)) {
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
        await refresh()
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
        await refresh()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(t('cache.deleteError'))
          console.error('Failed to delete cache key:', error)
        }
      }
    }

    const refresh = async () => {
      await Promise.all([loadCacheInfo(), loadCacheKeys()])
    }

    const handlePageChange = (page) => {
      currentPage.value = page
      loadCacheKeys()
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
      refresh()
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
      currentPage,
      pageSize,
      form,
      totalSizeMB,
      openCreate,
      openEdit,
      deleteKey,
      save,
      refresh,
      handlePageChange,
      formatBytes,
      formatTTL,
      estimateValueSize,
      RefreshCw,
      Plus,
      Pencil,
      Trash2
    }
  }
})
