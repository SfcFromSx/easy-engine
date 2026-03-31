<template>
  <div v-loading="loading">
    <div v-if="job" class="job-detail">
      <div class="header-section">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/jobs' }">{{ $t('jobDetail.breadcrumb') }}</el-breadcrumb-item>
          <el-breadcrumb-item>{{ job.name }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="title-row">
          <div class="title-group">
            <h1>{{ job.name }}</h1>
            <p class="subtitle">{{ $t('jobDetail.subtitle') }}</p>
          </div>
          <div class="actions">
            <el-button @click="$router.push('/jobs')" plain>{{ $t('common.back') }}</el-button>
            <el-button type="primary" @click="startJob" :loading="starting">
              <el-icon style="margin-right: 4px"><VideoPlay /></el-icon>
              {{ $t('jobDetail.start') }}
            </el-button>
          </div>
        </div>
      </div>

      <el-row :gutter="24">
        <el-col :span="16">
          <div class="glass-card detail-card">
            <div class="card-title">
              <el-icon><Setting /></el-icon>
              {{ $t('jobDetail.config') }}
            </div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item :label="$t('jobDetail.concurrency')">{{ job.concurrentThreads }}</el-descriptions-item>
              <el-descriptions-item :label="$t('jobDetail.rounds')">{{ job.rounds }}</el-descriptions-item>
              <el-descriptions-item :label="$t('jobDetail.strategy')">
                <el-tag size="small" effect="dark" :type="strategyTagType(job.strategy)">{{ job.strategy }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('jobs.colDataSource')">
                <span>{{ dataSource?.name || `#${job.dataSourceId || '-'}` }}</span>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('jobDetail.driver')">
                <code class="driver-code">{{ dataSource?.driverClass || 'N/A' }}</code>
              </el-descriptions-item>
              <el-descriptions-item label="JDBC URL" :span="2">
                <code class="url-code">{{ dataSource?.jdbcUrl || 'N/A' }}</code>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="glass-card detail-card" style="margin-top: 24px">
            <div class="card-title">
              <el-icon><DataLine /></el-icon>
              {{ $t('jobDetail.trend') }}
            </div>
            <div class="chart-container" style="height: 320px">
              <PerformanceCharts
                v-if="trendData.length"
                title="QPS 与 P50 延迟趋势"
                type="line"
                :data="trendData"
                xKey="runId"
                :yKeys="[
                  { name: 'QPS', key: 'qps', color: '#3b82f6' },
                  { name: 'P50 (ms)', key: 'p50', color: '#10b981' }
                ]"
              />
              <el-empty v-else :description="$t('jobDetail.noTrend')" :image-size="80" />
            </div>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="glass-card detail-card history-card">
            <div class="card-title">
              <el-icon><Clock /></el-icon>
              {{ $t('jobDetail.history') }}
            </div>
            <div class="run-history">
              <div v-for="run in runs" :key="run.id" class="history-item" @click="goRun(run.id)">
                <div class="history-main">
                  <span class="run-tag">#{{ run.id }}</span>
                  <span class="run-date">{{ formatDate(run.startedAt) }}</span>
                </div>
                <div class="history-metrics" v-if="run.status === 'COMPLETED'">
                  <div class="metric-mini">
                    <span class="label">QPS</span>
                    <span class="val">{{ run.qps?.toFixed(2) }}</span>
                  </div>
                  <div class="metric-mini">
                    <span class="label">P50</span>
                    <span class="val">{{ run.p50Ms?.toFixed(2) }}ms</span>
                  </div>
                </div>
                <div class="history-status">
                  <el-tag :type="run.status === 'COMPLETED' ? 'success' : 'info'" size="small" effect="plain">
                    {{ run.status }}
                  </el-tag>
                  <el-button link type="primary" size="small">报告</el-button>
                </div>
              </div>
              <el-empty v-if="runs.length === 0" description="暂无运行记录" :image-size="60" />
            </div>
          </div>

          <div class="glass-card detail-card" style="margin-top: 24px">
            <div class="card-title">关联测试集</div>
            <div v-if="job.testSetId" class="test-set-link-box">
              <file-json :size="24" color="#3b82f6" />
              <div class="ts-body">
                <div class="ts-name">ID #{{ job.testSetId }}</div>
                <el-button type="primary" link @click="$router.push('/test-sets')" size="small">查看明细</el-button>
              </div>
            </div>
            <div v-else class="empty-msg">未关联专属测试集，随机抽取全局模板。</div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FileJson } from 'lucide-vue-next'
import { Setting, DataLine, Clock, VideoPlay } from '@element-plus/icons-vue'
import client from '../api/client'
import { API_ENDPOINTS, JOB_BY_ID, RUN_START } from '../api/endpoints'
import PerformanceCharts from '../components/PerformanceCharts.vue'

const route = useRoute()
const router = useRouter()
const job = ref(null)
const dataSource = ref(null)
const runs = ref([])
const loading = ref(false)
const starting = ref(false)

const trendData = computed(() => {
  return runs.value
    .filter(r => r.status === 'COMPLETED' && r.qps != null)
    .map(r => ({
      runId: `#${r.id}`,
      qps: r.qps,
      p50: r.p50Ms
    }))
    .reverse()
})

function strategyTagType(s) {
  if (s === 'RANDOM_WEIGHT') return 'primary'
  if (s === 'ROUND_ROBIN') return 'success'
  if (s === 'CACHE_PENETRATION') return 'warning'
  return 'info'
}

async function load() {
  const id = route.params.id
  loading.value = true
  try {
    const res = await client.get(JOB_BY_ID(id))
    job.value = res.data
    dataSource.value = null
    if (job.value?.dataSourceId) {
      const dsRes = await client.get(`/datasources/${job.value.dataSourceId}`)
      dataSource.value = dsRes.data
    }
    const runsRes = await client.get(API_ENDPOINTS.RUNS, { params: { jobId: id, size: 10 } })
    runs.value = runsRes.data.content || []
  } catch (e) {
    ElMessage.error('加载任务详情失败')
  } finally {
    loading.value = false
  }
}

async function startJob() {
  try {
    await ElMessageBox.confirm(`确认启动任务「${job.value.name}」？`, '提示')
    starting.value = true
    const { data } = await client.post(RUN_START, { jobId: job.value.id })
    ElMessage.success(`压测已提交: Run #${data.id}`)
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('启动失败')
  } finally {
    starting.value = false
  }
}

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString()
}

function goRun(id) {
  router.push({ path: '/runs', query: { id } })
}

onMounted(load)
</script>

<style scoped>
.job-detail {
  padding: 0;
}

.header-section {
  margin-bottom: 12px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.title-row h1 {
  font-size: 20px;
  margin: 0;
}

.subtitle {
  font-size: 13px;
}

.detail-card {
  padding: 12px 16px;
}

.card-title {
  font-size: 13px;
  margin-bottom: 12px;
}

.chart-container {
  height: 240px;
}

.run-history {
  max-height: 300px;
  overflow-y: auto;
}
</style>
