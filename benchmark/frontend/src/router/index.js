import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import Jobs from '../views/Jobs.vue'
import JobDetail from '../views/JobDetail.vue'
import Templates from '../views/Templates.vue'
import DataSources from '../views/DataSources.vue'
import TestSets from '../views/TestSets.vue'
import Runs from '../views/Runs.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', alias: '/dashboard', component: Dashboard, meta: { title: '控制台' } },
    { path: '/jobs', component: Jobs, meta: { title: '任务配置' } },
    { path: '/jobs/:id', component: JobDetail, meta: { title: '任务详情' } },
    { path: '/test-sets', component: TestSets, meta: { title: '测试集' } },
    { path: '/templates', component: Templates, meta: { title: 'SQL 模板' } },
    { path: '/datasources', component: DataSources, meta: { title: '数据源管理' } },
    { path: '/runs', component: Runs, meta: { title: '运行记录' } }
  ]
})
