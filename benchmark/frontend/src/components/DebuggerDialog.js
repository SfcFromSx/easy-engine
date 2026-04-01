import { ref, onMounted, watch } from 'vue'
import { Play, Terminal, Activity, AlertCircle } from 'lucide-vue-next'
import client from '../api/client'


export default {
  __name: 'DebuggerDialog',
  components: {
    Play,
    Terminal,
    Activity,
    AlertCircle
  },
  props: {
  modelValue: Boolean,
  sql: String,
  initialJobId: [Number, String]
},
  emits: ['update:modelValue'],
  setup(__props, { expose: __expose, emit: __emit }) {
  __expose();

const props = __props
const emit = __emit

const visible = ref(props.modelValue)
watch(() => props.modelValue, (v) => visible.value = v)
watch(visible, (v) => emit('update:modelValue', v))

const dataSources = ref([])
const selectedDsId = ref(null)
const loading = ref(false)
const result = ref(null)
const error = ref(null)

async function loadDataSources() {
  try {
    const { data } = await client.get('/datasources')
    dataSources.value = data
    if (data.length > 0 && !selectedDsId.value) {
      selectedDsId.value = data[0].id
    }
  } catch (err) {
    console.error('Failed to load datasources', err)
  }
}

async function runQuery() {
  if (!selectedDsId.value) return
  loading.value = true
  result.value = null
  error.value = null
  try {
    const { data } = await client.post(`/datasources/${selectedDsId.value}/query`, { sql: props.sql })
    if (data.error) {
      error.value = data.error
    } else {
      result.value = data
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    loading.value = false
  }
}

onMounted(loadDataSources)

const __returned__ = { props, emit, visible, dataSources, selectedDsId, loading, result, error, loadDataSources, runQuery, ref, onMounted, watch, get Play() { return Play }, get Terminal() { return Terminal }, get Activity() { return Activity }, get AlertCircle() { return AlertCircle }, get client() { return client } }
return __returned__
}

}
