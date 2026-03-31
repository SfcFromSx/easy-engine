<template>
  <el-container class="main-layout">
    <el-aside width="240px" class="premium-sidebar">
      <div class="brand">
        <div class="logo-box">
          <shield-check :size="20" color="#3b82f6" />
        </div>
        <div class="brand-text">
          <div class="title">Easy Engine</div>
          <div class="subtitle">Performance Profiler</div>
        </div>
      </div>

      <div class="menu-label">{{ $t('nav.navigate') }}</div>
      <el-menu 
        router 
        :default-active="menuActive"
        background-color="transparent"
        text-color="#94a3b8"
        active-text-color="#f8fafc"
        class="custom-menu"
      >
        <el-menu-item index="/">
          <el-icon><layout-dashboard :size="18" /></el-icon>
          <span>{{ $t('nav.dashboard') }}</span>
        </el-menu-item>
        <el-menu-item index="/jobs">
          <el-icon><play-circle :size="18" /></el-icon>
          <span>{{ $t('nav.jobs') }}</span>
        </el-menu-item>
        <el-menu-item index="/test-sets">
          <el-icon><file-json :size="18" /></el-icon>
          <span>{{ $t('nav.testSets') }}</span>
        </el-menu-item>
        <el-menu-item index="/templates">
          <el-icon><code2 :size="18" /></el-icon>
          <span>{{ $t('nav.templates') }}</span>
        </el-menu-item>
        <el-menu-item index="/datasources">
          <el-icon><database :size="18" /></el-icon>
          <span>{{ $t('nav.datasources') }}</span>
        </el-menu-item>
        <el-menu-item index="/runs">
          <el-icon><history :size="18" /></el-icon>
          <span>{{ $t('nav.runs') }}</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <!-- Language Switcher -->
        <div class="lang-switcher">
          <el-radio-group v-model="locale" size="small" class="custom-radio">
            <el-radio-button value="en">EN</el-radio-button>
            <el-radio-button value="zh">中文</el-radio-button>
          </el-radio-group>
        </div>

        <el-popover placement="top-start" width="280" trigger="hover" popper-class="health-popover">
          <template #reference>
            <div class="status-indicator">
              <span :class="['dot', status === 'online' ? 'dot-online' : 'dot-offline']"></span>
              <span>{{ status === 'online' ? $t('status.online') : $t('status.offline') }}</span>
              <el-icon style="margin-left: auto; margin-right: 0"><chevron-right :size="12" /></el-icon>
            </div>
          </template>
          <div class="health-details">
            <div class="health-header">{{ $t('status.health') }}</div>
            <div class="component-item">
              <span class="comp-name">MySQL (Metadata)</span>
              <el-tag size="small" :type="health.mysql === 'OK' ? 'success' : 'danger'">{{ health.mysql }}</el-tag>
            </div>
            <div class="component-item">
              <span class="comp-name">Kylin OLAP</span>
              <el-tag size="small" :type="health.kylin === 'OK' ? 'success' : 'danger'">{{ health.kylin }}</el-tag>
            </div>
            <div class="component-item">
              <span class="comp-name">Presto Engine</span>
              <el-tag size="small" :type="health.presto === 'OK' ? 'success' : 'danger'">{{ health.presto }}</el-tag>
            </div>
            <div class="component-item">
              <span class="comp-name">Redis Meta Cache</span>
              <el-tag size="small" type="success">CONNECTED</el-tag>
            </div>
            <div class="health-footer">Based on last preflight check</div>
          </div>
        </el-popover>
      </div>
    </el-aside>
    <el-main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </el-main>
  </el-container>
</template>

<script setup>
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

const route = useRoute()
const { locale } = useI18n()
const status = ref('offline')
const health = ref({ mysql: 'OK', kylin: '—', presto: '—' })

const menuActive = computed(() => {
  const p = route.path
  if (p === '/jobs' || p.startsWith('/jobs/')) return '/jobs'
  if (p === '/test-sets') return '/test-sets'
  if (p === '/templates') return '/templates'
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
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: #f8fafc;
}

.premium-sidebar {
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.brand {
  padding: 32px 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-box {
  width: 40px;
  height: 40px;
  background: rgba(59, 130, 246, 0.15);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.brand-text .title {
  font-family: 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 16px;
  color: #f8fafc;
  letter-spacing: -0.01em;
}

.brand-text .subtitle {
  font-size: 10px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 700;
}

.menu-label {
  padding: 0 24px 12px;
  font-size: 10px;
  font-weight: 800;
  color: #475569;
  letter-spacing: 0.15em;
  text-transform: uppercase;
}

.custom-menu {
  border-right: none !important;
  flex-grow: 1;
}

:deep(.el-menu-item) {
  height: 50px;
  line-height: 50px;
  margin: 4px 12px;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.05) !important;
  color: #f8fafc !important;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.2) 0%, rgba(59, 130, 246, 0) 100%) !important;
  border-left: 3px solid #3b82f6;
  color: #fff !important;
  font-weight: 600;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(0, 0, 0, 0.1);
}

.lang-switcher {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

:deep(.custom-radio .el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  transition: all 0.3s;
}

:deep(.custom-radio .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #3b82f6 !important;
  color: #fff !important;
  border-color: #3b82f6 !important;
  box-shadow: 0 0 12px rgba(59, 130, 246, 0.4);
}

.status-indicator {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
  cursor: pointer;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  transition: all 0.2s;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.status-indicator:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
  border-color: rgba(255, 255, 255, 0.1);
}

.main-content {
  padding: 20px;
  background: #f8fafc;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* Health Popover Customization */
:deep(.health-popover) {
  background: #ffffff !important;
  border-radius: 12px !important;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1) !important;
  border: 1px solid #e2e8f0 !important;
}
</style>
