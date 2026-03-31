import { computed, defineComponent, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { RefreshCw, Terminal } from 'lucide-vue-next'
import client from '../api/client'
import { ACCEL_FROM_PATTERN, API_ENDPOINTS } from '../api/endpoints'
import { shortFingerprint } from '../utils/formatters'

export default defineComponent({
  name: 'PatternsView',
  components: {
    RefreshCw,
    Terminal
  },
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()

    const patterns = ref([])
    const total = ref(0)
    const page = ref(1)
    const size = ref(10)
    const loading = ref(false)
    const submitting = ref(false)
    const error = ref('')
    const visible = ref(false)
    const selected = ref(null)
    const filterDraft = reactive(emptyFilters())
    const form = reactive({ tableName: '', schemaName: 'public' })

    const activeFilters = computed(() => ({
      fingerprint: normalizeQuery(route.query.fingerprint),
      sqlKeyword: normalizeQuery(route.query.sqlKeyword),
      minExecutionCount: normalizeQuery(route.query.minExecutionCount)
    }))

    const hasActiveFilters = computed(() => Object.values(activeFilters.value).some(Boolean))
    const activeFilterEntries = computed(() => {
      const entries = []
      if (activeFilters.value.fingerprint) {
        entries.push({ label: t('patterns.fingerprintId'), value: activeFilters.value.fingerprint })
      }
      if (activeFilters.value.sqlKeyword) {
        entries.push({ label: t('patterns.sampleSql'), value: activeFilters.value.sqlKeyword })
      }
      if (activeFilters.value.minExecutionCount) {
        entries.push({ label: t('patterns.minExecutionCount'), value: activeFilters.value.minExecutionCount })
      }
      return entries
    })

    function goTraces(row) {
      router.push({ path: '/traces', query: { fingerprint: row.sqlFingerprint } })
    }

    function emptyFilters() {
      return {
        fingerprint: '',
        sqlKeyword: '',
        minExecutionCount: ''
      }
    }

    function normalizeQuery(value) {
      return typeof value === 'string' && value.trim() ? value.trim() : ''
    }

    function buildQuery(filters) {
      const query = {}
      if (filters.fingerprint.trim()) query.fingerprint = filters.fingerprint.trim()
      if (filters.sqlKeyword.trim()) query.sqlKeyword = filters.sqlKeyword.trim()
      if (filters.minExecutionCount.trim()) query.minExecutionCount = filters.minExecutionCount.trim()
      return query
    }

    function isSameQuery(nextQuery) {
      return JSON.stringify(buildQuery(activeFilters.value)) === JSON.stringify(nextQuery)
    }

    function updateFilters() {
      const nextQuery = buildQuery(filterDraft)
      page.value = 1
      if (isSameQuery(nextQuery)) {
        load()
        return
      }
      router.push({ path: '/patterns', query: nextQuery })
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
      router.push({ path: '/patterns', query: {} })
    }

    function rowClassName({ row }) {
      return row.sqlFingerprint === activeFilters.value.fingerprint ? 'is-selected-row' : ''
    }

    async function load() {
      loading.value = true
      error.value = ''
      try {
        const params = { page: page.value - 1, size: size.value }
        if (activeFilters.value.fingerprint) params.fingerprint = activeFilters.value.fingerprint
        if (activeFilters.value.sqlKeyword) params.sqlKeyword = activeFilters.value.sqlKeyword
        if (activeFilters.value.minExecutionCount) params.minExecutionCount = Number(activeFilters.value.minExecutionCount)
        const { data } = await client.get(API_ENDPOINTS.PATTERNS_TOP, { params })
        patterns.value = data.content || []
        total.value = data.totalElements || 0
      } catch (e) {
        console.error('Failed to load patterns', e)
        error.value = e.response?.data?.message || t('patterns.loadFailed')
        patterns.value = []
        total.value = 0
      } finally {
        loading.value = false
      }
    }

    function openDialog(row) {
      selected.value = row
      form.tableName = `rollup_${(row.sqlFingerprint || '').slice(0, 8)}`
      form.schemaName = 'public'
      visible.value = true
    }

    async function submit() {
      if (!selected.value) {
        ElMessage.error(t('patterns.selectPatternFirst'))
        return
      }
      if (!form.tableName.trim()) {
        ElMessage.error(t('patterns.tableNameRequired'))
        return
      }

      submitting.value = true
      try {
        await client.post(ACCEL_FROM_PATTERN, {
          patternStatsId: selected.value.id,
          tableName: form.tableName.trim(),
          schemaName: form.schemaName.trim() || 'public'
        })
        ElMessage.success(t('patterns.created'))
        visible.value = false
      } catch (e) {
        ElMessage.error(e.response?.data?.message || e.message)
      } finally {
        submitting.value = false
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
      activeFilters,
      applyFilter,
      clearFilter,
      error,
      filterDraft,
      form,
      goTraces,
      hasActiveFilters,
      load,
      loading,
      openDialog,
      page,
      patterns,
      rowClassName,
      shortFingerprint,
      size,
      submit,
      submitting,
      t,
      total,
      visible
    }
  }
})
