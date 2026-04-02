import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { 
  LayoutDashboard, 
  PlayCircle, 
  FileJson, 
  Code2, 
  History, 
  ShieldCheck,
  ChevronRight,
  Database
} from 'lucide-vue-next'
import client from './api/client'


export default {
  __name: 'App',
  components: {
    LayoutDashboard,
    PlayCircle,
    FileJson,
    Code2,
    History,
    ShieldCheck,
    ChevronRight,
    Database
  },
  setup(__props, { expose: __expose }) {
  __expose();

const route = useRoute()
const { locale } = useI18n()
const status = ref('offline')
const health = ref({ mysql: 'OK', kylin: '—', presto: '—' })

const menuActive = computed(() => {
  const p = route.path
  if (p === '/jobs' || p.startsWith('/jobs/')) return '/jobs'
  if (p === '/test-sets') return '/test-sets'
  if (p === '/sql-lib' || p === '/templates') return '/sql-lib'
  if (p === '/datasources') return '/datasources'
  if (p === '/runs') return '/runs'
  return '/'
})

async function checkStatus() {
  try {
    const { data } = await client.get('/preflight')
    health.value = {
      mysql: data.mysql?.status === 'OK' ? 'OK' : 'ERROR',
      kylin: data.kylinRest?.status === 'OK' ? 'OK' : 'ERROR',
      presto: data.prestoUi?.status === 'OK' ? 'OK' : 'ERROR'
    }
    status.value = (health.value.kylin === 'OK' || health.value.presto === 'OK') ? 'online' : 'offline'
  } catch {
    status.value = 'offline'
  }
}

onMounted(() => {
  checkStatus()
  setInterval(checkStatus, 30000) 
})

const __returned__ = { route, locale, status, health, menuActive, checkStatus, computed, onMounted, ref, get useRoute() { return useRoute }, get useI18n() { return useI18n }, get LayoutDashboard() { return LayoutDashboard }, get PlayCircle() { return PlayCircle }, get FileJson() { return FileJson }, get Code2() { return Code2 }, get History() { return History }, get ShieldCheck() { return ShieldCheck }, get ChevronRight() { return ChevronRight }, get Database() { return Database }, get client() { return client } }
return __returned__
}

}
