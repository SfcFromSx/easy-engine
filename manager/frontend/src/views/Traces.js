import { computed, defineComponent, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { RefreshCw, Terminal } from 'lucide-vue-next'
import client from '../api/client'
import { API_ENDPOINTS } from '../api/endpoints'
import { formatDateTime, formatDuration, shortFingerprint } from '../utils/formatters'

export default defineComponent({
  name: 'TracesView',
  components: {
    RefreshCw,
    Terminal
  },
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()
    const rows = ref([])
    const total = ref(0)
    const page = ref(1)
    const size = ref(10)
    const loading = ref(false)
    const error = ref('')
    const filterDraft = reactive(emptyFilters())

    const activeFilters = computed(() => ({
      fingerprint: normalizeQuery(route.query.fingerprint),
      datasource: normalizeQuery(route.query.datasource),
      cacheKey: normalizeQuery(route.query.cacheKey),
      sourceFlag: normalizeQuery(route.query.sourceFlag),
      cacheHit: normalizeQuery(route.query.cacheHit),
      parseStatus: normalizeQuery(route.query.parseStatus),
      sqlKeyword: normalizeQuery(route.query.sqlKeyword)
    }))

    const hasActiveFilters = computed(() => Object.values(activeFilters.value).some(Boolean))
    const activeFilterEntries = computed(() => {
      const entries = []
      if (activeFilters.value.fingerprint) {
        entries.push({ label: t('traces.fingerprint'), value: activeFilters.value.fingerprint })
      }
      if (activeFilters.value.datasource) {
        entries.push({ label: t('traces.datasource'), value: activeFilters.value.datasource })
      }
      if (activeFilters.value.cacheKey) {
        entries.push({ label: t('traces.cacheKey'), value: activeFilters.value.cacheKey })
      }
      if (activeFilters.value.sourceFlag) {
        entries.push({ label: t('traces.flag'), value: activeFilters.value.sourceFlag })
      }
      if (activeFilters.value.cacheHit) {
        entries.push({ label: t('traces.cache'), value: activeFilters.value.cacheHit === 'true' ? 'HIT' : 'MISS' })
      }
      if (activeFilters.value.parseStatus) {
        entries.push({ label: t('traces.parseResult'), value: activeFilters.value.parseStatus })
      }
      if (activeFilters.value.sqlKeyword) {
        entries.push({ label: t('traces.sql'), value: activeFilters.value.sqlKeyword })
      }
      return entries
    })

    function goPattern(sqlFingerprint) {
      router.push({ path: '/patterns', query: { fingerprint: sqlFingerprint } })
    }

    function emptyFilters() {
      return {
        fingerprint: '',
        datasource: '',
        cacheKey: '',
        sourceFlag: '',
        cacheHit: '',
        parseStatus: '',
        sqlKeyword: ''
      }
    }

    function normalizeQuery(value) {
      return typeof value === 'string' && value.trim() ? value.trim() : ''
    }

    function buildQuery(filters) {
      const query = {}
      if (filters.fingerprint.trim()) query.fingerprint = filters.fingerprint.trim()
      if (filters.datasource.trim()) query.datasource = filters.datasource.trim()
      if (filters.cacheKey.trim()) query.cacheKey = filters.cacheKey.trim()
      if (filters.sourceFlag.trim()) query.sourceFlag = filters.sourceFlag.trim()
      if (filters.cacheHit.trim()) query.cacheHit = filters.cacheHit.trim()
      if (filters.parseStatus.trim()) query.parseStatus = filters.parseStatus.trim()
      if (filters.sqlKeyword.trim()) query.sqlKeyword = filters.sqlKeyword.trim()
      return query
    }

    function isSameQuery(nextQuery) {
      const current = buildQuery(activeFilters.value)
      return JSON.stringify(current) === JSON.stringify(nextQuery)
    }

    function updateFilters() {
      const nextQuery = buildQuery(filterDraft)
      page.value = 1
      if (isSameQuery(nextQuery)) {
        load()
        return
      }
      router.push({ path: '/traces', query: nextQuery })
    }

    function applyFilter() {
      updateFilters()
    }

    function clearFilter() {
      Object.assign(filterDraft, emptyFilters())
      page.value = 1
      if (!hasActiveFilters.value) {
        load()
        return
      }
      router.push({ path: '/traces', query: {} })
    }

    function statusType(status) {
      if (status === 'OK') return 'success'
      if (status === 'ERROR') return 'danger'
      return 'info'
    }

    function sourceFlagType(flag) {
      if (flag === 'SEED') return 'info'
      if (flag === 'SELF') return 'warning'
      if (flag === 'JDBC') return 'success'
      return 'info'
    }

    async function load() {
      loading.value = true
      error.value = ''
      try {
        const params = { page: page.value - 1, size: size.value }
        if (activeFilters.value.fingerprint) params.fingerprint = activeFilters.value.fingerprint
        if (activeFilters.value.datasource) params.datasource = activeFilters.value.datasource
        if (activeFilters.value.cacheKey) params.cacheKey = activeFilters.value.cacheKey
        if (activeFilters.value.sourceFlag) params.sourceFlag = activeFilters.value.sourceFlag
        if (activeFilters.value.cacheHit) params.cacheHit = activeFilters.value.cacheHit === 'true'
        if (activeFilters.value.parseStatus) params.parseStatus = activeFilters.value.parseStatus
        if (activeFilters.value.sqlKeyword) params.sqlKeyword = activeFilters.value.sqlKeyword
        const { data } = await client.get(API_ENDPOINTS.TRACES, { params })
        rows.value = data.content || []
        total.value = data.totalElements || 0
      } catch (e) {
        console.error('Failed to load traces', e)
        error.value = e.response?.data?.message || t('traces.loadFailed')
        rows.value = []
        total.value = 0
      } finally {
        loading.value = false
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
      activeFilterEntries,
      applyFilter,
      clearFilter,
      error,
      filterDraft,
      formatDateTime,
      formatDuration,
      goPattern,
      hasActiveFilters,
      load,
      loading,
      page,
      rows,
      shortFingerprint,
      size,
      sourceFlagType,
      statusType,
      t,
      total
    }
  }
})
