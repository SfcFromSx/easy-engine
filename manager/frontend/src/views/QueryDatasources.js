import { computed, defineComponent, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Server } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import { API_ENDPOINTS, QUERY_DATASOURCE_BY_ID } from '../api/endpoints'

export default defineComponent({
  name: 'QueryDatasourcesView',
  components: {
    Plus,
    Server
  },
  setup() {
    const { t } = useI18n()
    const configs = ref([])
    const loading = ref(false)
    const saving = ref(false)
    const error = ref('')
    const dialogVisible = ref(false)

    const filters = reactive({
      keyword: '',
      type: '',
      scope: 'all'
    })

    const form = reactive({
      id: null,
      name: '',
      type: '',
      driverClass: '',
      jdbcUrl: '',
      username: '',
      password: '',
      maxPoolSize: 4,
      minIdle: 1,
      connectionTimeoutMs: 10000,
      isDefault: false
    })

    const typeOptions = computed(() => (
      [...new Set(
        configs.value
          .map((row) => String(row.type || '').trim().toLowerCase())
          .filter(Boolean)
      )].sort()
    ))

    const filteredConfigs = computed(() => configs.value.filter((row) => matchesFilters(row)))
    const defaultCount = computed(() => filteredConfigs.value.filter((row) => row.isDefault).length)
    const customCount = computed(() => Math.max(0, filteredConfigs.value.length - defaultCount.value))
    const hasActiveFilters = computed(() => (
      Boolean(filters.keyword.trim()) || Boolean(filters.type) || filters.scope !== 'all'
    ))

    async function load() {
      loading.value = true
      error.value = ''
      try {
        const { data } = await client.get(API_ENDPOINTS.QUERY_DATASOURCES)
        configs.value = Array.isArray(data) ? data : []
      } catch (e) {
        console.error('Failed to load datasource configs', e)
        error.value = e.response?.data?.message || t('datasources.loadFailed')
        configs.value = []
      } finally {
        loading.value = false
      }
    }

    function matchesFilters(row) {
      const keyword = filters.keyword.trim().toLowerCase()
      if (keyword) {
        const haystack = [
          row.name,
          row.jdbcUrl,
          row.driverClass
        ].filter(Boolean).join(' ').toLowerCase()
        if (!haystack.includes(keyword)) {
          return false
        }
      }

      if (filters.type && String(row.type || '').trim().toLowerCase() !== filters.type) {
        return false
      }

      if (filters.scope === 'default' && !row.isDefault) {
        return false
      }

      if (filters.scope === 'custom' && row.isDefault) {
        return false
      }

      return true
    }

    function clearFilters() {
      filters.keyword = ''
      filters.type = ''
      filters.scope = 'all'
    }

    function resetForm() {
      Object.assign(form, {
        id: null,
        name: '',
        type: '',
        driverClass: '',
        jdbcUrl: '',
        username: '',
        password: '',
        maxPoolSize: 4,
        minIdle: 1,
        connectionTimeoutMs: 10000,
        isDefault: false
      })
    }

    function openCreate() {
      resetForm()
      dialogVisible.value = true
    }

    function edit(row) {
      Object.assign(form, {
        id: row.id,
        name: row.name || '',
        type: row.type || '',
        driverClass: row.driverClass || '',
        jdbcUrl: row.jdbcUrl || '',
        username: row.username || '',
        password: row.password || '',
        maxPoolSize: row.maxPoolSize ?? 4,
        minIdle: row.minIdle ?? 1,
        connectionTimeoutMs: row.connectionTimeoutMs ?? 10000,
        isDefault: Boolean(row.isDefault)
      })
      dialogVisible.value = true
    }

    async function promote(row) {
      edit(row)
      form.isDefault = true
      await save()
    }

    async function remove(row) {
      try {
        await ElMessageBox.confirm(`${row.name}`, t('common.remove'), {
          confirmButtonText: t('common.remove'),
          cancelButtonText: t('common.cancel'),
          type: 'warning'
        })
        await client.delete(QUERY_DATASOURCE_BY_ID(row.id))
        ElMessage.success(t('datasources.removed'))
        await load()
      } catch (e) {
        if (e !== 'cancel' && e !== 'close') {
          ElMessage.error(e.response?.data?.message || e.message)
        }
      }
    }

    async function save() {
      if (!form.name.trim()) {
        ElMessage.warning(t('datasources.nameRequired'))
        return
      }
      if (!form.type.trim()) {
        ElMessage.warning(t('datasources.typeRequired'))
        return
      }
      if (!form.driverClass.trim()) {
        ElMessage.warning(t('datasources.driverRequired'))
        return
      }
      if (!form.jdbcUrl.trim()) {
        ElMessage.warning(t('datasources.jdbcRequired'))
        return
      }

      const payload = {
        name: form.name.trim(),
        type: form.type.trim(),
        driverClass: form.driverClass.trim(),
        jdbcUrl: form.jdbcUrl.trim(),
        username: form.username?.trim() || null,
        password: form.password ?? '',
        maxPoolSize: form.maxPoolSize,
        minIdle: form.minIdle,
        connectionTimeoutMs: form.connectionTimeoutMs,
        isDefault: Boolean(form.isDefault)
      }

      saving.value = true
      try {
        if (form.id) {
          await client.put(QUERY_DATASOURCE_BY_ID(form.id), payload)
        } else {
          await client.post(API_ENDPOINTS.QUERY_DATASOURCES, payload)
        }
        dialogVisible.value = false
        ElMessage.success(t('datasources.saved'))
        await load()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || e.message)
      } finally {
        saving.value = false
      }
    }

    onMounted(load)

    return {
      clearFilters,
      customCount,
      defaultCount,
      dialogVisible,
      edit,
      error,
      filteredConfigs,
      filters,
      form,
      hasActiveFilters,
      load,
      loading,
      openCreate,
      promote,
      remove,
      save,
      saving,
      t,
      typeOptions
    }
  }
})
