import { computed, defineComponent, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Plus, Database } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import client from '../api/client'
import { ACCELERATION_BY_ID, ACCELERATION_STATUS, API_ENDPOINTS } from '../api/endpoints'

export default defineComponent({
  name: 'AccelerationView',
  components: {
    Plus,
    Database
  },
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()
    const tables = ref([])
    const total = ref(0)
    const page = ref(1)
    const size = ref(10)
    const loading = ref(false)
    const saving = ref(false)
    const error = ref('')
    const dlg = ref(false)
    const activeCount = ref(0)
    const draftCount = ref(0)
    const disabledCount = ref(0)
    const filterDraft = reactive(emptyFilters())

    const c = reactive({
      name: '',
      schemaName: 'public',
      ddlText: '',
      refreshSql: '',
      cronExpr: ''
    })

    const activeFilters = computed(() => ({
      keyword: normalizeQuery(route.query.keyword),
      status: normalizeQuery(route.query.status),
      schemaName: normalizeQuery(route.query.schemaName),
      source: normalizeQuery(route.query.source)
    }))

    const hasActiveFilters = computed(() => Object.values(activeFilters.value).some(Boolean))
    const activeFilterEntries = computed(() => {
      const entries = []
      if (activeFilters.value.keyword) {
        entries.push({ label: t('acceleration.name'), value: activeFilters.value.keyword })
      }
      if (activeFilters.value.status) {
        entries.push({ label: t('acceleration.status'), value: activeFilters.value.status })
      }
      if (activeFilters.value.schemaName) {
        entries.push({ label: t('acceleration.schema'), value: activeFilters.value.schemaName })
      }
      if (activeFilters.value.source) {
        entries.push({ label: t('acceleration.source'), value: activeFilters.value.source })
      }
      return entries
    })

    function emptyFilters() {
      return {
        keyword: '',
        status: '',
        schemaName: '',
        source: ''
      }
    }

    function normalizeQuery(value) {
      return typeof value === 'string' && value.trim() ? value.trim() : ''
    }

    function buildQuery(filters) {
      const query = {}
      if (filters.keyword.trim()) query.keyword = filters.keyword.trim()
      if (filters.status.trim()) query.status = filters.status.trim()
      if (filters.schemaName.trim()) query.schemaName = filters.schemaName.trim()
      if (filters.source.trim()) query.source = filters.source.trim()
      return query
    }

    function isSameQuery(nextQuery) {
      return JSON.stringify(buildQuery(activeFilters.value)) === JSON.stringify(nextQuery)
    }

    function applyFilter() {
      const nextQuery = buildQuery(filterDraft)
      page.value = 1
      if (isSameQuery(nextQuery)) {
        load()
        return
      }
      router.push({ path: '/acceleration', query: nextQuery })
    }

    function clearFilter() {
      Object.assign(filterDraft, emptyFilters())
      page.value = 1
      if (!hasActiveFilters.value) {
        load()
        return
      }
      router.push({ path: '/acceleration', query: {} })
    }

    async function load() {
      loading.value = true
      error.value = ''
      try {
        const [listRes, summaryRes] = await Promise.all([
          client.get(API_ENDPOINTS.ACCELERATION_TABLES, {
            params: {
              page: page.value - 1,
              size: size.value,
              ...(activeFilters.value.keyword ? { keyword: activeFilters.value.keyword } : {}),
              ...(activeFilters.value.status ? { status: activeFilters.value.status } : {}),
              ...(activeFilters.value.schemaName ? { schemaName: activeFilters.value.schemaName } : {}),
              ...(activeFilters.value.source ? { source: activeFilters.value.source } : {})
            }
          }),
          client.get(API_ENDPOINTS.STATS_SUMMARY)
        ])
        tables.value = listRes.data.content || []
        total.value = listRes.data.totalElements || 0
        activeCount.value = summaryRes.data.activeAccelerationCount || 0
        draftCount.value = summaryRes.data.draftAccelerationCount || 0
        disabledCount.value = Math.max(0, total.value - activeCount.value - draftCount.value)
      } catch (e) {
        console.error('Failed to load acceleration tables', e)
        error.value = e.response?.data?.message || t('acceleration.loadFailed')
        tables.value = []
        total.value = 0
        activeCount.value = 0
        draftCount.value = 0
        disabledCount.value = 0
      } finally {
        loading.value = false
      }
    }

    function openCreate() {
      Object.assign(c, { id: null, name: '', schemaName: 'public', ddlText: '', refreshSql: '', cronExpr: '' })
      dlg.value = true
    }

    function edit(row) {
      Object.assign(c, {
        ...row,
        schemaName: row.schemaName || 'public',
        ddlText: row.ddlText || '',
        refreshSql: row.refreshSql || '',
        cronExpr: row.cronExpr || ''
      })
      dlg.value = true
    }

    async function remove(row) {
      try {
        await client.delete(ACCELERATION_BY_ID(row.id))
        ElMessage.success(t('acceleration.removed'))
        await load()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || e.message)
      }
    }

    async function toggleStatus(row) {
      const status = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
      try {
        await client.patch(ACCELERATION_STATUS(row.id), { status })
        ElMessage.success(status === 'ACTIVE' ? t('acceleration.activated') : t('acceleration.disabledMsg'))
        await load()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || e.message)
      }
    }

    async function save() {
      const name = String(c.name || '').trim()
      const schemaName = String(c.schemaName || '').trim() || 'public'
      const ddlText = String(c.ddlText || '').trim()
      const refreshSql = String(c.refreshSql || '').trim()
      const cronExpr = String(c.cronExpr || '').trim()

      if (!name) {
        ElMessage.error(t('acceleration.nameRequired'))
        return
      }
      if (!ddlText) {
        ElMessage.error(t('acceleration.ddlRequired'))
        return
      }

      saving.value = true
      try {
        const payload = {
          name,
          schemaName,
          ddlText,
          refreshSql: refreshSql || null,
          cronExpr: cronExpr || null
        }
        if (c.id) {
          await client.put(ACCELERATION_BY_ID(c.id), payload)
        } else {
          await client.post(API_ENDPOINTS.ACCELERATION_TABLES, payload)
        }
        ElMessage.success(t('acceleration.saved'))
        dlg.value = false
        await load()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || e.message)
      } finally {
        saving.value = false
      }
    }

    watch(
      activeFilters,
      (nextFilters) => {
        Object.assign(filterDraft, nextFilters)
        page.value = 1
        load()
      },
      { immediate: true }
    )

    return {
      activeCount,
      activeFilterEntries,
      applyFilter,
      c,
      clearFilter,
      disabledCount,
      dlg,
      draftCount,
      edit,
      error,
      filterDraft,
      hasActiveFilters,
      load,
      loading,
      openCreate,
      page,
      remove,
      save,
      saving,
      size,
      t,
      tables,
      toggleStatus,
      total
    }
  }
})
