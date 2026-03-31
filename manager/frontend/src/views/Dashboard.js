import { computed, defineComponent, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Activity, Database, Rocket, Target, RefreshCw } from 'lucide-vue-next'
import client from '../api/client'
import { API_ENDPOINTS } from '../api/endpoints'
import { formatDateTime, formatDuration, formatPercent, shortFingerprint } from '../utils/formatters'

const EMPTY_STATS = {
  totalTraces: null,
  patternCount: null,
  parseOk: null,
  parseError: null,
  activeAccelerationCount: null,
  draftAccelerationCount: null,
  cacheHitCount: null,
  lastTraceAt: null
}

export default defineComponent({
  name: 'DashboardView',
  components: {
    Activity,
    Database,
    Rocket,
    Target,
    RefreshCw
  },
  setup() {
    const { t } = useI18n()
    const stats = reactive({ ...EMPTY_STATS })
    const summaryState = reactive({ loading: false, loaded: false, error: '' })
    const tracesState = reactive({ loading: false, loaded: false, error: '' })
    const patternsState = reactive({ loading: false, loaded: false, error: '' })

    const topPatterns = ref([])
    const recentTraces = ref([])
    const refreshing = computed(() => (
      summaryState.loading || tracesState.loading || patternsState.loading
    ))

    const showInitialState = computed(() => !summaryState.loaded)
    const showRecentTracesEmpty = computed(() => (
      tracesState.loaded && !tracesState.loading && !tracesState.error && !recentTraces.value.length
    ))
    const showTopPatternsEmpty = computed(() => (
      patternsState.loaded && !patternsState.loading && !patternsState.error && !topPatterns.value.length
    ))

    const totalTracesValue = computed(() => metricValue(stats.totalTraces))
    const patternCountValue = computed(() => metricValue(stats.patternCount))
    const activeAccelerationValue = computed(() => metricValue(stats.activeAccelerationCount))
    const parseSuccessRate = computed(() => metricPercent(stats.parseOk, stats.totalTraces))
    const cacheHitRate = computed(() => metricPercent(stats.cacheHitCount, stats.totalTraces))
    const lastTraceNote = computed(() => {
      if (showInitialState.value) {
        return t(summaryState.error ? 'dashboard.summaryUnavailable' : 'dashboard.loadingMetrics')
      }
      if (!stats.lastTraceAt) {
        return t('dashboard.noTraceYet')
      }
      return t('dashboard.lastTraceAt', { time: formatDateTime(stats.lastTraceAt) })
    })
    const patternNote = computed(() => (
      showInitialState.value
        ? t(summaryState.error ? 'dashboard.summaryUnavailable' : 'dashboard.loadingMetrics')
        : t('dashboard.patternNote')
    ))
    const accelerationNote = computed(() => (
      showInitialState.value
        ? t(summaryState.error ? 'dashboard.summaryUnavailable' : 'dashboard.loadingMetrics')
        : t('dashboard.draftAcceleration', { count: stats.draftAccelerationCount })
    ))
    const rateNote = computed(() => (
      showInitialState.value
        ? t(summaryState.error ? 'dashboard.summaryUnavailable' : 'dashboard.loadingMetrics')
        : t('dashboard.cacheHitRate', { value: cacheHitRate.value })
    ))

    const healthText = computed(() => {
      if (summaryState.loading) return t('dashboard.refreshing')
      if (summaryState.error) return t('dashboard.loadFailed')
      if (!summaryState.loaded || !stats.totalTraces) return t('dashboard.waiting')
      if (stats.parseError > 0) return t('dashboard.warning')
      return t('dashboard.healthy')
    })

    const healthTagType = computed(() => {
      if (summaryState.loading) return 'info'
      if (summaryState.error) return 'danger'
      if (!summaryState.loaded || !stats.totalTraces) return 'info'
      if (stats.parseError > 0) return 'warning'
      return 'success'
    })

    function metricValue(value) {
      return value == null ? '--' : value
    }

    function metricPercent(part, total) {
      if (!summaryState.loaded || part == null || total == null) {
        return '--'
      }
      return formatPercent(part, total)
    }

    function applySummary(summary = {}) {
      stats.totalTraces = summary.totalTraces ?? 0
      stats.patternCount = summary.patternCount ?? 0
      stats.parseOk = summary.parseOk ?? 0
      stats.parseError = summary.parseError ?? 0
      stats.activeAccelerationCount = summary.activeAccelerationCount ?? 0
      stats.draftAccelerationCount = summary.draftAccelerationCount ?? 0
      stats.cacheHitCount = summary.cacheHitCount ?? 0
      stats.lastTraceAt = summary.lastTraceAt ?? null
    }

    async function loadSummary() {
      summaryState.loading = true
      summaryState.error = ''
      try {
        const { data } = await client.get(API_ENDPOINTS.STATS_SUMMARY)
        applySummary(data)
        summaryState.loaded = true
      } catch (e) {
        console.error('Failed to load dashboard summary', e)
        summaryState.error = e.response?.data?.message || t('dashboard.loadFailed')
        if (!summaryState.loaded) {
          Object.assign(stats, EMPTY_STATS)
        }
      } finally {
        summaryState.loading = false
      }
    }

    async function loadPatterns() {
      patternsState.loading = true
      patternsState.error = ''
      try {
        const { data } = await client.get(API_ENDPOINTS.PATTERNS_TOP, { params: { page: 0, size: 5 } })
        topPatterns.value = data?.content || []
        patternsState.loaded = true
      } catch (e) {
        console.error('Failed to load dashboard patterns', e)
        patternsState.error = e.response?.data?.message || t('patterns.loadFailed')
        if (!patternsState.loaded) {
          topPatterns.value = []
        }
      } finally {
        patternsState.loading = false
      }
    }

    async function loadTraces() {
      tracesState.loading = true
      tracesState.error = ''
      try {
        const { data } = await client.get(API_ENDPOINTS.TRACES, { params: { page: 0, size: 5 } })
        recentTraces.value = data?.content || []
        tracesState.loaded = true
      } catch (e) {
        console.error('Failed to load dashboard traces', e)
        tracesState.error = e.response?.data?.message || t('traces.loadFailed')
        if (!tracesState.loaded) {
          recentTraces.value = []
        }
      } finally {
        tracesState.loading = false
      }
    }

    async function load() {
      await Promise.allSettled([
        loadSummary(),
        loadPatterns(),
        loadTraces()
      ])
    }

    onMounted(load)

    return {
      activeAccelerationValue,
      accelerationNote,
      formatDateTime,
      formatDuration,
      healthTagType,
      healthText,
      lastTraceNote,
      load,
      parseSuccessRate,
      patternCountValue,
      patternNote,
      patternsState,
      rateNote,
      recentTraces,
      refreshing,
      shortFingerprint,
      showRecentTracesEmpty,
      showTopPatternsEmpty,
      summaryState,
      t,
      topPatterns,
      totalTracesValue,
      tracesState
    }
  }
})
