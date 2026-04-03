import { computed, defineComponent, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  LayoutDashboard,
  Activity,
  Terminal,
  Server,
  Rocket,
  Database,
  Zap
} from 'lucide-vue-next'

export default defineComponent({
  name: 'App',
  components: {
    LayoutDashboard,
    Activity,
    Terminal,
    Server,
    Rocket,
    Database,
    Zap
  },
  setup() {
    const route = useRoute()
    const { locale, t } = useI18n()
    const menuActive = computed(() => route.path)

    watch(
      locale,
      (value) => {
        localStorage.setItem('manager-locale', value)
        document.documentElement.lang = value === 'zh' ? 'zh-CN' : 'en'
        document.title = t('app.title')
      },
      { immediate: true }
    )

    return {
      locale,
      menuActive,
      t
    }
  }
})
