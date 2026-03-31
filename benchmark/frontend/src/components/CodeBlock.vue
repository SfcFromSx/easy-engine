<template>
  <div class="code-block-container" :class="{ 'is-compact': compact }" @mouseenter="showCopy = true" @mouseleave="showCopy = false">
    <div class="code-header" v-if="label || showCopy">
      <span class="lang-label">{{ label }}</span>
      <transition name="fade">
        <el-button v-if="showCopy" link class="copy-btn" @click="copy">
          <el-icon><CopyDocument /></el-icon>
          <span style="margin-left: 4px">Copy SQL</span>
        </el-button>
      </transition>
    </div>
    <div class="code-body">
      <pre><code v-html="highlightedCode"></code></pre>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { CopyDocument } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { highlightSql } from '../utils/sqlHighlight'

const props = defineProps({
  code: { type: String, default: '' },
  label: { type: String, default: 'SQL' },
  compact: { type: Boolean, default: false }
})

const showCopy = ref(false)

const highlightedCode = computed(() => highlightSql(props.code))

function copy() {
  navigator.clipboard.writeText(props.code).then(() => {
    ElMessage.success({ message: 'SQL 复制成功', duration: 1500 })
  })
}
</script>

<style>
.token-keyword { color: #f472b6; font-weight: 700; }
.token-string { color: #34d399; }
.token-number { color: #fbbf24; }
.token-comment { color: #94a3b8; font-style: italic; }
</style>

<style scoped>
.code-block-container {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  margin: 12px 0;
  background: #0f172a;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.code-block-container:hover {
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow: 0 8px 16px -4px rgba(0, 0, 0, 0.4);
}

.code-header {
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(8px);
  padding: 8px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  position: sticky;
  top: 0;
  z-index: 2;
}

.lang-label {
  font-size: 10px;
  color: #64748b;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.15em;
}

.copy-btn {
  color: #3b82f6 !important;
  font-size: 11px;
  font-weight: 600;
}

.code-body {
  padding: 16px;
  max-height: 500px;
  overflow-y: auto;
  transition: all 0.2s ease;
}

.code-block-container.is-compact .code-body {
  padding: 8px 12px;
  max-height: 120px;
}

pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

code {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 13px;
  color: #e2e8f0;
  line-height: 1.5;
}

.code-block-container.is-compact code {
  font-size: 12px;
  line-height: 1.35;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
