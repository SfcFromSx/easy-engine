<template>
  <el-dialog
    v-model="visible"
    :title="$t('debugger.title')"
    width="900px"
    top="5vh"
    destroy-on-close
    custom-class="premium-dialog"
  >
    <div class="debugger-body">
      <div class="debug-controls">
        <div class="control-item">
          <span class="label">{{ $t('debugger.targetDataSource') }}</span>
          <el-select
            v-model="selectedDsId"
            :placeholder="$t('debugger.selectDataSource')"
            style="width: 350px"
            filterable
          >
            <el-option
              v-for="ds in dataSources"
              :key="ds.id"
              :label="ds.name"
              :value="ds.id"
            >
              <div class="ds-option-item">
                <span class="ds-name">{{ ds.name }}</span>
                <span class="ds-url">{{ ds.jdbcUrl }}</span>
              </div>
            </el-option>
          </el-select>
        </div>
        <el-button
          type="primary"
          class="premium-btn"
          :loading="loading"
          :disabled="!selectedDsId"
          @click="runQuery"
        >
          <el-icon style="margin-right: 4px"><play :size="16" /></el-icon>
          {{ $t('debugger.execute') }}
        </el-button>
      </div>

      <div class="sql-area">
        <div class="area-header">
          <terminal :size="16" />
          <span>{{ $t('debugger.statement') }}</span>
        </div>
        <div class="sql-box">{{ sql }}</div>
      </div>

      <div v-if="result || error" class="result-area">
        <div class="area-header">
          <activity :size="16" />
          <span>{{ $t('debugger.output') }}</span>
          <div v-if="result" class="stats">
            <el-tag size="small" type="success" effect="dark">
              {{ result.latencyMs }}ms
            </el-tag>
            <el-tag size="small" type="info" effect="plain" style="margin-left: 8px">
              {{ (result.rows || []).length }} {{ $t('common.results') }}
            </el-tag>
          </div>
        </div>

        <div v-if="error" class="error-box">
          <alert-circle :size="20" color="#ef4444" />
          <div class="error-content">
            <div class="error-title">{{ $t('common.error') }}</div>
            <div class="error-msg">{{ error }}</div>
          </div>
        </div>

        <div v-else-if="result" class="data-preview">
          <el-table 
            v-if="result.headers && result.headers.length"
            :data="result.rows" 
            size="small" 
            stripe 
            border 
            height="350px"
          >
            <el-table-column
              v-for="col in result.headers"
              :key="col"
              :prop="col"
              :label="col"
              min-width="150"
              show-overflow-tooltip
            />
          </el-table>
          <div v-else-if="result.message" class="empty-message">
             {{ result.message }}
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { Play, Terminal, Activity, AlertCircle } from 'lucide-vue-next'
import client from '../api/client'

const props = defineProps({
  modelValue: Boolean,
  sql: String,
  initialJobId: [Number, String]
})
const emit = defineEmits(['update:modelValue'])

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
</script>

<style scoped>
.debugger-body {
  padding: 8px;
}
.debug-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: #f8fafc;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}
.control-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ds-option-item {
  display: flex;
  flex-direction: column;
  padding: 4px 0;
}
.ds-name {
  font-weight: 600;
  line-height: 1.2;
}
.ds-url {
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.2;
}
.label {
  font-weight: 700;
  color: #475569;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.area-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 700;
  color: #1e293b;
  font-size: 14px;
}
.sql-area {
  margin-bottom: 24px;
}
.sql-box {
  background: #0f172a;
  color: #e2e8f0;
  padding: 16px;
  border-radius: 8px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 150px;
  overflow-y: auto;
}
.result-area {
  margin-top: 24px;
  border-top: 1px solid #e2e8f0;
  padding-top: 20px;
}
.error-box {
  background: #fef2f2;
  border: 1px solid #fee2e2;
  padding: 16px;
  border-radius: 8px;
  display: flex;
  gap: 12px;
}
.error-title {
  font-weight: 700;
  color: #991b1b;
  margin-bottom: 4px;
}
.error-msg {
  color: #b91c1c;
  font-size: 13px;
  font-family: monospace;
}
.stats {
  margin-left: auto;
}
.data-preview {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}
.empty-message {
  padding: 40px;
  text-align: center;
  color: #94a3b8;
  font-style: italic;
}
.premium-btn {
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}
</style>
