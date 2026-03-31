<template>
  <el-container class="main-layout">
    <el-aside width="240px">
      <div class="brand">
        <div class="logo-box">
          <zap :size="20" color="#3b82f6" />
        </div>
        <div class="brand-text">
          <div class="title">Easy Engine</div>
          <div class="subtitle">{{ t('app.subtitle') }}</div>
        </div>
      </div>

      <div class="menu-label">{{ t('app.section') }}</div>
      <el-menu router :default-active="menuActive">
        <el-menu-item index="/">
          <el-icon><layout-dashboard :size="18" /></el-icon>
          <span>{{ t('nav.dashboard') }}</span>
        </el-menu-item>
        <el-menu-item index="/traces">
          <el-icon><activity :size="18" /></el-icon>
          <span>{{ t('nav.traces') }}</span>
        </el-menu-item>
        <el-menu-item index="/patterns">
          <el-icon><terminal :size="18" /></el-icon>
          <span>{{ t('nav.patterns') }}</span>
        </el-menu-item>
        <el-menu-item index="/query-datasources">
          <el-icon><server :size="18" /></el-icon>
          <span>{{ t('nav.datasources') }}</span>
        </el-menu-item>
        <el-menu-item index="/acceleration">
          <el-icon><rocket :size="18" /></el-icon>
          <span>{{ t('nav.acceleration') }}</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="lang-switcher">
          <el-radio-group v-model="locale" size="small" class="custom-radio">
            <el-radio-button value="en">{{ t('app.english') }}</el-radio-button>
            <el-radio-button value="zh">{{ t('app.chinese') }}</el-radio-button>
          </el-radio-group>
        </div>
        <div class="status-indicator">
          <span class="dot dot-online"></span>
          <span>{{ t('app.console') }}</span>
        </div>
      </div>
    </el-aside>
    <el-main>
      <div class="content-shell">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { 
  LayoutDashboard, 
  Activity, 
  Terminal, 
  Server,
  Rocket, 
  Zap 
} from 'lucide-vue-next'

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
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
}

.content-shell {
  width: 100%;
}

.brand {
  padding: 30px 24px 26px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-box {
  width: 42px;
  height: 42px;
  background: linear-gradient(180deg, rgba(47, 109, 246, 0.22) 0%, rgba(47, 109, 246, 0.1) 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(103, 157, 255, 0.18);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.brand-text .title {
  font-family: 'Outfit', sans-serif;
  font-weight: 700;
  font-size: 16px;
  color: #f8fafc;
  letter-spacing: -0.01em;
}

.brand-text .subtitle {
  font-size: 11px;
  color: #70809d;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-weight: 700;
}

.menu-label {
  padding: 0 24px 10px;
  font-size: 10px;
  font-weight: 800;
  color: #5b6b88;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.sidebar-footer {
  position: absolute;
  bottom: 0;
  width: 240px;
  padding: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(8, 14, 24, 0.18);
}

.lang-switcher {
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
}

:deep(.custom-radio .el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  color: #8ea0bf;
  font-size: 11px;
  font-weight: 700;
  box-shadow: none;
}

:deep(.custom-radio .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #2f6df6 !important;
  color: #fff !important;
  border-color: #2f6df6 !important;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #93a6c7;
  font-weight: 600;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.dot-online {
  background: #38bdf8;
  box-shadow: 0 0 0 4px rgba(56, 189, 248, 0.12);
}

.el-icon {
  width: 18px;
  height: 18px;
  display: flex !important;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

:deep(.el-main) {
  padding: 22px 26px 28px;
}

@media (max-width: 960px) {
  :deep(.el-main) {
    padding: 24px 20px 28px;
  }

  .content-shell {
    max-width: none;
  }
}
</style>
